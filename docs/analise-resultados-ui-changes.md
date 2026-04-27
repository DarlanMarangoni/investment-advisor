# Alterações — Feature: Análise de Resultados

Branch: `featute/analise_resultados`

---

## Visão geral

Esta feature adiciona um fluxo completo de análise de demonstrativos de resultado (PDFs) de empresas da B3 usando IA. As principais mudanças se dividem em três grupos:

1. **Troca do SDK de IA**: `anthropic-java` → LangChain4j (suporte a múltiplos provedores)
2. **Persistência das análises**: histórico salvo no PostgreSQL (datasource analítico)
3. **Interface Vaadin**: nova página de upload + correções de estilo

---

## Backend

### 4. `pom.xml` — Troca de SDK de IA

```xml
<!-- Removido -->
<dependency>
    <groupId>com.anthropic</groupId>
    <artifactId>anthropic-java</artifactId>
    <version>2.17.0</version>
</dependency>

<!-- Adicionado -->
<dependency>groupId>dev.langchain4j</groupId> <!-- core -->
<dependency>groupId>dev.langchain4j</groupId> <!-- langchain4j-ollama -->
<dependency>groupId>dev.langchain4j</groupId> <!-- langchain4j-open-ai -->
<dependency>groupId>dev.langchain4j</groupId> <!-- langchain4j-anthropic -->
<dependency>groupId>dev.langchain4j</groupId> <!-- langchain4j-google-ai-gemini -->
```

**Por quê:** O SDK oficial da Anthropic (`anthropic-java`) só suporta o Claude. O LangChain4j abstrai a camada de LLM atrás de uma interface única (`ChatLanguageModel`), permitindo trocar de provedor (Ollama, OpenAI, Anthropic, Gemini) apenas por configuração, sem alterar o código de negócio. Isso é especialmente útil em desenvolvimento local, onde um modelo Ollama evita custos de API.

---

### 5. `AnthropicConfig.java` + `AnthropicEarningsAnalysisAdapter.java` — Removidos

**Por quê:** Eram a implementação direta do SDK `anthropic-java`. Com a migração para LangChain4j, a configuração do cliente e o adapter foram reescritos — `AnthropicConfig` foi substituído por `LlmConfig` e `AnthropicEarningsAnalysisAdapter` por `LangChainEarningsAnalysisAdapter`.

---

### 6. `LlmConfig.java` — Novo: configuração multi-provedor

```java
@Bean
@ConditionalOnProperty(name = "llm.provider", havingValue = "ollama", matchIfMissing = true)
public ChatLanguageModel ollamaChatModel(...) { ... }

@Bean
@ConditionalOnProperty(name = "llm.provider", havingValue = "openai")
public ChatLanguageModel openAiChatModel(...) { ... }

// idem para anthropic e gemini
```

**Por quê:** Cada `@Bean` é condicionado ao valor de `llm.provider` no `application.yml`. O Spring instancia apenas um dos quatro beans, injetando-o em `LangChainEarningsAnalysisAdapter`. Trocar de provedor é alterar uma linha de configuração — nenhum código Java precisa mudar. O Ollama é o padrão (`matchIfMissing = true`) para facilitar o desenvolvimento local sem chaves de API.

---

### 7. `LangChainEarningsAnalysisAdapter.java` — Novo: adapter de IA genérico

```java
@Service
public class LangChainEarningsAnalysisAdapter implements EarningsAnalysisPort {
    private final ChatLanguageModel chatLanguageModel;

    public String analyze(String reportText, String ticker) {
        Response<AiMessage> response = chatLanguageModel.generate(
            SystemMessage.from(SYSTEM_PROMPT),
            UserMessage.from(prompt)
        );
        return response.content().text();
    }
}
```

**Por quê:** Implementa a porta de saída `EarningsAnalysisPort` usando a abstração `ChatLanguageModel` do LangChain4j. O `chatLanguageModel` injetado é o bean criado pelo `LlmConfig` conforme o provedor configurado. O texto do PDF é truncado a 50 000 caracteres antes de enviar, para não exceder o limite de contexto de modelos menores como o Llama 3.1 8B.

---

### 8. `application.yml` — Configuração de LLM

