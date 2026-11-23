# Arquitetura do HelpSLA

## Stack

- Java 17+ e Jakarta Servlet 6 no Tomcat 10.1.
- HTML, CSS e JavaScript sem framework no navegador.
- Firebase Admin SDK no servidor e Cloud Firestore como banco principal.
- Maven empacota como WAR; BCrypt armazena hashes de senha.

## Fluxo

```text
HTML/CSS/JavaScript
    -> Servlet Controller (MVC e REST)
    -> Service (regras de negócio e autorização)
    -> Command (ações específicas) / DAO (persistência)
    -> Cloud Firestore
```

O navegador nunca usa uma chave administrativa do Firebase. As requisições passam pela API Java, que verifica sessão, CSRF, perfil e vínculo com o chamado antes de usar o Admin SDK.

## Coleções

| Coleção | Campos principais |
|---|---|
| `empresas` | `id`, `nome`, `ativa` |
| `prestadoras` | `id`, `nome`, `ativa` |
| `servicos` | `id`, `nome`, `descricao`, `ativo` |
| `prestadoras_servicos` | `prestadoraId`, `servicoId`, `ativa` |
| `usuarios` | `id`, `empresaId`, `prestadoraId`, `nome`, `email`, `senhaHash`, `perfil`, `ativo` |
| `usuarios_por_email` | índice normalizado de e-mail para `usuarioId` |
| `categorias` | `id`, `servicoId`, `nome`, `descricao`, `ativa` |
| `regras_sla` | `id`, `empresaId`, `servicoId`, `prioridade`, prazos, vigência e autor |
| `chamados` | dados, IDs relacionados, status, prazos e solução |
| `avaliacoes` | um documento por chamado, com solicitante, nota e comentário |
| `preferencias_notificacao` | `tipos`, `atualizadoEm`, por usuário |
| `notificacoes` | destinatário, tipo, mensagem, chamado, data e lida |

Cada documento de `chamados` contém subcoleções `historico` e `mensagens`. Datas são persistidas como strings ISO `LocalDateTime` no MVP e convertidas ao construir os modelos Java.

As coleções são criadas pelo Firestore quando o seed ou o fluxo grava o primeiro documento. A interface do Firebase não precisa de uma etapa para declarar tabelas ou colunas. Datas de prazo usam ISO `LocalDateTime` com a zona `America/Sao_Paulo` de forma consistente no backend.

## Padrões

- **MVC:** separação entre Controller, Service/Model e páginas web.
- **DAO:** cada DAO concentra a persistência Firestore por tipo de dado.
- **Command:** as ações dos chamados são comandos próprios; atualização de estado e evento de histórico são escritos na mesma transação.
- **Factory Method:** `ChamadoCommandFactory` constrói o comando de uma ação.
- **Builder:** `ChamadoBuilder` valida dados da abertura antes de qualquer persistência.
- **Decorator:** `CanalComPreferencias` envolve `CanalNotificacao`, verifica `Set<TipoNotificacao>` e delega ao canal concreto somente quando habilitado. O número de classes não depende das combinações de preferências.

## Configuração de credenciais

O servidor usa `GOOGLE_CLOUD_PROJECT` e Application Default Credentials, normalmente apontadas por `GOOGLE_APPLICATION_CREDENTIALS` em desenvolvimento. `FIREBASE_SERVICE_ACCOUNT_JSON` permite fornecer o JSON como segredo de ambiente numa hospedagem. Chaves não pertencem ao Git. O Admin SDK é privilegiado e as verificações de autorização ficam no backend.

## Limites do MVP

- Uma empresa, uma prestadora de suporte de TI e contas iniciais de demonstração.
- Sem arquivos anexos, pagamentos ou múltiplos domínios de serviço.
- Sem tarefa agendada para alertas prévios de vencimento; as telas mostram contagem regressiva e atraso no momento da consulta.
- A consulta e ordenação por prioridade são dimensionadas para o conjunto pequeno da apresentação. Volume maior exigiria paginação e consultas/indexes Firestore específicos.
