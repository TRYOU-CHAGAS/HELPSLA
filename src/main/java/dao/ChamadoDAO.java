package dao;

import builder.ChamadoBuilder;
import config.FirestoreFactory;
import config.FirestoreUtil;
import exception.RegraNegocioException;
import model.entity.Chamado;
import model.entity.Usuario;
import model.enums.Perfil;
import model.enums.Prioridade;
import model.enums.StatusChamado;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import com.google.cloud.firestore.Transaction;
import com.google.cloud.firestore.WriteBatch;
import com.google.cloud.firestore.Query;

public class ChamadoDAO {
    public long criarComHistorico(String codigo, Usuario solicitante, ChamadoBuilder.DadosAbertura dados,
                                  long prestadoraId, long regraSlaId, LocalDateTime prazoAceite,
                                  int prazoResolucaoMinutos, String categoriaNome) {
        long id = FirestoreUtil.novoId();
        String chamadoId = Long.toString(id);
        LocalDateTime agora = FirestoreUtil.agora();
        var referencia = FirestoreFactory.obter().collection("chamados").document(chamadoId);
        WriteBatch lote = FirestoreFactory.obter().batch();
        Map<String, Object> chamado = dadosNovoChamado(id, codigo, solicitante, dados, prestadoraId,
                regraSlaId, prazoAceite, prazoResolucaoMinutos, categoriaNome, agora);
        Map<String, Object> historico = dadosHistorico("ABERTO", null, StatusChamado.ABERTO,
                "Chamado criado.", solicitante, agora);
        lote.set(referencia, chamado);
        lote.set(referencia.collection("historico").document(UUID.randomUUID().toString()), historico);
        FirestoreUtil.aguardar(lote.commit(), "Não foi possível abrir o chamado.");
        return id;
    }

    public List<Chamado> listar(Usuario usuario) {
        try {
            List<Chamado> chamados = new ArrayList<>();
            Query consulta = FirestoreFactory.obter().collection("chamados");
            if (usuario.perfil() == Perfil.SOLICITANTE) consulta = consulta.whereEqualTo("solicitanteId", usuario.id());
            else if (usuario.perfil() == Perfil.TECNICO) consulta = consulta.whereEqualTo("prestadoraId", usuario.prestadoraId());
            else consulta = consulta.whereEqualTo("empresaId", usuario.empresaId());
            for (var doc : consulta.get().get().getDocuments()) {
                Chamado chamado = mapear(doc.getData());
                boolean visivel = switch (usuario.perfil()) {
                    case SOLICITANTE -> chamado.solicitanteId() == usuario.id();
                    case TECNICO -> java.util.Objects.equals(usuario.prestadoraId(), chamado.prestadoraId())
                            && (chamado.tecnicoId() == null || chamado.tecnicoId() == usuario.id())
                            && chamado.status() != StatusChamado.ENCERRADO && chamado.status() != StatusChamado.CANCELADO;
                    case ADMIN -> chamado.empresaId() == usuario.empresaId();
                };
                if (visivel) chamados.add(chamado);
            }
            chamados.sort(Comparator.comparingInt((Chamado c) -> prioridadeStatus(c.status()))
                    .thenComparing(Chamado::prazoSla, Comparator.nullsLast(Comparator.naturalOrder())));
            return chamados;
        } catch (Exception e) { throw new IllegalStateException("Não foi possível carregar os chamados.", e); }
    }

    public Optional<Chamado> buscar(long id) {
        try {
            var doc = FirestoreFactory.obter().collection("chamados").document(Long.toString(id)).get().get();
            return doc.exists() ? Optional.of(mapear(doc.getData())) : Optional.empty();
        } catch (Exception e) { throw new IllegalStateException("Não foi possível consultar o chamado.", e); }
    }

    public Optional<Chamado> buscar(Transaction transacao, long id) {
        try {
            var doc = transacao.get(FirestoreFactory.obter().collection("chamados").document(Long.toString(id))).get();
            return doc.exists() ? Optional.of(mapear(doc.getData())) : Optional.empty();
        } catch (Exception e) { throw new IllegalStateException("Não foi possível carregar o chamado para atualização.", e); }
    }

    public List<Map<String, Object>> listarHistorico(long chamadoId) {
        try {
            List<Map<String, Object>> itens = new ArrayList<>();
            for (var doc : FirestoreFactory.obter().collection("chamados").document(Long.toString(chamadoId))
                    .collection("historico").get().get().getDocuments()) itens.add(doc.getData());
            itens.sort(Comparator.comparing(item -> item.get("data").toString()));
            return itens;
        } catch (Exception e) { throw new IllegalStateException("Não foi possível carregar o histórico.", e); }
    }

    public List<Map<String, Object>> listarMensagens(long chamadoId) {
        try {
            List<Map<String, Object>> itens = new ArrayList<>();
            for (var doc : FirestoreFactory.obter().collection("chamados").document(Long.toString(chamadoId))
                    .collection("mensagens").get().get().getDocuments()) {
                Map<String, Object> item = new LinkedHashMap<>(doc.getData()); item.put("id", doc.getId()); itens.add(item);
            }
            itens.sort(Comparator.comparing(item -> item.get("criadoEm").toString()));
            return itens;
        } catch (Exception e) { throw new IllegalStateException("Não foi possível carregar as mensagens.", e); }
    }

    public void inserirMensagem(Transaction transacao, long chamadoId, Usuario autor, String mensagem) {
        var referencia = FirestoreFactory.obter().collection("chamados").document(Long.toString(chamadoId))
                .collection("mensagens").document(UUID.randomUUID().toString());
        transacao.set(referencia, dadosMensagem(autor, mensagem));
    }

