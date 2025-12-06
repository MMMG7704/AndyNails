/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package Interfaces;

import Interfaces.NewJAgenC;
import andynails.ConexionBD;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import com.toedter.calendar.JCalendar;
import java.time.LocalDate;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.HashMap;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.ArrayList;
import Interfaces.NewJAgendarcita;
import andynails.DisponibilidadManager;
import andynails.SesionUsuario;

/**
 *
 * @author User
 */
public class NewJCitaAgenda extends javax.swing.JFrame {

    ConexionBD conexion;
    private boolean modoNuevaCita = false;

    DisponibilidadManager dispManager;

    public NewJCitaAgenda() {
        initComponents();
        conexion = new ConexionBD();
        dispManager = new DisponibilidadManager(conexion);

        llenarComboRoles();

        jTablecontenidocitas.getColumnModel().getColumn(0).setMinWidth(0);
        jTablecontenidocitas.getColumnModel().getColumn(0).setMaxWidth(0);
        jTablecontenidocitas.getColumnModel().getColumn(0).setWidth(0);

        CalCitasAgendadas.addPropertyChangeListener("calendar", evt -> {
            Date fechaSeleccionada = CalCitasAgendadas.getCalendar().getTime();
            if (fechaSeleccionada != null) {
                cargarCitasPorFecha(fechaSeleccionada);
            }
        });

        // Cargar día actual
        cargarCitasPorFecha(new Date());
        modoNuevaCita = true;
    }

    private JFrame ventanaAnterior;

