package example.legaltech.service;

import example.legaltech.domain.MatterDeliveryStatus;
import example.legaltech.domain.MatterFollowUpAction;
import example.legaltech.domain.MatterIntakeRequest;
import example.legaltech.domain.MatterWorkflowResult;
import example.legaltech.infrai.InfraiTelemetryClient;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class MatterWorkflowService {
    private final OpenAiMatterIntakeWriter intakeWriter;
    private final InfraiTelemetryClient telemetryClient;
    private final SignedPacketDelivery signedPacketDelivery;

    public MatterWorkflowService(OpenAiMatterIntakeWriter intakeWriter,
                                 InfraiTelemetryClient telemetryClient,
                                 SignedPacketDelivery signedPacketDelivery) {
        this.intakeWriter = intakeWriter;
        this.telemetryClient = telemetryClient;
        this.signedPacketDelivery = signedPacketDelivery;
    }

    public MatterWorkflowResult process(MatterIntakeRequest request) {
        String note = intakeWriter.writeIntakeNote(request);
        telemetryClient.countTokens(note);

        try {
            signedPacketDelivery.deliver(request);
            telemetryClient.reportWorkflowMetric(request.matterId(), "signed_delivery", "delivered");
            MatterFollowUpAction action = decideFollowUp(request.filingDeadline(), false);
            return new MatterWorkflowResult(request.matterId(), note, MatterDeliveryStatus.DELIVERED, action);
        } catch (RuntimeException exception) {
            telemetryClient.captureException(exception, request.matterId());
            telemetryClient.reportWorkflowMetric(request.matterId(), "signed_delivery", "delivery_failed");
            MatterFollowUpAction action = decideFollowUp(request.filingDeadline(), true);
            return new MatterWorkflowResult(request.matterId(), note, MatterDeliveryStatus.DELIVERY_FAILED, action);
        }
    }

    MatterFollowUpAction decideFollowUp(LocalDate filingDeadline, boolean deliveryFailed) {
        if (!deliveryFailed) {
            return MatterFollowUpAction.NONE;
        }
        long daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), filingDeadline);
        if (daysLeft <= 2) {
            return MatterFollowUpAction.URGENT;
        }
        if (daysLeft <= 7) {
            return MatterFollowUpAction.SCHEDULED;
        }
        return MatterFollowUpAction.NONE;
    }
}