    public void atualizarStatus(Transaction transacao, Chamado chamado, StatusChamado status,
                                Usuario ator, String solucao) {
        LocalDateTime agora = FirestoreUtil.agora();
        Map<String, Object> atualizacao = new LinkedHashMap<>();
        atualizacao.put("status", status.name());
        if (ator.perfil() == Perfil.TECNICO) {
            atualizacao.put("tecnicoId", ator.id()); atualizacao.put("tecnicoNome", ator.nome());
        }
        if (solucao != null) atualizacao.put("solucao", solucao);
        if (status == StatusChamado.EM_ATENDIMENTO && chamado.prazoResolucaoSla() == null) {
            atualizacao.put("assumidoEm", agora.toString());
            atualizacao.put("prazoResolucaoSla", agora.plusMinutes(chamado.prazoResolucaoMinutos()).toString());
        }
        if (status == StatusChamado.RESOLVIDO) atualizacao.put("resolvidoEm", agora.toString());
        if (status == StatusChamado.ENCERRADO) atualizacao.put("encerradoEm", agora.toString());
        atualizacao.put("atualizadoEm", agora.toString());
        transacao.update(FirestoreFactory.obter().collection("chamados").document(Long.toString(chamado.id())), atualizacao);
    }

    public void registrarHistorico(Transaction transacao, long chamadoId, Usuario ator, String acao,
                                   StatusChamado anterior, StatusChamado novo, String observacao) {
        var referencia = FirestoreFactory.obter().collection("chamados").document(Long.toString(chamadoId))
                .collection("historico").document(UUID.randomUUID().toString());
        transacao.set(referencia, dadosHistorico(acao, anterior, novo, observacao, ator, FirestoreUtil.agora()));
    }

    private Map<String, Object> dadosNovoChamado(long id, String codigo, Usuario solicitante,
                                                  ChamadoBuilder.DadosAbertura dados, long prestadoraId,
                                                  long regraSlaId, LocalDateTime prazoAceite,
                                                  int prazoResolucaoMinutos, String categoriaNome, LocalDateTime agora) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("id", id); doc.put("codigo", codigo); doc.put("empresaId", solicitante.empresaId());
        doc.put("prestadoraId", prestadoraId); doc.put("solicitanteId", solicitante.id()); doc.put("tecnicoId", null);
        doc.put("categoriaId", dados.categoriaId()); doc.put("regraSlaId", regraSlaId);
        doc.put("titulo", dados.titulo()); doc.put("descricao", dados.descricao());
        doc.put("prioridade", dados.prioridade().name()); doc.put("status", StatusChamado.ABERTO.name());
        doc.put("abertoEm", agora.toString()); doc.put("prazoSla", prazoAceite.toString());
        doc.put("prazoResolucaoSla", null); doc.put("prazoResolucaoMinutos", prazoResolucaoMinutos);
        doc.put("assumidoEm", null); doc.put("resolvidoEm", null); doc.put("encerradoEm", null);
        doc.put("categoriaNome", categoriaNome); doc.put("solicitanteNome", solicitante.nome());
        doc.put("tecnicoNome", null); doc.put("solucao", null);
        return doc;
    }

    private Map<String, Object> dadosHistorico(String acao, StatusChamado anterior, StatusChamado novo,
                                                String observacao, Usuario ator, LocalDateTime data) {
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("acao", acao); doc.put("statusAnterior", anterior == null ? null : anterior.name());
        doc.put("statusNovo", novo == null ? null : novo.name()); doc.put("observacao", observacao);
        doc.put("data", data.toString()); doc.put("ator", ator.nome());
        return doc;
    }

    private Map<String, Object> dadosMensagem(Usuario autor, String mensagem) {
        return Map.of("autorId", autor.id(), "autorNome", autor.nome(), "autorPerfil", autor.perfil().name(),
                "mensagem", mensagem, "criadoEm", FirestoreUtil.agora().toString());
    }

    private Chamado mapear(Map<String, Object> d) {
        return new Chamado(FirestoreUtil.longo(d, "id"), (String) d.get("codigo"), FirestoreUtil.longo(d, "empresaId"),
                FirestoreUtil.longoOpcional(d, "prestadoraId"), FirestoreUtil.longo(d, "solicitanteId"),
                FirestoreUtil.longoOpcional(d, "tecnicoId"), FirestoreUtil.longo(d, "categoriaId"), FirestoreUtil.longo(d, "regraSlaId"),
                (String) d.get("titulo"), (String) d.get("descricao"), Prioridade.valueOf((String) d.get("prioridade")),
                StatusChamado.valueOf((String) d.get("status")), FirestoreUtil.data(d.get("abertoEm")),
                FirestoreUtil.data(d.get("prazoSla")), FirestoreUtil.data(d.get("prazoResolucaoSla")),
                FirestoreUtil.data(d.get("resolvidoEm")), FirestoreUtil.data(d.get("encerradoEm")),
                (String) d.get("categoriaNome"), (String) d.get("solicitanteNome"), (String) d.get("tecnicoNome"),
                (String) d.get("solucao"), FirestoreUtil.inteiro(d, "prazoResolucaoMinutos"));
    }

    private int prioridadeStatus(StatusChamado status) {
        return switch (status) { case ABERTO -> 0; case EM_TRIAGEM -> 1; case EM_ATENDIMENTO -> 2; default -> 3; };
    }
}
