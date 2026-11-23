package dao;

import config.FirestoreFactory;
import config.FirestoreUtil;
import exception.RegraNegocioException;
import java.util.Map;
import com.google.cloud.firestore.Transaction;

public class AvaliacaoDAO {
    public void inserir(Transaction transacao, long chamadoId, long solicitanteId, int nota, String comentario) {
        var ref = config.FirestoreFactory.obter().collection("avaliacoes").document(Long.toString(chamadoId));
        try {
            if (transacao.get(ref).get().exists()) throw new RegraNegocioException("Este chamado já foi avaliado.");
        } catch (RegraNegocioException e) { throw e; }
        catch (Exception e) { throw new IllegalStateException("Não foi possível verificar a avaliação.", e); }
        transacao.create(ref, Map.of("chamadoId", chamadoId, "solicitanteId", solicitanteId,
                "nota", nota, "comentario", comentario == null ? "" : comentario,
                "criadaEm", config.FirestoreUtil.agora().toString()));
    }
}
