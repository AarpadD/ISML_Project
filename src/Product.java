import java.io.Serializable;

public class Product implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String productId;
    private String name;
    private int quantity;
    private int threshold;
    private double price;
    
    public Product(String productId, String name, int quantity, int threshold, double price) {
        this.productId = productId;
        this.name = name;
        this.quantity = quantity;
        this.threshold = threshold;
        this.price = price;
    }
    
    public String getProductId() {
        return productId;
    }
    
    public String getName() {
        return name;
    }
    
    public int getQuantity() {
        return quantity;
    }
    
    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
    
    public int getThreshold() {
        return threshold;
    }
    
    public double getPrice() {
        return price;
    }
    
    public boolean needsRestock() {
        return quantity < threshold;
    }
    
    @Override
    public String toString() {
        return String.format("Product[ID=%s, Name=%s, Quantity=%d, Threshold=%d, Price=%.2f]",
                productId, name, quantity, threshold, price);
    }
}