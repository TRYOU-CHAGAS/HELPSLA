package service;

import dao.UsuarioDAO;
import model.entity.Usuario;
import org.mindrot.jbcrypt.BCrypt;
import exception.RegraNegocioException;
import dao.PreferenciaNotificacaoDAO;
import model.enums.TipoNotificacao;
import java.util.Set;
import java.util.Objects;

public class AutenticacaoService {
    private final UsuarioDAO usuarioDAO;
    private final PreferenciaNotificacaoDAO preferenciaDAO;

    public AutenticacaoService() { this(new UsuarioDAO(), new PreferenciaNotificacaoDAO()); }

    public AutenticacaoService(UsuarioDAO usuarioDAO, PreferenciaNotificacaoDAO preferenciaDAO) {
        this.usuarioDAO = Objects.requireNonNull(usuarioDAO);
        this.preferenciaDAO = Objects.requireNonNull(preferenciaDAO);
    }

    public Usuario autenticar(String email, String senha) {
        if (email == null || senha == null || email.isBlank() || senha.isBlank()) return null;
        try {
            return usuarioDAO.buscarCredencial(email.trim().toLowerCase())
                    .filter(c -> BCrypt.checkpw(senha, c.senhaHash()))
                    .map(UsuarioDAO.Credencial::usuario).orElse(null);
        } catch (Exception e) { throw new IllegalStateException("Não foi possível autenticar.", e); }
    }

    public void cadastrarSolicitante(String nome, String email, String senha, Set<TipoNotificacao> tiposNotificacao) {
        if (nome == null || nome.trim().length() < 3 || nome.trim().length() > 120) throw new RegraNegocioException("Informe um nome entre 3 e 120 caracteres.");
        if (email == null || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw new RegraNegocioException("Informe um e-mail válido.");
        if (senha == null || senha.length() < 8) throw new RegraNegocioException("A senha deve ter pelo menos 8 caracteres.");
        try {
            long id = usuarioDAO.cadastrarSolicitante(nome.trim(), email.trim().toLowerCase(), senha);
            preferenciaDAO.salvar(id, tiposNotificacao);
        }
        catch (RegraNegocioException e) { throw e; }
        catch (Exception e) { throw new IllegalStateException("Não foi possível criar a conta.", e); }
    }
}
