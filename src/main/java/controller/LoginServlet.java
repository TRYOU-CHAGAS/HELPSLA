package controller;

import config.JsonUtil;
import filter.AutenticacaoFilter;
import model.entity.Usuario;
import service.AutenticacaoService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Map;
import model.enums.TipoNotificacao;
import java.util.Set;
import java.util.EnumSet;

@WebServlet(urlPatterns = {"/api/login", "/api/cadastro", "/api/logout", "/api/me"})
public class LoginServlet extends HttpServlet {
    private final AutenticacaoService service = new AutenticacaoService();
    public record LoginRequest(String email, String senha) { }
    public record CadastroRequest(String nome, String email, String senha, Set<TipoNotificacao> notificacoes) { }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.equals("/api/logout")) { logout(request, response); return; }
        if (path.equals("/api/cadastro")) {
            try {
                CadastroRequest dados = JsonUtil.ler(request, CadastroRequest.class);
                Set<TipoNotificacao> tipos = dados.notificacoes() == null
                        ? EnumSet.allOf(TipoNotificacao.class) : dados.notificacoes();
                service.cadastrarSolicitante(dados.nome(), dados.email(), dados.senha(), tipos);
                JsonUtil.enviar(response, 201, Map.of("mensagem", "Cadastro realizado. Agora você já pode entrar."));
            } catch (exception.RegraNegocioException e) { JsonUtil.erro(response, 400, e.getMessage()); }
            catch (Exception e) { JsonUtil.erro(response, 500, "Não foi possível criar a conta."); }
            return;
        }
        if (!path.equals("/api/login")) { JsonUtil.erro(response, 405, "Método não permitido."); return; }
        try {
            LoginRequest dados = JsonUtil.ler(request, LoginRequest.class);
            Usuario usuario = service.autenticar(dados.email(), dados.senha());
            if (usuario == null) { JsonUtil.erro(response, 401, "E-mail ou senha inválidos."); return; }
            HttpSession anterior = request.getSession(false); if (anterior != null) anterior.invalidate();
            HttpSession session = request.getSession(true); session.setAttribute("usuario", usuario);
            session.setAttribute("csrf", AutenticacaoFilter.novoToken()); session.setMaxInactiveInterval(30 * 60);
            JsonUtil.enviar(response, 200, Map.of("usuario", usuario, "csrf", session.getAttribute("csrf")));
        } catch (Exception e) { JsonUtil.erro(response, 500, "Não foi possível entrar. Confira a conexão com o Firestore."); }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("usuario") == null) { JsonUtil.erro(response, 401, "Faça login para continuar."); return; }
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        JsonUtil.enviar(response, 200, Map.of("usuario", usuario, "csrf", session.getAttribute("csrf")));
    }

    private void logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false); if (session != null) session.invalidate();
        JsonUtil.enviar(response, 200, Map.of("mensagem", "Sessão encerrada."));
    }
}
