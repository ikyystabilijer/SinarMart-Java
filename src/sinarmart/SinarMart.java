package sinarmart;

import sinarmart.gui.FormLogin;
import javax.swing.UIManager;

/**
 * Main Class Aplikasi Toko Sinar Mart
 */
public class SinarMart {

    public static void main(String[] args) {
        // Atur Look and Feel modern
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignore) {}
        }

        // Jalankan Form Login
        java.awt.EventQueue.invokeLater(() -> {
            new FormLogin().setVisible(true);
        });
    }
}
