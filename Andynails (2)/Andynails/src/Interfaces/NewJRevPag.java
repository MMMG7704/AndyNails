/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package Interfaces;

import andynails.ConexionBD;
import andynails.RedesSociales;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author Mariana Mora
 */
public class NewJRevPag extends javax.swing.JFrame {

    ConexionBD conexion;

    /**
     * Creates new form NewJRevPag
     */
    public NewJRevPag() {
        initComponents();
        RedesSociales.configurarRedesSociales(INS, WPP, FACE);
        conexion = new andynails.ConexionBD("andynails");

        // Configuración simple
        configurarComboBox();
        ocultarColumnaID();

        // Cargar pagos después de un pequeño delay
        javax.swing.Timer timer = new javax.swing.Timer(100, new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                cargarPagosDesdeBD();
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {
        // Contacto
        NewJContacto NewJContacto = new NewJContacto();
        NewJContacto.setVisible(true);
        this.dispose();
    }

    private void jMenuItem5ActionPerformed(java.awt.event.ActionEvent evt) {
        // Login
        NewJLogin NewJLogin = new NewJLogin();
        NewJLogin.setVisible(true);
        this.dispose();
    }

    private void jMenuItem6ActionPerformed(java.awt.event.ActionEvent evt) {
        // Cancelar Cita
        NewJCitaAgendaE NewJCitaAgenda = new NewJCitaAgendaE();
        NewJCitaAgenda.setVisible(true);
        this.dispose();
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

    private void configurarComboBox() {
        try {
            // Configurar ComboBox de fechas
            jComboBox1.removeAllItems();
            jComboBox1.addItem("Todas las fechas");

            String sqlFechas = "SELECT DISTINCT DATE(fecha_pago) as fecha FROM pago ORDER BY fecha DESC";
            java.sql.PreparedStatement psFechas = conexion.conectar().prepareStatement(sqlFechas);
            java.sql.ResultSet rsFechas = psFechas.executeQuery();

            while (rsFechas.next()) {
                String fecha = rsFechas.getString("fecha");
                if (fecha != null) {
                    jComboBox1.addItem(fecha);
                }
            }
            rsFechas.close();
            psFechas.close();

            // Configurar ComboBox de clientes - Busca en ambos lugares
            jComboBox2.removeAllItems();
            jComboBox2.addItem("Todos los clientes");

            String sqlClientes = "SELECT DISTINCT COALESCE(u.Nombre, 'Sin cliente') as Nombre "
                    + "FROM ("
                    + "  SELECT p.idUsuarios FROM pago p "
                    + "  UNION "
                    + "  SELECT c.idUsuarios FROM cita c WHERE c.Pago_idPago IS NOT NULL"
                    + ") as ids "
                    + "LEFT JOIN usuarios u ON ids.idUsuarios = u.idUsuarios "
                    + "ORDER BY Nombre";

            java.sql.PreparedStatement psClientes = conexion.conectar().prepareStatement(sqlClientes);
            java.sql.ResultSet rsClientes = psClientes.executeQuery();

            while (rsClientes.next()) {
                String nombre = rsClientes.getString("Nombre");
                jComboBox2.addItem(nombre);
            }
            rsClientes.close();
            psClientes.close();

            // Establecer selecciones por defecto
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    jComboBox1.setSelectedItem("Todas las fechas");
                    jComboBox2.setSelectedItem("Todos los clientes");
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cargarPagosDesdeBD() {
        try {
            // Consulta mejorada para obtener siempre el cliente correcto
            String sql = "SELECT "
                    + "p.idPago, "
                    + "CASE "
                    + "  WHEN COALESCE(u2.idUsuarios, u1.idUsuarios) IS NULL THEN 'Sin cliente' "
                    + "  WHEN c.idUsuarios IS NOT NULL THEN CONCAT(u2.Nombre, ' ', COALESCE(u2.Paterno, ''), ' ', COALESCE(u2.Materno, '')) "
                    + "  ELSE CONCAT(u1.Nombre, ' ', COALESCE(u1.Paterno, ''), ' ', COALESCE(u1.Materno, '')) "
                    + "END as Cliente, "
                    + "COALESCE(p.Monto, p.Monto_pagado, 0) as Monto, "
                    + "p.fecha_pago, "
                    + "p.Estado_pago, "
                    + "p.Comprobante "
                    + "FROM pago p "
                    + "LEFT JOIN cita c ON p.idPago = c.Pago_idPago "
                    + "LEFT JOIN usuarios u1 ON p.idUsuarios = u1.idUsuarios "
                    + "LEFT JOIN usuarios u2 ON c.idUsuarios = u2.idUsuarios "
                    + "WHERE 1=1";

            // Agregar filtros si es necesario
            java.util.List<Object> parametros = new java.util.ArrayList<>();

            String fechaSeleccionada = (jComboBox1.getSelectedItem() != null)
                    ? jComboBox1.getSelectedItem().toString() : "Todas las fechas";
            String clienteSeleccionado = (jComboBox2.getSelectedItem() != null)
                    ? jComboBox2.getSelectedItem().toString() : "Todos los clientes";

            if (!fechaSeleccionada.equals("Todas las fechas")) {
                sql += " AND DATE(p.fecha_pago) = ?";
                parametros.add(fechaSeleccionada);
            }

            if (!clienteSeleccionado.equals("Todos los clientes")
                    && !clienteSeleccionado.equals("Sin cliente")) {
                sql += " AND (u1.Nombre LIKE ? OR u2.Nombre LIKE ?)";
                parametros.add("%" + clienteSeleccionado + "%");
                parametros.add("%" + clienteSeleccionado + "%");
            }

            sql += " ORDER BY p.fecha_pago DESC, p.idPago DESC";

            java.sql.PreparedStatement ps = conexion.conectar().prepareStatement(sql);

            // Establecer parámetros
            for (int i = 0; i < parametros.size(); i++) {
                ps.setObject(i + 1, parametros.get(i));
            }

            java.sql.ResultSet rs = ps.executeQuery();

            javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) jTable1.getModel();
            model.setRowCount(0);

            // Columnas - ID Pago como primera columna (pero la ocultaremos)
            model.setColumnIdentifiers(new String[]{
                "ID", // Columna 0: ID Pago (oculta)
                "Cliente", // Columna 1: Cliente
                "Monto", // Columna 2: Monto
                "Fecha", // Columna 3: Fecha
                "Estado", // Columna 4: Estado
                "Comprobante" // Columna 5: Comprobante
            });

            while (rs.next()) {
                String comprobante = rs.getString("Comprobante");
                String estadoComprobante = "Sin comprobante";

                if (comprobante != null && !comprobante.trim().isEmpty() && !comprobante.equalsIgnoreCase("NULL")) {
                    // Verificar si el archivo existe
                    java.io.File archivoComprobante = new java.io.File("comprobantes/" + comprobante);
                    estadoComprobante = archivoComprobante.exists() ? "Con comprobante" : " Archivo no encontrado";
                }

                double monto = rs.getDouble("Monto");
                String montoStr = (monto > 0) ? "$" + String.format("%.2f", monto) : "$0.00";

                // Obtener cliente
                String cliente = rs.getString("Cliente");
                if (cliente == null || cliente.trim().isEmpty() || cliente.equals("null null null")) {
                    cliente = "Sin cliente";
                }

                model.addRow(new Object[]{
                    rs.getInt("idPago"), // Columna 0: ID Pago (oculta)
                    cliente, // Columna 1: Cliente
                    montoStr, // Columna 2: Monto
                    rs.getDate("fecha_pago"), // Columna 3: Fecha
                    rs.getString("Estado_pago"),// Columna 4: Estado
                    estadoComprobante // Columna 5: Comprobante
                });
            }

            rs.close();
            ps.close();

            // OCULTAR LA COLUMNA DE ID
            ocultarColumnaID();

        } catch (Exception e) {
            e.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Error al cargar pagos: " + e.getMessage(),
                    "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

// Método para ocultar la columna del ID
    private void ocultarColumnaID() {
        javax.swing.table.TableColumn column = jTable1.getColumnModel().getColumn(0);
        jTable1.removeColumn(column);
    }

    private void subirComprobantePago(int idPago) {
        try {
            javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser();
            fileChooser.setDialogTitle("Seleccionar comprobante para Pago #" + idPago);
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "Imágenes y PDF", "jpg", "jpeg", "png", "pdf"));

            int resultado = fileChooser.showOpenDialog(this);

            if (resultado == javax.swing.JFileChooser.APPROVE_OPTION) {
                java.io.File archivo = fileChooser.getSelectedFile();

                // Verificar que el archivo exista
                if (!archivo.exists()) {
                    javax.swing.JOptionPane.showMessageDialog(this,
                            "El archivo seleccionado no existe",
                            "Error",
                            javax.swing.JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Verificar tamaño del archivo (máximo 10MB)
                long fileSize = archivo.length();
                if (fileSize > 10 * 1024 * 1024) {
                    javax.swing.JOptionPane.showMessageDialog(this,
                            "El archivo es demasiado grande. Máximo 10MB",
                            "Error",
                            javax.swing.JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Crear nombre único
                String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
                String extension = "";
                String nombreArchivoOriginal = archivo.getName();
                int i = nombreArchivoOriginal.lastIndexOf('.');
                if (i > 0 && i < nombreArchivoOriginal.length() - 1) {
                    extension = nombreArchivoOriginal.substring(i).toLowerCase();
                }

                // Validar extensión
                if (!extension.matches("(\\.jpg|\\.jpeg|\\.png|\\.pdf)")) {
                    javax.swing.JOptionPane.showMessageDialog(this,
                            "Formato de archivo no permitido. Use JPG, PNG o PDF",
                            "Error de Formato",
                            javax.swing.JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Nombre descriptivo simple
                String nuevoNombre = "pago" + idPago + "_" + timestamp + extension;

                // Carpeta destino para comprobantes
                java.io.File carpetaComprobantes = new java.io.File("comprobantes");
                if (!carpetaComprobantes.exists()) {
                    carpetaComprobantes.mkdir();
                }

                // Ruta completa del archivo destino
                java.io.File destino = new java.io.File(carpetaComprobantes, nuevoNombre);

                // Copiar archivo
                try {
                    java.nio.file.Files.copy(archivo.toPath(), destino.toPath(),
                            java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                } catch (java.io.IOException e) {
                    javax.swing.JOptionPane.showMessageDialog(this,
                            "Error al copiar el archivo: " + e.getMessage(),
                            "Error de Copia",
                            javax.swing.JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Actualizar base de datos
                String sqlUpdate = "UPDATE pago SET Comprobante = ? WHERE idPago = ?";
                java.sql.PreparedStatement psUpdate = conexion.conectar().prepareStatement(sqlUpdate);
                psUpdate.setString(1, nuevoNombre);
                psUpdate.setInt(2, idPago);
                int filasActualizadas = psUpdate.executeUpdate();
                psUpdate.close();

                if (filasActualizadas > 0) {
                    javax.swing.JOptionPane.showMessageDialog(this,
                            "Comprobante subido exitosamente\n\n"
                            + "Nombre: " + nuevoNombre,
                            "Listo",
                            javax.swing.JOptionPane.INFORMATION_MESSAGE);

                    // Recargar tabla inmediatamente
                    cargarPagosDesdeBD();
                }
            }

        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Error al subir comprobante: " + e.getMessage(),
                    "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void forzarActualizacionTabla() {
        cargarPagosDesdeBD();
    }

    private void asignarClienteAPago(int idPago) {
        try {
            // Obtener lista de usuarios
            String sqlUsuarios = "SELECT idUsuarios, Nombre FROM usuarios ORDER BY Nombre";
            java.sql.PreparedStatement psUsuarios = conexion.conectar().prepareStatement(sqlUsuarios);
            java.sql.ResultSet rsUsuarios = psUsuarios.executeQuery();

            java.util.Vector<String> usuarios = new java.util.Vector<>();
            java.util.HashMap<String, Integer> usuarioIds = new java.util.HashMap<>();

            while (rsUsuarios.next()) {
                String nombre = rsUsuarios.getString("Nombre");
                int id = rsUsuarios.getInt("idUsuarios");
                usuarios.add(nombre);
                usuarioIds.put(nombre, id);
            }
            rsUsuarios.close();
            psUsuarios.close();

            // Mostrar diálogo para seleccionar cliente
            String usuarioSeleccionado = (String) javax.swing.JOptionPane.showInputDialog(this,
                    "Selecciona el cliente para el pago #" + idPago + ":",
                    "Asignar Cliente",
                    javax.swing.JOptionPane.QUESTION_MESSAGE,
                    null,
                    usuarios.toArray(),
                    usuarios.firstElement());

            if (usuarioSeleccionado != null) {
                int idUsuario = usuarioIds.get(usuarioSeleccionado);

                // Actualizar el pago
                String sqlUpdate = "UPDATE pago SET idUsuarios = ? WHERE idPago = ?";
                java.sql.PreparedStatement psUpdate = conexion.conectar().prepareStatement(sqlUpdate);
                psUpdate.setInt(1, idUsuario);
                psUpdate.setInt(2, idPago);
                int filasActualizadas = psUpdate.executeUpdate();
                psUpdate.close();

                if (filasActualizadas > 0) {
                    javax.swing.JOptionPane.showMessageDialog(this,
                            "Cliente asignado correctamente al pago #" + idPago,
                            "Cliente Asignado",
                            javax.swing.JOptionPane.INFORMATION_MESSAGE);

                    // Recargar tabla
                    cargarPagosDesdeBD();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Error al asignar cliente: " + e.getMessage(),
                    "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
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
        jLabel11 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jComboBox1 = new javax.swing.JComboBox<>();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jButton4 = new javax.swing.JButton();
        jButton5 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        INS = new javax.swing.JLabel();
        FACE = new javax.swing.JLabel();
        WPP = new javax.swing.JLabel();
        jComboBox2 = new javax.swing.JComboBox<>();
        btnRegresar = new javax.swing.JButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu3 = new javax.swing.JMenu();
        jMenu12 = new javax.swing.JMenu();
        jMenuItem8 = new javax.swing.JMenuItem();
        jMenuItem9 = new javax.swing.JMenuItem();
        jMenuItem10 = new javax.swing.JMenuItem();
        jMenuItem11 = new javax.swing.JMenuItem();
        jMenu5 = new javax.swing.JMenu();
        jMenuItem7 = new javax.swing.JMenuItem();
        jMenu6 = new javax.swing.JMenu();
        jMenuItem4 = new javax.swing.JMenuItem();
        jMenu7 = new javax.swing.JMenu();
        jMenuItem5 = new javax.swing.JMenuItem();
        jMenu19 = new javax.swing.JMenu();
        jMenuItemCerrarSecion6 = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(243, 224, 255));

        jLabel11.setFont(new java.awt.Font("Serif", 3, 14)); // NOI18N
        jLabel11.setText("REVISION DE PAGOS");

        jLabel1.setText("Fecha");

        jComboBox1.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Fecha" }));
        jComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox1ActionPerformed(evt);
            }
        });

        jLabel2.setText("Nombre");

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(jTable1);

        jButton4.setBackground(new java.awt.Color(255, 204, 255));
        jButton4.setText("Ver");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        jButton5.setBackground(new java.awt.Color(255, 204, 255));
        jButton5.setText("Registrar");
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        jButton6.setBackground(new java.awt.Color(255, 204, 255));
        jButton6.setText("Eliminar");
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });

        jPanel5.setBackground(new java.awt.Color(204, 0, 204));

        INS.setText("INS");

        FACE.setText("FACE");

        WPP.setText("WPP");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap(117, Short.MAX_VALUE)
                .addComponent(INS, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(246, 246, 246)
                .addComponent(WPP, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(207, 207, 207)
                .addComponent(FACE, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(85, 85, 85))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(INS)
                    .addComponent(WPP)
                    .addComponent(FACE))
                .addContainerGap(25, Short.MAX_VALUE))
        );

        jComboBox2.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jComboBox2.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Nombre cliente" }));
        jComboBox2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox2ActionPerformed(evt);
            }
        });

        btnRegresar.setBackground(new java.awt.Color(255, 204, 255));
        btnRegresar.setText("Regresar");
        btnRegresar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRegresarActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(91, 91, 91)
                .addComponent(jLabel1)
                .addGap(18, 18, 18)
                .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(54, 54, 54)
                .addComponent(jLabel2)
                .addGap(18, 18, 18)
                .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(261, 261, 261)
                        .addComponent(jLabel11))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(33, 33, 33)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 566, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(45, 45, 45)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jButton5)
                                .addComponent(jButton6)
                                .addComponent(jButton4, javax.swing.GroupLayout.Alignment.TRAILING))
                            .addComponent(btnRegresar))))
                .addGap(0, 45, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(38, 38, 38)
                .addComponent(jLabel11)
                .addGap(43, 43, 43)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2)
                    .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(24, 24, 24)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(47, 47, 47)
                        .addComponent(jButton4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButton5)
                        .addGap(18, 18, 18)
                        .addComponent(jButton6)
                        .addGap(30, 30, 30)
                        .addComponent(btnRegresar))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 233, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 27, Short.MAX_VALUE)
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jMenu3.setText("LOGO");
        jMenuBar1.add(jMenu3);

