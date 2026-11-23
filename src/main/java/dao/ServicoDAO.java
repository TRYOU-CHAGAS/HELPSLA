package dao;

import config.FirestoreFactory;
import config.FirestoreUtil;

public class ServicoDAO {
    public long primeiroAtivoId() {
        try {
            return FirestoreFactory.obter().collection("servicos").whereEqualTo("ativo", true).get().get().getDocuments().stream()
                    .mapToLong(doc -> FirestoreUtil.longo(doc.getData(), "id")).findFirst()
                    .orElseThrow(() -> new IllegalStateException("Cadastre um serviço antes de configurar o SLA."));
        } catch (IllegalStateException e) { throw e; }
        catch (Exception e) { throw new IllegalStateException("Não foi possível consultar os serviços.", e); }
    }

    public String nome(long id) {
        try {
            var documento = FirestoreFactory.obter().collection("servicos").document(Long.toString(id)).get().get();
            return documento.exists() ? documento.getString("nome") : "TI";
        } catch (Exception e) { throw new IllegalStateException("Não foi possível consultar o serviço.", e); }
    }
}
