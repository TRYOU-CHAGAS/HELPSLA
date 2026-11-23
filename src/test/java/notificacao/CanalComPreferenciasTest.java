package notificacao;

import model.entity.Notificacao;
import model.entity.Usuario;
import model.enums.Perfil;
import model.enums.TipoNotificacao;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.*;

class CanalComPreferenciasTest {
    private final CanalNotificacao destino = mock(CanalNotificacao.class);
    private final Usuario usuario = new Usuario(7, 1, null, "Ana", "ana@exemplo.com", Perfil.SOLICITANTE);

    @Test
    void encaminhaQuandoTipoFoiSelecionado() {
        Notificacao alerta = notificacao(TipoNotificacao.NOVA_MENSAGEM);
        CanalComPreferencias canal = new CanalComPreferencias(destino,
                EnumSet.of(TipoNotificacao.NOVA_MENSAGEM));

        canal.enviar(usuario, alerta);

        verify(destino).enviar(usuario, alerta);
    }

    @Test
    void descartaQuandoTipoNaoFoiSelecionado() {
        CanalComPreferencias canal = new CanalComPreferencias(destino,
                EnumSet.of(TipoNotificacao.STATUS_ALTERADO));

        canal.enviar(usuario, notificacao(TipoNotificacao.NOVA_MENSAGEM));

        verifyNoInteractions(destino);
    }

    @Test
    void encaminhaTodosOsTiposQuandoTodosEstaoAtivos() {
        CanalComPreferencias canal = new CanalComPreferencias(destino,
                EnumSet.allOf(TipoNotificacao.class));

        for (TipoNotificacao tipo : TipoNotificacao.values()) canal.enviar(usuario, notificacao(tipo));

        verify(destino, times(TipoNotificacao.values().length)).enviar(eq(usuario), any(Notificacao.class));
    }

    @Test
    void naoEncaminhaNadaQuandoNenhumaPreferenciaEstaAtiva() {
        CanalComPreferencias canal = new CanalComPreferencias(destino, Set.of());

        for (TipoNotificacao tipo : TipoNotificacao.values()) canal.enviar(usuario, notificacao(tipo));

        verifyNoInteractions(destino);
    }

    private Notificacao notificacao(TipoNotificacao tipo) {
        return new Notificacao("n-1", usuario.id(), tipo, "Atualização", "Mensagem nova",
                25L, Instant.parse("2026-01-01T12:00:00Z"), false);
    }
}
