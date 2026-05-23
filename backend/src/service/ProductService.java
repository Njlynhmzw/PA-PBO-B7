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
    public boolean           isEmpty()                  { return productRepository.isEmpty(); }

    public boolean hapusProduk(String id) {
        return productRepository.delete(id);
    }

    public boolean updateProdukUtuh(Product p) {
        if (!productRepository.existsById(p.getId())) return false;
        productRepository.update(p);
        return true;
    }
}