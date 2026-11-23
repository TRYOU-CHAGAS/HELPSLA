package controller;

import config.JsonUtil;
import dao.CategoriaDAO;
import exception.RegraNegocioException;
import model.entity.Usuario;
import service.AdminService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.Map;

@WebServlet("/api/admin/*")
public class AdminServlet extends HttpServlet {
    private final AdminService service = new AdminService();
    private final CategoriaDAO categoriaDAO = new CategoriaDAO();
    @Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
        Usuario usuario = usuario(req); String path = req.getPathInfo();
        try {
            if ("/relatorios".equals(path)) JsonUtil.enviar(res, 200, service.relatorio(usuario));
            else if ("/slas".equals(path)) JsonUtil.enviar(res, 200, service.listarSlas(usuario));
            else if ("/categorias".equals(path)) { exigirAdmin(usuario); JsonUtil.enviar(res, 200, categoriaDAO.listarAtivas()); }
            else JsonUtil.erro(res, 404, "Rota não encontrada.");
        } catch (Exception e) { tratar(res, e); }
    }
    @Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
        Usuario usuario = usuario(req); String path = req.getPathInfo();
        try {
            if ("/slas".equals(path)) { service.salvarSla(usuario, JsonUtil.ler(req, AdminService.RegraInput.class)); JsonUtil.enviar(res, 200, Map.of("mensagem", "Regra SLA salva.")); }
            else if ("/categorias".equals(path)) { service.criarCategoria(usuario, JsonUtil.ler(req, AdminService.CategoriaInput.class)); JsonUtil.enviar(res, 201, Map.of("mensagem", "Categoria criada.")); }
            else JsonUtil.erro(res, 404, "Rota não encontrada.");
        } catch (Exception e) { tratar(res, e); }
    }
    private Usuario usuario(HttpServletRequest r) { return (Usuario) r.getSession(false).getAttribute("usuario"); }
    private void exigirAdmin(Usuario u) { if (u.perfil() != model.enums.Perfil.ADMIN) throw new RegraNegocioException("Apenas administradores podem acessar esta função."); }
    private void tratar(HttpServletResponse r, Exception e) throws IOException {
        if (e instanceof RegraNegocioException) JsonUtil.erro(r, 400, e.getMessage());
        else if (e instanceof IllegalStateException) JsonUtil.erro(r, 500, e.getMessage());
        else JsonUtil.erro(r, 500, "Não foi possível processar a solicitação.");
    }
}
