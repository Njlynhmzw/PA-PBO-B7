# 🏎️ McLaren Collection — Merchandise Management System

Aplikasi kasir dan manajemen inventaris berbasis web (*Full-stack*) yang dibangun menggunakan **Java Murni (Vanilla Java)** untuk Backend dan **Vanilla HTML/CSS/JS** untuk Frontend.

Proyek ini dibangun dari nol (*from scratch*) tanpa menggunakan *framework* besar seperti Spring Boot atau React. Tujuannya adalah untuk mendemonstrasikan pemahaman mendalam tentang arsitektur *Client-Server*, pola perancangan *N-Tier*, JDBC murni, dan implementasi **Pemrograman Berorientasi Objek (OOP)** secara komprehensif.

---

## 🛠️ Teknologi yang Digunakan

| Komponen | Teknologi |
|---|---|
| **Backend** | Java SE 17+ |
| **Web Server** | `com.sun.net.httpserver` (bawaan JDK) |
| **Database** | MySQL / MariaDB |
| **Database Access** | JDBC — Native SQL |
| **Frontend** | Vanilla JavaScript (ES6 Modules), HTML5, CSS3 |

---

## 📂 Struktur Direktori

Aplikasi ini menggunakan arsitektur **MVC / N-Tier**.

```
📦 mclaren-collection
 ┣ 📂 src
 ┃ ┣ 📂 api                         # [CONTROLLER LAYER] HTTP Request & Response
 ┃ ┃ ┣ 📜 ApiServer.java            # Setup routing server (port 8080)
 ┃ ┃ ┣ 📜 ProductHandler.java       # Endpoint /api/products
 ┃ ┃ ┣ 📜 MemberHandler.java        # Endpoint /api/members
 ┃ ┃ ┣ 📜 TransactionHandler.java   # Endpoint /api/transactions
 ┃ ┃ ┗ 📜 JsonParser.java           # Custom JSON Parser (tanpa Gson/Jackson)
 ┃ ┣ 📂 service                     # [BUSINESS LOGIC LAYER] Aturan bisnis & validasi
 ┃ ┃ ┣ 📜 ProductService.java
 ┃ ┃ ┣ 📜 MemberService.java
 ┃ ┃ ┗ 📜 TransactionService.java
 ┃ ┣ 📂 repository                  # [DATA ACCESS LAYER] Query SQL & JDBC
 ┃ ┃ ┣ 📜 ProductRepository.java
 ┃ ┃ ┣ 📜 MemberRepository.java
 ┃ ┃ ┗ 📜 TransactionRepository.java
 ┃ ┣ 📂 models                      # [DATA MODELS] Representasi entitas (OOP)
 ┃ ┃ ┣ 📜 Product.java              # Abstract class
 ┃ ┃ ┣ 📜 TShirt.java, Footwear.java, Outerwear.java, Headwear.java, GiftAccessory.java
 ┃ ┃ ┣ 📜 Member.java
 ┃ ┃ ┣ 📜 Transaction.java & TransactionItem.java
 ┃ ┃ ┗ 📜 Discountable.java         # Interface
 ┃ ┣ 📂 db
 ┃ ┃ ┗ 📜 DatabaseConfig.java       # Konfigurasi koneksi database
 ┃ ┗ 📜 Main.java                   # Entry point aplikasi
 ┣ 📂 frontend
 ┃ ┣ 📜 index.html                  # Halaman utama SPA
 ┃ ┣ 📜 global.css                  # Styling UI
 ┃ ┣ 📜 app.js                      # Logika UI & DOM Manipulation
 ┃ ┣ 📜 api.js                      # Fetch API ke backend Java
 ┃ ┗ 📜 constants.js                # Variabel konstan UI
 ┣ 📜 .env                          # Kredensial database (jangan di-commit!)
 ┗ 📜 README.md
```

---

## 🧬 Penerapan Konsep OOP (Modul 1–7 PBO)

Program ini mengimplementasikan 4 Pilar OOP secara menyeluruh, beserta konsep lanjutan lainnya.

### 1. Encapsulation (Enkapsulasi)

Menyembunyikan data internal dan hanya memberikan akses melalui method tertentu (Getter & Setter).

- **Lokasi:** Seluruh kelas di package `models`
- **Bukti:** Pada `Member.java`, atribut `private String name` dan `private Tier tier` tidak dapat diakses langsung. Kelas lain harus menggunakan `getName()` untuk membaca dan `setName(String n)` untuk mengubah nilainya.

---

### 2. Inheritance (Pewarisan)

