package Interfaces;

import javax.swing.ImageIcon;
import java.awt.Image;
import andynails.ConexionBD;
import andynails.RedesSociales;
import java.awt.BorderLayout;
import java.awt.Font;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.Timer;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
/**
 *
 * @author User
 */
public class NewJCatalogoPeinado0 extends javax.swing.JFrame {

    ConexionBD conexion;
    private List<CategoriaServicio> categorias = new ArrayList<>();
    private JLabel lblCarrusel;
    private int indice = 0;
    private Timer timer;

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

        this.setTitle("Catálogo de peinados - Modo Visualización");
        cambiarAparienciaBotonesNoLogueados();
        System.out.println("Botones de peinados deshabilitados");
    }

    private void cambiarAparienciaBotonesNoLogueados() {
        javax.swing.JButton[] botones = {jButton3, jButton5, jButton7};
        for (javax.swing.JButton boton : botones) {
            if (boton != null) {
                boton.setBackground(java.awt.Color.LIGHT_GRAY);
                boton.setForeground(java.awt.Color.DARK_GRAY);
                boton.setText(boton.getText() + " bloqueados");
            }
        }
    }

    public NewJCatalogoPeinado0() {
        initComponents();
                RedesSociales.configurarRedesSociales(INS, WPP, FACE);


        // Inicializar lblCarrusel dentro de jPanel4
        lblCarrusel = new JLabel();
        lblCarrusel.setHorizontalAlignment(SwingConstants.CENTER);
        lblImg1.setLayout(new BorderLayout());
        jPanel4.add(lblCarrusel, BorderLayout.CENTER);

        // Configuración de fuentes
        lblDesc1.setFont(new Font("Arial", Font.BOLD, 14));
        lblDesc2.setFont(new Font("Arial", Font.BOLD, 14));
        lblDesc3.setFont(new Font("Arial", Font.BOLD, 14));

        txtPrecio1.setFont(new Font("Arial", Font.PLAIN, 12));
        txtPrecio2.setFont(new Font("Arial", Font.PLAIN, 12));
        txtPrecio3.setFont(new Font("Arial", Font.PLAIN, 12));

        setLocationRelativeTo(null);
        cargarCategoriasDesdeBD();
        iniciarCarrusel();
    }

    private void cargarCategoriasDesdeBD() {
        try (Connection con = ConexionBD.getConnection(); PreparedStatement ps = con.prepareStatement(
                "SELECT Imagen_Archivo, Nombre_categoria, Descripcion, Precio, idCategoria_Servicio "
                + "FROM categoria_servicio WHERE idServicios = 3")) {

            ResultSet rs = ps.executeQuery();
            categorias.clear(); // Limpiar lista antes de cargar

            while (rs.next()) {
                String rutaImagen = rs.getString("Imagen_Archivo");
                ImageIcon icon = null;
                if (rutaImagen != null && !rutaImagen.isEmpty()) {
                    java.io.File archivo = new java.io.File(rutaImagen);
                    if (archivo.exists()) {
                        Image img = new ImageIcon(rutaImagen).getImage().getScaledInstance(208, 214, Image.SCALE_SMOOTH);
                        icon = new ImageIcon(img);
                    }
                }

                CategoriaServicio c = new CategoriaServicio();
                c.imagen = icon;
                c.nombre = rs.getString("Nombre_categoria");
                c.descripcion = rs.getString("Descripcion");
                c.precio = rs.getString("Precio");
                categorias.add(c);
            }

            // Mostrar hasta tres categorías en los labels
            if (categorias.size() > 0) {
                lblImg1.setIcon(categorias.get(0).imagen);
            }
            if (categorias.size() > 1) {
                lblImg2.setIcon(categorias.get(1).imagen);
            }
            if (categorias.size() > 2) {
                lblImg3.setIcon(categorias.get(2).imagen);
            }

            if (!categorias.isEmpty()) {
                lblDesc1.setText(categorias.get(0).descripcion);
                txtPrecio1.setText("$" + categorias.get(0).precio);
            }
            if (categorias.size() > 1) {
                lblDesc2.setText(categorias.get(1).descripcion);
                txtPrecio2.setText("$" + categorias.get(1).precio);
            }
            if (categorias.size() > 2) {
                lblDesc3.setText(categorias.get(2).descripcion);
                txtPrecio3.setText("$" + categorias.get(2).precio);
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al cargar categorías: " + e.getMessage());
        }
    }

    private void mostrarCategoria(int i) {
        if (categorias.isEmpty() || lblCarrusel == null) {
            return;
        }

        CategoriaServicio c = categorias.get(i);
        lblCarrusel.setIcon(c.imagen);
        lblDesc1.setText("<html>" + c.descripcion + "</html>");
        txtPrecio1.setText("$" + c.precio);
    }

    private void iniciarCarrusel() {
        timer = new Timer(4000, e -> {
            if (!categorias.isEmpty()) {
                indice = (indice + 1) % categorias.size();
                mostrarCategoria(indice);
            }
        });
        timer.start();
    }

    // Métodos de navegación del carrusel
    private void siguiente() {
        if (!categorias.isEmpty()) {
            indice = (indice + 1) % categorias.size();
            mostrarCategoria(indice);
        }
    }

    private void anterior() {
        if (!categorias.isEmpty()) {
            indice = (indice - 1 + categorias.size()) % categorias.size();
            mostrarCategoria(indice);
        }
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
        INS = new javax.swing.JLabel();
        FACE = new javax.swing.JLabel();
        WPP = new javax.swing.JLabel();
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
        jButtonAnterior = new javax.swing.JButton();
        jButton7 = new javax.swing.JButton();
        jButtonSiguiente = new javax.swing.JButton();
        jButtonAnterior1 = new javax.swing.JButton();
        jButtonSiguiente1 = new javax.swing.JButton();
        jButtonSiguiente2 = new javax.swing.JButton();
        jButtonAnterior2 = new javax.swing.JButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenu2 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        jMenuItem4 = new javax.swing.JMenuItem();
        jMenu4 = new javax.swing.JMenu();
        jMenuItem5 = new javax.swing.JMenuItem();
        jMenu6 = new javax.swing.JMenu();
        jMenuItem6 = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        jPanel2.setBackground(new java.awt.Color(243, 224, 255));

        jPanel3.setBackground(new java.awt.Color(204, 0, 204));

        INS.setText("INS");

        FACE.setText("FACE");

        WPP.setText("WPP");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(166, 166, 166)
                .addComponent(INS, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(136, 136, 136)
                .addComponent(WPP, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(175, 175, 175)
                .addComponent(FACE, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(INS)
                    .addComponent(WPP)
                    .addComponent(FACE))
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
        jButton3.setText("Seleccioanr");
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
        jButton5.setText("Seleccioanar");
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        jButtonAnterior.setBackground(new java.awt.Color(255, 204, 255));
        jButtonAnterior.setText("Anterior");
        jButtonAnterior.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAnteriorActionPerformed(evt);
            }
        });

        jButton7.setBackground(new java.awt.Color(255, 204, 255));
        jButton7.setText("Seleccioanar");
        jButton7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton7ActionPerformed(evt);
            }
        });

        jButtonSiguiente.setBackground(new java.awt.Color(255, 204, 255));
        jButtonSiguiente.setText("Siguiente");
        jButtonSiguiente.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSiguienteActionPerformed(evt);
            }
        });

        jButtonAnterior1.setBackground(new java.awt.Color(255, 204, 255));
        jButtonAnterior1.setText("Anterior");
        jButtonAnterior1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAnterior1ActionPerformed(evt);
            }
        });

        jButtonSiguiente1.setBackground(new java.awt.Color(255, 204, 255));
        jButtonSiguiente1.setText("Siguiente");
        jButtonSiguiente1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSiguiente1ActionPerformed(evt);
            }
        });

        jButtonSiguiente2.setBackground(new java.awt.Color(255, 204, 255));
        jButtonSiguiente2.setText("Siguiente");
        jButtonSiguiente2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSiguiente2ActionPerformed(evt);
            }
        });

        jButtonAnterior2.setBackground(new java.awt.Color(255, 204, 255));
        jButtonAnterior2.setText("Anterior");
        jButtonAnterior2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAnterior2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(86, 86, 86)
                .addComponent(jButton7)
                .addGap(329, 329, 329)
                .addComponent(jButton3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jButton5)
                .addGap(286, 286, 286))
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(lblDesc1)
                                .addGap(127, 127, 127))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addGap(139, 139, 139)
                                        .addComponent(labelCategoria1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addGap(68, 68, 68)
                                        .addComponent(lblImg1, javax.swing.GroupLayout.PREFERRED_SIZE, 208, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addGap(39, 39, 39)
                                        .addComponent(label7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(80, 80, 80)
                                        .addComponent(txtPrecio1, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addGap(25, 25, 25)
                                        .addComponent(jButtonAnterior)
                                        .addGap(106, 106, 106)
                                        .addComponent(jButtonSiguiente)))
                                .addGap(50, 50, 50)))
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(138, 138, 138)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(jLabel11)
                                    .addComponent(labelCategoria2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(labelCategoria3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(184, 184, 184))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(162, 162, 162)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lblDesc2)
                                    .addComponent(lblImg2, javax.swing.GroupLayout.PREFERRED_SIZE, 208, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(79, 79, 79)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                        .addComponent(lblDesc3)
                                        .addGap(15, 15, 15))
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addGap(147, 147, 147)
                                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(label11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(lblImg3, javax.swing.GroupLayout.PREFERRED_SIZE, 208, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addGap(133, 133, 133)
                                        .addComponent(label10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addGap(110, 110, 110)
                                        .addComponent(jButtonAnterior1)))
                                .addGap(99, 99, 99)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(txtPrecio2, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jButtonSiguiente1))
                                .addGap(96, 218, Short.MAX_VALUE)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                        .addComponent(jButtonAnterior2)
                                        .addGap(97, 97, 97)
                                        .addComponent(jButtonSiguiente2))
                                    .addComponent(txtPrecio3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 59, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(874, 874, 874)
                        .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(224, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addGap(36, 36, 36)
                .addComponent(jLabel11)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(57, 57, 57)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(labelCategoria2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(labelCategoria3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(labelCategoria1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(19, 19, 19)
                                .addComponent(lblImg1, javax.swing.GroupLayout.PREFERRED_SIZE, 214, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(10, 10, 10)
                                .addComponent(lblImg3, javax.swing.GroupLayout.PREFERRED_SIZE, 214, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addComponent(lblImg2, javax.swing.GroupLayout.PREFERRED_SIZE, 214, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblDesc2)
                            .addComponent(lblDesc1)
                            .addComponent(lblDesc3))
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(jPanel2Layout.createSequentialGroup()
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(txtPrecio1))
                                .addGroup(jPanel2Layout.createSequentialGroup()
                                    .addGap(45, 45, 45)
                                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(txtPrecio3)
                                            .addGroup(jPanel2Layout.createSequentialGroup()
                                                .addGap(2, 2, 2)
                                                .addComponent(txtPrecio2)))
                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                            .addComponent(label11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addGap(2, 2, 2)))
                                    .addGap(0, 0, Short.MAX_VALUE)))
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(57, 57, 57)
                                .addComponent(label7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addGap(85, 85, 85)
                        .addComponent(label10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(55, 55, 55)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButtonAnterior)
                            .addComponent(jButtonSiguiente)))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(64, 64, 64)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButtonAnterior1)
                            .addComponent(jButtonSiguiente1)
                            .addComponent(jButtonSiguiente2)
                            .addComponent(jButtonAnterior2))))
                .addGap(38, 38, 38)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton7)
                    .addComponent(jButton3)
                    .addComponent(jButton5))
                .addGap(45, 45, 45)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
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

        jMenu2.setText("CATALÓGO");

        jMenuItem1.setText("UÑAS");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItem1);

        jMenuItem2.setText("PEINADOS");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItem2);

        jMenuItem3.setText("MAQUILLAJ");
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem3ActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItem3);

        jMenuBar1.add(jMenu2);

        jMenu3.setText("AGENDAR CITA");

        jMenuItem4.setText("Cncelar Cita");
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

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    // Mostrar imagen escalada

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        // TODO add your handling code here:
        //para arir uñas
        NewJCatalogoUñas0 NewJCatalogoUñas = new NewJCatalogoUñas0();
        NewJCatalogoUñas.setVisible(true);
        this.dispose(); // cierra la actual


    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        // TODO add your handling code here:

        //para abrir peinados
        NewJCatalogoPeinado0 NewJCatalogoPeinado = new NewJCatalogoPeinado0();
        NewJCatalogoPeinado.setVisible(true);
        this.dispose(); // cierra la actual


    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void jMenu1MenuSelected(javax.swing.event.MenuEvent evt) {//GEN-FIRST:event_jMenu1MenuSelected
        // TODO add your handling code here:
        //inicio
        Inicio Inicio = new Inicio();
        Inicio.setVisible(true);
        this.dispose(); // cierra la actual

    }//GEN-LAST:event_jMenu1MenuSelected

    private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem3ActionPerformed
        // TODO add your handling code here:
        //para maquillaje
        NewJCatalogoMaq0 NewJCatalogoMaq = new NewJCatalogoMaq0();
        NewJCatalogoMaq.setVisible(true);
        this.dispose(); // cierra la actual

    }//GEN-LAST:event_jMenuItem3ActionPerformed

    private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem4ActionPerformed
        // TODO add your handling code here:
        //agendar cancelar
        NewJCancelarC NewJCancelarC = new NewJCancelarC();
        NewJCancelarC.setVisible(true);
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

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        // TODO add your handling code here:

    }//GEN-LAST:event_formWindowOpened

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        // TODO add your handling code here:
        String descripcion = "Peinado XV años";
        String precio = txtPrecio3.getText();
        ImageIcon imagen = (ImageIcon) lblImg3.getIcon();

        NewJAgenC ventanaCita = new NewJAgenC(imagen, descripcion, precio);
        ventanaCita.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        // TODO add your handling code here:
        String descripcion = "Peinado Social";
        String precio = txtPrecio2.getText();
        ImageIcon imagen = (ImageIcon) lblImg2.getIcon();

        NewJAgenC ventanaCita = new NewJAgenC(imagen, descripcion, precio);
        ventanaCita.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButtonAnteriorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAnteriorActionPerformed
        // TODO add your handling code here:
        anterior();
    }//GEN-LAST:event_jButtonAnteriorActionPerformed

    private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton7ActionPerformed
        // TODO add your handling code here:
        String descripcion = "Peinado boda";
        String precio = txtPrecio3.getText();
        ImageIcon imagen = (ImageIcon) lblImg3.getIcon();

        NewJAgenC ventanaCita = new NewJAgenC(imagen, descripcion, precio);
        ventanaCita.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_jButton7ActionPerformed

    private void jButtonSiguienteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSiguienteActionPerformed
        // TODO add your handling code here:
        siguiente();
    }//GEN-LAST:event_jButtonSiguienteActionPerformed

    private void jButtonAnterior1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAnterior1ActionPerformed
        // TODO add your handling code here:
        if (!categorias.isEmpty()) {
            indice = (indice - 1 + categorias.size()) % categorias.size();
            mostrarCategoria(indice);
        }
    }//GEN-LAST:event_jButtonAnterior1ActionPerformed

    private void jButtonSiguiente1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSiguiente1ActionPerformed
        // TODO add your handling code here:
        if (!categorias.isEmpty()) {
            indice = (indice + 1) % categorias.size();
            mostrarCategoria(indice);
        }
    }//GEN-LAST:event_jButtonSiguiente1ActionPerformed

    private void jButtonSiguiente2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSiguiente2ActionPerformed
        // TODO add your handling code here:
        if (!categorias.isEmpty()) {
            indice = (indice + 1) % categorias.size();
            mostrarCategoria(indice);
        }
    }//GEN-LAST:event_jButtonSiguiente2ActionPerformed

    private void jButtonAnterior2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAnterior2ActionPerformed
        // TODO add your handling code here:
        if (!categorias.isEmpty()) {
            indice = (indice - 1 + categorias.size()) % categorias.size();
            mostrarCategoria(indice);
        }
    }//GEN-LAST:event_jButtonAnterior2ActionPerformed

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
            java.util.logging.Logger.getLogger(NewJCatalogoPeinado0.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(NewJCatalogoPeinado0.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(NewJCatalogoPeinado0.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(NewJCatalogoPeinado0.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
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
                new NewJCatalogoPeinado0().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel FACE;
    private javax.swing.JLabel INS;
    private javax.swing.JLabel WPP;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton7;
    private javax.swing.JButton jButtonAnterior;
    private javax.swing.JButton jButtonAnterior1;
    private javax.swing.JButton jButtonAnterior2;
    private javax.swing.JButton jButtonSiguiente;
    private javax.swing.JButton jButtonSiguiente1;
    private javax.swing.JButton jButtonSiguiente2;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenu jMenu4;
    private javax.swing.JMenu jMenu6;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem jMenuItem5;
    private javax.swing.JMenuItem jMenuItem6;
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
