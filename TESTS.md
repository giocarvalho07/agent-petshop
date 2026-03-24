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