```yaml
llm:
  provider: ollama          # troca de provedor sem recompilar
  ollama:
    base-url: ${LLM_OLLAMA_BASE_URL:http://localhost:11434}
    model: ${LLM_OLLAMA_MODEL:llama3.1:8b}
  openai:
    api-key: ${OPENAI_API_KEY:}
    model: ${OPENAI_MODEL:gpt-4o-mini}
  anthropic:
    api-key: ${ANTHROPIC_API_KEY:}
    model: ${ANTHROPIC_MODEL:claude-haiku-4-5}
  gemini:
    api-key: ${GEMINI_API_KEY:}
    model: ${GEMINI_MODEL:gemini-2.0-flash}
```

**Por quê:** Cada chave de API é lida de variável de ambiente com fallback vazio, evitando que segredos entrem no repositório. Os modelos também têm defaults sensatos para cada provedor, mas podem ser sobrescritos via env sem rebuildar a aplicação.

---

### 9. `EarningsAnalysis.java` — Campo `analyzedAt` adicionado

```java
// Antes
public record EarningsAnalysis(String ticker, String analysis) {}

// Depois
public record EarningsAnalysis(String ticker, String analysis, LocalDateTime analyzedAt) {}
```

**Por quê:** A análise precisa de timestamp para ser exibida no histórico (coluna "Analisado em" no grid) e para a ordenação cronológica ao recuperar do banco. O campo é preenchido pelo `EarningsReportService` no momento em que a análise é gerada, mantendo o modelo de domínio independente de infraestrutura.

---

### 10. `UploadEarningsReportUseCase.java` — `MultipartFile` → `byte[]`

```java
// Antes
EarningsAnalysis upload(MultipartFile file, String ticker);

// Depois
EarningsAnalysis upload(byte[] pdfBytes, String ticker);
```

**Por quê:** `MultipartFile` é uma abstração do Spring MVC/Web — expô-la numa porta de caso de uso viola a regra de dependência da arquitetura hexagonal (o domínio não deve depender de frameworks de infraestrutura). Usando `byte[]`, a porta fica neutra: pode ser chamada tanto pelo controller REST (que converte `MultipartFile.getBytes()`) quanto pela view Vaadin (que entrega os bytes direto do `ByteArrayOutputStream`).

---

### 11. `GetEarningsAnalysesUseCase.java` — Nova porta de entrada

```java
public interface GetEarningsAnalysesUseCase {
    List<EarningsAnalysis> findAll();
}
```

**Por quê:** Separar a leitura do histórico em sua própria porta de entrada segue o princípio de segregação de interfaces (ISP). A view Vaadin só precisa ler o histórico — não faz sentido injetar `UploadEarningsReportUseCase` apenas para listar registros.

---

### 12. `EarningsAnalysisRepositoryPort.java` — Nova porta de saída

```java
public interface EarningsAnalysisRepositoryPort {
    void save(EarningsAnalysis analysis);
    List<EarningsAnalysis> findAll();
}
```

**Por quê:** Define o contrato de persistência no lado do domínio, sem nenhuma referência a JPA ou banco de dados. O domínio declara o que precisa; a infraestrutura (`PostgresEarningsAnalysisRepository`) implementa como.

---

### 13. `EarningsReportService.java` — Persistência e novo caso de uso

```java
// Passa a implementar dois casos de uso
public class EarningsReportService implements UploadEarningsReportUseCase, GetEarningsAnalysesUseCase {

    @Override
    public EarningsAnalysis upload(byte[] pdfBytes, String ticker) {
        validatePdf(pdfBytes);
        String reportText = extractText(pdfBytes);
        String analysis = earningsAnalysisPort.analyze(reportText, ticker);
        EarningsAnalysis result = new EarningsAnalysis(ticker, analysis, LocalDateTime.now());
        earningsAnalysisRepositoryPort.save(result);   // persiste antes de retornar
        return result;
    }

    @Override
    public List<EarningsAnalysis> findAll() {
        return earningsAnalysisRepositoryPort.findAll();
    }
}
```

**Por quê:**
- A validação do PDF passou a usar **magic bytes** (`%PDF` = `0x25 0x50 0x44 0x46`) em vez do `Content-Type` do `MultipartFile`. O `Content-Type` é informado pelo cliente e pode ser falsificado; os magic bytes verificam o conteúdo real do arquivo.
- O resultado é salvo via `earningsAnalysisRepositoryPort` imediatamente após a análise, garantindo que toda análise bem-sucedida seja persistida.
- `findAll()` delega inteiramente ao repositório.

---

### 14. `EarningsReportController.java` — Novo endpoint GET + adaptação do upload

