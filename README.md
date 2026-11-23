# APLIKASI POINT OF SALE (POS) TOKO SINAR MART
**SMK Bina Informatika - Sumatif Akhir Semester (SAS)**  
**Mata Pelajaran:** Pemrograman Berbasis Teks dan Grafis (PTG)  
**Tingkat / Keahlian:** XI / Rekayasa Perangkat Lunak (RPL)

---

## 1. DESKRIPSI PROYEK
Aplikasi Kasir POS Desktop berbasis Graphical User Interface (Java Swing JFrame Form) untuk memodernisasi Toko Sinar Mart dengan implementasi interaksi antar objek (OOP), validasi ketat 10 aturan bisnis, pencetakan struk transaksi, dan mekanisme sinkronisasi offline-to-online yang aman dari pencatatan ganda (*Idempotency Deduplication*).

---

## 2. STRUKTUR DAN DESAIN KELAS (OOP)

Aplikasi dirancang dengan arsitektur bersih (*Separation of Concerns*), memisahkan Model Data, Logika Bisnis (*Service*), dan Antarmuka Pengguna (*GUI JFrame Form*):

- **`sinarmart.model.Barang`**: Menyimpan atribut barang, validasi harga jual $\ge$ harga beli (Aturan 1), metode pengecekan dan pengurangan stok (Aturan 2), serta batas minimum stok (Aturan 8).
- **`sinarmart.model.DetailTransaksi`**: Menyimpan barang, jumlah beli, harga satuan saat transaksi, dan kalkulasi subtotal.
- **`sinarmart.model.Transaksi`**: Inti transaksi yang menghasilkan nomor otomatis berurutan `TRX-yyyyMMdd-NNNN` (Aturan 5), membuat kunci unik *idempotency* (UUID), menghitung diskon 5% hanya jika belanja $> \text{Rp}100.000$ (Aturan 3), serta mengunci transaksi agar tidak dapat diubah setelah lunas (Aturan 6).
- **`sinarmart.model.Pembayaran` (Abstrak)**:
  - **`PembayaranTunai`**: Memvalidasi nominal tunai diterima, menolak pembayaran kurang (Aturan 4), dan menghitung kembalian.
  - **`PembayaranNonTunai`**: Memvalidasi referensi pembayaran QRIS / Transfer Bank.
- **`sinarmart.model.Shift`**: Mengelola modal awal, rekapitulasi penjualan tunai & non-tunai, perhitungan uang seharusnya di laci, dan pencatatan selisih kas fisik. Menolak kasir membuka shift baru jika masih ada shift aktif (Aturan 9).
- **`sinarmart.model.Struk`**: Memformat bukti pembayaran teks persis sesuai stimulus soal, menyediakan pratinjau di layar dan penyimpanan berkas fisik `.txt`.
- **`sinarmart.model.AntreanKirim`**: Menyimpan transaksi lokal saat jaringan offline dan mengalirkan data ke server saat jaringan pulih.
- **`sinarmart.model.LayananServer`**: Simulasi layanan backend server dengan tombol pemutus jaringan dan verifikasi kunci unik (*Idempotency Deduplication*) untuk menyelesaikan masalah pencatatan ganda pada Bukti 1 & 2.
- **`sinarmart.service.KasirService`**: Pusat pengendali alur bisnis yang menghubungkan objek model dengan GUI.
- **`sinarmart.database.Koneksi`**: Manajemen koneksi MySQL dengan fallback offline yang aman.

---

## 3. SELURUH FORM MENGGUNAKAN JFRAME FORM (NETBEANS BUILDER COMPATIBLE)

Setiap halaman antarmuka dibuat menggunakan **JFrame Form** resmi NetBeans (`.java` berpasangan dengan `.form` XML):
1. **`FormLogin`** (`FormLogin.java` & `FormLogin.form`): Halaman masuk kasir, admin gudang, dan pemilik.
2. **`FormBukaShift`** (`FormBukaShift.java` & `FormBukaShift.form`): Input modal awal kas dan validasi shift tunggal.
3. **`FormTransaksiKasir`** (`FormTransaksiKasir.java` & `FormTransaksiKasir.form`): Dashboard utama kasir, pencarian barang, keranjang tabel, kalkulasi diskon otomatis, indikator & tombol simulasi putus/hubung jaringan online-offline.
4. **`FormPembayaran`** (`FormPembayaran.java` & `FormPembayaran.form`): Pemilihan Tunai/QRIS/Bank, kalkulasi kembalian, dan validasi uang kurang.
5. **`FormTutupShift`** (`FormTutupShift.java` & `FormTutupShift.form`): Rekapitulasi penjualan shift, input uang fisik, perhitungan selisih, dan penguncian shift.
6. **`FormRiwayatAntrean`** (`FormRiwayatAntrean.java` & `FormRiwayatAntrean.form`): Pemantauan status kirim antrean lokal dan tombol sinkronisasi manual ke server.
7. **`FormKelolaBarang`** (`FormKelolaBarang.java` & `FormKelolaBarang.form`): Manajemen produk untuk admin gudang / pemilik.
8. **`FormLaporanPemilik`** (`FormLaporanPemilik.java` & `FormLaporanPemilik.form`): Laporan omzet, estimasi laba kotor, dan peringatan stok menipis untuk pemilik.

