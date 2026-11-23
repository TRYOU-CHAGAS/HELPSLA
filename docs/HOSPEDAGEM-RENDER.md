# Publicar o HelpSLA gratuitamente no Render

Este projeto roda como uma aplicação Java Servlet no Tomcat e usa o Firestore. A configuração de Docker permite que o Render construa o WAR e inicie o Tomcat com a porta que a plataforma fornecer.

## Enviar o código ao GitHub

1. No GitHub, crie um repositório vazio, sem README, licença ou `.gitignore` inicial. Copie o endereço HTTPS do repositório.
2. Abra o PowerShell na pasta do projeto:

   ```powershell
   cd "$env:USERPROFILE\Documents\ChatGPT\HelpSLA 2"
   ```

3. Configure o nome/e-mail que quer exibir nos commits (faça isso uma vez):

   ```powershell
   git config user.name "SEU NOME"
   git config user.email "SEU EMAIL DO GITHUB"
   ```

4. Confira os arquivos antes de publicar. A chave JSON do Firebase não pode aparecer nessa lista:

   ```powershell
   git add -A
   git status --short
   ```

5. Crie o commit inicial e envie para o repositório vazio (troque a URL):

   ```powershell
   git commit -m "Prepare HelpSLA for deployment"
   git branch -M main
   git remote add origin https://github.com/SEU-USUARIO/SEU-REPOSITORIO.git
   git push -u origin main
   ```

## Antes da publicação

1. Para uma demonstração pública, crie um projeto Firebase separado do usado nos testes locais. Assim, os dados da apresentação não se misturam com os dados atuais. O Admin SDK tem acesso privilegiado ao Firestore, portanto a chave da conta de serviço deve ficar só nos segredos do Render.
2. No Firebase Console, crie/ative o Cloud Firestore nesse projeto e gere uma chave de conta de serviço em **Configurações do projeto > Contas de serviço > Gerar nova chave privada**. Não salve o JSON no projeto nem no Git.

## Criar o serviço

1. No Render, escolha **New > Blueprint** e conecte o repositório GitHub que você acabou de enviar.
2. O Render encontrará `render.yaml` e preparará um Web Service Docker no plano Free.
3. Quando pedir as variáveis, preencha `GOOGLE_CLOUD_PROJECT` com o ID do projeto Firebase (campo `project_id` dentro do JSON).
4. Preencha `FIREBASE_SERVICE_ACCOUNT_JSON` com o conteúdo completo do JSON da conta de serviço. Faça isso no campo secreto do Render; não cole o JSON em arquivos do repositório ou no chat.
5. Configure `HELPSLA_DEMO_PASSWORD` com uma senha temporária forte para as contas demonstrativas criadas no Firestore vazio. Essa senha é usada somente na criação inicial; ela não altera contas que já existam.
6. Confirme o deploy. O Render compila o projeto com Maven, gera o WAR, instala-o como aplicação raiz do Tomcat e fornece um endereço HTTPS `*.onrender.com`.

O Blueprint solicita os três valores necessários ao conectar o serviço, sem gravar IDs de projeto ou segredos no código. O serviço usa a variável `PORT` do Render e escuta em `0.0.0.0` pelo conector padrão do Tomcat.

## Limitações do plano gratuito

- O serviço pode suspender após 15 minutos sem tráfego e levar aproximadamente um minuto para voltar na próxima visita.
- O Render pode reiniciar instâncias gratuitas. O app não guarda dados locais; chamados e contas ficam no Firestore.
- O plano é adequado para demonstração acadêmica, não para operação de produção.
- O endereço `onrender.com` é fornecido sem comprar domínio.

## Atualizações

Com o deploy automático habilitado, novos commits na branch conectada ao Render iniciam um novo build e publicação. Nunca envie credenciais, arquivos de conta de serviço ou segredos ao Git.
