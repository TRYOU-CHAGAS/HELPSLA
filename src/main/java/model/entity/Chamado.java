package model.entity;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import model.enums.Prioridade;
import model.enums.StatusChamado;
import java.time.LocalDateTime;

public record Chamado(@JsonSerialize(using = ToStringSerializer.class) long id, String codigo, long empresaId, Long prestadoraId,
                      long solicitanteId, Long tecnicoId, long categoriaId, long regraSlaId,
                      String titulo, String descricao, Prioridade prioridade, StatusChamado status,
                      LocalDateTime abertoEm, LocalDateTime prazoSla, LocalDateTime prazoResolucaoSla,
                      LocalDateTime resolvidoEm,
                      LocalDateTime encerradoEm, String categoriaNome, String solicitanteNome,
                      String tecnicoNome, String solucao, int prazoResolucaoMinutos) {
    public boolean atrasado() {
        if (status == StatusChamado.RESOLVIDO || status == StatusChamado.ENCERRADO || status == StatusChamado.CANCELADO) return false;
        LocalDateTime prazoAtual = switch (status) {
            case ABERTO, EM_TRIAGEM -> prazoSla;
            case EM_ATENDIMENTO, AGUARDANDO_SOLICITANTE -> prazoResolucaoSla;
            default -> prazoSla;
        };
        return prazoAtual != null && LocalDateTime.now(java.time.ZoneId.of("America/Sao_Paulo")).isAfter(prazoAtual);
    }
}
