package models;

public class Footwear extends Product {
    public enum JenisFootwear {
        SNEAKERS("Sneakers"), SANDALS("Sandals"), BOOTS("Boots"),
        SLIP_ON("Slip-On"), RACING_SHOES("Racing Shoes");
        private final String label;
        JenisFootwear(String label) { this.label = label; }
        public String getLabel() { return label; }
    }
    private JenisFootwear jenisFootwear;
    public Footwear(String name, double price, int stock, String size,
                    boolean hasDiscount, double discountPercent, JenisFootwear jenis) {
        super(name, price, stock, size, hasDiscount, discountPercent);
        this.jenisFootwear = jenis;
    }
    @Override public String getCategory()   { return "Footwear"; }
    @Override public String getJenis()      { return jenisFootwear.getLabel(); }
}