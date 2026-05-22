package models;

public class GiftAccessory extends Product {

    public enum JenisGift {
        KEYCHAIN("Keychain"), MUG("Mug"), PHONE_CASE("Phone Case"),
        LANYARD("Lanyard"), STICKER_PACK("Sticker Pack"),
        MODEL_CAR("Model Car"), BACKPACK("Backpack"), WALLET("Wallet");
        private final String label;
        JenisGift(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private JenisGift jenisGift;

    public GiftAccessory(String name, double price, int stock, String size,
                         boolean hasDiscount, double discountPercent, JenisGift jenis) {
        super(name, price, stock, size, hasDiscount, discountPercent);
        this.jenisGift = jenis;
    }

    @Override public String getCategory()   { return "Gift & Accessories"; }
    @Override public String getJenis()      { return jenisGift.getLabel(); }
}