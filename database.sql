-- ====================================================================
-- SKRIP DATABASE: Toko Sinar Mart (POS System)
-- SMK Bina Informatika - SAS Pemrograman Berbasis Teks dan Grafis
-- ====================================================================

CREATE DATABASE IF NOT EXISTS db_sinarmart;
USE db_sinarmart;

-- 1. Tabel Pengguna (Hak Akses: kasir, admin_gudang, pemilik)
CREATE TABLE IF NOT EXISTS users (
    id_user INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    nama VARCHAR(100) NOT NULL,
    role ENUM('kasir', 'admin_gudang', 'pemilik') NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Tabel Data Barang
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

-- 3. Tabel Shift Kasir
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

-- 4. Tabel Transaksi Penjualan
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

-- 5. Tabel Detail Transaksi
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

-- ====================================================================
-- DATA AWAL (SEED DATA)
-- ====================================================================

-- Data User Awal
INSERT INTO users (username, password, nama, role) VALUES
('rina', 'kasir123', 'Rina', 'kasir'),
('agus', 'admin123', 'Agus Gudang', 'admin_gudang'),
('budi', 'pemilik123', 'Budi Santoso', 'pemilik')
ON DUPLICATE KEY UPDATE nama=VALUES(nama);

-- Data Barang Sesuai Studi Kasus Sinar Mart
INSERT INTO barang (kode_barang, nama_barang, kategori, satuan, harga_beli, harga_jual, stok, stok_min, aktif) VALUES
('BRG001', 'Beras 5 kg', 'Sembako', 'Sak', 60000, 68000, 20, 5, TRUE),
('BRG002', 'Minyak 1 L', 'Sembako', 'Pouch', 15000, 18000, 30, 8, TRUE),
('BRG003', 'Teh Botol', 'Minuman', 'Botol', 3800, 5000, 50, 12, TRUE),
('BRG004', 'Gula Pasir 1 kg', 'Sembako', 'Bungkus', 14000, 17000, 25, 5, TRUE),
('BRG005', 'Air Mineral 600ml', 'Minuman', 'Botol', 2500, 3500, 4, 10, TRUE), -- Stok di bawah minimum (Peringatan)
('BRG006', 'Mie Instan Goreng', 'Makanan Ringan', 'Bungkus', 2800, 3500, 80, 15, TRUE),
('BRG007', 'Sabun Mandi 85g', 'Perlengkapan Mandi', 'Buah', 4000, 5500, 15, 5, TRUE),
('BRG008', 'Shampoo 170ml', 'Perlengkapan Mandi', 'Botol', 19000, 24000, 12, 4, TRUE),
('BRG009', 'Kopi Bubuk 150g', 'Minuman', 'Bungkus', 9500, 12500, 22, 6, TRUE),
('BRG010', 'Pasta Gigi 120g', 'Perlengkapan Mandi', 'Tube', 11000, 14500, 18, 5, TRUE)
ON DUPLICATE KEY UPDATE nama_barang=VALUES(nama_barang), harga_jual=VALUES(harga_jual), stok=VALUES(stok);
