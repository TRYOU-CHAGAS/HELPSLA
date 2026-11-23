package service;

import dao.CategoriaDAO;
import dao.PainelDAO;
import dao.RegraSlaDAO;
import dao.ServicoDAO;
import exception.RegraNegocioException;
import model.entity.Usuario;
import model.enums.Perfil;
import model.enums.Prioridade;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AdminService {
    private final RegraSlaDAO slaDAO;
    private final CategoriaDAO categoriaDAO;
    private final PainelDAO painelDAO;
    private final ServicoDAO servicoDAO;
    public record RegraInput(String prioridade, Integer prazoMinutos, Integer prazoAceiteMinutos) { }
    public record CategoriaInput(String nome, String descricao) { }

    public AdminService() { this(new RegraSlaDAO(), new CategoriaDAO(), new PainelDAO(), new ServicoDAO()); }

    public AdminService(RegraSlaDAO slaDAO, CategoriaDAO categoriaDAO, PainelDAO painelDAO, ServicoDAO servicoDAO) {
        this.slaDAO = Objects.requireNonNull(slaDAO);
        this.categoriaDAO = Objects.requireNonNull(categoriaDAO);
        this.painelDAO = Objects.requireNonNull(painelDAO);
        this.servicoDAO = Objects.requireNonNull(servicoDAO);
    }

    public Map<String, Long> relatorio(Usuario usuario) { exigirAdmin(usuario); return painelDAO.contar(usuario.empresaId()); }
    public List<Map<String, Object>> listarSlas(Usuario usuario) { exigirAdmin(usuario); return slaDAO.listar(usuario.empresaId()); }

    public void salvarSla(Usuario usuario, RegraInput input) {
        exigirAdmin(usuario);
        Prioridade prioridade;
        try { prioridade = Prioridade.valueOf(input.prioridade()); }
        catch (Exception e) { throw new RegraNegocioException("Prioridade inválida."); }
        if (input.prazoMinutos() == null || input.prazoMinutos() < 1 || input.prazoMinutos() > 525600)
            throw new RegraNegocioException("O prazo deve estar entre 1 minuto e 365 dias.");
        if (input.prazoAceiteMinutos() == null || input.prazoAceiteMinutos() < 1 || input.prazoAceiteMinutos() > 525600)
            throw new RegraNegocioException("O prazo para aceite deve estar entre 1 minuto e 365 dias.");
        long servicoId = servicoDAO.primeiroAtivoId();
        slaDAO.atualizarRegra(usuario.empresaId(), servicoId, servicoDAO.nome(servicoId), prioridade,
                input.prazoMinutos(), input.prazoAceiteMinutos(), usuario.id());
    }

    public void criarCategoria(Usuario usuario, CategoriaInput input) {
        exigirAdmin(usuario);
        if (input.nome() == null || input.nome().trim().length() < 3 || input.nome().trim().length() > 100)
            throw new RegraNegocioException("O nome da categoria deve ter entre 3 e 100 caracteres.");
        long servicoId = servicoDAO.primeiroAtivoId();
        categoriaDAO.criar(servicoId, input.nome().trim(), input.descricao());
    }

    private void exigirAdmin(Usuario usuario) {
        if (usuario.perfil() != Perfil.ADMIN) throw new RegraNegocioException("Apenas administradores podem acessar esta função.");
    }
}
