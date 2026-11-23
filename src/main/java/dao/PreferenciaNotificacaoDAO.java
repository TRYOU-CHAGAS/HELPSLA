package dao;

import com.google.cloud.firestore.DocumentSnapshot;
import config.FirestoreFactory;
import model.enums.TipoNotificacao;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class PreferenciaNotificacaoDAO {
    private static final String COLECAO = "preferencias_notificacao";

    public Set<TipoNotificacao> buscar(long usuarioId) {
        try {
            DocumentSnapshot documento = FirestoreFactory.obter().collection(COLECAO)
                    .document(Long.toString(usuarioId)).get().get();
            if (!documento.exists() || documento.get("tipos") == null)
                return EnumSet.allOf(TipoNotificacao.class);
            return ((java.util.List<?>) documento.get("tipos")).stream()
                    .map(Object::toString).map(TipoNotificacao::valueOf).collect(Collectors.toSet());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Não foi possível carregar preferências de notificação.", e);
        } catch (ExecutionException | IllegalArgumentException e) {
            throw new IllegalStateException("Não foi possível carregar preferências de notificação.", e);
        }
    }

    public void salvar(long usuarioId, Set<TipoNotificacao> tipos) {
        try {
            FirestoreFactory.obter().collection(COLECAO).document(Long.toString(usuarioId))
                    .set(java.util.Map.of("tipos", tipos.stream().map(Enum::name).toList(),
                            "atualizadoEm", java.time.Instant.now().toString())).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Não foi possível salvar as preferências de notificação.", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Não foi possível salvar as preferências de notificação.", e);
        }
    }

    public Set<TipoNotificacao> validar(String[] valores) {
        if (valores == null) return EnumSet.allOf(TipoNotificacao.class);
        try {
            return Arrays.stream(valores).map(TipoNotificacao::valueOf).collect(Collectors.toSet());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Uma das opções de notificação é inválida.", e);
        }
    }
}
