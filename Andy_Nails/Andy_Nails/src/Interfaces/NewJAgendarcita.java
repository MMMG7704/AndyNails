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
public class NewJAgendarcita extends javax.swing.JFrame {

    ConexionBD conexion;
    private int idCita = -1;

    public NewJAgendarcita(int idCita) {
        initComponents();
        RedesSociales.configurarRedesSociales(INS, WPP, FACE);

        setLocationRelativeTo(null);
        conexion = new ConexionBD();
        this.idCita = idCita;
        jLabel1.setText("EDITAR CITA");
        configurarCalendario();
        // Cargar datos
        cargarClientes();
        cargarServicios();
        cargarHoras();
        generarNumeroCitaAutomatico();
        cargarDatosCita(idCita);
    }

    private JFrame ventanaAnterior;

    public NewJAgendarcita(JFrame ventanaAnterior) {
        initComponents();
        this.ventanaAnterior = ventanaAnterior;
        setLocationRelativeTo(null);
        conexion = new ConexionBD();
        configurarCalendario();
        // Cargar datos
        cargarClientes();
        cargarServicios();
        cargarHoras();
        generarNumeroCitaAutomatico();
    }

    public NewJAgendarcita() {
        initComponents();
        setLocationRelativeTo(null);
        conexion = new ConexionBD();
        jLabel1.setText("REGISTRAR NUEVA CITA");
        configurarCalendario();

        if (lblTelefono == null) {
            System.out.println("ADVERTENCIA: lblTelefono no está inicializado");
        }
        if (lblCorreo == null) {
            System.out.println("ADVERTENCIA: lblCorreo no está inicializado");
        }

        // Cargar datos
        cargarClientes();
        cargarServicios();
        cargarHoras();
        generarNumeroCitaAutomatico();
    }

    private void cargarClientes() {
        try (Connection con = conexion.conectar()) {
            if (con == null) {
                JOptionPane.showMessageDialog(this, "No se pudo conectar a la base de datos.");
                return;
            }

            String sql = "SELECT idUsuarios, Nombre FROM Usuarios ORDER BY Nombre";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            model.addElement("Seleccione un cliente");

            while (rs.next()) {
                model.addElement(rs.getString("Nombre"));
            }

            jComboBoxnombrecliente.setModel(model);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar clientes: " + e.getMessage());
        }
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

    // Método para cargar horas disponibles
    private void cargarHoras() {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        model.addElement("Seleccione hora");
        for (int i = 9; i <= 19; i++) {
            model.addElement(String.format("%02d:00", i));
        }
        jComboBox3hora.setModel(model);
    }

    // Método para cargar categorías según el servicio seleccionado
    private void cargarCategoriasPorServicio(String nombreServicio) {
        try (Connection con = conexion.conectar()) {
            if (con == null) {
                JOptionPane.showMessageDialog(this, "No se pudo conectar a la base de datos.");
                return;
            }

            String sql = "SELECT DISTINCT cs.Nombre_categoria "
                    + "FROM categoria_Servicio cs "
                    + "JOIN Servicios s ON cs.idServicios = s.idServicios "
                    + "WHERE s.Nombre_servicio = ? "
                    + "ORDER BY cs.Nombre_categoria";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, nombreServicio);
            ResultSet rs = ps.executeQuery();

            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            model.addElement("Seleccione una categoría");

            while (rs.next()) {
                model.addElement(rs.getString("Nombre_categoria"));
            }

            jComboBox1diseñoselecionado.setModel(model);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar categorías: " + e.getMessage());
        }
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
            String sql = "SELECT Telefono, Correo FROM Usuarios WHERE Nombre = ?";
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
            lblTelefono.setText("Teléfono: " + (telefono != null && !telefono.isEmpty() ? telefono : "No disponible"));
        } else {
            System.out.println("lblTelefono es null");
        }

        if (lblCorreo != null) {
            lblCorreo.setText("Correo: " + (correo != null && !correo.isEmpty() ? correo : "No disponible"));
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

            String sql = "SELECT c.Fecha, c.Hora, u.Nombre, s.Nombre_servicio AS Servicio, c.Estado "
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
                String nombreCliente = rs.getString("Nombre");
                jComboBoxnombrecliente.setSelectedItem(nombreCliente);
                jComboBoxnombrecliente.setEnabled(false);

                // Fecha
                java.sql.Date fechaSQL = rs.getDate("Fecha");
                if (fechaSQL != null) {
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    cal.setTime(fechaSQL);
                    CalCitas.setCalendar(cal);
                    CalCitas.setEnabled(false);
                }

                // Hora
                String horaStr = rs.getString("Hora");
                if (horaStr != null && horaStr.length() >= 5) {
                    horaStr = horaStr.substring(0, 5);
                }
                jComboBox3hora.setSelectedItem(horaStr);
                jComboBox3hora.setEnabled(false);

                // Servicio (sin categoría)
                String servicio = rs.getString("Servicio");
                if (servicio != null) {
                    jComboBox1servicios.setSelectedItem(servicio);
                    // Ya no cargamos categorías
                }

                jComboBox1servicios.setEnabled(false);
                jComboBox1diseñoselecionado.setEnabled(false);

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
    }

