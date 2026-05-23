package models;

public class Headwear extends Product {
    public enum JenisHeadwear {
        SNAPBACK("Snapback Cap"), FITTED_CAP("Fitted Cap"),
        BUCKET_HAT("Bucket Hat"), BEANIE("Beanie"), VISOR("Visor");
        private final String label;
        JenisHeadwear(String label) { this.label = label; }
        public String getLabel() { return label; }
    }
    private JenisHeadwear jenisHeadwear;
    public Headwear(String name, double price, int stock, String size,
                    boolean hasDiscount, double discountPercent, JenisHeadwear jenis) {
        super(name, price, stock, size, hasDiscount, discountPercent);
        this.jenisHeadwear = jenis;
    }
    @Override public String getCategory()   { return "Headwear"; }
    @Override public String getJenis()      { return jenisHeadwear.getLabel(); }
}