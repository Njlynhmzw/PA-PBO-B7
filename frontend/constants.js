// ── KATEGORI & JENIS ─────────────────────────────────────────────
export const KATEGORI_LIST = [
  "T-Shirts",
  "Headwear",
  "Outerwear",
  "Footwear",
  "Gift & Accessories",
];

export const JENIS_MAP = {
  "T-Shirts":           ["Polo Shirt", "Crew Neck", "V-Neck", "Oversized", "Racing Tee"],
  "Headwear":           ["Snapback Cap", "Fitted Cap", "Bucket Hat", "Beanie", "Visor"],
  "Outerwear":          ["Jacket", "Hoodie", "Windbreaker", "Rain Jacket", "Varsity Jacket"],
  "Footwear":           ["Sneakers", "Sandals", "Boots", "Slip-On", "Racing Shoes"],
  "Gift & Accessories": ["Keychain", "Mug", "Phone Case", "Lanyard", "Sticker Pack", "Model Car", "Backpack", "Wallet"],
};

export const CATEGORY_ICONS = {
  "T-Shirts":           "👕",
  "Headwear":           "🧢",
  "Outerwear":          "🧥",
  "Footwear":           "👟",
  "Gift & Accessories": "🎁",
};

// ── HELPERS ───────────────────────────────────────────────────────
/** Format angka ke Rupiah */
export function formatRp(n) {
  return new Intl.NumberFormat("id-ID", {
    style: "currency",
    currency: "IDR",
    minimumFractionDigits: 0,
  }).format(n ?? 0);
}

/**
 * Hitung harga final produk.
 * Backend mengirim finalPrice, tapi ini fallback kalau tidak ada.
 */
export function getFinalPrice(p) {
  if (p.finalPrice != null) return p.finalPrice;
  return p.hasDiscount ? p.price * (1 - p.discountPercent / 100) : p.price;
}

/** Status stok dari field statusStok Java atau hitung dari stock */
export function getStatusStok(p) {
  if (p.statusStok) return p.statusStok;
  if (p.stock === 0) return "Habis";
  if (p.stock <= 5)  return "Menipis";
  return "Tersedia";
}

export function statusColor(status) {
  if (status === "Tersedia") return "badge-green";
  if (status === "Menipis")  return "badge-yellow";
  return "badge-red";
}

/** Diskon member berdasarkan tier */
export function memberDiscountRate(tier) {
  if (tier === "PLUS")    return 0.15;
  if (tier === "REGULAR") return 0.10;
  return 0;
}

/** Progress menuju tier PLUS (maks 100) */
export function plusProgress(totalBelanja) {
  return Math.min(100, (totalBelanja / 5_000_000) * 100);
}

export function sisaMenujuPlus(totalBelanja) {
  return Math.max(0, 5_000_000 - totalBelanja);
}