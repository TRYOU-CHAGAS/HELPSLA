# Guia de apresentação do HelpSLA

## O que o sistema faz

O HelpSLA recebe pedidos de suporte de TI. O solicitante escolhe categoria e prioridade; o servidor consulta o SLA e calcula os prazos. Um técnico assume o chamado, acompanha o tempo e registra a solução. O solicitante confirma o encerramento e pode avaliar.

## Como explicar o caminho de uma operação

### Login

1. `login.js` envia e-mail e senha para `POST /api/login`.
2. `LoginServlet` chama `AutenticacaoService.autenticar(...)`.
3. `UsuarioDAO` procura o usuário na coleção `usuarios` do Firestore e retorna o hash BCrypt.
4. O serviço compara a senha digitada com o hash. Se combinar, o servlet cria uma sessão HTTP e um token CSRF.
5. `AutenticacaoFilter` verifica sessão e token nas operações que mudam dados.

### Abrir chamado

1. A tela envia título, descrição, categoria e prioridade para `POST /api/chamados`.
2. `ChamadoServlet` encaminha os dados a `ChamadoService.abrir(...)`.
3. `ChamadoBuilder` valida os dados e monta `DadosAbertura`.
4. O serviço consulta categoria, prestadora e regra SLA. `ChamadoDAO` grava o chamado e o primeiro item de histórico no Firestore usando um `WriteBatch`.
5. O sistema tenta avisar os técnicos da prestadora; o Decorator encaminha só os tipos que cada técnico habilitou.

### Alterar o chamado

`ChamadoCommandFactory` escolhe um Command, por exemplo `AssumirChamadoCommand`. `ChamadoService` inicia uma transação Firestore, carrega o estado atual do chamado e executa o Command. O Command escreve o novo estado e o evento de histórico na mesma transação. Assim, as duas alterações são confirmadas juntas.

### Notificações

`NotificacaoService` monta `CanalComPreferencias` ao redor de um `CanalNotificacao`. Se o tipo estiver no `Set<TipoNotificacao>` do usuário, o canal concreto `NotificacaoDAO` persiste o aviso no Firestore; caso contrário, o Decorator não encaminha. Não existe uma classe para cada combinação de opções.

Os eventos atuais são: novo chamado, técnico assumiu, mudança de status, solução registrada e nova mensagem. Alertas automáticos disparados por um relógio quando o SLA está perto de vencer ainda não fazem parte do MVP.

## Construtores que você pode apontar

Um construtor cria e inicializa um objeto. O projeto usa construtores em pontos concretos do fluxo:

- `ChamadoService()` cria as dependências Firestore padrão e delega ao construtor com parâmetros. Esse outro construtor recebe DAOs e serviços e os guarda nos campos `final`; isso também permite injetar mocks nos testes.
- `new ChamadoCommandFactory(chamadoDAO)` recebe o DAO para que os Commands usem a mesma persistência.
- `new ChamadoBuilder()` usa o construtor sem argumentos implícito do Java. O serviço chama os métodos de configuração e depois `build()`; o objeto gerado é usado para abrir o chamado.
- `new CanalComPreferencias(canal, tiposAtivos)` injeta o canal interno e as escolhas do usuário no Decorator. O `NotificacaoService` usa imediatamente esse objeto para decidir se chama `enviar()`.
- `new Usuario(...)` e `new Notificacao(...)` usam construtores canônicos gerados pelo Java para `record`s.
- `FirestoreFactory` tem construtor privado porque é um ponto de acesso estático compartilhado; ninguém deve criar instâncias dela.

## Padrões no projeto

- **MVC:** `controller` recebe HTTP; `service` executa o fluxo e as regras; `model` contém os dados.
- **DAO:** `UsuarioDAO`, `ChamadoDAO`, `RegraSlaDAO` e os demais DAOs isolam operações Firestore.
- **Command:** cada ação do atendimento tem uma classe própria, como `AssumirChamadoCommand` e `AlterarStatusCommand`.
- **Factory Method:** `ChamadoCommandFactory.criar(acao)` escolhe qual Command executar.
- **Builder:** `ChamadoBuilder` valida e monta dados da abertura em etapas.
- **Decorator:** `CanalComPreferencias` adiciona o filtro de preferência ao canal sem mudar `NotificacaoDAO` e sem usar framework.

## SOLID e Calistenia de Objetos

Servlets tratam HTTP; services coordenam regras; DAOs persistem; Commands executam ações específicas. As dependências principais de `ChamadoService`, `AdminService` e `AutenticacaoService` entram por construtor, permitindo substituí-las por mocks em testes. Os DAOs dependem do Firestore e não contêm regra de permissão. O código mantém métodos focados e evita profundidade desnecessária; Calistenia é aplicada de forma pragmática, sem criar classes artificiais para cada campo.

## Pastas principais

| Pasta | Conteúdo |
|---|---|
| `controller` | Servlets e rotas REST |
| `service` | Fluxos de autenticação, chamados, administração e notificações |
| `dao` | Leitura e gravação das coleções Firestore |
| `model/entity` | Records de usuário, chamado, SLA, categoria e notificação |
| `model/enums` | Perfis, status, prioridades e tipos de aviso |
| `command` | Interface, Commands e fábrica |
| `builder` | Builder de abertura do chamado |
| `notificacao` | Canal, Decorator base e filtro por preferência |
| `config` | Firebase/Firestore, serialização JSON e seed inicial |
| `filter` | Sessão, autorização geral e proteção CSRF |
| `src/main/webapp` | HTML, CSS e JavaScript do navegador |
| `src/test` | Testes unitários |

## Demonstração sugerida

1. Entre como `solicitante@helpsla.local` e abra um chamado.
2. Entre como `tecnico@helpsla.local`, assuma-o e mude o status; mostre o prazo e o histórico.
3. Registre a solução e volte como solicitante para encerrar e avaliar.
4. Mostre as preferências e a tela de notificações.

As contas de demonstração usam `Demo123!`, a menos que `HELPSLA_DEMO_PASSWORD` tenha sido configurada.

## Perguntas comuns

- **Por que Decorator?** Para aplicar a regra de preferência antes de chegar ao canal de persistência.
- **Onde ele é usado?** `NotificacaoService.notificar()` cria `CanalComPreferencias` e chama `enviar()`.
- **Como evitou a explosão de classes?** As combinações ficam num conjunto de tipos por usuário; um único Decorator trata qualquer conjunto.
- **Como o histórico fica consistente?** O Command atualiza o chamado e cria o evento de histórico dentro de uma transação Firestore.
- **Onde fica o banco?** No Cloud Firestore. O backend Java usa a conta de serviço e o navegador chama a API Java, não o Firestore diretamente.

## Testes

Execute `mvn clean verify`. Os testes atuais cobrem montagem/validação do Builder e passagem de notificações com todas, nenhuma ou parte das preferências. As chamadas ao Firestore real ainda precisam de uma configuração de desenvolvimento para teste de integração.
