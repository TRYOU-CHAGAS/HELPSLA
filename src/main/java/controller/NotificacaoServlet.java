package controller;

import config.JsonUtil;
import dao.PreferenciaNotificacaoDAO;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import model.entity.Usuario;
import model.enums.TipoNotificacao;
import java.io.IOException;
import java.util.Set;
import dao.NotificacaoDAO;

@WebServlet("/api/notificacoes/*")
public class NotificacaoServlet extends HttpServlet {
    private final PreferenciaNotificacaoDAO preferencias = new PreferenciaNotificacaoDAO();
    private final NotificacaoDAO notificacoes = new NotificacaoDAO();
    public record PreferenciasRequest(Set<TipoNotificacao> tipos) { }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Usuario usuario = (Usuario) request.getSession(false).getAttribute("usuario");
        if ("/preferencias".equals(request.getPathInfo()))
            JsonUtil.enviar(response, 200, java.util.Map.of("tipos", preferencias.buscar(usuario.id())));
        else if (request.getPathInfo() == null || "/".equals(request.getPathInfo()))
            JsonUtil.enviar(response, 200, notificacoes.listar(usuario.id()));
        else JsonUtil.erro(response, 404, "Rota não encontrada.");
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Usuario usuario = (Usuario) request.getSession(false).getAttribute("usuario");
        try {
            if (!"/preferencias".equals(request.getPathInfo())) {
                JsonUtil.erro(response, 404, "Rota não encontrada.");
                return;
            }
            PreferenciasRequest dados = JsonUtil.ler(request, PreferenciasRequest.class);
            if (dados.tipos() == null) {
                JsonUtil.erro(response, 400, "Informe a lista de notificações que deseja receber.");
                return;
            }
            preferencias.salvar(usuario.id(), dados.tipos());
            JsonUtil.enviar(response, 200, java.util.Map.of("tipos", dados.tipos(), "mensagem", "Preferências atualizadas."));
        } catch (IllegalArgumentException e) {
            JsonUtil.erro(response, 400, "Tipo de notificação inválido.");
        }
    }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if ("PATCH".equalsIgnoreCase(request.getMethod())) {
            marcarComoLida(request, response);
            return;
        }
        super.service(request, response);
    }

    private void marcarComoLida(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Usuario usuario = (Usuario) request.getSession(false).getAttribute("usuario");
        String caminho = request.getPathInfo();
        if (caminho == null || caminho.length() < 2 || caminho.equals("/preferencias")) {
            JsonUtil.erro(response, 404, "Rota não encontrada.");
            return;
        }
        String id = caminho.substring(1);
        if (notificacoes.marcarComoLida(usuario.id(), id))
            JsonUtil.enviar(response, 200, java.util.Map.of("mensagem", "Notificação marcada como lida."));
        else JsonUtil.erro(response, 404, "Notificação não encontrada.");
    }
}
