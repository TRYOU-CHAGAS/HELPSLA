# HelpSLA

MVP acadêmico para abrir e acompanhar chamados de suporte de TI com prazos SLA. O backend usa Java 17, Jakarta Servlets e Firebase Admin SDK; o banco principal é Cloud Firestore. A interface usa HTML, CSS e JavaScript sem framework web.

## Requisitos

- JDK 17 ou superior.
- Apache Tomcat 10.1 ou compatível com Jakarta Servlet 6.
- Projeto Firebase com Cloud Firestore habilitado.
- Credencial de serviço Firebase disponível para o processo Java.

Não é necessário instalar MySQL, iniciar o XAMPP ou rodar scripts SQL para esta versão.

## Configurar Firestore no computador

1. No Firebase Console, crie um projeto e habilite **Cloud Firestore** em modo nativo.
2. Escolha a região do banco com cuidado; ela não pode ser alterada depois.
3. Em **Configurações do projeto > Contas de serviço**, crie uma chave privada para desenvolvimento. Salve o JSON em uma pasta fora do projeto e fora do Git.
4. Antes de iniciar o NetBeans/Tomcat, configure as variáveis no PowerShell (substitua os valores):

   ```powershell
   $env:GOOGLE_CLOUD_PROJECT = "ID_DO_PROJETO_FIREBASE"
   $env:GOOGLE_APPLICATION_CREDENTIALS = "C:\caminho-seguro\firebase-service-account.json"
   ```

   Se o Tomcat for iniciado por outro processo, configure as mesmas variáveis no ambiente desse processo. Para hospedagens sem ADC, o backend também aceita `FIREBASE_SERVICE_ACCOUNT_JSON` como segredo contendo o JSON da conta de serviço.

5. O Admin SDK usa permissões de servidor e não depende das regras de acesso do SDK web. Como o navegador conversa com o backend Java, não habilite acesso público do navegador ao Firestore. Proteja a chave de serviço: quem a possuir pode operar os dados do projeto conforme as permissões IAM dela.

O código cria as coleções e documentos iniciais automaticamente. Não crie tabelas nem campos manualmente no Console.

## Rodar pelo NetBeans/Tomcat

1. Configure `GOOGLE_CLOUD_PROJECT` e as credenciais Firebase antes de iniciar o NetBeans.
2. No NetBeans, abra a pasta deste projeto por **File > Open Project**; escolha a pasta onde está `pom.xml`.
3. Configure um JDK 17+ e registre o Tomcat 10.1 em **Tools > Servers**.
4. Use **Run Project** (F6). O NetBeans compila e publica o WAR.
5. Abra `http://localhost:8080/helpsla/login.html`.

Na primeira inicialização, se a coleção `usuarios` estiver vazia, o listener cria empresa, prestadora, serviço, categoria, relação prestadora-serviço, quatro regras SLA e três usuários de demonstração. As contas usam `Demo123!`, a menos que `HELPSLA_DEMO_PASSWORD` esteja definida antes de iniciar o Tomcat:

- `admin@helpsla.local` — administrador
- `solicitante@helpsla.local` — solicitante
- `tecnico@helpsla.local` — técnico

O seed não é executado quando já encontra usuários. Ele usa IDs conhecidos para que as relações de demonstração sejam consistentes.

## Build e testes

Com Maven instalado (ou pelo Maven do NetBeans), na raiz do projeto:

```text
mvn clean verify
```

O WAR fica em `target/helpsla.war`. Os testes unitários não precisam de credenciais Firebase. Para testar os endpoints e a persistência, configure um projeto de desenvolvimento separado; não use credenciais de produção em testes.

### Testes de integração com o Firestore Emulator

O repositório inclui configuração em `firebase.json` e testes de integração condicionais. Instale o Firebase CLI, inicie apenas o emulador Firestore na raiz do projeto:

```text
firebase emulators:start --only firestore --project demo-helpsla
```

Em outro PowerShell, configure o endpoint local do emulador e rode os testes:

```powershell
$env:FIRESTORE_EMULATOR_HOST = "127.0.0.1:8081"
$env:GOOGLE_CLOUD_PROJECT = "demo-helpsla"
mvn clean verify
```

