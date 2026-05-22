package service;

import models.*;
import repository.ProductRepository;

import java.util.List;
import java.util.Optional;


public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Product tambahProduk(String nama, double harga, int stok, String size,
                                boolean hasDiscount, double discountPercent,
                                String kategori, int jenisIndex) {

        return productRepository.createNew(
                nama, harga, stok, size,
                hasDiscount, discountPercent,
                kategori, jenisIndex
        );
    }

    public List<Product>     semuaProduk()              { return productRepository.findAll(); }
    public Optional<Product> cariById(String id)        { return productRepository.findById(id); }
    public List<Product>     cariByKategori(String kat) { return productRepository.findByCategory(kat); }
    public boolean           isEmpty()                  { return productRepository.isEmpty(); }


    public boolean updateNama(String id, String namaBaru) {
        Optional<Product> opt = productRepository.findById(id);
        if (opt.isEmpty()) return false;
        Product p = opt.get();
        p.setName(namaBaru);
        productRepository.update(p);
        return true;
    }

    public boolean updateHarga(String id, double hargaBaru) {
        Optional<Product> opt = productRepository.findById(id);
        if (opt.isEmpty()) return false;
        Product p = opt.get();
        p.setPrice(hargaBaru);
        productRepository.update(p);
        return true;
    }

    public boolean updateStok(String id, int stokBaru) {
        Optional<Product> opt = productRepository.findById(id);
        if (opt.isEmpty()) return false;
        Product p = opt.get();
        p.setStock(stokBaru);
        productRepository.update(p);
        return true;
    }

    public boolean updateLengkap(String id, String nama, double harga, int stok) {
        Optional<Product> opt = productRepository.findById(id);
        if (opt.isEmpty()) return false;
        Product p = opt.get();
        p.setName(nama);
        p.setPrice(harga);
        p.setStock(stok);
        productRepository.update(p);
        return true;
    }

    public boolean hapusProduk(String id) {
        return productRepository.delete(id);
    }

    public boolean kurangiStok(String id, int qty) {
        Optional<Product> opt = productRepository.findById(id);
        if (opt.isEmpty()) return false;
        Product p = opt.get();
        if (p.getStock() < qty) return false;
        p.setStock(p.getStock() - qty);
        productRepository.update(p);
        return true;
    }

    // Tambahkan ini di ProductService.java
    public boolean updateProdukUtuh(Product p) {
        if (!productRepository.existsById(p.getId())) return false;
        productRepository.update(p);
        return true;
    }
}