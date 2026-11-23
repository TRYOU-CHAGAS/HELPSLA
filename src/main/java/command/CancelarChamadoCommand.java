package command;

import dao.ChamadoDAO;
import exception.RegraNegocioException;
import model.entity.Chamado;
import model.entity.Usuario;
import model.enums.StatusChamado;
import com.google.cloud.firestore.Transaction;

public class CancelarChamadoCommand implements ChamadoCommand {
    private final ChamadoDAO dao;

    public CancelarChamadoCommand(ChamadoDAO dao) { this.dao = dao; }

    @Override
    public String executar(Transaction transacao, Chamado chamado, Usuario ator, String motivo) {
        if (chamado.status() == StatusChamado.RESOLVIDO || chamado.status() == StatusChamado.ENCERRADO
                || chamado.status() == StatusChamado.CANCELADO) {
            throw new RegraNegocioException("Este chamado já foi finalizado e não pode ser cancelado.");
        }
        if (motivo == null || motivo.trim().length() < 5 || motivo.trim().length() > 1000) {
            throw new RegraNegocioException("Informe um motivo com pelo menos 5 caracteres.");
        }
        dao.atualizarStatus(transacao, chamado, StatusChamado.CANCELADO, ator, null);
        dao.registrarHistorico(transacao, chamado.id(), ator, "CANCELADO", chamado.status(), StatusChamado.CANCELADO,
                "Motivo do cancelamento: " + motivo.trim());
        return "Chamado cancelado. O motivo foi registrado no histórico.";
    }
}