        jMenu12.setText("CATALÓGO");

        jMenuItem8.setText("Uñas");
        jMenuItem8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem8ActionPerformed(evt);
            }
        });
        jMenu12.add(jMenuItem8);

        jMenuItem9.setText("Peinados");
        jMenuItem9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem9ActionPerformed(evt);
            }
        });
        jMenu12.add(jMenuItem9);

        jMenuItem10.setText("Maquillaje");
        jMenuItem10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem10ActionPerformed(evt);
            }
        });
        jMenu12.add(jMenuItem10);

        jMenuItem11.setText("Otros");
        jMenuItem11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem11ActionPerformed(evt);
            }
        });
        jMenu12.add(jMenuItem11);

        jMenuBar1.add(jMenu12);

        jMenu5.setText("AGENDAR CITA");

        jMenuItem7.setText("Agendar Cita");
        jMenuItem7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem7ActionPerformed(evt);
            }
        });
        jMenu5.add(jMenuItem7);

        jMenuBar1.add(jMenu5);

        jMenu6.setText("CONTACTO");

        jMenuItem4.setText("Contacto");
        jMenu6.add(jMenuItem4);

        jMenuBar1.add(jMenu6);

        jMenu7.setText("LOGIN");

        jMenuItem5.setText("Login");
        jMenu7.add(jMenuItem5);

        jMenuBar1.add(jMenu7);

        jMenu19.setText("CERRAR SESIÓN");

        jMenuItemCerrarSecion6.setText("Cerrar sesión");
        jMenuItemCerrarSecion6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemCerrarSecion6jMenuItemCerrarSecionActionPerformed(evt);
            }
        });
        jMenu19.add(jMenuItemCerrarSecion6);

        jMenuBar1.add(jMenu19);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        int selectedRow = jTable1.getSelectedRow();
        if (selectedRow == -1) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Por favor, selecciona un pago para ver los detalles",
                    "Selección requerida",
                    javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // El ID está en la columna 0 (oculta) pero accesible desde el modelo
            javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) jTable1.getModel();
            int idPago = Integer.parseInt(model.getValueAt(selectedRow, 0).toString());
            String clienteActual = model.getValueAt(selectedRow, 1).toString();

            // Consulta simple
            String sql = "SELECT p.*, "
                    + "CASE "
                    + "  WHEN COALESCE(u2.idUsuarios, u1.idUsuarios) IS NULL THEN 'Sin cliente' "
                    + "  WHEN c.idUsuarios IS NOT NULL THEN CONCAT(u2.Nombre, ' ', COALESCE(u2.Paterno, ''), ' ', COALESCE(u2.Materno, '')) "
                    + "  ELSE CONCAT(u1.Nombre, ' ', COALESCE(u1.Paterno, ''), ' ', COALESCE(u1.Materno, '')) "
                    + "END as Cliente "
                    + "FROM pago p "
                    + "LEFT JOIN cita c ON p.idPago = c.Pago_idPago "
                    + "LEFT JOIN usuarios u1 ON p.idUsuarios = u1.idUsuarios "
                    + "LEFT JOIN usuarios u2 ON c.idUsuarios = u2.idUsuarios "
                    + "WHERE p.idPago = ?";

            java.sql.PreparedStatement ps = conexion.conectar().prepareStatement(sql);
            ps.setInt(1, idPago);
            java.sql.ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String mensaje = "Detalles del Pago:\n\n"
                        + "ID Pago: " + rs.getInt("idPago") + "\n"
                        + "Cliente: " + rs.getString("Cliente") + "\n"
                        + "Monto: $" + String.format("%.2f", rs.getDouble("Monto")) + "\n"
                        + "Fecha Pago: " + rs.getDate("fecha_pago") + "\n"
                        + "Estado: " + rs.getString("Estado_pago") + "\n"
                        + "Comprobante: " + (rs.getString("Comprobante") != null ? rs.getString("Comprobante") : "Ninguno");

                // Si el cliente es "Sin cliente", ofrecer opción de asignar
                Object[] opciones;
                if (clienteActual.equals("Sin cliente")) {
                    opciones = new Object[]{"OK", "Subir Comprobante", "Asignar Cliente"};
                } else {
                    opciones = new Object[]{"OK", "Subir Comprobante"};
                }

                int eleccion = javax.swing.JOptionPane.showOptionDialog(this,
                        mensaje,
                        "Detalles del Pago - ID: " + idPago,
                        javax.swing.JOptionPane.DEFAULT_OPTION,
                        javax.swing.JOptionPane.INFORMATION_MESSAGE,
                        null,
                        opciones,
                        opciones[0]);

                if (eleccion == 1) { // Subir Comprobante
                    subirComprobantePago(idPago);
                } else if (eleccion == 2 && opciones.length > 2) { // Asignar Cliente
                    asignarClienteAPago(idPago);
                }
            } else {
                javax.swing.JOptionPane.showMessageDialog(this,
                        "No se encontraron detalles para el pago seleccionado",
                        "Error",
                        javax.swing.JOptionPane.ERROR_MESSAGE);
            }

            rs.close();
            ps.close();

        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Error al mostrar detalles: " + e.getMessage(),
                    "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
        }

    }//GEN-LAST:event_jButton4ActionPerformed

    private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1ActionPerformed
        // TODO add your handling code here:
        cargarPagosDesdeBD();

    }//GEN-LAST:event_jComboBox1ActionPerformed

    private void jComboBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox2ActionPerformed
        // TODO add your handling code here:

        cargarPagosDesdeBD();

    }//GEN-LAST:event_jComboBox2ActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        int selectedRow = jTable1.getSelectedRow();
        if (selectedRow == -1) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Por favor, selecciona un pago para cambiar su estado",
                    "Selección requerida",
                    javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) jTable1.getModel();
            int idPago = Integer.parseInt(model.getValueAt(selectedRow, 0).toString());
            String cliente = model.getValueAt(selectedRow, 1).toString();
            String monto = model.getValueAt(selectedRow, 2).toString();
            String estadoActual = model.getValueAt(selectedRow, 4).toString();

            // Opciones de estado
            String[] opciones = {"Completado", "Pendiente", "Cancelado"};
            String nuevoEstado = (String) javax.swing.JOptionPane.showInputDialog(this,
                    "Cambiar estado del pago:\n\n"
                    + "Cliente: " + cliente + "\n"
                    + "Monto: " + monto + "\n"
                    + "Estado actual: " + estadoActual + "\n\n"
                    + "Selecciona el nuevo estado:",
                    "Cambiar Estado de Pago",
                    javax.swing.JOptionPane.QUESTION_MESSAGE,
                    null,
                    opciones,
                    estadoActual);

            if (nuevoEstado != null && !nuevoEstado.equals(estadoActual)) {
                // Actualizar estado en la base de datos
                String sql = "UPDATE pago SET Estado_pago = ? WHERE idPago = ?";
                java.sql.PreparedStatement ps = conexion.conectar().prepareStatement(sql);
                ps.setString(1, nuevoEstado);
                ps.setInt(2, idPago);
                int filasAfectadas = ps.executeUpdate();
                ps.close();

                if (filasAfectadas > 0) {
                    javax.swing.JOptionPane.showMessageDialog(this,
                            "Estado del pago actualizado exitosamente:\n"
                            + "De '" + estadoActual + "' a '" + nuevoEstado + "'",
                            "Estado Actualizado",
                            javax.swing.JOptionPane.INFORMATION_MESSAGE);

                    // Recargar la tabla
                    cargarPagosDesdeBD();
                }
            }

        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Error al cambiar estado: " + e.getMessage(),
                    "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_jButton5ActionPerformed

    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        int selectedRow = jTable1.getSelectedRow();
        if (selectedRow == -1) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Por favor, selecciona un pago para eliminar",
                    "Selección requerida",
                    javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) jTable1.getModel();
            int idPago = Integer.parseInt(model.getValueAt(selectedRow, 0).toString());
            String cliente = model.getValueAt(selectedRow, 1).toString();
            String monto = model.getValueAt(selectedRow, 2).toString();

            Object[] opciones = {"Sí", "No"};
            int confirmacion = javax.swing.JOptionPane.showOptionDialog(this,
                    "¿Estás seguro de que quieres eliminar este pago?\n\n"
                    + "Cliente: " + cliente + "\n"
                    + "Monto: " + monto + "\n"
                    + "ID: " + idPago + "\n\n"
                    + "ADVERTENCIA: Esta acción también eliminará las referencias en citas y servicios.",
                    "Confirmar Eliminación",
                    javax.swing.JOptionPane.YES_NO_OPTION,
                    javax.swing.JOptionPane.WARNING_MESSAGE,
                    null,
                    opciones,
                    opciones[1]); // 

            if (confirmacion == javax.swing.JOptionPane.YES_OPTION) {
                Connection con = null;
                try {
                    con = conexion.conectar();
                    con.setAutoCommit(false); // Iniciar transacción

                    // Paso 1: Verificar si el pago está siendo usado en citas
                    String sqlCheckCitas = "SELECT COUNT(*) as count FROM cita WHERE Pago_idPago = ?";
                    PreparedStatement psCheckCitas = con.prepareStatement(sqlCheckCitas);
                    psCheckCitas.setInt(1, idPago);
                    ResultSet rsCitas = psCheckCitas.executeQuery();

                    int citasCount = 0;
                    if (rsCitas.next()) {
                        citasCount = rsCitas.getInt("count");
                    }
                    rsCitas.close();
                    psCheckCitas.close();

                    // Paso 2: Verificar si el pago está siendo usado en cita_has_servicios
                    String sqlCheckServicios = "SELECT COUNT(*) as count FROM cita_has_servicios WHERE Pago_idPago = ?";
                    PreparedStatement psCheckServicios = con.prepareStatement(sqlCheckServicios);
                    psCheckServicios.setInt(1, idPago);
                    ResultSet rsServicios = psCheckServicios.executeQuery();

                    int serviciosCount = 0;
                    if (rsServicios.next()) {
                        serviciosCount = rsServicios.getInt("count");
                    }
                    rsServicios.close();
                    psCheckServicios.close();

                    // Paso 3: Si hay referencias, preguntar qué hacer
                    if (citasCount > 0 || serviciosCount > 0) {
                        Object[] opcionesReferencias = {
                            "Eliminar todas las referencias y el pago",
                            "Desvincular referencias y mantener pago",
                            "Cancelar"
                        };

                        int eleccion = javax.swing.JOptionPane.showOptionDialog(this,
                                "Este pago tiene referencias en otras tablas:\n"
                                + "- Citas: " + citasCount + " referencia(s)\n"
                                + "- Servicios: " + serviciosCount + " referencia(s)\n\n"
                                + "¿Qué deseas hacer?",
                                "Pago con Referencias",
                                javax.swing.JOptionPane.DEFAULT_OPTION,
                                javax.swing.JOptionPane.WARNING_MESSAGE,
                                null,
                                opcionesReferencias,
                                opcionesReferencias[2]);

                        if (eleccion == 0) { // Eliminar todo
                            // Primero eliminar referencias en cita_has_servicios
                            if (serviciosCount > 0) {
                                String sqlDeleteServicios = "DELETE FROM cita_has_servicios WHERE Pago_idPago = ?";
                                PreparedStatement psDeleteServicios = con.prepareStatement(sqlDeleteServicios);
                                psDeleteServicios.setInt(1, idPago);
                                psDeleteServicios.executeUpdate();
                                psDeleteServicios.close();
                            }

                            // Luego actualizar citas para quitar referencia
                            if (citasCount > 0) {
                                String sqlUpdateCitas = "UPDATE cita SET Pago_idPago = NULL WHERE Pago_idPago = ?";
                                PreparedStatement psUpdateCitas = con.prepareStatement(sqlUpdateCitas);
                                psUpdateCitas.setInt(1, idPago);
                                psUpdateCitas.executeUpdate();
                                psUpdateCitas.close();
                            }

                            // Finalmente eliminar el pago
                            String sqlDeletePago = "DELETE FROM pago WHERE idPago = ?";
                            PreparedStatement psDeletePago = con.prepareStatement(sqlDeletePago);
                            psDeletePago.setInt(1, idPago);
                            int filasAfectadas = psDeletePago.executeUpdate();
                            psDeletePago.close();

                            if (filasAfectadas > 0) {
                                con.commit(); // Confirmar transacción
                                javax.swing.JOptionPane.showMessageDialog(this,
                                        "Pago y referencias eliminados exitosamente",
                                        "Eliminación Completa",
                                        javax.swing.JOptionPane.INFORMATION_MESSAGE);
                            }

                        } else if (eleccion == 1) { // Solo desvincular
                            // Desvincular de cita_has_servicios
                            if (serviciosCount > 0) {
                                String sqlUpdateServicios = "UPDATE cita_has_servicios SET Pago_idPago = NULL WHERE Pago_idPago = ?";
                                PreparedStatement psUpdateServicios = con.prepareStatement(sqlUpdateServicios);
                                psUpdateServicios.setInt(1, idPago);
                                psUpdateServicios.executeUpdate();
                                psUpdateServicios.close();
                            }

                            // Desvincular de citas
                            if (citasCount > 0) {
                                String sqlUpdateCitas = "UPDATE cita SET Pago_idPago = NULL WHERE Pago_idPago = ?";
                                PreparedStatement psUpdateCitas = con.prepareStatement(sqlUpdateCitas);
                                psUpdateCitas.setInt(1, idPago);
                                psUpdateCitas.executeUpdate();
                                psUpdateCitas.close();
                            }

                            con.commit(); // Confirmar transacción
                            javax.swing.JOptionPane.showMessageDialog(this,
                                    "Referencias desvinculadas exitosamente. El pago se mantiene.",
                                    "Desvinculación Exitosa",
                                    javax.swing.JOptionPane.INFORMATION_MESSAGE);

                        } else {
                            // Cancelar
                            con.rollback(); // Revertir transacción
                            return;
                        }

                    } else {
                        // No hay referencias, eliminar directamente
                        String sqlDeletePago = "DELETE FROM pago WHERE idPago = ?";
                        PreparedStatement psDeletePago = con.prepareStatement(sqlDeletePago);
                        psDeletePago.setInt(1, idPago);
                        int filasAfectadas = psDeletePago.executeUpdate();
                        psDeletePago.close();

                        if (filasAfectadas > 0) {
                            con.commit(); // Confirmar transacción
                            javax.swing.JOptionPane.showMessageDialog(this,
                                    "Pago eliminado exitosamente",
                                    "Eliminación Exitosa",
                                    javax.swing.JOptionPane.INFORMATION_MESSAGE);
                        }
                    }

                    // Recargar la tabla
                    cargarPagosDesdeBD();

                } catch (SQLException e) {
                    try {
                        if (con != null) {
                            con.rollback(); // Revertir en caso de error
                        }
                    } catch (SQLException rollbackEx) {
                        rollbackEx.printStackTrace();
                    }

                    javax.swing.JOptionPane.showMessageDialog(this,
                            "Error al eliminar pago: " + e.getMessage() + "\n"
                            + "El pago podría estar referenciado en otras tablas.",
                            "Error de Eliminación",
                            javax.swing.JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();

                } finally {
                    try {
                        if (con != null) {
                            con.setAutoCommit(true);
                            con.close();
                        }
                    } catch (SQLException closeEx) {
                        closeEx.printStackTrace();
                    }
                }
            }

        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Error: " + e.getMessage(),
                    "Error General",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }//GEN-LAST:event_jButton6ActionPerformed

    private void jMenuItem7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem7ActionPerformed
        // TODO add your handling code here:
        //agendar cita
        NewJAgendarcita NewJAgendarcita = new NewJAgendarcita();
        NewJAgendarcita.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem7ActionPerformed

    private void jMenuItem8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem8ActionPerformed
        // TODO add your handling code here:
        NewJCatalogoUñas NewJCatalogoUñas = new NewJCatalogoUñas();
        NewJCatalogoUñas.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem8ActionPerformed

    private void jMenuItem9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem9ActionPerformed
        // TODO add your handling code here:
        NewJCatalogoPeinado NewJCatalogoPeinado = new NewJCatalogoPeinado();
        NewJCatalogoPeinado.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem9ActionPerformed

    private void jMenuItem10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem10ActionPerformed
        // TODO add your handling code here:
        NewJCatalogoMaq NewJCatalogoMaq = new NewJCatalogoMaq();
        NewJCatalogoMaq.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem10ActionPerformed

    private void jMenuItem11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem11ActionPerformed
        // TODO add your handling code here:
        ConexionBD conexionCatalogo = new ConexionBD("andynails");
        NewJCatalogoGenerico catalogo = new NewJCatalogoGenerico(conexionCatalogo);
        catalogo.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem11ActionPerformed

    private void btnRegresarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRegresarActionPerformed
        // TODO add your handling code here:
        // Regresar al panel anterior
        NewJPanelAdministracion anterior = new NewJPanelAdministracion();
        anterior.setVisible(true);
        this.dispose(); // Cierra la ventana actual
    }//GEN-LAST:event_btnRegresarActionPerformed

    private void jMenuItemCerrarSecion6jMenuItemCerrarSecionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemCerrarSecion6jMenuItemCerrarSecionActionPerformed
        // TODO add your handling code here:
        andynails.SessionManager.cerrarSesion(this);
    }//GEN-LAST:event_jMenuItemCerrarSecion6jMenuItemCerrarSecionActionPerformed

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
            java.util.logging.Logger.getLogger(NewJRevPag.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(NewJRevPag.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(NewJRevPag.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(NewJRevPag.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new NewJRevPag().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel FACE;
    private javax.swing.JLabel INS;
    private javax.swing.JLabel WPP;
    private javax.swing.JButton btnRegresar;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JComboBox<String> jComboBox2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JMenu jMenu12;
    private javax.swing.JMenu jMenu19;
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
    private javax.swing.JMenuItem jMenuItemCerrarSecion6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
}
