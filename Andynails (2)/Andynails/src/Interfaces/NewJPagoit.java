package Interfaces;

import andynails.ConexionBD;
import andynails.RedesSociales;
import andynails.SesionUsuario;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.filechooser.FileNameExtensionFilter;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
/**
 *
 * @author User
 */
public class NewJPagoit extends javax.swing.JFrame {

    ConexionBD conexion;
    private File archivoSeleccionado;

    /**
     * Creates new form NewJCitaConf
     */
    public NewJPagoit() {
        initComponents();
                RedesSociales.configurarRedesSociales(INS, WPP, FACE);

        conexion = new ConexionBD("andynails");// Inicializo la conexión a la base de datos
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        debugServiciosEnBD();
        actualizarInterfazConMonto();

    }

    private void actualizarInterfazConMonto() {
        double montoTotal = SesionUsuario.getMontoTotalCita();
        String textoTransferencia
                = "Datos para Transferencia Bancaria:\n"
                + "Banco: Nombre Banco\n"
                + "Cuenta CLABE: 012345678901234567\n"
                + "Titular: Andy Nails\n"
                + "Referencia: Anticipo + tu nombre completo\n\n"
                + "MONTO A TRANSFERIR: $" + montoTotal;

        jTextArea1.setText(textoTransferencia);
        jTextArea1.setEditable(false);

    }

    private int obtenerIdUsuarioActual() {
        return SesionUsuario.getIdUsuario();
    }

    private String usuarioActual() {
        String nombre = SesionUsuario.getNombreUsuario();
        if (nombre == null || nombre.isEmpty()) {
            nombre = "usuario_sin_sesion"; // nombre genérico o temporal
        }
        return nombre.replaceAll("\\s+", "_");
    }
    
