package service;
import models.*;
import repository.ProductRepository;
import repository.TransactionRepository;
import java.util.List;
import java.util.Optional;

public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final ProductRepository     productRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              ProductRepository productRepository) {
        this.transactionRepository = transactionRepository;
        this.productRepository     = productRepository;
    }

    public Transaction buatTransaksi(Member member) {
        return new Transaction(member);
    }

    public String tambahItem(Transaction transaksi, String produkId, int qty) {
        Optional<Product> opt = productRepository.findById(produkId);
        if (opt.isEmpty())            return "Produk tidak ditemukan.";

        Product produk = opt.get();
        if (qty <= 0)                 return "Jumlah harus lebih dari 0.";
        if (produk.getStock() < qty)  return "Stok tidak mencukupi! (tersisa " + produk.getStock() + ")";

        TransactionItem item = new TransactionItem(produk, qty, transaksi.getMemberTier());
        transaksi.addItem(item);

        produk.setStock(produk.getStock() - qty);
        productRepository.update(produk);

        return null;
    }

    public void simpanTransaksi(Transaction transaksi) {
        transactionRepository.save(transaksi);
    }

    public List<Transaction>     semuaTransaksi()          { return transactionRepository.findAll(); }
    public Optional<Transaction> cariById(String id)       { return transactionRepository.findById(id); }
    public boolean               isEmpty()                 { return transactionRepository.isEmpty(); }
}