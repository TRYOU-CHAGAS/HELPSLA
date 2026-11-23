package notificacao;

import model.entity.Notificacao;
import model.entity.Usuario;
import model.enums.TipoNotificacao;
import java.util.Set;

public final class CanalComPreferencias extends CanalNotificacaoDecorator {
    private final Set<TipoNotificacao> preferencias;

    public CanalComPreferencias(CanalNotificacao canalDecorado, Set<TipoNotificacao> preferencias) {
        super(canalDecorado);
        this.preferencias = Set.copyOf(preferencias);
    }

    @Override
    public void enviar(Usuario destinatario, Notificacao notificacao) {
        if (preferencias.contains(notificacao.tipo())) encaminhar(destinatario, notificacao);
    }
}