    private void debugServiciosEnBD() {
    System.out.println("=== DEBUG SERVICIOS EN BD ===");
    String sql = """
        SELECT 
            s.idServicios,
            s.Nombre_servicio as Servicio,
            cs.idCategoria_Servicio,
            cs.Nombre_categoria as Categoria
        FROM servicios s
        LEFT JOIN categoria_servicio cs ON s.idServicios = cs.idServicios
        ORDER BY s.idServicios
        """;
    
    try (java.sql.Connection conn = ConexionBD.getConnection();
         java.sql.PreparedStatement ps = conn.prepareStatement(sql);
         java.sql.ResultSet rs = ps.executeQuery()) {
        
        while (rs.next()) {
            int idServicio = rs.getInt("idServicios");
            String servicio = rs.getString("Servicio");
            int idCategoria = rs.getInt("idCategoria_Servicio");
            String categoria = rs.getString("Categoria");
            
            System.out.println("Servicio ID: " + idServicio + " | '" + servicio + 
                             "' | Categoría ID: " + idCategoria + " | '" + categoria + "'");
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    System.out.println("=============================");
}


    private void guardarComprobanteEnBD(String nombreArchivo) {
        String sql = "UPDATE Pago SET Comprobante = ?, fecha_pago = NOW() WHERE idUsuarios = ?";

        try (Connection conn = new ConexionBD().getConexion(); PreparedStatement ps = conn.prepareStatement(sql)) {

            int idUsuario = obtenerIdUsuarioActual(); // viene de la sesión
            ps.setString(1, nombreArchivo);
            ps.setInt(2, idUsuario);

            int filas = ps.executeUpdate();
            if (filas == 0) {
                JOptionPane.showMessageDialog(this, "No se encontró un registro de pago para este usuario.");
            } else {
                System.out.println("Comprobante guardado correctamente en BD.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al guardar en la base de datos: " + e.getMessage());
        }
    }

    // Método para obtener el monto total (debes implementarlo según tu lógica)
    private double obtenerMontoTotalCita() {
        return SesionUsuario.getMontoTotalCita(); // Implementa este método en SesionUsuario
    }

    // MÉTODO NUEVO PARA INSERTAR CITA DESPUÉS DEL PAGO
    private void insertarCitaYServicios(int idPago) {
        String fecha = SesionUsuario.getFechaCita();
        String hora = SesionUsuario.getHoraCita();
        java.util.List<Object[]> servicios = SesionUsuario.getServiciosCita();

        if (fecha == null || hora == null || servicios.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Error: No hay datos de cita guardados.");
            return;
        }

        java.sql.Connection conn = null;
        java.sql.PreparedStatement psCita = null;
        java.sql.PreparedStatement psServicios = null;
        java.sql.ResultSet rsCita = null;

        try {
            conn = ConexionBD.getConnection();
            conn.setAutoCommit(false);

            // 1. INSERTAR CITA
            String sqlCita = "INSERT INTO cita (Fecha, Hora, Estado, idUsuarios, Pago_idPago) VALUES (?, ?, ?, ?, ?)";
            psCita = conn.prepareStatement(sqlCita, java.sql.Statement.RETURN_GENERATED_KEYS);

            psCita.setDate(1, java.sql.Date.valueOf(fecha));
            psCita.setTime(2, java.sql.Time.valueOf(hora + ":00"));
            psCita.setString(3, "Confirmada");
            psCita.setInt(4, SesionUsuario.getIdUsuario());
            psCita.setInt(5, idPago);

            psCita.executeUpdate();

            // OBTENER ID DE LA CITA
            rsCita = psCita.getGeneratedKeys();
            int idCita = 0;
            if (rsCita.next()) {
                idCita = rsCita.getInt(1);
            } else {
                throw new SQLException("No se pudo obtener el ID de la cita creada");
            }

            // 2. INSERTAR SERVICIOS DE LA CITA - CORREGIDO
            String sqlServicios = "INSERT INTO cita_has_servicios (idCita, idServicios, Pago_idPago, Monto_anticipo) VALUES (?, ?, ?, ?)";
            psServicios = conn.prepareStatement(sqlServicios);

            for (Object[] servicio : servicios) {
                String descripcion = (String) servicio[1];
                int idServicio = obtenerIdServicioPorDescripcion(descripcion);

                if (idServicio > 0) {
                    psServicios.setInt(1, idCita);
                    psServicios.setInt(2, idServicio);
                    psServicios.setInt(3, idPago);
                    psServicios.setBigDecimal(4, java.math.BigDecimal.valueOf(SesionUsuario.getMontoTotalCita()));
                    psServicios.addBatch();
                    System.out.println("Insertando servicio: " + descripcion + " -> ID: " + idServicio);
                } else {
                    System.out.println("No se encontró ID para servicio: " + descripcion);
                }
            }

            psServicios.executeBatch();
            conn.commit();

            JOptionPane.showMessageDialog(this, "¡Cita agendada exitosamente!");

            // Limpiar datos de la sesión
            SesionUsuario.limpiarDatosCita();

        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al agendar cita: " + e.getMessage());

            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        } finally {
            // Cerrar todos los recursos
            try {
                if (rsCita != null) {
                    rsCita.close();
                }
                if (psCita != null) {
                    psCita.close();
                }
                if (psServicios != null) {
                    psServicios.close();
                }
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

// Método auxiliar para obtener ID de servicio - CORREGIDO
    private int obtenerIdServicioPorDescripcion(String descripcion) {
        java.sql.Connection conn = null;
        java.sql.PreparedStatement ps = null;
        java.sql.ResultSet rs = null;

        try {
            conn = ConexionBD.getConnection();

            // PRIMERO buscar en la tabla servicios
            String sql = "SELECT idServicios FROM servicios WHERE Nombre_servicio LIKE ? OR Descripcion LIKE ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, "%" + descripcion + "%");
            ps.setString(2, "%" + descripcion + "%");
            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("idServicios");
            }

            // SI no encuentra, buscar en categoria_servicio
            rs.close();
            ps.close();

            String sqlCategoria = "SELECT idServicios FROM categoria_servicio WHERE Nombre_categoria LIKE ?";
            ps = conn.prepareStatement(sqlCategoria);
            ps.setString(1, "%" + descripcion + "%");
            rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("idServicios");
            }

        } catch (Exception e) {
            System.out.println("ERROR en obtenerIdServicioPorDescripcion: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Cerrar recursos
            try {
                if (rs != null) {
                    rs.close();
                }
                if (ps != null) {
                    ps.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        // Si no encuentra, usar búsqueda alternativa
        return buscarServicioAlternativo(descripcion);
    }

// Método de búsqueda alternativa
    private int buscarServicioAlternativo(String descripcion) {
        java.util.Map<String, Integer> mapeoServicios = new java.util.HashMap<>();

        // Mapeo de descripciones a IDs de servicio
        mapeoServicios.put("uñas francesa", 1);
        mapeoServicios.put("francesa", 1);
        mapeoServicios.put("uñas ballerina", 2);
        mapeoServicios.put("ballerina", 2);
        mapeoServicios.put("uñas cuadradas", 3);
        mapeoServicios.put("cuadradas", 3);
        mapeoServicios.put("maquillaje", 4);
        mapeoServicios.put("peinado", 5);
        mapeoServicios.put("peinados", 5);

        // Buscar por coincidencia parcial
        for (String clave : mapeoServicios.keySet()) {
            if (descripcion.toLowerCase().contains(clave.toLowerCase())) {
                System.out.println("Encontrado por mapeo alternativo: " + descripcion + " -> " + mapeoServicios.get(clave));
                return mapeoServicios.get(clave);
            }
        }

        System.out.println("No se encontró ID para: " + descripcion);
        return 0;
    }

    private int buscarCategoriaAlternativa(String descripcion) {
        int id = 0;
        String sql = "SELECT idServicios FROM categoria_servicio WHERE Nombre_categoria LIKE ? LIMIT 1";

        try (java.sql.Connection con = conexion.getConexion(); java.sql.PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, "%" + descripcion + "%");
            java.sql.ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                id = rs.getInt("idServicios");
            } else {
                // Usar valores por defecto
                if (descripcion.toLowerCase().contains("uña") || descripcion.contains("Ballerina")
                        || descripcion.contains("Cuadradas") || descripcion.contains("Francesa")) {
                    id = 1;
                } else if (descripcion.toLowerCase().contains("maquillaje")) {
                    id = 2;
                } else if (descripcion.toLowerCase().contains("peinado")) {
                    id = 3;
                }
            }

        } catch (Exception e) {
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
        jLabel1 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jLabel2 = new javax.swing.JLabel();
        btnEnviarComprobante = new javax.swing.JButton();
        btnsubirdocumento = new javax.swing.JButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu2 = new javax.swing.JMenu();
        jMenuItem6 = new javax.swing.JMenuItem();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem7 = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();
        jMenu4 = new javax.swing.JMenu();
        jMenuItem4 = new javax.swing.JMenuItem();
        jMenuItem8 = new javax.swing.JMenuItem();
        jMenu5 = new javax.swing.JMenu();
        jMenuItem5 = new javax.swing.JMenuItem();

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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(WPP, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(134, 134, 134)
                .addComponent(FACE, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(123, 123, 123))
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

        jLabel1.setIcon(new javax.swing.ImageIcon("C:\\Users\\mgmmo\\Documents\\7SEMESTRE\\INGENIERIASOF\\Andynails\\Andynails\\Img\\logo.jpg")); // NOI18N

        jTextArea1.setColumns(20);
        jTextArea1.setRows(5);
        jTextArea1.setText("Datos para Transferencia Bancaria:\n Banco: Nombre Banco\n Cuenta CLABE: 012345678901234567\n Titular: Andy Nails\n Referencia: Anticipo + tu nombre completo");
        jScrollPane1.setViewportView(jTextArea1);

        jLabel2.setFont(new java.awt.Font("Serif", 3, 14)); // NOI18N
        jLabel2.setText("Transferencia bancaria");

        btnEnviarComprobante.setBackground(new java.awt.Color(255, 204, 255));
        btnEnviarComprobante.setText("Enviar comprobante");
        btnEnviarComprobante.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEnviarComprobanteActionPerformed(evt);
            }
        });

        btnsubirdocumento.setBackground(new java.awt.Color(255, 204, 255));
        btnsubirdocumento.setText("Subir Documento");
        btnsubirdocumento.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnsubirdocumentoActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 222, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(59, 59, 59)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 257, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(132, 132, 132)
                                .addComponent(jLabel2)))
                        .addGap(0, 85, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnEnviarComprobante)
                        .addGap(17, 17, 17))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(138, 138, 138)
                        .addComponent(btnsubirdocumento)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 241, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(8, 8, 8)
                        .addComponent(jLabel2)
                        .addGap(18, 18, 18)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 127, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnsubirdocumento)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnEnviarComprobante)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 12, Short.MAX_VALUE)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jMenu2.setText("LOGIN");

