package repository;
import db.DatabaseConfig;
import models.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TransactionRepository {
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    public TransactionRepository(ProductRepository productRepository, MemberRepository memberRepository) {
        this.productRepository = productRepository;
        this.memberRepository = memberRepository;
    }
    private Connection conn() { return DatabaseConfig.getConnection(); }
    private String generateId() {
        String updateSql = "UPDATE counters SET value = value + 1 WHERE name = 'transaction'";
        String selectSql = "SELECT value FROM counters WHERE name = 'transaction'";

        try (Connection conn = conn()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psUpdate = conn.prepareStatement(updateSql);
                 PreparedStatement psSelect = conn.prepareStatement(selectSql)) {

                psUpdate.executeUpdate();
                try (ResultSet rs = psSelect.executeQuery()) {
                    if (rs.next()) {
                        int num = rs.getInt(1);
                        conn.commit();
                        return String.format("TRX-%06d", num);
                    }
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Gagal generate transaction ID", e);
        }
        return null;
    }
    public void save(Transaction transaction) {
        if (transaction.getTransactionId() == null || transaction.getTransactionId().isBlank()) {
            transaction.setTransactionId(generateId());
        }
        String sqlTrx = """
            INSERT INTO transactions
                (id, member_id, subtotal, member_discount, total)
            VALUES (?, ?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE id = id
            """;
        String sqlItem = """
            INSERT INTO transaction_items
                (transaction_id, product_id, product_name, qty, price_per_item, subtotal)
            VALUES (?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = conn()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psTrx = conn.prepareStatement(sqlTrx);
                 PreparedStatement psItem = conn.prepareStatement(sqlItem)) {

                // 1. Simpan Transaksi Utama
                psTrx.setString(1, transaction.getTransactionId());
                psTrx.setString(2, transaction.getMember() != null
                        ? transaction.getMember().getMemberId() : null);
                psTrx.setDouble(3, transaction.getTotalBeforeDiscount());
                psTrx.setDouble(4, transaction.getTotalSavings());
                psTrx.setDouble(5, transaction.getTotalAfterDiscount());
                psTrx.executeUpdate();
                // 2. Simpan Item Transaksi
                for (TransactionItem item : transaction.getItems()) {
                    psItem.setString(1, transaction.getTransactionId());
                    psItem.setString(2, item.getProduct().getId());
                    psItem.setString(3, item.getProduct().getName());
                    psItem.setInt   (4, item.getQty());
                    psItem.setDouble(5, item.getDiscountedPrice());
                    psItem.setDouble(6, item.getSubtotal());
                    psItem.addBatch();
                }
                psItem.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Gagal menyimpan transaksi: " + e.getMessage(), e);
        }
    }
    public List<Transaction> findAll() {
        String sql = "SELECT * FROM transactions ORDER BY created_at DESC";
        List<Transaction> result = new ArrayList<>();
        try (Connection conn = conn();
             Statement st = conn.createStatement();
             ResultSet rs  = st.executeQuery(sql)) {
            while (rs.next()) {
                Transaction t = mapRow(rs);
                loadItems(t);
                result.add(t);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Gagal mengambil transaksi: " + e.getMessage(), e);
        }
        return result;
    }
    public Optional<Transaction> findById(String transactionId) {
        String sql = "SELECT * FROM transactions WHERE id = ?";
        try (Connection conn = conn();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, transactionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Transaction t = mapRow(rs);
                    loadItems(t);
                    return Optional.of(t);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Gagal mencari transaksi: " + e.getMessage(), e);
        }
        return Optional.empty();
    }
    public int count() {
        String sql = "SELECT COUNT(*) FROM transactions";
        try (Connection conn = conn();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { return 0; }
        return 0;
    }
    public boolean isEmpty() { return count() == 0; }
    private Transaction mapRow(ResultSet rs) throws SQLException {
        String trxId    = rs.getString("id");
        String memberId = rs.getString("member_id");
        double subtotal = rs.getDouble("subtotal");
        double discount = rs.getDouble("member_discount");
        double total    = rs.getDouble("total");

        Transaction t = new Transaction();
        t.setTransactionId(trxId);
        t.setTotalsFromDb(subtotal, discount, total);

        if (memberId != null) {
            // Menggunakan memberRepository yang sudah di-inject
            memberRepository.findById(memberId).ifPresent(t::setMember);
        }
        return t;
    }

    private void loadItems(Transaction t) {
        String sql = """
        SELECT id, product_id, product_name,
               qty, price_per_item, subtotal
        FROM transaction_items
        WHERE transaction_id = ?
        ORDER BY id ASC
        """;
        try (
                Connection conn = conn();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, t.getTransactionId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String productId =
                            rs.getString("product_id");
                    String productName =
                            rs.getString("product_name");
                    int qty =
                            rs.getInt("qty");
                    double pricePerItem =
                            rs.getDouble("price_per_item");
                    double subtotalItem =
                            rs.getDouble("subtotal");
                    double discounted =
                            qty > 0
                                    ? subtotalItem / qty
                                    : pricePerItem;
                    Optional<Product> optProd =
                            productRepository.findById(productId);
                    TransactionItem item;
                    if (optProd.isPresent()) {
                        item = new TransactionItem(
                                optProd.get(),
                                qty,
                                pricePerItem,
                                discounted
                        );
                    } else {
                        item = new TransactionItem(
                                productId,
                                productName,
                                qty,
                                pricePerItem,
                                discounted
                        );
                    }
                    t.addItemFromDb(item);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Gagal memuat item transaksi: "
                            + e.getMessage(),
                    e
            );
        }
    }
}

