package models;

public class Outerwear extends Product {

    public enum JenisOuterwear {
        JACKET("Jacket"), HOODIE("Hoodie"), WINDBREAKER("Windbreaker"),
        RAIN_JACKET("Rain Jacket"), VARSITY("Varsity Jacket");
        private final String label;
        JenisOuterwear(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private JenisOuterwear jenisOuterwear;

    public Outerwear(String name, double price, int stock, String size,
                     boolean hasDiscount, double discountPercent, JenisOuterwear jenis) {
        super(name, price, stock, size, hasDiscount, discountPercent);
        this.jenisOuterwear = jenis;
    }

    @Override public String getCategory()   { return "Outerwear"; }
    @Override public String getJenis()      { return jenisOuterwear.getLabel(); }
}