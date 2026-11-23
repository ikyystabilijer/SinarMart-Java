package sinarmart.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Pengelola Koneksi Database MySQL Sinar Mart
 */
public class Koneksi {
    private static final String URL = "jdbc:mysql://localhost:3306/db_sinarmart?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Jakarta";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private static Connection connection = null;

    public static synchronized Connection getKoneksi() {
        try {
            if (connection == null || connection.isClosed()) {
                // Register driver
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
            }
            return connection;
        } catch (ClassNotFoundException e) {
            System.err.println("[KONEKSI] Driver MySQL tidak ditemukan: " + e.getMessage());
            return null;
        } catch (SQLException e) {
            System.err.println("[KONEKSI] Tidak dapat terhubung ke MySQL: " + e.getMessage());
            return null;
        }
    }

    public static boolean isConnected() {
        try {
            Connection conn = getKoneksi();
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}
