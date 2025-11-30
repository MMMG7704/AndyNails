/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package Interfaces;

import andynails.ConexionBD;
import andynails.RedesSociales;
import javax.swing.JFrame;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.JOptionPane;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.sql.Statement;
import java.sql.Time;
import javax.swing.DefaultComboBoxModel;

/**
 *
 * @author User
 */
public class NewJAgendarcitaREC extends javax.swing.JFrame {

    ConexionBD conexion;
    private int idCita = -1;
    private String categoriaSeleccionada;

    public NewJAgendarcitaREC(int idCita) {
        initComponents();
        setLocationRelativeTo(null);

        conexion = new ConexionBD();
        this.idCita = idCita;

        // Cargar catálogos
        cargarClientes();
        cargarServicios();
        cargarHoras();

        // Cargar datos desde BD
        cargarDatosCita(idCita);

        // === IMPORTANTE ===
        // Ahora sí se carga y bloquea el ID
        txtnumerocita.setText(String.valueOf(idCita));
        txtnumerocita.setEditable(false);

        // Bloquear servicio para edición
        jComboBox1servicios.setEnabled(false);
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

    private JFrame ventanaAnterior;

    public NewJAgendarcitaREC(JFrame ventanaAnterior) {
        initComponents();
        this.ventanaAnterior = ventanaAnterior;

        setLocationRelativeTo(null);
        conexion = new ConexionBD();

        // Cargar datos
        cargarClientes();
        cargarServicios();
        cargarHoras();
        generarNumeroCitaAutomatico();
    }

    public NewJAgendarcitaREC() {
        initComponents();
        setLocationRelativeTo(null);

        conexion = new ConexionBD();

        cargarClientes();
        cargarServicios();
        cargarHoras();
        generarNumeroCitaAutomatico();

        // Campos activos para cita nueva
        CalCitas.setEnabled(true);
        jComboBox3hora.setEnabled(true);

    }

    private void cargarClientes() {
        try (Connection con = conexion.conectar()) {
            if (con == null) {
                JOptionPane.showMessageDialog(this, "No se pudo conectar a la base de datos.");
                return;
            }

            String sql = "SELECT idUsuarios, CONCAT(Nombre, ' ', Paterno, ' ', Materno) AS NombreCompleto "
                    + "FROM Usuarios ORDER BY Nombre";

            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            model.addElement("Seleccione un cliente");

            while (rs.next()) {
                model.addElement(rs.getString("NombreCompleto"));
            }

            jComboBoxnombrecliente.setModel(model);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar clientes: " + e.getMessage());
        }
    }

    // Método para cargar servicios desde la base de datos
    private void cargarServicios() {
        try (Connection con = conexion.conectar()) {
            if (con == null) {
                JOptionPane.showMessageDialog(this, "No se pudo conectar a la base de datos.");
                return;
            }

            String sql = "SELECT idServicios, Nombre_servicio FROM Servicios ORDER BY Nombre_servicio";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            model.addElement("Seleccione un servicio");

            while (rs.next()) {
                model.addElement(rs.getString("Nombre_servicio"));
            }

            jComboBox1servicios.setModel(model);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar servicios: " + e.getMessage());
        }
    }

    private boolean fechaHoraServicioDisponible(Date fecha, String horaStr, String servicio, int idCitaExcluir) {
        try (Connection con = conexion.conectar()) {
            if (con == null) {
                return false;
            }

            java.sql.Date fechaSQL = new java.sql.Date(fecha.getTime());
            int idCliente = obtenerIdUsuario(jComboBoxnombrecliente.getSelectedItem().toString());

            // 1. Verificar si OTRO cliente ya tiene ese servicio a esa hora
            String sqlServicio = "SELECT c.idCita FROM Cita c "
                    + "JOIN cita_has_servicios cs ON c.idCita = cs.idCita "
                    + "JOIN Servicios s ON cs.idServicios = s.idServicios "
                    + "WHERE c.Fecha = ? AND c.Hora = ? AND s.Nombre_servicio = ?";

            if (idCitaExcluir > 0) {
                sqlServicio += " AND c.idCita <> ?";
            }

            try (PreparedStatement ps = con.prepareStatement(sqlServicio)) {
                ps.setDate(1, fechaSQL);
                ps.setString(2, horaStr);
                ps.setString(3, servicio);
                if (idCitaExcluir > 0) {
                    ps.setInt(4, idCitaExcluir);
                }

                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    return false; // otro cliente tiene ese servicio en esa hora
                }
            }

            // 2. Verificar si el MISMO cliente tiene ya cita a esa hora
            String sqlCliente = "SELECT idCita FROM Cita WHERE Fecha = ? AND Hora = ? AND idUsuarios = ?";

            if (idCitaExcluir > 0) {
                sqlCliente += " AND idCita <> ?";
            }

            try (PreparedStatement ps2 = con.prepareStatement(sqlCliente)) {
                ps2.setDate(1, fechaSQL);
                ps2.setString(2, horaStr);
                ps2.setInt(3, idCliente);
                if (idCitaExcluir > 0) {
                    ps2.setInt(4, idCitaExcluir);
                }

                ResultSet rs2 = ps2.executeQuery();
                if (rs2.next()) {
                    return false; // el cliente ya tiene una cita a esa hora
                }
            }

            // 3. Validar rango horario
            int hora = Integer.parseInt(horaStr.split(":")[0]);
            return hora >= 9 && hora <= 19;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean clientePuedeAgendar(int idUsuario, Date fecha, String horaStr) {
        try (Connection con = conexion.conectar()) {
            if (con == null) {
                return false;
            }

            java.sql.Date fechaSQL = new java.sql.Date(fecha.getTime());

            if (horaStr.length() == 5) {
                horaStr += ":00";
            }

            String sql = "SELECT COUNT(*) as count FROM Cita "
                    + "WHERE idUsuarios = ? AND Fecha = ? AND Hora = ?";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, idUsuario);
            ps.setDate(2, fechaSQL);
            ps.setString(3, horaStr);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("count") == 0; // true si el cliente NO tiene cita
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al verificar citas del cliente: " + e.getMessage());
        }
        return false;
    }

    // Método para cargar horas disponibles
    private void cargarHoras() {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        model.addElement("Seleccione hora");
        for (int i = 9; i <= 19; i++) {
            model.addElement(String.format("%02d:00", i));
        }
        jComboBox3hora.setModel(model);
    }

    // Método para generar número de cita automático
    private void generarNumeroCitaAutomatico() {
        try (Connection con = conexion.conectar()) {
            String sql = "SELECT COALESCE(MAX(idCita), 0) + 1 as siguiente_cita FROM Cita";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                txtnumerocita.setText(String.valueOf(rs.getInt("siguiente_cita")));
            }
            txtnumerocita.setEditable(false);
        } catch (SQLException e) {
            System.out.println("Error al generar número de cita: " + e.getMessage());
        }
    }

