package service;

import dao.NotificacaoDAO;
import dao.PreferenciaNotificacaoDAO;
import model.entity.Notificacao;
import model.entity.Usuario;
import notificacao.CanalComPreferencias;
import notificacao.CanalNotificacao;
import java.util.Set;

public class NotificacaoService {
    private final PreferenciaNotificacaoDAO preferencias;
    private final CanalNotificacao canal;

    public NotificacaoService(PreferenciaNotificacaoDAO preferencias, CanalNotificacao canal) {
        this.preferencias = preferencias;
        this.canal = canal;
    }

    public NotificacaoService() {
        this(new PreferenciaNotificacaoDAO(), new NotificacaoDAO());
    }

    public void notificar(Usuario destinatario, Notificacao notificacao) {
        Set<model.enums.TipoNotificacao> tiposAtivos = preferencias.buscar(destinatario.id());
        new CanalComPreferencias(canal, tiposAtivos).enviar(destinatario, notificacao);
    }
}
