# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

**investment-advisor** — aplicação Spring Boot para análise técnica de ações da B3, apoiando decisões de investimento. Busca cotações automaticamente de segunda a sexta das 10h às 18h via BrAPI (`brapi.dev`).

## Tech Stack

- **Java 25** (gerenciado via SDKMAN — ver `.sdkmanrc`)
- **Spring Boot 4.0.5** com virtual threads habilitadas
- **Maven** como build tool
- **BrAPI** (`https://brapi.dev`) como fonte de cotações da B3
- Armazenamento em memória (sem banco de dados)

## Commands

```bash
# Instalar o SDK correto (requer SDKMAN)
sdk env install

# Build
mvn clean package

# Executar
mvn spring-boot:run

# Rodar os testes
mvn test

# Rodar um teste específico
mvn test -Dtest=NomeDaClasseTest

# Build sem testes
mvn clean package -DskipTests
```

## Arquitetura Hexagonal

```
domain/
  model/          → entidades e value objects puros (sem dependência de framework)
  port/in/        → interfaces dos casos de uso (driving ports)
  port/out/       → interfaces para dependências externas (driven ports)

application/
  service/        → implementações dos casos de uso; contém a lógica de negócio

infrastructure/
  adapter/in/
    scheduler/    → QuoteScheduler — dispara busca de cotações via cron
    web/          → QuoteController — endpoints REST
  adapter/out/
    client/       → BrapiClient — implementa StockQuotePort via HTTP
    repository/   → InMemoryQuoteRepository — implementa QuoteRepositoryPort
  config/         → RestClientConfig, etc.
```

**Regra de dependência**: `domain` não depende de nada. `application` depende só de `domain`. `infrastructure` depende de `domain` e `application` — nunca o contrário.

## Scheduler

Configurado em `application.yml` via `app.scheduler.cron`. Padrão: `0 */15 10-17 * * MON-FRI` (a cada 15 min, seg–sex, 10h–17h45) + execução fixa às 18h (`closingFetch`). Para forçar uma busca fora do horário, use `POST /api/v1/quotes/fetch`.

## Análise Técnica

O `TechnicalAnalysisService` calcula:
- **SMA20 / SMA50** — médias móveis simples de 20 e 50 períodos
- **RSI(14)** — Relative Strength Index de 14 períodos
- **Sinal**: BUY / SELL / HOLD combinando RSI com cruzamento de médias

Requer ao menos 15 cotações históricas para calcular RSI e 20/50 para as SMAs. Cotações são acumuladas em memória enquanto a aplicação roda.

## Endpoints

| Método | Path | Descrição |
|--------|------|-----------|
| GET | `/api/v1/analysis` | Análise de todos os ativos configurados |
| GET | `/api/v1/analysis/{ticker}` | Análise de um ativo específico |
| POST | `/api/v1/quotes/fetch` | Dispara busca manual de cotações |
| GET | `/swagger-ui.html` | Documentação interativa da API |
| GET | `/actuator/health` | Health check |

## Configuração

Ativos monitorados: `app.stocks.tickers` em `application.yml` (padrão: PETR4, VALE3, ITUB4, BBDC4, ABEV3, WEGE3, RENT3, MGLU3).

Token BrAPI (opcional, aumenta rate limit): variável de ambiente `BRAPI_TOKEN`.