---

## 4. PERINTAH SQL DATABASE (`database.sql`)
Untuk membuat database dan data awal pada MySQL / phpMyAdmin:

```sql
-- 1. Buat Database
CREATE DATABASE IF NOT EXISTS db_sinarmart;
USE db_sinarmart;

-- 2. Tabel Pengguna (Hak Akses)
CREATE TABLE IF NOT EXISTS users (
    id_user INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    nama VARCHAR(100) NOT NULL,
    role ENUM('kasir', 'admin_gudang', 'pemilik') NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Tabel Data Barang
CREATE TABLE IF NOT EXISTS barang (
    kode_barang VARCHAR(20) PRIMARY KEY,
    nama_barang VARCHAR(100) NOT NULL,
    kategori VARCHAR(50) NOT NULL,
    satuan VARCHAR(20) NOT NULL,
    harga_beli DOUBLE NOT NULL,
    harga_jual DOUBLE NOT NULL,
    stok INT NOT NULL DEFAULT 0,
    stok_min INT NOT NULL DEFAULT 5,
    aktif BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. Tabel Shift Kasir
CREATE TABLE IF NOT EXISTS shift (
    id_shift VARCHAR(30) PRIMARY KEY,
    kasir VARCHAR(100) NOT NULL,
    waktu_buka DATETIME NOT NULL,
    waktu_tutup DATETIME NULL,
    modal_awal DOUBLE NOT NULL DEFAULT 0,
    total_tunai DOUBLE NOT NULL DEFAULT 0,
    total_nontunai DOUBLE NOT NULL DEFAULT 0,
    total_transaksi INT NOT NULL DEFAULT 0,
    uang_fisik_laci DOUBLE NULL DEFAULT 0,
    selisih DOUBLE NULL DEFAULT 0,
    status ENUM('OPEN', 'CLOSED') NOT NULL DEFAULT 'OPEN'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. Tabel Transaksi Penjualan
CREATE TABLE IF NOT EXISTS transaksi (
    no_transaksi VARCHAR(30) PRIMARY KEY,
    unique_key VARCHAR(64) NOT NULL UNIQUE,
    id_shift VARCHAR(30) NOT NULL,
    kasir VARCHAR(100) NOT NULL,
    tanggal DATETIME NOT NULL,
    subtotal DOUBLE NOT NULL,
    diskon_persen DOUBLE NOT NULL DEFAULT 0,
    nominal_diskon DOUBLE NOT NULL DEFAULT 0,
    total_akhir DOUBLE NOT NULL,
    metode_pembayaran VARCHAR(20) NOT NULL,
    nominal_bayar DOUBLE NOT NULL,
    kembalian DOUBLE NOT NULL DEFAULT 0,
    status_kirim ENUM('PENDING', 'TERKIRIM') NOT NULL DEFAULT 'TERKIRIM',
    INDEX idx_unique_key (unique_key),
    INDEX idx_id_shift (id_shift)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. Tabel Detail Transaksi
CREATE TABLE IF NOT EXISTS detail_transaksi (
    id_detail INT AUTO_INCREMENT PRIMARY KEY,
    no_transaksi VARCHAR(30) NOT NULL,
    kode_barang VARCHAR(20) NOT NULL,
    nama_barang VARCHAR(100) NOT NULL,
    jumlah INT NOT NULL,
    harga_satuan DOUBLE NOT NULL,
    subtotal DOUBLE NOT NULL,
    FOREIGN KEY (no_transaksi) REFERENCES transaksi(no_transaksi) ON DELETE RESTRICT ON UPDATE CASCADE,
    FOREIGN KEY (kode_barang) REFERENCES barang(kode_barang) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. Data Awal (Seed Data)
INSERT INTO users (username, password, nama, role) VALUES
('rina', 'kasir123', 'Rina', 'kasir'),
('agus', 'admin123', 'Agus Gudang', 'admin_gudang'),
('budi', 'pemilik123', 'Budi Santoso', 'pemilik')
ON DUPLICATE KEY UPDATE nama=VALUES(nama);

INSERT INTO barang (kode_barang, nama_barang, kategori, satuan, harga_beli, harga_jual, stok, stok_min, aktif) VALUES
('BRG001', 'Beras 5 kg', 'Sembako', 'Sak', 60000, 68000, 20, 5, TRUE),
('BRG002', 'Minyak 1 L', 'Sembako', 'Pouch', 15000, 18000, 30, 8, TRUE),
('BRG003', 'Teh Botol', 'Minuman', 'Botol', 3800, 5000, 50, 12, TRUE),
('BRG004', 'Gula Pasir 1 kg', 'Sembako', 'Bungkus', 14000, 17000, 25, 5, TRUE),
('BRG005', 'Air Mineral 600ml', 'Minuman', 'Botol', 2500, 3500, 4, 10, TRUE),
('BRG006', 'Mie Instan Goreng', 'Makanan Ringan', 'Bungkus', 2800, 3500, 80, 15, TRUE),
('BRG007', 'Sabun Mandi 85g', 'Perlengkapan Mandi', 'Buah', 4000, 5500, 15, 5, TRUE),
('BRG008', 'Shampoo 170ml', 'Perlengkapan Mandi', 'Botol', 19000, 24000, 12, 4, TRUE),
('BRG009', 'Kopi Bubuk 150g', 'Minuman', 'Bungkus', 9500, 12500, 22, 6, TRUE),
('BRG010', 'Pasta Gigi 120g', 'Perlengkapan Mandi', 'Tube', 11000, 14500, 18, 5, TRUE)
ON DUPLICATE KEY UPDATE nama_barang=VALUES(nama_barang), harga_jual=VALUES(harga_jual), stok=VALUES(stok);
```

