package dao;

import config.FirestoreFactory;
import config.FirestoreUtil;
import model.enums.Prioridade;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RegraSlaDAO {
    public record Regra(long id, int prazoMinutos, int prazoAceiteMinutos) { }

    public Optional<Regra> buscarAtiva(long empresaId, long servicoId, Prioridade prioridade) {
        return consultar(empresaId).stream().filter(this::vigente)
                .filter(d -> FirestoreUtil.longo(d, "servicoId") == servicoId)
                .filter(d -> prioridade.name().equals(d.get("prioridade")))
                .max(Comparator.comparing(d -> FirestoreUtil.data(d.get("vigenteDesde"))))
                .map(d -> new Regra(FirestoreUtil.longo(d, "id"), FirestoreUtil.inteiro(d, "prazoMinutos"),
                        FirestoreUtil.inteiro(d, "prazoAceiteMinutos")));
    }

    public List<Map<String, Object>> listar(long empresaId) {
        List<Map<String, Object>> itens = new ArrayList<>();
        for (Map<String, Object> regra : consultar(empresaId)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", FirestoreUtil.longo(regra, "id")); item.put("servico", regra.getOrDefault("servicoNome", "TI"));
            item.put("prioridade", regra.get("prioridade")); item.put("prazoMinutos", FirestoreUtil.inteiro(regra, "prazoMinutos"));
            item.put("prazoAceiteMinutos", FirestoreUtil.inteiro(regra, "prazoAceiteMinutos"));
            item.put("ativa", vigente(regra)); itens.add(item);
        }
        itens.sort(Comparator.comparingInt(m -> Prioridade.valueOf(m.get("prioridade").toString()).ordinal()));
        return itens;
    }

    public List<Map<String, Object>> listarAtivas(long empresaId) {
        return listar(empresaId).stream().filter(item -> Boolean.TRUE.equals(item.get("ativa"))).toList();
    }

    public long atualizarRegra(long empresaId, long servicoId, String servicoNome, Prioridade prioridade,
                               int prazoMinutos, int prazoAceiteMinutos, long adminId) {
        var db = FirestoreFactory.obter();
        var lote = db.batch();
        LocalDateTime agora = FirestoreUtil.agora();
        try {
            for (var doc : db.collection("regras_sla").whereEqualTo("empresaId", empresaId).get().get().getDocuments()) {
                Map<String, Object> dados = doc.getData();
                if (FirestoreUtil.longo(dados, "empresaId") == empresaId
                        && FirestoreUtil.longo(dados, "servicoId") == servicoId
                        && prioridade.name().equals(dados.get("prioridade")) && vigente(dados))
                    lote.update(doc.getReference(), "ativa", false, "vigenteAte", agora.toString());
            }
            long id = FirestoreUtil.novoId();
            Map<String, Object> dados = new LinkedHashMap<>();
            dados.put("id", id); dados.put("empresaId", empresaId); dados.put("servicoId", servicoId);
            dados.put("servicoNome", servicoNome); dados.put("prioridade", prioridade.name());
            dados.put("prazoMinutos", prazoMinutos); dados.put("prazoAceiteMinutos", prazoAceiteMinutos);
            dados.put("ativa", true); dados.put("criadaPor", adminId); dados.put("vigenteDesde", agora.toString());
            lote.set(db.collection("regras_sla").document(Long.toString(id)), dados);
            FirestoreUtil.aguardar(lote.commit(), "Não foi possível salvar a regra de SLA.");
            return id;
        } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException("Atualização de SLA interrompida.", e); }
        catch (Exception e) { throw new IllegalStateException("Não foi possível salvar a regra de SLA.", e); }
    }

    private List<Map<String, Object>> consultar(long empresaId) {
        try { return FirestoreFactory.obter().collection("regras_sla").whereEqualTo("empresaId", empresaId)
                .get().get().getDocuments().stream().map(d -> d.getData()).toList(); }
        catch (Exception e) { throw new IllegalStateException("Não foi possível consultar as regras SLA.", e); }
    }

    private boolean vigente(Map<String, Object> dados) {
        if (!Boolean.TRUE.equals(dados.get("ativa"))) return false;
        LocalDateTime agora = FirestoreUtil.agora();
        LocalDateTime inicio = FirestoreUtil.data(dados.get("vigenteDesde"));
        LocalDateTime fim = FirestoreUtil.data(dados.get("vigenteAte"));
        return (inicio == null || !inicio.isAfter(agora)) && (fim == null || fim.isAfter(agora));
    }
}
