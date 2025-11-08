/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package andynails;

import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 *
 * @author mgmmo
 */
public class MainFrame extends JFrame {
    public MainFrame(String rol) {
        
        setTitle("Sistema Andynails");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        JPanel panel = null;

        switch (rol) {
            case "Admin":
                panel = new VistaAdministrador();
                break;
            case "Recepcionista":
                panel = new VistaRecepcionista();
                break;
            case "Cliente":
                panel = new VistaCliente();
                break;
        }

        if (panel != null) {
            setContentPane(panel);
        }
    }
}
