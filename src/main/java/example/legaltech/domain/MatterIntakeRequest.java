package example.legaltech.domain;

import java.time.LocalDate;

public record MatterIntakeRequest(
        String matterId,
        String clientName,
        String matterSummary,
        String signedPacketDestination,
        LocalDate filingDeadline,
        boolean deliveryFails
) {
}
