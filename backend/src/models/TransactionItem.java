package models;

public class TransactionItem {

    private Product product;
    private int     qty;
    private double  priceAtTime;
    private double  discountedPrice;

    // CONSTRUCTOR 1 (Menerima 3 Argumen):
    // Dipakai oleh TransactionService saat ada transaksi BARU (harga dihitung otomatis)
    public TransactionItem(Product product, int qty, String memberTier) {
        this.product      = product;
        this.qty          = qty;
        this.priceAtTime  = product.getFinalPrice();

        if (memberTier != null && !"NONE".equals(memberTier) && product instanceof Discountable) {
            this.discountedPrice = ((Discountable) product).calculateMemberDiscount(memberTier);
        } else {
            this.discountedPrice = priceAtTime;
        }
    }

    // CONSTRUCTOR 2 (Menerima 4 Argumen - Overloading):
    // Dipakai KHUSUS oleh TransactionRepository saat memuat riwayat murni dari database MySQL
    public TransactionItem(Product product, int qty, double priceAtTime, double discountedPrice) {
        this.product         = product;
        this.qty             = qty;
        this.priceAtTime     = priceAtTime;
        this.discountedPrice = discountedPrice;
    }

    public double getSubtotal() { return discountedPrice * qty; }
    public double getSavings()  { return (priceAtTime - discountedPrice) * qty; }

    public Product getProduct()          { return product; }
    public int     getQty()              { return qty; }
    public double  getPriceAtTime()      { return priceAtTime; }
    public double  getDiscountedPrice()  { return discountedPrice; }
}