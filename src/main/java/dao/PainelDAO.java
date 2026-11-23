package dao;

import config.FirestoreFactory;
import config.FirestoreUtil;
import model.enums.StatusChamado;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class PainelDAO {
    public Map<String, Long> contar(long empresaId) {
        long abertos = 0, resolvidos = 0, atrasados = 0;
        try {
            for (var doc : FirestoreFactory.obter().collection("chamados").whereEqualTo("empresaId", empresaId).get().get().getDocuments()) {
                Map<String, Object> dados = doc.getData();
                StatusChamado status = StatusChamado.valueOf((String) dados.get("status"));
                if (status == StatusChamado.ABERTO || status == StatusChamado.EM_TRIAGEM
                        || status == StatusChamado.EM_ATENDIMENTO || status == StatusChamado.AGUARDANDO_SOLICITANTE) abertos++;
                if (status == StatusChamado.RESOLVIDO || status == StatusChamado.ENCERRADO) resolvidos++;
                if (estaAtrasado(dados, status)) atrasados++;
            }
        } catch (Exception e) { throw new IllegalStateException("Não foi possível gerar o relatório.", e); }
        Map<String, Long> resultado = new LinkedHashMap<>();
        resultado.put("abertos", abertos); resultado.put("resolvidos", resolvidos); resultado.put("atrasados", atrasados);
        return resultado;
    }

    private boolean estaAtrasado(Map<String, Object> dados, StatusChamado status) {
        if (status == StatusChamado.RESOLVIDO || status == StatusChamado.ENCERRADO || status == StatusChamado.CANCELADO) return false;
        String campo = status == StatusChamado.EM_ATENDIMENTO || status == StatusChamado.AGUARDANDO_SOLICITANTE
                ? "prazoResolucaoSla" : "prazoSla";
        LocalDateTime prazo = FirestoreUtil.data(dados.get(campo));
        return prazo != null && config.FirestoreUtil.agora().isAfter(prazo);
    }
}
