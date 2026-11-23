package sinarmart.model;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Model Pengelola Antrean Pengiriman Offline (Offline Queue)
 * Menyimpan transaksi saat offline dan melakukan sinkronisasi otomatis/manual ke server saat online.
 */
public class AntreanKirim {
    private static AntreanKirim instance;

    private final List<Transaksi> daftarTransaksi = Collections.synchronizedList(new ArrayList<>());
    private final File storageFile;

    public static synchronized AntreanKirim getInstance() {
        if (instance == null) {
            instance = new AntreanKirim();
        }
        return instance;
    }

    public AntreanKirim() {
        String userHome = System.getProperty("user.dir");
        File dir = new File(userHome, "data");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        this.storageFile = new File(dir, "offline_queue.txt");
        muatDariPenyimpananLokal();
    }

    public synchronized void tambahAntrean(Transaksi t) {
        if (t == null) return;
        
        // Cek jika sudah ada di daftar lokal
        boolean found = false;
        for (Transaksi existing : daftarTransaksi) {
            if (existing.getUniqueKey().equals(t.getUniqueKey())) {
                found = true;
                break;
            }
        }
        if (!found) {
            daftarTransaksi.add(t);
        }
        simpanKePenyimpananLokal();
    }

    public synchronized int sinkronkan(LayananServer server) {
        if (server == null || !server.isOnline()) {
            return 0; // Tidak dapat sinkronisasi karena server offline
        }

        int syncedCount = 0;
        for (Transaksi t : daftarTransaksi) {
            if ("PENDING".equalsIgnoreCase(t.getStatusKirim())) {
                try {
                    // Mengirim ke server (memanfaatkan deduplikasi idempotensi)
                    server.kirimTransaksi(t);
                    t.setStatusKirim("TERKIRIM");
                    syncedCount++;
                } catch (Exception ex) {
                    System.err.println("[ANTREAN] Gagal sinkron " + t.getNoTransaksi() + ": " + ex.getMessage());
                    break; // Berhenti jika koneksi putus di tengah jalan
                }
            }
        }

        if (syncedCount > 0) {
            simpanKePenyimpananLokal();
        }

        return syncedCount;
    }

    public synchronized int getJumlahPending() {
        int count = 0;
        for (Transaksi t : daftarTransaksi) {
            if ("PENDING".equalsIgnoreCase(t.getStatusKirim())) {
                count++;
            }
        }
        return count;
    }

    public synchronized int getJumlahTerkirim() {
        int count = 0;
        for (Transaksi t : daftarTransaksi) {
            if ("TERKIRIM".equalsIgnoreCase(t.getStatusKirim())) {
                count++;
            }
        }
        return count;
    }

    public List<Transaksi> getDaftarTransaksi() {
        return new ArrayList<>(daftarTransaksi);
    }

    private synchronized void simpanKePenyimpananLokal() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(storageFile))) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            for (Transaksi t : daftarTransaksi) {
                // Format baris: noTrx|uniqueKey|kasir|idShift|tanggal|totalAkhir|metode|statusKirim
                String metode = (t.getPembayaran() != null) ? t.getPembayaran().getMetode() : "Tunai";
                pw.println(String.format("%s|%s|%s|%s|%s|%.0f|%s|%s",
                    t.getNoTransaksi(),
                    t.getUniqueKey(),
                    t.getKasir(),
                    t.getIdShift() != null ? t.getIdShift() : "-",
                    sdf.format(t.getTanggal()),
                    t.getTotalAkhir(),
                    metode,
                    t.getStatusKirim()
                ));
            }
        } catch (IOException e) {
            System.err.println("[STORAGE WARNING] Gagal menulis offline queue: " + e.getMessage());
        }
    }

    private synchronized void muatDariPenyimpananLokal() {
        if (!storageFile.exists()) return;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        try (BufferedReader br = new BufferedReader(new FileReader(storageFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\\|");
                if (parts.length >= 8) {
                    String noTrx = parts[0];
                    String uniqueKey = parts[1];
                    String kasir = parts[2];
                    String idShift = parts[3];
                    String tglStr = parts[4];
                    double total = Double.parseDouble(parts[5]);
                    String metode = parts[6];
                    String statusKirim = parts[7];

                    Transaksi t = new Transaksi(noTrx, uniqueKey, sdf.parse(tglStr), kasir, idShift);
                    t.setStatusKirim(statusKirim);
                    t.setLunas(true);
                    if ("Tunai".equalsIgnoreCase(metode)) {
                        PembayaranTunai pt = new PembayaranTunai(total);
                        pt.validasi(total);
                        t.setPembayaran(pt);
                    } else {
                        PembayaranNonTunai pnt = new PembayaranNonTunai(metode, "OFFLINE-REC");
                        pnt.validasi(total);
                        t.setPembayaran(pnt);
                    }
                    daftarTransaksi.add(t);
                }
            }
        } catch (Exception e) {
            System.err.println("[STORAGE WARNING] Gagal memuat offline queue: " + e.getMessage());
        }
    }
}
