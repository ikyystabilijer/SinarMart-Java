package sinarmart.model;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;

/**
 * Model Pembuatan dan Pencetakan Struk Transaksi Sinar Mart
 * Format disesuaikan secara presisi dengan stimulus soal.
 */
public class Struk {
    private final Transaksi transaksi;

    public Struk(Transaksi transaksi) {
        if (transaksi == null) {
            throw new IllegalArgumentException("Transaksi tidak boleh kosong!");
        }
        this.transaksi = transaksi;
    }

    public String cetak() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        String tglStr = sdf.format(transaksi.getTanggal());

        StringBuilder sb = new StringBuilder();
        sb.append("        TOKO SINAR MART\n");
        sb.append("   Jl. Mawar No. 12, Kota Contoh\n");
        sb.append("----------------------------------\n");
        sb.append(String.format("No   : %s\n", transaksi.getNoTransaksi()));
        sb.append(String.format("Tgl  : %s\n", tglStr));
        sb.append(String.format("Kasir: %s\n", transaksi.getKasir()));
        sb.append("----------------------------------\n");

        for (DetailTransaksi item : transaksi.getItems()) {
            String nama = item.getBarang().getNamaBarang();
            if (nama.length() > 14) {
                nama = nama.substring(0, 14);
            }
            int qty = item.getJumlah();
            double harga = item.getHargaSatuan();
            double sub = item.getSubtotal();

            // Format kolom: [Nama 14 char] [qty x harga] [subtotal]
            String leftCol = String.format("%-14s", nama);
            String middleCol = String.format("%2d x %7s", qty, formatUang(harga));
            String rightCol = String.format("%8s", formatUang(sub));
            sb.append(leftCol).append("  ").append(middleCol).append("  ").append(rightCol).append("\n");
        }

        sb.append("----------------------------------\n");
        sb.append(String.format("%-25s %8s\n", "Subtotal", formatUang(transaksi.getSubtotal())));

        if (transaksi.getDiskonPersen() > 0) {
            String diskonStr = "-" + formatUang(transaksi.getNominalDiskon());
            sb.append(String.format("%-25s %8s\n", "Diskon 5%", diskonStr));
        }

        sb.append(String.format("%-25s %8s\n", "TOTAL", formatUang(transaksi.getTotalAkhir())));

        if (transaksi.getPembayaran() instanceof PembayaranTunai) {
            PembayaranTunai pt = (PembayaranTunai) transaksi.getPembayaran();
            sb.append(String.format("%-25s %8s\n", "Tunai", formatUang(pt.getUangDiterima())));
            sb.append(String.format("%-25s %8s\n", "Kembali", formatUang(pt.getKembalian())));
        } else if (transaksi.getPembayaran() instanceof PembayaranNonTunai) {
            PembayaranNonTunai pnt = (PembayaranNonTunai) transaksi.getPembayaran();
            sb.append(String.format("%-25s %8s\n", pnt.getMetode(), formatUang(pnt.getNominalBayar())));
            sb.append(String.format("%-15s: %s\n", "Ref", pnt.getNomorReferensi()));
        }

        sb.append("----------------------------------\n");
        sb.append("  Terima kasih, selamat belanja\n");

        return sb.toString();
    }

    public File simpanKeFile(String direktoriPath) throws IOException {
        File dir = new File(direktoriPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String fileName = transaksi.getNoTransaksi() + ".txt";
        File file = new File(dir, fileName);

        try (FileWriter fw = new FileWriter(file)) {
            fw.write(cetak());
        }

        return file;
    }

    private String formatUang(double nominal) {
        return String.format("%,.0f", nominal).replace(',', '.');
    }

    public Transaksi getTransaksi() {
        return transaksi;
    }
}
