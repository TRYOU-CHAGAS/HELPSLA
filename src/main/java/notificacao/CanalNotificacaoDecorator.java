package notificacao;

import model.entity.Notificacao;
import model.entity.Usuario;
import java.util.Objects;

public abstract class CanalNotificacaoDecorator implements CanalNotificacao {
    private final CanalNotificacao canalDecorado;

    protected CanalNotificacaoDecorator(CanalNotificacao canalDecorado) {
        this.canalDecorado = Objects.requireNonNull(canalDecorado);
    }

    protected final void encaminhar(Usuario destinatario, Notificacao notificacao) {
        canalDecorado.enviar(destinatario, notificacao);
    }
}
