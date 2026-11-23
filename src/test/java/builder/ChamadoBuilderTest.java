package builder;

import exception.RegraNegocioException;
import model.enums.Prioridade;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ChamadoBuilderTest {
    @Test
    void criaDadosComCamposValidos() {
        ChamadoBuilder.DadosAbertura dados = new ChamadoBuilder()
                .titulo("Notebook sem acesso")
                .descricao("O notebook não conecta à rede da empresa.")
                .categoriaId(3L)
                .prioridade(Prioridade.ALTA)
                .build();

        assertEquals("Notebook sem acesso", dados.titulo());
        assertEquals(3, dados.categoriaId());
        assertEquals(Prioridade.ALTA, dados.prioridade());
    }

    @Test
    void rejeitaDescricaoCurta() {
        ChamadoBuilder builder = new ChamadoBuilder().titulo("Erro rede")
                .descricao("curta").categoriaId(3L).prioridade(Prioridade.MEDIA);

        assertThrows(RegraNegocioException.class, builder::build);
    }
}
