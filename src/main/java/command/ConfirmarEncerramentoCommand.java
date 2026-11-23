package command;

import dao.ChamadoDAO;
import exception.RegraNegocioException;
import model.entity.*;
import model.enums.StatusChamado;
import com.google.cloud.firestore.Transaction;

public class ConfirmarEncerramentoCommand implements ChamadoCommand {
    private final ChamadoDAO dao;
    public ConfirmarEncerramentoCommand(ChamadoDAO dao) { this.dao = dao; }
    public String executar(Transaction transacao, Chamado chamado, Usuario ator, String dado) {
        if (chamado.status() != StatusChamado.RESOLVIDO) throw new RegraNegocioException("Só é possível encerrar um chamado resolvido.");
        dao.atualizarStatus(transacao, chamado, StatusChamado.ENCERRADO, ator, null);
        dao.registrarHistorico(transacao, chamado.id(), ator, "ENCERRADO", chamado.status(), StatusChamado.ENCERRADO, "Encerramento confirmado pelo solicitante.");
        return "Chamado encerrado. Você já pode avaliar o atendimento.";
    }
}
