package example.legaltech;

import example.legaltech.config.ServiceConfig;
import example.legaltech.domain.MatterFollowUpAction;
import example.legaltech.domain.MatterIntakeRequest;
import example.legaltech.domain.MatterWorkflowResult;
import example.legaltech.infrai.InfraiTelemetryClient;
import example.legaltech.service.MatterWorkflowService;
import example.legaltech.service.OpenAiMatterIntakeWriter;
import example.legaltech.service.SignedPacketDelivery;

import java.time.LocalDate;

public class MatterWorkflowApplication {
    public static void main(String[] args) {
        ServiceConfig config = ServiceConfig.fromEnvironment();
        OpenAiMatterIntakeWriter writer = new OpenAiMatterIntakeWriter(config.openAiClient());
        InfraiTelemetryClient telemetryClient = new InfraiTelemetryClient(config.baseUrl(), config.apiKey());
        SignedPacketDelivery delivery = new SignedPacketDelivery();
        MatterWorkflowService workflow = new MatterWorkflowService(writer, telemetryClient, delivery);

        MatterIntakeRequest input = new MatterIntakeRequest(
                "matter-1001",
                "Avery Cole",
                "Lease dispute with signed settlement addendum ready for client delivery.",
                "client-portal://avery-cole/packet",
                LocalDate.now().plusDays(2),
                true
        );

        MatterWorkflowResult result = workflow.process(input);

        System.out.println("matterId=" + result.matterId());
        System.out.println("deliveryStatus=" + result.deliveryStatus());
        System.out.println("followUpAction=" + result.followUpAction());
        System.out.println("draftedNote=" + result.intakeNote());
        if (result.followUpAction() == MatterFollowUpAction.URGENT) {
            System.out.println("Next step: call client and confirm alternate delivery route today.");
        }
    }
}
