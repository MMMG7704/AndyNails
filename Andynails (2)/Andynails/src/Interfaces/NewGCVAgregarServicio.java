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
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author User
 */
public class NewGCVAgregarServicio extends javax.swing.JFrame {

    private javax.swing.JTextField jTextFieldNombreservicio;
    private javax.swing.JTextField jTextField1; // descripción
    private javax.swing.JTextField jTextFieldPrecio;

    /**
     * Creates new form NewGCV
     */
    public NewGCVAgregarServicio() {
        initComponents();
                RedesSociales.configurarRedesSociales(INS, WPP, FACE);

        // Cargar datos inicialmente según la primera opción del JComboBox
        if (jComboBox1.getItemCount() > 0) {
            cargarTabla(jComboBox1.getItemAt(0));
        }

        // Evento para cambiar tabla al seleccionar otro servicio
        jComboBox1.addActionListener(e -> {
            String servicio = jComboBox1.getSelectedItem().toString();
            cargarTabla(servicio);
        });

        jComboBox1.addActionListener(evt -> {
            String seleccion = jComboBox1.getSelectedItem().toString();
            cargarTabla(seleccion);
        });

    }

    // Método para cargar la tabla con imágenes, nombre y precio
    private void cargarTabla(String categoriaSeleccionada) {
        DefaultTableModel modelo = new DefaultTableModel() {
            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 0) {
                    return ImageIcon.class; // Imagen
                }
                return Object.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        modelo.addColumn("Imagen");
        modelo.addColumn("Nombre");
        modelo.addColumn("Precio");

        jTable1setvicio.setRowHeight(120); // Altura suficiente para las imágenes
        jTable1setvicio.setModel(modelo);

        // Rutas base (usuario mgmmo) - las que compartiste
        String base = "C:\\Users\\mgmmo\\Documents\\7SEMESTRE\\INGENIERIASOF\\rubi\\Andynails (2)\\Andynails\\src\\Img\\";

        List<String> rutasImagenes = new ArrayList<>();
        List<String> rutasPreciosTxt = new ArrayList<>();

        // Agregamos TODAS las subcarpetas y sus archivos de precios según la categoría
        switch (categoriaSeleccionada) {
            case "Maquillaje":
                // tus rutas: Boda, Social, xv
                rutasImagenes.add(base + "Maquillaje\\Boda");
                rutasPreciosTxt.add(base + "Maquillaje\\Boda\\precios.txt");

                rutasImagenes.add(base + "Maquillaje\\Social");
                rutasPreciosTxt.add(base + "Maquillaje\\Social\\precios.txt");

                rutasImagenes.add(base + "Maquillaje\\xv");
                rutasPreciosTxt.add(base + "Maquillaje\\xv\\precios.txt");
                break;

            case "Peinados":
                // tus rutas: Boda, Social, XV (nota: tu carpeta tiene 'XV' mayúscula)
                rutasImagenes.add(base + "Peinados\\Boda");
                rutasPreciosTxt.add(base + "Peinados\\Boda\\precios.txt");

                rutasImagenes.add(base + "Peinados\\Social");
                rutasPreciosTxt.add(base + "Peinados\\Social\\precios.txt");

                rutasImagenes.add(base + "Peinados\\XV");
                rutasPreciosTxt.add(base + "Peinados\\XV\\precios.txt");
                break;

            case "Uñas":
            case "Unas": // por si en algún lado usas sin tilde
                // tus rutas: Francesa, Ballerina, Cuadradas
                rutasImagenes.add(base + "unas\\Francesa");
                rutasPreciosTxt.add(base + "unas\\Francesa\\precios.txt");

                rutasImagenes.add(base + "unas\\Ballerina");
                rutasPreciosTxt.add(base + "unas\\Ballerina\\precios.txt");

                rutasImagenes.add(base + "unas\\Cuadradas");
                rutasPreciosTxt.add(base + "unas\\Cuadradas\\precios.txt");
                break;

            case "Otros":
                // Si tienes subcarpetas para 'otros', agrégalas aquí (ejemplo vacío)
                break;

            default:
                // nada
                break;
        }

        // Recolectar todas las rutas de imagen y precios en una sola lista (manteniendo orden)
        List<String> todasImagenes = new ArrayList<>();
        List<String> todosPrecios = new ArrayList<>();

        for (int i = 0; i < rutasImagenes.size(); i++) {
            String carpeta = rutasImagenes.get(i);
            List<String> imgs = cargarImagenes(carpeta);
            todasImagenes.addAll(imgs);

            // cargar precios del archivo .txt correspondiente (si existe)
            String rutaPrecios = rutasPreciosTxt.get(i);
            List<String> precios = cargarPrecios(rutaPrecios);
            todosPrecios.addAll(precios);
        }

        // Si hay menos precios que imágenes, completamos con "0.00"
        while (todosPrecios.size() < todasImagenes.size()) {
            todosPrecios.add("0.00");
        }

        // Llenar la tabla
        for (int i = 0; i < todasImagenes.size(); i++) {
            String rutaImg = todasImagenes.get(i);
            String precioStr = (i < todosPrecios.size()) ? todosPrecios.get(i) : "0.00";
            double precio = 0.0;
            try {
                // limpiar caracteres y símbolos si los hay
                precio = Double.parseDouble(precioStr.replace("$", "").replace(",", "").trim());
            } catch (NumberFormatException ex) {
                precio = 0.0;
            }
            // Nombre: puedes cambiar por el nombre del archivo o una lógica propia
            String nombre = Paths.get(rutaImg).getFileName().toString();

            // Crear ImageIcon escalada
            ImageIcon icono = null;
            try {
                ImageIcon tmp = new ImageIcon(rutaImg);
                Image imgEscalada = tmp.getImage().getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                icono = new ImageIcon(imgEscalada);
            } catch (Exception ex) {
                icono = new ImageIcon(); // icono vacío si falla
            }

            modelo.addRow(new Object[]{icono, nombre, String.format("$ %.2f", precio)});
        }

        // Si no hay imágenes, muestra una fila informativa
        if (todasImagenes.isEmpty()) {
            modelo.addRow(new Object[]{new ImageIcon(), "No hay servicios", ""});
        }
    }

// Método para listar imágenes de una carpeta (devuelve rutas absolutas)
    private List<String> cargarImagenes(String rutaCarpeta) {
        List<String> lista = new ArrayList<>();
        try {
            File carpeta = new File(rutaCarpeta);
            if (!carpeta.exists() || !carpeta.isDirectory()) {
                return lista;
            }
            File[] archivos = carpeta.listFiles();
            if (archivos == null) {
                return lista;
            }
            for (File f : archivos) {
                if (f.isFile()) {
                    String nombre = f.getName().toLowerCase();
                    if (nombre.endsWith(".jpg") || nombre.endsWith(".jpeg") || nombre.endsWith(".png") || nombre.endsWith(".gif") || nombre.endsWith(".bmp")) {
                        lista.add(f.getAbsolutePath());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }

    private List<String> cargarPrecios(String rutaArchivo) {
        List<String> lista = new ArrayList<>();
        File f = new File(rutaArchivo);
        if (!f.exists() || !f.isFile()) {
            return lista;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String linea;
            while ((linea = br.readLine()) != null) {
                if (!linea.trim().isEmpty()) {
                    lista.add(linea.trim());
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return lista;
    }

    // Método para obtener el id de la categoría según la fila seleccionada
    private int obtenerIdDeFila(int fila) {
        int id = -1;
        try (Connection con = (Connection) ConexionBD.getConnection()) {
            String servicio = jComboBox1.getSelectedItem().toString();
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
        jComboBox1 = new javax.swing.JComboBox<>();
        btninsertar = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu3 = new javax.swing.JMenu();
        jMenu4 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();
        jMenu5 = new javax.swing.JMenu();
        jMenuItem6 = new javax.swing.JMenuItem();
        jMenu6 = new javax.swing.JMenu();
        jMenuItem4 = new javax.swing.JMenuItem();
        jMenu7 = new javax.swing.JMenu();
        jMenuItem5 = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

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
                .addGap(109, 109, 109)
                .addComponent(WPP, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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

        jLabel5.setText("Servicio");

        jLabel2.setFont(new java.awt.Font("Serif", 3, 14)); // NOI18N
        jLabel2.setText("GESTION DE CATALÓGO VISUAL");

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
        btnEditar.setText("Editar");
        btnEditar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEditarActionPerformed(evt);
            }
        });

        btnEliminar.setBackground(new java.awt.Color(255, 204, 255));
        btnEliminar.setText("Eliminar");
        btnEliminar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEliminarActionPerformed(evt);
            }
        });

        btnAgregarservicios.setBackground(new java.awt.Color(255, 204, 255));
        btnAgregarservicios.setText("Agregar servicio");
        btnAgregarservicios.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAgregarserviciosActionPerformed(evt);
            }
        });

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Maquillaje", "Uñas", "Peinados", "otros" }));
        jComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox1ActionPerformed(evt);
            }
        });

        btninsertar.setBackground(new java.awt.Color(255, 204, 255));
        btninsertar.setText("Insertar");
        btninsertar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btninsertarActionPerformed(evt);
            }
        });

        jLabel4.setFont(new java.awt.Font("Serif", 3, 14)); // NOI18N
        jLabel4.setText("Categoria de cada servicio");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(163, 163, 163)
                        .addComponent(jLabel2))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(35, 35, 35)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 443, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(172, 172, 172)
                        .addComponent(jLabel4))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(58, 58, 58)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(53, 53, 53)
                                .addComponent(btnAgregarservicios))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(btninsertar)
                                .addGap(74, 74, 74)
                                .addComponent(btnEliminar)
                                .addGap(95, 95, 95)
                                .addComponent(btnEditar)))))
                .addContainerGap(38, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addComponent(jLabel2)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 158, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(30, 30, 30)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnAgregarservicios))
                .addGap(104, 104, 104)
                .addComponent(jLabel4)
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btninsertar)
                    .addComponent(btnEliminar)
                    .addComponent(btnEditar))
                .addGap(35, 35, 35)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jMenu3.setText("LOGO");
        jMenuBar1.add(jMenu3);

        jMenu4.setText("CATALÓGO");

        jMenuItem1.setText("UÑAS");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu4.add(jMenuItem1);

        jMenuItem2.setText("PEINADOS");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu4.add(jMenuItem2);

        jMenuItem3.setText("MAQUILLAJES");
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem3ActionPerformed(evt);
            }
        });
        jMenu4.add(jMenuItem3);

        jMenuBar1.add(jMenu4);

        jMenu5.setText("AGENDAR CITA");

        jMenuItem6.setText("CANECLAR CITA");
        jMenu5.add(jMenuItem6);

        jMenuBar1.add(jMenu5);

        jMenu6.setText("CONTACTO");

        jMenuItem4.setText("Contacto");
        jMenu6.add(jMenuItem4);

        jMenuBar1.add(jMenu6);

        jMenu7.setText("LOGIN");

        jMenuItem5.setText("Login");
        jMenu7.add(jMenuItem5);

        jMenuBar1.add(jMenu7);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnEditarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditarActionPerformed
        // TODO add your handling code here:
        NewGCVEditar NewGCVEditar = new NewGCVEditar();
        NewGCVEditar.setVisible(true);
        this.dispose(); // cierra la actual

    }//GEN-LAST:event_btnEditarActionPerformed

    private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1ActionPerformed
        // TODO add your handling code here:
            String cat = jComboBox1.getSelectedItem().toString();
    cargarTabla(cat);
    }//GEN-LAST:event_jComboBox1ActionPerformed

    private void btnAgregarserviciosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAgregarserviciosActionPerformed
        // TODO add your handling code here:
    String nombre = jTextFieldNombreservicio.getText().trim();
    String descripcion = jTextField1.getText().trim();
    String precioText = jTextFieldPrecio.getText().trim();

    if(nombre.isEmpty()){
        JOptionPane.showMessageDialog(this, "Debe ingresar un nombre de servicio", "Advertencia", JOptionPane.WARNING_MESSAGE);
        return;
    }

    double precio = 0;
    try {
        if(!precioText.isEmpty())
            precio = Double.parseDouble(precioText);
    } catch(NumberFormatException e){
        JOptionPane.showMessageDialog(this, "Precio inválido", "Error", JOptionPane.ERROR_MESSAGE);
        return;
    }

    try {
        Class.forName("org.mariadb.jdbc.Driver");
        Connection con = DriverManager.getConnection(
            "jdbc:mariadb://localhost:3307/andynails", "root", "mora"
        );

        String sql = "INSERT INTO servicios(Nombre_servicio, Descripcion, Precio) VALUES(?, ?, ?)";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, nombre);
        ps.setString(2, descripcion);
        ps.setDouble(3, precio);

        int filas = ps.executeUpdate();
        if(filas > 0){
            JOptionPane.showMessageDialog(this, "¡Servicio agregado correctamente!");
            // Limpiar campos
            jTextFieldNombreservicio.setText("");
            jTextField1.setText("");
            jTextFieldPrecio.setText("");
        }

        ps.close();
        con.close();

    } catch(ClassNotFoundException e){
        JOptionPane.showMessageDialog(this, "Driver no encontrado: " + e.getMessage());
    } catch(SQLException e){
        JOptionPane.showMessageDialog(this, "Error al guardar: " + e.getMessage());
    }


    }//GEN-LAST:event_btnAgregarserviciosActionPerformed

    private void btninsertarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btninsertarActionPerformed
        // TODO add your handling code here:

         NewGCVInsertar newGCVInsertar = new NewGCVInsertar();
        newGCVInsertar.setVisible(true);
        this.dispose(); // cierra la actual
        
    }//GEN-LAST:event_btninsertarActionPerformed

    private void btnEliminarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEliminarActionPerformed
        // TODO add your handling code here:
        NewGCVEliminar NewGCVEliminar = new NewGCVEliminar();
        NewGCVEliminar.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_btnEliminarActionPerformed

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
            java.util.logging.Logger.getLogger(NewGCVAgregarServicio.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(NewGCVAgregarServicio.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(NewGCVAgregarServicio.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(NewGCVAgregarServicio.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new NewGCVAgregarServicio().setVisible(true);
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
    private javax.swing.JButton btninsertar;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenu jMenu4;
    private javax.swing.JMenu jMenu5;
    private javax.swing.JMenu jMenu6;
    private javax.swing.JMenu jMenu7;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem jMenuItem5;
    private javax.swing.JMenuItem jMenuItem6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1setvicio;
    // End of variables declaration//GEN-END:variables
}