---

## 5. AKUN PENGGUNA DEFAULT
| Username | Password | Nama Lengkap | Peran (*Role*) | Hak Akses |
|---|---|---|---|---|
| `rina` | `kasir123` | Rina | `kasir` | Buka Shift, Transaksi POS, Cetak Struk, Antrean Offline, Tutup Shift |
| `agus` | `admin123` | Agus Gudang | `admin_gudang` | Kelola Data Barang, Stok Masuk, Batas Minimum, Ubah Status |
| `budi` | `pemilik123` | Budi Santoso | `pemilik` | Semua Menu, Laporan Penjualan, Laba Kotor, Rekap Tutup Shift |

---

## 6. HASIL PENGUJIAN OTOMATIS (UNIT TEST)
Pengujian dijalankan dengan JUnit 4 mencakup 8 skenario pengujian komprehensif:

| No | Nama Unit Test | Kriteria Pengujian | Hasil |
|---|---|---|:---:|
| 1 | `testDiskon5PersenBelanjaDiatas100Ribu` | Total belanja > Rp100.000 otomatis mendapatkan diskon 5% (Subtotal Rp119.000 $\rightarrow$ Diskon Rp5.950 $\rightarrow$ Total Rp113.050) | **LULUS** |
| 2 | `testTanpaDiskonJikaTepatAtauKurang100Ribu` | Total belanja tepat Rp100.000 atau kurang TIDAK mendapatkan diskon (Diskon = 0%) | **LULUS** |
| 3 | `testValidasiStokKurangDitolak` | Transaksi ditolak dengan melempar `IllegalArgumentException` jika kuantitas diminta > stok tersedia | **LULUS** |
| 4 | `testValidasiPembayaranTunaiKurangDitolak` | Pembayaran tunai ditolak jika nominal uang diterima kurang dari total tagihan | **LULUS** |
| 5 | `testImmutabilityTransaksiLunas` | Transaksi lunas dikunci (`isLunas = true`), penambahan atau penghapusan item ditolak | **LULUS** |
| 6 | `testIdempotencyKirimUlangAmanTanpaDuplikasi` | Kirim ulang transaksi yang sama saat jaringan pulih diabaikan oleh server via kunci unik (Bukti 1-2 terpecahkan) | **LULUS** |
| 7 | `testPembukaanShiftKeduaDitolak` | Kasir ditolak membuka shift baru jika masih memiliki shift berstatus OPEN | **LULUS** |
| 8 | `testFormatStrukSesuaiStimulus` | Format fisik teks struk dan seluruh angka identik dengan stimulus lembar soal | **LULUS** |

---

## 7. CARA MENJALANKAN APLIKASI
### Melalui NetBeans IDE:
1. Buka aplikasi NetBeans IDE.
2. Pilih menu **File -> Open Project**, lalu arahkan ke folder `/home/ikyys/NetBeansProjects/SinarMart`.
3. Buka form di `sinarmart.gui` (misal `FormTransaksiKasir.java`), klik tab **Design** untuk melihat tampilan grafis visual Matisse.
4. Klik kanan project **SinarMart** -> **Run** (atau tekan **F6**).

### Melalui Terminal:
```bash
cd /home/ikyys/NetBeansProjects/SinarMart
java -jar dist/SinarMart.jar
```
