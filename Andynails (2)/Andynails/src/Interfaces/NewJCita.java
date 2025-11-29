/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package Interfaces;

import andynails.ConexionBD;
import andynails.RedesSociales;
import javax.swing.JFrame;
import java.sql.*;
import javax.swing.JOptionPane;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;

/**
 *
 * @author User
 */
public class NewJCita extends javax.swing.JFrame {

    ConexionBD conexion;
    private JFrame ventanaAnterior;
    private String idCitaActual;
    private double totalAcumulado = 0.0;
    private javax.swing.JList<String> jListServiciosSeleccionados;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton btnAgregarServicio;
    private javax.swing.JButton btnQuitarServicio;

    /**
     * Creates new form NewJCitaAgenda
     */
    public NewJCita() {
        initComponents();
        conexion = new ConexionBD("andynails");
        inicializarComponentesFaltantes();
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        RedesSociales.configurarRedesSociales(INS, WPP, FACE);
        cargarComboBoxes();
        configurarCalculos();
        generarNumeroCita();
    }

    private void inicializarComponentesFaltantes() {
        // Inicializar JList y componentes relacionados si son null
        if (jListServiciosSeleccionados == null) {
            jListServiciosSeleccionados = new javax.swing.JList<>();
            jScrollPane1 = new javax.swing.JScrollPane();
            btnAgregarServicio = new javax.swing.JButton();
            btnQuitarServicio = new javax.swing.JButton();

            // Configurar JList
            jListServiciosSeleccionados.setModel(new javax.swing.DefaultListModel<>());
            jScrollPane1.setViewportView(jListServiciosSeleccionados);

            // Configurar botones
            btnAgregarServicio.setText("Agregar Servicio");
            btnAgregarServicio.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    agregarServicio(); 
                }
            });

            btnQuitarServicio.setText("Quitar Servicio");
            btnQuitarServicio.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                 quitarServicio(); 
                }
            });
        }
    }

    private DefaultListModel<String> getModeloSeguro() {
        if (jListServiciosSeleccionados == null) {
            inicializarComponentesFaltantes();
        }
        return (DefaultListModel<String>) jListServiciosSeleccionados.getModel();
    }

    public NewJCita(JFrame anterior, String idCita) {
        initComponents();
        conexion = new ConexionBD("andynails");
        this.ventanaAnterior = anterior;
        this.idCitaActual = idCita;
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        RedesSociales.configurarRedesSociales(INS, WPP, FACE);
        cargarComboBoxes();
        configurarCalculos();
        cargarDatosCita(idCita);
    }

    private void cargarComboBoxes() {
        // Cargar horas disponibles
        String[] horas = {"09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00", "18:00"};
        jComboBoxhora.setModel(new DefaultComboBoxModel<>(horas));

        // Cargar servicios desde la base de datos
        cargarServicios();

        // Configurar fecha actual - FORMA CORRECTA
        cargarFechasDisponibles();

        // Estado por defecto
        txtEstadoservicio.setText("Pendiente");

        // Limpiar lista de servicios
        DefaultListModel<String> model = getModeloSeguro();
        model.clear();

        // Reiniciar totales
        totalAcumulado = 0.0;
        txtTotalágar.setText("");
        txtmontoanticipo.setText("");
        txtmontorestante.setText("");
        jLabelPrecioservicio.setText("");
    }

    private void cargarFechasDisponibles() {
        // Crear array con próximos 7 días
        String[] fechas = new String[7];
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        for (int i = 0; i < 7; i++) {
            fechas[i] = dateFormat.format(cal.getTime());
            cal.add(Calendar.DATE, 1); // Siguiente día
        }

        jComboBox2.setModel(new DefaultComboBoxModel<>(fechas));
    }

    private void generarNumeroCita() {
        try (Connection con = conexion.conectar(); PreparedStatement ps = con.prepareStatement("SELECT MAX(idCita) as ultimoId FROM Cita"); ResultSet rs = ps.executeQuery()) {

            int ultimoId = 0;
            if (rs.next()) {
                ultimoId = rs.getInt("ultimoId");
            }
            txtNumerodecita.setText(String.valueOf(ultimoId + 1));

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al generar número de cita: " + e.getMessage());
            txtNumerodecita.setText("1"); // Valor por defecto
        }
    }

    private void cargarServicios() {
        try (Connection con = conexion.conectar(); PreparedStatement ps = con.prepareStatement("SELECT idServicios, Nombre_servicio FROM Servicios"); ResultSet rs = ps.executeQuery()) {

            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            model.addElement("Seleccionar servicio");
            while (rs.next()) {
                model.addElement(rs.getString("Nombre_servicio"));
            }
            jComboBoxservicios.setModel(model);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar servicios: " + e.getMessage());
        }
    }

    private void cargarCategoriasPorServicio(String servicio) {
        try (Connection con = conexion.conectar(); PreparedStatement ps = con.prepareStatement(
                "SELECT cs.Nombre_categoria, cs.Precio "
                + "FROM categoria_Servicio cs "
                + "INNER JOIN Servicios s ON cs.idServicios = s.idServicios "
                + "WHERE s.Nombre_servicio = ?")) {

            ps.setString(1, servicio);
            ResultSet rs = ps.executeQuery();

            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
            model.addElement("Seleccionar categoría");

            while (rs.next()) {
                String categoria = rs.getString("Nombre_categoria");
                model.addElement(categoria);
            }

            jComboBoxCategoria.setModel(model);
            jLabelPrecioservicio.setText(""); // Limpiar precio

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar categorías: " + e.getMessage());
        }
    }

    private void cargarPrecioPorCategoria(String servicio, String categoria) {
        try (Connection con = conexion.conectar(); PreparedStatement ps = con.prepareStatement(
                "SELECT cs.Precio "
                + "FROM categoria_Servicio cs "
                + "INNER JOIN Servicios s ON cs.idServicios = s.idServicios "
                + "WHERE s.Nombre_servicio = ? AND cs.Nombre_categoria = ?")) {

            ps.setString(1, servicio);
            ps.setString(2, categoria);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                double precio = rs.getDouble("Precio");
                jLabelPrecioservicio.setText("$" + precio);
                // NO actualizar el total aquí, solo cuando se agregue el servicio
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar precio: " + e.getMessage());
        }
    }

    private void configurarCalculos() {
        txtmontoanticipo.addActionListener(e -> calcularRestante());
        // También calcular cuando se escribe
        txtmontoanticipo.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                calcularRestante();
            }
        });
    }

    private void calcularRestante() {
        try {
            double total = totalAcumulado;
            double anticipo = txtmontoanticipo.getText().isEmpty() ? 0 : Double.parseDouble(txtmontoanticipo.getText());
            double restante = total - anticipo;
            txtmontorestante.setText(String.valueOf(restante));
        } catch (NumberFormatException e) {
            // Si hay error en el formato, poner 0
            txtmontorestante.setText("0");
        }
    }

    private void cargarDatosCita(String idCita) {
        String sql = """
            SELECT c.idCita, CONCAT(u.Nombre, ' ', u.Paterno, ' ', u.Materno) as Cliente, 
                   c.Fecha, c.Hora, s.Nombre_servicio as Servicio, c.Estado, c.Total
            FROM Cita c
            INNER JOIN Usuarios u ON c.idUsuarios = u.idUsuarios
            INNER JOIN Cita_has_Servicios cs ON c.idCita = cs.idCita
            INNER JOIN Servicios s ON cs.idServicios = s.idServicios
            WHERE c.idCita = ?
            """;

        try (Connection con = conexion.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, idCita);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                txtNumerodecita.setText(rs.getString("idCita"));
                txtNombreclienta.setText(rs.getString("Cliente"));
                jComboBox2.setSelectedItem(rs.getString("Fecha"));
                jComboBoxhora.setSelectedItem(rs.getString("Hora"));
                jComboBoxservicios.setSelectedItem(rs.getString("Servicio"));
                txtEstadoservicio.setText(rs.getString("Estado"));
                txtTotalágar.setText(rs.getString("Total"));
                totalAcumulado = rs.getDouble("Total");

                // Cargar categorías para este servicio
                cargarCategoriasPorServicio(rs.getString("Servicio"));

                // Cargar servicios seleccionados
                cargarServiciosSeleccionados(idCita);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar datos de la cita: " + e.getMessage());
        }
    }

    private void cargarServiciosSeleccionados(String idCita) {
        String sql = """
            SELECT s.Nombre_servicio, cs.Nombre_categoria, cs.Precio
            FROM Cita_has_Servicios chs
            INNER JOIN Servicios s ON chs.idServicios = s.idServicios
            INNER JOIN categoria_Servicio cs ON s.idServicios = cs.idServicios
            WHERE chs.idCita = ?
            """;

        try (Connection con = conexion.conectar(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, idCita);
            ResultSet rs = ps.executeQuery();

            DefaultListModel<String> model = (DefaultListModel<String>) jListServiciosSeleccionados.getModel();
            model.clear();

            while (rs.next()) {
                String servicio = rs.getString("Nombre_servicio");
                String categoria = rs.getString("Nombre_categoria");
                double precio = rs.getDouble("Precio");
                String servicioCompleto = servicio + " - " + categoria + " - $" + precio;
                model.addElement(servicioCompleto);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar servicios seleccionados: " + e.getMessage());
        }
    }

    private void guardarCita() {
        // Validar campos obligatorios - USAR MÉTODO SEGURO
        DefaultListModel<String> model = getModeloSeguro();

        if (txtNombreclienta.getText().trim().isEmpty() || model.getSize() == 0) {
            JOptionPane.showMessageDialog(this, "Por favor complete el nombre del cliente y agregue al menos un servicio");
            return;
        }

        try {
            Connection con = conexion.conectar();

            if (idCitaActual == null) {
                // Nueva cita
                String sql = "INSERT INTO Cita (Fecha, Hora, Estado, idUsuarios, Total) VALUES (?, ?, ?, ?, ?)";
                try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, jComboBox2.getSelectedItem().toString());
                    ps.setString(2, jComboBoxhora.getSelectedItem().toString());
                    ps.setString(3, txtEstadoservicio.getText());
                    ps.setInt(4, 1); // ID de usuario temporal
                    ps.setDouble(5, totalAcumulado);

                    int affectedRows = ps.executeUpdate();
                    if (affectedRows > 0) {
                        try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                            if (generatedKeys.next()) {
                                int idCita = generatedKeys.getInt(1);
                                guardarServiciosCita(idCita, con);
                            }
                        }
                    }
                }
            } else {
                // Actualizar cita existente
                String sql = "UPDATE Cita SET Fecha = ?, Hora = ?, Estado = ?, Total = ? WHERE idCita = ?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, jComboBox2.getSelectedItem().toString());
                    ps.setString(2, jComboBoxhora.getSelectedItem().toString());
                    ps.setString(3, txtEstadoservicio.getText());
                    ps.setDouble(4, totalAcumulado);
                    ps.setString(5, idCitaActual);
                    ps.executeUpdate();

                    actualizarServiciosCita(con);
                }
            }

            JOptionPane.showMessageDialog(this, "Cita guardada exitosamente");
            limpiarCampos();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al guardar cita: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void guardarServiciosCita(int idCita, Connection con) throws SQLException {
        DefaultListModel<String> model = (DefaultListModel<String>) jListServiciosSeleccionados.getModel();

        for (int i = 0; i < model.getSize(); i++) {
            String servicioCompleto = model.getElementAt(i);
            String nombreServicio = servicioCompleto.split(" - ")[0];

            String sqlServicio = "SELECT idServicios FROM Servicios WHERE Nombre_servicio = ?";
            try (PreparedStatement ps = con.prepareStatement(sqlServicio)) {
                ps.setString(1, nombreServicio);
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    int idServicio = rs.getInt("idServicios");
                    String sql = "INSERT INTO Cita_has_Servicios (idCita, idServicios) VALUES (?, ?)";
                    try (PreparedStatement ps2 = con.prepareStatement(sql)) {
                        ps2.setInt(1, idCita);
                        ps2.setInt(2, idServicio);
                        ps2.executeUpdate();
                    }
                }
            }
        }
    }

    private void actualizarServiciosCita(Connection con) throws SQLException {
        // Primero eliminar servicios existentes
        String sqlDelete = "DELETE FROM Cita_has_Servicios WHERE idCita = ?";
        try (PreparedStatement ps = con.prepareStatement(sqlDelete)) {
            ps.setString(1, idCitaActual);
            ps.executeUpdate();
        }

        // Luego agregar los nuevos servicios
        guardarServiciosCita(Integer.parseInt(idCitaActual), con);
    }

    private void cancelarCita() {
        if (idCitaActual == null) {
            JOptionPane.showMessageDialog(this, "No hay cita seleccionada para cancelar");
            return;
        }

        Object[] opciones = {"Sí", "No"};
        int confirm = JOptionPane.showOptionDialog(this,
                "¿Está seguro de que desea cancelar esta cita?",
                "Confirmar cancelación",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                opciones,
                opciones[1]);

        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection con = conexion.conectar()) {
                // Eliminar servicios asociados
                String sqlServicios = "DELETE FROM Cita_has_Servicios WHERE idCita = ?";
                try (PreparedStatement ps = con.prepareStatement(sqlServicios)) {
                    ps.setString(1, idCitaActual);
                    ps.executeUpdate();
                }

                // Eliminar cita
                String sqlCita = "DELETE FROM Cita WHERE idCita = ?";
                try (PreparedStatement ps = con.prepareStatement(sqlCita)) {
                    ps.setString(1, idCitaActual);
                    ps.executeUpdate();
                }

                JOptionPane.showMessageDialog(this, "Cita cancelada exitosamente");
                limpiarCampos();
                if (ventanaAnterior != null) {
                    this.dispose();
                    ventanaAnterior.setVisible(true);
                }

            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error al cancelar cita: " + e.getMessage());
            }
        }
    }

    private void reagendarCita() {
        if (idCitaActual == null) {
            JOptionPane.showMessageDialog(this, "No hay cita seleccionada para reagendar");
            return;
        }

        // Simplemente permitir edición de fecha y hora
        JOptionPane.showMessageDialog(this, "Puede cambiar la fecha y hora de la cita y luego guardar los cambios");
    }

    private void limpiarCampos() {
        txtNombreclienta.setText("");
        txtEstadoservicio.setText("Pendiente");
        txtTotalágar.setText("");
        txtmontoanticipo.setText("");
        txtmontorestante.setText("");
        jLabelPrecioservicio.setText("");
        jComboBoxservicios.setSelectedIndex(0);
        jComboBoxCategoria.setSelectedIndex(0);
        jComboBoxCategoria.setModel(new DefaultComboBoxModel<>(new String[]{"Seleccionar categoría"}));

        // Limpiar lista de servicios seleccionados
        DefaultListModel<String> model = (DefaultListModel<String>) jListServiciosSeleccionados.getModel();
        model.clear();
        totalAcumulado = 0.0;

        Calendar cal = Calendar.getInstance();
        jComboBox2.setSelectedItem(new SimpleDateFormat("yyyy-MM-dd").format(cal.getTime()));
        jComboBoxhora.setSelectedIndex(0);

        idCitaActual = null;
        generarNumeroCita(); // Generar nuevo número de cita
    }

    private void agregarServicio() {
        if (jComboBoxservicios.getSelectedIndex() == 0 || jComboBoxCategoria.getSelectedIndex() == 0) {
            JOptionPane.showMessageDialog(this, "Selecciona un servicio y una categoría primero");
            return;
        }

        String servicio = jComboBoxservicios.getSelectedItem().toString();
        String categoria = jComboBoxCategoria.getSelectedItem().toString();
        String precioText = jLabelPrecioservicio.getText();

        if (precioText.isEmpty() || precioText.equals("$")) {
            JOptionPane.showMessageDialog(this, "No hay precio disponible para este servicio");
            return;
        }

        try {
            double precio = Double.parseDouble(precioText.replace("$", "").trim());
            String servicioCompleto = servicio + " - " + categoria + " - $" + precio;

            // Agregar a la lista - USAR MÉTODO SEGURO
            DefaultListModel<String> model = getModeloSeguro();
            model.addElement(servicioCompleto);

            // ACTUALIZAR TOTAL AUTOMÁTICAMENTE
            totalAcumulado += precio;
            txtTotalágar.setText(String.valueOf(totalAcumulado));

            // CALCULAR MONTO RESTANTE AUTOMÁTICAMENTE
            calcularRestante();

            // Limpiar para nuevo servicio
            jComboBoxCategoria.setSelectedIndex(0);
            jLabelPrecioservicio.setText("");

            JOptionPane.showMessageDialog(this, "Servicio agregado: " + servicio);

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Error en el formato del precio");
        }
    }

    private void quitarServicio() {
        int selectedIndex = jListServiciosSeleccionados.getSelectedIndex();
        if (selectedIndex != -1) {
            DefaultListModel<String> model = (DefaultListModel<String>) jListServiciosSeleccionados.getModel();
            String servicioRemovido = model.getElementAt(selectedIndex);

            // Extraer precio del servicio removido
            String[] partes = servicioRemovido.split("\\$");
            if (partes.length > 1) {
                try {
                    double precioRemovido = Double.parseDouble(partes[1]);
                    totalAcumulado -= precioRemovido;
                    txtTotalágar.setText(String.valueOf(totalAcumulado));
                    calcularRestante();
                } catch (NumberFormatException e) {
                    // Ignorar error
                }
            }

            model.remove(selectedIndex);
        } else {
            JOptionPane.showMessageDialog(this, "Selecciona un servicio de la lista para quitar");
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

        jCalendar1 = new com.toedter.calendar.JCalendar();
        jPanel1 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        INS = new javax.swing.JLabel();
        FACE = new javax.swing.JLabel();
        WPP = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        btnguardar = new javax.swing.JButton();
        btnReagendarcita = new javax.swing.JButton();
        btncancelarcita = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        txtNombreclienta = new javax.swing.JTextField();
        jComboBox2 = new javax.swing.JComboBox<>();
        txtNumerodecita = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        txtmontoanticipo = new javax.swing.JTextField();
        txtTotalágar = new javax.swing.JTextField();
        txtmontorestante = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        txtEstadoservicio = new javax.swing.JTextField();
        jComboBoxhora = new javax.swing.JComboBox<>();
        btnRegresar = new javax.swing.JButton();
        jComboBoxservicios = new javax.swing.JComboBox<>();
        jComboBoxCategoria = new javax.swing.JComboBox<>();
        jLabelCategorias = new javax.swing.JLabel();
        jLabelPrecioservicio = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        btnQuitarservicio = new javax.swing.JButton();
        btnAgregarservico = new javax.swing.JButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu4 = new javax.swing.JMenu();
        jMenu5 = new javax.swing.JMenu();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();
        jMenu8 = new javax.swing.JMenu();
        jMenuItem6 = new javax.swing.JMenuItem();
        jMenuItem7 = new javax.swing.JMenuItem();
        jMenu7 = new javax.swing.JMenu();
        jMenuItem5 = new javax.swing.JMenuItem();
        jMenu9 = new javax.swing.JMenu();
        jMenuItem8 = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setBackground(new java.awt.Color(243, 224, 255));

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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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
        jLabel2.setText("CITAS ");

        btnguardar.setBackground(new java.awt.Color(255, 204, 255));
        btnguardar.setText("Guardar");
        btnguardar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnguardarActionPerformed(evt);
            }
        });

        btnReagendarcita.setBackground(new java.awt.Color(255, 204, 255));
        btnReagendarcita.setText("Reagendar cita");
        btnReagendarcita.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnReagendarcitaActionPerformed(evt);
            }
        });

        btncancelarcita.setBackground(new java.awt.Color(255, 204, 255));
        btncancelarcita.setText("Cancelar cita");
        btncancelarcita.setToolTipText("");
        btncancelarcita.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btncancelarcitaActionPerformed(evt);
            }
        });

        jLabel1.setText("Número de cita:");

        jLabel4.setText("Fecha");

        jLabel5.setText("Hora");

        jLabel9.setText("Estado");

        txtNombreclienta.setText("Nombre Cliente");

        jComboBox2.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Fecha" }));
        jComboBox2.setToolTipText("Fecha");
        jComboBox2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox2ActionPerformed(evt);
            }
        });

        txtNumerodecita.setText("Numero de cita");
        txtNumerodecita.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtNumerodecitaActionPerformed(evt);
            }
        });

        jLabel10.setText("Servicio");

        jLabel11.setText("Monto anticipo");

        txtmontoanticipo.setText("Monto Ant");
        txtmontoanticipo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtmontoanticipoActionPerformed(evt);
            }
        });

        txtTotalágar.setText("Totala a pagar");
        txtTotalágar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtTotalágarActionPerformed(evt);
            }
        });

        txtmontorestante.setText("Monto Res");
        txtmontorestante.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtmontorestanteActionPerformed(evt);
            }
        });

        jLabel12.setText("Total ");

        jLabel13.setText("Monto restante");

        jLabel14.setText("Precio servicios");

        txtEstadoservicio.setText("Estado Servicio");
        txtEstadoservicio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtEstadoservicioActionPerformed(evt);
            }
        });

        jComboBoxhora.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Hora", "Item 2", "Item 3", "Item 4" }));
        jComboBoxhora.setToolTipText("Hora");
        jComboBoxhora.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxhoraActionPerformed(evt);
            }
        });

        btnRegresar.setBackground(new java.awt.Color(255, 204, 255));
        btnRegresar.setText("Regresar");
        btnRegresar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRegresarActionPerformed(evt);
            }
        });

        jComboBoxservicios.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Servicios", " " }));
        jComboBoxservicios.setToolTipText("Servicios");
        jComboBoxservicios.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxserviciosActionPerformed(evt);
            }
        });

        jComboBoxCategoria.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "categorias", " ", " " }));
        jComboBoxCategoria.setToolTipText("Servicios");
        jComboBoxCategoria.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxCategoriaActionPerformed(evt);
            }
        });

        jLabelCategorias.setText("Categoria");

        jLabel6.setText("Confirmada");

        jLabel7.setText("Pendiente");

        btnQuitarservicio.setBackground(new java.awt.Color(255, 204, 255));
        btnQuitarservicio.setText("Quitar servicio");
        btnQuitarservicio.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnQuitarservicioActionPerformed(evt);
            }
        });

        btnAgregarservico.setBackground(new java.awt.Color(255, 204, 255));
        btnAgregarservico.setText("Agregar otrp Servicio");
        btnAgregarservico.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAgregarservicoActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(45, 45, 45)
                        .addComponent(jLabel4))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(33, 33, 33)
                        .addComponent(jLabel1))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(44, 44, 44)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel9)
                            .addComponent(jLabel11)
                            .addComponent(jLabel13)
                            .addComponent(jLabel12)
                            .addComponent(jLabel14)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(6, 6, 6)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel10)
                                    .addComponent(jLabel5)
                                    .addComponent(jLabelCategorias))))))
                .addGap(49, 49, 49)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jComboBox2, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(1, 1, 1)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jComboBoxhora, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(txtNumerodecita, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jComboBoxservicios, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jComboBoxCategoria, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(txtEstadoservicio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(0, 0, Short.MAX_VALUE)))
                        .addGap(60, 60, 60)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 104, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 104, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(9, 9, 9)
                                .addComponent(btnQuitarservicio)))
                        .addGap(68, 68, 68))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(txtmontorestante, javax.swing.GroupLayout.PREFERRED_SIZE, 174, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(txtmontoanticipo, javax.swing.GroupLayout.Alignment.LEADING)
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                                    .addGap(10, 10, 10)
                                    .addComponent(jLabelPrecioservicio, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addComponent(txtTotalágar, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 174, Short.MAX_VALUE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(btnguardar, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btncancelarcita, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(btnReagendarcita, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(87, 87, 87))))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnRegresar, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(64, 64, 64))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(261, 261, 261)
                        .addComponent(jLabel2))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(41, 41, 41)
                        .addComponent(txtNombreclienta, javax.swing.GroupLayout.PREFERRED_SIZE, 529, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                    .addContainerGap(366, Short.MAX_VALUE)
                    .addComponent(btnAgregarservico)
                    .addGap(128, 128, 128)))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(28, 28, 28)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(txtNombreclienta, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(43, 43, 43)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel4)
                        .addComponent(jComboBox2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel1)
                            .addComponent(txtNumerodecita, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(56, 56, 56)))
                .addGap(34, 34, 34)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(jComboBoxhora, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(jComboBoxservicios, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 19, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jComboBoxCategoria, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelCategorias)
                    .addComponent(btnQuitarservicio))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(txtEstadoservicio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6)
                    .addComponent(jLabel7))
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(30, 30, 30)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel14)
                            .addComponent(jLabelPrecioservicio, javax.swing.GroupLayout.PREFERRED_SIZE, 16, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 32, Short.MAX_VALUE)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel12, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(txtTotalágar, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(31, 31, 31)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel11)
                            .addComponent(txtmontoanticipo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(34, 34, 34))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnguardar)
                        .addGap(31, 31, 31)
                        .addComponent(btncancelarcita)
                        .addGap(32, 32, 32)
                        .addComponent(btnReagendarcita)
                        .addGap(11, 11, 11)))
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel13)
                    .addComponent(txtmontorestante, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(42, 42, 42)
                .addComponent(btnRegresar)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addGap(274, 274, 274)
                    .addComponent(btnAgregarservico)
                    .addContainerGap(400, Short.MAX_VALUE)))
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

        jMenu5.setText("CATALÓGO");

        jMenuItem2.setText("UÑAS");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu5.add(jMenuItem2);

        jMenuItem1.setText("PEINADO");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu5.add(jMenuItem1);

        jMenuItem3.setText("MAQUILLAJES");
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem3ActionPerformed(evt);
            }
        });
        jMenu5.add(jMenuItem3);

        jMenuBar1.add(jMenu5);

        jMenu8.setText("AGENDAR CITA");

        jMenuItem6.setText("Agendar Cita");
        jMenuItem6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem6ActionPerformed(evt);
            }
        });
        jMenu8.add(jMenuItem6);

        jMenuItem7.setText("Cancelar cita");
        jMenuItem7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem7ActionPerformed(evt);
            }
        });
        jMenu8.add(jMenuItem7);

        jMenuBar1.add(jMenu8);

        jMenu7.setText("CONTACTO");

        jMenuItem5.setText("Contacto");
        jMenuItem5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem5ActionPerformed(evt);
            }
        });
        jMenu7.add(jMenuItem5);

        jMenuBar1.add(jMenu7);

        jMenu9.setText("LOGIN");

        jMenuItem8.setText("Login");
        jMenu9.add(jMenuItem8);

        jMenuBar1.add(jMenu9);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
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
        //para arir uñas
        NewJCatalogoUñas NewJCatalogoUñas = new NewJCatalogoUñas();
        NewJCatalogoUñas.setVisible(true);
        this.dispose(); // cierra la actual

    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        // TODO add your handling code here:
        //para abrir peinados
        NewJCatalogoPeinado NewJCatalogoPeinado = new NewJCatalogoPeinado();
        NewJCatalogoPeinado.setVisible(true);
        this.dispose(); // cierra la actual

    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem3ActionPerformed
        // TODO add your handling code here:
        //para maquillaje
        NewJCatalogoMaq NewJCatalogoMaq = new NewJCatalogoMaq();
        NewJCatalogoMaq.setVisible(true);
        this.dispose(); // cierra la actual

    }//GEN-LAST:event_jMenuItem3ActionPerformed

    private void jMenuItem5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem5ActionPerformed
        // TODO add your handling code here:
        //boton de contacto
        NewJContacto NewJContacto = new NewJContacto();
        NewJContacto.setVisible(true);
        this.dispose(); // cierra la actual

    }//GEN-LAST:event_jMenuItem5ActionPerformed

    private void btncancelarcitaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btncancelarcitaActionPerformed
        // TODO add your handling code here:
        cancelarCita();
    }//GEN-LAST:event_btncancelarcitaActionPerformed

    private void btnReagendarcitaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnReagendarcitaActionPerformed
        // TODO add your handling code here:
        reagendarCita();
    }//GEN-LAST:event_btnReagendarcitaActionPerformed

    private void btnguardarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnguardarActionPerformed
        // TODO add your handling code here:
        guardarCita();
    }//GEN-LAST:event_btnguardarActionPerformed

    private void jComboBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox2ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jComboBox2ActionPerformed

    private void txtNumerodecitaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtNumerodecitaActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtNumerodecitaActionPerformed

    private void txtmontoanticipoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtmontoanticipoActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtmontoanticipoActionPerformed

    private void txtTotalágarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtTotalágarActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtTotalágarActionPerformed

    private void txtmontorestanteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtmontorestanteActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtmontorestanteActionPerformed

    private void txtEstadoservicioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtEstadoservicioActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtEstadoservicioActionPerformed

    private void jComboBoxhoraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxhoraActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jComboBoxhoraActionPerformed

    private void btnRegresarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRegresarActionPerformed
        // TODO add your handling code here:
        this.dispose();
        if (ventanaAnterior != null) {
            ventanaAnterior.setVisible(true);
        }
    }//GEN-LAST:event_btnRegresarActionPerformed

    private void jComboBoxserviciosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxserviciosActionPerformed
        // TODO add your handling code here:
        if (jComboBoxservicios.getSelectedIndex() > 0) {
            String servicioSeleccionado = jComboBoxservicios.getSelectedItem().toString();
            cargarCategoriasPorServicio(servicioSeleccionado);
        }
    }//GEN-LAST:event_jComboBoxserviciosActionPerformed

    private void jComboBoxCategoriaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxCategoriaActionPerformed
        // TODO add your handling code here:
        if (jComboBoxservicios.getSelectedIndex() > 0 && jComboBoxCategoria.getSelectedIndex() > 0) {
            String servicio = jComboBoxservicios.getSelectedItem().toString();
            String categoria = jComboBoxCategoria.getSelectedItem().toString();
            cargarPrecioPorCategoria(servicio, categoria);
        }
    }//GEN-LAST:event_jComboBoxCategoriaActionPerformed

    private void jMenuItem6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem6ActionPerformed
        // TODO add your handling code here:
        NewJAgendarcita NewJAgendarcita = new NewJAgendarcita();
        NewJAgendarcita.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem6ActionPerformed

    private void jMenuItem7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem7ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jMenuItem7ActionPerformed

    private void btnAgregarservicoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAgregarservicoActionPerformed
        // TODO add your handling code here:
         agregarServicio();
    }//GEN-LAST:event_btnAgregarservicoActionPerformed

    private void btnQuitarservicioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnQuitarservicioActionPerformed
        // TODO add your handling code here:
            quitarServicio();

    }//GEN-LAST:event_btnQuitarservicioActionPerformed

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
            java.util.logging.Logger.getLogger(NewJCita.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(NewJCita.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(NewJCita.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(NewJCita.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>


        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new NewJCita().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel FACE;
    private javax.swing.JLabel INS;
    private javax.swing.JLabel WPP;
    private javax.swing.JButton btnAgregarservico;
    private javax.swing.JButton btnQuitarservicio;
    private javax.swing.JButton btnReagendarcita;
    private javax.swing.JButton btnRegresar;
    private javax.swing.JButton btncancelarcita;
    private javax.swing.JButton btnguardar;
    private com.toedter.calendar.JCalendar jCalendar1;
    private javax.swing.JComboBox<String> jComboBox2;
    private javax.swing.JComboBox<String> jComboBoxCategoria;
    private javax.swing.JComboBox<String> jComboBoxhora;
    private javax.swing.JComboBox<String> jComboBoxservicios;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel jLabelCategorias;
    private javax.swing.JLabel jLabelPrecioservicio;
    private javax.swing.JMenu jMenu4;
    private javax.swing.JMenu jMenu5;
    private javax.swing.JMenu jMenu7;
    private javax.swing.JMenu jMenu8;
    private javax.swing.JMenu jMenu9;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem5;
    private javax.swing.JMenuItem jMenuItem6;
    private javax.swing.JMenuItem jMenuItem7;
    private javax.swing.JMenuItem jMenuItem8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JTextField txtEstadoservicio;
    private javax.swing.JTextField txtNombreclienta;
    private javax.swing.JTextField txtNumerodecita;
    private javax.swing.JTextField txtTotalágar;
    private javax.swing.JTextField txtmontoanticipo;
    private javax.swing.JTextField txtmontorestante;
    // End of variables declaration//GEN-END:variables
}
