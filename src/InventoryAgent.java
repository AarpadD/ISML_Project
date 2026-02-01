import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.util.HashMap;
import java.util.Map;
import java.io.*;

public class InventoryAgent extends Agent {
    private Map<String, Product> inventory;
    private AID supplierAgent;
    private static final String INVENTORY_FILE = "inventory.dat";

    protected void setup() {
        System.out.println("InventoryAgent " + getAID().getName() + " is ready.");

        // Load inventory from file or initialize
        loadInventory();

        System.out.println("[INVENTORY] Current inventory:");
        for (Product p : inventory.values()) {
            System.out.println("  " + p);
        }

        // Register with DF
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("inventory-management");
        sd.setName("inventory-service");
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        // Search for supplier agent
        addBehaviour(new OneShotBehaviour() {
            public void action() {
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("supplier-service");
                template.addServices(sd);

                try {
                    DFAgentDescription[] result = DFService.search(myAgent, template);
                    if (result.length > 0) {
                        supplierAgent = result[0].getName();
                        System.out.println("[INVENTORY] Found Supplier Agent: " + supplierAgent.getName());
                    } else {
                        System.out.println("[INVENTORY] Supplier Agent not found yet, will retry...");
                        myAgent.doWait(2000);
                        myAgent.addBehaviour(this);
                    }
                } catch (FIPAException fe) {
                    fe.printStackTrace();
                }
            }
        });

        addBehaviour(new PurchaseRequestServer());
        addBehaviour(new RestockConfirmationServer());
        addBehaviour(new ProductListServer());
    }

    private void loadInventory() {
        File file = new File(INVENTORY_FILE);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                inventory = (Map<String, Product>) ois.readObject();
                System.out.println("[INVENTORY] Loaded inventory from file");
            } catch (Exception e) {
                System.err.println("[INVENTORY] Error loading inventory: " + e.getMessage());
                initializeInventory();
            }
        } else {
            initializeInventory();
        }
    }

    private void initializeInventory() {
        inventory = new HashMap<>();
        inventory.put("LAPTOP001", new Product("LAPTOP001", "Dell Laptop", 5, 3, 999.99));
        inventory.put("MOUSE001", new Product("MOUSE001", "Logitech Mouse", 15, 5, 29.99));
        inventory.put("KEYBOARD001", new Product("KEYBOARD001", "Mechanical Keyboard", 8, 4, 89.99));
        System.out.println("[INVENTORY] Initialized new inventory");
        saveInventory();
    }

    private void saveInventory() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(INVENTORY_FILE))) {
            oos.writeObject(inventory);
            System.out.println("[INVENTORY] Saved inventory to file");
        } catch (Exception e) {
            System.err.println("[INVENTORY] Error saving inventory: " + e.getMessage());
        }
    }

    private class ProductListServer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchContent("GET_PRODUCTS");
            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.INFORM);

                StringBuilder sb = new StringBuilder();
                for (Product p : inventory.values()) {
                    sb.append(p.getProductId()).append(",");
                    sb.append(p.getName()).append(",");
                    sb.append(p.getQuantity()).append(",");
                    sb.append(p.getThreshold()).append(",");
                    sb.append(p.getPrice()).append("|");
                }

                reply.setContent(sb.toString());
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    private class PurchaseRequestServer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {
                String content = msg.getContent();

                if (content.startsWith("PURCHASE:")) {
                    System.out.println("[INVENTORY] Received request: " + content);

                    String[] parts = content.split(":");
                    if (parts.length == 3) {
                        String productId = parts[1];
                        int quantity = Integer.parseInt(parts[2]);

                        ACLMessage reply = msg.createReply();

                        Product product = inventory.get(productId);
                        if (product != null) {
                            if (product.getQuantity() >= quantity) {
                                product.setQuantity(product.getQuantity() - quantity);
                                reply.setPerformative(ACLMessage.INFORM);
                                reply.setContent(String.format("Purchase successful! %d x %s. Remaining stock: %d",
                                        quantity, product.getName(), product.getQuantity()));

                                System.out.println(String.format("[INVENTORY] Purchase processed: %d x %s (Stock: %d)",
                                        quantity, product.getName(), product.getQuantity()));

                                saveInventory();

                                if (product.needsRestock()) {
                                    requestRestock(product);
                                }
                            } else {
                                reply.setPerformative(ACLMessage.FAILURE);
                                reply.setContent(String.format("Insufficient stock! Available: %d, Requested: %d",
                                        product.getQuantity(), quantity));

                                System.out.println(String.format("[INVENTORY] Insufficient stock for %s (Available: %d, Requested: %d)",
                                        product.getName(), product.getQuantity(), quantity));
                            }
                        } else {
                            reply.setPerformative(ACLMessage.FAILURE);
                            reply.setContent("Product not found: " + productId);
                            System.out.println("[INVENTORY] Product not found: " + productId);
                        }

                        myAgent.send(reply);
                    }
                }
            } else {
                block();
            }
        }
    }

    private void requestRestock(Product product) {
        if (supplierAgent != null) {
            int restockAmount = product.getThreshold() * 2;

            ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
            request.addReceiver(supplierAgent);
            request.setContent("RESTOCK:" + product.getProductId() + ":" + restockAmount);
            request.setConversationId("restock-request");
            request.setReplyWith("restock-" + System.currentTimeMillis());
            send(request);

            System.out.println(String.format("[INVENTORY] Stock below threshold! Requesting restock: %d x %s",
                    restockAmount, product.getName()));
        }
    }

    private class RestockConfirmationServer extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.and(
                MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchConversationId("restock-request")
            );
            ACLMessage msg = myAgent.receive(mt);

            if (msg != null) {
                String content = msg.getContent();
                String[] parts = content.split(":");
                if (parts[0].equals("RESTOCKED") && parts.length == 3) {
                    String productId = parts[1];
                    int quantity = Integer.parseInt(parts[2]);

                    Product product = inventory.get(productId);
                    if (product != null) {
                        product.setQuantity(product.getQuantity() + quantity);
                        System.out.println(String.format("[INVENTORY] Stock replenished: %d x %s (New stock: %d)",
                                quantity, product.getName(), product.getQuantity()));

                        saveInventory();
                    }
                }
            } else {
                block();
            }
        }
    }

    protected void takeDown() {
        saveInventory();
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("InventoryAgent " + getAID().getName() + " terminating.");
    }
}