Membuat kelas baru yang mewarisi sifat dan perilaku kelas induk, menghindari pengulangan kode (Prinsip DRY).

- **Lokasi:** `TShirt.java`, `Outerwear.java`, `Headwear.java`, `Footwear.java`, `GiftAccessory.java`
- **Bukti:** Semua kelas tersebut menggunakan `extends Product`, mewarisi atribut umum seperti nama, harga, dan stok, namun masing-masing memiliki Enum Jenis yang unik.

---

### 3. Polymorphism (Polimorfisme)

Kemampuan objek mengambil banyak bentuk, diwujudkan dalam **Overriding** dan **Overloading**.

**Method Overriding** *(Run-time Polymorphism)*
- **Lokasi:** Kelas turunan `Product`, contohnya `TShirt.java`
- **Bukti:** Anotasi `@Override` pada method `getCategory()` dan `getJenis()`. Meskipun dipanggil dari referensi bertipe `Product`, nilai yang dikembalikan bergantung pada bentuk objek sesungguhnya.

**Constructor Overloading** *(Compile-time Polymorphism)*
- **Lokasi:** `TransactionItem.java` dan `Member.java`
- **Bukti:** Terdapat beberapa constructor `TransactionItem(...)` dengan jumlah parameter berbeda — satu untuk membuat transaksi baru, satu untuk memuat data historis dari database, dan satu untuk menangani produk yang telah dihapus.

---

### 4. Abstraction (Abstraksi)

Menyembunyikan detail implementasi yang kompleks dan hanya menampilkan kerangka yang penting.

**Abstract Class**
- **Lokasi:** `Product.java`
- **Bukti:** Dideklarasikan dengan `public abstract class Product`. Kelas ini tidak dapat diinstansiasi langsung (`new Product()` dilarang) dan memaksa setiap kelas turunan untuk mengimplementasikan `public abstract String getCategory()`.

**Interface**
- **Lokasi:** `Discountable.java`
- **Bukti:** Menetapkan kontrak method `calculateMemberDiscount(String tier)` yang kemudian diimplementasikan oleh `Product.java`.

---

### 🔬 Konsep Lanjutan (Modul 5–7)

| Konsep | Implementasi |
|---|---|
| **Enumeration (Enum)** | `Member.Tier`, `TShirt.JenisTShirt`, dan sejenisnya untuk data konstan yang statis |
| **Exception Handling** | Blok `try-catch-finally` di lapisan Repository (JDBC); `throw new IllegalArgumentException(...)` di lapisan Service |
| **Generics & Collections** | `List<Product>`, `Optional<Member>`, `Set<String>` untuk mengelola kumpulan objek |
| **JDBC & ACID Transaction** | `PreparedStatement` (mencegah SQL Injection), `conn.setAutoCommit(false)`, dan `conn.commit()` di `TransactionRepository.java` |

---

## 🚀 Cara Menjalankan Aplikasi

### 1. Persiapan Database

1. Buka MySQL / XAMPP / HeidiSQL.
2. Jalankan script SQL yang disediakan untuk membuat database `mclaren_db` beserta seluruh tabelnya (`products`, `members`, `transactions`, `transaction_items`, `counters`).
3. Data produk, member, dan transaksi dikelola sepenuhnya melalui antarmuka web aplikasi.

---

### 2. Konfigurasi `.env`

Buat file `.env` di root folder (sejajar dengan `src`) dan isi dengan kredensial database Anda:

```env
DB_URL=jdbc:mysql://localhost:3306/mclaren_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
DB_USER=root
DB_PASSWORD=
```

> ⚠️ Jangan pernah meng-*commit* file `.env` ke repositori publik.

---

### 3. Menjalankan Backend (Java Server)

1. Buka proyek di IDE Anda (IntelliJ IDEA / Eclipse).
2. Pastikan driver **MySQL JDBC Connector** (`mysql-connector-j.jar`) sudah ditambahkan ke Libraries/Dependencies proyek.
3. Jalankan file `Main.java`.

Jika berhasil, console akan menampilkan:

```
╔══════════════════════════════════════╗
║  McLaren API Server — port 8080      ║
║  http://localhost:8080/api/products  ║
╚══════════════════════════════════════╝
```

---

### 4. Menjalankan Frontend

1. Buka folder `frontend` di **Visual Studio Code**.
2. Jalankan file `index.html` menggunakan ekstensi **Live Server**.
3. Aplikasi web siap digunakan di browser.

---

## 👤 Informasi Proyek

Dibuat untuk memenuhi Tugas/Proyek Mata Kuliah **Pemrograman Berorientasi Objek (PBO)**.