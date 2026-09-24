package example.legaltech.service;

import com.openai.client.OpenAIClient;
import com.openai.models.ChatCompletionCreateParams;
import example.legaltech.domain.MatterIntakeRequest;

public class OpenAiMatterIntakeWriter {
    private final OpenAIClient openAIClient;

    public OpenAiMatterIntakeWriter(OpenAIClient openAIClient) {
        this.openAIClient = openAIClient;
    }

    public String writeIntakeNote(MatterIntakeRequest request) {
        String prompt = "Write a concise legal matter intake note for client " + request.clientName()
                + ". Matter summary: " + request.matterSummary()
                + ". Mention the signed packet destination and the filing deadline.";

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .model("auto")
                .addUserMessage(prompt)
                .build();

        return openAIClient.chat().completions().create(params)
                .choices().get(0)
                .message()
                .content().orElse("");
    }
}
