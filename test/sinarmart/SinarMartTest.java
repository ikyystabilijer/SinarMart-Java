package sinarmart;

import sinarmart.model.*;
import sinarmart.service.KasirService;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit Testing Komprehensif Toko Sinar Mart
 * Menguji seluruh aturan bisnis dan skenario stimulus SAS.
 */
public class SinarMartTest {

    private KasirService service;

    @Before
    public void setUp() {
        service = KasirService.getInstance();
        service.getServer().reset();
    }

    /**
     * Uji 1: Logika Diskon 5% untuk belanja di atas Rp 100.000 (Kasus Bukti 1)
     * Beras 1 x 68.000 = 68.000
     * Minyak 2 x 18.000 = 36.000
     * Teh Botol 3 x 5.000 = 15.000
     * Subtotal = 119.000 (> 100.000) -> Diskon 5% = 5.950 -> TOTAL = 113.050
     */
    @Test
    public void testDiskon5PersenBelanjaDiatas100Ribu() {
        Barang beras = new Barang("TEST01", "Beras 5 kg", "Sembako", "Sak", 60000, 68000, 10, 2, true);
        Barang minyak = new Barang("TEST02", "Minyak 1 L", "Sembako", "Pouch", 15000, 18000, 10, 2, true);
        Barang teh = new Barang("TEST03", "Teh Botol", "Minuman", "Botol", 3800, 5000, 10, 2, true);

        Transaksi t = new Transaksi("TRX-TEST-0001", "Rina", "SFT-001");
        t.tambahItem(beras, 1);
        t.tambahItem(minyak, 2);
        t.tambahItem(teh, 3);

        assertEquals(119000.0, t.getSubtotal(), 0.001);
        assertEquals(5.0, t.getDiskonPersen(), 0.001);
        assertEquals(5950.0, t.getNominalDiskon(), 0.001);
        assertEquals(113050.0, t.getTotalAkhir(), 0.001);
    }

    /**
     * Uji 2: Aturan Bisnis 3: Tepat Rp 100.000 atau kurang TIDAK mendapatkan diskon
     */
    @Test
    public void testTanpaDiskonJikaTepatAtauKurang100Ribu() {
        // Tepat 100.000
        Barang b1 = new Barang("TEST04", "Barang 50rb", "Lain", "Pcs", 40000, 50000, 10, 2, true);
        Transaksi t1 = new Transaksi("TRX-TEST-0002", "Rina", "SFT-001");
        t1.tambahItem(b1, 2); // 2 x 50.000 = 100.000

        assertEquals(100000.0, t1.getSubtotal(), 0.001);
        assertEquals(0.0, t1.getDiskonPersen(), 0.001);
        assertEquals(0.0, t1.getNominalDiskon(), 0.001);
        assertEquals(100000.0, t1.getTotalAkhir(), 0.001);

        // Kurang dari 100.000 (misal 50.000)
        Transaksi t2 = new Transaksi("TRX-TEST-0003", "Rina", "SFT-001");
        t2.tambahItem(b1, 1);
        assertEquals(50000.0, t2.getSubtotal(), 0.001);
        assertEquals(0.0, t2.getNominalDiskon(), 0.001);
        assertEquals(50000.0, t2.getTotalAkhir(), 0.001);
    }

    /**
     * Uji 3: Aturan Bisnis 2: Tolak transaksi jika stok barang tidak mencukupi
     */
    @Test(expected = IllegalArgumentException.class)
    public void testValidasiStokKurangDitolak() {
        Barang b = new Barang("TEST05", "Air Mineral", "Minuman", "Botol", 2500, 3500, 3, 5, true);
        Transaksi t = new Transaksi("TRX-TEST-0004", "Rina", "SFT-001");
        // Minta 4 sedangkan stok hanya 3 -> harus throw IllegalArgumentException
        t.tambahItem(b, 4);
    }

    /**
     * Uji 4: Aturan Bisnis 4: Pembayaran tunai kurang dari total belanja ditolak
     */
    @Test(expected = IllegalArgumentException.class)
    public void testValidasiPembayaranTunaiKurangDitolak() {
        Barang b = new Barang("TEST06", "Beras 5 kg", "Sembako", "Sak", 60000, 68000, 10, 2, true);
        Transaksi t = new Transaksi("TRX-TEST-0005", "Rina", "SFT-001");
        t.tambahItem(b, 1); // Total: 68.000

        // Pembeli membayar 50.000 (kurang dari 68.000) -> harus ditolak
        PembayaranTunai pt = new PembayaranTunai(50000);
        t.prosesPembayaran(pt);
    }

