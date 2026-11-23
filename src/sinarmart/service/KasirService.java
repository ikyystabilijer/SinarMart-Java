package sinarmart.service;

import sinarmart.database.Koneksi;
import sinarmart.model.*;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

/**
 * Service Layer Pusat Logika Bisnis Toko Sinar Mart
 * Memisahkan logika bisnis dari antarmuka pengguna (GUI).
 */
public class KasirService {
    private static KasirService instance;

    private final Map<String, Barang> catalogBarang = new LinkedHashMap<>();
    private final List<Shift> riwayatShift = new ArrayList<>();
    private Shift shiftAktif = null;
    private int counterTransaksiHariIni = 0;
    private String tanggalCounter = "";
    private int counterShiftHariIni = 0;

    private final LayananServer server = LayananServer.getInstance();
    private final AntreanKirim antreanKirim = AntreanKirim.getInstance();

    public static synchronized KasirService getInstance() {
        if (instance == null) {
            instance = new KasirService();
        }
        return instance;
    }

    public KasirService() {
        inisialisasiKatalogAwal();
        muatBarangDariDatabase();
    }

    private void inisialisasiKatalogAwal() {
        // Data default sesuai kasus Sinar Mart
        tambahKatalog(new Barang("BRG001", "Beras 5 kg", "Sembako", "Sak", 60000, 68000, 20, 5, true));
        tambahKatalog(new Barang("BRG002", "Minyak 1 L", "Sembako", "Pouch", 15000, 18000, 30, 8, true));
        tambahKatalog(new Barang("BRG003", "Teh Botol", "Minuman", "Botol", 3800, 5000, 50, 12, true));
        tambahKatalog(new Barang("BRG004", "Gula Pasir 1 kg", "Sembako", "Bungkus", 14000, 17000, 25, 5, true));
        tambahKatalog(new Barang("BRG005", "Air Mineral 600ml", "Minuman", "Botol", 2500, 3500, 4, 10, true)); // Peringatan stok menipis
        tambahKatalog(new Barang("BRG006", "Mie Instan Goreng", "Makanan Ringan", "Bungkus", 2800, 3500, 80, 15, true));
        tambahKatalog(new Barang("BRG007", "Sabun Mandi 85g", "Perlengkapan Mandi", "Buah", 4000, 5500, 15, 5, true));
        tambahKatalog(new Barang("BRG008", "Shampoo 170ml", "Perlengkapan Mandi", "Botol", 19000, 24000, 12, 4, true));
        tambahKatalog(new Barang("BRG009", "Kopi Bubuk 150g", "Minuman", "Bungkus", 9500, 12500, 22, 6, true));
        tambahKatalog(new Barang("BRG010", "Pasta Gigi 120g", "Perlengkapan Mandi", "Tube", 11000, 14500, 18, 5, true));
    }

    private void tambahKatalog(Barang b) {
        catalogBarang.put(b.getKodeBarang().toUpperCase(), b);
    }

