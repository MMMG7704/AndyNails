package Interfaces;

import andynails.ConexionBD;
import andynails.RedesSociales;
import javax.swing.JFrame;
import javax.swing.table.DefaultTableModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.JOptionPane;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
/**
 *
 * @author User
 */
public class NewJPanelAdministracion extends javax.swing.JFrame {

    ConexionBD conexion;
    private javax.swing.JMenuItem jMenuItemCerrarSesion;

    private LocalDate fechaSeleccionada = LocalDate.now();
    private DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private void actualizarEncabezadosSemana(LocalDate fechaBase) {
        DefaultTableModel modelo = (DefaultTableModel) jTable2.getModel();

        // Obtener lunes de la semana
        LocalDate lunes = fechaBase.with(DayOfWeek.MONDAY);

        // Formato: dd/MM
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM");

        // Columnas: 0 = Hora, 1 = lunes ... 7 = domingo
        String[] dias = {"Hora", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};

        for (int i = 0; i < 8; i++) { // Ahora son 8 columnas
            if (i == 0) {
                // La primera columna es "Hora"
                jTable2.getColumnModel().getColumn(i).setHeaderValue(dias[i]);
            } else {
                // Las demás columnas son días con fecha
                LocalDate dia = lunes.plusDays(i - 1);
                String nombreColumna = dias[i] + " " + dia.format(formato);
                jTable2.getColumnModel().getColumn(i).setHeaderValue(nombreColumna);
            }
        }

        // Refrescar la tabla para que se vea el cambio
        jTable2.getTableHeader().repaint();
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

    /**
     * Creates new form NewJRegistro
     */
    public NewJPanelAdministracion() {
        initComponents();
        RedesSociales.configurarRedesSociales(INS, WPP, FACE);

        conexion = new ConexionBD("andynails");
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // === FECHA INICIAL ===
        fechaSeleccionada = LocalDate.now();

        // === ACTUALIZAR LABEL SUPERIOR DE FECHA ===
        actualizarLabelFecha();

        // === CONFIGURAR jDateInicio CON LA FECHA ACTUAL ===
        jDateInicio.setDate(java.sql.Date.valueOf(fechaSeleccionada));

        // === LISTENER PARA CAMBIOS EN jDateInicio ===
        jDateInicio.addPropertyChangeListener("date", evt -> {
            if (evt.getNewValue() != null) {
                filtrarPorRango();
            }
        });

        // === CONFIGURAR BOTONES DE NAVEGACIÓN ===
        jButtonAnterior.addActionListener(e -> cambiarSemana(-1));
        jButtonSiguiente.addActionListener(e -> cambiarSemana(1));

        // === TABLA PRINCIPAL AGENDA SEMANAL ===
        DefaultTableModel modelo = new DefaultTableModel();

        // Columnas: Hora + Lunes a Domingo (8 columnas en total)
        String[] columnas = {"Hora", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};
        for (String columna : columnas) {
            modelo.addColumn(columna);
        }

        // Filas con horas
        String[] horas = {
            "9:00", "10:00", "11:00", "12:00", "13:00", "14:00",
            "15:00", "16:00", "17:00", "18:00", "19:00", "20:00"
        };

        // Agregar las horas en la primera columna y dejar las demás vacías
        for (String hora : horas) {
            Object[] fila = new Object[8]; // 8 columnas
            fila[0] = hora; // Primera columna tiene la hora
            for (int i = 1; i < 8; i++) {
                fila[i] = ""; // Las demás columnas vacías
            }
            modelo.addRow(fila);
        }

        jTable2.setModel(modelo);

        // === TABLA DE RESUMEN CON NUEVOS INDICADORES ===
        DefaultTableModel modeloResumen = new DefaultTableModel(
                new Object[][]{
                    {"Citas para hoy", 0},
                    {"Clientes Registrados", 0},
                    {"Anticipo pendientes", 0},
                    {"Diseño en catálogo", 0},
                    {"Horarios bloqueados esta semana", 0}
                },
                new String[]{"Concepto", "Cantidad"}
        );
        jTable1.setModel(modeloResumen);

        // === CARGAR SERVICIOS EN EL COMBOBOX ===
        cargarServiciosEnCombo();

        // === CARGAR DATOS INICIALES ===
        actualizarEncabezadosSemana(fechaSeleccionada);
        cargarAgendaSemanal(fechaSeleccionada);
        actualizarResumen();
    }


    private void actualizarLabelFecha() {
        DateTimeFormatter formato = DateTimeFormatter.ofPattern(
                "EEEE, dd 'de' MMMM 'de' yyyy", new Locale("es", "ES")
        );
        lblFechaHoy.setText("Bienvenid@, Admin! Hoy es " + fechaSeleccionada.format(formato));
    }

    private void cambiarSemana(int desplazamiento) {
        fechaSeleccionada = fechaSeleccionada.plusWeeks(desplazamiento);
        actualizarLabelFecha();
        actualizarEncabezadosSemana(fechaSeleccionada);
        cargarAgendaSemanal(fechaSeleccionada);  // Esto ahora usará el servicio seleccionado
        actualizarResumen();
    }
// === MÉTODO PARA CARGAR LA AGENDA DE UNA SEMANA ===

    private void cargarAgendaSemanal(LocalDate fechaBase) {
        try (Connection cn = conexion.conectar()) {

            // Determinar lunes y domingo de la semana del filtro
            LocalDate lunes = fechaBase.with(DayOfWeek.MONDAY);
            LocalDate domingo = fechaBase.with(DayOfWeek.SUNDAY);

            // Limpiar tabla (manteniendo las horas en la primera columna)
            DefaultTableModel modelo = (DefaultTableModel) jTable2.getModel();
            for (int i = 0; i < modelo.getRowCount(); i++) {
                // La columna 0 (Hora) se mantiene, limpiamos solo las columnas 1-7
                for (int j = 1; j < modelo.getColumnCount(); j++) {
                    modelo.setValueAt("", i, j);
                }
            }

            // Obtener servicio seleccionado
            String servicioSeleccionado = (String) cmbservicio.getSelectedItem();

            // Construir la consulta SQL dinámicamente
            String sql = """
    SELECT 
        CONCAT(u.Nombre, ' ', u.Paterno, ' ', u.Materno) AS NombreCliente,
        s.Nombre_servicio,
        c.Fecha,
        c.Hora,
        c.Estado
    FROM cita c
    INNER JOIN usuarios u ON u.idUsuarios = c.idUsuarios
    INNER JOIN cita_has_servicios chs ON chs.idCita = c.idCita
    INNER JOIN servicios s ON s.idServicios = chs.idServicios
    WHERE c.Fecha BETWEEN ? AND ?
""";

            // Si no es "Todos", agregar filtro por servicio
            if (servicioSeleccionado != null && !servicioSeleccionado.equals("Todos")) {
                sql += " AND s.Nombre_servicio = ?";
            }

            sql += " ORDER BY c.Fecha, c.Hora";

            try (PreparedStatement ps = cn.prepareStatement(sql)) {
                ps.setDate(1, java.sql.Date.valueOf(lunes));
                ps.setDate(2, java.sql.Date.valueOf(domingo));

                // Si no es "Todos", establecer el parámetro del servicio
                if (servicioSeleccionado != null && !servicioSeleccionado.equals("Todos")) {
                    ps.setString(3, servicioSeleccionado);
                }

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String cliente = rs.getString("NombreCliente");
                        String nombreServicio = rs.getString("Nombre_servicio");
                        LocalDate fechaCita = rs.getDate("Fecha").toLocalDate();
                        String hora = rs.getString("Hora");
                        String estado = rs.getString("Estado");

                        // Ajustar el índice de la columna: 0=Hora, 1=Lunes, 2=Martes, etc.
                        int diaSemana = fechaCita.getDayOfWeek().getValue(); // 1 = Lunes, 7 = Domingo
                        int fila = obtenerFilaPorHora(hora);

                        if (fila != -1 && diaSemana >= 1 && diaSemana <= 7) {
                            String texto = cliente + " - " + nombreServicio;

                            if ("Completada".equalsIgnoreCase(estado)) {
                                texto += " (✔)";
                            } else if ("Pendiente".equalsIgnoreCase(estado)) {
                                texto += " (⏳)";
                            }

                            // Columna = diaSemana (1=Lunes -> columna 1, 7=Domingo -> columna 7)
                            modelo.setValueAt(texto, fila, diaSemana);
                        }
                    }
                }
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al cargar agenda: " + ex.getMessage());
        }
    }