    /**
     * Uji 5: Aturan Bisnis 6: Transaksi yang sudah lunas tidak boleh diubah atau dihapus
     */
    @Test(expected = IllegalStateException.class)
    public void testImmutabilityTransaksiLunas() {
        Barang b = new Barang("TEST07", "Minyak 1 L", "Sembako", "Pouch", 15000, 18000, 10, 2, true);
        Transaksi t = new Transaksi("TRX-TEST-0006", "Rina", "SFT-001");
        t.tambahItem(b, 1);

        PembayaranTunai pt = new PembayaranTunai(20000);
        t.prosesPembayaran(pt);
        assertTrue(t.isLunas());
        assertEquals(2000.0, pt.getKembalian(), 0.001);

        // Percobaan mengubah item setelah lunas harus melempar IllegalStateException
        t.hapusItem(0);
    }

    /**
     * Uji 6: Tugas 4 & Bukti 1-2: Mekanisme Idempotensi mencegah transaksi ganda saat sinkronisasi / kirim ulang
     */
    @Test
    public void testIdempotencyKirimUlangAmanTanpaDuplikasi() {
        LayananServer server = service.getServer();
        server.reset();
        server.setOnline(true);

        Barang b = new Barang("TEST08", "Teh Botol", "Minuman", "Botol", 3800, 5000, 20, 5, true);
        Transaksi t = new Transaksi("TRX-20261205-0007", "Rina", "SFT-001");
        t.tambahItem(b, 2);
        PembayaranTunai pt = new PembayaranTunai(15000);
        t.prosesPembayaran(pt);

        // Pengiriman pertama: berhasil
        boolean kirimPertama = server.kirimTransaksi(t);
        assertTrue("Kirim pertama harus berhasil dicatat", kirimPertama);
        assertEquals(1, server.getJumlahTransaksiServer());

        // Pengiriman kedua (simulasi kasus Bukti 2: kirim ulang akibat jaringan putus / respon hilang)
        boolean kirimKedua = server.kirimTransaksi(t);
        assertFalse("Kirim kedua harus diabaikan (idempotent duplicate prevention)", kirimKedua);
        assertEquals("Server tetap hanya memiliki 1 transaksi, tidak ada duplikasi ganda!", 1, server.getJumlahTransaksiServer());
    }

    /**
     * Uji 7: Aturan Bisnis 9: Satu kasir hanya boleh memiliki satu shift terbuka pada satu waktu
     */
    @Test(expected = IllegalStateException.class)
    public void testPembukaanShiftKeduaDitolak() {
        service.bukaShift("KasirTest", 200000);
        // Membuka shift lagi saat shift sebelumnya masih OPEN -> harus throw IllegalStateException
        service.bukaShift("KasirTest", 150000);
    }

    /**
     * Uji 8: Format Struk Sesuai Stimulus Soal
     */
    @Test
    public void testFormatStrukSesuaiStimulus() {
        Barang beras = new Barang("BRG001", "Beras 5 kg", "Sembako", "Sak", 60000, 68000, 20, 5, true);
        Barang minyak = new Barang("BRG002", "Minyak 1 L", "Sembako", "Pouch", 15000, 18000, 30, 8, true);
        Barang teh = new Barang("BRG003", "Teh Botol", "Minuman", "Botol", 3800, 5000, 50, 12, true);

        Transaksi t = new Transaksi("TRX-20261205-0007", "Rina", "SFT-001");
        t.tambahItem(beras, 1);
        t.tambahItem(minyak, 2);
        t.tambahItem(teh, 3);

        PembayaranTunai pt = new PembayaranTunai(120000);
        t.prosesPembayaran(pt);

        Struk struk = new Struk(t);
        String cetak = struk.cetak();

        assertTrue(cetak.contains("TOKO SINAR MART"));
        assertTrue(cetak.contains("TRX-20261205-0007"));
        assertTrue(cetak.contains("Subtotal"));
        assertTrue(cetak.contains("119.000"));
        assertTrue(cetak.contains("Diskon 5%"));
        assertTrue(cetak.contains("-5.950"));
        assertTrue(cetak.contains("TOTAL"));
        assertTrue(cetak.contains("113.050"));
        assertTrue(cetak.contains("Tunai"));
        assertTrue(cetak.contains("120.000"));
        assertTrue(cetak.contains("Kembali"));
        assertTrue(cetak.contains("6.950"));
        assertTrue(cetak.contains("Terima kasih, selamat belanja"));
    }
}
