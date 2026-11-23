package sinarmart.model;

import java.util.Date;

/**
 * Model Shift Kerja Kasir Toko Sinar Mart
 */
public class Shift {
    private String idShift;
    private String kasir;
    private Date waktuBuka;
    private Date waktuTutup;
    private double modalAwal;
    private double totalTunai;
    private double totalNonTunai;
    private int totalTransaksi;
    private double uangFisikLaci;
    private double selisih;
    private String status; // "OPEN" atau "CLOSED"

    public Shift(String idShift, String kasir, double modalAwal) {
        if (modalAwal < 0) {
            throw new IllegalArgumentException("Modal awal tidak boleh negatif!");
        }
        this.idShift = idShift;
        this.kasir = kasir;
        this.modalAwal = modalAwal;
        this.waktuBuka = new Date();
        this.totalTunai = 0.0;
        this.totalNonTunai = 0.0;
        this.totalTransaksi = 0;
        this.uangFisikLaci = 0.0;
        this.selisih = 0.0;
        this.status = "OPEN";
    }

    public void catatTransaksi(Transaksi t) {
        if (!"OPEN".equalsIgnoreCase(this.status)) {
            throw new IllegalStateException("Tidak dapat mencatat transaksi pada shift yang sudah ditutup!");
        }
        if (t == null || !t.isLunas()) {
            throw new IllegalArgumentException("Hanya transaksi yang sudah lunas yang dapat dicatat ke shift!");
        }

        if (t.getPembayaran() instanceof PembayaranTunai) {
            this.totalTunai += t.getTotalAkhir();
        } else {
            this.totalNonTunai += t.getTotalAkhir();
        }
        this.totalTransaksi++;
    }

    public double hitungUangSeharusnya() {
        return this.modalAwal + this.totalTunai;
    }

    public void tutupShift(double uangFisik) {
        if (!"OPEN".equalsIgnoreCase(this.status)) {
            throw new IllegalStateException("Shift ini sudah ditutup sebelumnya!");
        }
        this.uangFisikLaci = uangFisik;
        this.selisih = uangFisik - hitungUangSeharusnya();
        this.status = "CLOSED";
        this.waktuTutup = new Date();
    }

    // Getters and Setters
    public String getIdShift() { return idShift; }
    public void setIdShift(String idShift) { this.idShift = idShift; }

    public String getKasir() { return kasir; }
    public void setKasir(String kasir) { this.kasir = kasir; }

    public Date getWaktuBuka() { return waktuBuka; }
    public void setWaktuBuka(Date waktuBuka) { this.waktuBuka = waktuBuka; }

    public Date getWaktuTutup() { return waktuTutup; }
    public void setWaktuTutup(Date waktuTutup) { this.waktuTutup = waktuTutup; }

    public double getModalAwal() { return modalAwal; }
    public void setModalAwal(double modalAwal) { this.modalAwal = modalAwal; }

    public double getTotalTunai() { return totalTunai; }
    public void setTotalTunai(double totalTunai) { this.totalTunai = totalTunai; }

    public double getTotalNonTunai() { return totalNonTunai; }
    public void setTotalNonTunai(double totalNonTunai) { this.totalNonTunai = totalNonTunai; }

    public int getTotalTransaksi() { return totalTransaksi; }
    public void setTotalTransaksi(int totalTransaksi) { this.totalTransaksi = totalTransaksi; }

    public double getUangFisikLaci() { return uangFisikLaci; }
    public void setUangFisikLaci(double uangFisikLaci) { this.uangFisikLaci = uangFisikLaci; }

    public double getSelisih() { return selisih; }
    public void setSelisih(double selisih) { this.selisih = selisih; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
