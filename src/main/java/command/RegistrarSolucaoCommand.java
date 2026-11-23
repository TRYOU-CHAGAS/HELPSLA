package command;

import dao.ChamadoDAO;
import exception.RegraNegocioException;
import model.entity.*;
import model.enums.StatusChamado;
import com.google.cloud.firestore.Transaction;

public class RegistrarSolucaoCommand implements ChamadoCommand {
    private final ChamadoDAO dao;
    public RegistrarSolucaoCommand(ChamadoDAO dao) { this.dao = dao; }
    public String executar(Transaction transacao, Chamado chamado, Usuario ator, String dado) {
        if (dado == null || dado.trim().length() < 5) throw new RegraNegocioException("Descreva a solução (mínimo de 5 caracteres).");
        if (chamado.status() != StatusChamado.EM_ATENDIMENTO && chamado.status() != StatusChamado.AGUARDANDO_SOLICITANTE)
            throw new RegraNegocioException("Assuma o chamado antes de registrar a solução.");
        dao.atualizarStatus(transacao, chamado, StatusChamado.RESOLVIDO, ator, dado.trim());
        dao.registrarHistorico(transacao, chamado.id(), ator, "SOLUCAO_REGISTRADA", chamado.status(), StatusChamado.RESOLVIDO, dado.trim());
        return "Solução registrada. Aguarde a confirmação do solicitante.";
    }
}
