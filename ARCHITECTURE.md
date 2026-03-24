# Arquitetura do Agente Autônomo (PetShop Agent)

Este documento descreve a arquitetura do projeto **PetShop Agent**, uma aplicação Java (Spring Boot) que implementa um **Agente de Inteligência Artificial Autônomo** focado no agendamento de serviços para pets.

---

## 1. Visão Geral do Sistema

O sistema é um assistente virtual construído com **Spring Boot** e **Spring AI**, integrado ao LLM (Large Language Model) **Google Gemini**. O grande diferencial deste projeto é a sua **autonomia avançada**: a IA não apenas conversa com o tutor, mas possui a capacidade de invocar funções locais (executar código Java), consultar o banco de dados e integrar-se a APIs externas (Google Calendar) por conta própria.

### Stack Tecnológica
* **Linguagem:** Java 17+
* **Framework:** Spring Boot 3
* **Integração de IA:** Spring AI (usando a interface OpenAI-compatível apontando para a API do Google Gemini)
* **Banco de Dados:** MySQL (via Hibernate/JPA). Migrations com Flyway.
* **APIs Externas:** Google Calendar API v3
* **Modelo de IA:** `gemini-2.5-flash`

---

## 2. Como o Agente de IA Trabalha de Forma Autônoma?

A autonomia do agente se dá através de um padrão arquitetural conhecido como **Function Calling** (ou Tool Calling).

Em um chatbot tradicional, o modelo apenas recebe um texto e cospe outro texto. No **PetShop Agent**, nós registramos uma lista de "Ferramentas" (Tools) que o modelo de IA tem autorização para usar.

### O Fluxo Autônomo (Passo a Passo)

1. **Entrada do Tutor:** O tutor envia uma mensagem (ex: *"Quero banho para meu cachorro"*).
2. **Análise de Intenção (LLM):** O `MedicalAssistantAgent` envia essa mensagem junto com o histórico da conversa e o **System Prompt** para o Google Gemini.
3. **Decisão Autônoma (Tool Calling):** O modelo percebe que precisa saber quais atendentes fazem Banho & Tosa. Em vez de inventar uma resposta, o modelo devolve um comando: *"Execute a ferramenta `getVetsByService` passando `Banho & Tosa` e me devolva o resultado"*.
4. **Execução Local (Java):** O Spring AI intercepta essa requisição, localiza o método na classe `AgentToolsConfig.java` e executa a busca real no banco de dados.
5. **Realimentação do Modelo:** A aplicação retorna o resultado (ex: *"Ana encontrada"*) e injeta de volta para o modelo.
6. **Resposta Final (LLM):** Baseado nessa informação real, a IA formula a resposta: *"Encontrei a Ana, responsável por Banho & Tosa. Qual data e horário você prefere?"*.

Tudo isso ocorre em **frações de segundo**, dando a ilusão de uma resposta contínua, mas na verdade a IA foi e voltou na sua aplicação chamando métodos de forma autônoma!

---

## 3. Principais Componentes do Projeto

Abaixo estão as responsabilidades de cada camada do código:

### 3.1. `ChatController.java`
A porta de entrada via API REST. Fornece um endpoint POST (`/api/chat`) onde o cliente (frontend ou Postman) posta o ID de uma sessão de conversa (`sessionId`) e a mensagem do tutor.

### 3.2. `ChatSessionService.java`
Mantém a preservação de contexto. Guarda o histórico da conversa de cada sessão na memória. Sem isso, a IA sofreria de "amnésia" a cada nova mensagem.

### 3.3. `MedicalAssistantAgent.java`
O cérebro de orquestração do Agente.
* Configura o **System Prompt** inicial, definindo as restrições da IA (*"Você é um assistente do PetShop PetCare..."*).
* Faz a chamada direta ao ChatClient do Spring AI, "autorizando" explicitamente quais funções o agente pode invocar de forma autônoma.

### 3.4. `AgentToolsConfig.java`
A "caixa de ferramentas" da IA. Expõe métodos como *Beans* (`@Bean`) do tipo `Function` ou `Supplier`.
Cada ferramenta tem uma anotação `@Description`. **A IA lê essa descrição** para decidir sozinha qual função escolher!

O arsenal atual do Agente inclui:
* `today()`: Descobre que dia é hoje.
* `listServices()`: Consulta quais serviços existem no PetShop (BD).
* `getVetsByService()`: Retorna os atendentes de um serviço (BD).
* `findVetByName()`: Checa os dados de um atendente específico (BD).
* `checkAvailability()`: Verifica na API do Google Calendar se o atendente tem vaga disponível.
* `createAppointment()`: Cria oficialmente o evento no Google Calendar com nome do tutor e do pet.

### 3.5. `GoogleCalendarService.java`
Serviço de ponte oficial (Gateway) com o Workspace do Google. Utiliza a técnica de **ADC (Application Default Credentials)** para injetar credenciais de login seguras. Cria o evento no calendário do atendente responsável de forma automática.

### 3.6. DB Migration (Flyway)
Roteiros SQL que versionam a estrutura do banco de dados (tabelas `vets`) e geram os inserts de dados iniciais (Ana, Carlos, Roberto) a cada `build`, mantendo total integridade no setup.

---

## 4. O Sistema de "Guarda-Corpo" (Segurança e Limites da IA)

Como garantimos que a IA não divague ou invente atendentes?
* A IA é impedida de prosseguir etapas sem antes consultar o banco. (Ex: ela não agenda sem chamar `getVetsByService`).
* Se o serviço é inválido, a ferramenta devolve "Erro/Não encontrou", e o Agente reflete essa negação ao usuário (Autocorreção).
* O System Prompt determina diretivamente o escopo: o Agente sabe estritamente que só resolve coisas relacionadas ao PetShop PetCare.

Essa arquitetura molda um verdadeiro **Assistente Inteligente** onde o modelo de IA é meramente a interface cognitiva (raciocínio e linguagem), mas os braços operacionais (banco, calendário, agenda) continuam sob forte controle da nossa API em Java!
