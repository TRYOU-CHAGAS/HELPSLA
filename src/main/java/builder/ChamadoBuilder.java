package builder;

import exception.RegraNegocioException;
import model.enums.Prioridade;

public class ChamadoBuilder {
    private String titulo;
    private String descricao;
    private Long categoriaId;
    private Prioridade prioridade;

    public ChamadoBuilder titulo(String valor) { titulo = valor == null ? null : valor.trim(); return this; }
    public ChamadoBuilder descricao(String valor) { descricao = valor == null ? null : valor.trim(); return this; }
    public ChamadoBuilder categoriaId(Long valor) { categoriaId = valor; return this; }
    public ChamadoBuilder prioridade(Prioridade valor) { prioridade = valor; return this; }

    public DadosAbertura build() {
        if (titulo == null || titulo.length() < 5 || titulo.length() > 160) throw new RegraNegocioException("O título deve ter entre 5 e 160 caracteres.");
        if (descricao == null || descricao.length() < 10) throw new RegraNegocioException("A descrição deve ter pelo menos 10 caracteres.");
        if (categoriaId == null || categoriaId <= 0) throw new RegraNegocioException("Selecione uma categoria válida.");
        if (prioridade == null) throw new RegraNegocioException("Selecione a prioridade.");
        return new DadosAbertura(titulo, descricao, categoriaId, prioridade);
    }

    public record DadosAbertura(String titulo, String descricao, long categoriaId, Prioridade prioridade) { }
}
