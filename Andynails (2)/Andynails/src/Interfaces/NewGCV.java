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
public class NewGCV extends javax.swing.JFrame {

    // private javax.swing.JTextField jTextFieldNombreservicio;
    //private javax.swing.JTextField jTextField1; // descripción
    //private javax.swing.JTextField jTextFieldPrecio;
    /**
     * Creates new form NewGCV
     */
    public NewGCV() {
        initComponents();
        cargarServicios(); // carga los servicios desde la BD
        RedesSociales.configurarRedesSociales(INS, WPP, FACE);

        // Cargar tabla con el primer servicio si existe
        if (jComboBox1.getItemCount() > 0) {
            jComboBox1.setSelectedIndex(0);
            cargarTabla(jComboBox1.getSelectedItem().toString());
        }

        jComboBox1.addActionListener(e -> {
            Object seleccionado = jComboBox1.getSelectedItem();
            if (seleccionado != null) {
                cargarTabla(seleccionado.toString());
            }
        });
    }

// Método seguro para actualizar combo después de insertar un servicio
    public void actualizarComboServicios() {
        try (Connection cn = ConexionBD.getConnection()) {
            String sql = "SELECT Nombre_servicio FROM servicios ORDER BY idServicios";
            PreparedStatement ps = cn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            jComboBox1.removeAllItems(); // limpiar combo
            while (rs.next()) {
                jComboBox1.addItem(rs.getString("Nombre_servicio"));
            }

            rs.close();
            ps.close();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar servicios: " + e.getMessage());
        }
    }

    // Método para cargar servicios desde BD al iniciar
    public void cargarServicios() {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            Connection con = ConexionBD.getConnection();

            jComboBox1.removeAllItems();

            String sql = "SELECT Nombre_servicio FROM servicios ORDER BY idServicios";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                jComboBox1.addItem(rs.getString("Nombre_servicio"));
            }

            rs.close();
            ps.close();
            con.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al cargar servicios: " + e.getMessage());
        }
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

      //  jTable1setvicio.setRowHeight(120); // Altura suficiente para las imágenes
       // jTable1setvicio.setModel(modelo);

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
//            String nombreCategoria = jTable1setvicio.getValueAt(fila, 1).toString();
            String sql = "SELECT idCategoria_Servicio FROM categoria_servicio c "
                    + "JOIN servicios s ON c.idServicios = s.idServicios "
                    + "WHERE s.Nombre_servicio=? AND c.Nombre_categoria=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, servicio);
