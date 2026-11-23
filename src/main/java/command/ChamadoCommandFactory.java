package command;

import dao.ChamadoDAO;
import exception.RegraNegocioException;

public class ChamadoCommandFactory {
    private final ChamadoDAO dao;
    public ChamadoCommandFactory(ChamadoDAO dao) { this.dao = dao; }
    public ChamadoCommand criar(String acao) {
        return switch (acao) {
            case "assumir" -> new AssumirChamadoCommand(dao);
            case "status" -> new AlterarStatusCommand(dao);
            case "solucao" -> new RegistrarSolucaoCommand(dao);
            case "encerrar" -> new ConfirmarEncerramentoCommand(dao);
            case "cancelar" -> new CancelarChamadoCommand(dao);
            default -> throw new RegraNegocioException("Ação de chamado desconhecida.");
        };
    }
}
