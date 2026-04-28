package br.com.investmentadvisor.infrastructure.config;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class LlmConfig {

    @Bean
    @ConditionalOnProperty(name = "llm.provider", havingValue = "ollama", matchIfMissing = true)
    public ChatLanguageModel ollamaChatModel(
            @Value("${llm.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${llm.ollama.model:llama3.2}") String model) {
        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(model)
                .timeout(Duration.ofMinutes(5))
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "llm.provider", havingValue = "openai")
    public ChatLanguageModel openAiChatModel(
            @Value("${llm.openai.api-key}") String apiKey,
            @Value("${llm.openai.model:gpt-4o-mini}") String model) {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(model)
                .timeout(Duration.ofMinutes(5))
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "llm.provider", havingValue = "anthropic")
    public ChatLanguageModel anthropicChatModel(
            @Value("${llm.anthropic.api-key}") String apiKey,
            @Value("${llm.anthropic.model:claude-haiku-4-5}") String model) {
        return AnthropicChatModel.builder()
                .apiKey(apiKey)
                .modelName(model)
                .timeout(Duration.ofMinutes(5))
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "llm.provider", havingValue = "gemini")
    public ChatLanguageModel geminiChatModel(
            @Value("${llm.gemini.api-key}") String apiKey,
            @Value("${llm.gemini.model:gemini-2.0-flash}") String model) {
        return GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(model)
                .timeout(Duration.ofMinutes(5))
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "llm.provider", havingValue = "deepseek")
    public ChatLanguageModel deepSeekChatModel(
            @Value("${llm.deepseek.api-key}") String apiKey,
            @Value("${llm.deepseek.base-url:https://api.deepseek.com/v1}") String baseUrl,
            @Value("${llm.deepseek.model:deepseek-chat}") String model) {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(model)
                .timeout(Duration.ofMinutes(5))
                .build();
    }
}
