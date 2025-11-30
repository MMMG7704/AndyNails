/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package Interfaces;

import andynails.ConexionBD;
import andynails.RedesSociales;

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

        // Primero configurar los ComboBox
        configurarComboBox();
        verificarEstructuraTablaPago();

        // Luego cargar los pagos (después de un pequeño delay para asegurar que los ComboBox están listos)
        javax.swing.Timer timer = new javax.swing.Timer(100, new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                cargarPagosDesdeBD();
            }
        });
        timer.setRepeats(false);
        timer.start();

        // Configurar tooltips
        jButton4.setToolTipText("Ver detalles del pago seleccionado");
        jButton5.setToolTipText("Registrar nuevo pago");
        jButton6.setToolTipText("Eliminar pago seleccionado");
        jComboBox1.setToolTipText("Filtrar por fecha");
        jComboBox2.setToolTipText("Filtrar por cliente");
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

            // Usar fecha_pago en lugar de Fecha_pago
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

            // Configurar ComboBox de clientes
            jComboBox2.removeAllItems();
            jComboBox2.addItem("Todos los clientes");

            String sqlClientes = "SELECT DISTINCT u.idUsuarios, u.Nombre FROM pago p "
                    + "INNER JOIN usuarios u ON p.idUsuarios = u.idUsuarios "
                    + "ORDER BY u.Nombre";
            java.sql.PreparedStatement psClientes = conexion.conectar().prepareStatement(sqlClientes);
            java.sql.ResultSet rsClientes = psClientes.executeQuery();

            while (rsClientes.next()) {
                String nombre = rsClientes.getString("Nombre");
                if (nombre != null) {
                    jComboBox2.addItem(nombre);
                }
            }
            rsClientes.close();
            psClientes.close();

            // Establecer selecciones por defecto después de cargar todos los items
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    jComboBox1.setSelectedItem("Todas las fechas");
                    jComboBox2.setSelectedItem("Todos los clientes");
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Error al cargar filtros: " + e.getMessage(),
                    "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    private void verificarEstructuraTablaPago() {
        try {
            String sql = "DESCRIBE pago";
            java.sql.PreparedStatement ps = conexion.conectar().prepareStatement(sql);
            java.sql.ResultSet rs = ps.executeQuery();

            System.out.println("=== ESTRUCTURA TABLA PAGO ===");
            while (rs.next()) {
                System.out.println(rs.getString("Field") + " - " + rs.getString("Type"));
            }
            rs.close();
            ps.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cargarPagosDesdeBD() {
        try {
            // Verificar que los ComboBox tengan elementos seleccionados
            String fechaSeleccionada = (jComboBox1.getSelectedItem() != null)
                    ? jComboBox1.getSelectedItem().toString() : "Todas las fechas";
            String clienteSeleccionado = (jComboBox2.getSelectedItem() != null)
                    ? jComboBox2.getSelectedItem().toString() : "Todos los clientes";

            // Versión simplificada sin metodo_pago
            String sql = "SELECT p.idPago, u.Nombre as Cliente, p.Monto, p.fecha_pago, "
                    + "p.Estado_pago, p.Comprobante, "
                    + "GROUP_CONCAT(s.Nombre_servicio SEPARATOR ', ') as Servicios "
                    + "FROM pago p "
                    + "INNER JOIN usuarios u ON p.idUsuarios = u.idUsuarios "
                    + "LEFT JOIN cita_has_servicios chs ON p.idPago = chs.Pago_idPago "
                    + "LEFT JOIN servicios s ON chs.idServicios = s.idServicios "
                    + "WHERE 1=1";

            // Agregar filtros si se seleccionaron
            if (!fechaSeleccionada.equals("Todas las fechas")) {
                sql += " AND DATE(p.fecha_pago) = ?";
            }
            if (!clienteSeleccionado.equals("Todos los clientes")) {
                sql += " AND u.Nombre = ?";
            }

            sql += " GROUP BY p.idPago, u.Nombre, p.Monto, p.fecha_pago, p.Estado_pago, p.Comprobante "
                    + "ORDER BY p.fecha_pago DESC, p.idPago DESC";

            java.sql.PreparedStatement ps = conexion.conectar().prepareStatement(sql);

            int paramIndex = 1;
            if (!fechaSeleccionada.equals("Todas las fechas")) {
                ps.setString(paramIndex++, fechaSeleccionada);
            }
            if (!clienteSeleccionado.equals("Todos los clientes")) {
                ps.setString(paramIndex++, clienteSeleccionado);
            }

            java.sql.ResultSet rs = ps.executeQuery();

            javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) jTable1.getModel();
            model.setRowCount(0);

            // Columnas simplificadas
            model.setColumnIdentifiers(new String[]{
                "ID Pago",
                "Cliente",
                "Monto",
                "Fecha",
                "Estado",
                "Servicios",
                "Comprobante"
            });

            while (rs.next()) {
                String comprobante = rs.getString("Comprobante");
                String nombreComprobante = "Sin comprobante";
                if (comprobante != null && !comprobante.trim().isEmpty()) {
                    // Extraer nombre legible del comprobante
                    String[] partes = comprobante.split("_");
                    if (partes.length > 3) {
                        nombreComprobante = partes[partes.length - 1];
                        // Limpiar extensión si es muy larga
                        if (nombreComprobante.length() > 20) {
                            nombreComprobante = nombreComprobante.substring(0, 17) + "...";
                        }
                    } else {
                        nombreComprobante = comprobante;
                    }
                }

                model.addRow(new Object[]{
                    rs.getInt("idPago"),
                    rs.getString("Cliente"),
                    "$" + String.format("%.2f", rs.getDouble("Monto")),
                    rs.getDate("fecha_pago"),
                    rs.getString("Estado_pago"),
                    rs.getString("Servicios"),
                    nombreComprobante
                });
            }

            rs.close();
            ps.close();

        } catch (Exception e) {
            e.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Error al cargar pagos: " + e.getMessage(),
                    "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    private void verificarEstructuraTablaCitaHasServicios() {
        try {
            String sql = "DESCRIBE cita_has_servicios";
            java.sql.PreparedStatement ps = conexion.conectar().prepareStatement(sql);
            java.sql.ResultSet rs = ps.executeQuery();

            System.out.println("=== ESTRUCTURA TABLA cita_has_servicios ===");
            while (rs.next()) {
                System.out.println(rs.getString("Field") + " - " + rs.getString("Type") + " - " + rs.getString("Null"));
            }
            rs.close();
            ps.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void subirComprobantePago(int idPago) {
        try {
            javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser();
            fileChooser.setDialogTitle("Seleccionar comprobante de pago");
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "Imágenes y PDF", "jpg", "jpeg", "png", "pdf"));

            int resultado = fileChooser.showOpenDialog(this);

            if (resultado == javax.swing.JFileChooser.APPROVE_OPTION) {
                java.io.File archivo = fileChooser.getSelectedFile();

                // Obtener información del cliente para el nombre del archivo
                String sqlCliente = "SELECT u.Nombre FROM pago p "
                        + "INNER JOIN usuarios u ON p.idUsuarios = u.idUsuarios "
                        + "WHERE p.idPago = ?";
                java.sql.PreparedStatement psCliente = conexion.conectar().prepareStatement(sqlCliente);
                psCliente.setInt(1, idPago);
                java.sql.ResultSet rsCliente = psCliente.executeQuery();

                String nombreCliente = "cliente";
                if (rsCliente.next()) {
                    nombreCliente = rsCliente.getString("Nombre");
                }
                rsCliente.close();
                psCliente.close();

                // Crear nombre único para el archivo
                String timestamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
                String extension = "";
                String nombreArchivo = fileChooser.getSelectedFile().getName();
                int i = nombreArchivo.lastIndexOf('.');
                if (i > 0) {
                    extension = nombreArchivo.substring(i);
                }

                String nuevoNombre = nombreCliente + "_" + timestamp + "_comprobante" + extension;

                // Carpeta destino para comprobantes
                java.io.File carpetaComprobantes = new java.io.File("comprobantes");
                if (!carpetaComprobantes.exists()) {
                    carpetaComprobantes.mkdir();
                }

                java.io.File destino = new java.io.File(carpetaComprobantes, nuevoNombre);

                // Copiar archivo
                java.nio.file.Files.copy(archivo.toPath(), destino.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                // Actualizar base de datos
                String sqlUpdate = "UPDATE pago SET Comprobante = ? WHERE idPago = ?";
                java.sql.PreparedStatement psUpdate = conexion.conectar().prepareStatement(sqlUpdate);
                psUpdate.setString(1, nuevoNombre);
                psUpdate.setInt(2, idPago);
                psUpdate.executeUpdate();
                psUpdate.close();

                javax.swing.JOptionPane.showMessageDialog(this,
                        " Comprobante subido exitosamente: " + nuevoNombre,
                        "Comprobante Guardado",
                        javax.swing.JOptionPane.INFORMATION_MESSAGE);

                // Recargar tabla
                cargarPagosDesdeBD();
            }

        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Error al subir comprobante: " + e.getMessage(),
                    "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void agregarServicioAPago(int idPago) {
        try {
            // PRIMERO: Verificar si existe una cita asociada a este pago
            String sqlCita = "SELECT c.idCita FROM cita c WHERE c.Pago_idPago = ?";
            java.sql.PreparedStatement psCita = conexion.conectar().prepareStatement(sqlCita);
            psCita.setInt(1, idPago);
            java.sql.ResultSet rsCita = psCita.executeQuery();

            Integer idCita = null;
            if (rsCita.next()) {
                idCita = rsCita.getInt("idCita");
            }
            rsCita.close();
            psCita.close();

            // SI NO HAY CITA ASOCIADA, preguntar si crear una
            if (idCita == null) {
                int crearCita = javax.swing.JOptionPane.showConfirmDialog(this,
                        "No hay una cita asociada a este pago.\n"
                        + "¿Deseas crear una nueva cita para asociar el servicio?",
                        "Crear Cita",
                        javax.swing.JOptionPane.YES_NO_OPTION);

                if (crearCita == javax.swing.JOptionPane.YES_OPTION) {
                    idCita = crearCitaParaPago(idPago);

                    // Si el usuario canceló la creación de la cita, salir
                    if (idCita == null) {
                        return;
                    }
                } else {
                    javax.swing.JOptionPane.showMessageDialog(this,
                            "No se puede agregar servicio sin una cita asociada",
                            "Operación Cancelada",
                            javax.swing.JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }

            // Obtener lista de servicios disponibles
            String sqlServicios = "SELECT idServicios, Nombre_servicio, Precio FROM servicios ORDER BY Nombre_servicio";
            java.sql.PreparedStatement psServicios = conexion.conectar().prepareStatement(sqlServicios);
            java.sql.ResultSet rsServicios = psServicios.executeQuery();

            java.util.Vector<String> servicios = new java.util.Vector<>();
            java.util.HashMap<String, Integer> servicioIds = new java.util.HashMap<>();
            java.util.HashMap<String, Double> servicioPrecios = new java.util.HashMap<>();

            while (rsServicios.next()) {
                String nombre = rsServicios.getString("Nombre_servicio");
                int id = rsServicios.getInt("idServicios");
                double precio = rsServicios.getDouble("Precio");

                servicios.add(nombre + " - $" + precio);
                servicioIds.put(nombre + " - $" + precio, id);
                servicioPrecios.put(nombre + " - $" + precio, precio);
            }
            rsServicios.close();
            psServicios.close();

            if (servicios.isEmpty()) {
                javax.swing.JOptionPane.showMessageDialog(this,
                        "No hay servicios disponibles",
                        "Error",
                        javax.swing.JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Mostrar diálogo para seleccionar servicio
            String servicioSeleccionado = (String) javax.swing.JOptionPane.showInputDialog(this,
                    "Selecciona el servicio a agregar:",
                    "Agregar Servicio al Pago - Cita #" + idCita,
                    javax.swing.JOptionPane.QUESTION_MESSAGE,
                    null,
                    servicios.toArray(),
                    servicios.firstElement());

            if (servicioSeleccionado != null) {
                int idServicio = servicioIds.get(servicioSeleccionado);
                double precio = servicioPrecios.get(servicioSeleccionado);

                // CONFIRMAR antes de agregar el servicio
                int confirmacion = javax.swing.JOptionPane.showConfirmDialog(this,
                        "¿Estás seguro de que quieres agregar este servicio?\n\n"
                        + "Servicio: " + servicioSeleccionado + "\n"
                        + "Cita: #" + idCita + "\n"
                        + "Pago: #" + idPago,
                        "Confirmar Agregar Servicio",
                        javax.swing.JOptionPane.YES_NO_OPTION);

                if (confirmacion == javax.swing.JOptionPane.YES_OPTION) {
                    // Insertar en la tabla cita_has_servicios CON idCita
                    String sqlInsert = "INSERT INTO cita_has_servicios (idCita, idServicios, Pago_idPago) VALUES (?, ?, ?)";
                    java.sql.PreparedStatement psInsert = conexion.conectar().prepareStatement(sqlInsert);
                    psInsert.setInt(1, idCita);
                    psInsert.setInt(2, idServicio);
                    psInsert.setInt(3, idPago);
                    psInsert.executeUpdate();
                    psInsert.close();

                    // Actualizar monto total del pago
                    String sqlUpdateMonto = "UPDATE pago SET Monto = Monto + ? WHERE idPago = ?";
                    java.sql.PreparedStatement psUpdateMonto = conexion.conectar().prepareStatement(sqlUpdateMonto);
                    psUpdateMonto.setDouble(1, precio);
                    psUpdateMonto.setInt(2, idPago);
                    psUpdateMonto.executeUpdate();
                    psUpdateMonto.close();

                    javax.swing.JOptionPane.showMessageDialog(this,
                            "✅ Servicio agregado exitosamente\n"
                            + "Servicio: " + servicioSeleccionado + "\n"
                            + "Cita asociada: #" + idCita + "\n"
                            + "Monto actualizado: +$" + precio,
                            "Servicio Agregado",
                            javax.swing.JOptionPane.INFORMATION_MESSAGE);

                    // Recargar tabla
                    cargarPagosDesdeBD();
                } else {
                    javax.swing.JOptionPane.showMessageDialog(this,
                            "Operación cancelada por el usuario",
                            "Cancelado",
                            javax.swing.JOptionPane.INFORMATION_MESSAGE);
                }
            }

        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Error al agregar servicio: " + e.getMessage(),
                    "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private Integer crearCitaParaPago(int idPago) {
        try {
            // Obtener información del cliente desde el pago
            String sqlInfo = "SELECT p.idUsuarios, u.Nombre FROM pago p "
                    + "INNER JOIN usuarios u ON p.idUsuarios = u.idUsuarios "
                    + "WHERE p.idPago = ?";
            java.sql.PreparedStatement psInfo = conexion.conectar().prepareStatement(sqlInfo);
            psInfo.setInt(1, idPago);
            java.sql.ResultSet rsInfo = psInfo.executeQuery();

            if (!rsInfo.next()) {
                throw new Exception("No se pudo obtener información del cliente");
            }

            int idUsuario = rsInfo.getInt("idUsuarios");
            String nombreCliente = rsInfo.getString("Nombre");
            rsInfo.close();
            psInfo.close();

            // Preguntar por la fecha de la cita
            String fechaInput = javax.swing.JOptionPane.showInputDialog(this,
                    "Ingresa la fecha para la nueva cita (YYYY-MM-DD):\n"
                    + "Cliente: " + nombreCliente,
                    "Nueva Cita",
                    javax.swing.JOptionPane.QUESTION_MESSAGE);

            if (fechaInput == null || fechaInput.trim().isEmpty()) {
                // Usar fecha por defecto (mañana) si no se ingresa nada
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.add(java.util.Calendar.DATE, 1);
                fechaInput = new java.text.SimpleDateFormat("yyyy-MM-dd").format(cal.getTime());
            }

            // Validar formato de fecha
            java.sql.Date fechaCita;
            try {
                fechaCita = java.sql.Date.valueOf(fechaInput);
            } catch (IllegalArgumentException e) {
                javax.swing.JOptionPane.showMessageDialog(this,
                        "Formato de fecha inválido. Use YYYY-MM-DD",
                        "Error de Formato",
                        javax.swing.JOptionPane.ERROR_MESSAGE);
                return null;
            }

            // CONFIRMAR creación de la cita
            int confirmacion = javax.swing.JOptionPane.showConfirmDialog(this,
                    "¿Confirmar creación de cita?\n\n"
                    + "Cliente: " + nombreCliente + "\n"
                    + "Fecha: " + fechaCita + "\n"
                    + "Hora: 10:00 AM\n"
                    + "Estado: Confirmada",
                    "Confirmar Creación de Cita",
                    javax.swing.JOptionPane.YES_NO_OPTION);

            if (confirmacion == javax.swing.JOptionPane.YES_OPTION) {
                String sqlInsertCita = "INSERT INTO cita (idUsuarios, Fecha, Hora, Estado, Pago_idPago) VALUES (?, ?, '10:00:00', 'Confirmada', ?)";
                java.sql.PreparedStatement psInsertCita = conexion.conectar().prepareStatement(sqlInsertCita, java.sql.Statement.RETURN_GENERATED_KEYS);
                psInsertCita.setInt(1, idUsuario);
                psInsertCita.setDate(2, fechaCita);
                psInsertCita.setInt(3, idPago);
                psInsertCita.executeUpdate();

                // Obtener el ID de la cita creada
                java.sql.ResultSet generatedKeys = psInsertCita.getGeneratedKeys();
                int idCita = -1;
                if (generatedKeys.next()) {
                    idCita = generatedKeys.getInt(1);
                }
                generatedKeys.close();
                psInsertCita.close();

                javax.swing.JOptionPane.showMessageDialog(this,
                        " Cita creada exitosamente\n"
                        + "Cliente: " + nombreCliente + "\n"
                        + "Fecha: " + fechaCita + "\n"
                        + "Hora: 10:00 AM\n"
                        + "ID Cita: #" + idCita,
                        "Cita Creada",
                        javax.swing.JOptionPane.INFORMATION_MESSAGE);

                return idCita;
            } else {
                javax.swing.JOptionPane.showMessageDialog(this,
                        "Creación de cita cancelada",
                        "Cancelado",
                        javax.swing.JOptionPane.INFORMATION_MESSAGE);
                return null;
            }

        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Error al crear cita: " + e.getMessage(),
                    "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return null;
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
        jButton7 = new javax.swing.JButton();
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
        jMenu16 = new javax.swing.JMenu();
        jMenuItemCerrarSecion = new javax.swing.JMenuItem();

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
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(INS, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(123, 123, 123)
                .addComponent(WPP, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(148, 148, 148)
                .addComponent(FACE, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(267, 267, 267))
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

        jButton7.setBackground(new java.awt.Color(255, 204, 255));
        jButton7.setText("Subir Comprobante");
        jButton7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton7ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(261, 261, 261)
                .addComponent(jLabel11)
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                        .addGap(91, 91, 91)
                        .addComponent(jLabel1)
                        .addGap(18, 18, 18)
                        .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(54, 54, 54)
                        .addComponent(jLabel2)
                        .addGap(18, 18, 18)
                        .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                        .addGap(33, 33, 33)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 566, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(23, 23, 23)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jButton7)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jButton4)
                                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(jButton5)
                                        .addComponent(jButton6)))
                                .addGap(19, 19, 19))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addComponent(btnRegresar)
                                .addGap(14, 14, 14)))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(38, 38, 38)
                .addComponent(jLabel11)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jButton4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButton5)
                        .addGap(18, 18, 18)
                        .addComponent(jButton6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButton7)
                        .addGap(18, 18, 18)
                        .addComponent(btnRegresar)
                        .addGap(38, 38, 38))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(43, 43, 43)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel1)
                            .addComponent(jLabel2)
                            .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(24, 24, 24)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 233, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 27, Short.MAX_VALUE)))
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
            int idPago = Integer.parseInt(jTable1.getValueAt(selectedRow, 0).toString());

            // Consulta para obtener todos los detalles del pago
            String sql = "SELECT p.*, u.Nombre as Cliente, u.Telefono, "
                    + "GROUP_CONCAT(DISTINCT s.Nombre_servicio SEPARATOR ', ') as Servicios, "
                    + "GROUP_CONCAT(DISTINCT c.Fecha) as Fechas_Cita "
                    + "FROM pago p "
                    + "INNER JOIN usuarios u ON p.idUsuarios = u.idUsuarios "
                    + "LEFT JOIN cita_has_servicios chs ON p.idPago = chs.Pago_idPago "
                    + "LEFT JOIN servicios s ON chs.idServicios = s.idServicios "
                    + "LEFT JOIN cita c ON chs.idCita = c.idCita "
                    + "WHERE p.idPago = ? "
                    + "GROUP BY p.idPago";

            java.sql.PreparedStatement ps = conexion.conectar().prepareStatement(sql);
            ps.setInt(1, idPago);
            java.sql.ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String mensaje = "📋 DETALLES COMPLETOS DEL PAGO\n\n"
                        + "🔹 ID Pago: " + rs.getInt("idPago") + "\n"
                        + "👤 Cliente: " + rs.getString("Cliente") + "\n"
                        + "📞 Teléfono: " + (rs.getString("Telefono") != null ? rs.getString("Telefono") : "N/A") + "\n"
                        + "💰 Monto: $" + String.format("%.2f", rs.getDouble("Monto")) + "\n"
                        + "📅 Fecha Pago: " + rs.getDate("fecha_pago") + "\n"
                        + "📊 Estado: " + rs.getString("Estado_pago") + "\n"
                        + "🛍️ Servicios: " + (rs.getString("Servicios") != null ? rs.getString("Servicios") : "N/A") + "\n"
                        + "📅 Fechas Cita: " + (rs.getString("Fechas_Cita") != null ? rs.getString("Fechas_Cita") : "N/A") + "\n"
                        + "📎 Comprobante: " + (rs.getString("Comprobante") != null ? rs.getString("Comprobante") : "N/A") + "\n"
                        + "🏦 Banco: " + (rs.getString("Banco") != null ? rs.getString("Banco") : "N/A") + "\n"
                        + "📝 Concepto: " + (rs.getString("Concepto") != null ? rs.getString("Concepto") : "N/A");

                // AGREGAR OPCIONES ADICIONALES
                Object[] opciones = {"OK", " Subir Comprobante", " Agregar Servicio"};
                int eleccion = javax.swing.JOptionPane.showOptionDialog(this,
                        mensaje,
                        "Detalles Completos del Pago - ID: " + idPago,
                        javax.swing.JOptionPane.DEFAULT_OPTION,
                        javax.swing.JOptionPane.INFORMATION_MESSAGE,
                        null,
                        opciones,
                        opciones[0]);

                if (eleccion == 1) { // Subir Comprobante
                    subirComprobantePago(idPago);
                } else if (eleccion == 2) { // Agregar Servicio
                    agregarServicioAPago(idPago);
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
        if (jComboBox1.getSelectedItem() != null) {
            cargarPagosDesdeBD();
        }
    }//GEN-LAST:event_jComboBox1ActionPerformed

    private void jComboBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox2ActionPerformed
        // TODO add your handling code here:
        if (jComboBox2.getSelectedItem() != null) {
            cargarPagosDesdeBD();
        }
    }//GEN-LAST:event_jComboBox2ActionPerformed

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        // TODO add your handling code here:
        // Botón "Registrar" - Cambiar estado del pago
        int selectedRow = jTable1.getSelectedRow();
        if (selectedRow == -1) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Por favor, selecciona un pago para cambiar su estado",
                    "Selección requerida",
                    javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int idPago = Integer.parseInt(jTable1.getValueAt(selectedRow, 0).toString());
            String cliente = jTable1.getValueAt(selectedRow, 1).toString();
            String monto = jTable1.getValueAt(selectedRow, 2).toString();
            String estadoActual = jTable1.getValueAt(selectedRow, 4).toString();

            // Opciones de estado
            String[] opciones = {"Completado", "Pendiente", "Cancelado", "Reembolsado"};
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
                            " Estado del pago actualizado exitosamente:\n"
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
        // TODO add your handling code here:
        int selectedRow = jTable1.getSelectedRow();
        if (selectedRow == -1) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Por favor, selecciona un pago para eliminar",
                    "Selección requerida",
                    javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int idPago = Integer.parseInt(jTable1.getValueAt(selectedRow, 0).toString());
            String cliente = jTable1.getValueAt(selectedRow, 1).toString();
            String monto = jTable1.getValueAt(selectedRow, 2).toString();

            int confirmacion = javax.swing.JOptionPane.showConfirmDialog(this,
                    "¿Estás seguro de que quieres eliminar este pago?\n\n"
                    + "Cliente: " + cliente + "\n"
                    + "Monto: " + monto + "\n"
                    + "ID: " + idPago,
                    "Confirmar Eliminación",
                    javax.swing.JOptionPane.YES_NO_OPTION);

            if (confirmacion == javax.swing.JOptionPane.YES_OPTION) {
                // Eliminar de la base de datos
                String sql = "DELETE FROM pago WHERE idPago = ?";
                java.sql.PreparedStatement ps = conexion.conectar().prepareStatement(sql);
                ps.setInt(1, idPago);
                int filasAfectadas = ps.executeUpdate();
                ps.close();

                if (filasAfectadas > 0) {
                    javax.swing.JOptionPane.showMessageDialog(this,
                            "Pago eliminado exitosamente",
                            "Eliminación Exitosa",
                            javax.swing.JOptionPane.INFORMATION_MESSAGE);

                    // Recargar la tabla
                    cargarPagosDesdeBD();
                } else {
                    javax.swing.JOptionPane.showMessageDialog(this,
                            "No se pudo eliminar el pago",
                            "Error",
                            javax.swing.JOptionPane.ERROR_MESSAGE);
                }
            }

        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Error al eliminar pago: " + e.getMessage(),
                    "Error",
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

    private void jMenuItemCerrarSecionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemCerrarSecionActionPerformed
        // TODO add your handling code here:
        andynails.SessionManager.cerrarSesion(this);
    }//GEN-LAST:event_jMenuItemCerrarSecionActionPerformed

    private void jButton7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton7ActionPerformed
        // TODO add your handling code here:
        int selectedRow = jTable1.getSelectedRow();
        if (selectedRow == -1) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Por favor, selecciona un pago para subir comprobante",
                    "Selección requerida",
                    javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }

        int idPago = Integer.parseInt(jTable1.getValueAt(selectedRow, 0).toString());
        subirComprobantePago(idPago);
    }//GEN-LAST:event_jButton7ActionPerformed

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
    private javax.swing.JButton jButton7;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JComboBox<String> jComboBox2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
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
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
}
