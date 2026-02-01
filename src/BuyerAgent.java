import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class BuyerAgent extends Agent {
    private AID inventoryAgent;
    private BuyerGUI myGui;

    protected void setup() {
        System.out.println("BuyerAgent " + getAID().getName() + " is ready.");

        // Create and show GUI
        myGui = new BuyerGUI(this);
        myGui.setVisible(true);

        // Search for inventory agent
        addBehaviour(new OneShotBehaviour() {
            public void action() {
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("inventory-management");
                template.addServices(sd);

                try {
                    DFAgentDescription[] result = DFService.search(myAgent, template);
                    if (result.length > 0) {
                        inventoryAgent = result[0].getName();
                        System.out.println("Found Inventory Agent: " + inventoryAgent.getName());
                        myGui.logMessage("✓ Connected to Inventory System");

                        // Request product list
                        requestProductList();
                    } else {
                        System.out.println("Inventory Agent not found!");
                        myGui.logMessage("✗ Inventory Agent not found!");
                    }
                } catch (FIPAException fe) {
                    fe.printStackTrace();
                }
            }
        });
    }

    public void requestProductList() {
        addBehaviour(new OneShotBehaviour() {
            public void action() {
                ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
                request.addReceiver(inventoryAgent);
                request.setContent("GET_PRODUCTS");
                request.setConversationId("product-list");
                request.setReplyWith("list-" + System.currentTimeMillis());
                myAgent.send(request);

                // Wait for response
                addBehaviour(new ReceiveProductListBehaviour());
            }
        });
    }

    public void makePurchase(String productId, int quantity) {
        addBehaviour(new PurchaseRequestBehaviour(productId, quantity));
    }

    private class ReceiveProductListBehaviour extends Behaviour {
        private boolean finished = false;

        public void action() {
            MessageTemplate mt = MessageTemplate.MatchConversationId("product-list");
            ACLMessage reply = myAgent.receive(mt);

            if (reply != null) {
                if (reply.getPerformative() == ACLMessage.INFORM) {
                    String content = reply.getContent();
                    myGui.updateProductList(content);
                }
                finished = true;
            } else {
                block();
            }
        }

        public boolean done() {
            return finished;
        }
    }

    private class PurchaseRequestBehaviour extends Behaviour {
        private int step = 0;
        private MessageTemplate mt;
        private String requestId;
        private String productId;
        private int quantity;

        public PurchaseRequestBehaviour(String productId, int quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }

        public void action() {
            switch (step) {
                case 0:
                    ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
                    request.addReceiver(inventoryAgent);
                    request.setContent("PURCHASE:" + productId + ":" + quantity);
                    request.setConversationId("purchase-request");
                    requestId = "purchase-" + System.currentTimeMillis();
                    request.setReplyWith(requestId);
                    myAgent.send(request);

                    myGui.logMessage("Sending purchase request: " + quantity + "x " + productId);

                    mt = MessageTemplate.and(
                        MessageTemplate.MatchConversationId("purchase-request"),
                        MessageTemplate.MatchInReplyTo(requestId)
                    );
                    step = 1;
                    break;

                case 1:
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            myGui.logMessage("✓ " + reply.getContent());
                            JOptionPane.showMessageDialog(myGui,
                                reply.getContent(),
                                "Purchase Successful",
                                JOptionPane.INFORMATION_MESSAGE);
                        } else if (reply.getPerformative() == ACLMessage.FAILURE) {
                            myGui.logMessage("✗ " + reply.getContent());
                            JOptionPane.showMessageDialog(myGui,
                                reply.getContent(),
                                "Purchase Failed",
                                JOptionPane.ERROR_MESSAGE);
                        }
                        // Refresh product list
                        requestProductList();
                        step = 2;
                    } else {
                        block();
                    }
                    break;
            }
        }

        public boolean done() {
            return step == 2;
        }
    }

    protected void takeDown() {
        if (myGui != null) {
            myGui.dispose();
        }
        System.out.println("BuyerAgent " + getAID().getName() + " terminating.");
    }

    // GUI Class
    class BuyerGUI extends JFrame {
        private BuyerAgent myAgent;
        private JTextArea logArea;
        private JPanel productsPanel;

        public BuyerGUI(BuyerAgent a) {
            super("Buyer Agent - Inventory System");
            myAgent = a;

            setLayout(new BorderLayout(10, 10));
            setSize(700, 500);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // Products Panel
            JPanel topPanel = new JPanel(new BorderLayout());
            topPanel.setBorder(BorderFactory.createTitledBorder("Available Products"));
            productsPanel = new JPanel();
            productsPanel.setLayout(new BoxLayout(productsPanel, BoxLayout.Y_AXIS));
            JScrollPane productsScroll = new JScrollPane(productsPanel);
            productsScroll.setPreferredSize(new Dimension(680, 250));
            topPanel.add(productsScroll, BorderLayout.CENTER);

            // Log Panel
            JPanel bottomPanel = new JPanel(new BorderLayout());
            bottomPanel.setBorder(BorderFactory.createTitledBorder("Activity Log"));
            logArea = new JTextArea(10, 50);
            logArea.setEditable(false);
            logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            JScrollPane logScroll = new JScrollPane(logArea);
            bottomPanel.add(logScroll, BorderLayout.CENTER);

            // Refresh Button
            JButton refreshBtn = new JButton("Refresh Products");
            refreshBtn.addActionListener(e -> myAgent.requestProductList());
            bottomPanel.add(refreshBtn, BorderLayout.SOUTH);

            add(topPanel, BorderLayout.CENTER);
            add(bottomPanel, BorderLayout.SOUTH);

            setLocationRelativeTo(null);
        }

        public void updateProductList(String productData) {
            SwingUtilities.invokeLater(() -> {
                productsPanel.removeAll();

                String[] products = productData.split("\\|");
                for (String prod : products) {
                    if (!prod.trim().isEmpty()) {
                        String[] parts = prod.split(",");
                        if (parts.length == 5) {
                            String id = parts[0];
                            String name = parts[1];
                            int quantity = Integer.parseInt(parts[2]);
                            int threshold = Integer.parseInt(parts[3]);
                            double price = Double.parseDouble(parts[4]);

                            JPanel productPanel = createProductPanel(id, name, quantity, threshold, price);
                            productsPanel.add(productPanel);
                            productsPanel.add(Box.createRigidArea(new Dimension(0, 5)));
                        }
                    }
                }

                productsPanel.revalidate();
                productsPanel.repaint();
            });
        }

        private JPanel createProductPanel(String id, String name, int quantity, int threshold, double price) {
            JPanel panel = new JPanel(new BorderLayout(10, 5));
            panel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            panel.setMaximumSize(new Dimension(650, 80));

            // Product Info
            JPanel infoPanel = new JPanel(new GridLayout(3, 1));
            JLabel nameLabel = new JLabel(name + " (" + id + ")");
            nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
            JLabel stockLabel = new JLabel("Stock: " + quantity + " units (Threshold: " + threshold + ")");
            JLabel priceLabel = new JLabel("Price: $" + String.format("%.2f", price));

            if (quantity < threshold) {
                stockLabel.setForeground(Color.RED);
            } else if (quantity < threshold * 1.5) {
                stockLabel.setForeground(Color.ORANGE);
            } else {
                stockLabel.setForeground(new Color(0, 150, 0));
            }

            infoPanel.add(nameLabel);
            infoPanel.add(stockLabel);
            infoPanel.add(priceLabel);

            // Purchase Controls
            JPanel controlPanel = new JPanel(new FlowLayout());
            JLabel qtyLabel = new JLabel("Quantity:");
            SpinnerModel spinnerModel = new SpinnerNumberModel(0, 0, quantity > 0 ? quantity : 1, 1);
            JSpinner quantitySpinner = new JSpinner(spinnerModel);
            quantitySpinner.setPreferredSize(new Dimension(60, 25));

            JButton buyButton = new JButton("Buy");
            buyButton.setEnabled(quantity > 0);
            buyButton.addActionListener(e -> {
                int qty = (Integer) quantitySpinner.getValue();
                if (qty > 0) {
                    myAgent.makePurchase(id, qty);
                } else {
                    JOptionPane.showMessageDialog(panel,
                        "To have a successful purchase, review the quantity.\nQuantity must be greater than 0.",
                        "Invalid Quantity",
                        JOptionPane.WARNING_MESSAGE);
                }
            });

            controlPanel.add(qtyLabel);
            controlPanel.add(quantitySpinner);
            controlPanel.add(buyButton);

            panel.add(infoPanel, BorderLayout.CENTER);
            panel.add(controlPanel, BorderLayout.EAST);

            return panel;
        }

        public void logMessage(String msg) {
            SwingUtilities.invokeLater(() -> {
                logArea.append("[" + java.time.LocalTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")) + "] " + msg + "\n");
                logArea.setCaretPosition(logArea.getDocument().getLength());
            });
        }
    }
}