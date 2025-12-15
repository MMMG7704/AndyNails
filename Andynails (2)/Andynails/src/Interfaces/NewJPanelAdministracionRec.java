package Interfaces;

import andynails.ConexionBD;
import andynails.RedesSociales;
import com.toedter.calendar.JDateChooser;
import javax.swing.JFrame;
import java.util.Date;
import javax.swing.table.DefaultTableModel;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.JOptionPane;
import java.sql.Connection;
import java.awt.Color;
import java.awt.Component;
import java.sql.SQLException;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.DayOfWeek;
import java.util.Locale;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
/**
 *
 * @author User
 */
public class NewJPanelAdministracionRec extends javax.swing.JFrame {

    private Connection cn;
    private ConexionBD conexion;

    private LocalDate fechaSeleccionada = LocalDate.now();
    private com.toedter.calendar.JDateChooser dateChooser;
    private DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private javax.swing.JMenuItem jMenuItemCerrarSesion;

    private void actualizarEncabezadosSemana(LocalDate fechaBase) {
        DefaultTableModel modelo = (DefaultTableModel) tabla.getModel();

        // Obtener lunes de la semana
        LocalDate lunes = fechaBase.with(DayOfWeek.MONDAY);

        // Formato: dd/MM
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM");

        // Columnas: 1 = lunes ... 7 = domingo
        String[] dias = {"Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"};

        for (int i = 0; i < 7; i++) {
            LocalDate dia = lunes.plusDays(i);
            String nombreColumna = dias[i] + " " + dia.format(formato);
            tabla.getColumnModel().getColumn(i + 1).setHeaderValue(nombreColumna);
        }

        // Refrescar la tabla para que se vea el cambio
        tabla.getTableHeader().repaint();
    }

