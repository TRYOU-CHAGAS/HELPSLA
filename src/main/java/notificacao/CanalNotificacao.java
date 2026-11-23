package notificacao;

import model.entity.Notificacao;
import model.entity.Usuario;

public interface CanalNotificacao {
    void enviar(Usuario destinatario, Notificacao notificacao);
}
