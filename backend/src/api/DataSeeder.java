package api;

import service.MemberService;
import service.ProductService;

/**
 * DataSeeder — mengisi data awal agar frontend langsung bisa ditest.
 * Dipanggil sekali saat server start dari Main.java.
 *
 * ✅ PERBAIKAN: Sekarang mengecek database terlebih dahulu.
 *    - Jika database KOSONG  → isi 12 produk + 3 member awal.
 *    - Jika database SUDAH ADA ISINYA → langsung skip, data aman.
 */
public class DataSeeder {

    public static void seed(ProductService ps, MemberService ms) {

        // ── PROTEKSI: Cek apakah database sudah berisi data ──────────
        // Jika sudah ada produk, skip seluruh proses seeding.
        // Ini mencegah data manual yang sudah diinput tertimpa ulang.
        if (!ps.isEmpty()) {
            System.out.println("ℹ️  Data sudah ada di database. Seeding dilewati.");
            return;
        }

        System.out.println("📦 Database kosong. Mengisi data awal...");

        // ── PRODUK ────────────────────────────────────────────────────
        // T-Shirts (kategori 0, index jenis: 4=Racing Tee, 0=Polo, 1=Crew Neck)
        ps.tambahProduk("McLaren Heritage Racing Tee",   450_000,  20, "M",         true,  10, "T-Shirts", 4);
        ps.tambahProduk("Papaya Orange Polo Shirt",      380_000,  15, "L",         false,  0, "T-Shirts", 0);
        ps.tambahProduk("McLaren Team Crew Neck",        320_000,  25, "XL",        true,   5, "T-Shirts", 1);

        // Headwear (kategori 1: 0=Snapback, 2=Bucket Hat)
        ps.tambahProduk("Papaya Orange Snapback",        320_000,  15, "Free Size", false,  0, "Headwear", 0);
        ps.tambahProduk("McLaren F1 Bucket Hat",         275_000,  10, "Free Size", true,  10, "Headwear", 2);

        // Outerwear (kategori 2: 1=Hoodie, 2=Windbreaker)
        ps.tambahProduk("McLaren Team Hoodie",           890_000,   8, "L",         true,  15, "Outerwear", 1);
        ps.tambahProduk("McLaren Windbreaker Jacket",  1_200_000,   5, "M",         false,  0, "Outerwear", 2);

        // Footwear (kategori 3: 0=Sneakers, 1=Sandals)
        ps.tambahProduk("F1 Pit Lane Sneakers",        1_250_000,   5, "42",        false,  0, "Footwear", 0);
        ps.tambahProduk("McLaren Slide Sandals",         350_000,  12, "43",        true,  10, "Footwear", 1);

        // Gift & Accessories (kategori 4: 5=Model Car, 0=Keychain, 6=Backpack)
        ps.tambahProduk("McLaren Model Car 1:43",        275_000,  30, "-",         true,   5, "Gift & Accessories", 5);
        ps.tambahProduk("McLaren Keychain Set",           85_000,  50, "-",         false,  0, "Gift & Accessories", 0);
        ps.tambahProduk("McLaren Team Backpack",         650_000,   7, "-",         true,  10, "Gift & Accessories", 6);

        // ── MEMBER ────────────────────────────────────────────────────
        try {
            ms.daftarMember("Lando Norris Fan",  "08111111111", "lando@fan.com",   "PLUS");
            ms.daftarMember("Oscar Piastri Fan", "08222222222", "oscar@fan.com",   "REGULAR");
            ms.daftarMember("McLaren Collector", "08333333333", "collector@f1.com","REGULAR");
        } catch (Exception e) {
            System.out.println("⚠️  Member seed dilewati (sudah ada): " + e.getMessage());
        }

        System.out.println("✅ Data awal selesai: 12 produk, 3 member.");
    }
}