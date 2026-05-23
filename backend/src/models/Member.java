package models;

public class Member {
    public enum Tier {
        REGULAR("Regular", 10.0),
        PLUS("Plus", 15.0);
        private final String label;
        private final double discountRate;
        Tier(String label, double discountRate) {
            this.label        = label;
            this.discountRate = discountRate;
        }
        public String getLabel()        { return label; }
    }
    private String memberId;
    private String name;
    private String phone;
    private String email;
    private Tier   tier;
    private int    totalTransaksi;
    private double totalBelanja;
    public Member(String nama, String phone, String email, Tier tier) {
        this.memberId       = null;
        this.name           = nama;
        this.phone          = phone;
        this.email          = email;
        this.tier           = tier;
        this.totalTransaksi = 0;
        this.totalBelanja   = 0.0;
    }
    public Member(String memberId, String name, String phone, String email,
                  Tier tier, int totalTransaksi, double totalBelanja) {
        this.memberId       = memberId;
        this.name           = name;
        this.phone          = phone;
        this.email          = email;
        this.tier           = tier;
        this.totalTransaksi = totalTransaksi;
        this.totalBelanja   = totalBelanja;
    }
    public void tambahTransaksi(double amount) {
        this.totalTransaksi++;
        this.totalBelanja += amount;
        if (this.tier == Tier.REGULAR && this.totalBelanja >= 5_000_000) {
            this.tier = Tier.PLUS;
            System.out.println("🎉 Selamat! " + name + " telah upgrade ke Tier PLUS!");
        }
    }
    public String getMemberId()              { return memberId; }
    public void   setMemberId(String id)     { this.memberId = id; }
    public String getName()                  { return name; }
    public void   setName(String n)          { this.name = n; }
    public String getPhone()                 { return phone; }
    public void   setPhone(String p)         { this.phone = p; }
    public String getEmail()                 { return email; }
    public void   setEmail(String e)         { this.email = e; }
    public Tier   getTier()                  { return tier; }
    public void   setTier(Tier t)            { this.tier = t; }
    public int    getTotalTransaksi()        { return totalTransaksi; }
    public double getTotalBelanja()          { return totalBelanja; }

    @Override
    public String toString() {
        return String.format("%-10s | %-20s | %-15s | Tier: %-8s | Transaksi: %d | Total: Rp %,.0f",
                memberId, name, phone, tier.getLabel(), totalTransaksi, totalBelanja);
    }
}