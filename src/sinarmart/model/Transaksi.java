package sinarmart.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Model Transaksi Penjualan Toko Sinar Mart
 */
public class Transaksi {
    private String noTransaksi;
    private final String uniqueKey; // Idempotency key (dibuat sekali per transaksi)
    private Date tanggal;
    private String kasir;
    private String idShift;
    private final List<DetailTransaksi> items;
    private double subtotal;
    private double diskonPersen;
    private double nominalDiskon;
    private double totalAkhir;
    private Pembayaran pembayaran;
    private boolean lunas;
    private String statusKirim; // "PENDING" atau "TERKIRIM"

    public Transaksi(String noTransaksi, String kasir, String idShift) {
        this.noTransaksi = noTransaksi;
        this.uniqueKey = UUID.randomUUID().toString();
        this.tanggal = new Date();
        this.kasir = kasir;
        this.idShift = idShift;
        this.items = new ArrayList<>();
        this.subtotal = 0.0;
        this.diskonPersen = 0.0;
        this.nominalDiskon = 0.0;
        this.totalAkhir = 0.0;
        this.lunas = false;
        this.statusKirim = "PENDING";
    }

    public Transaksi(String noTransaksi, String uniqueKey, Date tanggal, String kasir, String idShift) {
        this.noTransaksi = noTransaksi;
        this.uniqueKey = (uniqueKey != null && !uniqueKey.trim().isEmpty()) ? uniqueKey : UUID.randomUUID().toString();
        this.tanggal = (tanggal != null) ? tanggal : new Date();
        this.kasir = kasir;
        this.idShift = idShift;
        this.items = new ArrayList<>();
        this.subtotal = 0.0;
        this.diskonPersen = 0.0;
        this.nominalDiskon = 0.0;
        this.totalAkhir = 0.0;
        this.lunas = false;
        this.statusKirim = "PENDING";
    }

    private void cekStatusBelumLunas() {
        // Aturan Bisnis 6: Transaksi yang sudah lunas tidak boleh diubah atau dihapus
        if (this.lunas) {
            throw new IllegalStateException("Transaksi " + noTransaksi + " sudah lunas! Tidak boleh diubah atau dihapus.");
        }
    }

    public void tambahItem(Barang barang, int qty) {
        cekStatusBelumLunas();
        if (barang == null) {
            throw new IllegalArgumentException("Barang tidak boleh kosong!");
        }
        if (qty <= 0) {
            throw new IllegalArgumentException("Jumlah barang harus minimal 1!");
        }

        // Cek apakah barang sudah ada di keranjang
        DetailTransaksi itemExisting = null;
        for (DetailTransaksi d : items) {
            if (d.getBarang().getKodeBarang().equalsIgnoreCase(barang.getKodeBarang())) {
                itemExisting = d;
                break;
            }
        }

        int targetQty = qty;
        if (itemExisting != null) {
            targetQty = itemExisting.getJumlah() + qty;
        }

        // Aturan Bisnis 2: Transaksi tidak dapat disimpan jika stok barang tidak mencukupi
        if (!barang.cekStok(targetQty)) {
            throw new IllegalArgumentException(
                "Stok barang '" + barang.getNamaBarang() + "' tidak mencukupi! (Stok tersedia: " +
                barang.getStok() + ", diminta: " + targetQty + ")"
            );
        }

        if (itemExisting != null) {
            itemExisting.setJumlah(targetQty);
        } else {
            items.add(new DetailTransaksi(barang, qty));
        }

        hitungTotal();
    }

    public void ubahJumlahItem(int index, int newQty) {
        cekStatusBelumLunas();
        if (index < 0 || index >= items.size()) {
            throw new IndexOutOfBoundsException("Baris item transaksi tidak valid!");
        }
        if (newQty <= 0) {
            throw new IllegalArgumentException("Jumlah harus minimal 1!");
        }
        DetailTransaksi item = items.get(index);
        if (!item.getBarang().cekStok(newQty)) {
            throw new IllegalArgumentException(
                "Stok barang '" + item.getBarang().getNamaBarang() + "' tidak mencukupi! (Stok: " +
                item.getBarang().getStok() + ", diminta: " + newQty + ")"
            );
        }
        item.setJumlah(newQty);
        hitungTotal();
    }

    public void hapusItem(int index) {
        cekStatusBelumLunas();
        if (index < 0 || index >= items.size()) {
            throw new IndexOutOfBoundsException("Baris item transaksi tidak valid!");
        }
        items.remove(index);
        hitungTotal();
    }

    public void hitungTotal() {
        this.subtotal = 0.0;
        for (DetailTransaksi item : items) {
            this.subtotal += item.hitungSubtotal();
        }

        // Aturan Bisnis 3: Diskon 5% berlaku untuk total belanja di atas Rp100.000 (> 100.000; tepat Rp100.000 tidak diskon)
        if (this.subtotal > 100000.0) {
            this.diskonPersen = 5.0;
            this.nominalDiskon = (this.subtotal * 5.0) / 100.0;
        } else {
            this.diskonPersen = 0.0;
            this.nominalDiskon = 0.0;
        }

        this.totalAkhir = this.subtotal - this.nominalDiskon;
    }

    public void prosesPembayaran(Pembayaran p) {
        cekStatusBelumLunas();
        if (items.isEmpty()) {
            throw new IllegalStateException("Transaksi tidak memiliki item belanja!");
        }
        if (p == null) {
            throw new IllegalArgumentException("Metode pembayaran harus ditentukan!");
        }

        // Hitung ulang untuk memastikan konsistensi
        hitungTotal();

        // Validasi pembayaran (misal uang tunai cukup)
        p.validasi(this.totalAkhir);

        // Interaksi objek: Transaksi mengurangi stok barang
        for (DetailTransaksi item : items) {
            item.getBarang().kurangiStok(item.getJumlah());
        }

        this.pembayaran = p;
        this.lunas = true;
    }

    // Getters and Setters
    public String getNoTransaksi() { return noTransaksi; }
    public void setNoTransaksi(String noTransaksi) { this.noTransaksi = noTransaksi; }

    public String getUniqueKey() { return uniqueKey; }

    public Date getTanggal() { return tanggal; }
    public void setTanggal(Date tanggal) { this.tanggal = tanggal; }

    public String getKasir() { return kasir; }
    public void setKasir(String kasir) { this.kasir = kasir; }

    public String getIdShift() { return idShift; }
    public void setIdShift(String idShift) { this.idShift = idShift; }

    public List<DetailTransaksi> getItems() { return items; }

    public double getSubtotal() { return subtotal; }
    public double getDiskonPersen() { return diskonPersen; }
    public double getNominalDiskon() { return nominalDiskon; }
    public double getTotalAkhir() { return totalAkhir; }

    public Pembayaran getPembayaran() { return pembayaran; }
    public void setPembayaran(Pembayaran pembayaran) { this.pembayaran = pembayaran; }

    public boolean isLunas() { return lunas; }
    public void setLunas(boolean lunas) { this.lunas = lunas; }

    public String getStatusKirim() { return statusKirim; }
    public void setStatusKirim(String statusKirim) { this.statusKirim = statusKirim; }
}
