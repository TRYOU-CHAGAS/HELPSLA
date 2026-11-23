package filter;

import config.JsonUtil;
import model.entity.Usuario;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

@WebFilter("/api/*")
public class AutenticacaoFilter implements Filter {
    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        response.setHeader("Cache-Control", "no-store");
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.equals("/api/login") || path.equals("/api/cadastro")) { chain.doFilter(req, res); return; }
        HttpSession session = request.getSession(false);
        Usuario usuario = session == null ? null : (Usuario) session.getAttribute("usuario");
        if (usuario == null) { JsonUtil.erro(response, 401, "Faça login para continuar."); return; }
        if (isUnsafe(request.getMethod())) {
            String esperado = (String) session.getAttribute("csrf");
            String recebido = request.getHeader("X-CSRF-Token");
            if (esperado == null || recebido == null || !esperado.equals(recebido)) {
                JsonUtil.erro(response, 403, "Sessão de segurança inválida. Atualize a página e tente novamente."); return;
            }
        }
        chain.doFilter(req, res);
    }

    public static String novoToken() {
        byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private boolean isUnsafe(String method) { return !("GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method)); }
}
