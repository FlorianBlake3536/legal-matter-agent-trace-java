package example.legaltech.service;

import example.legaltech.domain.MatterIntakeRequest;

public class SignedPacketDelivery {
    public void deliver(MatterIntakeRequest request) {
        if (request.deliveryFails()) {
            throw new IllegalStateException("Signed packet delivery was rejected by the destination.");
        }
    }
}
