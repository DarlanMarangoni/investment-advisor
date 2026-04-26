# Migração para Múltiplos Datasources PostgreSQL

## Contexto

O projeto utilizava um único datasource PostgreSQL para tudo. A migração introduz dois bancos independentes, separando a responsabilidade de armazenar cotações brutas da responsabilidade de armazenar os resultados de análise técnica.

---

## Arquitetura dos Datasources

| Datasource | Banco sugerido | Finalidade | Entidade JPA |
|---|---|---|---|
| `quotes` (primário) | `investment_advisor` | Cotações recebidas da BrAPI | `QuoteEntity` |
| `analytics` (secundário) | `investment_advisor_analytics` | Resultados de análise técnica | `AnalysisEntity` |

Cada datasource tem seu próprio `EntityManagerFactory` e `PlatformTransactionManager`, totalmente isolados. Os repositórios Spring Data são roteados pelo pacote via `@EnableJpaRepositories`.

---

## Variáveis de Ambiente

### Antes
```
DB_URL
DB_USERNAME
DB_PASSWORD
```

### Depois
```
DB_QUOTES_URL
DB_QUOTES_USERNAME
DB_QUOTES_PASSWORD

DB_ANALYTICS_URL
DB_ANALYTICS_USERNAME
DB_ANALYTICS_PASSWORD
```

---

## Arquivos Alterados

### `application.yml`
```yaml
# Antes
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver

# Depois
spring:
  datasource:
    quotes:
      url: ${DB_QUOTES_URL}
      username: ${DB_QUOTES_USERNAME}
      password: ${DB_QUOTES_PASSWORD}
      driver-class-name: org.postgresql.Driver
    analytics:
      url: ${DB_ANALYTICS_URL}
      username: ${DB_ANALYTICS_USERNAME}
      password: ${DB_ANALYTICS_PASSWORD}
      driver-class-name: org.postgresql.Driver
```

### `InvestmentAdvisorApplication.java`
A auto-configuração de datasource e JPA do Spring Boot é desativada para evitar conflito com a configuração manual:

```java
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
```

> No Spring Boot 4, essas classes estão nos pacotes `org.springframework.boot.jdbc.autoconfigure` e `org.springframework.boot.hibernate.autoconfigure`.

### `TechnicalAnalysisService.java`
O serviço passa a persistir cada análise técnica calculada no banco analytics:

```java
public TechnicalAnalysis analyze(String ticker) {
    List<Quote> quotes = quoteRepository.findLatestByTicker(ticker, 100);
    TechnicalAnalysis analysis = buildAnalysis(ticker, quotes);
    analysisRepository.save(analysis);   // novo: persiste no banco analytics
    return analysis;
}
```

---

## Arquivos Criados

### `infrastructure/config/QuotesDataSourceConfig.java`
Configura o datasource primário (cotações):
- `@Primary` em todos os beans
- Escaneia o pacote `infrastructure.adapter.out.persistence`
- Usa `DataSourceProperties.initializeDataSourceBuilder()` para mapear `url` corretamente para o HikariCP

### `infrastructure/config/AnalyticsDataSourceConfig.java`
Configura o datasource secundário (análises):
- Escaneia o pacote `infrastructure.adapter.out.analytics`
- Mesma estrutura do primário, sem `@Primary`

### `domain/port/out/AnalysisRepositoryPort.java`
Port de domínio para o banco analytics:
```java
public interface AnalysisRepositoryPort {
    void save(TechnicalAnalysis analysis);
    Optional<TechnicalAnalysis> findLatest(String ticker);
    List<TechnicalAnalysis> findByTicker(String ticker);
}
```

### `infrastructure/adapter/out/analytics/AnalysisEntity.java`
Entidade JPA mapeada para a tabela `technical_analyses` no banco analytics. Armazena `ticker`, `price`, `sma20`, `sma50`, `rsi`, `signal` e `analyzedAt`.

### `infrastructure/adapter/out/analytics/AnalysisJpaRepository.java`
Repositório Spring Data JPA para `AnalysisEntity`. Roteado automaticamente para o `analyticsEntityManagerFactory` pelo `@EnableJpaRepositories` em `AnalyticsDataSourceConfig`.

### `infrastructure/adapter/out/analytics/PostgresAnalysisRepository.java`
Implementa `AnalysisRepositoryPort`, fazendo o mapeamento entre o modelo de domínio `TechnicalAnalysis` e a entidade `AnalysisEntity`.

---

## Testes

### `src/test/resources/application.yml`
Dois bancos H2 em memória, um por datasource:
```yaml
spring:
  datasource:
    quotes:
      url: jdbc:h2:mem:quotesdb;DB_CLOSE_DELAY=-1
      driver-class-name: org.h2.Driver
      username: sa
      password:
    analytics:
      url: jdbc:h2:mem:analyticsdb;DB_CLOSE_DELAY=-1
      driver-class-name: org.h2.Driver
      username: sa
      password:
  jpa:
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
```

As propriedades `spring.jpa.hibernate.ddl-auto` e `spring.jpa.properties.hibernate.dialect` são lidas via `@Value` nos dois configs de datasource, garantindo que o dialeto correto (H2 em testes, PostgreSQL em produção) seja aplicado em ambos.

### `TechnicalAnalysisServiceTest.java`
Adicionado `@Mock AnalysisRepositoryPort analysisRepository` para que o `@InjectMocks` do Mockito consiga instanciar o `TechnicalAnalysisService` com o novo campo.

### `InvestmentAdvisorApplicationTests.java`
Corrigido para usar `@MockitoBean` (substituto do `@MockBean` no Spring Boot 4 / Spring Framework 7) para mockar o `ClientRegistrationRepository` do Keycloak, que não está disponível no ambiente de testes.

---

## Resultado

```
Tests run: 38, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS
```
