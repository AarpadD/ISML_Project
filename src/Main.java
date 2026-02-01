import jade.core.Runtime;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;


public class Main {
    
    public static void main(String[] args) {
        try {
            // Get JADE runtime instance
            Runtime runtime = Runtime.instance();
            
            // Create a default profile
            Profile profile = new ProfileImpl();
            profile.setParameter(Profile.MAIN_HOST, "localhost");
            profile.setParameter(Profile.MAIN_PORT, "1099");
            profile.setParameter(Profile.GUI, "true");
            
            // Create main container
            AgentContainer mainContainer = runtime.createMainContainer(profile);
            
            System.out.println("==============================================");
            System.out.println("  JADE Inventory Management System");
            System.out.println("==============================================\n");
            
            // Create and start Supplier Agent
            AgentController supplierAgent = mainContainer.createNewAgent(
                "SupplierAgent", 
                "SupplierAgent", 
                null
            );
            supplierAgent.start();
            
            // Wait a bit for supplier to register
            Thread.sleep(500);
            
            // Create and start Inventory Agent
            AgentController inventoryAgent = mainContainer.createNewAgent(
                "InventoryAgent", 
                "InventoryAgent", 
                null
            );
            inventoryAgent.start();
            
            // Wait for inventory to initialize
            Thread.sleep(1000);
            
            // Create and start Buyer Agent
            AgentController buyerAgent = mainContainer.createNewAgent(
                "BuyerAgent", 
                "BuyerAgent", 
                null
            );
            buyerAgent.start();
            
            System.out.println("\n==============================================");
            System.out.println("  All agents started successfully!");
            System.out.println("  Check the JADE GUI for agent interactions");
            System.out.println("==============================================\n");
            
        } catch (StaleProxyException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}