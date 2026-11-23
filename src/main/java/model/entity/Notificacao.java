package model.entity;

import model.enums.TipoNotificacao;
import java.time.Instant;

public record Notificacao(String id, long destinatarioId, TipoNotificacao tipo,
                          String titulo, String mensagem, Long chamadoId,
                          Instant criadaEm, boolean lida) { }
