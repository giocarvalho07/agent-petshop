# Guia Completo de Setup — Medical Agent (Spring Boot)

Este guia documenta **todo o processo** para configurar e executar o projeto do agente autônomo de agendamento médico.

---

## 📋 Pré-requisitos

| Ferramenta | Versão Mínima | Verificação |
|---|---|---|
| Java JDK | 17+ | `java -version` |
| Maven | 3.8+ | `mvn -version` |
| MySQL | 8.0+ | `mysql --version` |

---

## 1. Obter a API Key do Google Gemini

A API Key é necessária para que o agente converse com a inteligência artificial do Google (Gemini 2.0 Flash).

### Passo a passo:

1. Acesse o **Google AI Studio**: [https://aistudio.google.com/apikey](https://aistudio.google.com/apikey)

2. Faça login com sua **conta Google** (qualquer conta pessoal ou corporativa funciona)

3. Na página de API Keys, clique no botão **"Create API Key"**

4. Selecione um projeto do Google Cloud existente ou deixe o AI Studio criar um automaticamente

5. A chave será gerada e exibida na tela. Ela terá o formato:
   ```
   AIzaSy...xxxxx
   ```

6. **Copie a chave** e guarde em um local seguro

> ⚠️ **IMPORTANTE:** Nunca compartilhe sua API Key publicamente (em repositórios Git, chats, etc.). Se acidentalmente expor a chave, acesse o AI Studio e regenere uma nova imediatamente.

### Verificação (opcional):

Você pode testar se a chave funciona fazendo uma chamada direta no PowerShell:

```powershell
$headers = @{ "Content-Type" = "application/json" }
$body = '{"contents":[{"parts":[{"text":"Diga olá em uma frase"}]}]}'
Invoke-RestMethod -Uri "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=SUA_API_KEY_AQUI" -Method POST -Headers $headers -Body $body
```

Se retornar uma resposta JSON com texto, a chave está funcionando. ✅

---

## 2. Instalar e Configurar o MySQL

O MySQL é o banco de dados que armazena as informações dos médicos da clínica.

### Opção A: Via Docker (recomendado — mais rápido)

Se você tem o **Docker Desktop** instalado:

```powershell
docker run --name mysql-agent `
  -e MYSQL_ROOT_PASSWORD=root123 `
  -e MYSQL_DATABASE=medical_agent `
  -p 3306:3306 `
  -d mysql:8
```

Sua senha será: `root123`

Para verificar se está rodando:
```powershell
docker ps
```

### Opção B: Instalação manual

1. Acesse: [https://dev.mysql.com/downloads/installer/](https://dev.mysql.com/downloads/installer/)

2. Baixe o **MySQL Installer for Windows**

3. Execute o instalador e selecione **"Developer Default"**

4. Siga o assistente de instalação. Na etapa de configuração do servidor:
    - **Authentication Method:** Use Strong Password Encryption
    - **Root Password:** Defina uma senha e **anote-a** (você vai precisar)
    - **Windows Service:** Deixe marcado para iniciar automaticamente

5. Finalize a instalação

6. Abra o **MySQL Workbench** (instalado junto) ou o terminal MySQL e crie o banco:
   ```sql
   CREATE DATABASE IF NOT EXISTS medical_agent;
   ```

> ℹ️ **Nota:** Não é necessário criar tabelas manualmente. O **Flyway** (integrado no projeto) criará as tabelas e inserirá os dados iniciais automaticamente na primeira execução.

---

## 3. Instalar o Java 17+ e Maven

### Java JDK 17

1. Acesse: [https://adoptium.net/](https://adoptium.net/) (Eclipse Temurin — recomendado)
2. Baixe o instalador para **Windows x64** com JDK 17 ou superior
3. Execute o instalador marcando a opção **"Set JAVA_HOME variable"**
4. Verifique:
   ```powershell
   java -version
   ```
   Deve exibir algo como: `openjdk version "17.x.x"`

### Maven

1. Acesse: [https://maven.apache.org/download.cgi](https://maven.apache.org/download.cgi)
2. Baixe o arquivo **Binary zip archive** (`apache-maven-x.x.x-bin.zip`)
3. Extraia para `C:\Program Files\Apache\maven`
4. Adicione ao PATH do sistema:
    - Abra **"Editar variáveis de ambiente do sistema"**
    - Em **Path**, adicione: `C:\Program Files\Apache\maven\bin`
5. Verifique:
   ```powershell
   mvn -version
   ```

---

## 4. Configurar Variáveis de Ambiente

Antes de executar o projeto, defina as variáveis no PowerShell:

```powershell
# Senha do MySQL (a que você definiu na instalação)
$env:MYSQL_PASSWORD = "root123"

# API Key do Gemini (obtida no Passo 1)
$env:GEMINI_API_KEY = "cole_sua_api_key_aqui"
```

### Tornar permanente (opcional):

Para não precisar definir toda vez que abrir o terminal:

1. Abra **"Editar variáveis de ambiente do sistema"** (pesquise no menu Iniciar)
2. Clique em **"Variáveis de Ambiente..."**
3. Em **"Variáveis do usuário"**, clique em **"Novo"** e crie:
    - Nome: `MYSQL_PASSWORD` | Valor: `sua_senha`
    - Nome: `GEMINI_API_KEY` | Valor: `sua_api_key`

---

## 5. Executar o Projeto

### Compilar e rodar:

```powershell
cd "c:\Users\giova\Downloads\mentoriatech\curso\go-autonomous-agent\java-autonomous-agent"

mvn spring-boot:run
```

Aguarde até ver no console:
```
Started AgentApplication in X.XX seconds
```

---

## 6. Testar a API

Com o projeto rodando, abra **outro terminal PowerShell** e teste:

### Iniciar uma conversa:

```powershell
$body = '{"userMessage": "Quero agendar uma consulta"}'
$response = Invoke-RestMethod -Uri "http://localhost:8080/api/chat" -Method POST -ContentType "application/json" -Body $body
$response
```

### Continuar a conversa (usando o sessionId retornado):

```powershell
$body = '{"sessionId": "COLE_O_SESSION_ID_AQUI", "userMessage": "Com o Dr. Matheus"}'
$response = Invoke-RestMethod -Uri "http://localhost:8080/api/chat" -Method POST -ContentType "application/json" -Body $body
$response
```

### Health check:

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/health"
```

Resultado esperado: `{ "status": "UP", "service": "medical-agent" }`

---

## 🔧 Resolução de Problemas

| Problema | Solução |
|---|---|
| `Connection refused` ao iniciar | MySQL não está rodando. Verifique com `docker ps` ou inicie o serviço MySQL no Windows |
| `Access denied for user 'root'` | Confira a senha em `$env:MYSQL_PASSWORD` |
| `Invalid API Key` | Verifique a chave em `$env:GEMINI_API_KEY`. Gere uma nova no AI Studio se necessário |
| `Port 8080 already in use` | Outra aplicação usando a porta. Mude em `application.yml` com `server.port: 8081` |
| `java: command not found` | Java não está no PATH. Reinstale marcando "Set JAVA_HOME" |

---

## 📁 Estrutura do Projeto

```
java-autonomous-agent/
├── pom.xml                                 ← Dependências Maven
├── SETUP.md                                ← Este guia
├── src/main/
│   ├── java/com/mentoriatech/agent/
│   │   ├── AgentApplication.java           ← Entry point
│   │   ├── controller/
│   │   │   └── ChatController.java         ← REST API (/api/chat)
│   │   ├── dto/                            ← Objetos de transferência
│   │   ├── entity/
│   │   │   └── Doctor.java                 ← Entidade JPA
│   │   ├── repository/
│   │   │   └── DoctorRepository.java       ← Acesso ao banco
│   │   ├── service/
│   │   │   ├── GoogleCalendarService.java  ← Integração Google Calendar
│   │   │   ├── ChatSessionService.java     ← Memória das conversas
│   │   │   └── MedicalAssistantAgent.java  ← Cérebro do agente (IA)
│   │   └── tools/
│   │       └── AgentToolsConfig.java       ← Ferramentas da IA
│   └── resources/
│       ├── application.yml                 ← Configurações
│       └── db/migration/                   ← Scripts Flyway
│           ├── V1__create_doctors_table.sql
│           └── V2__insert_initial_data.sql
```


# Guia de Testes — PetShop Agent API

Todos os testes abaixo podem ser executados no **PowerShell** enquanto a aplicação estiver rodando em `http://localhost:8080`.

---

## 🔹 Teste 0 — Health Check

Verifica se a aplicação está no ar.

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/health" -Method GET
```

**Resultado esperado:**
```json
{ "status": "UP", "service": "petshop-agent" }
```

---

## 🔹 Teste 1 — Criar uma Sessão

Cria uma nova sessão de conversa e retorna um `sessionId`.

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/session" -Method POST
```

**Resultado esperado:**
```json
{ "sessionId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx" }
```

> 💡 **Guarde o `sessionId`** retornado. Ele será usado em todos os testes de conversa abaixo para manter o contexto.

---

## 🔹 Teste 2 — Primeira Mensagem (Saudação)

Inicia a conversa sem informar `sessionId` (será criado automaticamente).

```powershell
$response = Invoke-RestMethod -Uri "http://localhost:8080/api/chat" `
  -Method POST `
  -ContentType "application/json" `
  -Body '{"userMessage": "Olá, quero agendar um serviço para meu pet"}'
Write-Host "SessionId: $($response.sessionId)"
Write-Host "Resposta: $($response.replyText)"
```

**Resultado esperado:**
- O agente cumprimenta e pergunta com qual atendente ou serviço o tutor deseja agendamento.
- Um `sessionId` é gerado automaticamente.

---

## 🔹 Teste 3 — Buscar Atendente por Nome (Existente)

Continua a conversa informando o nome de um atendente cadastrado.

```powershell
$sessionId = "COLE_SEU_SESSION_ID_AQUI"

$response = Invoke-RestMethod -Uri "http://localhost:8080/api/chat" `
  -Method POST `
  -ContentType "application/json" `
  -Body "{`"sessionId`": `"$sessionId`", `"userMessage`": `"Quero agendar com a Ana`"}"

Write-Host "Resposta: $($response.replyText)"
```

**Resultado esperado:**
- O agente reconhece a Ana (Banho & Tosa).
- Pergunta qual data e horário o tutor prefere.

---

## 🔹 Teste 4 — Buscar Atendente por Nome (Inexistente)

Testa o fluxo quando o atendente não está cadastrado.

```powershell
$response = Invoke-RestMethod -Uri "http://localhost:8080/api/chat" `
  -Method POST `
  -ContentType "application/json" `
  -Body '{"userMessage": "Quero agendamento com a Maria"}'

Write-Host "SessionId: $($response.sessionId)"
Write-Host "Resposta: $($response.replyText)"
```

**Resultado esperado:**
- O agente informa que a atendente não foi encontrada.
- Pergunta qual serviço o tutor busca.

---

## 🔹 Teste 5 — Buscar por Serviço (Existente)

Testa a busca de atendentes por serviço.

```powershell
$sessionId = "COLE_SEU_SESSION_ID_DO_TESTE_4"

$response = Invoke-RestMethod -Uri "http://localhost:8080/api/chat" `
  -Method POST `
  -ContentType "application/json" `
  -Body "{`"sessionId`": `"$sessionId`", `"userMessage`": `"Preciso de uma consulta veterinária`"}"

Write-Host "Resposta: $($response.replyText)"
```

**Resultado esperado:**
- O agente encontra o Carlos (Consultas) no banco.
- Oferece o Carlos como opção e pergunta se o tutor deseja agendar.

---

## 🔹 Teste 6 — Buscar por Serviço (Inexistente)

Testa quando o serviço solicitado não existe no PetShop.

```powershell
$response = Invoke-RestMethod -Uri "http://localhost:8080/api/chat" `
  -Method POST `
  -ContentType "application/json" `
  -Body '{"userMessage": "Quero agendar adestramento"}'

Write-Host "Resposta: $($response.replyText)"
```

**Resultado esperado:**
- O agente informa que não oferece adestramento.
- Lista os serviços disponíveis: Banho & Tosa, Consultas, Pronto Socorro.

---

## 🔹 Teste 7 — Listar Serviços

Testa se o agente consegue listar todos os serviços ao ser questionado.

```powershell
$response = Invoke-RestMethod -Uri "http://localhost:8080/api/chat" `
  -Method POST `
  -ContentType "application/json" `
  -Body '{"userMessage": "Quais serviços vocês oferecem?"}'

Write-Host "Resposta: $($response.replyText)"
```

**Resultado esperado:**
- O agente lista: Banho & Tosa, Consultas, Pronto Socorro.

---

## 🔹 Teste 8 — Fluxo Completo de Agendamento

Simula uma conversa inteira do início ao agendamento. Execute os comandos em sequência:

### Passo 1 — Iniciar

```powershell
$response = Invoke-RestMethod -Uri "http://localhost:8080/api/chat" `
  -Method POST `
  -ContentType "application/json" `
  -Body '{"userMessage": "Oi, quero marcar um banho para meu cachorro"}'

$sessionId = $response.sessionId
Write-Host "Session: $sessionId"
Write-Host "Agente: $($response.replyText)"
```

### Passo 2 — Informar o atendente

```powershell
$response = Invoke-RestMethod -Uri "http://localhost:8080/api/chat" `
  -Method POST `
  -ContentType "application/json" `
  -Body "{`"sessionId`": `"$sessionId`", `"userMessage`": `"Com a Ana, por favor`"}"

Write-Host "Agente: $($response.replyText)"
```

### Passo 3 — Informar data e hora

```powershell
$response = Invoke-RestMethod -Uri "http://localhost:8080/api/chat" `
  -Method POST `
  -ContentType "application/json" `
  -Body "{`"sessionId`": `"$sessionId`", `"userMessage`": `"Amanhã às 14:00`"}"

Write-Host "Agente: $($response.replyText)"
```

### Passo 4 — Confirmar e informar tutor e nome do pet

```powershell
$response = Invoke-RestMethod -Uri "http://localhost:8080/api/chat" `
  -Method POST `
  -ContentType "application/json" `
  -Body "{`"sessionId`": `"$sessionId`", `"userMessage`": `"Pode confirmar! Meu nome é Giovanni e meu cachorro se chama Rex`"}"