    public NewJCitaAgenda(JFrame anterior) {
        initComponents();
        this.ventanaAnterior = anterior;

        conexion = new ConexionBD();
        dispManager = new DisponibilidadManager(conexion);

        llenarComboRoles(); // llenar roles
        jTablecontenidocitas.getColumnModel().getColumn(0).setMinWidth(0);
        jTablecontenidocitas.getColumnModel().getColumn(0).setMaxWidth(0);
        jTablecontenidocitas.getColumnModel().getColumn(0).setWidth(0);

        CalCitasAgendadas.addPropertyChangeListener("calendar", evt -> {
            Date fechaSeleccionada = CalCitasAgendadas.getCalendar().getTime();
            if (fechaSeleccionada != null) {
                cargarCitasPorFecha(fechaSeleccionada);
            }
        });

        cargarCitasPorFecha(new Date()); // cargar día actual
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

    private void regresar() {
        if (ventanaAnterior != null) {
            ventanaAnterior.setVisible(true);
        }
        this.dispose();
    }

    private void cargarServiciosEnCombo() {
        comboRoles.removeAllItems();
        Map<Integer, String> servicios = dispManager.obtenerServicios();
        comboRoles.addItem("Todos"); // opción para mostrar todas las citas
        for (String nombre : servicios.values()) {
            comboRoles.addItem(nombre); // cada servicio disponible
        }
    }

    private void actualizarTabla() {
        Date fecha = CalCitasAgendadas.getCalendar().getTime();
        if (fecha != null) {
            cargarCitasPorFecha(fecha);
        }
    }

    private void llenarComboRoles() {
        comboRoles.removeAllItems();
        comboRoles.addItem("Todos");

        String sql = "SELECT Nombre_servicio FROM servicios ORDER BY Nombre_servicio ASC";

        try (Connection con = conexion.conectar(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                comboRoles.addItem(rs.getString("Nombre_servicio"));
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar servicios: " + e.getMessage());
        }

        comboRoles.addActionListener(e -> {
            Date fecha = CalCitasAgendadas.getCalendar().getTime();
            if (fecha != null) {
                cargarCitasPorFecha(fecha);
            }
        });
    }

    private void filtrarServiciosPorRol(String rol) {
        // Guardar listener actual
        ActionListener[] listeners = comboRoles.getActionListeners();

        comboRoles.removeAllItems(); // solo borra items
        comboRoles.addItem("Todos");

        String sql = "SELECT Nombre_servicio FROM Servicios";
        if (rol.equals("Manicurista")) {
            sql += " WHERE Nombre_servicio LIKE '%Manicure%'";
        } else if (rol.equals("Estilista")) {
            sql += " WHERE Nombre_servicio LIKE '%Peinado%'";
        } else if (rol.equals("Maquillista")) {
            sql += " WHERE Nombre_servicio LIKE '%Maquillaje%'";
        }

        try (Connection con = conexion.conectar(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                comboRoles.addItem(rs.getString("Nombre_servicio"));
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar servicios: " + e.getMessage());
        }

        for (ActionListener l : listeners) {
            comboRoles.addActionListener(l);
        }
    }

    private void cargarCitasPorFecha(Date fechaSeleccionada) {
        DefaultTableModel model = (DefaultTableModel) jTablecontenidocitas.getModel();
        model.setRowCount(0);

        if (fechaSeleccionada == null) {
            return;
        }

        String servicioSeleccionado = comboRoles.getSelectedItem() != null
                ? comboRoles.getSelectedItem().toString()
                : "Todos";

        String sql = """
    SELECT c.idCita,
           CONCAT(u.Nombre, ' ', IFNULL(u.Paterno, ''), ' ', IFNULL(u.Materno, '')) AS Cliente,
           u.Telefono, u.Correo,
           s.Nombre_servicio AS Servicio,
           c.Fecha, c.Hora,
           CASE 
                WHEN chs.Pago_idPago IS NULL THEN 'Pendiente'
                WHEN p.Estado_pago = 'Validado' THEN 'Agendada'
                ELSE 'Pendiente'
           END AS Estado
    FROM cita c
    INNER JOIN usuarios u ON c.idUsuarios = u.idUsuarios
    INNER JOIN cita_has_servicios chs ON c.idCita = chs.idCita
    INNER JOIN servicios s ON chs.idServicios = s.idServicios
    LEFT JOIN pago p ON chs.Pago_idPago = p.idPago
    WHERE DATE(c.Fecha) = ?
""";

        if (!servicioSeleccionado.equals("Todos")) {
            sql += " AND s.Nombre_servicio = ?";
        }

        sql += " ORDER BY c.Hora";

        try (Connection con = conexion.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setDate(1, new java.sql.Date(fechaSeleccionada.getTime()));

            if (!servicioSeleccionado.equals("Todos")) {
                ps.setString(2, servicioSeleccionado);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    model.addRow(new Object[]{
                        rs.getInt("idCita"), // Columna 0: ID (oculta)
                        rs.getString("Cliente"), // Columna 1: Nombre
                        rs.getString("Telefono"), // Columna 2: Teléfono
                        rs.getString("Correo"), // Columna 3: Correo
                        rs.getString("Servicio"), // Columna 4: Servicio
                        rs.getDate("Fecha"), // Columna 5: Fecha
                        rs.getString("Hora"), // Columna 6: Hora
                        rs.getString("Estado") // Columna 7: Estado
                    });
                }

                jTablecontenidocitas.getColumnModel().getColumn(0).setMinWidth(0);
                jTablecontenidocitas.getColumnModel().getColumn(0).setMaxWidth(0);
                jTablecontenidocitas.getColumnModel().getColumn(0).setWidth(0);

                jTablecontenidocitas.getTableHeader().getColumnModel().getColumn(0).setHeaderValue("");
                jTablecontenidocitas.getTableHeader().repaint();
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar citas: " + e.getMessage());
        }

    }

    /**
     * Creates new form NewJCitaAgenda
     */
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jCalendar1 = new com.toedter.calendar.JCalendar();
        jPanel1 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        btnEditarCita = new javax.swing.JButton();
        btnEliminarCita = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTablecontenidocitas = new javax.swing.JTable();
        comboRoles = new javax.swing.JComboBox<>();
        btnRegresar = new javax.swing.JButton();
        CalCitasAgendadas = new com.toedter.calendar.JCalendar();
        btnRegistrarCita = new javax.swing.JButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu4 = new javax.swing.JMenu();
        jMenu5 = new javax.swing.JMenu();
        menuBuscarCita = new javax.swing.JMenuItem();
        menuAgendaCitas = new javax.swing.JMenuItem();
        menuAgendarCita = new javax.swing.JMenuItem();
        lognmenu = new javax.swing.JMenu();
        jMenuItem4 = new javax.swing.JMenuItem();
        jMenu16 = new javax.swing.JMenu();
        jMenuItemCerrarSecion3 = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(243, 224, 255));

        jPanel4.setBackground(new java.awt.Color(204, 0, 204));

        jLabel3.setText("INS");

        jLabel6.setText("FACE");

        jLabel7.setText("WPP");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(115, 115, 115)
                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(190, 190, 190)
                .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 973, Short.MAX_VALUE)
                .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(79, 79, 79))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jLabel7)
                    .addComponent(jLabel6))
                .addContainerGap(17, Short.MAX_VALUE))
        );

        jLabel2.setFont(new java.awt.Font("Serif", 3, 14)); // NOI18N
        jLabel2.setText("CITAS AGENDADAS");

        btnEditarCita.setBackground(new java.awt.Color(255, 204, 255));
        btnEditarCita.setText("Editar cita");
        btnEditarCita.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEditarCitaActionPerformed(evt);
            }
        });

        btnEliminarCita.setBackground(new java.awt.Color(255, 204, 255));
        btnEliminarCita.setText("Eliminar cita");
        btnEliminarCita.setToolTipText("");
        btnEliminarCita.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEliminarCitaActionPerformed(evt);
            }
        });

        jTablecontenidocitas.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null}
            },
            new String [] {
                "Id Cita", "Nombre del Cliente", "Teléfono", "Correo Electrónico", "Nombre de servicio", "Fecha ", "Hora", "Estado"
            }
        ));
        jScrollPane1.setViewportView(jTablecontenidocitas);

        comboRoles.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        btnRegresar.setBackground(new java.awt.Color(255, 204, 255));
        btnRegresar.setText("Regresar");
        btnRegresar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRegresarActionPerformed(evt);
            }
        });

        btnRegistrarCita.setBackground(new java.awt.Color(255, 204, 255));
        btnRegistrarCita.setText("Registrar cita");
        btnRegistrarCita.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRegistrarCitaActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(408, 408, 408)
                        .addComponent(comboRoles, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(315, 315, 315)
                        .addComponent(jLabel2))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(66, 66, 66)
                                .addComponent(btnRegistrarCita)
                                .addGap(60, 60, 60)
                                .addComponent(btnEditarCita, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(72, 72, 72)
                                .addComponent(btnEliminarCita))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(45, 45, 45)
                                .addComponent(CalCitasAgendadas, javax.swing.GroupLayout.PREFERRED_SIZE, 401, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 935, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(51, 51, 51)
                                .addComponent(btnRegresar)))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(comboRoles, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 233, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGap(30, 30, 30)
                        .addComponent(CalCitasAgendadas, javax.swing.GroupLayout.PREFERRED_SIZE, 221, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 37, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnEliminarCita)
                    .addComponent(btnEditarCita)
                    .addComponent(btnRegresar)
                    .addComponent(btnRegistrarCita))
                .addGap(18, 18, 18)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
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

        menuBuscarCita.setText("Buscar citas");
        menuBuscarCita.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuBuscarCitaActionPerformed(evt);
            }
        });
        jMenu5.add(menuBuscarCita);

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

        lognmenu.setText("LOGIN");

        jMenuItem4.setText("login");
        jMenuItem4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem4ActionPerformed(evt);
            }
        });
        lognmenu.add(jMenuItem4);

        jMenuBar1.add(lognmenu);

        jMenu16.setText("CERRAR SESIÓN");

        jMenuItemCerrarSecion3.setText("Cerrar sesión");
        jMenuItemCerrarSecion3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemCerrarSecion3jMenuItemCerrarSecionActionPerformed(evt);
            }
        });
        jMenu16.add(jMenuItemCerrarSecion3);

        jMenuBar1.add(jMenu16);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents


    private void btnEditarCitaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditarCitaActionPerformed

        int fila = jTablecontenidocitas.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona una cita para editar.");
            return;
        }

        Object valId = jTablecontenidocitas.getValueAt(fila, 0);
        if (valId == null) {
            JOptionPane.showMessageDialog(this, "Id de cita inválido.");
            return;
        }

        int idCita;
        try {
            idCita = Integer.parseInt(valId.toString());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Id de cita no es numérico.");
            return;
        }

        NewJAgendarcitaREC editor = new NewJAgendarcitaREC(idCita);
        editor.setVisible(true);
        this.dispose();


    }//GEN-LAST:event_btnEditarCitaActionPerformed

    private void btnEliminarCitaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEliminarCitaActionPerformed
        int fila = jTablecontenidocitas.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona una cita para eliminar.");
            return;
        }

        // Obtener idCita
        Object valorId = jTablecontenidocitas.getValueAt(fila, 0);
        int idCita;

        try {
            idCita = Integer.parseInt(valorId.toString());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al leer el ID de la cita.");
            return;
        }

        // 🔹 MOSTRAR CONFIRMACIÓN EN ESPAÑOL CON BOTONES "SÍ" Y "NO"
        Object[] opciones = {"Sí", "No"};
        int confirm = JOptionPane.showOptionDialog(
                this,
                "¿Estás seguro de que deseas eliminar esta cita?",
                "Confirmar eliminación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                opciones,
                opciones[1] // Opción por defecto ("No")
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try (Connection con = conexion.conectar()) {
            if (con == null) {
                JOptionPane.showMessageDialog(this, "No se pudo conectar a la base de datos.");
                return;
            }

            // Desactivar temporalmente las verificaciones de claves foráneas
            try (Statement stmt = con.createStatement()) {
                stmt.execute("SET FOREIGN_KEY_CHECKS=0");
            }

            String sqlPagosServicios = "SELECT Pago_idPago FROM cita_has_servicios WHERE idCita = ?";
            List<Integer> pagosServicios = new ArrayList<>();

            try (PreparedStatement ps = con.prepareStatement(sqlPagosServicios)) {
                ps.setInt(1, idCita);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    int pagoId = rs.getInt("Pago_idPago");
                    if (!rs.wasNull()) { // Solo agregar si no es NULL
                        pagosServicios.add(pagoId);
                    }
                }
            }

            Integer pagoPrincipal = null;
            String sqlPagoPrincipal = "SELECT Pago_idPago FROM Cita WHERE idCita = ?";

            try (PreparedStatement ps = con.prepareStatement(sqlPagoPrincipal)) {
                ps.setInt(1, idCita);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    pagoPrincipal = rs.getInt("Pago_idPago");
                    if (rs.wasNull()) {
                        pagoPrincipal = null;
                    }
                }
            }

            String sqlDeleteServicios = "DELETE FROM cita_has_servicios WHERE idCita = ?";
            try (PreparedStatement ps = con.prepareStatement(sqlDeleteServicios)) {
                ps.setInt(1, idCita);
                ps.executeUpdate();
            }

            if (!pagosServicios.isEmpty()) {
                String sqlDeletePago = "DELETE FROM pago WHERE idPago = ?";
                try (PreparedStatement ps = con.prepareStatement(sqlDeletePago)) {
                    for (Integer idPago : pagosServicios) {
                        ps.setInt(1, idPago);
                        ps.executeUpdate();
                    }
                }
            }

            if (pagoPrincipal != null) {
                String sqlDeletePago = "DELETE FROM pago WHERE idPago = ?";
                try (PreparedStatement ps = con.prepareStatement(sqlDeletePago)) {
                    ps.setInt(1, pagoPrincipal);
                    ps.executeUpdate();
                }
            }

            String sqlDeleteCita = "DELETE FROM Cita WHERE idCita = ?";
            try (PreparedStatement ps = con.prepareStatement(sqlDeleteCita)) {
                ps.setInt(1, idCita);
                int filasAfectadas = ps.executeUpdate();

                if (filasAfectadas > 0) {
                    JOptionPane.showMessageDialog(this, "Cita eliminada correctamente.");
                } else {
                    JOptionPane.showMessageDialog(this, "No se pudo eliminar la cita.");
                }
            }

            try (Statement stmt = con.createStatement()) {
                stmt.execute("SET FOREIGN_KEY_CHECKS=1");
            }

            cargarCitasPorFecha(CalCitasAgendadas.getCalendar().getTime());

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Error al eliminar cita: " + e.getMessage()
                    + "\n\nPosible causa: La cita tiene registros relacionados que no se pueden eliminar.");
            e.printStackTrace();

            // Intentar reactivar las verificaciones en caso de error
            try (Connection con = conexion.conectar(); Statement stmt = con.createStatement()) {
                stmt.execute("SET FOREIGN_KEY_CHECKS=1");
            } catch (SQLException ex) {
                // Ignorar error secundario
            }
        }
    }//GEN-LAST:event_btnEliminarCitaActionPerformed

    private void jMenu4MenuSelected(javax.swing.event.MenuEvent evt) {//GEN-FIRST:event_jMenu4MenuSelected
        // TODO add your handling code here:
        //inicio
        Inicio Inicio = new Inicio();
        Inicio.setVisible(true);
        this.dispose(); // cierra la actual

    }//GEN-LAST:event_jMenu4MenuSelected

    private void menuBuscarCitaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuBuscarCitaActionPerformed
        // TODO add your handling code here:

        NewJBuscarCita buscar = new NewJBuscarCita(this);
        buscar.setVisible(true);
        this.setVisible(false);
    }//GEN-LAST:event_menuBuscarCitaActionPerformed

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

    private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem4ActionPerformed
        // TODO add your handling code here:

        NewJCancelarC NewJCancelarC = new NewJCancelarC();
        NewJCancelarC.setVisible(true);
        this.dispose(); // cierra la actual

    }//GEN-LAST:event_jMenuItem4ActionPerformed

    private void btnRegresarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRegresarActionPerformed
        // TODO add your handling code here:
       String nombreUsuario = SesionUsuario.getNombreUsuario();
    
    // Heurística simple basada en el nombre
    if (nombreUsuario != null && 
        (nombreUsuario.equalsIgnoreCase("admin") || 
         nombreUsuario.toLowerCase().contains("recepcion") ||
         nombreUsuario.equalsIgnoreCase("administrador"))) {
        // Suponer que es staff
        NewJPanelAdministracion adminPanel = new NewJPanelAdministracion();
        adminPanel.setVisible(true);
    } 
    else {
        // Suponer que es cliente
        NewJAgenC agendarCita = new NewJAgenC();
        agendarCita.setVisible(true);
    }
    
    this.dispose();
    
    }//GEN-LAST:event_btnRegresarActionPerformed

    private void btnRegistrarCitaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRegistrarCitaActionPerformed
        // TODO add your handling code here:

        NewJAgendarcitaREC registrar = new NewJAgendarcitaREC();
        registrar.setVisible(true);

        registrar.limpiarCampos(); 

        this.dispose();

    }//GEN-LAST:event_btnRegistrarCitaActionPerformed

    private void jMenuItemCerrarSecion3jMenuItemCerrarSecionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemCerrarSecion3jMenuItemCerrarSecionActionPerformed
        // TODO add your handling code here:
        andynails.SessionManager.cerrarSesion(this);
    }//GEN-LAST:event_jMenuItemCerrarSecion3jMenuItemCerrarSecionActionPerformed

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
            java.util.logging.Logger.getLogger(NewJCitaAgenda.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(NewJCitaAgenda.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(NewJCitaAgenda.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(NewJCitaAgenda.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
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
                new NewJCitaAgenda().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private com.toedter.calendar.JCalendar CalCitasAgendadas;
    private javax.swing.JButton btnEditarCita;
    private javax.swing.JButton btnEliminarCita;
    private javax.swing.JButton btnRegistrarCita;
    private javax.swing.JButton btnRegresar;
    private javax.swing.JComboBox<String> comboRoles;
    private com.toedter.calendar.JCalendar jCalendar1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JMenu jMenu16;
    private javax.swing.JMenu jMenu4;
    private javax.swing.JMenu jMenu5;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem jMenuItemCerrarSecion3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTablecontenidocitas;
    private javax.swing.JMenu lognmenu;
    private javax.swing.JMenuItem menuAgendaCitas;
    private javax.swing.JMenuItem menuAgendarCita;
    private javax.swing.JMenuItem menuBuscarCita;
    // End of variables declaration//GEN-END:variables

}
