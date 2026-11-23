package config;

import com.google.api.core.ApiFuture;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public final class FirestoreUtil {
    private FirestoreUtil() { }

    public static <T> T aguardar(ApiFuture<T> operacao, String mensagem) {
        try { return operacao.get(); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(mensagem, e); }
        catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException erro) throw erro;
            throw new IllegalStateException(mensagem, e.getCause());
        }
    }

    // Keep generated IDs exactly representable by JavaScript Number (2^53 - 1).
    public static long novoId() { return java.util.concurrent.ThreadLocalRandom.current().nextLong(1, 9_007_199_254_740_992L); }

    public static LocalDateTime agora() { return LocalDateTime.now(ZoneId.of("America/Sao_Paulo")); }

    public static long longo(Map<String, Object> dados, String campo) {
        Object valor = dados.get(campo);
        return valor == null ? 0 : ((Number) valor).longValue();
    }

    public static Long longoOpcional(Map<String, Object> dados, String campo) {
        Object valor = dados.get(campo);
        return valor == null ? null : ((Number) valor).longValue();
    }

    public static int inteiro(Map<String, Object> dados, String campo) {
        Object valor = dados.get(campo);
        return valor == null ? 0 : ((Number) valor).intValue();
    }

    public static LocalDateTime data(Object valor) {
        return valor == null ? null : LocalDateTime.parse(valor.toString());
    }

    public static Object data(LocalDateTime valor) { return valor == null ? null : valor.toString(); }
}
