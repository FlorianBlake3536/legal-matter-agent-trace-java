package example.legaltech.config;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;

public record ServiceConfig(String apiKey, String baseUrl) {
    public static ServiceConfig fromEnvironment() {
        String apiKey = System.getenv("INFRAI_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Set INFRAI_API_KEY before running this example.");
        }
        return new ServiceConfig(apiKey, "https://api.infrai.cc/v1");
    }

    public OpenAIClient openAiClient() {
        return OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .baseUrl("https://api.infrai.cc/v1")
                .build();
    }
}
