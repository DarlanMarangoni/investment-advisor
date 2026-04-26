package br.com.investmentadvisor.infrastructure.adapter.out.ai;

import br.com.investmentadvisor.domain.port.out.EarningsAnalysisPort;
import com.anthropic.client.AnthropicClient;
import com.anthropic.core.http.StreamResponse;
import com.anthropic.models.messages.CacheControlEphemeral;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.RawMessageStreamEvent;
import com.anthropic.models.messages.TextBlockParam;
import com.anthropic.models.messages.ThinkingConfigAdaptive;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnthropicEarningsAnalysisAdapter implements EarningsAnalysisPort {

    private static final String SYSTEM_PROMPT = """
            Você é um analista financeiro especializado em empresas listadas na B3 (Bolsa de Valores do Brasil).
            Analise relatórios de resultados trimestrais e anuais com precisão, objetividade e foco em dados.
            Responda sempre em português brasileiro com análises estruturadas, claras e baseadas nos números apresentados.
            Quando um dado não estiver disponível no relatório, indique explicitamente.
            """;

    private final AnthropicClient anthropicClient;

    @Override
    public String analyze(String reportText, String ticker) {
        MessageCreateParams params = MessageCreateParams.builder()
                .model("claude-opus-4-7")
                .maxTokens(8192L)
                .thinking(ThinkingConfigAdaptive.builder().build())
                .outputConfig(OutputConfig.builder()
                        .effort(OutputConfig.Effort.HIGH)
                        .build())
                .systemOfTextBlockParams(List.of(
                        TextBlockParam.builder()
                                .text(SYSTEM_PROMPT)
                                .cacheControl(CacheControlEphemeral.builder().build())
                                .build()))
                .addUserMessage(buildPrompt(ticker, reportText))
                .build();

        StringBuilder result = new StringBuilder();
        try (StreamResponse<RawMessageStreamEvent> stream = anthropicClient.messages().createStreaming(params)) {
            stream.stream()
                    .flatMap(event -> event.contentBlockDelta().stream())
                    .flatMap(delta -> delta.delta().text().stream())
                    .forEach(text -> result.append(text.text()));
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao analisar relatório com Claude: " + e.getMessage(), e);
        }
        return result.toString();
    }

    private String buildPrompt(String ticker, String reportText) {
        return """
                Analise o relatório de resultados abaixo da empresa %s e forneça uma análise estruturada:

                ## 1. Resumo Executivo
                Principais destaques do período reportado.

                ## 2. Métricas Financeiras Chave
                Receita Líquida, EBITDA, Margem EBITDA, Lucro Líquido, Margem Líquida, Dívida Líquida/EBITDA.

                ## 3. Variação vs Períodos Anteriores
                Crescimento ou queda em relação ao trimestre anterior (QoQ) e mesmo período do ano anterior (YoY).

                ## 4. Guidance e Projeções
                Expectativas da empresa para os próximos períodos (se divulgado no relatório).

                ## 5. Pontos Positivos
                Destaques favoráveis do resultado.

                ## 6. Pontos de Atenção
                Riscos, pontos negativos ou aspectos que merecem acompanhamento.

                ## 7. Conclusão
                Avaliação geral do resultado e perspectivas para a empresa.

                ---
                RELATÓRIO:
                %s
                """.formatted(ticker, reportText);
    }
}
