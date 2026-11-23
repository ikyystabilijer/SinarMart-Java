package sinarmart.model;

/**
 * Model Data Barang (Produk Toko Sinar Mart)
 */
public class Barang {
    private String kodeBarang;
    private String namaBarang;
    private String kategori;
    private String satuan;
    private double hargaBeli;
    private double hargaJual;
    private int stok;
    private int stokMin;
    private boolean aktif;

    public Barang() {
        this.aktif = true;
    }

    public Barang(String kodeBarang, String namaBarang, String kategori, String satuan,
                  double hargaBeli, double hargaJual, int stok, int stokMin, boolean aktif) {
        // Aturan Bisnis 1: Harga jual tidak boleh lebih rendah dari harga beli
        if (hargaJual < hargaBeli) {
            throw new IllegalArgumentException("Harga jual (" + hargaJual + ") tidak boleh lebih rendah dari harga beli (" + hargaBeli + ")!");
        }
        this.kodeBarang = kodeBarang;
        this.namaBarang = namaBarang;
        this.kategori = kategori;
        this.satuan = satuan;
        this.hargaBeli = hargaBeli;
        this.hargaJual = hargaJual;
        this.stok = stok;
        this.stokMin = stokMin;
        this.aktif = aktif;
    }

    public boolean cekStok(int jumlah) {
        return this.stok >= jumlah;
    }

    public void kurangiStok(int jumlah) {
        // Aturan Bisnis 2: Transaksi tidak dapat disimpan jika stok barang tidak mencukupi
        if (!cekStok(jumlah)) {
            throw new IllegalArgumentException("Stok barang '" + namaBarang + "' tidak mencukupi! (Sisa: " + stok + ", Diminta: " + jumlah + ")");
        }
        this.stok -= jumlah;
    }

    public void tambahStok(int jumlah) {
        if (jumlah > 0) {
            this.stok += jumlah;
        }
    }

    public boolean isDibawahStokMin() {
        return this.stok < this.stokMin;
    }

    // Getters and Setters
    public String getKodeBarang() { return kodeBarang; }
    public void setKodeBarang(String kodeBarang) { this.kodeBarang = kodeBarang; }

    public String getNamaBarang() { return namaBarang; }
    public void setNamaBarang(String namaBarang) { this.namaBarang = namaBarang; }

    public String getKategori() { return kategori; }
    public void setKategori(String kategori) { this.kategori = kategori; }

    public String getSatuan() { return satuan; }
    public void setSatuan(String satuan) { this.satuan = satuan; }

    public double getHargaBeli() { return hargaBeli; }
    public void setHargaBeli(double hargaBeli) {
        if (this.hargaJual > 0 && this.hargaJual < hargaBeli) {
            throw new IllegalArgumentException("Harga beli tidak boleh lebih tinggi dari harga jual!");
        }
        this.hargaBeli = hargaBeli;
    }

    public double getHargaJual() { return hargaJual; }
    public void setHargaJual(double hargaJual) {
        if (hargaJual < this.hargaBeli) {
            throw new IllegalArgumentException("Harga jual tidak boleh lebih rendah dari harga beli!");
        }
        this.hargaJual = hargaJual;
    }

    public int getStok() { return stok; }
    public void setStok(int stok) { this.stok = stok; }

    public int getStokMin() { return stokMin; }
    public void setStokMin(int stokMin) { this.stokMin = stokMin; }

    public boolean isAktif() { return aktif; }
    public void setAktif(boolean aktif) { this.aktif = aktif; }

    @Override
    public String toString() {
        return kodeBarang + " - " + namaBarang + " (Rp" + String.format("%,.0f", hargaJual) + ")";
    }
}
