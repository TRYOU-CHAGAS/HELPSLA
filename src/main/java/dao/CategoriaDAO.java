package dao;

import com.google.cloud.firestore.QueryDocumentSnapshot;
import config.FirestoreFactory;
import config.FirestoreUtil;
import exception.RegraNegocioException;
import model.entity.Categoria;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class CategoriaDAO {
    public long buscarServicoId(long categoriaId) {
        Map<String, Object> dados = buscarDados(categoriaId);
        if (dados == null) throw new RegraNegocioException("Categoria não encontrada.");
        return FirestoreUtil.longo(dados, "servicoId");
    }

    public List<Categoria> listarAtivas() {
        try {
            return FirestoreFactory.obter().collection("categorias").get().get().getDocuments().stream()
                    .filter(doc -> Boolean.TRUE.equals(doc.getBoolean("ativa"))).map(this::mapear)
                    .sorted(Comparator.comparing(Categoria::nome)).toList();
        } catch (Exception e) { throw new IllegalStateException("Não foi possível carregar as categorias.", e); }
    }

    public boolean existeAtiva(long id) { return buscarDados(id) != null; }

    public void criar(long servicoId, String nome, String descricao) {
        if (listarAtivas().stream().anyMatch(item -> item.nome().equalsIgnoreCase(nome)))
            throw new RegraNegocioException("Já existe uma categoria com esse nome.");
        long id = FirestoreUtil.novoId();
        Map<String, Object> dados = Map.of("id", id, "servicoId", servicoId, "nome", nome,
                "descricao", descricao == null ? "" : descricao, "ativa", true);
        FirestoreUtil.aguardar(FirestoreFactory.obter().collection("categorias").document(Long.toString(id)).set(dados),
                "Não foi possível criar a categoria.");
    }

    private Map<String, Object> buscarDados(long id) {
        try {
            var doc = FirestoreFactory.obter().collection("categorias").document(Long.toString(id)).get().get();
            return doc.exists() && Boolean.TRUE.equals(doc.getBoolean("ativa")) ? doc.getData() : null;
        } catch (Exception e) { throw new IllegalStateException("Não foi possível consultar a categoria.", e); }
    }

    private Categoria mapear(QueryDocumentSnapshot doc) {
        return new Categoria(FirestoreUtil.longo(doc.getData(), "id"), doc.getString("nome"), doc.getString("descricao"));
    }
}
