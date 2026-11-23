package dao;

import config.FirestoreFactory;
import config.FirestoreUtil;

public class PrestadoraDAO {
    public long buscarAtivaParaServico(long servicoId) {
        try {
            return FirestoreFactory.obter().collection("prestadoras_servicos").whereEqualTo("servicoId", servicoId).get().get().getDocuments().stream()
                    .filter(doc -> Boolean.TRUE.equals(doc.getBoolean("ativa")))
                    .mapToLong(doc -> FirestoreUtil.longo(doc.getData(), "prestadoraId")).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Não há prestadora ativa para esse serviço."));
        } catch (IllegalArgumentException e) { throw e; }
        catch (Exception e) { throw new IllegalStateException("Não foi possível consultar a prestadora.", e); }
    }
}
