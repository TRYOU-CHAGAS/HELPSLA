package config;

import com.google.cloud.firestore.Firestore;
import dao.UsuarioDAO;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import model.enums.Perfil;
import model.enums.Prioridade;
import org.mindrot.jbcrypt.BCrypt;
import java.util.Map;

@WebListener
public class DadosDemonstracaoListener implements ServletContextListener {
    private static final long EMPRESA_ID = 1;
    private static final long PRESTADORA_ID = 1;
    private static final long SERVICO_ID = 1;
    private static final long CATEGORIA_ID = 1;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        event.getServletContext().getSessionCookieConfig().setHttpOnly(true);
        try {
            Firestore firestore = FirestoreFactory.obter();
            if (firestore.collection("usuarios").limit(1).get().get().isEmpty()) criarDados(firestore);
            event.getServletContext().log("HelpSLA iniciou usando Firestore.");
        } catch (Exception e) {
            event.getServletContext().log("HelpSLA não conseguiu conectar ao Firestore. Confira GOOGLE_CLOUD_PROJECT e credenciais.", e);
        }
    }

    private void criarDados(Firestore firestore) {
        gravar(firestore, "empresas", EMPRESA_ID, Map.of("id", EMPRESA_ID,
                "nome", "Empresa Demonstração", "ativa", true));
        gravar(firestore, "prestadoras", PRESTADORA_ID, Map.of("id", PRESTADORA_ID,
                "nome", "Suporte TI Demonstração", "ativa", true));
        gravar(firestore, "servicos", SERVICO_ID, Map.of("id", SERVICO_ID,
                "nome", "Tecnologia da Informação", "descricao", "Suporte técnico de TI", "ativo", true));
        gravar(firestore, "categorias", CATEGORIA_ID, Map.of("id", CATEGORIA_ID, "servicoId", SERVICO_ID,
                "nome", "Computadores e acesso", "descricao", "Computadores, sistemas e acesso à rede", "ativa", true));
        gravar(firestore, "prestadoras_servicos", PRESTADORA_ID + "_" + SERVICO_ID,
                Map.of("prestadoraId", PRESTADORA_ID, "servicoId", SERVICO_ID, "ativa", true));

        String senha = System.getenv().getOrDefault("HELPSLA_DEMO_PASSWORD", "Demo123!");
        UsuarioDAO usuarios = new UsuarioDAO();
        String hash = BCrypt.hashpw(senha, BCrypt.gensalt());
        usuarios.gravarUsuario(10, EMPRESA_ID, null, "Administradora", "admin@helpsla.local", hash, Perfil.ADMIN);
        usuarios.gravarUsuario(11, EMPRESA_ID, null, "Solicitante Demo", "solicitante@helpsla.local", hash, Perfil.SOLICITANTE);
        usuarios.gravarUsuario(12, EMPRESA_ID, PRESTADORA_ID, "Técnico Demo", "tecnico@helpsla.local", hash, Perfil.TECNICO);

        int[] prazosSolucao = {7200, 1440, 360, 30};
        int[] prazosAceite = {1440, 240, 120, 30};
        Prioridade[] prioridades = Prioridade.values();
        for (int i = 0; i < prioridades.length; i++) {
            long id = i + 1;
            gravar(firestore, "regras_sla", id, Map.of("id", id, "empresaId", EMPRESA_ID,
                    "servicoId", SERVICO_ID, "servicoNome", "Tecnologia da Informação",
                    "prioridade", prioridades[i].name(), "prazoMinutos", prazosSolucao[i],
                    "prazoAceiteMinutos", prazosAceite[i], "ativa", true,
                    "criadaPor", 10L, "vigenteDesde", FirestoreUtil.agora().minusDays(1).toString()));
        }
    }

    private void gravar(Firestore firestore, String colecao, Object id, Map<String, Object> dados) {
        FirestoreUtil.aguardar(firestore.collection(colecao).document(id.toString()).set(dados),
                "Não foi possível criar os dados iniciais do HelpSLA.");
    }
}