```java
// Upload adaptado para byte[]
public ResponseEntity<EarningsAnalysis> upload(
        @RequestPart("file") MultipartFile file,
        @RequestParam("ticker") String ticker) throws IOException {
    return ResponseEntity.ok(uploadEarningsReportUseCase.upload(file.getBytes(), ticker));
}

// Novo endpoint
@GetMapping
public ResponseEntity<List<EarningsAnalysis>> findAll() {
    return ResponseEntity.ok(getEarningsAnalysesUseCase.findAll());
}
```

**Por quê:** O controller continua recebendo `MultipartFile` (porque é a forma natural de upload multipart no Spring MVC), mas converte para `byte[]` antes de chamar o caso de uso, respeitando o contrato da porta. O endpoint `GET /api/v1/earnings` expõe o histórico via API REST, útil para integrações externas além da interface Vaadin.

---

### 15. Camada de persistência JPA (`analytics/`)

**Arquivos novos:**
- `EarningsAnalysisEntity.java` — entidade JPA mapeada para a tabela `earnings_analysis`
- `EarningsAnalysisJpaRepository.java` — Spring Data JPA com query de ordenação por data
- `PostgresEarningsAnalysisRepository.java` — implementa `EarningsAnalysisRepositoryPort`

```java
// EarningsAnalysisEntity
@Entity
@Table(name = "earnings_analysis", indexes = {
    @Index(name = "idx_earnings_ticker", columnList = "ticker"),
    @Index(name = "idx_earnings_analyzed_at", columnList = "analyzed_at DESC")
})
```

```java
// PostgresEarningsAnalysisRepository — converte entre entidade JPA e modelo de domínio
public List<EarningsAnalysis> findAll() {
    return jpaRepository.findAllByOrderByAnalyzedAtDesc().stream()
            .map(e -> new EarningsAnalysis(e.getTicker(), e.getAnalysis(), e.getAnalyzedAt()))
            .toList();
}
```

**Por quê:**
- A entidade JPA fica isolada em `infrastructure/adapter/out/analytics/`, nunca vazando para o domínio.
- Os dois índices cobrem os padrões de acesso esperados: filtrar por ticker e listar em ordem cronológica decrescente.
- As transações usam explicitamente `analyticsTransactionManager` (datasource separado para análises históricas, configurado em commit anterior), evitando misturar com a transação do datasource principal.
- O `PostgresEarningsAnalysisRepository` faz a conversão entre `EarningsAnalysisEntity` (JPA) e `EarningsAnalysis` (domínio) — o domínio nunca vê a entidade JPA.

---

## 1. `AppShellConfig.java` — Restauração do tema Lumo

**Arquivo:** `src/main/java/.../infrastructure/config/AppShellConfig.java`

```java
// Antes
@Push
public class AppShellConfig implements AppShellConfigurator {}

// Depois
@Push
@Theme(themeClass = Lumo.class)
public class AppShellConfig implements AppShellConfigurator {}
```

**Por quê:**
Em Vaadin 24+, a regra de aplicação de tema mudou:

| Cenário | Resultado |
|---|---|
| Nenhuma classe `AppShellConfigurator` | Lumo aplicado automaticamente |
| `AppShellConfigurator` **sem** `@Theme` | **Nenhum tema aplicado** |
| `AppShellConfigurator` **com** `@Theme(Lumo.class)` | Lumo aplicado corretamente |

O `AppShellConfig` foi criado nessa branch com apenas `@Push` (para habilitar Server Push / WebSocket). Sem o `@Theme`, o Vaadin interpretou isso como "sem tema": botões perderam as cores (primário azul, erro vermelho) e a fonte voltou ao padrão do browser em vez da fonte Inter do Lumo.

---

## 2. `AnalysisView.java` — Card layout consistente

**Arquivo:** `src/main/java/.../infrastructure/adapter/in/ui/AnalysisView.java`

### 2.1 Rota alterada para `"analysis"`

```java
// Antes
@Route("")

// Depois
@Route("analysis")
```

**Por quê:** A página de upload de relatórios (`EarningsView`) passou a ser a página padrão (`@Route("")`). A análise técnica ficou em `/analysis`.

### 2.2 Background cinza na view

```java
getStyle().set("background", "var(--lumo-contrast-5pct)");
```

**Por quê:** Sem background, o conteúdo fica "flutuando" num branco liso sem hierarquia visual. O fundo cinza claro (`~5% de contraste`) cria separação entre o fundo da página e os cards brancos, seguindo o mesmo padrão já usado no `LoginView`.