    private int obtenerIdUsuario(String nombreCliente) {
        int idUsuario = -1;
        Connection con = conexion.conectar();

        if (con == null) {
            JOptionPane.showMessageDialog(this, "No se pudo conectar a la base de datos.");
            return idUsuario;
        }

        String sql = "SELECT idUsuarios FROM Usuarios WHERE Nombre = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, nombreCliente);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                idUsuario = rs.getInt("idUsuarios");
            } else {
                JOptionPane.showMessageDialog(this, "No se encontró el usuario con nombre: " + nombreCliente);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al obtener ID de usuario: " + e.getMessage());
        } finally {
            try {
                con.close();
            } catch (SQLException ex) {
                /* ignorar */ }
        }

        return idUsuario;
    }

    private void insertarServicio(int idCita, String nombreServicio) {
        // Primero crear un pago temporal
        int idPago = crearPagoTemporal();

        if (idPago == -1) {
            JOptionPane.showMessageDialog(this, "Error al crear pago temporal para el servicio");
            return;
        }

        String sql = "INSERT INTO cita_has_servicios (idCita, idServicios, Pago_idPago) VALUES (?, ?, ?)";

        try (Connection con = conexion.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, idCita);

            // Mapear nombre del servicio a ID
            int idServicio = -1;
            switch (nombreServicio) {
                case "Uñas":
                    idServicio = 1;
                    break;
                case "Maquillaje":
                    idServicio = 2;
                    break;
                case "Peinado":
                    idServicio = 3;
                    break;
                case "Tatuajes":
                    idServicio = 4;
                    break;
                case "otros":
                    idServicio = 13;
                    break;
                default:
                    idServicio = 1;
            }

            ps.setInt(2, idServicio);
            ps.setInt(3, idPago);

            ps.executeUpdate();

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
                    || jComboBox1servicios.getSelectedIndex() == 0
                    || jComboBox1diseñoselecionado.getSelectedIndex() == 0) {

                JOptionPane.showMessageDialog(this,
                        "Por favor complete todos los campos:\n"
                        + "- Seleccione un cliente\n"
                        + "- Seleccione un servicio\n"
                        + "- Seleccione una categoría\n"
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
            java.util.Calendar hoy = java.util.Calendar.getInstance();
            hoy.set(java.util.Calendar.HOUR_OF_DAY, 0);
            hoy.set(java.util.Calendar.MINUTE, 0);
            hoy.set(java.util.Calendar.SECOND, 0);
            hoy.set(java.util.Calendar.MILLISECOND, 0);

            if (fechaSeleccionada.before(hoy.getTime())) {
                JOptionPane.showMessageDialog(this,
                        "No puedes agendar una cita en una fecha pasada.\n"
                        + "Por favor selecciona una fecha actual o futura.",
                        "Fecha inválida",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

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

            if (horaStr.length() == 5) {
                horaStr += ":00";
            }

            Time horaSQL;
            try {
                horaSQL = Time.valueOf(horaStr);
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(this, "Formato de hora inválido: " + horaStr);
                return;
            }

            // Obtener ID del usuario
            String nombreCliente = jComboBoxnombrecliente.getSelectedItem().toString();
            int idUsuario = obtenerIdUsuario(nombreCliente);
            if (idUsuario == -1) {
                JOptionPane.showMessageDialog(this, "No se encontró el usuario especificado.");
                return;
            }

            // Determinar anticipo
            String anticipo = "No";
            if (chksi.isSelected()) {
                anticipo = "Sí";
            }

            // Insertar cita
            String sql = "INSERT INTO Cita (idUsuarios, Fecha, Hora, Estado) VALUES (?, ?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, idUsuario);
            ps.setDate(2, fechaSQL);
            ps.setTime(3, horaSQL);
            ps.setString(4, anticipo);

            int filasAfectadas = ps.executeUpdate();

            if (filasAfectadas > 0) {
                // Obtener el ID de la cita recién insertada
                ResultSet generatedKeys = ps.getGeneratedKeys();
                int idCitaGenerada = -1;
                if (generatedKeys.next()) {
                    idCitaGenerada = generatedKeys.getInt(1);
                }

                // Insertar servicio y categoría seleccionados
                String servicio = jComboBox1servicios.getSelectedItem().toString();
                String categoria = jComboBox1diseñoselecionado.getSelectedItem().toString();
                insertarServicio(idCitaGenerada, servicio);
                JOptionPane.showMessageDialog(this,
                        " Cita registrada exitosamente\n"
                        + "Número de cita: " + idCitaGenerada + "\n"
                        + "Cliente: " + nombreCliente + "\n"
                        + "Fecha: " + fechaSQL + "\n"
                        + "Hora: " + horaStr);

                limpiarCampos();
                this.dispose();
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

    private void actualizarCita() {
        try (Connection con = conexion.conectar()) {
            if (con == null) {
                JOptionPane.showMessageDialog(this, "No se pudo conectar a la base de datos.");
                return;
            }

            Date fechaSeleccionada = obtenerFechaSeleccionada();
            if (fechaSeleccionada == null || jComboBox1servicios.getSelectedIndex() == 0
                    || jComboBox1diseñoselecionado.getSelectedIndex() == 0) {
                JOptionPane.showMessageDialog(this, "Por favor completa todos los campos antes de continuar.");
                return;
            }

            // Verificar si la fecha está bloqueada
            if (verificarFechaBloqueada(fechaSeleccionada)) {
                JOptionPane.showMessageDialog(this, "No se puede agendar cita en una fecha bloqueada. Por favor seleccione otra fecha.");
                return;
            }

            java.sql.Date fechaSQL = new java.sql.Date(fechaSeleccionada.getTime());

            // Procesar hora
            String horaStr = jComboBox3hora.getSelectedItem().toString();
            if (!horaStr.contains(":")) {
                horaStr += ":00";
            }
            if (horaStr.length() == 5) {
                horaStr += ":00";
            }

            // Determinar anticipo
            String anticipo = "No";
            if (chksi.isSelected()) {
                anticipo = "Sí";
            }

            String sql = "UPDATE Cita SET Fecha = ?, Hora = ?, Estado = ? WHERE idCita = ?";
            PreparedStatement ps = con.prepareStatement(sql);

            ps.setDate(1, fechaSQL);
            ps.setString(2, horaStr);
            ps.setString(3, anticipo);
            ps.setInt(4, idCita);

            int filasAfectadas = ps.executeUpdate();

            if (filasAfectadas > 0) {
                // Actualizar servicios (primero eliminar los existentes y luego insertar los nuevos)
                String deleteSql = "DELETE FROM cita_has_servicios WHERE idCita = ?";
                try (PreparedStatement deletePs = con.prepareStatement(deleteSql)) {
                    deletePs.setInt(1, idCita);
                    deletePs.executeUpdate();
                }

                // Insertar servicio y categoría seleccionados
                String servicio = jComboBox1servicios.getSelectedItem().toString();
                String categoria = jComboBox1diseñoselecionado.getSelectedItem().toString();
                insertarServicio(idCita, servicio);

                JOptionPane.showMessageDialog(this, "Cita actualizada correctamente.");
                this.dispose();
            } else {
                JOptionPane.showMessageDialog(this, "No se pudo actualizar la cita.");
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al actualizar cita: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void limpiarDatosCliente() {
        lblTelefono.setText("Telefono:  ");
        lblCorreo.setText("Correo:  ");
    }

    private void limpiarCampos() {
        jComboBoxnombrecliente.setSelectedIndex(0);
        jComboBox3hora.setSelectedIndex(0);
        CalCitas.setCalendar(java.util.Calendar.getInstance());
        jComboBox1servicios.setSelectedIndex(0);
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        model.addElement("Seleccione una categoría");
        jComboBox1diseñoselecionado.setModel(model);
        chksi.setSelected(false);
        chkno.setSelected(false);
        generarNumeroCitaAutomatico();

        // Limpiar datos del cliente
        if (lblTelefono != null) {
            lblTelefono.setText("Teléfono: ");
        }
        if (lblCorreo != null) {
            lblCorreo.setText("Correo: ");
        }
    }

// Constructor para editar cita
    public NewJAgendarcita(int idCita, String nombreCliente, String telefono,
            String correo, String servicio, String fecha,
            String hora, String estado) {
        initComponents();
        setLocationRelativeTo(null);
        conexion = new ConexionBD();
        this.idCita = idCita;
        jLabel1.setText("EDITAR CITA");
        configurarCalendario();

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

    private void configurarCalendario() {
        // Configurar fecha mínima como hoy
        java.util.Calendar fechaMinima = java.util.Calendar.getInstance();
        fechaMinima.set(java.util.Calendar.HOUR_OF_DAY, 0);
        fechaMinima.set(java.util.Calendar.MINUTE, 0);
        fechaMinima.set(java.util.Calendar.SECOND, 0);
        fechaMinima.set(java.util.Calendar.MILLISECOND, 0);

        // Aplicar fecha mínima al JCalendar
        CalCitas.setMinSelectableDate(fechaMinima.getTime());
        CalCitas.setMaxSelectableDate(null); // Sin límite máximo

        System.out.println("DEBUG - Fecha mínima establecida: "
                + new java.text.SimpleDateFormat("yyyy-MM-dd").format(fechaMinima.getTime()));

        // Agregar listener para detectar cambios en la fecha seleccionada
        CalCitas.addPropertyChangeListener("calendar", evt -> {
            if ("calendar".equals(evt.getPropertyName())) {
                validarFechaSeleccionada();
            }
        });
    }

    private void validarFechaSeleccionada() {
        try {
            java.util.Calendar calSeleccionada = CalCitas.getCalendar();
            if (calSeleccionada != null) {
                java.util.Date fechaSeleccionada = calSeleccionada.getTime();

                // Obtener fecha actual sin hora
                java.util.Calendar hoy = java.util.Calendar.getInstance();
                hoy.set(java.util.Calendar.HOUR_OF_DAY, 0);
                hoy.set(java.util.Calendar.MINUTE, 0);
                hoy.set(java.util.Calendar.SECOND, 0);
                hoy.set(java.util.Calendar.MILLISECOND, 0);

                java.text.SimpleDateFormat formato = new java.text.SimpleDateFormat("yyyy-MM-dd");
                String fechaFormateada = formato.format(fechaSeleccionada);
                String hoyFormateado = formato.format(hoy.getTime());

                // Verificar si la fecha es pasada
                if (fechaSeleccionada.before(hoy.getTime())) {
                    JOptionPane.showMessageDialog(this,
                            "No puedes seleccionar una fecha pasada.\n\n"
                            + "Fecha seleccionada: " + fechaFormateada + "\n"
                            + "Fecha actual: " + hoyFormateado + "\n\n"
                            + "Por favor selecciona una fecha actual o futura.",
                            "Fecha inválida",
                            JOptionPane.WARNING_MESSAGE);

                    // Restablecer a la fecha de hoy
                    CalCitas.setCalendar(hoy);
                    System.out.println("DEBUG - Fecha corregida a hoy: " + hoyFormateado);
                }

                // También verificar si la fecha está bloqueada
                mostrarInfoBloqueo(fechaSeleccionada);
            }
        } catch (Exception e) {
            e.printStackTrace();
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
        jComboBox1diseñoselecionado = new javax.swing.JComboBox<>();
        jLabel11 = new javax.swing.JLabel();
        txtnumerocita = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
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
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenu6 = new javax.swing.JMenu();
        jMenuItem4 = new javax.swing.JMenuItem();
        jMenu7 = new javax.swing.JMenu();
        jMenuItem5 = new javax.swing.JMenuItem();
        jMenu19 = new javax.swing.JMenu();
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

        jComboBox1diseñoselecionado.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jComboBox1diseñoselecionado.setToolTipText("Hora");
        jComboBox1diseñoselecionado.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox1diseñoselecionadoActionPerformed(evt);
            }
        });

        jLabel11.setText("Telefono");

        jLabel12.setText("Correo electronico");

        jLabel13.setText("Diseño selecionado");

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

        jComboBox1servicios.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Uñas     ", "Maquillaje  ", "Peinado ", "Tatuajes  ", "otros   " }));
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
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(txtnumerocita, javax.swing.GroupLayout.PREFERRED_SIZE, 364, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(CalCitas, javax.swing.GroupLayout.PREFERRED_SIZE, 355, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jComboBox1servicios, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                                    .addComponent(jLabel13)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                    .addComponent(jComboBox1diseñoselecionado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel1)
                                        .addComponent(jLabel11)
                                        .addComponent(jLabel12))
                                    .addGap(74, 74, 74)
                                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jComboBoxnombrecliente, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(lblTelefono, javax.swing.GroupLayout.PREFERRED_SIZE, 243, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(lblCorreo, javax.swing.GroupLayout.PREFERRED_SIZE, 185, javax.swing.GroupLayout.PREFERRED_SIZE))))
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
                .addGap(28, 28, 28)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel13)
                    .addComponent(jComboBox1diseñoselecionado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addComponent(jLabel4)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(9, 9, 9)
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

        jMenuItem2.setText("citas");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu5.add(jMenuItem2);

        jMenuBar1.add(jMenu5);

        jMenu6.setText("PAGOS");

        jMenuItem4.setText("Pagos");
        jMenuItem4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem4ActionPerformed(evt);
            }
        });
        jMenu6.add(jMenuItem4);

        jMenuBar1.add(jMenu6);

        jMenu7.setText("LOGIN");

        jMenuItem5.setText("login");
        jMenuItem5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem5ActionPerformed(evt);
            }
        });
        jMenu7.add(jMenuItem5);

        jMenuBar1.add(jMenu7);

        jMenu19.setText("CERRAR SESIÓN");

        jMenuItemCerrarSecion.setText("Cerrar sesión");
        jMenuItemCerrarSecion.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemCerrarSecionActionPerformed(evt);
            }
        });
        jMenu19.add(jMenuItemCerrarSecion);

        jMenuBar1.add(jMenu19);

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

    private void jMenu4MenuSelected(javax.swing.event.MenuEvent evt) {//GEN-FIRST:event_jMenu4MenuSelected
        // TODO add your handling code here:
        //inicio
        Inicio Inicio = new Inicio();
        Inicio.setVisible(true);
        this.dispose(); // cierra la actual

    }//GEN-LAST:event_jMenu4MenuSelected

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        // TODO add your handling code here:
        //citas
        NewJRegClient NewJRegClient = new NewJRegClient();
        NewJRegClient.setVisible(true);
        this.dispose(); // cierra la actual

    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem4ActionPerformed
        // TODO add your handling code here:
        //agendar cancelar
        NewJPago NewJPago = new NewJPago();
        NewJPago.setVisible(true);
        this.dispose(); // cierra la actual

    }//GEN-LAST:event_jMenuItem4ActionPerformed

    private void jMenuItem5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem5ActionPerformed
        // TODO add your handling code here:
        //boton de contacto
        NewJLogin NewJLogin = new NewJLogin();
        NewJLogin.setVisible(true);
        this.dispose(); // cierra la actual

    }//GEN-LAST:event_jMenuItem5ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed

        // Determinar si estamos creando o editando una cita
        if (idCita > 0) {
            actualizarCita();
        } else {
            registrarCitaNueva();
        }
        NewJPanelAdministracion NewJPanelAdministracion = new NewJPanelAdministracion();
        NewJPanelAdministracion.setVisible(true);
        this.dispose(); // cierra la actual


    }//GEN-LAST:event_jButton2ActionPerformed

    private void jComboBox1diseñoselecionadoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1diseñoselecionadoActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jComboBox1diseñoselecionadoActionPerformed

    private void jComboBox3horaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox3horaActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jComboBox3horaActionPerformed

    private void chknoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chknoActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_chknoActionPerformed

    private void btnRegresarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRegresarActionPerformed

        NewJCitaAgendaE NewJCitaAgenda = new NewJCitaAgendaE();
        NewJCitaAgenda.setVisible(true);
        this.dispose(); // cierra la actual


    }//GEN-LAST:event_btnRegresarActionPerformed

    private void jComboBox1serviciosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1serviciosActionPerformed
        // TODO add your handling code here:
        if (jComboBox1servicios.getSelectedIndex() > 0) {
            String servicioSeleccionado = jComboBox1servicios.getSelectedItem().toString();
            cargarCategoriasPorServicio(servicioSeleccionado);
        } else {
            // Limpiar el comboBox de categorías si no hay servicio seleccionado
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            model.addElement("Seleccione una categoría");
            jComboBox1diseñoselecionado.setModel(model);
        }
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
            java.util.logging.Logger.getLogger(NewJAgendarcita.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(NewJAgendarcita.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(NewJAgendarcita.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(NewJAgendarcita.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new NewJAgendarcita().setVisible(true);
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
    private javax.swing.JComboBox<String> jComboBox1diseñoselecionado;
    private javax.swing.JComboBox<String> jComboBox1servicios;
    private javax.swing.JComboBox<String> jComboBox3hora;
    private javax.swing.JComboBox<String> jComboBoxnombrecliente;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JMenu jMenu19;
    private javax.swing.JMenu jMenu4;
    private javax.swing.JMenu jMenu5;
    private javax.swing.JMenu jMenu6;
    private javax.swing.JMenu jMenu7;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem jMenuItem5;
    private javax.swing.JMenuItem jMenuItemCerrarSecion;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JLabel lblCorreo;
    private javax.swing.JLabel lblTelefono;
    private javax.swing.JTextField txtnumerocita;
    // End of variables declaration//GEN-END:variables
}