    /**
     * Creates new form NewJRegistro
     */
    public NewJPanelAdministracionRec() {
        initComponents();

        // === SIEMPRE carga la conexión con el nombre de tu BD ===
        conexion = new ConexionBD("andynails");
        cn = conexion.conectar();

        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // === FECHA INICIAL ===
        fechaSeleccionada = LocalDate.now();

        // === JDATECHOOSER DEL FORM ===
        dateChooser = jDateInicio;
        dateChooser.setDate(java.sql.Date.valueOf(fechaSeleccionada));

        actualizarLabelFecha();

        // === TABLA PRINCIPAL ===
        DefaultTableModel modelo = new DefaultTableModel();
        modelo.addColumn("Hora");
        modelo.addColumn("Lunes");
        modelo.addColumn("Martes");
        modelo.addColumn("Miércoles");
        modelo.addColumn("Jueves");
        modelo.addColumn("Viernes");
        modelo.addColumn("Sábado");
        modelo.addColumn("Domingo");

        String[] horas = {
            "9:00", "10:00", "11:00", "12:00", "13:00", "14:00",
            "15:00", "16:00", "17:00", "18:00", "19:00", "20:00"
        };

        for (String h : horas) {
            modelo.addRow(new Object[]{h, "", "", "", "", "", "", ""});
        }

        tabla.setModel(modelo);
        tabla.setDefaultRenderer(Object.class, new AgendaRenderer());

        // === TABLA RESUMEN ===
        DefaultTableModel modeloConteo = new DefaultTableModel(
                new Object[][]{
                    {"Total de Citas Hoy", 0},
                    {"Citas Pendientes", 0},
                    {"Citas Confirmadas", 0}
                },
                new String[]{"Concepto", "Cantidad"}
        );
        tablaResumen.setModel(modeloConteo);

        // === EVENTO FECHA ===
        dateChooser.addPropertyChangeListener("date", evt -> {
            if (evt.getNewValue() != null) {
                fechaSeleccionada = ((java.util.Date) evt.getNewValue())
                        .toInstant()
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate();
            }
        });

        // === CARGAR PRIMERA VEZ ===
        cargarCategorias();  // <-- esto NO lo tenías
        String servicio = (String) cmbservicio.getSelectedItem();

        if (servicio != null) {
            cargarAgendaSemanal(servicio, fechaSeleccionada);
            actualizarEncabezadosSemana(fechaSeleccionada);
        }

        actualizarTotales();

        btnFiltrarRango.addActionListener(e -> filtrarPorRango());
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

    private void actualizarTotales() {
        try (Connection con = conexion.conectar()) {

            DefaultTableModel modelo = (DefaultTableModel) tablaResumen.getModel();

            // Total citas HOY
            String sqlHoy = "SELECT COUNT(*) FROM cita WHERE Fecha = CURDATE()";
            PreparedStatement psHoy = con.prepareStatement(sqlHoy);
            ResultSet rsHoy = psHoy.executeQuery();
            if (rsHoy.next()) {
                modelo.setValueAt(rsHoy.getInt(1), 0, 1);
            }

            // Citas Pendientes
            String sqlPend = "SELECT COUNT(*) FROM cita WHERE Estado = 'Pendiente'";
            PreparedStatement psPend = con.prepareStatement(sqlPend);
            ResultSet rsPend = psPend.executeQuery();
            if (rsPend.next()) {
                modelo.setValueAt(rsPend.getInt(1), 1, 1);
            }

            // Citas Confirmadas
            String sqlConf = "SELECT COUNT(*) FROM cita WHERE Estado = 'Confirmada'";
            PreparedStatement psConf = con.prepareStatement(sqlConf);
            ResultSet rsConf = psConf.executeQuery();
            if (rsConf.next()) {
                modelo.setValueAt(rsConf.getInt(1), 2, 1);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al actualizar resumen: " + e.getMessage());
        }
    }

    private void filtrarPorRango() {

        if (jDateInicio.getDate() == null || jDateFin.getDate() == null) {
            JOptionPane.showMessageDialog(this, "Seleccione fecha inicio y fecha fin");
            return;
        }

        LocalDate inicio = jDateInicio.getDate().toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate();

        LocalDate fin = jDateFin.getDate().toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate();

        if (inicio.isAfter(fin)) {
            JOptionPane.showMessageDialog(this, "La fecha inicio no puede ser mayor que la fecha fin");
            return;
        }

        // 🚫 VALIDAR QUE NO EXCEDA UNA SEMANA
        if (fin.isAfter(inicio.plusDays(6))) {
            JOptionPane.showMessageDialog(this,
                    "El rango excede una semana. Solo puedes seleccionar una semana completa (lunes a domingo).");
            return;
        }

        // 🚫 VALIDAR QUE INICIO SEA LUNES
        if (inicio.getDayOfWeek() != DayOfWeek.MONDAY) {
            JOptionPane.showMessageDialog(this,
                    "La fecha de inicio debe ser lunes para consultar una semana completa.");
            return;
        }

        // 🚫 VALIDAR QUE FIN SEA DOMINGO
        if (fin.getDayOfWeek() != DayOfWeek.SUNDAY) {
            JOptionPane.showMessageDialog(this,
                    "La fecha final debe ser domingo para consultar una semana completa.");
            return;
        }

        // 🚫 VALIDAR QUE SEA LUNES → DOMINGO (6 días exactos)
        if (!fin.equals(inicio.plusDays(6))) {
            JOptionPane.showMessageDialog(this,
                    "Debes seleccionar una semana completa: del lunes al domingo.");
            return;
        }

        // ✔️ SI TODO ES CORRECTO, CARGAR AGENDA
        String servicio = (String) cmbservicio.getSelectedItem();

        if (servicio != null) {
            cargarAgendaPorRango(servicio, inicio, fin);
            actualizarEncabezadosSemana(inicio);
        }

        try (Connection cn = conexion.conectar()) {
            actualizarConteoDiario(cn, inicio);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        actualizarTotales();
    }

    private void cargarAgendaPorRango(String servicio, LocalDate inicio, LocalDate fin) {
        try (Connection cn = conexion.conectar()) {

            // Limpiar tabla
            DefaultTableModel modelo = (DefaultTableModel) tabla.getModel();
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

            PreparedStatement ps = cn.prepareStatement(sql);
            ps.setString(1, servicio);
            ps.setDate(2, java.sql.Date.valueOf(inicio));
            ps.setDate(3, java.sql.Date.valueOf(fin));
            ResultSet rs = ps.executeQuery();

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

            // Actualizar conteo diario solo del primer día del rango
            actualizarConteoDiario(cn, inicio);

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error cargando rango: " + e.getMessage());
        }
    }

    private void cambiarSemana(int desplazamiento) {
        fechaSeleccionada = fechaSeleccionada.plusWeeks(desplazamiento);

        dateChooser.setDate(java.util.Date.from(fechaSeleccionada.atStartOfDay()
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant()));

        actualizarLabelFecha();
        String servicio = (String) cmbservicio.getSelectedItem();
        if (servicio != null) {
            cargarAgendaSemanal(servicio, fechaSeleccionada);
            actualizarEncabezadosSemana(fechaSeleccionada);
        }
    }

    private void actualizarLabelFecha() {
        DateTimeFormatter formato = DateTimeFormatter.ofPattern(
                "EEEE, dd 'de' MMMM 'de' yyyy", new Locale("es", "ES")
        );
        lblFechaHoy.setText("Bienvenid@, Recepcionista! Hoy es "
                + fechaSeleccionada.format(formato));
    }

    private JFrame ventanaAnterior;

    public NewJPanelAdministracionRec(JFrame anterior) {
        initComponents();
        conexion = new ConexionBD("andynails");
        this.ventanaAnterior = anterior;
    }

    private void regresar() {
        if (ventanaAnterior != null) {
            ventanaAnterior.setVisible(true);
        }
        this.dispose();
    }

    private void actualizarConteoDiario(Connection cn, LocalDate fecha) throws SQLException {
        DefaultTableModel modeloConteo = (DefaultTableModel) tablaResumen.getModel();

        String sql = """
        SELECT Estado, COUNT(*) AS cantidad
        FROM cita
        WHERE Fecha = ?
        GROUP BY Estado
    """;

        PreparedStatement ps = cn.prepareStatement(sql);
        ps.setDate(1, java.sql.Date.valueOf(fecha));
        ResultSet rs = ps.executeQuery();

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

// === MÉTODO PARA CARGAR LA AGENDA DE UNA SEMANA ===
    private void cargarAgendaSemanal(String servicio, LocalDate fechaBase) {
        try (Connection cn = conexion.conectar()) {

            // Determinar lunes y domingo de la semana del filtro
            LocalDate lunes = fechaBase.with(DayOfWeek.MONDAY);
            LocalDate domingo = fechaBase.with(DayOfWeek.SUNDAY);

            // Limpiar tabla (desde columna 1 = lunes hasta domingo)
            DefaultTableModel modelo = (DefaultTableModel) tabla.getModel();
            for (int i = 0; i < modelo.getRowCount(); i++) {
                for (int j = 1; j < modelo.getColumnCount(); j++) {
                    modelo.setValueAt("", i, j);
                }
            }

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
    WHERE s.Nombre_servicio LIKE ?
      AND c.Fecha BETWEEN ? AND ?
    ORDER BY c.Fecha, c.Hora
""";

            PreparedStatement ps = cn.prepareStatement(sql);
            ps.setString(1, "%" + servicio + "%"); // <-- importante: % antes y después
            ps.setDate(2, java.sql.Date.valueOf(lunes));
            ps.setDate(3, java.sql.Date.valueOf(domingo));

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                String cliente = rs.getString("NombreCliente");
                String nombreServicio = rs.getString("Nombre_servicio");
                LocalDate fechaCita = rs.getDate("Fecha").toLocalDate();
                String hora = rs.getString("Hora");
                String estado = rs.getString("Estado");

                int diaSemana = fechaCita.getDayOfWeek().getValue(); // 1 = Lunes
                int col = diaSemana; // columna de lunes a domingo
                int fila = obtenerFilaPorHora(hora);

                if (fila != -1 && col <= 7) {

                    String texto = cliente; // SOLO nombre completo
                    // Si quieres: texto = cliente + " - " + nombreServicio;

                    if ("Completada".equalsIgnoreCase(estado)) {
                        texto += " (✔)";
                    } else if ("Pendiente".equalsIgnoreCase(estado)) {
                        texto += " (⏳)";
                    }

                    modelo.setValueAt(texto, fila, col);
                    actualizarTotales();

                }
            }

            actualizarConteoDiario(cn, fechaBase);

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al cargar agenda: " + ex.getMessage());
        }
    }

    private void cargarCategorias() {
        cmbservicio.removeAllItems();

        String sql = "SELECT idServicios, Nombre_servicio FROM servicios";

        try (Connection con = conexion.conectar(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String nombre = rs.getString("Nombre_servicio");
                cmbservicio.addItem(nombre);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int obtenerIdServicio(String nombreServicio) {
        String sql = "SELECT idServicios FROM servicios WHERE Nombre_servicio = ? LIMIT 1";

        try (PreparedStatement ps = conexion.getConnection().prepareStatement(sql)) {
            ps.setString(1, nombreServicio);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("idServicios");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return -1;
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
        tablaResumen = new javax.swing.JTable();
        jLabel3 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        lblFechaHoy = new javax.swing.JLabel();
        cmbservicio = new javax.swing.JComboBox<>();
        btnFiltrarRango = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        tabla = new javax.swing.JTable();
        jButtonSiguiente = new javax.swing.JButton();
        jButtonAnterior = new javax.swing.JButton();
        jDateFin = new com.toedter.calendar.JDateChooser();
        jDateInicio = new com.toedter.calendar.JDateChooser();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenuInicio = new javax.swing.JMenu();
        jMenuCitas = new javax.swing.JMenu();
        menuBuscarCitas = new javax.swing.JMenuItem();
        menuCitas = new javax.swing.JMenuItem();
        menuAgendarCita = new javax.swing.JMenuItem();
        jMenuPagos = new javax.swing.JMenu();
        jMenuItem8 = new javax.swing.JMenuItem();
        jMenuLogin = new javax.swing.JMenu();
        jMenuItem6 = new javax.swing.JMenuItem();
        jMenu14 = new javax.swing.JMenu();
        jMenuItem11 = new javax.swing.JMenuItem();
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

        tablaResumen.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"Total de Citas agendadas hoy", null},
                {"Citas pendientes", null},
                {"Citas Confirmadas", null}
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
        jScrollPane1.setViewportView(tablaResumen);

        jLabel3.setText("Bienvenid@, Recepcionista! Hoy es ");

        lblFechaHoy.setText("jLabel7");

        cmbservicio.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Maquillaje", "Uñas", "Peinados", " " }));

        btnFiltrarRango.setBackground(new java.awt.Color(246, 177, 246));
        btnFiltrarRango.setText("Filtrar Rango");
        btnFiltrarRango.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFiltrarRangoActionPerformed(evt);
            }
        });

        tabla.setModel(new javax.swing.table.DefaultTableModel(
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
        jScrollPane3.setViewportView(tabla);

        jButtonSiguiente.setBackground(new java.awt.Color(255, 204, 255));
        jButtonSiguiente.setText("Siguiente");
        jButtonSiguiente.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSiguienteActionPerformed(evt);
            }
        });

        jButtonAnterior.setBackground(new java.awt.Color(255, 204, 255));
        jButtonAnterior.setText("Anterior");
        jButtonAnterior.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAnteriorActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(29, 29, 29)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 887, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 422, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(108, 108, 108))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(cmbservicio, javax.swing.GroupLayout.PREFERRED_SIZE, 166, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(47, 47, 47)
                        .addComponent(jDateInicio, javax.swing.GroupLayout.PREFERRED_SIZE, 104, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jDateFin, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(57, 57, 57)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel21)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(lblFechaHoy, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel6))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addComponent(btnFiltrarRango)))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
            .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(223, 223, 223)
                .addComponent(jButtonAnterior)
                .addGap(53, 53, 53)
                .addComponent(jButtonSiguiente)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(28, 28, 28)
                        .addComponent(jLabel21)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel3)
                                    .addComponent(jLabel6)
                                    .addComponent(lblFechaHoy))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(btnFiltrarRango))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addGap(0, 31, Short.MAX_VALUE)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jDateFin, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(cmbservicio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jDateInicio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, 241, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(190, 190, 190))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(46, 46, 46)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 136, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(28, 28, 28)
                                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 307, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jButtonSiguiente)
                            .addComponent(jButtonAnterior))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jMenuInicio.setText("INICIO ");
        jMenuInicio.addMenuListener(new javax.swing.event.MenuListener() {
            public void menuCanceled(javax.swing.event.MenuEvent evt) {
            }
            public void menuDeselected(javax.swing.event.MenuEvent evt) {
            }
            public void menuSelected(javax.swing.event.MenuEvent evt) {
                jMenuInicioMenuSelected(evt);
            }
        });
        jMenuBar1.add(jMenuInicio);

        jMenuCitas.setText("CITAS");

        menuBuscarCitas.setText("Buscar citas");
        menuBuscarCitas.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuBuscarCitasActionPerformed(evt);
            }
        });
        jMenuCitas.add(menuBuscarCitas);

        menuCitas.setText("Agenda de citas");
        menuCitas.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuCitasActionPerformed(evt);
            }
        });
        jMenuCitas.add(menuCitas);

        menuAgendarCita.setText("Agendar cita");
        menuAgendarCita.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuAgendarCitaActionPerformed(evt);
            }
        });
        jMenuCitas.add(menuAgendarCita);

        jMenuBar1.add(jMenuCitas);

        jMenuPagos.setText("PAGOS  ");

        jMenuItem8.setText("Revision de Anticipos");
        jMenuItem8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem8ActionPerformed(evt);
            }
        });
        jMenuPagos.add(jMenuItem8);

        jMenuBar1.add(jMenuPagos);

        jMenuLogin.setText("LOGIN");

        jMenuItem6.setText("Login");
        jMenuItem6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem6ActionPerformed(evt);
            }
        });
        jMenuLogin.add(jMenuItem6);

        jMenuBar1.add(jMenuLogin);

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
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 1, Short.MAX_VALUE))
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

    private void jMenuInicioMenuSelected(javax.swing.event.MenuEvent evt) {//GEN-FIRST:event_jMenuInicioMenuSelected
        // TODO add your handling code here:       
//inicio
        Inicio NewJPanelAdministracionRec = new Inicio();
        NewJPanelAdministracionRec.setVisible(true);
        this.dispose(); // cierra la actual

    }//GEN-LAST:event_jMenuInicioMenuSelected

    private void jMenuItem6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem6ActionPerformed
        // TODO add your handling code here:
        //login
        NewJLogin NewJLogin = new NewJLogin();
        NewJLogin.setVisible(true);
        this.dispose(); // cierra la actual

    }//GEN-LAST:event_jMenuItem6ActionPerformed

    private void menuAgendarCitaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuAgendarCitaActionPerformed
        // TODO add your handling code here:
        NewJAgendarcitaREC agendar = new NewJAgendarcitaREC(this);
        agendar.setVisible(true);
        this.setVisible(false);
    }//GEN-LAST:event_menuAgendarCitaActionPerformed

    private void menuCitasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuCitasActionPerformed
        // TODO add your handling code here:
        NewJCitaAgenda agenda = new NewJCitaAgenda(this);
        agenda.setVisible(true);
        this.setVisible(false);
    }//GEN-LAST:event_menuCitasActionPerformed

    private void menuBuscarCitasActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuBuscarCitasActionPerformed
        // TODO add your handling code here:
        NewJBuscarCita buscar = new NewJBuscarCita(this);
        buscar.setVisible(true);
        this.setVisible(false);

    }//GEN-LAST:event_menuBuscarCitasActionPerformed

    private void btnFiltrarRangoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFiltrarRangoActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnFiltrarRangoActionPerformed

    private void jMenuItem11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem11ActionPerformed
        // TODO add your handling code here:

        NewJReportes NewJReportes = new NewJReportes();
        NewJReportes.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem11ActionPerformed

    private void jMenu14MenuSelected(javax.swing.event.MenuEvent evt) {//GEN-FIRST:event_jMenu14MenuSelected
        // TODO add your handling code here:
    }//GEN-LAST:event_jMenu14MenuSelected

    private void jButtonAnteriorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAnteriorActionPerformed
        // TODO add your handling code here:
        cambiarSemana(-1);
    }//GEN-LAST:event_jButtonAnteriorActionPerformed

    private void jButtonSiguienteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSiguienteActionPerformed
        // TODO add your handling code here:
        cambiarSemana(1);
    }//GEN-LAST:event_jButtonSiguienteActionPerformed

    private void jMenuItem8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem8ActionPerformed
        // TODO add your handling code here:
        NewJRevPag NewJRevPag = new NewJRevPag();
        NewJRevPag.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem8ActionPerformed

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
            java.util.logging.Logger.getLogger(NewJPanelAdministracionRec.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(NewJPanelAdministracionRec.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(NewJPanelAdministracionRec.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(NewJPanelAdministracionRec.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }


        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new NewJPanelAdministracionRec().setVisible(true);
            }
        });
    }

    public class AgendaRenderer extends DefaultTableCellRenderer {

        private String filtroServicio = "Todos";

        public void setFiltroServicio(String servicio) {
            this.filtroServicio = servicio;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {

            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            c.setBackground(Color.WHITE);
            c.setForeground(Color.BLACK);

            if (value != null) {
                String texto = value.toString().toLowerCase();

                // FILTRO visual
                if (!filtroServicio.equals("Todos") && !texto.contains(filtroServicio.toLowerCase())) {
                    c.setForeground(new Color(200, 200, 200)); // gris tenue si no coincide
                }

                // COLORES por tipo de servicio
                if (texto.contains("uña") || texto.contains("manic")) {
                    c.setBackground(new Color(255, 182, 193)); // Rosa
                } else if (texto.contains("pein")) {
                    c.setBackground(new Color(173, 216, 230)); // Azul claro
                } else if (texto.contains("maqu")) {
                    c.setBackground(new Color(255, 222, 173)); // Amarillo claro
                }
            }

            return c;
        }
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
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JMenu jMenu10;
    private javax.swing.JMenu jMenu11;
    private javax.swing.JMenu jMenu14;
    private javax.swing.JMenu jMenu19;
    private javax.swing.JMenu jMenu5;
    private javax.swing.JMenu jMenu7;
    private javax.swing.JMenu jMenu8;
    private javax.swing.JMenu jMenu9;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuBar jMenuBar2;
    private javax.swing.JMenuBar jMenuBar3;
    private javax.swing.JMenuBar jMenuBar4;
    private javax.swing.JMenu jMenuCitas;
    private javax.swing.JMenu jMenuInicio;
    private javax.swing.JMenuItem jMenuItem11;
    private javax.swing.JMenuItem jMenuItem6;
    private javax.swing.JMenuItem jMenuItem8;
    private javax.swing.JMenuItem jMenuItemCerrarSecion6;
    private javax.swing.JMenu jMenuLogin;
    private javax.swing.JMenu jMenuPagos;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JLabel lblFechaHoy;
    private javax.swing.JMenuItem menuAgendarCita;
    private javax.swing.JMenuItem menuBuscarCitas;
    private javax.swing.JMenuItem menuCitas;
    private javax.swing.JTable tabla;
    private javax.swing.JTable tablaResumen;
    // End of variables declaration//GEN-END:variables
}