    private void actualizarResumen() {
        try (Connection cn = conexion.conectar()) {
            DefaultTableModel modelo = (DefaultTableModel) jTable1.getModel();

            // 1. Citas para hoy
            actualizarContador(cn, modelo, 0, "SELECT COUNT(*) FROM cita WHERE Fecha = CURDATE()");

            // 2. Clientes Registrados
            actualizarContador(cn, modelo, 1, "SELECT COUNT(*) FROM usuarios WHERE Tipo_Usuario_idTipo_Usuario = 2");

            // 3. Anticipo pendientes - CORREGIDO (Estado_pago en lugar de Estado)
            actualizarContador(cn, modelo, 2, "SELECT COUNT(*) FROM pago WHERE Estado_pago = 'Pendiente'");

            // 4. Diseño en catálogo
            actualizarContador(cn, modelo, 3, "SELECT COUNT(*) FROM servicios");

            // 5. Horarios bloqueados esta semana
            LocalDate lunes = fechaSeleccionada.with(DayOfWeek.MONDAY);
            LocalDate domingo = fechaSeleccionada.with(DayOfWeek.SUNDAY);
            String sqlBloqueos = "SELECT COUNT(*) FROM bloqueo_horario WHERE Fecha BETWEEN ? AND ?";
            try (PreparedStatement ps = cn.prepareStatement(sqlBloqueos)) {
                ps.setDate(1, java.sql.Date.valueOf(lunes));
                ps.setDate(2, java.sql.Date.valueOf(domingo));
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        modelo.setValueAt(rs.getInt(1), 4, 1);
                    }
                }
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al actualizar resumen: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Método auxiliar para actualizar contadores
    private void actualizarContador(Connection cn, DefaultTableModel modelo, int fila, String sql) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                modelo.setValueAt(rs.getInt(1), fila, 1);
            }
        }
    }

    private int obtenerFilaPorHora(String hora) {
        String h = hora.startsWith("0") ? hora.substring(1) : hora;  // quitar cero inicial

        String[] horas = {"9:00", "10:00", "11:00", "12:00", "13:00",
            "14:00", "15:00", "16:00", "17:00",
            "18:00", "19:00", "20:00"};

        for (int i = 0; i < horas.length; i++) {
            if (h.startsWith(horas[i])) {
                return i;
            }
        }
        return -1;
    }

    private void filtrarPorRango() {
        // Validar que la fecha no sea null
        if (jDateInicio.getDate() == null) {
            jDateInicio.setDate(java.sql.Date.valueOf(LocalDate.now()));
        }

        LocalDate inicio = jDateInicio.getDate().toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate();

        // Asegurar que empiece en lunes
        inicio = obtenerLunesDeSemana(inicio);
        LocalDate fin = inicio.with(DayOfWeek.SUNDAY);

        jDateInicio.setDate(java.sql.Date.valueOf(inicio));
        jDateFin.setDate(java.sql.Date.valueOf(fin));

        // Simplemente cargar la agenda semanal con el servicio seleccionado
        cargarAgendaSemanal(inicio);

        actualizarEncabezadosSemana(inicio);
        actualizarResumen();
    }

    private void cargarAgendaPorRango(String servicio, LocalDate inicio, LocalDate fin) {
        try (Connection cn = conexion.conectar()) {

            // Limpiar tabla
            DefaultTableModel modelo = (DefaultTableModel) jTable2.getModel();
            for (int i = 0; i < modelo.getRowCount(); i++) {
                for (int j = 1; j < modelo.getColumnCount(); j++) {
                    modelo.setValueAt("", i, j);
                }
            }

            // Consulta correcta: unir usuarios para obtener nombre completo
            String sql = """
            SELECT 
                CONCAT(u.Nombre, ' ', u.Paterno, ' ', u.Materno) AS NombreCliente,
                s.Nombre_servicio,
                c.Fecha,
                c.Hora,
                c.Estado
            FROM cita c
            INNER JOIN usuarios u ON u.idUsuarios = c.idUsuarios
            INNER JOIN cita_has_servicios chs ON chs.idCita = c.idCita
            INNER JOIN servicios s ON s.idServicios = chs.idServicios
            WHERE s.Nombre_servicio = ? AND c.Fecha BETWEEN ? AND ?
            ORDER BY c.Fecha, c.Hora
        """;

            try (PreparedStatement ps = cn.prepareStatement(sql)) {
                ps.setString(1, servicio);
                ps.setDate(2, java.sql.Date.valueOf(inicio));
                ps.setDate(3, java.sql.Date.valueOf(fin));

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String cliente = rs.getString("NombreCliente"); // nombre completo
                        LocalDate fechaCita = rs.getDate("Fecha").toLocalDate();
                        String hora = rs.getString("Hora");
                        String estado = rs.getString("Estado");

                        int diaSemana = fechaCita.getDayOfWeek().getValue(); // 1=Lunes
                        int col = diaSemana; // columna de lunes a domingo
                        int fila = obtenerFilaPorHora(hora);

                        if (fila != -1 && col <= 7) {
                            String texto = cliente; // solo nombre del cliente
                            if ("Completada".equalsIgnoreCase(estado)) {
                                texto += " (✔)";
                            }
                            if ("Pendiente".equalsIgnoreCase(estado)) {
                                texto += " (⏳)";
                            }
                            modelo.setValueAt(texto, fila, col);
                        }
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error cargando rango: " + e.getMessage());
        }
    }

    private void actualizarConteoDiario(Connection cn, LocalDate fecha) throws SQLException {
        DefaultTableModel modeloConteo = (DefaultTableModel) jTable1.getModel();

        String sql = """
        SELECT Estado, COUNT(*) AS cantidad
        FROM cita
        WHERE Fecha = ?
        GROUP BY Estado
    """;

        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(fecha));
            try (ResultSet rs = ps.executeQuery()) {
                int total = 0, pendientes = 0, confirmadas = 0;
                while (rs.next()) {
                    String estado = rs.getString("Estado");
                    int cant = rs.getInt("cantidad");
                    total += cant;

                    if ("Pendiente".equalsIgnoreCase(estado)) {
                        pendientes += cant;
                    }
                    if ("Confirmada".equalsIgnoreCase(estado)) {
                        confirmadas += cant;
                    }
                }

                modeloConteo.setValueAt(total, 0, 1);
                modeloConteo.setValueAt(pendientes, 1, 1);
                modeloConteo.setValueAt(confirmadas, 2, 1);
            }
        }
    }

    private LocalDate obtenerLunesDeSemana(LocalDate fecha) {
        return fecha.with(DayOfWeek.MONDAY);
    }

    private void cargarServiciosEnCombo() {
        cmbservicio.removeAllItems();

        // Agregar opción para mostrar todos los servicios
        cmbservicio.addItem("Todos");

        String sql = "SELECT Nombre_servicio FROM servicios ORDER BY Nombre_servicio ASC";

        try (Connection con = conexion.conectar(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String nombre = rs.getString("Nombre_servicio");
                cmbservicio.addItem(nombre);
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al cargar servicios: " + e.getMessage());
            // Opciones por defecto en caso de error
            cmbservicio.addItem("Maquillaje");
            cmbservicio.addItem("Uñas");
            cmbservicio.addItem("Peinados");
        }

        // Agregar listener para filtrar automáticamente cuando cambie el servicio
        cmbservicio.addActionListener(e -> {
            if (jDateInicio.getDate() != null) {
                filtrarPorRango();
            }
        });
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel2 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        jTextField2 = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jPanel5 = new javax.swing.JPanel();
        jButton2 = new javax.swing.JButton();
        jMenuBar2 = new javax.swing.JMenuBar();
        jMenu5 = new javax.swing.JMenu();
        jMenu7 = new javax.swing.JMenu();
        jMenuBar3 = new javax.swing.JMenuBar();
        jMenu8 = new javax.swing.JMenu();
        jMenu9 = new javax.swing.JMenu();
        jMenuBar4 = new javax.swing.JMenuBar();
        jMenu10 = new javax.swing.JMenu();
        jMenu11 = new javax.swing.JMenu();
        jLabel1 = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        INS = new javax.swing.JLabel();
        FACE = new javax.swing.JLabel();
        WPP = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        jLabel21 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTable2 = new javax.swing.JTable();
        cmbservicio = new javax.swing.JComboBox<>();
        btnFiltrarRango = new javax.swing.JButton();
        jButtonAnterior = new javax.swing.JButton();
        jButtonSiguiente = new javax.swing.JButton();
        lblFechaHoy = new javax.swing.JLabel();
        jDateInicio = new com.toedter.calendar.JDateChooser();
        jDateFin = new com.toedter.calendar.JDateChooser();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenu15 = new javax.swing.JMenu();
        jregistrarcliente = new javax.swing.JMenuItem();
        jMenuItem12 = new javax.swing.JMenuItem();
        jMenuItem13 = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        jMenuItem4 = new javax.swing.JMenuItem();
        jMenu12 = new javax.swing.JMenu();
        jMenuItem8 = new javax.swing.JMenuItem();
        menuPagoRestante = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenu13 = new javax.swing.JMenu();
        jMenuItem9 = new javax.swing.JMenuItem();
        jMenuItem10 = new javax.swing.JMenuItem();
        jMenu4 = new javax.swing.JMenu();
        jMenuItem7 = new javax.swing.JMenuItem();
        jMenu14 = new javax.swing.JMenu();
        jMenuItem11 = new javax.swing.JMenuItem();
        jMenu6 = new javax.swing.JMenu();
        jMenuItem6 = new javax.swing.JMenuItem();
        jMenu19 = new javax.swing.JMenu();
        jMenuItemCerrarSecion6 = new javax.swing.JMenuItem();

        jLabel2.setFont(new java.awt.Font("Serif", 3, 14)); // NOI18N
        jLabel2.setText("Iniciar sesión");

        jLabel5.setText("Correo electrónico");

        jLabel10.setText("Contraseña");

        jTextField1.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        jTextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField1ActionPerformed(evt);
            }
        });

        jTextField2.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));

        jButton1.setBackground(new java.awt.Color(255, 204, 255));
        jButton1.setText("Iniciar sesión");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        jPanel5.setBackground(new java.awt.Color(242, 215, 245));

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 8, Short.MAX_VALUE)
        );

        jButton2.setBackground(new java.awt.Color(255, 204, 255));
        jButton2.setText("Registarse");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(84, 84, 84)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addComponent(jLabel5))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addGap(0, 24, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, 225, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 225, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(16, 16, 16))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel10)
                        .addGap(102, 102, 102))))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addComponent(jButton1)
                        .addGap(81, 81, 81))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                        .addComponent(jButton2)
                        .addGap(85, 85, 85))))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel2)
                .addGap(18, 18, 18)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(jLabel10)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextField2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButton1)
                .addGap(12, 12, 12)
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButton2)
                .addGap(12, 12, 12))
        );

        jMenu5.setText("File");
        jMenuBar2.add(jMenu5);

        jMenu7.setText("Edit");
        jMenuBar2.add(jMenu7);

        jMenu8.setText("File");
        jMenuBar3.add(jMenu8);

        jMenu9.setText("Edit");
        jMenuBar3.add(jMenu9);

        jMenu10.setText("File");
        jMenuBar4.add(jMenu10);

        jMenu11.setText("Edit");
        jMenuBar4.add(jMenu11);

        jLabel1.setText("jLabel1");

        jLabel20.setFont(new java.awt.Font("Serif", 3, 18)); // NOI18N
        jLabel20.setText("Servicio");

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
                .addGap(442, 442, 442)
                .addComponent(INS, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(50, 50, 50)
                .addComponent(WPP, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(49, 49, 49)
                .addComponent(FACE, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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

        jLabel21.setFont(new java.awt.Font("Serif", 3, 18)); // NOI18N
        jLabel21.setText("PANEL DE ADMINISTRACIÓN");

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"Citas para hoy", null},
                {"Clientes Registrados", null},
                {"Anticipo pendientes", null},
                {"Diseño en catálogo", null},
                {"Horarios bloqueados esta semana", null}
            },
            new String [] {
                "", ""
            }
        ) {
            boolean[] canEdit = new boolean [] {
                true, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(jTable1);

        jTable2.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null}
            },
            new String [] {
                "Hora", "Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado", "Domingo"
            }
        ));
        jScrollPane3.setViewportView(jTable2);

        cmbservicio.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Maquillaje", "Uñas", "Peinados", " " }));
        cmbservicio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbservicioActionPerformed(evt);
            }
        });

        btnFiltrarRango.setBackground(new java.awt.Color(246, 177, 246));
        btnFiltrarRango.setText("Filtrar Rango");
        btnFiltrarRango.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFiltrarRangoActionPerformed(evt);
            }
        });

        jButtonAnterior.setBackground(new java.awt.Color(255, 204, 255));
        jButtonAnterior.setText("Anterior");
        jButtonAnterior.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAnteriorActionPerformed(evt);
            }
        });

        jButtonSiguiente.setBackground(new java.awt.Color(255, 204, 255));
        jButtonSiguiente.setText("Siguiente");
        jButtonSiguiente.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSiguienteActionPerformed(evt);
            }
        });

        lblFechaHoy.setText("jLabel7");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(513, 513, 513)
                        .addComponent(jLabel21))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(117, 117, 117)
                        .addComponent(jButtonAnterior)
                        .addGap(46, 46, 46)
                        .addComponent(jButtonSiguiente))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(470, 470, 470)
                        .addComponent(lblFechaHoy, javax.swing.GroupLayout.PREFERRED_SIZE, 353, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(44, 44, 44)
                        .addComponent(cmbservicio, javax.swing.GroupLayout.PREFERRED_SIZE, 166, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(70, 70, 70)
                        .addComponent(jDateInicio, javax.swing.GroupLayout.PREFERRED_SIZE, 104, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(49, 49, 49)
                        .addComponent(jDateFin, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(57, 57, 57)
                        .addComponent(btnFiltrarRango))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(117, 117, 117)
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 827, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(29, 29, 29)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 422, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(0, 119, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(28, 28, 28)
                .addComponent(jLabel21)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblFechaHoy)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(73, 73, 73)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 136, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 84, Short.MAX_VALUE)
                        .addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, 241, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(34, 34, 34)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jDateFin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jDateInicio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(cmbservicio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnFiltrarRango))
                        .addGap(18, 18, 18)
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 313, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(28, 28, 28)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButtonAnterior)
                            .addComponent(jButtonSiguiente))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jMenu1.setText("INICIO ");
        jMenu1.addMenuListener(new javax.swing.event.MenuListener() {
            public void menuCanceled(javax.swing.event.MenuEvent evt) {
            }
            public void menuDeselected(javax.swing.event.MenuEvent evt) {
            }
            public void menuSelected(javax.swing.event.MenuEvent evt) {
                jMenu1MenuSelected(evt);
            }
        });
        jMenuBar1.add(jMenu1);

        jMenu15.setText("CLIENTES");

        jregistrarcliente.setText("Registro de clientes");
        jregistrarcliente.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jregistrarclienteActionPerformed(evt);
            }
        });
        jMenu15.add(jregistrarcliente);

        jMenuItem12.setText("Citas Agendadas");
        jMenuItem12.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem12ActionPerformed(evt);
            }
        });
        jMenu15.add(jMenuItem12);

        jMenuItem13.setText("Citas");
        jMenuItem13.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem13ActionPerformed(evt);
            }
        });
        jMenu15.add(jMenuItem13);

        jMenuBar1.add(jMenu15);

        jMenu3.setText("AGENDAR CITA");

        jMenuItem4.setText("Agendar Cita");
        jMenuItem4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem4ActionPerformed(evt);
            }
        });
        jMenu3.add(jMenuItem4);

        jMenuBar1.add(jMenu3);

        jMenu12.setText("PAGOS  ");

        jMenuItem8.setText("Revision de Anticipos");
        jMenuItem8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem8ActionPerformed(evt);
            }
        });
        jMenu12.add(jMenuItem8);

        menuPagoRestante.setText("Pago Restante");
        menuPagoRestante.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuPagoRestanteActionPerformed(evt);
            }
        });
        jMenu12.add(menuPagoRestante);

        jMenuBar1.add(jMenu12);

        jMenu2.setText("CATALÓGO  ");

        jMenuItem1.setText("Gestion de servicios ");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItem1);

        jMenuItem2.setText("Gestion de categorias ");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItem2);

        jMenuBar1.add(jMenu2);

        jMenu13.setText("BLOQUEO DE HORARIOS");

        jMenuItem9.setText("Bloqueo de horarios");
        jMenuItem9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem9ActionPerformed(evt);
            }
        });
        jMenu13.add(jMenuItem9);

        jMenuItem10.setText("Bloqueos existentes");
        jMenuItem10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem10ActionPerformed(evt);
            }
        });
        jMenu13.add(jMenuItem10);

        jMenuBar1.add(jMenu13);

        jMenu4.setText("ROLES");
        jMenu4.addMenuListener(new javax.swing.event.MenuListener() {
            public void menuCanceled(javax.swing.event.MenuEvent evt) {
            }
            public void menuDeselected(javax.swing.event.MenuEvent evt) {
            }
            public void menuSelected(javax.swing.event.MenuEvent evt) {
                jMenu4MenuSelected(evt);
            }
        });

        jMenuItem7.setText("Roles");
        jMenuItem7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem7ActionPerformed(evt);
            }
        });
        jMenu4.add(jMenuItem7);

        jMenuBar1.add(jMenu4);

        jMenu14.setText("REPORTES");
        jMenu14.addMenuListener(new javax.swing.event.MenuListener() {
            public void menuCanceled(javax.swing.event.MenuEvent evt) {
            }
            public void menuDeselected(javax.swing.event.MenuEvent evt) {
            }
            public void menuSelected(javax.swing.event.MenuEvent evt) {
                jMenu14MenuSelected(evt);
            }
        });

        jMenuItem11.setText("Reportes");
        jMenuItem11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem11ActionPerformed(evt);
            }
        });
        jMenu14.add(jMenuItem11);

        jMenuBar1.add(jMenu14);

        jMenu6.setText("LOGIN");

        jMenuItem6.setText("Login");
        jMenuItem6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem6ActionPerformed(evt);
            }
        });
        jMenu6.add(jMenuItem6);

        jMenuBar1.add(jMenu6);

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
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField1ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jMenu1MenuSelected(javax.swing.event.MenuEvent evt) {//GEN-FIRST:event_jMenu1MenuSelected
        // TODO add your handling code here:       
//inicio
        Inicio Inicio = new Inicio();
        Inicio.setVisible(true);
        this.dispose(); // cierra la actual

    }//GEN-LAST:event_jMenu1MenuSelected

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        // TODO add your handling code here:
        //para arir uñas
        NewGCV NewGCV = new NewGCV();
        NewGCV.setVisible(true);
        this.dispose(); // cierra la actual

    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        // TODO add your handling code here:
        //para abrir peinados
        NewGCVCategoriaServicio NewGCVCategoriaServicio = new NewGCVCategoriaServicio();
        NewGCVCategoriaServicio.setVisible(true);
        this.dispose(); // cierra la actual

    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem4ActionPerformed
        // TODO add your handling code here:
        //agendar cita
        NewJAgendarcitaREC NewJAgendarcita = new NewJAgendarcitaREC();
        NewJAgendarcita.setVisible(true);
        this.dispose(); // cierra la actual

    }//GEN-LAST:event_jMenuItem4ActionPerformed

    private void jMenuItem6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem6ActionPerformed
        // TODO add your handling code here:
        //login
        NewJLogin NewJLogin = new NewJLogin();
        NewJLogin.setVisible(true);
        this.dispose(); // cierra la actual

    }//GEN-LAST:event_jMenuItem6ActionPerformed

    private void jMenuItem8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem8ActionPerformed
        // TODO add your handling code here:
        NewJRevPag NewJRevPag = new NewJRevPag();
        NewJRevPag.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem8ActionPerformed

    private void jMenuItem9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem9ActionPerformed
        // TODO add your handling code here:
        NewJBloqueoHorario NewJBloqueoHorario = new NewJBloqueoHorario();
        NewJBloqueoHorario.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem9ActionPerformed

    private void jregistrarclienteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jregistrarclienteActionPerformed
        // TODO add your handling code here:
        NewJRegClient NewJRegClient = new NewJRegClient();
        NewJRegClient.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jregistrarclienteActionPerformed

    private void jMenuItem12ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem12ActionPerformed
        // TODO add your handling code here:

        NewJCitaAgenda NewJCitaAgenda = new NewJCitaAgenda();
        NewJCitaAgenda.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem12ActionPerformed

    private void jMenuItem13ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem13ActionPerformed
        // TODO add your handling code here:
        NewJAgendarcitaREC NewJAgendarcitaREC = new NewJAgendarcitaREC();
        NewJAgendarcitaREC.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem13ActionPerformed

    private void jMenuItem10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem10ActionPerformed
        // TODO add your handling code here:
        NewJBloqueosExistentess NewJBloqueosExistentess = new NewJBloqueosExistentess();
        NewJBloqueosExistentess.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem10ActionPerformed

    private void jMenu4MenuSelected(javax.swing.event.MenuEvent evt) {//GEN-FIRST:event_jMenu4MenuSelected
        // TODO add your handling code here:
    }//GEN-LAST:event_jMenu4MenuSelected

    private void jMenuItem7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem7ActionPerformed
        // TODO add your handling code here:NewJRoles
        NewJRoles NewJRoles = new NewJRoles();
        NewJRoles.setVisible(true);
        this.dispose(); // cierra la actual

    }//GEN-LAST:event_jMenuItem7ActionPerformed

    private void btnFiltrarRangoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFiltrarRangoActionPerformed
        // TODO add your handling code here:
        filtrarPorRango();
    }//GEN-LAST:event_btnFiltrarRangoActionPerformed

    private void jButtonAnteriorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAnteriorActionPerformed
        // TODO add your handling code here:
        cambiarSemana(-1);
    }//GEN-LAST:event_jButtonAnteriorActionPerformed

    private void jMenuItem11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem11ActionPerformed
        // TODO add your handling code here:

        NewJReportes NewJReportes = new NewJReportes();
        NewJReportes.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem11ActionPerformed

    private void jMenu14MenuSelected(javax.swing.event.MenuEvent evt) {//GEN-FIRST:event_jMenu14MenuSelected
        // TODO add your handling code here:
    }//GEN-LAST:event_jMenu14MenuSelected

    private void menuPagoRestanteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuPagoRestanteActionPerformed
        // TODO add your handling code here:
        NewJPagoRestante pago = new NewJPagoRestante(this);
        pago.setVisible(true);
        this.setVisible(false);
    }//GEN-LAST:event_menuPagoRestanteActionPerformed

    private void jButtonSiguienteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSiguienteActionPerformed
        // TODO add your handling code here:
        cambiarSemana(1);
    }//GEN-LAST:event_jButtonSiguienteActionPerformed

    private void cmbservicioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbservicioActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_cmbservicioActionPerformed

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
            java.util.logging.Logger.getLogger(NewJPanelAdministracion.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(NewJPanelAdministracion.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(NewJPanelAdministracion.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(NewJPanelAdministracion.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }


        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new NewJPanelAdministracion().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel FACE;
    private javax.swing.JLabel INS;
    private javax.swing.JLabel WPP;
    private javax.swing.JButton btnFiltrarRango;
    private javax.swing.JComboBox<String> cmbservicio;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButtonAnterior;
    private javax.swing.JButton jButtonSiguiente;
    private com.toedter.calendar.JDateChooser jDateFin;
    private com.toedter.calendar.JDateChooser jDateInicio;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu10;
    private javax.swing.JMenu jMenu11;
    private javax.swing.JMenu jMenu12;
    private javax.swing.JMenu jMenu13;
    private javax.swing.JMenu jMenu14;
    private javax.swing.JMenu jMenu15;
    private javax.swing.JMenu jMenu19;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenu jMenu3;
    private javax.swing.JMenu jMenu4;
    private javax.swing.JMenu jMenu5;
    private javax.swing.JMenu jMenu6;
    private javax.swing.JMenu jMenu7;
    private javax.swing.JMenu jMenu8;
    private javax.swing.JMenu jMenu9;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuBar jMenuBar2;
    private javax.swing.JMenuBar jMenuBar3;
    private javax.swing.JMenuBar jMenuBar4;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem10;
    private javax.swing.JMenuItem jMenuItem11;
    private javax.swing.JMenuItem jMenuItem12;
    private javax.swing.JMenuItem jMenuItem13;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem jMenuItem6;
    private javax.swing.JMenuItem jMenuItem7;
    private javax.swing.JMenuItem jMenuItem8;
    private javax.swing.JMenuItem jMenuItem9;
    private javax.swing.JMenuItem jMenuItemCerrarSecion6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTable jTable1;
    private javax.swing.JTable jTable2;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JMenuItem jregistrarcliente;
    private javax.swing.JLabel lblFechaHoy;
    private javax.swing.JMenuItem menuPagoRestante;
    // End of variables declaration//GEN-END:variables
}
