-- ============================================================
--  McLaren Collection — Database Schema
--  Jalankan file ini di HeidiSQL atau MySQL CLI
--  Database: mclaren_db
-- ============================================================

CREATE DATABASE IF NOT EXISTS mclaren_db
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
--  (detail item per transaksi — relasi 1 transaksi → N item)
-- ============================================================
CREATE TABLE IF NOT EXISTS transaction_items (
    id               BIGINT         NOT NULL AUTO_INCREMENT,
    transaction_id   VARCHAR(20)    NOT NULL,
    product_id       VARCHAR(20)    NOT NULL,
    product_name     VARCHAR(255)   NOT NULL,   -- snapshot nama saat transaksi
    qty              INT            NOT NULL,
    price_per_item   DOUBLE         NOT NULL,   -- harga final (sudah dipotong diskon produk)
    subtotal         DOUBLE         NOT NULL,   -- price_per_item × qty

    PRIMARY KEY (id),
    CONSTRAINT fk_item_trx
        FOREIGN KEY (transaction_id) REFERENCES transactions(id)
        ON DELETE CASCADE,
    CONSTRAINT fk_item_product
        FOREIGN KEY (product_id) REFERENCES products(id)
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================================
--  TABEL COUNTERS  (menyimpan nomor urut ID agar tidak reset)
-- ============================================================
CREATE TABLE IF NOT EXISTS counters (
    name     VARCHAR(50) NOT NULL,
    value    INT         NOT NULL DEFAULT 0,
    PRIMARY KEY (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO counters (name, value) VALUES
    ('product',     0),
    ('member',      0),
    ('transaction', 0)
ON DUPLICATE KEY UPDATE name = name;

-- ============================================================
--  DATA AWAL (opsional — sama seperti DataSeeder.java)
-- ============================================================
INSERT IGNORE INTO products
    (id, name, price, stock, size, category, jenis, has_discount, discount_percent)
VALUES
    ('PRD-0001', 'McLaren Heritage Racing Tee',  450000, 20, 'M',         'T-Shirts',           'Racing Tee',   1, 10),
    ('PRD-0002', 'Papaya Orange Polo Shirt',     380000, 15, 'L',         'T-Shirts',           'Polo Shirt',   0,  0),
    ('PRD-0003', 'McLaren Team Crew Neck',       320000, 25, 'XL',        'T-Shirts',           'Crew Neck',    1,  5),
    ('PRD-0004', 'Papaya Orange Snapback',       320000, 15, 'Free Size', 'Headwear',           'Snapback Cap', 0,  0),
    ('PRD-0005', 'McLaren F1 Bucket Hat',        275000, 10, 'Free Size', 'Headwear',           'Bucket Hat',   1, 10),
    ('PRD-0006', 'McLaren Team Hoodie',          890000,  8, 'L',         'Outerwear',          'Hoodie',       1, 15),
    ('PRD-0007', 'McLaren Windbreaker Jacket', 1200000,  5, 'M',         'Outerwear',          'Windbreaker',  0,  0),
    ('PRD-0008', 'F1 Pit Lane Sneakers',       1250000,  5, '42',        'Footwear',           'Sneakers',     0,  0),
    ('PRD-0009', 'McLaren Slide Sandals',        350000, 12, '43',        'Footwear',           'Sandals',      1, 10),
    ('PRD-0010', 'McLaren Model Car 1:43',       275000, 30, '-',         'Gift & Accessories', 'Model Car',    1,  5),
    ('PRD-0011', 'McLaren Keychain Set',          85000, 50, '-',         'Gift & Accessories', 'Keychain',     0,  0),
    ('PRD-0012', 'McLaren Team Backpack',        650000,  7, '-',         'Gift & Accessories', 'Backpack',     1, 10);

INSERT IGNORE INTO members
    (id, name, phone, email, tier, total_transaksi, total_belanja)
VALUES
    ('MEM-0001', 'Lando Norris Fan',  '08111111111', 'lando@fan.com',  'PLUS',    3, 5500000),
    ('MEM-0002', 'Oscar Piastri Fan', '08222222222', 'oscar@fan.com',  'REGULAR', 1,  890000),
    ('MEM-0003', 'McLaren Collector', '08333333333', 'collector@f1.com','REGULAR', 0,       0);

-- Update counter agar ID berikutnya lanjut dari data awal
UPDATE counters SET value = 12 WHERE name = 'product';
UPDATE counters SET value =  3 WHERE name = 'member';
UPDATE counters SET value =  0 WHERE name = 'transaction';

-- ============================================================
--  QUERY REFERENSI (untuk dicek / debugging di HeidiSQL)
-- ============================================================

-- Lihat semua produk dengan harga final:
-- SELECT id, name, category, jenis,
--        IF(has_discount, price*(1-discount_percent/100), price) AS final_price,
--        stock
-- FROM products ORDER BY category, name;

-- Lihat semua member dengan progress ke PLUS:
-- SELECT id, name, tier, total_belanja,
--        ROUND((total_belanja/5000000)*100, 1) AS progress_pct
-- FROM members;

-- Lihat riwayat transaksi lengkap dengan item:
-- SELECT t.id, t.created_at, m.name AS member,
--        ti.product_name, ti.qty, ti.price_per_item, ti.subtotal,
--        t.total
-- FROM transactions t
-- LEFT JOIN members m ON t.member_id = m.id
-- JOIN transaction_items ti ON ti.transaction_id = t.id
-- ORDER BY t.created_at DESC;