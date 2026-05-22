package models;

// Tambahkan implements Discountable di kelas induk
public abstract class Product implements Discountable {

    private String  id;
    private String  name;
    private double  price;
    private int     stock;
    private String  size;
    private boolean hasDiscount;
    private double  discountPercent;

    // ID tidak lagi digenerate manual di sini karena sudah diurus Database
    public Product(String name, double price, int stock,
                   String size, boolean hasDiscount, double discountPercent) {
        this.id              = null;
        this.name            = name;
        this.price           = price;
        this.stock           = stock;
        this.size            = size;
        this.hasDiscount     = hasDiscount;
        this.discountPercent = hasDiscount ? discountPercent : 0.0;
    }

    public abstract String getCategory();
    public abstract String getJenis();

    public double getFinalPrice() {
        if (hasDiscount) return price * (1 - discountPercent / 100);
        return price;
    }

    public String getStatusStok() {
        if (stock == 0)  return "HABIS";
        if (stock <= 5)  return "MENIPIS";
        return "TERSEDIA";
    }

    // --- IMPLEMENTASI DARI MODUL PBO (Prinsip DRY & Inheritance) ---
    // Sekarang kelima subclass turunan tidak perlu lagi menulis ulang method ini
    @Override
    public double calculateMemberDiscount(String memberTier) {
        double base = getFinalPrice();
        if ("PLUS".equals(memberTier))    return base * (1 - Discountable.MEMBER_PLUS_DISCOUNT / 100);
        if ("REGULAR".equals(memberTier)) return base * (1 - Discountable.MEMBER_DISCOUNT / 100);
        return base;
    }

    @Override
    public String getDiscountLabel() {
        if (!isHasDiscount()) return "Tidak ada diskon produk";
        return String.format("Diskon %.0f%% (Harga asli: Rp %,.0f)", getDiscountPercent(), getPrice());
    }

    // --- GETTER & SETTER ---
    public String getId()    { return id; }
    public void setId(String id) { this.id = id; }
    public String getName()  { return name; }
    public void setName(String name) { this.name = name.trim(); }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public int getStock()    { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public String getSize()  { return size; }
    public void setSize(String size) { this.size = size; }
    public boolean isHasDiscount() { return hasDiscount; }
    public void setHasDiscount(boolean v) {
        this.hasDiscount = v;
        if (!v) this.discountPercent = 0.0;
    }
    public double getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(double v) { this.discountPercent = v; }
}