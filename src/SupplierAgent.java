import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class SupplierAgent extends Agent {
    
    protected void setup() {
        System.out.println("SupplierAgent " + getAID().getName() + " is ready.");
        
        // Register with DF (Directory Facilitator)
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("supplier-service");
        sd.setName("supplier-restock");
        dfd.addServices(sd);
        
        try {
            DFService.register(this, dfd);
            System.out.println("[SUPPLIER] Registered with Directory Facilitator");
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        
        // Add behaviour to handle restock requests
        addBehaviour(new RestockRequestServer());
    }
    
    /**
     * Behaviour to handle restock requests from inventory
     */
    private class RestockRequestServer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                MessageTemplate.MatchConversationId("restock-request")
            );
            ACLMessage msg = myAgent.receive(mt);
            
            if (msg != null) {
                String content = msg.getContent();
                System.out.println("[SUPPLIER] Received restock request: " + content);
                
                // Parse request: RESTOCK:productId:quantity
                String[] parts = content.split(":");
                if (parts[0].equals("RESTOCK") && parts.length == 3) {
                    String productId = parts[1];
                    int quantity = Integer.parseInt(parts[2]);
                    
                    // Simulate processing time
                    doWait(1000);
                    
                    // Send confirmation back to inventory
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent("RESTOCKED:" + productId + ":" + quantity);
                    myAgent.send(reply);
                    
                    System.out.println(String.format("[SUPPLIER] Restock completed: %d units of %s",
                            quantity, productId));
                }
            } else {
                block();
            }
        }
    }
    
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("SupplierAgent " + getAID().getName() + " terminating.");
    }
}