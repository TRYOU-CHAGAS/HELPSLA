package dao;

import config.FirestoreFactory;
import model.entity.Notificacao;
import model.entity.Usuario;
import model.enums.TipoNotificacao;
import notificacao.CanalNotificacao;
import java.time.Instant;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import java.util.concurrent.ExecutionException;

public class NotificacaoDAO implements CanalNotificacao {
    @Override
    public void enviar(Usuario destinatario, Notificacao notificacao) {
        Notificacao persistida = notificacao.id() == null
                ? new Notificacao(UUID.randomUUID().toString(), destinatario.id(), notificacao.tipo(),
                notificacao.titulo(), notificacao.mensagem(), notificacao.chamadoId(), Instant.now(), false)
                : notificacao;
        try {
            FirestoreFactory.obter().collection("notificacoes").document(persistida.id())
                    .set(java.util.Map.of("destinatarioId", persistida.destinatarioId(),
                            "tipo", persistida.tipo().name(), "titulo", persistida.titulo(),
                            "mensagem", persistida.mensagem(), "chamadoId", persistida.chamadoId() == null ? "" : persistida.chamadoId().toString(),
                            "criadaEm", persistida.criadaEm().toString(), "lida", false)).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Não foi possível salvar a notificação.", e);
        } catch (Exception e) {
            throw new IllegalStateException("Não foi possível salvar a notificação.", e);
        }
    }

    public Notificacao criar(long usuarioId, TipoNotificacao tipo, String titulo,
                             String mensagem, Long chamadoId) {
        return new Notificacao(null, usuarioId, tipo, titulo, mensagem, chamadoId, Instant.now(), false);
    }

    public List<Map<String, Object>> listar(long usuarioId) {
        try {
            List<QueryDocumentSnapshot> documentos = FirestoreFactory.obter().collection("notificacoes")
                    .whereEqualTo("destinatarioId", usuarioId).get().get().getDocuments();
            List<Map<String, Object>> resultado = new ArrayList<>();
            for (QueryDocumentSnapshot documento : documentos) {
                resultado.add(Map.of("id", documento.getId(), "tipo", documento.getString("tipo"),
                        "titulo", documento.getString("titulo"), "mensagem", documento.getString("mensagem"),
                        "chamadoId", documento.getString("chamadoId"),
                        "criadaEm", documento.getString("criadaEm"), "lida", Boolean.TRUE.equals(documento.getBoolean("lida"))));
            }
            return resultado;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Não foi possível carregar as notificações.", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Não foi possível carregar as notificações.", e);
        }
    }

    public boolean marcarComoLida(long usuarioId, String notificacaoId) {
        try {
            var referencia = FirestoreFactory.obter().collection("notificacoes").document(notificacaoId);
            var documento = referencia.get().get();
            if (!documento.exists() || !Long.valueOf(usuarioId).equals(documento.getLong("destinatarioId"))) return false;
            referencia.update("lida", true).get();
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Não foi possível atualizar a notificação.", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Não foi possível atualizar a notificação.", e);
        }
    }
}
