package Interfaces;

import java.awt.*;
import java.awt.event.*;
import andynails.ConexionBD;
import java.awt.Color;
import java.awt.Image;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.Timer;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
/**
 *
 * @author User
 */
public class NewJCatalogoPeinado extends javax.swing.JFrame {

    ConexionBD conexion;
    private List<CategoriaServicio> categorias = new ArrayList<>();
    private ArrayList<CategoriaServicio> bodaList = new ArrayList<>();
    private ArrayList<CategoriaServicio> xvList = new ArrayList<>();
    private ArrayList<CategoriaServicio> socialList = new ArrayList<>();
    private int indice = 0;

    public static class CategoriaServicio {

        public ImageIcon imagen;
        public String nombre;
        public String descripcion;
        public String precio;
    }

    private boolean puedeSeleccionar() {
        if (!andynails.SesionUsuario.sesionActiva()) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Acceso restringido\n\n"
                    + "Para seleccionar servicios debes iniciar sesión.\n"
                    + "Ve al menú 'LOGIN' para registrarte o iniciar sesión.\n\n"
                    + "¡Te esperamos! 💖",
                    "Inicia sesión para continuar",
                    javax.swing.JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    public void deshabilitarSeleccion() {
        // Deshabilitar botones específicos de UÑAS
        if (jButton7 != null) {
            jButton7.setEnabled(false);
        }
        if (jButton3 != null) {
            jButton3.setEnabled(false);
        }
        if (jButton5 != null) {
            jButton5.setEnabled(false);
        }
    }

    public NewJCatalogoPeinado() {
        initComponents();
        setLocationRelativeTo(null);
        cargarCategoriasDesdeBD();
        iniciarCarrusel();
        iniciarCarrusel();
    }

    // Para cerrar sesión en cualquier interfaz
    private void jMenuItemCerrarSesionActionPerformed(java.awt.event.ActionEvent evt) {
        andynails.SessionManager.cerrarSesion(this);
    }

// Para obtener datos del usuario
    private void mostrarInfoUsuario() {
        String usuario = andynails.SessionManager.getUsuarioLogueado();
        String tipo = andynails.SessionManager.getTipoUsuario();
        int id = andynails.SessionManager.getIdUsuario();

        System.out.println("Usuario: " + usuario + ", Tipo: " + tipo + ", ID: " + id);
    }

    private void cargarCategoriasDesdeBD() {
        try (Connection con = ConexionBD.getConnection(); PreparedStatement ps = con.prepareStatement(
                "SELECT Imagen_Archivo, Nombre_categoria, Descripcion, Precio, idCategoria_Servicio "
                + "FROM categoria_servicio WHERE idServicios = 3")) {

            ResultSet rs = ps.executeQuery();
            categorias.clear();
            bodaList.clear();
            xvList.clear();
            socialList.clear();

            while (rs.next()) {
                String rutaImagen = rs.getString("Imagen_Archivo");
                String nombre = rs.getString("Nombre_categoria");
                String descripcion = rs.getString("Descripcion");
                String precio = rs.getString("Precio");

                ImageIcon icon = null;
                if (rutaImagen != null && !rutaImagen.isEmpty()) {
                    File archivo = new File(rutaImagen);
                    if (archivo.exists()) {
                        Image img = new ImageIcon(rutaImagen)
                                .getImage()
                                .getScaledInstance(208, 214, Image.SCALE_SMOOTH);
                        icon = new ImageIcon(img);
                    }
                }

                NewJCatalogoPeinado.CategoriaServicio c = new NewJCatalogoPeinado.CategoriaServicio();
                c.imagen = icon;
                c.descripcion = descripcion;
                c.precio = precio;
                c.nombre = nombre;

                // 🔹 Clasificación automática
                switch (nombre.toLowerCase().trim()) {
                    case "boda":
                    case "maquillaje boda":
                    case "maquillaje para boda":
                        bodaList.add(c);
                        break;
                    case "social":
                    case "maquillaje social":
                    case "maquillaje para evento":
                        socialList.add(c);
                        break;
                    case "xv":
                    case "maquillaje xv":
                    case "maquillaje para xv":
                        xvList.add(c);
                        break;

                }
            }
            // Mostrar la primera imagen de cada categoría
            if (!bodaList.isEmpty()) {
                mostrarCategoria(bodaList.get(0), lblImg1, lblDesc1, txtPrecio1);
            }
            if (!socialList.isEmpty()) {
                mostrarCategoria(socialList.get(0), lblImg2, lblDesc2, txtPrecio2);
            }
            if (!xvList.isEmpty()) {
                mostrarCategoria(xvList.get(0), lblImg3, lblDesc3, txtPrecio3);
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al cargar categorías: " + e.getMessage());
        }
    }

    private void mostrarCategoria(NewJCatalogoPeinado.CategoriaServicio c, JLabel lblImg, JLabel lblDesc, JLabel lblPrecio) {
        if (c.imagen != null) {
            lblImg.setIcon(c.imagen);
        }
        lblDesc.setText(c.descripcion);
        lblPrecio.setText("$" + c.precio);
    }
    private int indiceBoda = 0;
    private int indiceXV = 0;
    private int indiceSocial = 0;

    private void iniciarCarrusel() {
        // Carrusel para maquillaje de boda
        Timer timerBoda = new Timer(4000, e -> {
            if (!bodaList.isEmpty()) {
                indiceBoda = (indiceBoda + 1) % bodaList.size();
                mostrarCategoria(bodaList.get(indiceBoda), lblImg1, lblDesc1, txtPrecio1);
            }
        });
        timerBoda.start();

        // Carrusel para maquillaje de XV
        Timer timerXV = new Timer(4000, e -> {
            if (!xvList.isEmpty()) {
                indiceXV = (indiceXV + 1) % xvList.size();
                mostrarCategoria(xvList.get(indiceXV), lblImg3, lblDesc3, txtPrecio3);
            }
        });
        timerXV.start();

        // Carrusel para maquillaje social
        Timer timerSocial = new Timer(4000, e -> {
            if (!socialList.isEmpty()) {
                indiceSocial = (indiceSocial + 1) % socialList.size();
                mostrarCategoria(socialList.get(indiceSocial), lblImg2, lblDesc2, txtPrecio2);
            }
        });
        timerSocial.start();
    }

    private void abrirVentanaCita(JLabel lblDesc, JLabel lblPrecio, JLabel lblImg) {
        String descripcion = lblDesc.getText();
        String precio = lblPrecio.getText();
        ImageIcon imagen = (ImageIcon) lblImg.getIcon();

        NewJAgenC ventanaCita = new NewJAgenC(imagen, descripcion, precio);
        ventanaCita.setVisible(true);
        this.dispose();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")


    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        labelCategoria2 = new java.awt.Label();
        labelCategoria3 = new java.awt.Label();
        labelCategoria1 = new java.awt.Label();
        jButton3 = new javax.swing.JButton();
        jLabel11 = new javax.swing.JLabel();
        lblDesc1 = new javax.swing.JLabel();
        label7 = new java.awt.Label();
        txtPrecio1 = new javax.swing.JLabel();
        lblImg2 = new javax.swing.JLabel();
        lblImg1 = new javax.swing.JLabel();
        label10 = new java.awt.Label();
        lblDesc2 = new javax.swing.JLabel();
        txtPrecio2 = new javax.swing.JLabel();
        lblImg3 = new javax.swing.JLabel();
        lblDesc3 = new javax.swing.JLabel();
        label11 = new java.awt.Label();
        txtPrecio3 = new javax.swing.JLabel();
        jButton5 = new javax.swing.JButton();
        jButton7 = new javax.swing.JButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenu5 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();
        jMenuItem7 = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        jMenuItem4 = new javax.swing.JMenuItem();
        jMenu4 = new javax.swing.JMenu();
        jMenuItem5 = new javax.swing.JMenuItem();
        jMenu6 = new javax.swing.JMenu();
        jMenuItem6 = new javax.swing.JMenuItem();
        jMenu16 = new javax.swing.JMenu();
        jMenuItemCerrarSecion = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        jPanel2.setBackground(new java.awt.Color(243, 224, 255));

        jPanel3.setBackground(new java.awt.Color(204, 0, 204));

        jLabel3.setText("INS");

        jLabel6.setText("FACE");

        jLabel7.setText("WPP");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(166, 166, 166)
                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(136, 136, 136)
                .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(175, 175, 175)
                .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jLabel7)
                    .addComponent(jLabel6))
                .addContainerGap(14, Short.MAX_VALUE))
        );

        jPanel4.setBackground(new java.awt.Color(237, 204, 255));

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 162, Short.MAX_VALUE)
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        labelCategoria2.setFont(new java.awt.Font("Arial", 3, 12)); // NOI18N
        labelCategoria2.setText("Social");

        labelCategoria3.setFont(new java.awt.Font("Arial", 3, 12)); // NOI18N
        labelCategoria3.setText("XV años");

        labelCategoria1.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        labelCategoria1.setFont(new java.awt.Font("Arial", 3, 12)); // NOI18N
        labelCategoria1.setText("Boda");

        jButton3.setBackground(new java.awt.Color(255, 204, 255));
        jButton3.setText("Seleccionar");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jLabel11.setFont(new java.awt.Font("Serif", 3, 18)); // NOI18N
        jLabel11.setText("PEINADOS");

        lblDesc1.setText("Peinados para novia");

        label7.setBackground(new java.awt.Color(252, 237, 237));
        label7.setText("Precio");

        txtPrecio1.setText("$500");
        txtPrecio1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        lblImg2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        lblImg1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        label10.setBackground(new java.awt.Color(252, 237, 237));
        label10.setText("Precio");

        lblDesc2.setText("Peinados ideal para eventos ");

        txtPrecio2.setText("$500");
        txtPrecio2.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        lblImg3.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        lblDesc3.setText("Peiandos ideales para XV ");

        label11.setBackground(new java.awt.Color(252, 237, 237));
        label11.setText("Precio");

        txtPrecio3.setText("$500");
        txtPrecio3.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        jButton5.setBackground(new java.awt.Color(255, 204, 255));
        jButton5.setText("Seleccionar");
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        jButton7.setBackground(new java.awt.Color(255, 204, 255));
        jButton7.setText("Seleccionar");
        jButton7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton7ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(216, 216, 216)
                                .addComponent(labelCategoria1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(183, 183, 183)
                                .addComponent(jButton7)))
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(54, 54, 54)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addGap(158, 158, 158)
                                        .addComponent(label10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 106, Short.MAX_VALUE)
                                        .addComponent(txtPrecio2, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(347, 347, 347))
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addGap(228, 228, 228)
                                        .addComponent(labelCategoria2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(labelCategoria3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(69, 69, 69))))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(264, 264, 264)
                                .addComponent(jButton3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jButton5)
                                .addGap(32, 32, 32))))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(874, 874, 874)
                        .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(129, 129, 129)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addComponent(label7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(txtPrecio1, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(lblImg1, javax.swing.GroupLayout.PREFERRED_SIZE, 208, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(lblImg2, javax.swing.GroupLayout.PREFERRED_SIZE, 208, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(83, 83, 83))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(141, 141, 141)
                                .addComponent(lblDesc1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(lblDesc2)
                                .addGap(116, 116, 116)))
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(56, 56, 56)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addComponent(label11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(txtPrecio3, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(lblImg3, javax.swing.GroupLayout.PREFERRED_SIZE, 208, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                .addComponent(lblDesc3)
                                .addGap(40, 40, 40)))))
                .addContainerGap(129, Short.MAX_VALUE))
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(540, 540, 540)
                .addComponent(jLabel11)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(66, 66, 66)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(labelCategoria1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(26, 26, 26)
                        .addComponent(lblImg1, javax.swing.GroupLayout.PREFERRED_SIZE, 214, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(labelCategoria3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(labelCategoria2, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(26, 26, 26)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblImg3, javax.swing.GroupLayout.PREFERRED_SIZE, 214, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(lblImg2, javax.swing.GroupLayout.PREFERRED_SIZE, 214, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(txtPrecio1)
                                .addComponent(label10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                .addComponent(label7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(2, 2, 2)))
                        .addGap(39, 39, 39))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(27, 27, 27)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(lblDesc1)
                            .addComponent(lblDesc2)
                            .addComponent(lblDesc3))
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(45, 45, 45)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(txtPrecio3)
                                    .addComponent(label11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(0, 48, Short.MAX_VALUE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(txtPrecio2)
                                .addGap(31, 31, 31)))))
                .addGap(12, 12, 12)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton5)
                    .addComponent(jButton3)
                    .addComponent(jButton7))
                .addGap(67, 67, 67)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addComponent(jLabel11)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        jMenuBar1.setBackground(new java.awt.Color(255, 153, 255));

        jMenu1.setText("INICIO");
        jMenu1.addMenuListener(new javax.swing.event.MenuListener() {
            public void menuCanceled(javax.swing.event.MenuEvent evt) {
            }
            public void menuDeselected(javax.swing.event.MenuEvent evt) {
            }
            public void menuSelected(javax.swing.event.MenuEvent evt) {
                jMenu1MenuSelected(evt);
            }
        });
        jMenuBar1.add(jMenu1);

        jMenu5.setText("CATALÓGO");

        jMenuItem1.setText("Uñas");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu5.add(jMenuItem1);

        jMenuItem2.setText("Peinados");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu5.add(jMenuItem2);

        jMenuItem3.setText("Maquillaje");
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem3ActionPerformed(evt);
            }
        });
        jMenu5.add(jMenuItem3);

        jMenuItem7.setText("Otros");
        jMenuItem7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem7ActionPerformed(evt);
            }
        });
        jMenu5.add(jMenuItem7);

        jMenuBar1.add(jMenu5);

        jMenu3.setText("MIS CITAS");

        jMenuItem4.setText("Mis citas");
        jMenuItem4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem4ActionPerformed(evt);
            }
        });
        jMenu3.add(jMenuItem4);

        jMenuBar1.add(jMenu3);

        jMenu4.setText("CONTATCO");

        jMenuItem5.setText("Contacto");
        jMenuItem5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem5ActionPerformed(evt);
            }
        });
        jMenu4.add(jMenuItem5);

        jMenuBar1.add(jMenu4);

        jMenu6.setText("LOGIN");

        jMenuItem6.setText("Login");
        jMenuItem6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem6ActionPerformed(evt);
            }
        });
        jMenu6.add(jMenuItem6);

        jMenuBar1.add(jMenu6);

        jMenu16.setText("CERRAR SECION");

        jMenuItemCerrarSecion.setText("cerrar secion");
        jMenuItemCerrarSecion.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemCerrarSecionActionPerformed(evt);
            }
        });
        jMenu16.add(jMenuItemCerrarSecion);

        jMenuBar1.add(jMenu16);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 6, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jMenu1MenuSelected(javax.swing.event.MenuEvent evt) {//GEN-FIRST:event_jMenu1MenuSelected
        // TODO add your handling code here:
        //inicio
        Inicio Inicio = new Inicio();
        Inicio.setVisible(true);
        this.dispose(); // cierra la actual

    }//GEN-LAST:event_jMenu1MenuSelected

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        // TODO add your handling code here:

    }//GEN-LAST:event_formWindowOpened

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        // TODO add your handling code here:
        abrirVentanaCita(lblDesc3, txtPrecio3, lblImg3);
        this.dispose();
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        // TODO add your handling code here:
        abrirVentanaCita(lblDesc2, txtPrecio2, lblImg2);
        this.dispose();
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton7ActionPerformed
        // TODO add your handling code here:
        abrirVentanaCita(lblDesc1, txtPrecio1, lblImg1);
        this.dispose();
    }//GEN-LAST:event_jButton7ActionPerformed

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        // TODO add your handling code here:
        NewJCatalogoUñas NewJCatalogoUñas = new NewJCatalogoUñas();
        NewJCatalogoUñas.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        // TODO add your handling code here:
        NewJCatalogoPeinado NewJCatalogoPeinado = new NewJCatalogoPeinado();
        NewJCatalogoPeinado.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem3ActionPerformed
        // TODO add your handling code here:
        NewJCatalogoMaq NewJCatalogoMaq = new NewJCatalogoMaq();
        NewJCatalogoMaq.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem3ActionPerformed

    private void jMenuItem7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem7ActionPerformed
        // TODO add your handling code here:
        ConexionBD conexionCatalogo = new ConexionBD("andynails");
        NewJCatalogoGenerico catalogo = new NewJCatalogoGenerico(conexionCatalogo);
        catalogo.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem7ActionPerformed

    private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem4ActionPerformed
        // TODO add your handling code here:
        //agendar cancelar
        NewJMiscitasCi cliWindow = new NewJMiscitasCi();
        cliWindow.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem4ActionPerformed

    private void jMenuItem5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem5ActionPerformed
        // TODO add your handling code here:
        //boton de contacto
        NewJContacto NewJContacto = new NewJContacto();
        NewJContacto.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem5ActionPerformed

    private void jMenuItem6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem6ActionPerformed
        // TODO add your handling code here:
        //login
        NewJLogin NewJLogin = new NewJLogin();
        NewJLogin.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem6ActionPerformed

    private void jMenuItemCerrarSecionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemCerrarSecionActionPerformed
        // TODO add your handling code here:
        andynails.SessionManager.cerrarSesion(this);
    }//GEN-LAST:event_jMenuItemCerrarSecionActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(NewJCatalogoPeinado.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(NewJCatalogoPeinado.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(NewJCatalogoPeinado.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(NewJCatalogoPeinado.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new NewJCatalogoPeinado().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton7;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu16;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenu jMenu4;
    private javax.swing.JMenu jMenu5;
    private javax.swing.JMenu jMenu6;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem jMenuItem5;
    private javax.swing.JMenuItem jMenuItem6;
    private javax.swing.JMenuItem jMenuItem7;
    private javax.swing.JMenuItem jMenuItemCerrarSecion;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private java.awt.Label label10;
    private java.awt.Label label11;
    private java.awt.Label label7;
    private java.awt.Label labelCategoria1;
    private java.awt.Label labelCategoria2;
    private java.awt.Label labelCategoria3;
    private javax.swing.JLabel lblDesc1;
    private javax.swing.JLabel lblDesc2;
    private javax.swing.JLabel lblDesc3;
    private javax.swing.JLabel lblImg1;
    private javax.swing.JLabel lblImg2;
    private javax.swing.JLabel lblImg3;
    private javax.swing.JLabel txtPrecio1;
    private javax.swing.JLabel txtPrecio2;
    private javax.swing.JLabel txtPrecio3;
    // End of variables declaration//GEN-END:variables
}
