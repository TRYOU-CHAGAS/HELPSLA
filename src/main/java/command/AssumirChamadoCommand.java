package command;

import dao.ChamadoDAO;
import exception.RegraNegocioException;
import model.entity.*;
import model.enums.StatusChamado;
import com.google.cloud.firestore.Transaction;

public class AssumirChamadoCommand implements ChamadoCommand {
    private final ChamadoDAO dao;
    public AssumirChamadoCommand(ChamadoDAO dao) { this.dao = dao; }
    public String executar(Transaction transacao, Chamado chamado, Usuario ator, String dado) {
        if (chamado.status() != StatusChamado.ABERTO && chamado.status() != StatusChamado.EM_TRIAGEM)
            throw new RegraNegocioException("Este chamado não pode ser assumido no status atual.");
        dao.atualizarStatus(transacao, chamado, StatusChamado.EM_ATENDIMENTO, ator, null);
        dao.registrarHistorico(transacao, chamado.id(), ator, "ASSUMIDO", chamado.status(), StatusChamado.EM_ATENDIMENTO, "Chamado assumido pelo técnico.");
        return "Chamado assumido.";
    }
}
