package br.com.investmentadvisor.infrastructure.adapter.out.ai;

import br.com.investmentadvisor.domain.port.out.EarningsAnalysisPort;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LangChainEarningsAnalysisAdapter implements EarningsAnalysisPort {

    private static final String SYSTEM_PROMPT = """
            Você é um analista financeiro especializado em empresas listadas na B3 (bolsa de valores brasileira).
            Sua tarefa é analisar relatórios de resultados trimestrais e anuais de empresas.
            Forneça uma análise clara, objetiva e estruturada, destacando:
            - Principais métricas financeiras (receita, lucro, EBITDA, margem)
            - Comparação com períodos anteriores (crescimento ou queda)
            - Pontos positivos e negativos do resultado
            - Perspectivas e guidance se disponíveis
            - Conclusão sobre a saúde financeira da empresa
            - Mostra a pagina que você encontrou os dados apresentados também.
            Responda sempre em português brasileiro.
            """;

    private final ChatLanguageModel chatLanguageModel;

    @Override
    public String analyze(String reportText, String ticker) {
        String userPrompt = """
                Analise o seguinte relatório de resultados da empresa %s:

                %s
                """.formatted(ticker, truncate(reportText, 50_000));

        Response<AiMessage> response = chatLanguageModel.generate(
                SystemMessage.from(SYSTEM_PROMPT),
                UserMessage.from(userPrompt)
        );

        return response.content().text();
    }

    private String truncate(String text, int maxChars) {
        return text.length() <= maxChars ? text : text.substring(0, maxChars) + "\n[...texto truncado...]";
    }
}
