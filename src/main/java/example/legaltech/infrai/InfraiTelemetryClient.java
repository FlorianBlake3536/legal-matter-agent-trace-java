package example.legaltech.infrai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class InfraiTelemetryClient {
    private final String baseUrl;
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public InfraiTelemetryClient(String baseUrl, String apiKey) {
        this(baseUrl, apiKey, HttpClient.newHttpClient(), new ObjectMapper());
    }

    public InfraiTelemetryClient(String baseUrl, String apiKey, HttpClient httpClient, ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public JsonNode countTokens(String input) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", "auto");
        body.putArray("messages").addObject().put("role", "user").put("content", input);
        return postJson("/v1/ai/tokens/count", body, 0);
    }

    public JsonNode captureException(RuntimeException exception, String matterId) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("title", exception.getClass().getSimpleName());
        payload.put("message", exception.getMessage());
        payload.putObject("tags").put("matterId", matterId);
        return postJson("/v1/errors/capture", payload, 0);
    }

    public JsonNode reportWorkflowMetric(String matterId, String stage, String status) {
        ObjectNode payload = objectMapper.createObjectNode();
        payload.put("name", "matter.workflow." + stage);
        payload.put("value", 1);
        payload.put("type", "counter");
        payload.putObject("tags").put("matterId", matterId).put("status", status);
        return postJson("/v1/metrics/report", payload, 0);
    }

    private JsonNode postJson(String path, JsonNode body, int attempt) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            InfraiEnvelope envelope = parseEnvelope(response.body(), response.statusCode());
            if (!envelope.ok()) {
                throw new InfraiException(envelope.error() == null ? "Infrai request was rejected." : envelope.error().toString(), response.statusCode());
            }
            if (response.statusCode() == 429 && attempt < 3) {
                long delayMillis = retryDelayMillis(response, attempt);
                sleep(delayMillis);
                return postJson(path, body, attempt + 1);
            }
            if (response.statusCode() >= 500) {
                throw new InfraiException("Transport request failed.", response.statusCode());
            }
            return envelope.data();
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InfraiException(e.getMessage(), 0);
        }
    }

    private InfraiEnvelope parseEnvelope(String body, int statusCode) throws JsonProcessingException {
        InfraiEnvelope envelope = objectMapper.readValue(body, InfraiEnvelope.class);
        if (statusCode == 429 && envelope.ok()) {
            return envelope;
        }
        return envelope;
    }

    private long retryDelayMillis(HttpResponse<String> response, int attempt) {
        return response.headers()
                .firstValue("Retry-After")
                .map(value -> Long.parseLong(value) * 1000L)
                .orElse((long) Math.pow(2, attempt) * 250L);
    }

    private void sleep(long delayMillis) throws InterruptedException {
        Thread.sleep(delayMillis);
    }
}
