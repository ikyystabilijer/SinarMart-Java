package sinarmart.model;

/**
 * Model Detail Item pada Transaksi
 */
public class DetailTransaksi {
    private Barang barang;
    private int jumlah;
    private double hargaSatuan;
    private double subtotal;

    public DetailTransaksi(Barang barang, int jumlah) {
        if (barang == null) {
            throw new IllegalArgumentException("Barang tidak boleh kosong!");
        }
        if (jumlah <= 0) {
            throw new IllegalArgumentException("Jumlah barang harus lebih dari 0!");
        }
        this.barang = barang;
        this.jumlah = jumlah;
        this.hargaSatuan = barang.getHargaJual();
        this.subtotal = hitungSubtotal();
    }

    public DetailTransaksi(Barang barang, int jumlah, double hargaSatuan) {
        if (barang == null) {
            throw new IllegalArgumentException("Barang tidak boleh kosong!");
        }
        if (jumlah <= 0) {
            throw new IllegalArgumentException("Jumlah barang harus lebih dari 0!");
        }
        this.barang = barang;
        this.jumlah = jumlah;
        this.hargaSatuan = hargaSatuan;
        this.subtotal = hitungSubtotal();
    }

    public double hitungSubtotal() {
        this.subtotal = this.jumlah * this.hargaSatuan;
        return this.subtotal;
    }

    public void setJumlah(int jumlah) {
        if (jumlah <= 0) {
            throw new IllegalArgumentException("Jumlah barang harus lebih dari 0!");
        }
        this.jumlah = jumlah;
        hitungSubtotal();
    }

    public Barang getBarang() { return barang; }
    public void setBarang(Barang barang) { this.barang = barang; }

    public int getJumlah() { return jumlah; }
    public double getHargaSatuan() { return hargaSatuan; }
    public void setHargaSatuan(double hargaSatuan) { 
        this.hargaSatuan = hargaSatuan; 
        hitungSubtotal();
    }

    public double getSubtotal() { return subtotal; }
}
