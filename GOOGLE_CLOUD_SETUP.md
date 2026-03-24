# Configuração do Google Cloud, AI Studio e CLI SDK

Este documento detalha o passo a passo completo para configurar as integrações de Inteligência Artificial (Gemini) e acesso aos serviços do Google (Calendar) necessários para o projeto **Medical Agent**.

---

## 1. Configuração do Google AI Studio (API Key)

Para utilizar o modelo Gemini, você precisa de uma API Key gerada no Google AI Studio. 

### 1.1 Gerar a API Key
1. Acesse o **[Google AI Studio](https://aistudio.google.com/apikey)** e faça login com sua conta Google.
2. Clique no botão **"Create API key"**.
3. Escolha a opção para criar em um novo projeto. O Google vai gerar um projeto automaticamente (ex: `gen-lang-client-123456789`).
4. Copie a chave gerada (ela começa com `AIzaSy...`).
5. Cole essa chave no seu arquivo `src/main/resources/application.yml` na propriedade `ai.openai.api-key`.

### 1.2 Configurar o Plano (Pay-as-you-go)
O "Free Tier" (plano gratuito) do Gemini possui limites rigorosos e bloqueia o modelo após muitas requisições num curto período (Erro 429 - Quota Exceeded). Para usar sem interrupções:

1. Acesse novamente o **[Google AI Studio](https://aistudio.google.com/apikey)**.
2. Na lista de chaves, verifique a coluna **"Quota Tier"**. Deverá constar como "Free tier".
3. Clique no link azul **"Set up billing"** ao lado do "Free tier".
4. Você será redirecionado para o painel de faturamento do Google Cloud.
5. Siga as instruções para vincular ou criar uma nova Conta de Faturamento (Billing Account), inserindo um cartão de crédito.
6. Assim que finalizar, sua API Key passará imediatamente para o modo **Pay-as-you-go** (você só paga pelos tokens que efetivamente utilizar).

---

## 2. Configuração do Google Cloud (Ativar Calendar API)

Para que a Inteligência Artificial consiga visualizar sua disponibilidade e marcar consultas de verdade, é necessário ativar a API do Google Calendar no mesmo projeto.

1. Acesse o **[Google Cloud Console](https://console.cloud.google.com)**.
2. No menu superior esquerdo (ao lado da logo do Google Cloud), certifique-se de que o projeto selecionado é **o mesmo projeto** onde você criou sua API Key no AI Studio (ex: `gen-lang-client-123456789`).
3. No menu lateral esquerdo (ícone de 3 linhas), vá em **"APIs e Serviços"** > **"Biblioteca"** (Library).
4. Na barra de pesquisa, digite **"Google Calendar API"**.
5. Clique no resultado e depois no botão azul **"Habilitar"** (Enable).

---

## 3. Configuração do Google Cloud CLI (Acesso ao Calendar)

Quando construímos aplicações locais que precisam acessar dados pessoais (como o seu Google Agenda), o Google utiliza uma política de segurança ("Secure by Default") que pode bloquear a criação de chaves JSON (Service Accounts). 

A forma mais segura e recomendada pelo Google para o ambiente de desenvolvimento local é utilizar o **Google Cloud CLI (SDK)**.

### 3.1 Instalar o Google Cloud SDK
1. Acesse a página de download oficial ou use o instalador direto para Windows:
   * **[Google Cloud SDK Installer (.exe)](https://dl.google.com/dl/cloudsdk/channels/rapid/GoogleCloudSDKInstaller.exe)**
2. Execute o instalador e siga o fluxo padrão (Next, Agree, Install). 
3. Deixe as opções padrão marcadas.

### 3.2 Autenticar sua Aplicação Local (ADC)
Com o SDK instalado, vamos dizer ao seu computador que ele tem permissão para acessar o seu calendário.

1. Abra o seu terminal (PowerShell, Prompt de Comando ou o terminal do IntelliJ).
2. Digite exatamente o comando abaixo e aperte Enter:
   ```bash
   gcloud auth application-default login --scopes=openid,https://www.googleapis.com/auth/userinfo.email,https://www.googleapis.com/auth/cloud-platform,https://www.googleapis.com/auth/calendar
   ```
3. **O que vai acontecer:**
   * O terminal vai abrir uma nova aba no seu navegador web padrão.
   * O Google pedirá para você fazer login. **Faça login com a mesma conta Google onde estão as agendas médicas.**
   * O Google exibirá um aviso dizendo que o "Google Auth Library" quer acessar sua Conta do Google (ver e editar eventos de todas as suas agendas locais). Clique em **"Continuar"** e depois em **"Permitir"**.
4. Volte para o terminal. Você verá uma mensagem de sucesso indicando que as credenciais extras de aplicativo foram salvas.

### 3.3 Configurar o Projeto de Quota (Billing/Consumo)
Depois de autenticar, o Google Cloud exige saber qual projeto será "cobrado" ou contabilizará as requisições gratuitas que você faz localmente nas APIs (como a do Calendar API). 
Ainda no terminal, rode o comando abaixo substituindo `SEU_ID_DO_PROJETO` pelo nome do seu projeto no Google Cloud (ex: `teste-agente-ia-490205`):
```bash
gcloud auth application-default set-quota-project SEU_ID_DO_PROJETO
```

Você verá a mensagem: *Quota project "seu-projeto" was added to ADC*.

### 3.4 Como funciona? (Importante!)
Você precisou rodar esse comando **apenas uma vez**. O SDK salvou o token de segurança permanente no seu Windows.

* Sempre que você rodar seu projeto (`mvn spring-boot:run`), a biblioteca oficial do Google dentro do seu código Java (`GoogleCredentials.getApplicationDefault()`) vai procurar automaticamente esse arquivo escondido no Windows.
* **Ele funciona magicamente!** Você não precisa configurar variáveis de ambiente e não precisa baixar/colocar arquivos `.json` suspeitos para dentro da sua pasta do projeto.

---

## 4. Testando a Integração Completamente

1. Garanta que o banco de dados MySql esteja rodando (Docker ou Local).
2. Suba a aplicação:
   ```bash
   mvn spring-boot:run
   ```
3. Abra o **Postman** e importe a Collection do projeto.
4. Rode as requisições (Passo 01 para pegar Sessão, Passo 02 para interagir via Chat).
5. Peça à IA para agendar uma consulta: *"Quero agendar uma consulta com o Pediatra amanhã às 14:00"*.
6. O Java irá conversar com o Gemini (via API Key), o Gemini usará a *Tool* de calendário e o Java acessará a API do Calendar via as credenciais seguras do `gcloud`.
