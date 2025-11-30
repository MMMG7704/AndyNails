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
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Mariana Mora
 */
public class NewJRegClient extends javax.swing.JFrame {

    private int idUsuarioSeleccionado = -1;

    private int numeroCitaSeleccionado = -1; // Variable para el cliente seleccionado
    private javax.swing.JTextField txtTelefono;
    private javax.swing.JTextField txtCorreo;

    /**
     * Creates new form NewJRegClient
     */
    public NewJRegClient() {
        initComponents();
        RedesSociales.configurarRedesSociales(INS, WPP, FACE);

        cargarClientesTabla();
        cargarFechasComboBox();
        cargarNombresComboBox();

        jTablecontenidocitas.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                cargarEstadoCita();
                actualizarBotones(); // Actualizar estado de botones
            }
        });

        // Inicialmente deshabilitar botones hasta que se seleccione algo
        actualizarBotones();
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

    // Método para actualizar el estado de los botones según la selección
    private void actualizarBotones() {
        int fila = jTablecontenidocitas.getSelectedRow();
        boolean haySeleccion = (fila >= 0);

        btnver.setEnabled(haySeleccion);
        btnEditar.setEnabled(haySeleccion);
        btneliminar.setEnabled(haySeleccion);
    }

    // Método para cargar clientes en la tabla
    private void cargarClientesTabla() {
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("Nombre");
        model.addColumn("Teléfono");
        model.addColumn("Correo");
        model.addColumn("Fecha Registro");
        model.addColumn("Número de citas");

        try (Connection con = ConexionBD.getConnection()) {
            String sql = "SELECT u.idUsuarios, u.Nombre, u.Paterno, u.Materno, u.Telefono, u.Correo, u.fecha_registro, "
                    + "COUNT(c.idCita) as numero_citas "
                    + "FROM usuarios u "
                    + "LEFT JOIN cita c ON u.idUsuarios = c.idUsuarios "
                    + "GROUP BY u.idUsuarios, u.Nombre, u.Paterno, u.Materno, u.Telefono, u.Correo, u.fecha_registro "
                    + "ORDER BY u.fecha_registro DESC";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String nombreCompleto = rs.getString("Nombre") + " "
                        + rs.getString("Paterno") + " "
                        + rs.getString("Materno");

                model.addRow(new Object[]{
                    nombreCompleto,
                    rs.getString("Telefono"),
                    rs.getString("Correo"),
                    rs.getDate("fecha_registro"),
                    rs.getInt("numero_citas")
                });
            }

            jTablecontenidocitas.setModel(model);
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al cargar clientes: " + e.getMessage());
        }
    }

    private void cargarFechasComboBox() {
        jComboBox1Fecha.removeAllItems();
        jComboBox1Fecha.addItem("Todas las fechas");

        try (Connection con = ConexionBD.getConnection()) {
            String sql = "SELECT DISTINCT fecha_registro FROM usuarios ORDER BY fecha_registro DESC";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                jComboBox1Fecha.addItem(rs.getString("fecha_registro"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al cargar fechas: " + e.getMessage());
        }
    }

    private void cargarNombresComboBox() {
        jComboBoxNombreClie.removeAllItems();
        jComboBoxNombreClie.addItem("Todos los clientes");

        try (Connection con = ConexionBD.getConnection()) {
            String sql = "SELECT idUsuarios, Nombre, Paterno, Materno FROM usuarios ORDER BY Nombre";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String nombreCompleto = rs.getString("Nombre") + " "
                        + rs.getString("Paterno") + " "
                        + rs.getString("Materno");
                jComboBoxNombreClie.addItem(nombreCompleto);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al cargar nombres: " + e.getMessage());
        }
    }

    private void cargarEstadoCita() {
        int fila = jTablecontenidocitas.getSelectedRow();
        if (fila >= 0) {
            // Obtener el estado de la última cita del cliente seleccionado
            String cliente = jTablecontenidocitas.getValueAt(fila, 0).toString();
            String estado = obtenerEstadoUltimaCita(cliente);
            txtEstadocita.setText(estado);

            // Obtener el ID del usuario seleccionado
            idUsuarioSeleccionado = obtenerIdUsuarioSeleccionado(fila);
        } else {
            txtEstadocita.setText("");
            idUsuarioSeleccionado = -1;
        }
    }

    // Método para obtener el estado de la última cita del cliente
    private String obtenerEstadoUltimaCita(String nombreCliente) {
        try (Connection con = ConexionBD.getConnection()) {
            String sql = "SELECT c.Estado FROM cita c "
                    + "INNER JOIN usuarios u ON c.idUsuarios = u.idUsuarios "
                    + "WHERE CONCAT(u.Nombre, ' ', u.Paterno, ' ', u.Materno) = ? "
                    + "ORDER BY c.Fecha DESC, c.Hora DESC LIMIT 1";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, nombreCliente);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("Estado");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Sin citas";
    }

    // Método para obtener el ID del usuario seleccionado
    private int obtenerIdUsuarioSeleccionado(int filaTabla) {
        String nombreCompleto = jTablecontenidocitas.getValueAt(filaTabla, 0).toString();

        try (Connection con = ConexionBD.getConnection()) {
            // Separar el nombre completo en partes
            String[] partesNombre = nombreCompleto.split(" ");
            String nombre = partesNombre[0];
            String paterno = partesNombre.length > 1 ? partesNombre[1] : "";
            String materno = partesNombre.length > 2 ? partesNombre[2] : "";

            String sql = "SELECT idUsuarios FROM usuarios WHERE Nombre = ? AND Paterno = ? AND Materno = ?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, nombre);
            ps.setString(2, paterno);
            ps.setString(3, materno);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("idUsuarios");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    // Método para filtrar clientes por fecha de registro
    private void filtrarPorFecha() {
        String fechaSeleccionada = (String) jComboBox1Fecha.getSelectedItem();
        if (fechaSeleccionada == null || fechaSeleccionada.equals("Todas las fechas")) {
            cargarClientesTabla();
            return;
        }

        DefaultTableModel model = (DefaultTableModel) jTablecontenidocitas.getModel();
        model.setRowCount(0); // Limpiar tabla

        try (Connection con = ConexionBD.getConnection()) {
            String sql = "SELECT u.idUsuarios, u.Nombre, u.Paterno, u.Materno, u.Telefono, u.Correo, u.fecha_registro, "
                    + "COUNT(c.idCita) as numero_citas "
                    + "FROM usuarios u "
                    + "LEFT JOIN cita c ON u.idUsuarios = c.idUsuarios "
                    + "WHERE u.fecha_registro = ? "
                    + "GROUP BY u.idUsuarios, u.Nombre, u.Paterno, u.Materno, u.Telefono, u.Correo, u.fecha_registro "
                    + "ORDER BY u.Nombre";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, fechaSeleccionada);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String nombreCompleto = rs.getString("Nombre") + " "
                        + rs.getString("Paterno") + " "
                        + rs.getString("Materno");

                model.addRow(new Object[]{
                    nombreCompleto,
                    rs.getString("Telefono"),
                    rs.getString("Correo"),
                    rs.getDate("fecha_registro"),
                    rs.getInt("numero_citas")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al filtrar por fecha: " + e.getMessage());
        }
    }

    // BOTÓN VER - Mostrar detalles del cliente
    private void verCliente() {
        int fila = jTablecontenidocitas.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona un cliente para ver los detalles");
            return;
        }

        String nombre = jTablecontenidocitas.getValueAt(fila, 0).toString();
        String telefono = jTablecontenidocitas.getValueAt(fila, 1).toString();
        String correo = jTablecontenidocitas.getValueAt(fila, 2).toString();
        String fechaRegistro = jTablecontenidocitas.getValueAt(fila, 3).toString();
        String numeroCitas = jTablecontenidocitas.getValueAt(fila, 4).toString();
        String estado = txtEstadocita.getText();

        String mensaje = "Detalles del Cliente:\n\n"
                + "Nombre: " + nombre + "\n"
                + "Teléfono: " + telefono + "\n"
                + "Correo: " + correo + "\n"
                + "Fecha Registro: " + fechaRegistro + "\n"
                + "Número de Citas: " + numeroCitas + "\n"
                + "Estado Última Cita: " + estado;

        JOptionPane.showMessageDialog(this, mensaje, "Detalles del Cliente", JOptionPane.INFORMATION_MESSAGE);
    }

    // BOTÓN EDITAR - Editar cliente
    private void editarCliente() {
        int fila = jTablecontenidocitas.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Por favor, selecciona un cliente de la tabla para editar");
            return;
        }

        // Obtener el ID del usuario seleccionado
        int idUsuario = obtenerIdUsuarioSeleccionado(fila);

        if (idUsuario == -1) {
            JOptionPane.showMessageDialog(this, "No se pudo identificar el cliente seleccionado");
            return;
        }

        // Obtener datos actuales del cliente
        String nombreActual = jTablecontenidocitas.getValueAt(fila, 0).toString();
        String telefonoActual = jTablecontenidocitas.getValueAt(fila, 1).toString();
        String correoActual = jTablecontenidocitas.getValueAt(fila, 2).toString();

        // Separar nombre completo en partes
        String[] partesNombre = nombreActual.split(" ");
        String nombre = partesNombre.length > 0 ? partesNombre[0] : "";
        String paterno = partesNombre.length > 1 ? partesNombre[1] : "";
        String materno = partesNombre.length > 2 ? partesNombre[2] : "";

        // Mostrar mensaje de edición
        JOptionPane.showMessageDialog(this,
                "Editando cliente:\n\n"
                + "Nombre: " + nombreActual + "\n"
                + "Teléfono: " + telefonoActual + "\n"
                + "Correo: " + correoActual,
                "Editando Cliente",
                JOptionPane.INFORMATION_MESSAGE);

        // Pedir nuevos datos uno por uno usando JOptionPane
        String nuevoNombre = (String) JOptionPane.showInputDialog(
                this,
                "Ingrese el nuevo nombre:",
                "Editar Nombre",
                JOptionPane.QUESTION_MESSAGE,
                null,
                null,
                nombre
        );

        // Si el usuario cancela, salir
        if (nuevoNombre == null) {
            JOptionPane.showMessageDialog(this, "Edición cancelada");
            return;
        }

        String nuevoPaterno = (String) JOptionPane.showInputDialog(
                this,
                "Ingrese el nuevo apellido paterno:",
                "Editar Apellido Paterno",
                JOptionPane.QUESTION_MESSAGE,
                null,
                null,
                paterno
        );

        if (nuevoPaterno == null) {
            JOptionPane.showMessageDialog(this, "Edición cancelada");
            return;
        }

        String nuevoMaterno = (String) JOptionPane.showInputDialog(
                this,
                "Ingrese el nuevo apellido materno:",
                "Editar Apellido Materno",
                JOptionPane.QUESTION_MESSAGE,
                null,
                null,
                materno
        );

        if (nuevoMaterno == null) {
            JOptionPane.showMessageDialog(this, "Edición cancelada");
            return;
        }

        String nuevoTelefono = (String) JOptionPane.showInputDialog(
                this,
                "Ingrese el nuevo teléfono:",
                "Editar Teléfono",
                JOptionPane.QUESTION_MESSAGE,
                null,
                null,
                telefonoActual
        );

        if (nuevoTelefono == null) {
            JOptionPane.showMessageDialog(this, "Edición cancelada");
            return;
        }

        String nuevoCorreo = (String) JOptionPane.showInputDialog(
                this,
                "Ingrese el nuevo correo electrónico:",
                "Editar Correo",
                JOptionPane.QUESTION_MESSAGE,
                null,
                null,
                correoActual
        );

        if (nuevoCorreo == null) {
            JOptionPane.showMessageDialog(this, "Edición cancelada");
            return;
        }

        // Validar que ningún campo esté vacío
        if (nuevoNombre.trim().isEmpty() || nuevoPaterno.trim().isEmpty()
                || nuevoMaterno.trim().isEmpty() || nuevoTelefono.trim().isEmpty()
                || nuevoCorreo.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Todos los campos son obligatorios");
            return;
        }

        // Validar formato básico de correo
        if (!nuevoCorreo.contains("@") || !nuevoCorreo.contains(".")) {
            JOptionPane.showMessageDialog(this, "Por favor ingresa un correo electrónico válido");
            return;
        }

        // Mostrar resumen de cambios CON BOTONES EN ESPAÑOL
        Object[] opciones = {"Sí, guardar", "No, cancelar"};
        int confirmar = JOptionPane.showOptionDialog(
                this,
                "¿Confirmar los siguientes cambios?\n\n"
                + "Nombre: " + nombre + " " + paterno + " " + materno + " → "
                + nuevoNombre + " " + nuevoPaterno + " " + nuevoMaterno + "\n"
                + "Teléfono: " + telefonoActual + " → " + nuevoTelefono + "\n"
                + "Correo: " + correoActual + " → " + nuevoCorreo,
                "Confirmar Cambios",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                opciones,
                opciones[1] // Opción por defecto: "No, cancelar"
        );

        // confirmar = 0 para "Sí, guardar", 1 para "No, cancelar"
        if (confirmar == 0) {
            // Actualizar en la base de datos
            try (Connection con = ConexionBD.getConnection()) {
                // Verificar si el correo ya existe en otro usuario
                String sqlVerificar = "SELECT idUsuarios FROM usuarios WHERE Correo = ? AND idUsuarios != ?";
                PreparedStatement psVerificar = con.prepareStatement(sqlVerificar);
                psVerificar.setString(1, nuevoCorreo);
                psVerificar.setInt(2, idUsuario);
                ResultSet rs = psVerificar.executeQuery();

                if (rs.next()) {
                    JOptionPane.showMessageDialog(this, "El correo electrónico ya está registrado por otro usuario");
                    return;
                }

                // Actualizar datos
                String sqlActualizar = "UPDATE usuarios SET Nombre = ?, Paterno = ?, Materno = ?, Telefono = ?, Correo = ? WHERE idUsuarios = ?";
                PreparedStatement ps = con.prepareStatement(sqlActualizar);
                ps.setString(1, nuevoNombre.trim());
                ps.setString(2, nuevoPaterno.trim());
                ps.setString(3, nuevoMaterno.trim());
                ps.setString(4, nuevoTelefono.trim());
                ps.setString(5, nuevoCorreo.trim());
                ps.setInt(6, idUsuario);

                int filasAfectadas = ps.executeUpdate();

                if (filasAfectadas > 0) {
                    JOptionPane.showMessageDialog(this, " Cliente actualizado correctamente");
                    cargarClientesTabla(); // Refrescar la tabla
                    cargarNombresComboBox(); // Actualizar combo box si es necesario
                } else {
                    JOptionPane.showMessageDialog(this, "No se pudo actualizar el cliente");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error al actualizar cliente: " + e.getMessage());
            }
        } else {
            JOptionPane.showMessageDialog(this, " Edición cancelada");
        }
    }

// BOTÓN ELIMINAR - Eliminar cliente (en ESPAÑOL)
    private void eliminarCliente() {
        int fila = jTablecontenidocitas.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Por favor, selecciona un cliente de la tabla para eliminar");
            return;
        }

        String nombre = jTablecontenidocitas.getValueAt(fila, 0).toString();
        String telefono = jTablecontenidocitas.getValueAt(fila, 1).toString();
        String correo = jTablecontenidocitas.getValueAt(fila, 2).toString();

        // Crear opciones personalizadas en ESPAÑOL
        Object[] opciones = {"Sí, eliminar", "No, cancelar"};

        int confirm = JOptionPane.showOptionDialog(this,
                "<html><b>¿Estás seguro de eliminar este cliente?</b><br><br>"
                + "Nombre: " + nombre + "<br>"
                + "Teléfono: " + telefono + "<br>"
                + "Correo: " + correo + "<br><br>"
                + "<font color='red'>¡ESTA ACCIÓN ELIMINARÁ TODAS SUS CITAS Y NO SE PUEDE DESHACER!</font></html>",
                "Confirmar Eliminación de Cliente",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                opciones, // Usar nuestras opciones personalizadas
                opciones[1]); // Opción por defecto: "No, cancelar"

        // confirm = 0 para "Sí, eliminar", 1 para "No, cancelar"
        if (confirm == 0) {
            // Proceder con la eliminación - usuario hizo clic en "Sí, eliminar"
            try (Connection con = ConexionBD.getConnection()) {
                int idUsuario = obtenerIdUsuarioSeleccionado(fila);

                if (idUsuario != -1) {
                    // Primero eliminamos las citas asociadas al usuario
                    String sqlEliminarCitas = "DELETE FROM cita WHERE idUsuarios = ?";
                    PreparedStatement psCitas = con.prepareStatement(sqlEliminarCitas);
                    psCitas.setInt(1, idUsuario);
                    int citasEliminadas = psCitas.executeUpdate();

                    // Luego eliminamos el usuario
                    String sqlEliminarUsuario = "DELETE FROM usuarios WHERE idUsuarios = ?";
                    PreparedStatement psUsuario = con.prepareStatement(sqlEliminarUsuario);
                    psUsuario.setInt(1, idUsuario);
                    int filasAfectadas = psUsuario.executeUpdate();

                    if (filasAfectadas > 0) {
                        JOptionPane.showMessageDialog(this,
                                "Cliente eliminado correctamente\n"
                                + "Citas eliminadas: " + citasEliminadas);
                        cargarClientesTabla();
                        cargarFechasComboBox();
                        cargarNombresComboBox();
                        txtEstadocita.setText("");
                    } else {
                        JOptionPane.showMessageDialog(this, "No se pudo eliminar el cliente");
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "No se pudo identificar el cliente a eliminar");
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error al eliminar cliente: " + e.getMessage());
            }
        } else {
            // Usuario hizo clic en "No, cancelar" o cerró el diálogo
            JOptionPane.showMessageDialog(this, "Eliminación cancelada - El cliente se mantiene en el sistema");
        }
    }

    // Limpiar campos
    private void limpiarCampos() {
        txtTelefono.setText("");
        txtCorreo.setText("");
        jComboBox1Fecha.setSelectedIndex(0);
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
        jComboBox1Fecha = new javax.swing.JComboBox<>();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTablecontenidocitas = new javax.swing.JTable();
        btnver = new javax.swing.JButton();
        btnEditar = new javax.swing.JButton();
        btneliminar = new javax.swing.JButton();
        txtEstadocita = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jPanel6 = new javax.swing.JPanel();
        INS = new javax.swing.JLabel();
        FACE = new javax.swing.JLabel();
        WPP = new javax.swing.JLabel();
        jComboBoxNombreClie = new javax.swing.JComboBox<>();
        btnRegresar = new javax.swing.JButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu3 = new javax.swing.JMenu();
        jMenu10 = new javax.swing.JMenu();
        jMenuItem8 = new javax.swing.JMenuItem();
        jMenuItem9 = new javax.swing.JMenuItem();
        jMenuItem10 = new javax.swing.JMenuItem();
        jMenuItem11 = new javax.swing.JMenuItem();
        jMenu5 = new javax.swing.JMenu();
        jMenuItem6 = new javax.swing.JMenuItem();
        jMenuItem7 = new javax.swing.JMenuItem();
        jMenu6 = new javax.swing.JMenu();
        jMenuItem4 = new javax.swing.JMenuItem();
        jMenu7 = new javax.swing.JMenu();
        jMenuItem5 = new javax.swing.JMenuItem();
        jMenu16 = new javax.swing.JMenu();
        jMenuItemCerrarSecion = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(243, 224, 255));
        jPanel1.setFocusTraversalPolicyProvider(true);

        jLabel11.setFont(new java.awt.Font("Serif", 3, 14)); // NOI18N
        jLabel11.setText("REGISTRO DE CLIENTES");

        jLabel1.setText("Fecha");

        jComboBox1Fecha.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jComboBox1Fecha.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Fecha" }));
        jComboBox1Fecha.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox1FechaActionPerformed(evt);
            }
        });

        jLabel2.setText("Nombre");

        jTablecontenidocitas.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "Nombre del Cliente", "Teléfono", "Correo Electrónico", "Fecha Registro", "Número de citas"
            }
        ));
        jScrollPane1.setViewportView(jTablecontenidocitas);

        btnver.setBackground(new java.awt.Color(255, 204, 255));
        btnver.setText("Ver");
        btnver.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnverActionPerformed(evt);
            }
        });

        btnEditar.setBackground(new java.awt.Color(255, 204, 255));
        btnEditar.setText("Editar");
        btnEditar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEditarActionPerformed(evt);
            }
        });

        btneliminar.setBackground(new java.awt.Color(255, 204, 255));
        btneliminar.setText("Eliminar");
        btneliminar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btneliminarActionPerformed(evt);
            }
        });

        txtEstadocita.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtEstadocitaActionPerformed(evt);
            }
        });

        jLabel5.setText("Estado de cita");

        jPanel6.setBackground(new java.awt.Color(204, 0, 204));

        INS.setText("INS");

        FACE.setText("FACE");

        WPP.setText("WPP");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(INS, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(123, 123, 123)
                .addComponent(WPP, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(148, 148, 148)
                .addComponent(FACE, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(267, 267, 267))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(INS)
                    .addComponent(WPP)
                    .addComponent(FACE))
                .addContainerGap(25, Short.MAX_VALUE))
        );

        jComboBoxNombreClie.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jComboBoxNombreClie.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Fecha" }));
        jComboBoxNombreClie.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxNombreClieActionPerformed(evt);
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
                .addGap(48, 48, 48)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jComboBox1Fecha, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(115, 115, 115)
                                .addComponent(jLabel11))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(31, 31, 31)
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jComboBoxNombreClie, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(62, 62, 62)
                                .addComponent(jLabel5)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(txtEstadocita, javax.swing.GroupLayout.PREFERRED_SIZE, 181, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 566, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(67, 67, 67)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(btnEditar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btneliminar, javax.swing.GroupLayout.DEFAULT_SIZE, 83, Short.MAX_VALUE)
                            .addComponent(btnver, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btnRegresar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap(18, Short.MAX_VALUE))
            .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(34, 34, 34)
                .addComponent(jLabel11)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 47, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(85, 85, 85)
                        .addComponent(btnver)
                        .addGap(18, 18, 18)
                        .addComponent(btnEditar)
                        .addGap(28, 28, 28)
                        .addComponent(btneliminar)
                        .addGap(20, 20, 20)
                        .addComponent(btnRegresar))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jComboBox1Fecha, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel1)
                            .addComponent(jLabel2)
                            .addComponent(txtEstadocita, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel5)
                            .addComponent(jComboBoxNombreClie, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(43, 43, 43)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 233, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(31, 31, 31)
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jMenu3.setText("LOGO");
        jMenuBar1.add(jMenu3);

        jMenu10.setText("CATALÓGO");

        jMenuItem8.setText("Uñas");
        jMenuItem8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem8ActionPerformed(evt);
            }
        });
        jMenu10.add(jMenuItem8);

        jMenuItem9.setText("Peinados");
        jMenuItem9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem9ActionPerformed(evt);
            }
        });
        jMenu10.add(jMenuItem9);

        jMenuItem10.setText("Maquillaje");
        jMenuItem10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem10ActionPerformed(evt);
            }
        });
        jMenu10.add(jMenuItem10);

        jMenuItem11.setText("Otros");
        jMenuItem11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem11ActionPerformed(evt);
            }
        });
        jMenu10.add(jMenuItem11);

        jMenuBar1.add(jMenu10);

        jMenu5.setText("AGENDAR CITA");

        jMenuItem6.setText("Agendar Cita");
        jMenuItem6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem6ActionPerformed(evt);
            }
        });
        jMenu5.add(jMenuItem6);

        jMenuItem7.setText("Cancelar cita");
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
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnverActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnverActionPerformed
        // TODO add your handling code here:
        //  cargarCitas();
        verCliente();
    }//GEN-LAST:event_btnverActionPerformed

    private void txtEstadocitaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtEstadocitaActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtEstadocitaActionPerformed

    private void jComboBox1FechaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1FechaActionPerformed
        // TODO add your handling code here:
        String fechaSeleccionada = (String) jComboBox1Fecha.getSelectedItem();
        if (fechaSeleccionada != null && !fechaSeleccionada.equals("Todas las fechas")) {
            filtrarPorFecha();
        } else {
            cargarClientesTabla();
        }
    }//GEN-LAST:event_jComboBox1FechaActionPerformed

    private void btnEditarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditarActionPerformed
        // TODO add your handling code here:
        editarCliente();

    }//GEN-LAST:event_btnEditarActionPerformed

    private void btneliminarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btneliminarActionPerformed
        eliminarCliente();

    }//GEN-LAST:event_btneliminarActionPerformed

    private void jMenuItem6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem6ActionPerformed
        // TODO add your handling code here:
        NewJAgendarcita NewJAgendarcita = new NewJAgendarcita();
        NewJAgendarcita.setVisible(true);
        this.dispose(); // cierra la actual

    }//GEN-LAST:event_jMenuItem6ActionPerformed

    private void jMenuItem7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem7ActionPerformed
        // TODO add your handling code here:
        JOptionPane.showMessageDialog(this, "Funcionalidad de cancelar cita");

    }//GEN-LAST:event_jMenuItem7ActionPerformed

    private void jComboBoxNombreClieActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxNombreClieActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jComboBoxNombreClieActionPerformed

    private void jMenuItem5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem5ActionPerformed
        // TODO add your handling code here:
        NewJLogin NewJLogin = new NewJLogin();
        NewJLogin.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem5ActionPerformed

    private void btnRegresarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRegresarActionPerformed

        NewJPanelAdministracion NewJPanelAdministracion = new NewJPanelAdministracion();
        NewJPanelAdministracion.setVisible(true);
        this.dispose(); // cierra la actual

    }//GEN-LAST:event_btnRegresarActionPerformed

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
            java.util.logging.Logger.getLogger(NewJRegClient.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(NewJRegClient.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(NewJRegClient.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(NewJRegClient.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new NewJRegClient().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel FACE;
    private javax.swing.JLabel INS;
    private javax.swing.JLabel WPP;
    private javax.swing.JButton btnEditar;
    private javax.swing.JButton btnRegresar;
    private javax.swing.JButton btneliminar;
    private javax.swing.JButton btnver;
    private javax.swing.JComboBox<String> jComboBox1Fecha;
    private javax.swing.JComboBox<String> jComboBoxNombreClie;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JMenu jMenu10;
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
    private javax.swing.JMenuItem jMenuItem6;
    private javax.swing.JMenuItem jMenuItem7;
    private javax.swing.JMenuItem jMenuItem8;
    private javax.swing.JMenuItem jMenuItem9;
    private javax.swing.JMenuItem jMenuItemCerrarSecion;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTablecontenidocitas;
    private javax.swing.JTextField txtEstadocita;
    // End of variables declaration//GEN-END:variables
}
