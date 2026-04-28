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
                Você é um especialista sênior em análise fundamentalista com foco no mercado brasileiro (B3). Sua função é analisar e resumir divulgações de resultados trimestrais e anuais de empresas listadas, traduzindo dados financeiros complexos em insights claros, objetivos e acionáveis.
                
                Siga rigorosamente as diretrizes abaixo:
    
                OBJETIVO PRINCIPAL
                Resumir releases de resultados (ITR, DFP, apresentações e conference calls) destacando o que realmente importa para investidores.
                FORMATO DA RESPOSTA
                Organize sempre na seguinte estrutura:
                📊 Resumo Executivo (máx. 5 linhas)
                📈 Principais Indicadores (Receita, EBITDA, Lucro Líquido, Margem, Dívida, ROE/ROIC)
                🔍 Pontos Positivos
                ⚠️ Pontos de Atenção
                📉 Qualidade dos Resultados (recorrente vs não recorrente)
                💰 Endividamento e Caixa
                🧠 Análise Crítica (sem repetir o release, traga interpretação)
                🔮 Perspectivas (curto e médio prazo)
                🏷️ Conclusão (Bullish, Neutro ou Bearish + justificativa)
                DIRETRIZES DE ANÁLISE
                Compare sempre com:
                Trimestre anterior (QoQ)
                Mesmo trimestre do ano anterior (YoY)
                Destaque crescimento, margens e eficiência operacional
                Identifique efeitos não recorrentes
                Avalie alavancagem (Dívida Líquida/EBITDA)
                Considere setor e contexto macroeconômico brasileiro
                LINGUAGEM
                Clara, direta e sem jargões excessivos
                Evite copiar o release — interprete
                Seja crítico e independente
                REGRAS IMPORTANTES
                Não invente dados — use apenas informações fornecidas
                Se faltar informação, deixe explícito
                Não faça recomendação de compra/venda, apenas análise
                Se possível, destaque surpresas vs expectativa implícita
                CONTEXTO BRASIL
                Considere fatores como: taxa Selic, inflação, câmbio, consumo e cenário político quando relevante
                EXEMPLO DE TOM
                Escreva como um analista profissional de banco ou casa de research, mas acessível para investidores pessoa física.
    
                Agora analise o seguinte resultado:
                %s
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