    // Método para obtener la fecha seleccionada del calendario
    private Date obtenerFechaSeleccionada() {
        java.util.Calendar cal = CalCitas.getCalendar();
        if (cal != null) {
            return cal.getTime();
        }
        return null;
    }

    // Método para verificar si una fecha está bloqueada
    private boolean verificarFechaBloqueada(Date fecha) {
        try (Connection con = conexion.conectar()) {
            if (con == null) {
                return false;
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String fechaStr = sdf.format(fecha);

            String sql = "SELECT COUNT(*) as count FROM bloqueo_horario WHERE Fecha = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, fechaStr);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("count") > 0;
            }

        } catch (SQLException e) {
            System.out.println("Error al verificar fecha bloqueada: " + e.getMessage());
        }

        return false;
    }

    // Método para mostrar información de bloqueo si existe
    private void mostrarInfoBloqueo(Date fecha) {
        if (verificarFechaBloqueada(fecha)) {
            try (Connection con = conexion.conectar()) {
                if (con == null) {
                    return;
                }

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                String fechaStr = sdf.format(fecha);

                String sql = "SELECT Motivo, Hora_inicio, Hora_fin FROM bloqueo_horario WHERE Fecha = ?";
                PreparedStatement ps = con.prepareStatement(sql);
                ps.setString(1, fechaStr);
                ResultSet rs = ps.executeQuery();

                StringBuilder mensaje = new StringBuilder();
                mensaje.append("️ ATENCIÓN: Esta fecha está bloqueada\n\n");

                while (rs.next()) {
                    mensaje.append("Motivo: ").append(rs.getString("Motivo")).append("\n");
                    mensaje.append("Horario bloqueado: ").append(rs.getString("Hora_inicio"))
                            .append(" - ").append(rs.getString("Hora_fin")).append("\n\n");
                }

                mensaje.append("Por favor seleccione otra fecha.");

                JOptionPane.showMessageDialog(this, mensaje.toString(), "Fecha Bloqueada", JOptionPane.WARNING_MESSAGE);

            } catch (SQLException e) {
                System.out.println("Error al obtener información de bloqueo: " + e.getMessage());
            }
        }
    }

