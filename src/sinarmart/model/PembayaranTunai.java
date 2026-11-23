package sinarmart.model;

/**
 * Turunan Pembayaran Tunai
 */
public class PembayaranTunai extends Pembayaran {
    private double uangDiterima;
    private double kembalian;

    public PembayaranTunai(double uangDiterima) {
        super();
        this.uangDiterima = uangDiterima;
    }

    @Override
    public String getMetode() {
        return "Tunai";
    }

    @Override
    public void validasi(double totalTagihan) {
        // Aturan Bisnis 4: Pembayaran tunai kurang dari total belanja ditolak; kelebihan dikembalikan
        if (uangDiterima < totalTagihan) {
            double kekurangan = totalTagihan - uangDiterima;
            throw new IllegalArgumentException(
                String.format("Pembayaran tunai kurang! Tagihan: Rp%,.0f, Diterima: Rp%,.0f (Kurang Rp%,.0f)",
                              totalTagihan, uangDiterima, kekurangan)
            );
        }
        this.kembalian = this.uangDiterima - totalTagihan;
        this.nominalBayar = totalTagihan;
        this.statusLunas = true;
    }

    public double getUangDiterima() { return uangDiterima; }
    public void setUangDiterima(double uangDiterima) { this.uangDiterima = uangDiterima; }

    public double getKembalian() { return kembalian; }
}