Write-Host "Agente: $($response.replyText)"
```

**Resultado esperado:**
- Passo 1: Agente pergunta qual serviço ou atendente.
- Passo 2: Agente reconhece a Ana (Banho & Tosa) e pergunta data/hora.
- Passo 3: Agente verifica disponibilidade no Google Calendar.
- Passo 4: Agente cria o evento e confirma o agendamento com link do calendário.

---

## 🔹 Teste 9 — Mensagem Fora do Escopo

Testa se o agente se mantém no assunto de agendamentos do PetShop.

```powershell
$response = Invoke-RestMethod -Uri "http://localhost:8080/api/chat" `
  -Method POST `
  -ContentType "application/json" `
  -Body '{"userMessage": "Qual a previsão do tempo para amanhã?"}'

Write-Host "Resposta: $($response.replyText)"
```

**Resultado esperado:**
- O agente gentilmente informa que é especializado apenas em agendamentos do PetShop PetCare.

---

## 🔹 Teste 10 — Requisição com Body Inválido

Testa o tratamento de erros.

```powershell
try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/chat" `
      -Method POST `
      -ContentType "application/json" `
      -Body '{}'
} catch {
    Write-Host "Erro: $($_.Exception.Message)"
}
```

**Resultado esperado:**
- A API retorna erro 400 (Bad Request) ou comportamento de fallback.

---

## 📊 Resumo dos Atendentes Cadastrados

| ID | Nome | Serviço | Duração |
|---|---|---|---|
| 1 | Ana | Banho & Tosa | 90 min |
| 2 | Carlos | Consultas | 60 min |
| 3 | Roberto | Pronto Socorro | 45 min |

---

## 📋 Checklist de Validação

| # | Cenário | Status |
|---|---|---|
| 0 | Health check retorna UP | ⬜ |
| 1 | Criação de sessão | ⬜ |
| 2 | Primeira mensagem com saudação | ⬜ |
| 3 | Busca por atendente existente (Ana) | ⬜ |
| 4 | Busca por atendente inexistente (Maria) | ⬜ |
| 5 | Busca por serviço existente (Consultas) | ⬜ |
| 6 | Busca por serviço inexistente (adestramento) | ⬜ |
| 7 | Listar todos os serviços | ⬜ |
| 8 | Fluxo completo de agendamento (4 passos) | ⬜ |
| 9 | Mensagem fora do escopo | ⬜ |
| 10 | Body inválido / erro | ⬜ |
