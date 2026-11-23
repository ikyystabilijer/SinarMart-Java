package sinarmart.model;

/**
 * Turunan Pembayaran Non-Tunai (QRIS / Transfer Bank)
 */
public class PembayaranNonTunai extends Pembayaran {
    private String jenisMetode; // "QRIS" atau "Transfer Bank"
    private String nomorReferensi;

    public PembayaranNonTunai(String jenisMetode, String nomorReferensi) {
        super();
        this.jenisMetode = jenisMetode;
        this.nomorReferensi = nomorReferensi;
    }

    @Override
    public String getMetode() {
        return jenisMetode;
    }

    @Override
    public void validasi(double totalTagihan) {
        if (nomorReferensi == null || nomorReferensi.trim().isEmpty()) {
            throw new IllegalArgumentException("Nomor referensi / bukti pembayaran " + jenisMetode + " wajib diisi!");
        }
        this.nominalBayar = totalTagihan;
        this.statusLunas = true;
    }

    public String getJenisMetode() { return jenisMetode; }
    public void setJenisMetode(String jenisMetode) { this.jenisMetode = jenisMetode; }

    public String getNomorReferensi() { return nomorReferensi; }
    public void setNomorReferensi(String nomorReferensi) { this.nomorReferensi = nomorReferensi; }
}
