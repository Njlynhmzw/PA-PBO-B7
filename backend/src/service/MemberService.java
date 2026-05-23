package service;
import models.Member;
import repository.MemberRepository;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class MemberService {
    private final MemberRepository memberRepository;
    // Regex email
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    // Regex nomor telepon (hanya angka, 10–15 angka)
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^\\d{10,15}$");
    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    // ── CREATE
    public Member daftarMember(String nama, String phone, String email, String tierStr) {
        // Validasi field
        if (nama == null || nama.isBlank() ||
                phone == null || phone.isBlank() ||
                email == null || email.isBlank()) {

            throw new IllegalArgumentException("Nama, nomor telepon, dan email wajib diisi!");
        }
        // Validasi format nomor telepon
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new IllegalArgumentException(
                    "Nomor telepon harus terdiri dari 10-15 digit angka!"
            );
        }
        // Validasi format email
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException(
                    "Format email tidak valid!"
            );
        }
        // Validasi duplikasi nomor
        if (memberRepository.existsByPhone(phone)) {
            throw new IllegalArgumentException(
                    "Nomor telepon " + phone + " sudah terdaftar!"
            );
        }
        Member.Tier tier =
                tierStr.equalsIgnoreCase("PLUS")
                        ? Member.Tier.PLUS
                        : Member.Tier.REGULAR;
        Member member = new Member(nama, phone, email, tier);
        memberRepository.save(member);
        return member;
    }
    // ── READ
    public List<Member> semuaMember() {
        return memberRepository.findAll();
    }
    public Optional<Member> cariByPhone(String phone) {
        return memberRepository.findByPhone(phone);
    }
    public Optional<Member> cariById(String memberId) {
        return memberRepository.findById(memberId);
    }
    public boolean isEmpty() {
        return memberRepository.isEmpty();
    }
    // ── UPDATE ──────────────────────────────────────────────────────
    public boolean updateMember(String id, String nama, String phone, String email, String tierStr) {
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new IllegalArgumentException("Format nomor telepon tidak valid!");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Format email tidak valid!");
        }
        Optional<Member> opt = memberRepository.findById(id);
        opt.ifPresent(m -> {
            m.setName(nama);
            m.setPhone(phone);
            m.setEmail(email);
            if (tierStr != null && !tierStr.isBlank()) {
                m.setTier(tierStr.equalsIgnoreCase("PLUS") ? Member.Tier.PLUS : Member.Tier.REGULAR);
            }
            memberRepository.save(m); //
        });
        return opt.isPresent();
    }
    // ── DELETE
    public boolean hapusMember(String id) {
        return memberRepository.delete(id);
    }
    // ── BUSINESS LOGIC
    public void catatTransaksi(Member member, double total) {
        member.tambahTransaksi(total);
    }
}