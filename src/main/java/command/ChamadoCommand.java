package command;

import model.entity.Chamado;
import model.entity.Usuario;
import com.google.cloud.firestore.Transaction;

public interface ChamadoCommand {
    String executar(Transaction transacao, Chamado chamado, Usuario ator, String dado);
}
