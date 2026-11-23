package command;

import com.google.cloud.firestore.Transaction;
import dao.ChamadoDAO;
import exception.RegraNegocioException;
import model.entity.Chamado;
import model.entity.Usuario;
import model.enums.Perfil;
import model.enums.Prioridade;
import model.enums.StatusChamado;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class AssumirChamadoCommandTest {
    private final ChamadoDAO dao = mock(ChamadoDAO.class);
    private final Transaction transacao = mock(Transaction.class);
    private final AssumirChamadoCommand command = new AssumirChamadoCommand(dao);
    private final Usuario tecnico = new Usuario(12, 1, 3L, "Técnico", "tecnico@exemplo.com", Perfil.TECNICO);

    @Test
    void assumirGravaEstadoEHistoricoNaTransacao() {
        Chamado chamado = chamado(StatusChamado.ABERTO);

        String resultado = command.executar(transacao, chamado, tecnico, "");

        assertEquals("Chamado assumido.", resultado);
        verify(dao).atualizarStatus(transacao, chamado, StatusChamado.EM_ATENDIMENTO, tecnico, null);
        verify(dao).registrarHistorico(transacao, chamado.id(), tecnico, "ASSUMIDO", StatusChamado.ABERTO,
                StatusChamado.EM_ATENDIMENTO, "Chamado assumido pelo técnico.");
    }

    @Test
    void naoAlteraChamadoQueJaFoiResolvido() {
        assertThrows(RegraNegocioException.class,
                () -> command.executar(transacao, chamado(StatusChamado.RESOLVIDO), tecnico, ""));
        verifyNoInteractions(dao);
    }

    private Chamado chamado(StatusChamado status) {
        LocalDateTime agora = LocalDateTime.of(2026, 11, 22, 12, 0);
        return new Chamado(20, "HSL-12345678", 1, 3L, 11, null, 1, 1,
                "Sem acesso", "Não consigo acessar a rede", Prioridade.ALTA, status,
                agora, agora.plusHours(2), null, null, null, "Acesso", "Solicitante", null, null, 360);
    }
}
