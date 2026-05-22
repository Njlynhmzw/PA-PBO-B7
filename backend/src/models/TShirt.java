package models;

public class TShirt extends Product {

    public enum JenisTShirt {
        POLO("Polo Shirt"), CREW_NECK("Crew Neck"), V_NECK("V-Neck"),
        OVERSIZED("Oversized"), RACING("Racing Tee");
        private final String label;
        JenisTShirt(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private JenisTShirt jenisTShirt;

    public TShirt(String name, double price, int stock, String size,
                  boolean hasDiscount, double discountPercent, JenisTShirt jenis) {
        super(name, price, stock, size, hasDiscount, discountPercent);
        this.jenisTShirt = jenis;
    }

    @Override public String getCategory()   { return "T-Shirts"; }
    @Override public String getJenis()      { return jenisTShirt.getLabel(); }
}