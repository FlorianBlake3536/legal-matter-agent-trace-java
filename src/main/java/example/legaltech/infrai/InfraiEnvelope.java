package example.legaltech.infrai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public record InfraiEnvelope(
        boolean ok,
        JsonNode data,
        JsonNode error,
        JsonNode metadata
) {
}