    // Método para cargar los datos del cliente cuando se selecciona
// Método para cargar los datos del cliente cuando se selecciona
    private void cargarDatosCliente(String nombreCliente) {
        try (Connection con = conexion.conectar()) {
            if (con == null) {
                JOptionPane.showMessageDialog(this, "No se pudo conectar a la base de datos.");
                return;
            }

            // CORRECCIÓN: Quitar el espacio antes de "Telefono"
            String sql = "SELECT Telefono, Correo FROM Usuarios "
                    + "WHERE CONCAT(Nombre, ' ', Paterno, ' ', Materno) = ?";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, nombreCliente);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String telefono = rs.getString("Telefono"); // Sin espacio
                String correo = rs.getString("Correo");

                // Actualizar la interfaz con los datos del cliente
                actualizarDatosClienteEnInterfaz(telefono, correo);
            } else {
                // Limpiar datos si no se encuentra el cliente
                actualizarDatosClienteEnInterfaz("", "");
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar datos del cliente: " + e.getMessage());
            System.out.println("Error al cargar datos del cliente: " + e.getMessage());
        }
    }

// Método para actualizar la interfaz con los datos del cliente
    private void actualizarDatosClienteEnInterfaz(String telefono, String correo) {
        // Verificar que los JLabels no sean nulos antes de actualizarlos
        if (lblTelefono != null) {
            lblTelefono.setText("" + (telefono != null && !telefono.isEmpty() ? telefono : "No disponible"));
        } else {
            System.out.println("lblTelefono es null");
        }

        if (lblCorreo != null) {
            lblCorreo.setText("" + (correo != null && !correo.isEmpty() ? correo : "No disponible"));
        } else {
            System.out.println("lblCorreo es null");
        }
    }

    private void cargarDatosCita(int idCita) {
        try (Connection con = conexion.conectar()) {
            if (con == null) {
                JOptionPane.showMessageDialog(this, "No se pudo conectar a la base de datos.");
                return;
            }

            String sql = "SELECT c.Fecha, c.Hora, "
                    + "u.Nombre, u.Materno, u.Paterno, "
                    + "s.Nombre_servicio AS Servicio, "
                    + "c.Estado "
                    + "FROM Cita c "
                    + "JOIN Usuarios u ON c.idUsuarios = u.idUsuarios "
                    + "LEFT JOIN cita_has_servicios chs ON c.idCita = chs.idCita "
                    + "LEFT JOIN Servicios s ON chs.idServicios = s.idServicios "
                    + "WHERE c.idCita = ?";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, idCita);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                // Cliente
                String nombreCliente = rs.getString("Nombre") + " "
                        + rs.getString("Paterno") + " "
                        + rs.getString("Materno");

                jComboBoxnombrecliente.setSelectedItem(nombreCliente);
                jComboBoxnombrecliente.setEnabled(false);

                // Fecha
                java.sql.Date fechaSQL = rs.getDate("Fecha");
                if (fechaSQL != null) {
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    cal.setTime(fechaSQL);
                    CalCitas.setCalendar(cal);
                    CalCitas.setEnabled(true);
                }

                // Hora
                String horaStr = rs.getString("Hora");
                if (horaStr != null && horaStr.length() >= 5) {
                    horaStr = horaStr.substring(0, 5);
                }
                jComboBox3hora.setSelectedItem(horaStr);
                jComboBox3hora.setEnabled(true);

                // Servicio
                String servicio = rs.getString("Servicio");
                if (servicio != null) {
                    jComboBox1servicios.setSelectedItem(servicio);
                }
                jComboBox1servicios.setEnabled(false);

                // ❌ Eliminado todo lo de categoría
                // Estado
                String estado = rs.getString("Estado");
                chksi.setSelected("Sí".equalsIgnoreCase(estado) || "Confirmada".equalsIgnoreCase(estado));
                chkno.setSelected("No".equalsIgnoreCase(estado) || "Pendiente".equalsIgnoreCase(estado));
                chksi.setEnabled(false);
                chkno.setEnabled(false);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar cita: " + e.getMessage());
        }
        jComboBox1servicios.setEnabled(false);
        // mostrar y bloquear número de cita (asegura que siempre se ponga al cargar desde BD)
        txtnumerocita.setText(String.valueOf(idCita));
        txtnumerocita.setEditable(false);
// también por seguridad deshabilitamos el campo (opcional visual)
        txtnumerocita.setEnabled(false);