### 2.3 Título `H2` movido para dentro da toolbar

```java
// Antes: título numa linha, toolbar noutra
H2 title = new H2("Análise Técnica — B3");
add(title, toolbar, grid);

// Depois: título integrado na toolbar
HorizontalLayout toolbar = new HorizontalLayout(title, reloadButton, earningsButton, userLabel, logoutButton);
title.getStyle().set("margin", "0");
```

**Por quê:** O H2 fora da toolbar criava um bloco separado que consumia espaço vertical desnecessário. Integrado à toolbar, o layout fica compacto e consistente com o `EarningsView`. O `margin: 0` remove as margens padrão do browser no `H2` que causavam desalinhamento vertical dentro do `HorizontalLayout`.

### 2.4 Grid envolvido em card branco

```java
private VerticalLayout buildGridCard() {
    VerticalLayout card = new VerticalLayout(new H3("Posições"), grid);
    card.getStyle()
            .set("background", "white")
            .set("border-radius", "8px")
            .set("box-shadow", "0 2px 12px rgba(0,0,0,0.07)")
            .set("padding", "20px");
    card.setSizeFull();
    grid.setSizeFull();
    return card;
}
```

**Por quê:** O `EarningsView` usa cards brancos com sombra para todas as seções de conteúdo. Sem este padrão no `AnalysisView`, as duas páginas tinham aparências completamente diferentes. O card cria elevação visual e agrupa o conteúdo de forma clara.

### 2.5 `toolbar.setWidthFull()`

**Por quê:** Sem `setWidthFull()`, a toolbar ocupa apenas o espaço dos seus filhos. Com `setFlexGrow(1, userLabel)`, o userLabel precisa de um container que ocupe 100% da largura para poder se expandir e empurrar os botões de logout para a direita.

---

## 3. `EarningsView.java` — Correções e melhorias

**Arquivo:** `src/main/java/.../infrastructure/adapter/in/ui/EarningsView.java`

### 3.1 Rota alterada para `""` (página padrão)

```java
// Antes
@Route("earnings")

// Depois
@Route("")
```

**Por quê:** O fluxo principal do usuário é analisar relatórios. Faz mais sentido que esta página seja a primeira exibida após o login, em vez de exigir navegação extra.

### 3.2 Bug: `buildHistorySection()` chamado duas vezes

```java
// Antes (bug)
add(
    buildToolbar(authContext),
    buildUploadCard(),
    buildResultSection(),
    buildHistorySection()   // cria card1 com historyGrid
);
expand(buildHistorySection()); // cria card2, move historyGrid do card1 para card2

// Depois (correto)
VerticalLayout historyCard = buildHistorySection();
add(
    buildToolbar(authContext),
    buildUploadCard(),
    buildResultSection(),
    historyCard
);
expand(historyCard);
```

**Por quê:** Em Vaadin, um componente só pode ter um pai. Ao chamar `buildHistorySection()` duas vezes, duas instâncias de `VerticalLayout` eram criadas, mas o `historyGrid` (que é um campo da classe) era movido para o segundo card. Resultado: o primeiro card ficava vazio e visível no layout, o segundo tinha o grid mas era passado ao `expand()` que o adicionava ao layout como componente extra. A página renderizava um card vazio seguido do card com o histórico.

### 3.3 `MemoryBuffer` depreciado substituído

```java
// Antes (depreciado desde Vaadin 24.8, marcado para remoção)
private final MemoryBuffer buffer = new MemoryBuffer();
private final Upload upload = new Upload(buffer);

// Depois
private final ByteArrayOutputStream uploadedData = new ByteArrayOutputStream();
private String uploadedFileName;
private final Upload upload = new Upload();

// No buildUploadCard():
upload.setReceiver((filename, mimeType) -> {
    uploadedData.reset();
    uploadedFileName = filename;
    return uploadedData;
});
```

**Por quê:** O Vaadin 24.8 depreciou `MemoryBuffer` marcando-o para remoção futura. O padrão substituto é registrar um `Receiver` via lambda diretamente no `Upload`. O lambda recebe o `OutputStream` onde o Vaadin escreve os bytes do arquivo; armazenar um `ByteArrayOutputStream` como campo permite leitura posterior via `toByteArray()`, sem a necessidade de ler um `InputStream` intermediário.

### 3.4 Background cinza e `margin: 0` no título

Mesma motivação do `AnalysisView` (itens 2.2 e 2.3 acima) — consistência visual entre as páginas.
