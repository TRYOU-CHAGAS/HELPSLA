package controller;

import config.JsonUtil;
import dao.CategoriaDAO;
import dao.RegraSlaDAO;
import exception.RegraNegocioException;
import model.entity.Usuario;
import service.ChamadoService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.Map;

@WebServlet("/api/chamados/*")
public class ChamadoServlet extends HttpServlet {
    private final ChamadoService service = new ChamadoService();
    private final CategoriaDAO categoriaDAO = new CategoriaDAO();
    private final RegraSlaDAO regraSlaDAO = new RegraSlaDAO();
    public record NovoChamado(String titulo, String descricao, Long categoriaId, String prioridade) { }
    public record Acao(String valor) { }
    public record Avaliacao(int nota, String comentario) { }

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        if ("PATCH".equalsIgnoreCase(request.getMethod())) {
            atualizarParcialmente(request, response);
            return;
        }
        super.service(request, response);
    }

    private void atualizarParcialmente(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Usuario usuario = usuario(request);
        String info = path(request);
        if (info.length() < 2) {
            JsonUtil.erro(response, 404, "Rota não encontrada.");
            return;
        }
        String[] partes = info.substring(1).split("/");
        if (partes.length != 2 || !"status".equals(partes[1])) {
            JsonUtil.erro(response, 404, "Rota não encontrada.");
            return;
        }
        try {
            Acao dados = JsonUtil.ler(request, Acao.class);
            String mensagem = service.executarAcao(usuario, Long.parseLong(partes[0]), "status", dados.valor());
            JsonUtil.enviar(response, 200, Map.of("mensagem", mensagem));
        } catch (Exception e) { tratarErro(response, e); }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Usuario usuario = usuario(request);
        String path = path(request);
        try {
            if (path.isBlank() || path.equals("/")) { JsonUtil.enviar(response, 200, service.listar(usuario)); return; }
            if (path.equals("/categorias")) { JsonUtil.enviar(response, 200, categoriaDAO.listarAtivas()); return; }
            if (path.equals("/slas")) { JsonUtil.enviar(response, 200, regraSlaDAO.listarAtivas(usuario.empresaId())); return; }
            String[] partes = path.substring(1).split("/");
            if (partes.length == 1) { JsonUtil.enviar(response, 200, service.detalhe(usuario, Long.parseLong(partes[0]))); return; }
            JsonUtil.erro(response, 404, "Rota não encontrada.");
        } catch (Exception e) { tratarErro(response, e); }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Usuario usuario = usuario(request);
        String path = path(request);
        try {
            if (path.isBlank() || path.equals("/")) {
                NovoChamado dto = JsonUtil.ler(request, NovoChamado.class);
                long id = service.abrir(usuario, dto.titulo(), dto.descricao(), dto.categoriaId(), dto.prioridade());
                JsonUtil.enviar(response, 201, Map.of("id", Long.toString(id), "mensagem", "Chamado aberto com sucesso.")); return;
            }
            String[] partes = path.substring(1).split("/");
            if (partes.length == 2 && partes[1].equals("avaliacao")) {
                Avaliacao dto = JsonUtil.ler(request, Avaliacao.class);
                service.avaliar(usuario, Long.parseLong(partes[0]), dto.nota(), dto.comentario());
                JsonUtil.enviar(response, 201, Map.of("mensagem", "Avaliação registrada.")); return;
            }
            if (partes.length == 2 && partes[1].equals("mensagens")) {
                Acao dto = JsonUtil.ler(request, Acao.class);
                service.enviarMensagem(usuario, Long.parseLong(partes[0]), dto.valor());
                JsonUtil.enviar(response, 201, Map.of("mensagem", "Mensagem enviada.")); return;
            }
            if (partes.length == 2) {
                String dado = "";
                if (partes[1].equals("status") || partes[1].equals("solucao") || partes[1].equals("cancelar")) dado = JsonUtil.ler(request, Acao.class).valor();
                String mensagem = service.executarAcao(usuario, Long.parseLong(partes[0]), partes[1], dado);
                JsonUtil.enviar(response, 200, Map.of("mensagem", mensagem)); return;
            }
            JsonUtil.erro(response, 404, "Rota não encontrada.");
        } catch (Exception e) { tratarErro(response, e); }
    }

    private void tratarErro(HttpServletResponse response, Exception e) throws IOException {
        getServletContext().log("Falha ao processar requisição de chamado: "
                + e.getClass().getSimpleName() + " - " + e.getMessage(), e);
        if (e instanceof NumberFormatException) JsonUtil.erro(response, 400, "Identificador de chamado inválido.");
        else if (e instanceof RegraNegocioException) JsonUtil.erro(response, 400, e.getMessage());
        else if (e instanceof IllegalStateException) JsonUtil.erro(response, 500, e.getMessage());
        else JsonUtil.erro(response, 500, "Ocorreu um erro ao processar o chamado.");
    }
    private Usuario usuario(HttpServletRequest r) { return (Usuario) r.getSession(false).getAttribute("usuario"); }
    private String path(HttpServletRequest r) { String p = r.getPathInfo(); return p == null ? "" : p; }
}
