package models;

public class TransactionItem {
    private Product product;
    private String  productId;
    private String  productName;
    private int     qty;
    private double  priceAtTime;
    private double  discountedPrice;

    public TransactionItem(Product product, int qty, String memberTier) {
        this.product      = product;
        this.productId    = product.getId();
        this.productName  = product.getName();
        this.qty          = qty;
        this.priceAtTime  = product.getFinalPrice();

        if (memberTier != null && !"NONE".equals(memberTier) && product instanceof Discountable) {
            this.discountedPrice = ((Discountable) product).calculateMemberDiscount(memberTier);
        } else {
            this.discountedPrice = priceAtTime;
        }
    }
    public TransactionItem(Product product, int qty, double priceAtTime, double discountedPrice) {
        this.product         = product;
        this.productId       = product.getId();
        this.productName     = product.getName();
        this.qty             = qty;
        this.priceAtTime     = priceAtTime;
        this.discountedPrice = discountedPrice;
    }
    public TransactionItem(String productId, String productName,
                           int qty, double priceAtTime, double discountedPrice) {
        this.product         = null;
        this.productId       = productId;
        this.productName     = productName;
        this.qty             = qty;
        this.priceAtTime     = priceAtTime;
        this.discountedPrice = discountedPrice;
    }
    public double getSubtotal() { return discountedPrice * qty; }
    public double getSavings()  { return (priceAtTime - discountedPrice) * qty; }

    public Product getProduct()         { return product; }
    public String  getProductId()       { return productId; }
    public String  getProductName()     { return productName; }
    public int     getQty()             { return qty; }
    public double  getPriceAtTime()     { return priceAtTime; }
    public double  getDiscountedPrice() { return discountedPrice; }
}