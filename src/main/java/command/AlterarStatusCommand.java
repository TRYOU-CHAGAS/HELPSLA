package command;

import dao.ChamadoDAO;
import exception.RegraNegocioException;
import model.entity.*;
import model.enums.StatusChamado;
import com.google.cloud.firestore.Transaction;

public class AlterarStatusCommand implements ChamadoCommand {
    private final ChamadoDAO dao;
    public AlterarStatusCommand(ChamadoDAO dao) { this.dao = dao; }
    public String executar(Transaction transacao, Chamado chamado, Usuario ator, String dado) {
        StatusChamado novo;
        try { novo = StatusChamado.valueOf(dado); } catch (Exception e) { throw new RegraNegocioException("Status inválido."); }
        if (novo != StatusChamado.EM_TRIAGEM && novo != StatusChamado.EM_ATENDIMENTO && novo != StatusChamado.AGUARDANDO_SOLICITANTE)
            throw new RegraNegocioException("Use a ação específica para registrar solução ou encerrar o chamado.");
        if (chamado.status() == StatusChamado.RESOLVIDO || chamado.status() == StatusChamado.ENCERRADO || chamado.status() == StatusChamado.CANCELADO)
            throw new RegraNegocioException("Este chamado já foi finalizado.");
        dao.atualizarStatus(transacao, chamado, novo, ator, null);
        dao.registrarHistorico(transacao, chamado.id(), ator, "STATUS_ALTERADO", chamado.status(), novo, null);
        return "Status atualizado.";
    }
}
