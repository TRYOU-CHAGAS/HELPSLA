package service;

import builder.ChamadoBuilder;
import command.ChamadoCommand;
import command.ChamadoCommandFactory;
import dao.AvaliacaoDAO;
import dao.CategoriaDAO;
import dao.ChamadoDAO;
import dao.NotificacaoDAO;
import dao.PrestadoraDAO;
import dao.RegraSlaDAO;
import dao.UsuarioDAO;
import config.FirestoreFactory;
import config.FirestoreUtil;
import exception.RegraNegocioException;
import model.entity.Chamado;
import model.entity.Usuario;
import model.enums.Perfil;
import model.enums.Prioridade;
import model.enums.StatusChamado;
import model.enums.TipoNotificacao;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class ChamadoService {
    private final ChamadoDAO chamadoDAO;
    private final CategoriaDAO categoriaDAO;
    private final RegraSlaDAO regraSlaDAO;
    private final PrestadoraDAO prestadoraDAO;
    private final AvaliacaoDAO avaliacaoDAO;
    private final UsuarioDAO usuarioDAO;
    private final NotificacaoDAO notificacaoDAO;
    private final NotificacaoService notificacoes;
    private final ChamadoCommandFactory commandFactory;

    public record Detalhe(Chamado chamado, List<Map<String, Object>> historico, List<Map<String, Object>> mensagens) { }

    public ChamadoService() {
        this(new ChamadoDAO(), new CategoriaDAO(), new RegraSlaDAO(), new PrestadoraDAO(),
                new AvaliacaoDAO(), new UsuarioDAO(), new NotificacaoDAO(), new NotificacaoService());
    }

    public ChamadoService(ChamadoDAO chamadoDAO, CategoriaDAO categoriaDAO, RegraSlaDAO regraSlaDAO,
                          PrestadoraDAO prestadoraDAO, AvaliacaoDAO avaliacaoDAO, UsuarioDAO usuarioDAO,
                          NotificacaoDAO notificacaoDAO, NotificacaoService notificacoes) {
        this.chamadoDAO = Objects.requireNonNull(chamadoDAO);
        this.categoriaDAO = Objects.requireNonNull(categoriaDAO);
        this.regraSlaDAO = Objects.requireNonNull(regraSlaDAO);
        this.prestadoraDAO = Objects.requireNonNull(prestadoraDAO);
        this.avaliacaoDAO = Objects.requireNonNull(avaliacaoDAO);
        this.usuarioDAO = Objects.requireNonNull(usuarioDAO);
        this.notificacaoDAO = Objects.requireNonNull(notificacaoDAO);
        this.notificacoes = Objects.requireNonNull(notificacoes);
        this.commandFactory = new ChamadoCommandFactory(chamadoDAO);
    }

    public List<Chamado> listar(Usuario usuario) { return chamadoDAO.listar(usuario); }

    public Detalhe detalhe(Usuario usuario, long id) {
        Chamado chamado = chamadoDAO.buscar(id).orElseThrow(() -> new RegraNegocioException("Chamado não encontrado."));
        autorizarLeitura(usuario, chamado);
        return new Detalhe(chamado, chamadoDAO.listarHistorico(id), chamadoDAO.listarMensagens(id));
    }

    public long abrir(Usuario solicitante, String titulo, String descricao, Long categoriaId, String prioridadeTexto) {
        exigirPerfil(solicitante, Perfil.SOLICITANTE);
        Prioridade prioridade;
        try { prioridade = Prioridade.valueOf(prioridadeTexto); }
        catch (Exception e) { throw new RegraNegocioException("Prioridade inválida."); }
        ChamadoBuilder.DadosAbertura dados = new ChamadoBuilder().titulo(titulo).descricao(descricao)
                .categoriaId(categoriaId).prioridade(prioridade).build();
        long servicoId = categoriaDAO.buscarServicoId(dados.categoriaId());
        RegraSlaDAO.Regra regra = regraSlaDAO.buscarAtiva(solicitante.empresaId(), servicoId, prioridade)
                .orElseThrow(() -> new RegraNegocioException("Não existe regra de SLA ativa para essa prioridade."));
        long prestadoraId = prestadoraDAO.buscarAtivaParaServico(servicoId);
        String codigo = "HSL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        LocalDateTime abertura = FirestoreUtil.agora();
        long chamadoId = chamadoDAO.criarComHistorico(codigo, solicitante, dados, prestadoraId, regra.id(),
                abertura.plusMinutes(regra.prazoAceiteMinutos()), regra.prazoMinutos(),
                categoriaDAO.listarAtivas().stream().filter(c -> c.id() == dados.categoriaId()).map(c -> c.nome()).findFirst().orElse("Categoria"));
        notificarTecnicos(prestadoraId, codigo, chamadoId);
        return chamadoId;
    }

    public String executarAcao(Usuario ator, long chamadoId, String acao, String dado) {
        ChamadoCommand command = commandFactory.criar(acao);
        Chamado chamado = chamadoDAO.buscar(chamadoId).orElseThrow(() -> new RegraNegocioException("Chamado não encontrado."));
        autorizarAcao(ator, chamado, acao);
        String mensagem = FirestoreUtil.aguardar(FirestoreFactory.obter().runTransaction(transacao -> {
            Chamado atual = chamadoDAO.buscar(transacao, chamadoId)
                    .orElseThrow(() -> new RegraNegocioException("Chamado não encontrado."));
            autorizarAcao(ator, atual, acao);
            return command.executar(transacao, atual, ator, dado);
        }), "Não foi possível atualizar o chamado.");
        notificarAcao(chamado, acao, dado);
        return mensagem;
    }

    public void enviarMensagem(Usuario ator, long chamadoId, String texto) {
        String mensagem = texto == null ? "" : texto.trim();
        if (mensagem.isEmpty() || mensagem.length() > 2000)
            throw new RegraNegocioException("A mensagem deve ter entre 1 e 2000 caracteres.");
        Chamado chamado = FirestoreUtil.aguardar(FirestoreFactory.obter().runTransaction(transacao -> {
            Chamado atual = chamadoDAO.buscar(transacao, chamadoId)
                    .orElseThrow(() -> new RegraNegocioException("Chamado não encontrado."));
            autorizarMensagem(ator, atual);
            chamadoDAO.inserirMensagem(transacao, chamadoId, ator, mensagem);
            return atual;
        }), "Não foi possível enviar a mensagem.");
        notificarMensagem(ator, chamado);
    }

    public void avaliar(Usuario solicitante, long chamadoId, int nota, String comentario) {
        exigirPerfil(solicitante, Perfil.SOLICITANTE);
        if (nota < 1 || nota > 5) throw new RegraNegocioException("A nota deve estar entre 1 e 5.");
        FirestoreUtil.aguardar(FirestoreFactory.obter().runTransaction(transacao -> {
            Chamado chamado = chamadoDAO.buscar(transacao, chamadoId)
                    .orElseThrow(() -> new RegraNegocioException("Chamado não encontrado."));
            if (chamado.solicitanteId() != solicitante.id()) throw new RegraNegocioException("Você só pode avaliar seus chamados.");
            if (chamado.status() != StatusChamado.ENCERRADO) throw new RegraNegocioException("Só é possível avaliar um chamado encerrado.");
            avaliacaoDAO.inserir(transacao, chamadoId, solicitante.id(), nota, comentario == null ? null : comentario.trim());
            return null;
        }), "Não foi possível salvar a avaliação.");
    }

    private void notificarAcao(Chamado chamado, String acao, String dado) {
        TipoNotificacao tipo = switch (acao) {
            case "assumir" -> TipoNotificacao.CHAMADO_ASSUMIDO;
            case "status" -> TipoNotificacao.STATUS_ALTERADO;
            case "solucao" -> TipoNotificacao.CHAMADO_RESOLVIDO;
            default -> null;
        };
        if (tipo == null) return;
        String texto = switch (acao) {
            case "status" -> "O status do chamado foi alterado para " + dado + ".";
            case "solucao" -> "O técnico registrou a solução do chamado.";
            default -> "Um técnico assumiu o chamado.";
        };
        notificarUsuario(chamado.solicitanteId(), tipo, "Atualização do chamado " + chamado.codigo(), texto, chamado.id());
    }

    private void notificarMensagem(Usuario autor, Chamado chamado) {
        Long destinatarioId = autor.perfil() == Perfil.SOLICITANTE ? chamado.tecnicoId() : chamado.solicitanteId();
        if (destinatarioId != null) notificarUsuario(destinatarioId, TipoNotificacao.NOVA_MENSAGEM,
                "Nova mensagem no chamado " + chamado.codigo(), "Há uma nova mensagem na conversa do chamado.", chamado.id());
    }

    private void notificarTecnicos(long prestadoraId, String codigo, long chamadoId) {
        try {
            for (Usuario tecnico : usuarioDAO.listarTecnicos(prestadoraId))
                notificar(tecnico, TipoNotificacao.CHAMADO_NOVO, "Novo chamado " + codigo,
                        "Há um chamado disponível para atendimento.", chamadoId);
        } catch (RuntimeException e) { System.err.println("Aviso de novo chamado não enviado: " + e.getMessage()); }
    }

    private void notificarUsuario(long usuarioId, TipoNotificacao tipo, String titulo, String mensagem, long chamadoId) {
        try { usuarioDAO.buscarPorId(usuarioId).ifPresent(u -> notificar(u, tipo, titulo, mensagem, chamadoId)); }
        catch (RuntimeException e) { System.err.println("Notificação não enviada: " + e.getMessage()); }
    }

    private void notificar(Usuario usuario, TipoNotificacao tipo, String titulo, String mensagem, long chamadoId) {
        notificacoes.notificar(usuario, notificacaoDAO.criar(usuario.id(), tipo, titulo, mensagem, chamadoId));
    }

    private void autorizarLeitura(Usuario usuario, Chamado chamado) {
        boolean permitido = switch (usuario.perfil()) {
            case ADMIN -> usuario.empresaId() == chamado.empresaId();
            case SOLICITANTE -> usuario.id() == chamado.solicitanteId();
            case TECNICO -> Objects.equals(usuario.prestadoraId(), chamado.prestadoraId());
        };
        if (!permitido) throw new RegraNegocioException("Você não tem acesso a este chamado.");
    }

    private void autorizarAcao(Usuario usuario, Chamado chamado, String acao) {
        switch (acao) {
            case "assumir", "status", "solucao" -> {
                exigirPerfil(usuario, Perfil.TECNICO);
                if (!Objects.equals(usuario.prestadoraId(), chamado.prestadoraId())) throw new RegraNegocioException("Este chamado não pertence à sua prestadora.");
                if (chamado.tecnicoId() != null && chamado.tecnicoId() != usuario.id()) throw new RegraNegocioException("O chamado já está atribuído a outro técnico.");
            }
            case "encerrar", "cancelar" -> {
                exigirPerfil(usuario, Perfil.SOLICITANTE);
                if (usuario.id() != chamado.solicitanteId()) throw new RegraNegocioException("Somente quem abriu o chamado pode realizar esta ação.");
            }
            default -> throw new RegraNegocioException("Ação inválida.");
        }
    }

    private void autorizarMensagem(Usuario usuario, Chamado chamado) {
        if (chamado.status() == StatusChamado.ENCERRADO || chamado.status() == StatusChamado.CANCELADO)
            throw new RegraNegocioException("A conversa foi encerrada com o chamado.");
        boolean autorizado = switch (usuario.perfil()) {
            case SOLICITANTE -> usuario.id() == chamado.solicitanteId();
            case TECNICO -> Objects.equals(usuario.prestadoraId(), chamado.prestadoraId()) && Objects.equals(chamado.tecnicoId(), usuario.id());
            case ADMIN -> false;
        };
        if (!autorizado) throw new RegraNegocioException("Apenas o solicitante e o técnico responsável podem conversar neste chamado.");
    }

    private void exigirPerfil(Usuario usuario, Perfil esperado) {
        if (usuario.perfil() != esperado) throw new RegraNegocioException("Seu perfil não pode realizar esta ação.");
    }
}