// reforzar bloqueo del combo de servicios
        jComboBox1servicios.setEnabled(false);

    }

    private int obtenerIdUsuario(String nombreCompleto) {
        int idUsuario = -1;

        try (Connection con = conexion.conectar()) {

            String sql = "SELECT idUsuarios FROM Usuarios "
                    + "WHERE CONCAT(Nombre, ' ', Paterno, ' ', Materno) = ?";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, nombreCompleto);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                idUsuario = rs.getInt("idUsuarios");
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al obtener ID de usuario: " + e.getMessage());
        }

        return idUsuario;
    }

    private void insertarServicio(int idCita, String nombreServicio) {
        int idPago = crearPagoTemporal();
        if (idPago == -1) {
            JOptionPane.showMessageDialog(this, "Error al crear pago temporal para el servicio");
            return;
        }

        try (Connection con = conexion.conectar()) {
            // Buscar idServicios desde la BD
            String sqlId = "SELECT idServicios FROM Servicios WHERE Nombre_servicio = ?";
            PreparedStatement psId = con.prepareStatement(sqlId);
            psId.setString(1, nombreServicio);
            ResultSet rs = psId.executeQuery();

            int idServicio = -1;
            if (rs.next()) {
                idServicio = rs.getInt("idServicios");
            } else {
                JOptionPane.showMessageDialog(this, "No se encontró el servicio: " + nombreServicio);
                return;
            }

            // Insertar en cita_has_servicios
            String sqlInsert = "INSERT INTO cita_has_servicios (idCita, idServicios, Pago_idPago) VALUES (?, ?, ?)";
            PreparedStatement psInsert = con.prepareStatement(sqlInsert);
            psInsert.setInt(1, idCita);
            psInsert.setInt(2, idServicio);
            psInsert.setInt(3, idPago);
            psInsert.executeUpdate();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al insertar servicio: " + e.getMessage());
        }
    }

    private void registrarCitaNueva() {
        try (Connection con = conexion.conectar()) {
            if (con == null) {
                JOptionPane.showMessageDialog(this, "No se pudo conectar a la base de datos.");
                return;
            }

            // Validar campos obligatorios
            if (jComboBoxnombrecliente.getSelectedIndex() == 0
                    || jComboBox3hora.getSelectedIndex() == 0
                    || jComboBox1servicios.getSelectedIndex() == 0) {

                JOptionPane.showMessageDialog(this,
                        "Por favor complete todos los campos:\n"
                        + "- Seleccione un cliente\n"
                        + "- Seleccione un servicio\n"
                        + "- Seleccione una hora");
                return;
            }

            // Obtener fecha
            java.util.Calendar cal = CalCitas.getCalendar();
            if (cal == null) {
                JOptionPane.showMessageDialog(this, "Por favor selecciona una fecha.");
                return;
            }

            Date fechaSeleccionada = cal.getTime();

            // Verificar si la fecha está bloqueada
            if (verificarFechaBloqueada(fechaSeleccionada)) {
                JOptionPane.showMessageDialog(this, "No se puede agendar cita en una fecha bloqueada. Por favor seleccione otra fecha.");
                return;
            }

            java.sql.Date fechaSQL = new java.sql.Date(fechaSeleccionada.getTime());

            // Procesar hora
            String horaStr = jComboBox3hora.getSelectedItem().toString();
            if (horaStr.equals("Seleccione hora")) {
                JOptionPane.showMessageDialog(this, "Por favor selecciona una hora válida.");
                return;
            }

            if (horaStr.length() == 5) { // Si viene en formato HH:mm
                horaStr += ":00";
            }

            Time horaSQL;
            try {
                horaSQL = Time.valueOf(horaStr);
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(this, "Formato de hora inválido: " + horaStr);
                return;
            }

            // Obtener servicio y verificar disponibilidad
            // Obtener servicio y verificar disponibilidad
            String servicio = jComboBox1servicios.getSelectedItem().toString();

// ✅ Validar si la fecha, hora y servicio están disponibles
            if (!fechaHoraServicioDisponible(fechaSeleccionada, horaStr, servicio, -1)) {
                JOptionPane.showMessageDialog(this,
                        "❌ Esta hora ya está ocupada o no está disponible para el cliente/servicio.",
                        "Hora ocupada",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Obtener ID del usuario
            String nombreCliente = jComboBoxnombrecliente.getSelectedItem().toString();
            int idUsuario = obtenerIdUsuario(nombreCliente);
            if (idUsuario == -1) {
                JOptionPane.showMessageDialog(this, "No se encontró el usuario especificado.");
                return;
            }

            // Estado según checkbox
            String estado = chksi.isSelected() ? "Confirmada" : "Pendiente";

            // Insertar cita
            String sql = "INSERT INTO Cita (idUsuarios, Fecha, Hora, Estado) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, idUsuario);
                ps.setDate(2, fechaSQL);
                ps.setTime(3, horaSQL);
                ps.setString(4, estado);

                int filasAfectadas = ps.executeUpdate();

                if (filasAfectadas > 0) {
                    // Obtener ID de cita generada
                    int idCitaGenerada = -1;
                    try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            idCitaGenerada = generatedKeys.getInt(1);
                        }
                    }

                    // Insertar servicio seleccionado
                    insertarServicio(idCitaGenerada, servicio);

                    JOptionPane.showMessageDialog(this,
                            "Cita registrada exitosamente\n"
                            + "Número de cita: " + idCitaGenerada + "\n"
                            + "Cliente: " + nombreCliente + "\n"
                            + "Fecha: " + fechaSQL + "\n"
                            + "Hora: " + horaStr);

                    limpiarCampos();
                }
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al registrar cita: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private int crearPagoTemporal() {
        Connection con = conexion.conectar();
        if (con == null) {
            return -1;
        }

        try {
            // Usar las columnas correctas de tu tabla Pago
            String sql = "INSERT INTO Pago (Estado_pago, fecha_pago, Monto) VALUES ('Pendiente', CURDATE(), 0)";
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.executeUpdate();

            ResultSet generatedKeys = ps.getGeneratedKeys();
            if (generatedKeys.next()) {
                return generatedKeys.getInt(1);
            }
        } catch (SQLException e) {
            System.out.println("Error al crear pago temporal: " + e.getMessage());
        } finally {
            try {
                con.close();
            } catch (SQLException ex) {
                /* ignorar */ }
        }
        return -1;
    }

// --- 1) Método que valida disponibilidad AL EDITAR (excluye la propia cita) ---
    private boolean fechaHoraDisponibleParaEditar(Date fecha, String horaStr, String servicio, int idCitaExcluir, int idCliente) {
        if (fecha == null || horaStr == null || servicio == null) {
            return false;
        }

        // Normalizar hora a HH:mm:ss
        if (horaStr.length() == 5) {
            horaStr = horaStr + ":00";
        }

        try (Connection con = conexion.conectar()) {
            if (con == null) {
                return false;
            }

            java.sql.Date fechaSQL = new java.sql.Date(fecha.getTime());

            // 1) Verificar si OTRO cliente ya tiene ese mismo servicio a esa fecha/hora
            String sqlServicio = "SELECT c.idCita FROM Cita c "
                    + "JOIN cita_has_servicios cs ON c.idCita = cs.idCita "
                    + "JOIN Servicios s ON cs.idServicios = s.idServicios "
                    + "WHERE c.Fecha = ? AND c.Hora = ? AND s.Nombre_servicio = ? AND c.idCita <> ?";
            try (PreparedStatement ps = con.prepareStatement(sqlServicio)) {
                ps.setDate(1, fechaSQL);
                ps.setString(2, horaStr);
                ps.setString(3, servicio);
                ps.setInt(4, idCitaExcluir);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    // Otro registro (distinto a la cita que editas) ya ocupa ese servicio
                    return false;
                }
            }

            // 2) Verificar si el MISMO cliente ya tiene otra cita (distinta) a esa fecha/hora
            String sqlCliente = "SELECT idCita FROM Cita WHERE Fecha = ? AND Hora = ? AND idUsuarios = ? AND idCita <> ?";
            try (PreparedStatement ps2 = con.prepareStatement(sqlCliente)) {
                ps2.setDate(1, fechaSQL);
                ps2.setString(2, horaStr);
                ps2.setInt(3, idCliente);
                ps2.setInt(4, idCitaExcluir);
                ResultSet rs2 = ps2.executeQuery();
                if (rs2.next()) {
                    // El cliente tiene otra cita en esa fecha/hora
                    return false;
                }
            }

            // 3) Rango horario
            int hora = Integer.parseInt(horaStr.split(":")[0]);
            if (hora < 9 || hora > 19) {
                return false;
            }

            // Si pasa todo, está disponible
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

// --- 2) Método actualizarCitaEditar() reemplazado (usar en tu clase) ---
    private void actualizarCitaEditar() {
        Date fecha = obtenerFechaSeleccionada();
        if (fecha == null) {
            JOptionPane.showMessageDialog(this, "Selecciona una fecha válida.");
            return;
        }

        String horaStr = jComboBox3hora.getSelectedItem() == null ? ""
                : jComboBox3hora.getSelectedItem().toString();
        if (horaStr.equals("Seleccione hora") || horaStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Selecciona una hora válida.");
            return;
        }
        // Normalizar a HH:mm:ss
        if (horaStr.length() == 5) {
            horaStr += ":00";
        }

        String servicio = jComboBox1servicios.getSelectedItem() == null ? ""
                : jComboBox1servicios.getSelectedItem().toString();
        if (servicio.equals("Seleccione un servicio") || servicio.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Selecciona un servicio válido.");
            return;
        }

        // Obtener idCliente (si el combo trae "Seleccione..." o está deshabilitado, asegurarse)
        int idCliente = obtenerIdUsuario(jComboBoxnombrecliente.getSelectedItem().toString());
        if (idCliente == -1) {
            JOptionPane.showMessageDialog(this, "No se pudo obtener el cliente.");
            return;
        }

        // Validar disponibilidad excluyendo la cita actual (this.idCita)
        if (!fechaHoraDisponibleParaEditar(fecha, horaStr, servicio, this.idCita, idCliente)) {
            JOptionPane.showMessageDialog(this,
                    "❌ No se puede actualizar: la fecha/hora están ocupadas por ese servicio o por el cliente.",
                    "Hora ocupada",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Si llegó aquí, se puede actualizar. Usar Time para setTime en vez de String:
        try (Connection con = conexion.conectar()) {
            if (con == null) {
                JOptionPane.showMessageDialog(this, "No se pudo conectar a la base de datos.");
                return;
            }

            java.sql.Date fechaSQL = new java.sql.Date(fecha.getTime());
            java.sql.Time horaSQL = java.sql.Time.valueOf(horaStr); // requiere HH:mm:ss

            String sql = "UPDATE Cita SET Fecha = ?, Hora = ? WHERE idCita = ?";
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setDate(1, fechaSQL);
                ps.setTime(2, horaSQL);
                ps.setInt(3, this.idCita);

                int filas = ps.executeUpdate();
                if (filas > 0) {
                    JOptionPane.showMessageDialog(this, "Cita actualizada correctamente.");
                    // Opcional: si necesitas actualizar tabla cita_has_servicios (por ejemplo cambiar servicio),
                    // lo harías aquí dentro de la misma transacción.
                    limpiarCampos();
                } else {
                    JOptionPane.showMessageDialog(this, "No se pudo actualizar la cita (id no encontrado).");
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al actualizar: " + ex.getMessage());
        }
    }

    private void limpiarDatosCliente() {
        lblTelefono.setText("Telefono:  ");
        lblCorreo.setText("Correo:  ");
    }

    public void limpiarCampos() {
        jComboBoxnombrecliente.setSelectedIndex(0);
        jComboBox3hora.setSelectedIndex(0);
        CalCitas.setCalendar(java.util.Calendar.getInstance());
        jComboBox1servicios.setSelectedIndex(0);

        chksi.setSelected(false);
        chkno.setSelected(false);
        generarNumeroCitaAutomatico();

        if (lblTelefono != null) {
            lblTelefono.setText("Teléfono: ");
        }
        if (lblCorreo != null) {
            lblCorreo.setText("Correo: ");
        }
    }

// Constructor para editar cita
    public NewJAgendarcitaREC(int idCita, String nombreCliente, String telefono,
            String correo, String servicio, String fecha,
            String hora, String estado) {
        initComponents();
        setLocationRelativeTo(null);
        conexion = new ConexionBD();
        this.idCita = idCita;
        jLabel1.setText("EDITAR CITA");

        cargarClientes();
        cargarServicios();
        cargarHoras();

        txtnumerocita.setText(String.valueOf(idCita));
        txtnumerocita.setEditable(false);

        jComboBoxnombrecliente.setSelectedItem(nombreCliente);
        jComboBoxnombrecliente.setEnabled(false);

        if (servicio != null && !servicio.isEmpty()) {
            jComboBox1servicios.setSelectedItem(servicio);
        }
        jComboBox1servicios.setEnabled(false);

        if (hora != null && !hora.isEmpty()) {
            if (hora.length() > 5) {
                hora = hora.substring(0, 5);
            }
            jComboBox3hora.setSelectedItem(hora);
        }
        jComboBox3hora.setEnabled(false);

        if ("Sí".equalsIgnoreCase(estado) || "Confirmada".equalsIgnoreCase(estado)) {
            chksi.setSelected(true);
            chkno.setSelected(false);
        } else {
            chksi.setSelected(false);
            chkno.setSelected(true);
        }
        chksi.setEnabled(false);
        chkno.setEnabled(false);

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date fechaDate = sdf.parse(fecha);

            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.setTime(fechaDate);
            CalCitas.setCalendar(cal);
            CalCitas.setEnabled(false);
        } catch (Exception e) {
            System.out.println("Error al asignar fecha: " + e.getMessage());
            CalCitas.setCalendar(java.util.Calendar.getInstance());
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
        jLabel2 = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        txtnumerocita = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        jComboBox3hora = new javax.swing.JComboBox<>();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        chksi = new javax.swing.JCheckBox();
        chkno = new javax.swing.JCheckBox();
        jLabel17 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        btnRegresar = new javax.swing.JButton();
        CalCitas = new com.toedter.calendar.JCalendar();
        jComboBox1servicios = new javax.swing.JComboBox<>();
        jComboBoxnombrecliente = new javax.swing.JComboBox<>();
        lblTelefono = new javax.swing.JLabel();
        lblCorreo = new javax.swing.JLabel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu4 = new javax.swing.JMenu();
        jMenu5 = new javax.swing.JMenu();
        menuBuscarCitas = new javax.swing.JMenuItem();
        menuAgendaCitas = new javax.swing.JMenuItem();
        menuAgendarCita = new javax.swing.JMenuItem();
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
                .addGap(190, 190, 190)
                .addComponent(WPP, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 230, Short.MAX_VALUE)
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

        jLabel2.setFont(new java.awt.Font("Serif", 3, 14)); // NOI18N
        jLabel2.setText("CITA ");

        jButton2.setBackground(new java.awt.Color(255, 204, 255));
        jButton2.setText("Guardar cita");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jLabel1.setText("Nombre del cliente:");

        jLabel4.setText("Fecha");

        jLabel5.setText("Servicio a realizar:");

        jLabel8.setText("Numero de cita");

        jLabel11.setText("Telefono");

        jLabel12.setText("Correo electronico");

        jComboBox3hora.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Hora" }));
        jComboBox3hora.setToolTipText("Diseño selecionado");
        jComboBox3hora.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox3horaActionPerformed(evt);
            }
        });

        jLabel15.setText("Hora");

        jLabel16.setText("¿Anticipo recibido?");

        chksi.setText("Si");

        chkno.setText("no");
        chkno.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chknoActionPerformed(evt);
            }
        });

        jLabel17.setText("Monto recibido");

        jLabel18.setText("$100 MXN");

        btnRegresar.setBackground(new java.awt.Color(255, 204, 255));
        btnRegresar.setText("Regresar");
        btnRegresar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRegresarActionPerformed(evt);
            }
        });

        CalCitas.setAutoscrolls(true);

        jComboBox1servicios.setToolTipText("Hora");
        jComboBox1servicios.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox1serviciosActionPerformed(evt);
            }
        });

        jComboBoxnombrecliente.setToolTipText("Diseño selecionado");
        jComboBoxnombrecliente.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxnombreclienteActionPerformed(evt);
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
                            .addComponent(jLabel16)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addGroup(jPanel1Layout.createSequentialGroup()
                                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel8)
                                        .addComponent(jLabel5))
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(txtnumerocita, javax.swing.GroupLayout.PREFERRED_SIZE, 364, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(CalCitas, javax.swing.GroupLayout.PREFERRED_SIZE, 355, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jComboBox1servicios, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel1)
                                        .addComponent(jLabel11)
                                        .addComponent(jLabel12))
                                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                            .addGap(74, 74, 74)
                                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(lblTelefono, javax.swing.GroupLayout.PREFERRED_SIZE, 243, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(lblCorreo, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                            .addGap(18, 18, 18)
                                            .addComponent(jComboBoxnombrecliente, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel15)
                                    .addComponent(jLabel17))
                                .addGap(88, 88, 88)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addComponent(chksi)
                                        .addGap(18, 18, 18)
                                        .addComponent(chkno))
                                    .addComponent(jComboBox3hora, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addComponent(jLabel18)
                                        .addGap(133, 133, 133)
                                        .addComponent(btnRegresar)
                                        .addGap(18, 18, 18)
                                        .addComponent(jButton2, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE))))
                            .addComponent(jLabel4)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(347, 347, 347)
                        .addComponent(jLabel2))
                    .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(95, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2)
                .addGap(46, 46, 46)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel1)
                            .addComponent(jComboBoxnombrecliente, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(25, 25, 25)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel11)
                            .addComponent(lblTelefono, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jLabel12))
                    .addComponent(lblCorreo, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(txtnumerocita, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(27, 27, 27)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(jComboBox1servicios, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(65, 65, 65)
                        .addComponent(jLabel4)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(56, 56, 56)
                        .addComponent(CalCitas, javax.swing.GroupLayout.DEFAULT_SIZE, 208, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel15)
                            .addComponent(jComboBox3hora, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel16)
                            .addComponent(chksi)
                            .addComponent(chkno))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel17)
                            .addComponent(jLabel18)
                            .addComponent(jButton2)
                            .addComponent(btnRegresar))
                        .addGap(31, 31, 31)
                        .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
        );

        jMenu4.setText("INICIO");
        jMenu4.addMenuListener(new javax.swing.event.MenuListener() {
            public void menuCanceled(javax.swing.event.MenuEvent evt) {
            }
            public void menuDeselected(javax.swing.event.MenuEvent evt) {
            }
            public void menuSelected(javax.swing.event.MenuEvent evt) {
                jMenu4MenuSelected(evt);
            }
        });
        jMenuBar1.add(jMenu4);

        jMenu5.setText("CITAS");

        menuBuscarCitas.setText("Buscar citas");
        menuBuscarCitas.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuBuscarCitasActionPerformed(evt);
            }
        });
        jMenu5.add(menuBuscarCitas);

        menuAgendaCitas.setText("Agenda de citas");
        menuAgendaCitas.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuAgendaCitasActionPerformed(evt);
            }
        });
        jMenu5.add(menuAgendaCitas);

        menuAgendarCita.setText("Agendar cita");
        menuAgendarCita.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuAgendarCitaActionPerformed(evt);
            }
        });
        jMenu5.add(menuAgendarCita);

        jMenuBar1.add(jMenu5);

        jMenu7.setText("LOGIN");

        jMenuItem5.setText("login");
        jMenuItem5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem5ActionPerformed(evt);
            }
        });
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
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed

        // Determinar si estamos creando o editando una cita
        if (idCita > 0) {
            actualizarCitaEditar();
        } else {
            registrarCitaNueva();
        }

    }//GEN-LAST:event_jButton2ActionPerformed

    private void jComboBox3horaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox3horaActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jComboBox3horaActionPerformed

    private void chknoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chknoActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_chknoActionPerformed

    private void btnRegresarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRegresarActionPerformed

        NewJCitaAgenda NewJCitaAgenda = new NewJCitaAgenda();
        NewJCitaAgenda.setVisible(true);
        this.dispose(); // cierra la actual


    }//GEN-LAST:event_btnRegresarActionPerformed

    private void jComboBox1serviciosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1serviciosActionPerformed
        // TODO add your handling code here:

    }//GEN-LAST:event_jComboBox1serviciosActionPerformed

    private void jComboBoxnombreclienteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxnombreclienteActionPerformed
        // TODO add your handling code here:
        if (jComboBoxnombrecliente.getSelectedIndex() > 0) {
            String nombreCliente = jComboBoxnombrecliente.getSelectedItem().toString();
            cargarDatosCliente(nombreCliente);
        } else {
            // Limpiar datos si no hay cliente seleccionado
            lblTelefono.setText("Teléfono: ");
            lblCorreo.setText("Correo: ");
        }
    }//GEN-LAST:event_jComboBoxnombreclienteActionPerformed

    private void jMenu4MenuSelected(javax.swing.event.MenuEvent evt) {//GEN-FIRST:event_jMenu4MenuSelected
        // TODO add your handling code here:
        //inicio
        Inicio Inicio = new Inicio();
        Inicio.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenu4MenuSelected

    private void menuBuscarCitasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuBuscarCitasActionPerformed
        // TODO add your handling code here:
        //citas
        NewJBuscarCita buscar = new NewJBuscarCita(this);
        buscar.setVisible(true);
        this.setVisible(false);
    }//GEN-LAST:event_menuBuscarCitasActionPerformed

    private void jMenuItem5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem5ActionPerformed
        // TODO add your handling code here:
        //boton de contacto
        NewJLogin NewJLogin = new NewJLogin();
        NewJLogin.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem5ActionPerformed

    private void menuAgendaCitasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuAgendaCitasActionPerformed
        // TODO add your handling code here:
        NewJCitaAgenda agenda = new NewJCitaAgenda(this);
        agenda.setVisible(true);
        this.setVisible(false);
    }//GEN-LAST:event_menuAgendaCitasActionPerformed

    private void menuAgendarCitaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuAgendarCitaActionPerformed
        // TODO add your handling code here:
        NewJAgendarcitaREC agendar = new NewJAgendarcitaREC(this);
        agendar.setVisible(true);
        this.setVisible(false);
    }//GEN-LAST:event_menuAgendarCitaActionPerformed

    private void jMenuItemCerrarSecionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemCerrarSecionActionPerformed
        // TODO add your handling code here:
        andynails.SessionManager.cerrarSesion(this);
    }//GEN-LAST:event_jMenuItemCerrarSecionActionPerformed

    private void CalCitasPropertyChange(java.beans.PropertyChangeEvent evt) {
        if ("calendar".equals(evt.getPropertyName())) {
            Date fechaSeleccionada = obtenerFechaSeleccionada(); // Cambia esta línea
            if (fechaSeleccionada != null) {
                mostrarInfoBloqueo(fechaSeleccionada);
            }
        }
    }

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
            java.util.logging.Logger.getLogger(NewJAgendarcitaREC.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(NewJAgendarcitaREC.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(NewJAgendarcitaREC.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(NewJAgendarcitaREC.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
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
                new NewJAgendarcitaREC().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private com.toedter.calendar.JCalendar CalCitas;
    private javax.swing.JLabel FACE;
    private javax.swing.JLabel INS;
    private javax.swing.JLabel WPP;
    private javax.swing.JButton btnRegresar;
    private javax.swing.JCheckBox chkno;
    private javax.swing.JCheckBox chksi;
    private javax.swing.JButton jButton2;
    private javax.swing.JComboBox<String> jComboBox1servicios;
    private javax.swing.JComboBox<String> jComboBox3hora;
    private javax.swing.JComboBox<String> jComboBoxnombrecliente;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JMenu jMenu16;
    private javax.swing.JMenu jMenu4;
    private javax.swing.JMenu jMenu5;
    private javax.swing.JMenu jMenu7;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem5;
    private javax.swing.JMenuItem jMenuItemCerrarSecion;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JLabel lblCorreo;
    private javax.swing.JLabel lblTelefono;
    private javax.swing.JMenuItem menuAgendaCitas;
    private javax.swing.JMenuItem menuAgendarCita;
    private javax.swing.JMenuItem menuBuscarCitas;
    private javax.swing.JTextField txtnumerocita;
    // End of variables declaration//GEN-END:variables
}
