package repository;

import db.DatabaseConfig;
import models.Member;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MemberRepository {

    private Connection conn() { return DatabaseConfig.getConnection(); }

    private String generateId() {
        String updateSql = "UPDATE counters SET value = value + 1 WHERE name = 'member'";
        String selectSql = "SELECT value FROM counters WHERE name = 'member'";

        // Membungkus koneksi agar otomatis tertutup (Resource Leak fix)
        try (Connection conn = conn()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psUpdate = conn.prepareStatement(updateSql);
                 PreparedStatement psSelect = conn.prepareStatement(selectSql)) {

                psUpdate.executeUpdate();
                try (ResultSet rs = psSelect.executeQuery()) {
                    if (rs.next()) {
                        int num = rs.getInt(1);
                        conn.commit();
                        return String.format("MEM-%04d", num);
                    }
                }
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Gagal generate member ID: " + e.getMessage(), e);
        }
        return null;
    }

    public void save(Member member) {
        if (member.getMemberId() == null || member.getMemberId().isBlank()) {
            member.setMemberId(generateId());
        }

        String sql = """
            INSERT INTO members
                (id, name, phone, email, tier, total_transaksi, total_belanja)
            VALUES (?,?,?,?,?,?,?)
            ON DUPLICATE KEY UPDATE
                name=VALUES(name), phone=VALUES(phone), email=VALUES(email),
                tier=VALUES(tier), total_transaksi=VALUES(total_transaksi),
                total_belanja=VALUES(total_belanja)
            """;

        try (Connection conn = conn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString (1, member.getMemberId());
            ps.setString (2, member.getName());
            ps.setString (3, member.getPhone());
            ps.setString (4, member.getEmail());
            ps.setString (5, member.getTier().name());
            ps.setInt    (6, member.getTotalTransaksi());
            ps.setDouble (7, member.getTotalBelanja());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Gagal menyimpan member: " + e.getMessage(), e);
        }
    }

    public void update(Member member) {
        save(member);
    }

    public Optional<Member> findById(String id) {
        String sql = "SELECT * FROM members WHERE id = ?";
        try (Connection conn = conn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Gagal mencari member by ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public Optional<Member> findByPhone(String phone) {
        String sql = "SELECT * FROM members WHERE phone = ?";
        try (Connection conn = conn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, phone);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Gagal mencari member by Phone: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public List<Member> findAll() {
        String sql = "SELECT * FROM members ORDER BY created_at DESC";
        List<Member> result = new ArrayList<>();

        try (Connection conn = conn();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) result.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Gagal mengambil semua member: " + e.getMessage(), e);
        }
        return result;
    }

    public boolean delete(String memberId) {
        String sql = "DELETE FROM members WHERE id = ?";
        try (Connection conn = conn();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, memberId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Gagal menghapus member: " + e.getMessage(), e);
        }
    }

    public boolean existsByPhone(String phone) {
        return findByPhone(phone).isPresent();
    }

    public int count() {
        String sql = "SELECT COUNT(*) FROM members";
        try (Connection conn = conn();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { return 0; }
        return 0;
    }

    public boolean isEmpty() { return count() == 0; }

    private Member mapRow(ResultSet rs) throws SQLException {
        String id     = rs.getString("id");
        String name   = rs.getString("name");
        String phone  = rs.getString("phone");
        String email  = rs.getString("email");
        Member.Tier tier = Member.Tier.valueOf(rs.getString("tier"));
        int    totalTrx  = rs.getInt("total_transaksi");
        double totalBel  = rs.getDouble("total_belanja");

        return new Member(id, name, phone, email, tier, totalTrx, totalBel);
    }
}