Sem `FIRESTORE_EMULATOR_HOST`, os testes de integração são ignorados e os unitários continuam executando. O Admin SDK detecta o emulador por essa variável, que deve conter host e porta sem `http://`, de acordo com a [documentação oficial do Firebase](https://firebase.google.com/docs/emulator-suite/connect_firestore). A versão atual do emulador requer Java 21+; o backend do projeto continua compilando para Java 17.

## O que o MVP implementa

- Login, cadastro de solicitante e perfis solicitante, técnico e administrador.
- Abertura de chamado por categoria e prioridade, com cálculo de prazos de aceite e resolução via SLA.
- Fila de chamados, atribuição a técnico, contagem regressiva e indicação de atraso.
- Alteração de status, solução, cancelamento com motivo, encerramento e avaliação.
- Conversa persistente entre solicitante e técnico responsável.
- Histórico de alterações e painel administrativo com categorias, regras SLA e indicadores.
- Preferências de notificação no cadastro e na tela **Notificações**; avisos persistidos no Firestore.

Prioridades e tempos de demonstração: baixa 5 dias, média 24 horas, alta 6 horas e crítica 30 minutos para resolver; aceite baixa 24 horas, média 4 horas, alta 2 horas e crítica 30 minutos. Os prazos usam horas corridas e podem ser alterados pelo administrador.

## Padrões e responsabilidades

- **MVC:** `controller` trata HTTP; `service` concentra o fluxo e regras; `model` representa os dados.
- **DAO:** DAOs encapsulam operações do Firestore.
- **Command:** cada ação de atendimento é um comando próprio e persiste status e histórico dentro da mesma transação Firestore.
- **Factory Method:** `ChamadoCommandFactory` escolhe o comando para a ação solicitada.
- **Builder:** `ChamadoBuilder` monta e valida os dados de abertura.
- **Decorator (sem framework):** `CanalComPreferencias` envolve a interface `CanalNotificacao` e encaminha apenas os tipos escolhidos. As combinações são um `Set<TipoNotificacao>` por pessoa; não há uma subclasse para cada combinação.

Veja [docs/GUIA-APRESENTACAO.md](docs/GUIA-APRESENTACAO.md) para o roteiro de explicação, os fluxos e os exemplos de construtores. A estrutura dos documentos está em [docs/ARQUITETURA.md](docs/ARQUITETURA.md), e os últimos passos de configuração/teste estão em [docs/TASK-MIGRACAO-FIRESTORE.md](docs/TASK-MIGRACAO-FIRESTORE.md). Os SQLs antigos foram arquivados em `docs/legacy-mysql/` para consulta histórica; não são usados pelo sistema.

## API REST

Todas as rotas privadas exigem sessão. Escritas exigem o token CSRF enviado no header `X-CSRF-Token`. As respostas usam JSON.

| Método | Endpoint | Uso/perfil |
|---|---|---|
| POST | `/api/login` | autenticar |
| POST | `/api/cadastro` | criar solicitante e preferências |
| GET | `/api/me` | usuário e token da sessão |
| POST | `/api/logout` | encerrar sessão |
| GET/POST | `/api/chamados` | listar / abrir chamado |
| GET | `/api/chamados/categorias` | categorias disponíveis |
| GET | `/api/chamados/slas` | prazos SLA disponíveis |
| GET | `/api/chamados/{id}` | detalhe, histórico e mensagens autorizados |
| POST | `/api/chamados/{id}/assumir` | técnico assume |
| PATCH | `/api/chamados/{id}/status` | técnico altera status |
| POST | `/api/chamados/{id}/solucao` | técnico registra solução |
| POST | `/api/chamados/{id}/encerrar` | solicitante confirma encerramento |
| POST | `/api/chamados/{id}/cancelar` | solicitante cancela com motivo |
| POST | `/api/chamados/{id}/mensagens` | solicitante/técnico responsável conversa |
| POST | `/api/chamados/{id}/avaliacao` | solicitante avalia chamado encerrado |
| GET/POST | `/api/admin/slas` | administrador consulta/altera SLAs |
| GET/POST | `/api/admin/categorias` | administrador consulta/cria categorias |
| GET | `/api/admin/relatorios` | indicadores do administrador |
| GET | `/api/notificacoes` | avisos do usuário logado |
| PATCH | `/api/notificacoes/{id}` | marcar aviso próprio como lido |
| GET/PUT | `/api/notificacoes/preferencias` | consultar/atualizar preferências |

## Coleções Firestore

O backend cria documentos conforme necessário. As coleções de topo são `empresas`, `prestadoras`, `servicos`, `prestadoras_servicos`, `usuarios`, `usuarios_por_email`, `categorias`, `regras_sla`, `chamados`, `avaliacoes`, `preferencias_notificacao` e `notificacoes`. Cada documento de `chamados` tem subcoleções `historico` e `mensagens`.

O projeto não deve ser descrito como produção: é um MVP de apresentação, sem rotina agendada para avisos automáticos de vencimento do SLA. Dados anteriores do MySQL não são importados automaticamente; as contas e regras de demonstração são criadas no Firestore quando a coleção de usuários está vazia.

## Hospedagem gratuita

O projeto inclui `Dockerfile`, `render.yaml` e um roteiro de publicação no Render em [docs/HOSPEDAGEM-RENDER.md](docs/HOSPEDAGEM-RENDER.md). A plataforma cria uma URL HTTPS gratuita `onrender.com`; serviços grátis podem suspender após inatividade. Nunca publique a chave da conta de serviço Firebase no repositório.
