/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package Interfaces;

import Interfaces.NewGCVEliminar;
import Interfaces.NewGCVInsertar;
import andynails.ConexionBD;
import andynails.RedesSociales;
import java.awt.Image;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author User
 */
public class NewGCVCategoriaServicio extends javax.swing.JFrame {

    private javax.swing.JTextField jTextFieldNombreservicio;
    private javax.swing.JTextField jTextField1; // descripción
    private javax.swing.JTextField jTextFieldPrecio;
    private javax.swing.JComboBox<String> jComboServicios = new javax.swing.JComboBox<>();

    /**
     * Creates new form NewGCV
     */
    public NewGCVCategoriaServicio() {
        initComponents();
        RedesSociales.configurarRedesSociales(INS, WPP, FACE);

        jComboServicios.addItem("Maquillaje");
        jComboServicios.addItem("Peinados");
        jComboServicios.addItem("Uñas");
        jComboServicios.addItem("Otros");

        // Cargar datos inicialmente según la primera opción del JComboBox
        if (jComboServicios.getItemCount() > 0) {
            cargarTabla(jComboServicios.getItemAt(0));
        }

        // Evento para cambiar tabla al seleccionar otro servicio
        jComboServicios.addActionListener(e -> {
            String servicio = jComboServicios.getSelectedItem().toString();
            cargarTabla(servicio);
        });

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

    // Método para cargar la tabla con imágenes, nombre y precio
    private void cargarTabla(String categoriaSeleccionada) {
        DefaultTableModel modelo = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        modelo.addColumn("Imagen");
        modelo.addColumn("Nombre de Categoría");
        modelo.addColumn("Descripción");
        modelo.addColumn("Precio");

        jTable1setvicio.setModel(modelo);
        jTable1setvicio.setRowHeight(70);

        try (Connection con = ConexionBD.getConnection()) {
            //Sin filtro por servicio para mostrar todas las categorías
            String sql = "SELECT cs.Imagen_Archivo, cs.Nombre_categoria, cs.Descripcion, cs.Precio "
                    + "FROM categoria_servicio cs "
                    + "INNER JOIN servicios s ON cs.idServicios = s.idServicios";

            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String rutaImagen = rs.getString("Imagen_Archivo");
                ImageIcon icono = new ImageIcon(rutaImagen);
                Image imagen = icono.getImage().getScaledInstance(60, 60, Image.SCALE_SMOOTH);
                icono = new ImageIcon(imagen);

                String nombre = rs.getString("Nombre_categoria");
                String descripcion = rs.getString("Descripcion");
                double precio = rs.getDouble("Precio");

                modelo.addRow(new Object[]{
                    icono,
                    nombre,
                    descripcion,
                    String.format("$ %.2f", precio)
                });
            }

            if (modelo.getRowCount() == 0) {
                modelo.addRow(new Object[]{"", "Sin categorías registradas", "", ""});
            }

            jTable1setvicio.setModel(modelo);

            jTable1setvicio.getColumn("Imagen").setCellRenderer(new DefaultTableCellRenderer() {
                @Override
                public void setValue(Object value) {
                    if (value instanceof ImageIcon) {
                        setIcon((ImageIcon) value);
                        setText("");
                        setHorizontalAlignment(CENTER); // centramos imagen
                    } else {
                        setIcon(null);
                        setText((value != null) ? value.toString() : "");
                    }
                }
            });

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar categorías: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    // Método para obtener el id de la categoría según la fila seleccionada
    private int obtenerIdDeFila(int fila) {
        int id = -1;
        try (Connection con = (Connection) ConexionBD.getConnection()) {
            String servicio = jComboServicios.getSelectedItem().toString();
            String nombreCategoria = jTable1setvicio.getValueAt(fila, 1).toString();
            String sql = "SELECT idCategoria_Servicio FROM categoria_servicio c "
                    + "JOIN servicios s ON c.idServicios = s.idServicios "
                    + "WHERE s.Nombre_servicio=? AND c.Nombre_categoria=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, servicio);
            ps.setString(2, nombreCategoria);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                id = rs.getInt("idCategoria_Servicio");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return id;
    }

    private int obtenerIdServicio(String nombreServicio) {
        int id = -1;
        try (Connection con = ConexionBD.getConnection()) {
            String sql = "SELECT idServicios FROM servicios WHERE Nombre_servicio = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, nombreServicio);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                id = rs.getInt("idServicios");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return id;
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
        jPanel4 = new javax.swing.JPanel();
        INS = new javax.swing.JLabel();
        FACE = new javax.swing.JLabel();
        WPP = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1setvicio = new javax.swing.JTable();
        btnEditar = new javax.swing.JButton();
        btnEliminar = new javax.swing.JButton();
        btnAgregarservicios = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu3 = new javax.swing.JMenu();
        jMenu12 = new javax.swing.JMenu();
        jMenuItem7 = new javax.swing.JMenuItem();
        jMenuItem8 = new javax.swing.JMenuItem();
        jMenuItem9 = new javax.swing.JMenuItem();
        jMenuItem10 = new javax.swing.JMenuItem();
        jMenu5 = new javax.swing.JMenu();
        jMenuItem11 = new javax.swing.JMenuItem();
        jMenu6 = new javax.swing.JMenu();
        jMenuItem4 = new javax.swing.JMenuItem();
        jMenu7 = new javax.swing.JMenu();
        jMenuItem5 = new javax.swing.JMenuItem();
        jMenu16 = new javax.swing.JMenu();
        jMenuItemCerrarSecion = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(243, 224, 255));

        jPanel4.setBackground(new java.awt.Color(204, 0, 204));

        INS.setText("INS");

        FACE.setText("FACE");

        WPP.setText("WPP");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(115, 115, 115)
                .addComponent(INS, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(193, 193, 193)
                .addComponent(WPP, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 168, Short.MAX_VALUE)
                .addComponent(FACE, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(79, 79, 79))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(INS)
                    .addComponent(WPP)
                    .addComponent(FACE))
                .addContainerGap(14, Short.MAX_VALUE))
        );

        jLabel5.setText("Selecionar servicio para agregar categoria :");

        jLabel2.setFont(new java.awt.Font("Serif", 3, 18)); // NOI18N
        jLabel2.setText("Categoria de cada servicio");

        jTable1setvicio.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null},
                {null, null, null},
                {null, null, null},
                {null, null, null}
            },
            new String [] {
                "Imagen", "Nombre", "Precio"
            }
        ));
        jScrollPane1.setViewportView(jTable1setvicio);

        btnEditar.setBackground(new java.awt.Color(255, 204, 255));
        btnEditar.setText("Editar Categoria");
        btnEditar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEditarActionPerformed(evt);
            }
        });

        btnEliminar.setBackground(new java.awt.Color(255, 204, 255));
        btnEliminar.setText("Eliminar Categoria");
        btnEliminar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEliminarActionPerformed(evt);
            }
        });

        btnAgregarservicios.setBackground(new java.awt.Color(255, 204, 255));
        btnAgregarservicios.setText("Agregar Nueva Categoria");
        btnAgregarservicios.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAgregarserviciosActionPerformed(evt);
            }
        });

        jButton2.setBackground(new java.awt.Color(255, 204, 255));
        jButton2.setText("Regresar");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(45, 45, 45)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(118, 118, 118)
                                .addComponent(jLabel2))
                            .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 244, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(btnAgregarservicios)
                                .addGap(26, 26, 26)
                                .addComponent(btnEditar)
                                .addGap(36, 36, 36)
                                .addComponent(btnEliminar)))
                        .addGap(29, 29, 29)
                        .addComponent(jButton2))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 501, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(104, 104, 104))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel5)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel2)
                        .addGap(58, 58, 58)))
                .addGap(40, 40, 40)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 158, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(32, 32, 32)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnAgregarservicios)
                    .addComponent(btnEditar)
                    .addComponent(btnEliminar)
                    .addComponent(jButton2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 51, Short.MAX_VALUE)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jMenu3.setText("LOGO");
        jMenuBar1.add(jMenu3);

        jMenu12.setText("CATALÓGO");

        jMenuItem7.setText("Uñas");
        jMenuItem7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem7ActionPerformed(evt);
            }
        });
        jMenu12.add(jMenuItem7);

        jMenuItem8.setText("Peinados");
        jMenuItem8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem8ActionPerformed(evt);
            }
        });
        jMenu12.add(jMenuItem8);

        jMenuItem9.setText("Maquillaje");
        jMenuItem9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem9ActionPerformed(evt);
            }
        });
        jMenu12.add(jMenuItem9);

        jMenuItem10.setText("Otros");
        jMenuItem10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem10ActionPerformed(evt);
            }
        });
        jMenu12.add(jMenuItem10);

        jMenuBar1.add(jMenu12);

        jMenu5.setText("AGENDAR CITA");

        jMenuItem11.setText("Agendar Cita");
        jMenuItem11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem11ActionPerformed(evt);
            }
        });
        jMenu5.add(jMenuItem11);

        jMenuBar1.add(jMenu5);

        jMenu6.setText("CONTACTO");

        jMenuItem4.setText("Contacto");
        jMenu6.add(jMenuItem4);

        jMenuBar1.add(jMenu6);

        jMenu7.setText("LOGIN");

        jMenuItem5.setText("Login");
        jMenu7.add(jMenuItem5);

        jMenuBar1.add(jMenu7);

        jMenu16.setText("CERRAR SESIÓN");

        jMenuItemCerrarSecion.setText("Cerrar sesión");
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
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnEditarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditarActionPerformed
        // TODO add your handling code here:
        NewGCVEditarCategoriaServicio editar = new NewGCVEditarCategoriaServicio();
        editar.setVisible(true);
        this.dispose();

    }//GEN-LAST:event_btnEditarActionPerformed

    private void btnAgregarserviciosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAgregarserviciosActionPerformed
        // TODO add your handling code here:
        String servicioSeleccionado = jComboServicios.getSelectedItem().toString();
        int idServicioSeleccionado = obtenerIdServicio(servicioSeleccionado); // método que obtendrá el id desde BD

        NewGCVInsertarCategoriaServicio ventanaInsertar = new NewGCVInsertarCategoriaServicio();
        ventanaInsertar.setDatosServicio(idServicioSeleccionado, servicioSeleccionado); // pasamos id y nombre
        ventanaInsertar.setVisible(true);
        this.dispose();

    }//GEN-LAST:event_btnAgregarserviciosActionPerformed

    private void btnEliminarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEliminarActionPerformed
        // TODO add your handling code here:
        NewGCVEliminarCategoriaServicio eliminar = new NewGCVEliminarCategoriaServicio();
        eliminar.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btnEliminarActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        // TODO add your handling code here:
        NewJPanelAdministracion anterior = new NewJPanelAdministracion();
        anterior.setVisible(true);
        this.dispose(); // Cierra la ventana actual
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jMenuItem7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem7ActionPerformed
        // TODO add your handling code here:
        NewJCatalogoUñas NewJCatalogoUñas = new NewJCatalogoUñas();
        NewJCatalogoUñas.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem7ActionPerformed

    private void jMenuItem8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem8ActionPerformed
        // TODO add your handling code here:
        NewJCatalogoPeinado NewJCatalogoPeinado = new NewJCatalogoPeinado();
        NewJCatalogoPeinado.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem8ActionPerformed

    private void jMenuItem9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem9ActionPerformed
        // TODO add your handling code here:
        NewJCatalogoMaq NewJCatalogoMaq = new NewJCatalogoMaq();
        NewJCatalogoMaq.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem9ActionPerformed

    private void jMenuItem10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem10ActionPerformed
        // TODO add your handling code here:
        ConexionBD conexionCatalogo = new ConexionBD("andynails");
        NewJCatalogoGenerico catalogo = new NewJCatalogoGenerico(conexionCatalogo);
        catalogo.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem10ActionPerformed

    private void jMenuItem11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem11ActionPerformed
        // TODO add your handling code here:
        //agendar cita
        NewJAgenC NewJAgenC = new NewJAgenC();
        NewJAgenC.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem11ActionPerformed

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
            java.util.logging.Logger.getLogger(NewGCVCategoriaServicio.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(NewGCVCategoriaServicio.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(NewGCVCategoriaServicio.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(NewGCVCategoriaServicio.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new NewGCVCategoriaServicio().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel FACE;
    private javax.swing.JLabel INS;
    private javax.swing.JLabel WPP;
    private javax.swing.JButton btnAgregarservicios;
    private javax.swing.JButton btnEditar;
    private javax.swing.JButton btnEliminar;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JMenu jMenu12;
    private javax.swing.JMenu jMenu16;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenu jMenu5;
    private javax.swing.JMenu jMenu6;
    private javax.swing.JMenu jMenu7;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem10;
    private javax.swing.JMenuItem jMenuItem11;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem jMenuItem5;
    private javax.swing.JMenuItem jMenuItem7;
    private javax.swing.JMenuItem jMenuItem8;
    private javax.swing.JMenuItem jMenuItem9;
    private javax.swing.JMenuItem jMenuItemCerrarSecion;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1setvicio;
    // End of variables declaration//GEN-END:variables
}