//            ps.setString(2, nombreCategoria);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                id = rs.getInt("idCategoria_Servicio");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return id;
    }

    private void actualizarComboBox() {
        try (Connection con = ConexionBD.getConnection()) {
            String sql = "SELECT Nombre_servicio FROM servicios";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            jComboBox1.removeAllItems(); // limpia el combo

            while (rs.next()) {
                jComboBox1.addItem(rs.getString("Nombre_servicio"));
            }

            rs.close();
            ps.close();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al actualizar ComboBox: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    
    private void btnEditarActionPerformed(java.awt.event.ActionEvent evt) {                                          
    String servicioSeleccionado = jComboBox1.getSelectedItem().toString();

    if (servicioSeleccionado == null || servicioSeleccionado.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Selecciona un servicio para editar.");
        return;
    }

    try (Connection conn = DriverManager.getConnection(
            "jdbc:mariadb://localhost:3307/andynails", "root", "mora")) {

        String sql = "SELECT Nombre_servicio, Descripcion, Precio FROM servicios WHERE Nombre_servicio = ?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, servicioSeleccionado);
        var rs = ps.executeQuery();

        if (rs.next()) {
            String nombre = rs.getString("Nombre_servicio");
            String descripcion = rs.getString("Descripcion");
            String precio = rs.getString("Precio");

            // 🔹 Crear la ventana de edición y pasarle los datos
            NewGCVInsertar ventanaEditar = new NewGCVInsertar();
            ventanaEditar.setVisible(true);

            // Llenar los campos con los valores obtenidos
            ventanaEditar.llenarCampos(nombre, descripcion, precio);

            this.dispose(); // cerrar ventana actual si quieres
        } else {
            JOptionPane.showMessageDialog(this, "No se encontró información del servicio.");
        }

    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Error al cargar datos: " + e.getMessage());
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
        jPanel4 = new javax.swing.JPanel();
        INS = new javax.swing.JLabel();
        FACE = new javax.swing.JLabel();
        WPP = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        btnAgregarservicios = new javax.swing.JButton();
        jComboBox1 = new javax.swing.JComboBox<>();
        btnEditar1 = new javax.swing.JButton();
        btnEliminar1 = new javax.swing.JButton();
        btnEliminar2 = new javax.swing.JButton();
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
                .addGap(206, 206, 206)
                .addComponent(WPP, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(154, 154, 154)
                .addComponent(FACE, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(81, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(INS)
                    .addComponent(FACE)
                    .addComponent(WPP))
                .addContainerGap(14, Short.MAX_VALUE))
        );

        jLabel5.setText("Servicio");

        jLabel2.setFont(new java.awt.Font("Serif", 3, 14)); // NOI18N
        jLabel2.setText("GESTION DE CATALÓGO VISUAL");

        btnAgregarservicios.setBackground(new java.awt.Color(255, 204, 255));
        btnAgregarservicios.setText("Agregar Nuevo servicio");
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

        btnEditar1.setBackground(new java.awt.Color(255, 204, 255));
        btnEditar1.setText("Editar Servicio");
        btnEditar1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEditar1ActionPerformed(evt);
            }
        });

        btnEliminar1.setBackground(new java.awt.Color(255, 204, 255));
        btnEliminar1.setText("Eliminar Servicio");
        btnEliminar1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEliminar1ActionPerformed(evt);
            }
        });

        btnEliminar2.setBackground(new java.awt.Color(255, 204, 255));
        btnEliminar2.setText("Cancelar");
        btnEliminar2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEliminar2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addGap(16, 16, 16)
                            .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addGap(155, 155, 155)
                            .addComponent(jLabel2))
                        .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnAgregarservicios)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnEditar1)
                        .addGap(61, 61, 61)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btnEliminar1)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addComponent(btnEliminar2)
                                .addGap(20, 20, 20)))
                        .addGap(137, 137, 137)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(31, 31, 31)
                .addComponent(jLabel2)
                .addGap(35, 35, 35)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(24, 24, 24)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnEliminar1)
                    .addComponent(btnEditar1)
                    .addComponent(btnAgregarservicios))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 27, Short.MAX_VALUE)
                .addComponent(btnEliminar2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
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
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 631, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 6, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1ActionPerformed
        // TODO add your handling code here:
        Object seleccionado = jComboBox1.getSelectedItem();
        if (seleccionado != null) {
            cargarTabla(seleccionado.toString());
        }
    }//GEN-LAST:event_jComboBox1ActionPerformed

    private void btnAgregarserviciosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAgregarserviciosActionPerformed
        NewGCVInsertar insertar = new NewGCVInsertar(this); // <-- PASAMOS "this"
        insertar.setVisible(true);
        // this.dispose(); // NO cierres la ventana principal


    }//GEN-LAST:event_btnAgregarserviciosActionPerformed

    private void btnEditar1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditar1ActionPerformed
        // TODO add your handling code here:

    String servicioSeleccionado = jComboBox1.getSelectedItem().toString();

    if (servicioSeleccionado == null || servicioSeleccionado.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Selecciona un servicio para editar.");
        return;
    }

    // Crear ventana y pasar el nombre seleccionado
    NewGCVEditar ventanaEditar = new NewGCVEditar(servicioSeleccionado);
    ventanaEditar.setVisible(true);
    this.dispose();

    }//GEN-LAST:event_btnEditar1ActionPerformed

    private void btnEliminar1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEliminar1ActionPerformed
        // TODO add your handling code here:
    // 🔹 Obtener el nombre del servicio seleccionado en el ComboBox
    String servicioSeleccionado = (String) jComboBox1.getSelectedItem();

    if (servicioSeleccionado == null || servicioSeleccionado.trim().isEmpty()) {
        JOptionPane.showMessageDialog(this, "Selecciona un servicio para eliminar.");
        return;
    }

    try (Connection conn = ConexionBD.getConnection()) {
        // Buscar el ID del servicio seleccionado
        PreparedStatement ps = conn.prepareStatement(
            "SELECT idServicios FROM servicios WHERE Nombre_servicio = ?");
        ps.setString(1, servicioSeleccionado);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            int id = rs.getInt("idServicios");

            // Abrir ventana de confirmación con el ID y el nombre
            NewGCVEliminar eliminar = new NewGCVEliminar(id, servicioSeleccionado);
            eliminar.setVisible(true);
            this.dispose();
        } else {
            JOptionPane.showMessageDialog(this,
                "No se encontró el servicio seleccionado en la base de datos.");
        }
    } catch (SQLException e) {
        JOptionPane.showMessageDialog(this,
            "Error al buscar el servicio: " + e.getMessage());
    }
    }//GEN-LAST:event_btnEliminar1ActionPerformed

    private void btnEliminar2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEliminar2ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnEliminar2ActionPerformed

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        // TODO add your handling code here:
         NewJCatalogoUñas NewJCatalogoUñas = new NewJCatalogoUñas();
        NewJCatalogoUñas.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        // TODO add your handling code here:
          //para abrir peinados
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
            java.util.logging.Logger.getLogger(NewGCV.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(NewGCV.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(NewGCV.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(NewGCV.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new NewGCV().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel FACE;
    private javax.swing.JLabel INS;
    private javax.swing.JLabel WPP;
    private javax.swing.JButton btnAgregarservicios;
    private javax.swing.JButton btnEditar1;
    private javax.swing.JButton btnEliminar1;
    private javax.swing.JButton btnEliminar2;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JLabel jLabel2;
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
    // End of variables declaration//GEN-END:variables
}
