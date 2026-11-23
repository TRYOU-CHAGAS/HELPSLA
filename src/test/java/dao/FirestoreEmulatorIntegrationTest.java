package dao;

import model.entity.Usuario;
import model.enums.Perfil;
import model.enums.TipoNotificacao;
import model.enums.Prioridade;
import model.enums.StatusChamado;
import builder.ChamadoBuilder;
import command.AssumirChamadoCommand;
import config.FirestoreFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.util.EnumSet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FirestoreEmulatorIntegrationTest {
    @Test
    @EnabledIfEnvironmentVariable(named = "FIRESTORE_EMULATOR_HOST", matches = ".+")
    void persistePreferenciasENotificacoesNoEmulador() {
        long id = Math.abs(java.util.concurrent.ThreadLocalRandom.current().nextLong(1, Long.MAX_VALUE));
        PreferenciaNotificacaoDAO preferencias = new PreferenciaNotificacaoDAO();
        preferencias.salvar(id, EnumSet.of(TipoNotificacao.NOVA_MENSAGEM));
        assertEquals(EnumSet.of(TipoNotificacao.NOVA_MENSAGEM), preferencias.buscar(id));

        Usuario usuario = new Usuario(id, 1, null, "Teste", "teste@local", Perfil.SOLICITANTE);
        NotificacaoDAO notificacoes = new NotificacaoDAO();
        var aviso = notificacoes.criar(id, TipoNotificacao.NOVA_MENSAGEM,
                "Mensagem", "Teste do emulador", 101L);
        notificacoes.enviar(usuario, aviso);

        var itens = notificacoes.listar(id);
        assertTrue(itens.stream().anyMatch(item -> "Mensagem".equals(item.get("titulo"))));
        String avisoId = itens.stream().filter(item -> "Mensagem".equals(item.get("titulo")))
                .map(item -> item.get("id").toString()).findFirst().orElseThrow();
        assertTrue(notificacoes.marcarComoLida(id, avisoId));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "FIRESTORE_EMULATOR_HOST", matches = ".+")
    void gravaChamadoEHistoricoEExecutaCommandEmTransacao() throws Exception {
        ChamadoDAO chamados = new ChamadoDAO();
        Usuario solicitante = new Usuario(31, 1, null, "Solicitante teste", "req@local", Perfil.SOLICITANTE);
        Usuario tecnico = new Usuario(32, 1, 3L, "Técnico teste", "tech@local", Perfil.TECNICO);
        ChamadoBuilder.DadosAbertura dados = new ChamadoBuilder().titulo("Notebook sem rede")
                .descricao("O notebook de teste não consegue acessar a rede local.")
                .categoriaId(1L).prioridade(Prioridade.ALTA).build();
        long id = chamados.criarComHistorico("HSL-EMULADOR", solicitante, dados, 3, 4,
                config.FirestoreUtil.agora().plusHours(2), 360, "Computadores");

        FirestoreFactory.obter().runTransaction(transacao -> {
            var chamado = chamados.buscar(transacao, id).orElseThrow();
            return new AssumirChamadoCommand(chamados).executar(transacao, chamado, tecnico, "");
        }).get();

        assertEquals(StatusChamado.EM_ATENDIMENTO, chamados.buscar(id).orElseThrow().status());
        assertTrue(chamados.listarHistorico(id).stream().anyMatch(evento -> "ASSUMIDO".equals(evento.get("acao"))));
    }
}
