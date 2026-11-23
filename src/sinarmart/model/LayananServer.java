package sinarmart.model;

import sinarmart.database.Koneksi;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Layanan Server (Simulasi Backend / Sinkronisasi Online Sinar Mart)
 * Menerapkan Idempotency Deduplication menggunakan Unique Key (UUID)
 * untuk mencegah pencatatan transaksi ganda (Menyelesaikan Bukti 1 & 2).
 */
public class LayananServer {
    private static LayananServer instance;

    private boolean online = true;
    private final Set<String> processedKeys = Collections.synchronizedSet(new HashSet<>());
    private final Map<String, Transaksi> serverTransactions = Collections.synchronizedMap(new HashMap<>());

    public static synchronized LayananServer getInstance() {
        if (instance == null) {
            instance = new LayananServer();
        }
        return instance;
    }

    public LayananServer() {
        this.online = true;
    }

    /**
     * Mengirim transaksi ke server.
     * Mengembalikan true jika transaksi baru berhasil dicatat.
     * Mengembalikan false jika transaksi diabaikan karena duplikat (Idempotent).
     * Melempar Exception jika server sedang offline.
     */
    public synchronized boolean kirimTransaksi(Transaksi t) {
        if (!online) {
            throw new IllegalStateException("Koneksi jaringan terputus (Layanan Server Offline)!");
        }

        if (t == null) {
            throw new IllegalArgumentException("Payload transaksi tidak boleh kosong!");
        }

        // PENGECEKAN IDEMPOTENSI (MENCEGAH TRANSAKSI GANDA BUKTI 1 & 2)
        if (processedKeys.contains(t.getUniqueKey())) {
            System.out.println("[SERVER IDEMPOTENT] Kunci unik transaksi '" + t.getUniqueKey() +
                               "' sudah terdaftar. Mengabaikan duplikasi pengiriman!");
            return false; // Berhasil dicegah dari duplikasi!
        }

        // Simpan ke memori server
        processedKeys.add(t.getUniqueKey());
        serverTransactions.put(t.getNoTransaksi(), t);

        // Jika terhubung ke Database MySQL, simpan secara asinkron / persisten
        simpanKeDatabase(t);

        System.out.println("[SERVER SYNC] Transaksi " + t.getNoTransaksi() + " berhasil disinkronisasi ke server.");
        return true;
    }

    private void simpanKeDatabase(Transaksi t) {
        Connection conn = Koneksi.getKoneksi();
        if (conn == null) {
            return; // Mode offline / DB belum aktif, tetap aman di memori & file antrean
        }

        try {
            conn.setAutoCommit(false);

            // Insert Transaksi
            String sqlTrx = "INSERT INTO transaksi (no_transaksi, unique_key, id_shift, kasir, tanggal, " +
                            "subtotal, diskon_persen, nominal_diskon, total_akhir, metode_pembayaran, " +
                            "nominal_bayar, kembalian, status_kirim) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'TERKIRIM') " +
                            "ON DUPLICATE KEY UPDATE status_kirim='TERKIRIM'";
            try (PreparedStatement ps = conn.prepareStatement(sqlTrx)) {
                ps.setString(1, t.getNoTransaksi());
                ps.setString(2, t.getUniqueKey());
                ps.setString(3, t.getIdShift() != null ? t.getIdShift() : "SFT-DEFAULT");
                ps.setString(4, t.getKasir());
                ps.setTimestamp(5, new Timestamp(t.getTanggal().getTime()));
                ps.setDouble(6, t.getSubtotal());
                ps.setDouble(7, t.getDiskonPersen());
                ps.setDouble(8, t.getNominalDiskon());
                ps.setDouble(9, t.getTotalAkhir());
                ps.setString(10, t.getPembayaran() != null ? t.getPembayaran().getMetode() : "Tunai");
                double bayar = (t.getPembayaran() instanceof PembayaranTunai) 
                    ? ((PembayaranTunai) t.getPembayaran()).getUangDiterima() 
                    : t.getTotalAkhir();
                double kembali = (t.getPembayaran() instanceof PembayaranTunai) 
                    ? ((PembayaranTunai) t.getPembayaran()).getKembalian() 
                    : 0;
                ps.setDouble(11, bayar);
                ps.setDouble(12, kembali);
                ps.executeUpdate();
            }

            // Insert Detail Transaksi
            String sqlDetail = "INSERT INTO detail_transaksi (no_transaksi, kode_barang, nama_barang, jumlah, harga_satuan, subtotal) " +
                               "VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement psD = conn.prepareStatement(sqlDetail)) {
                for (DetailTransaksi d : t.getItems()) {
                    psD.setString(1, t.getNoTransaksi());
                    psD.setString(2, d.getBarang().getKodeBarang());
                    psD.setString(3, d.getBarang().getNamaBarang());
                    psD.setInt(4, d.getJumlah());
                    psD.setDouble(5, d.getHargaSatuan());
                    psD.setDouble(6, d.getSubtotal());
                    psD.addBatch();
                }
                psD.executeBatch();
            }

            conn.commit();
        } catch (SQLException ex) {
            try {
                conn.rollback();
            } catch (SQLException ignore) {}
            System.err.println("[DB SYNC WARNING] Gagal menyimpan ke DB: " + ex.getMessage());
        }
    }

    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }

    public boolean isKeyProcessed(String key) {
        return processedKeys.contains(key);
    }

    public int getJumlahTransaksiServer() {
        return serverTransactions.size();
    }

    public Map<String, Transaksi> getServerTransactions() {
        return serverTransactions;
    }

    public void reset() {
        processedKeys.clear();
        serverTransactions.clear();
        this.online = true;
    }
}