    public void muatBarangDariDatabase() {
        Connection conn = Koneksi.getKoneksi();
        if (conn == null) return;

        String sql = "SELECT * FROM barang";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Barang b = new Barang(
                    rs.getString("kode_barang"),
                    rs.getString("nama_barang"),
                    rs.getString("kategori"),
                    rs.getString("satuan"),
                    rs.getDouble("harga_beli"),
                    rs.getDouble("harga_jual"),
                    rs.getInt("stok"),
                    rs.getInt("stok_min"),
                    rs.getBoolean("aktif")
                );
                tambahKatalog(b);
            }
        } catch (SQLException e) {
            System.err.println("[SERVICE] Info DB barang: " + e.getMessage());
        }
    }

    // ==========================================
    // LOGIKA AUTENTIKASI PENGGUNA
    // ==========================================
    public static class UserInfo {
        public String username;
        public String nama;
        public String role;

        public UserInfo(String username, String nama, String role) {
            this.username = username;
            this.nama = nama;
            this.role = role;
        }
    }

    public UserInfo login(String username, String password) {
        // Cek database terlebih dahulu jika online
        Connection conn = Koneksi.getKoneksi();
        if (conn != null) {
            String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, password);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return new UserInfo(
                            rs.getString("username"),
                            rs.getString("nama"),
                            rs.getString("role")
                        );
                    }
                }
            } catch (SQLException ex) {
                System.err.println("[LOGIN DB INFO] " + ex.getMessage());
            }
        }

        // Fallback akun offline jika DB belum aktif
        if ("rina".equalsIgnoreCase(username) && "kasir123".equals(password)) {
            return new UserInfo("rina", "Rina", "kasir");
        } else if ("agus".equalsIgnoreCase(username) && "admin123".equals(password)) {
            return new UserInfo("agus", "Agus Gudang", "admin_gudang");
        } else if ("budi".equalsIgnoreCase(username) && "pemilik123".equals(password)) {
            return new UserInfo("budi", "Budi Santoso", "pemilik");
        }

        return null;
    }

    // ==========================================
    // LOGIKA SHIFT KASIR
    // ==========================================
    public synchronized Shift bukaShift(String kasir, double modalAwal) {
        // Aturan Bisnis 9: Satu kasir hanya boleh memiliki satu shift terbuka pada satu waktu
        if (shiftAktif != null && "OPEN".equalsIgnoreCase(shiftAktif.getStatus())) {
            if (shiftAktif.getKasir().equalsIgnoreCase(kasir)) {
                throw new IllegalStateException(
                    "Kasir '" + kasir + "' sudah memiliki shift yang masih TERBUKA (" +
                    shiftAktif.getIdShift() + "). Tutup shift lama terlebih dahulu!"
                );
            } else {
                throw new IllegalStateException(
                    "Masih ada shift terbuka oleh kasir '" + shiftAktif.getKasir() +
                    "'. Harap tutup shift tersebut sebelum membuka shift baru!"
                );
            }
        }

        String idShift = generateIdShift();
        Shift baru = new Shift(idShift, kasir, modalAwal);
        this.shiftAktif = baru;
        this.riwayatShift.add(baru);

        // Simpan ke DB jika ada koneksi
        simpanShiftKeDatabase(baru);

        return baru;
    }

    public synchronized void tutupShift(double uangFisikLaci) {
        if (shiftAktif == null || !"OPEN".equalsIgnoreCase(shiftAktif.getStatus())) {
            throw new IllegalStateException("Tidak ada shift aktif yang terbuka untuk ditutup!");
        }

        shiftAktif.tutupShift(uangFisikLaci);
        updateShiftKeDatabase(shiftAktif);
    }

    public Shift getShiftAktif() {
        return shiftAktif;
    }

    public List<Shift> getRiwayatShift() {
        return riwayatShift;
    }

    private void simpanShiftKeDatabase(Shift s) {
        Connection conn = Koneksi.getKoneksi();
        if (conn == null) return;
        String sql = "INSERT INTO shift (id_shift, kasir, waktu_buka, modal_awal, status) VALUES (?, ?, ?, ?, 'OPEN')";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getIdShift());
            ps.setString(2, s.getKasir());
            ps.setTimestamp(3, new Timestamp(s.getWaktuBuka().getTime()));
            ps.setDouble(4, s.getModalAwal());
            ps.executeUpdate();
        } catch (SQLException ignore) {}
    }

    private void updateShiftKeDatabase(Shift s) {
        Connection conn = Koneksi.getKoneksi();
        if (conn == null) return;
        String sql = "UPDATE shift SET waktu_tutup=?, total_tunai=?, total_nontunai=?, total_transaksi=?, " +
                     "uang_fisik_laci=?, selisih=?, status='CLOSED' WHERE id_shift=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, new Timestamp(s.getWaktuTutup().getTime()));
            ps.setDouble(2, s.getTotalTunai());
            ps.setDouble(3, s.getTotalNonTunai());
            ps.setInt(4, s.getTotalTransaksi());
            ps.setDouble(5, s.getUangFisikLaci());
            ps.setDouble(6, s.getSelisih());
            ps.setString(7, s.getIdShift());
            ps.executeUpdate();
        } catch (SQLException ignore) {}
    }

    // ==========================================
    // LOGIKA PENOMORAN TRANSAKSI OTOMATIS
    // ==========================================
    public synchronized String generateNoTransaksi() {
        // Aturan Bisnis 5: Nomor transaksi otomatis dan berurutan: TRX-yyyymmdd-nnnn
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String today = sdf.format(new Date());

        if (!today.equals(tanggalCounter)) {
            tanggalCounter = today;
            counterTransaksiHariIni = 0;
        }

        counterTransaksiHariIni++;
        return String.format("TRX-%s-%04d", today, counterTransaksiHariIni);
    }

    public synchronized String generateIdShift() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String today = sdf.format(new Date());
        counterShiftHariIni++;
        return String.format("SFT-%s-%04d", today, counterShiftHariIni);
    }

    // ==========================================
    // LOGIKA TRANSAKSI PENJUALAN
    // ==========================================
    public Transaksi buatTransaksiBaru(String kasir) {
        if (shiftAktif == null || !"OPEN".equalsIgnoreCase(shiftAktif.getStatus())) {
            throw new IllegalStateException("Kasir belum membuka shift! Silakan buka shift terlebih dahulu.");
        }
        String noTrx = generateNoTransaksi();
        return new Transaksi(noTrx, kasir, shiftAktif.getIdShift());
    }

    public void selesaikanTransaksi(Transaksi t, Pembayaran p) {
        if (t == null) {
            throw new IllegalArgumentException("Transaksi tidak valid!");
        }

        // Proses pembayaran dan pengurangan stok
        t.prosesPembayaran(p);

        // Catat ke shift aktif
        if (shiftAktif != null && "OPEN".equalsIgnoreCase(shiftAktif.getStatus())) {
            shiftAktif.catatTransaksi(t);
        }

        // Simpan ke antrean lokal offline
        antreanKirim.tambahAntrean(t);

        // Upayakan pengiriman ke server
        if (server.isOnline()) {
            try {
                boolean baru = server.kirimTransaksi(t);
                t.setStatusKirim("TERKIRIM");
                System.out.println("[TRANSAKSI SELESAI] " + t.getNoTransaksi() + " langsung terkirim ke server (Baru: " + baru + ")");
            } catch (Exception ex) {
                t.setStatusKirim("PENDING");
                System.err.println("[TRANSAKSI SELESAI] Jaringan terputus, disimpan ke antrean offline: " + ex.getMessage());
            }
        } else {
            t.setStatusKirim("PENDING");
            System.out.println("[TRANSAKSI SELESAI] Server offline, " + t.getNoTransaksi() + " tersimpan di antrean offline.");
        }
    }

    // ==========================================
    // PENGELOLAAN BARANG
    // ==========================================
    public Barang getBarang(String kode) {
        return catalogBarang.get(kode.toUpperCase());
    }

    public List<Barang> getSemuaBarang() {
        return new ArrayList<>(catalogBarang.values());
    }

    public List<Barang> cariBarang(String keyword) {
        List<Barang> hasil = new ArrayList<>();
        if (keyword == null || keyword.trim().isEmpty()) {
            return getSemuaBarang();
        }
        String kw = keyword.toLowerCase().trim();
        for (Barang b : catalogBarang.values()) {
            if (b.getKodeBarang().toLowerCase().contains(kw) ||
                b.getNamaBarang().toLowerCase().contains(kw) ||
                b.getKategori().toLowerCase().contains(kw)) {
                hasil.add(b);
            }
        }
        return hasil;
    }

    public List<Barang> getBarangStokMenipis() {
        List<Barang> list = new ArrayList<>();
        for (Barang b : catalogBarang.values()) {
            if (b.isDibawahStokMin()) {
                list.add(b);
            }
        }
        return list;
    }

    public void simpanBarang(Barang b) {
        // Simpan ke memori
        catalogBarang.put(b.getKodeBarang().toUpperCase(), b);

        // Simpan ke database jika online
        Connection conn = Koneksi.getKoneksi();
        if (conn != null) {
            String sql = "INSERT INTO barang (kode_barang, nama_barang, kategori, satuan, harga_beli, harga_jual, stok, stok_min, aktif) " +
                         "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                         "ON DUPLICATE KEY UPDATE nama_barang=VALUES(nama_barang), kategori=VALUES(kategori), " +
                         "satuan=VALUES(satuan), harga_beli=VALUES(harga_beli), harga_jual=VALUES(harga_jual), " +
                         "stok=VALUES(stok), stok_min=VALUES(stok_min), aktif=VALUES(aktif)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, b.getKodeBarang());
                ps.setString(2, b.getNamaBarang());
                ps.setString(3, b.getKategori());
                ps.setString(4, b.getSatuan());
                ps.setDouble(5, b.getHargaBeli());
                ps.setDouble(6, b.getHargaJual());
                ps.setInt(7, b.getStok());
                ps.setInt(8, b.getStokMin());
                ps.setBoolean(9, b.isAktif());
                ps.executeUpdate();
            } catch (SQLException ex) {
                System.err.println("[SERVICE] Gagal menyimpan barang ke DB: " + ex.getMessage());
            }
        }
    }

    public LayananServer getServer() { return server; }
    public AntreanKirim getAntreanKirim() { return antreanKirim; }
}
