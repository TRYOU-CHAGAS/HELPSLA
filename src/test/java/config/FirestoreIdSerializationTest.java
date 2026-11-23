package config;

import model.entity.Chamado;
import model.enums.Prioridade;
import model.enums.StatusChamado;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FirestoreIdSerializationTest {
    @Test
    void idGrandeDoChamadoVaiComoStringNoJson() throws Exception {
        long idGrande = 9_007_199_254_740_993L;
        LocalDateTime agora = LocalDateTime.of(2026, 11, 23, 12, 0);
        Chamado chamado = new Chamado(idGrande, "HSL-TESTE", 1, 1L, 11, null, 1, 1,
                "Teste de ID", "Descrição de teste", Prioridade.MEDIA, StatusChamado.ABERTO,
                agora, agora.plusHours(4), null, null, null, "Categoria", "Solicitante",
                null, null, 240);

        String json = JsonUtil.MAPPER.writeValueAsString(chamado);

        assertTrue(json.contains("\"id\":\"" + idGrande + "\""), json);
    }

    @Test
    void idsNovosCabemComPrecisaoNoJavaScript() {
        long id = FirestoreUtil.novoId();

        assertTrue(id > 0);
        assertTrue(id <= 9_007_199_254_740_991L);
    }
}
