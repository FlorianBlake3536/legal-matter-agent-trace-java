package example.legaltech.domain;

public record MatterWorkflowResult(
        String matterId,
        String intakeNote,
        MatterDeliveryStatus deliveryStatus,
        MatterFollowUpAction followUpAction
) {
}
