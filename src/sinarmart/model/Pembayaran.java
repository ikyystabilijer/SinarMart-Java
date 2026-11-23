package sinarmart.model;

/**
 * Kelas Abstrak Pembayaran
 */
public abstract class Pembayaran {
    protected double nominalBayar;
    protected boolean statusLunas;

    public Pembayaran() {
        this.statusLunas = false;
        this.nominalBayar = 0;
    }

    public abstract String getMetode();

    public abstract void validasi(double totalTagihan);

    public double getNominalBayar() { return nominalBayar; }
    public void setNominalBayar(double nominalBayar) { this.nominalBayar = nominalBayar; }

    public boolean isStatusLunas() { return statusLunas; }
    public void setStatusLunas(boolean statusLunas) { this.statusLunas = statusLunas; }
}