        jMenuItem6.setText("jMenuItem6");
        jMenu2.add(jMenuItem6);

        jMenuBar1.add(jMenu2);

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

        jMenuItem7.setText("jMenuItem7");
        jMenu1.add(jMenuItem7);

        jMenuBar1.add(jMenu1);

        jMenu3.setText("CATALÓGO");

        jMenuItem2.setText("UÑAS");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu3.add(jMenuItem2);

        jMenuItem1.setText("PEINADO");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu3.add(jMenuItem1);

        jMenuItem3.setText("MAQUILLAJES");
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem3ActionPerformed(evt);
            }
        });
        jMenu3.add(jMenuItem3);

        jMenuBar1.add(jMenu3);

        jMenu4.setText("AGENDAR CITA");

        jMenuItem4.setText("AGENDAR CITA");
        jMenuItem4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem4ActionPerformed(evt);
            }
        });
        jMenu4.add(jMenuItem4);

        jMenuItem8.setText("CANCELAR CITA");
        jMenuItem8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem8ActionPerformed(evt);
            }
        });
        jMenu4.add(jMenuItem8);

        jMenuBar1.add(jMenu4);

        jMenu5.setText("CONTACTO");

        jMenuItem5.setText("Contacto");
        jMenuItem5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem5ActionPerformed(evt);
            }
        });
        jMenu5.add(jMenuItem5);

        jMenuBar1.add(jMenu5);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnsubirdocumentoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnsubirdocumentoActionPerformed
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Seleccionar comprobante de pago");
        fileChooser.setFileFilter(new FileNameExtensionFilter("PDF, JPG, PNG", "pdf", "jpg", "png"));

        int resultado = fileChooser.showOpenDialog(this);
        if (resultado == JFileChooser.APPROVE_OPTION) {
            archivoSeleccionado = fileChooser.getSelectedFile();
            JOptionPane.showMessageDialog(this, "Archivo seleccionado: " + archivoSeleccionado.getName());
        }
    }//GEN-LAST:event_btnsubirdocumentoActionPerformed

    private void btnEnviarComprobanteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEnviarComprobanteActionPerformed
        if (archivoSeleccionado == null) {
            JOptionPane.showMessageDialog(this, "Primero selecciona un archivo.");
            return;
        }

        // Obtener el monto total
        double montoTotal = SesionUsuario.getMontoTotalCita();

        String carpetaDestino = "comprobantes";
        File carpeta = new File(carpetaDestino);
        if (!carpeta.exists()) {
            carpeta.mkdirs();
        }

        try {
            String nombreArchivo = usuarioActual() + "_" + new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())
                    + "_" + archivoSeleccionado.getName();

            Path destino = Paths.get(carpetaDestino, nombreArchivo);
            Files.copy(archivoSeleccionado.toPath(), destino, StandardCopyOption.REPLACE_EXISTING);

            // INSERTAR PAGO Y OBTENER EL ID GENERADO
            try (java.sql.Connection conn = ConexionBD.getConnection()) {
                String sql = "INSERT INTO pago (idMetodo_Pago, fecha_pago, Estado_pago, Comprobante, Monto, idUsuarios) VALUES (?, ?, ?, ?, ?, ?)";
                java.sql.PreparedStatement ps = conn.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS);

                ps.setInt(1, 4); // ID método de pago (Transferencia bancaria)
                ps.setDate(2, new java.sql.Date(System.currentTimeMillis()));
                ps.setString(3, "Pendiente");
                ps.setString(4, nombreArchivo);
                ps.setDouble(5, montoTotal);
                ps.setInt(6, obtenerIdUsuarioActual());

                int affectedRows = ps.executeUpdate();

                if (affectedRows == 0) {
                    throw new java.sql.SQLException("Columnas no afectadas");
                }

                // OBTENER EL ID DEL PAGO RECIÉN INSERTADO
                java.sql.ResultSet generatedKeys = ps.getGeneratedKeys();
                int idPago = 0;
                if (generatedKeys.next()) {
                    idPago = generatedKeys.getInt(1);

                    // GUARDAR EL ID DEL PAGO EN LA SESIÓN
                    SesionUsuario.setIdPagoActual(idPago);

                    JOptionPane.showMessageDialog(this, "Comprobante enviado correctamente. ");

                    //  AHORA INSERTAR LA CITA Y SERVICIOS DESPUÉS DEL PAGO
                    insertarCitaYServicios(idPago);

                    // Cerrar esta ventana
                    NewJMiscitasCi cliWindow = new NewJMiscitasCi();
                    cliWindow.setVisible(true);
                    this.dispose();

                } else {
                    throw new java.sql.SQLException("comnprobante no obtenido");
                }

            }

        } catch (IOException | java.sql.SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }

    }//GEN-LAST:event_btnEnviarComprobanteActionPerformed

    private void jMenuItem5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem5ActionPerformed
        // TODO add your handling code here:
        //boton de contacto
        NewJContacto NewJContacto = new NewJContacto();
        NewJContacto.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem5ActionPerformed

    private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem4ActionPerformed
        // TODO add your handling code here:
        //agendar cancelar
        NewJCitaCliente NewJCitaCliente = new NewJCitaCliente();
        NewJCitaCliente.setVisible(true);
        this.dispose(); // cierra la actual
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem4ActionPerformed

    private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem3ActionPerformed
        // TODO add your handling code here:
        //para maquillaje
        NewJCatalogoMaq0 NewJCatalogoMaq = new NewJCatalogoMaq0();
        NewJCatalogoMaq.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem3ActionPerformed

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        // TODO add your handling code here:
        //para abrir peinados
        NewJCatalogoPeinado0 NewJCatalogoPeinado = new NewJCatalogoPeinado0();
        NewJCatalogoPeinado.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        // TODO add your handling code here:
        //para arir uñas
        NewJCatalogoUñas0 NewJCatalogoUñas = new NewJCatalogoUñas0();
        NewJCatalogoUñas.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void jMenu1MenuSelected(javax.swing.event.MenuEvent evt) {//GEN-FIRST:event_jMenu1MenuSelected
        // TODO add your handling code here:
        //inicio
        Inicio Inicio = new Inicio();
        Inicio.setVisible(true);
        this.dispose(); // cierra la actual

    }//GEN-LAST:event_jMenu1MenuSelected

    private void jMenuItem8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem8ActionPerformed
        // TODO add your handling code here:
        //    NewJCancelarC NewJCancelarC = new NewJCancelarC();
        //NewJCancelarC.setVisible(true);
        //this.dispose(); // cierra la actual

    }//GEN-LAST:event_jMenuItem8ActionPerformed

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
            java.util.logging.Logger.getLogger(NewJPagoit.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(NewJPagoit.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(NewJPagoit.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(NewJPagoit.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
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
                new NewJPagoit().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel FACE;
    private javax.swing.JLabel INS;
    private javax.swing.JLabel WPP;
    private javax.swing.JButton btnEnviarComprobante;
    private javax.swing.JButton btnsubirdocumento;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenu jMenu4;
    private javax.swing.JMenu jMenu5;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem jMenuItem5;
    private javax.swing.JMenuItem jMenuItem6;
    private javax.swing.JMenuItem jMenuItem7;
    private javax.swing.JMenuItem jMenuItem8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea jTextArea1;
    // End of variables declaration//GEN-END:variables
}
