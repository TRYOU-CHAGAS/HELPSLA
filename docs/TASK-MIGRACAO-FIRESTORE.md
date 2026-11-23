# Handoff: concluir a configuração Firestore do HelpSLA

## Objetivo

O código do backend foi migrado para usar Cloud Firestore como persistência principal. Esta lista registra o que ainda depende do Firebase Console e da validação no computador da equipe. O antigo SQL está arquivado para consulta; não é usado pelo runtime.

## O que você precisa fazer no Firebase Console

1. Criar/escolher o projeto Firebase da equipe.
2. Habilitar **Cloud Firestore** em modo nativo e escolher uma localização próxima. A região do banco não pode ser alterada depois.
3. Guardar o Project ID.
4. Gerar uma credencial de conta de serviço para desenvolvimento local e guardar o JSON fora da pasta do projeto. Não enviar chave, senha ou token pelo chat nem para o GitHub.
5. Configurar as variáveis antes de iniciar o NetBeans/Tomcat:

   ```powershell
   $env:GOOGLE_CLOUD_PROJECT = "ID_DO_PROJETO"
   $env:GOOGLE_APPLICATION_CREDENTIALS = "C:\caminho-seguro\service-account.json"
   ```

6. Não criar tabelas, colunas ou coleções manualmente. O listener cria os dados iniciais e as coleções surgem no primeiro `set`/`create` do backend.

## O que o backend grava

Coleções de topo: `empresas`, `prestadoras`, `servicos`, `prestadoras_servicos`, `usuarios`, `usuarios_por_email`, `categorias`, `regras_sla`, `chamados`, `avaliacoes`, `preferencias_notificacao` e `notificacoes`. Cada chamado contém as subcoleções `historico` e `mensagens`.

O primeiro início cria dados de demonstração se `usuarios` estiver vazia. Contas: `admin@helpsla.local`, `solicitante@helpsla.local` e `tecnico@helpsla.local`; senha `Demo123!` por padrão. A variável `HELPSLA_DEMO_PASSWORD` permite trocar essa senha antes da inicialização.

## Checklist para validar na sua máquina

- [ ] Firestore Database aparece no projeto Firebase e o Project ID está correto.
- [ ] Iniciar o Tomcat sem MySQL/XAMPP e conferir o log de conexão/seed.
- [ ] Fazer login com as contas de demonstração.
- [ ] Abrir um chamado; conferir documentos em `chamados` e `chamados/{id}/historico`.
- [ ] Assumir o chamado e alterar status; conferir atualização e evento de histórico.
- [ ] Enviar mensagem; conferir `chamados/{id}/mensagens`.
- [ ] Registrar solução, encerrar, avaliar e conferir `avaliacoes`.
- [ ] Cadastrar/alterar categoria/SLA e conferir as coleções correspondentes.
- [ ] Conferir notificação e preferência em suas coleções.
- [ ] Rodar `mvn clean verify` antes do push.

Se qualquer etapa falhar, anotar o erro do console do Tomcat sem incluir conteúdo da chave JSON. O backend usa Admin SDK, então o acesso ao Firestore é autenticado pela conta de serviço; não habilite acesso público do navegador ao banco.

## Prompt pronto para revisão por outra IA ou integrante

> Revise o projeto HelpSLA após a migração para Firestore. Leia `README.md`, `docs/ARQUITETURA.md`, `docs/GUIA-APRESENTACAO.md` e este arquivo. Procure bugs de persistência, autorização, concorrência, consistência chamado/histórico, avaliação duplicada, preferências e notificações. Confirme que nenhum caminho de runtime usa MySQL/JDBC. Revise especialmente se o seed é seguro/idempotente, se o índice de e-mail impede duplicidade, se o Admin SDK nunca é exposto ao navegador e se a transação Firestore cobre cada escrita relacionada. Revise o padrão Decorator sem framework: deve haver um canal, uma camada base de Decorator e um filtro genérico por `Set<TipoNotificacao>`, sem subclasses para combinações. Confira os construtores usados e se as dependências entram por construtor nos Services. Execute `mvn clean verify`; não use Firestore de produção em testes e não faça commit/push. Liste achados com arquivo/linha e severidade. Se corrigir, rode novamente build e testes. Deixe explícito que validação com Firestore real depende das credenciais locais da equipe.
