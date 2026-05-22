CREATE DATABASE mclaren_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE mclaren_db;

-- ============================================================
--  TABEL PRODUCTS
-- ============================================================
CREATE TABLE IF NOT EXISTS products (
    id               VARCHAR(20)    NOT NULL,
    name             VARCHAR(255)   NOT NULL,
    price            DOUBLE         NOT NULL,
    stock            INT            NOT NULL DEFAULT 0,
    size             VARCHAR(50)    NOT NULL DEFAULT '-',
    category         VARCHAR(100)   NOT NULL,
    jenis            VARCHAR(100)   NOT NULL,
    has_discount     TINYINT(1)     NOT NULL DEFAULT 0,
    discount_percent DOUBLE         NOT NULL DEFAULT 0,
    created_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
--  TABEL MEMBERS
-- ============================================================
CREATE TABLE IF NOT EXISTS members (
    id               VARCHAR(20)    NOT NULL,
    name             VARCHAR(255)   NOT NULL,
    phone            VARCHAR(20)    NOT NULL,
    email            VARCHAR(255)            DEFAULT '',
    tier             ENUM('REGULAR','PLUS')  NOT NULL DEFAULT 'REGULAR',
    total_transaksi  INT            NOT NULL DEFAULT 0,
    total_belanja    DOUBLE         NOT NULL DEFAULT 0,
    created_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
--  TABEL TRANSACTIONS
-- ============================================================
CREATE TABLE IF NOT EXISTS transactions (
    id               VARCHAR(20)    NOT NULL,
    member_id        VARCHAR(20)             DEFAULT NULL,
    subtotal         DOUBLE         NOT NULL DEFAULT 0,
    member_discount  DOUBLE         NOT NULL DEFAULT 0,
    total            DOUBLE         NOT NULL DEFAULT 0,
    created_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT fk_trx_member
        FOREIGN KEY (member_id) REFERENCES members(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
--  TABEL TRANSACTION_ITEMS
-- ============================================================
CREATE TABLE IF NOT EXISTS transaction_items (
    id               BIGINT         NOT NULL AUTO_INCREMENT,
    transaction_id   VARCHAR(20)    NOT NULL,
    product_id       VARCHAR(20)    NOT NULL,
    product_name     VARCHAR(255)   NOT NULL,
    qty              INT            NOT NULL,
    price_per_item   DOUBLE         NOT NULL,
    subtotal         DOUBLE         NOT NULL,

    PRIMARY KEY (id),
    CONSTRAINT fk_item_trx
        FOREIGN KEY (transaction_id) REFERENCES transactions(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_item_product
        FOREIGN KEY (product_id) REFERENCES products(id)
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
--  TABEL COUNTERS
--  ✅ PERBAIKAN: Mulai dari 0, TIDAK ada UPDATE manual di sini.
--     Counter akan naik otomatis lewat Java setiap ada data baru.
-- ============================================================
CREATE TABLE IF NOT EXISTS counters (
    name     VARCHAR(50) NOT NULL,
    value    INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Inisialisasi counter dari 0 (hanya insert jika belum ada)
INSERT INTO counters (name, value) VALUES
    ('product',     0),
    ('member',      0),
    ('transaction', 0)
ON DUPLICATE KEY UPDATE name = name;

-- ============================================================
--  CATATAN PENTING
-- ============================================================
--  ❌ DIHAPUS: INSERT IGNORE INTO products (...) VALUES (...)
--  ❌ DIHAPUS: INSERT IGNORE INTO members  (...) VALUES (...)
--  ❌ DIHAPUS: UPDATE counters SET value = 12 WHERE name = 'product'
--  ❌ DIHAPUS: UPDATE counters SET value =  3 WHERE name = 'member'
--
--  Semua data awal sekarang dikontrol oleh DataSeeder.java.
--  Alur yang benar:
--    1. Jalankan SQL ini di HeidiSQL (sekali saja)
--    2. Jalankan Main.java → DataSeeder.seed() cek isEmpty()
--    3. Karena database kosong → DataSeeder isi PRD-0001 s/d PRD-0012
--    4. Run ulang Java lagi → DataSeeder skip (database sudah ada isinya)
--    5. Data manual yang kamu input lewat frontend AMAN tidak tertimpa
-- ============================================================

-- ============================================================
--  QUERY REFERENSI (untuk debugging di HeidiSQL)
-- ============================================================

-- Cek counter saat ini:
-- SELECT * FROM counters;

-- Lihat semua produk dengan harga final:
-- SELECT id, name, category, jenis,
--        IF(has_discount, price*(1-discount_percent/100), price) AS final_price,
--        stock
-- FROM products ORDER BY category, name;

-- Lihat semua member dengan progress ke PLUS:
-- SELECT id, name, tier, total_belanja,
--        ROUND((total_belanja/5000000)*100, 1) AS progress_pct
-- FROM members;

-- Lihat riwayat transaksi lengkap:
-- SELECT t.id, t.created_at, m.name AS member,
--        ti.product_name, ti.qty, ti.price_per_item, ti.subtotal, t.total
-- FROM transactions t
-- LEFT JOIN members m ON t.member_id = m.id
-- JOIN transaction_items ti ON ti.transaction_id = t.id
-- ORDER BY t.created_at DESC;