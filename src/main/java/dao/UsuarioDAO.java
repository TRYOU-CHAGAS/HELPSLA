package dao;

import config.FirestoreFactory;
import config.FirestoreUtil;
import exception.RegraNegocioException;
import model.entity.Usuario;
import model.enums.Perfil;
import org.mindrot.jbcrypt.BCrypt;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Base64;

public class UsuarioDAO {
    public record Credencial(Usuario usuario, String senhaHash) { }

    public Optional<Credencial> buscarCredencial(String email) {
        try {
            return FirestoreFactory.obter().collection("usuarios").whereEqualTo("email", email).get().get().getDocuments().stream()
                    .filter(doc -> Boolean.TRUE.equals(doc.getBoolean("ativo"))).findFirst()
                    .map(doc -> new Credencial(mapear(doc.getData()), doc.getString("senhaHash")));
        } catch (Exception e) { throw new IllegalStateException("Não foi possível consultar o usuário.", e); }
    }

    public Optional<Usuario> buscarPorId(long id) {
        try {
            var doc = FirestoreFactory.obter().collection("usuarios").document(Long.toString(id)).get().get();
            return doc.exists() && Boolean.TRUE.equals(doc.getBoolean("ativo"))
                    ? Optional.of(mapear(doc.getData())) : Optional.empty();
        } catch (Exception e) { throw new IllegalStateException("Não foi possível consultar o usuário.", e); }
    }

    public long cadastrarSolicitante(String nome, String email, String senha) {
        String normalizado = email.trim().toLowerCase();
        long empresaId = primeiraEmpresaAtiva();
        long id = FirestoreUtil.novoId();
        var firestore = FirestoreFactory.obter();
        var chaveEmail = firestore.collection("usuarios_por_email").document(chaveEmail(normalizado));
        var usuarioRef = firestore.collection("usuarios").document(Long.toString(id));
        Map<String, Object> usuario = dadosUsuario(id, empresaId, null, nome, normalizado,
                BCrypt.hashpw(senha, BCrypt.gensalt()), Perfil.SOLICITANTE);
        FirestoreUtil.aguardar(firestore.runTransaction(transacao -> {
            if (transacao.get(chaveEmail).get().exists()) throw new RegraNegocioException("Este e-mail já possui cadastro.");
            transacao.create(chaveEmail, Map.of("usuarioId", id));
            transacao.create(usuarioRef, usuario);
            return null;
        }), "Não foi possível criar a conta.");
        return id;
    }

    public List<Usuario> listarTecnicos(long prestadoraId) {
        try {
            List<Usuario> tecnicos = new ArrayList<>();
            for (var doc : FirestoreFactory.obter().collection("usuarios").whereEqualTo("prestadoraId", prestadoraId).get().get().getDocuments()) {
                if (Boolean.TRUE.equals(doc.getBoolean("ativo")) && "TECNICO".equals(doc.getString("perfil")))
                    tecnicos.add(mapear(doc.getData()));
            }
            return tecnicos;
        } catch (Exception e) { throw new IllegalStateException("Não foi possível carregar os técnicos.", e); }
    }

    public void gravarUsuario(long id, long empresaId, Long prestadoraId, String nome, String email,
                              String senhaHash, Perfil perfil) {
        var firestore = FirestoreFactory.obter();
        var lote = firestore.batch();
        String emailNormalizado = email.toLowerCase();
        lote.set(firestore.collection("usuarios").document(Long.toString(id)),
                dadosUsuario(id, empresaId, prestadoraId, nome, emailNormalizado, senhaHash, perfil));
        lote.set(firestore.collection("usuarios_por_email").document(chaveEmail(emailNormalizado)), Map.of("usuarioId", id));
        FirestoreUtil.aguardar(lote.commit(), "Não foi possível salvar o usuário.");
    }

    private long primeiraEmpresaAtiva() {
        try {
            return FirestoreFactory.obter().collection("empresas").whereEqualTo("ativa", true).get().get().getDocuments().stream()
                    .mapToLong(doc -> FirestoreUtil.longo(doc.getData(), "id")).findFirst()
                    .orElseThrow(() -> new IllegalStateException("Nenhuma empresa ativa foi configurada no Firestore."));
        } catch (IllegalStateException e) { throw e; }
        catch (Exception e) { throw new IllegalStateException("Não foi possível localizar a empresa.", e); }
    }

    private Usuario mapear(Map<String, Object> dados) {
        return new Usuario(FirestoreUtil.longo(dados, "id"), FirestoreUtil.longo(dados, "empresaId"),
                FirestoreUtil.longoOpcional(dados, "prestadoraId"), (String) dados.get("nome"),
                (String) dados.get("email"), Perfil.valueOf((String) dados.get("perfil")));
    }

    private Map<String, Object> dadosUsuario(long id, long empresaId, Long prestadoraId, String nome,
                                              String email, String senhaHash, Perfil perfil) {
        Map<String, Object> dados = new java.util.LinkedHashMap<>();
        dados.put("id", id); dados.put("empresaId", empresaId); dados.put("prestadoraId", prestadoraId);
        dados.put("nome", nome); dados.put("email", email); dados.put("senhaHash", senhaHash);
        dados.put("perfil", perfil.name()); dados.put("ativo", true);
        return dados;
    }

    private String chaveEmail(String email) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(email.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
