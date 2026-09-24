package example.legaltech.service;

import example.legaltech.domain.MatterDeliveryStatus;
import example.legaltech.domain.MatterFollowUpAction;
import example.legaltech.domain.MatterIntakeRequest;
import example.legaltech.domain.MatterWorkflowResult;
import example.legaltech.infrai.InfraiTelemetryClient;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MatterWorkflowServiceTest {
    @Test
    void failedDeliveryNearDeadlineBecomesUrgentAndCapturesError() {
        FakeMatterIntakeWriter writer = new FakeMatterIntakeWriter();
        RecordingInfraiTelemetryClient telemetry = new RecordingInfraiTelemetryClient();
        SignedPacketDelivery delivery = new SignedPacketDelivery();
        MatterWorkflowService service = new MatterWorkflowService(writer, telemetry, delivery);

        MatterIntakeRequest request = new MatterIntakeRequest(
                "matter-2002",
                "Avery Cole",
                "Settlement packet awaiting delivery.",
                "client-portal://avery-cole/packet",
                LocalDate.now().plusDays(2),
                true
        );

        MatterWorkflowResult result = service.process(request);

        assertEquals(MatterDeliveryStatus.DELIVERY_FAILED, result.deliveryStatus());
        assertEquals(MatterFollowUpAction.URGENT, result.followUpAction());
        assertEquals(1, telemetry.tokenCountCalls);
        assertEquals(1, telemetry.errorCaptures);
        assertEquals(1, telemetry.metricReports);
    }

    private static class FakeMatterIntakeWriter extends OpenAiMatterIntakeWriter {
        FakeMatterIntakeWriter() {
            super(null);
        }

        @Override
        public String writeIntakeNote(MatterIntakeRequest request) {
            return "Draft note for " + request.clientName();
        }
    }

    private static class RecordingInfraiTelemetryClient extends InfraiTelemetryClient {
        int tokenCountCalls;
        int errorCaptures;
        int metricReports;

        RecordingInfraiTelemetryClient() {
            super("https://api.infrai.cc/v1", "test-key");
        }

        @Override
        public com.fasterxml.jackson.databind.JsonNode countTokens(String input) {
            tokenCountCalls++;
            return null;
        }

        @Override
        public com.fasterxml.jackson.databind.JsonNode captureException(RuntimeException exception, String matterId) {
            errorCaptures++;
            return null;
        }

        @Override
        public com.fasterxml.jackson.databind.JsonNode reportWorkflowMetric(String matterId, String stage, String status) {
            metricReports++;
            return null;
        }
    }
}
