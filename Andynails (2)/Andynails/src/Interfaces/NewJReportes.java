/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package Interfaces;

import andynails.ConexionBD;
import andynails.RedesSociales;
import javax.swing.JFrame;

/**
 *
 * @author fanys
 */
public class NewJReportes extends javax.swing.JFrame {

    ConexionBD conexion;

    /**
     * Creates new form NewJReportes
     */
    public NewJReportes() {
        initComponents();
        RedesSociales.configurarRedesSociales(INS, WPP, FACE);
        conexion = new ConexionBD("andynails");
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Configurar ComboBox de fechas con opciones predefinidas
        configurarComboBoxFechas();

        // Configurar tooltips
        btnEstadisticos.setToolTipText("Generar reportes estadísticos y gráficos");
        btnPagos.setToolTipText("Reportes de estado de pagos e ingresos");
        btnServicios.setToolTipText("Catálogo y información de servicios");
        jButton3.setToolTipText("Cargar datos en la tabla antes de exportar");
        jButton1.setToolTipText("Exportar a PDF los datos mostrados");
        jComboBox1.setToolTipText("Seleccione el tipo específico de reporte");
        comboxRangoinicio.setToolTipText("Seleccione fecha inicial");
        comboxRangofin.setToolTipText("Seleccione fecha final");
    }

    private void configurarComboBoxFechas() {
        // Limpiar los ComboBox
        comboxRangoinicio.removeAllItems();
        comboxRangofin.removeAllItems();

        // Agregar opciones predefinidas de fechas
        String[] opcionesFechas = {
            "Última semana",
            "Último mes",
            "Últimos 3 meses",
            "Últimos 6 meses",
            "Este año",
            "Año pasado",
            "Todo el historial"
        };

        for (String opcion : opcionesFechas) {
            comboxRangoinicio.addItem(opcion);
            comboxRangofin.addItem(opcion);
        }

        // Seleccionar opciones por defecto
        comboxRangoinicio.setSelectedItem("Último mes");
        comboxRangofin.setSelectedItem("Todo el historial");
    }

    private void cargarServiciosDesdeBD() {
        try {
            String sql = "SELECT idServicios, Nombre_servicio, Descripcion, Precio FROM servicios";
            java.sql.PreparedStatement ps = conexion.conectar().prepareStatement(sql);
            java.sql.ResultSet rs = ps.executeQuery();

            javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) jTable1.getModel();
            model.setRowCount(0);

            // Actualizar nombres de columnas para servicios
            model.setColumnIdentifiers(new String[]{"ID", "Nombre del Servicio", "Descripción", "Precio"});

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("idServicios"),
                    rs.getString("Nombre_servicio"),
                    rs.getString("Descripcion"),
                    "$" + rs.getDouble("Precio")
                });
            }

            rs.close();
            ps.close();

        } catch (Exception e) {
            e.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Error al cargar servicios: " + e.getMessage(),
                    "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargarPagosDesdeBD() {
        try {
            // Primero, verifiquemos qué columnas tiene realmente tu tabla pago
            String sql = "SELECT p.idPago, u.Nombre as Cliente, p.Monto, p.Fecha_pago "
                    + "FROM pago p "
                    + "INNER JOIN usuarios u ON p.idUsuarios = u.idUsuarios "
                    + "ORDER BY p.Fecha_pago DESC";

            java.sql.PreparedStatement ps = conexion.conectar().prepareStatement(sql);
            java.sql.ResultSet rs = ps.executeQuery();

            javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) jTable1.getModel();
            model.setRowCount(0);

            // Actualizar nombres de columnas para pagos (sin Estado)
            model.setColumnIdentifiers(new String[]{"ID Pago", "Cliente", "Monto", "Fecha"});

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("idPago"),
                    rs.getString("Cliente"),
                    "$" + rs.getDouble("Monto"),
                    rs.getDate("Fecha_pago")
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

    private void cargarServiciosMasSolicitados() {
        try {
            String sql = "SELECT s.Nombre_servicio, COUNT(chs.idServicios) as Total_Citas, "
                    + "SUM(s.Precio) as Ingreso_Total "
                    + "FROM cita_has_servicios chs "
                    + "INNER JOIN servicios s ON chs.idServicios = s.idServicios "
                    + "GROUP BY s.Nombre_servicio "
                    + "ORDER BY Total_Citas DESC";
            java.sql.PreparedStatement ps = conexion.conectar().prepareStatement(sql);
            java.sql.ResultSet rs = ps.executeQuery();

            javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) jTable1.getModel();
            model.setRowCount(0);

            // Actualizar nombres de columnas para estadísticas
            model.setColumnIdentifiers(new String[]{"Servicio", "Total Citas", "Ingreso Total"});

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString("Nombre_servicio"),
                    rs.getInt("Total_Citas"),
                    "$" + rs.getDouble("Ingreso_Total")
                });
            }

            rs.close();
            ps.close();

        } catch (Exception e) {
            e.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Error al cargar servicios más solicitados: " + e.getMessage(),
                    "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargarClientesFrecuentes() {
        try {
            String sql = "SELECT u.Nombre, u.Telefono, COUNT(c.idCita) as Total_Citas "
                    + "FROM cita c "
                    + "INNER JOIN usuarios u ON c.idUsuarios = u.idUsuarios "
                    + "GROUP BY u.idUsuarios, u.Nombre, u.Telefono "
                    + "ORDER BY Total_Citas DESC "
                    + "LIMIT 10";
            java.sql.PreparedStatement ps = conexion.conectar().prepareStatement(sql);
            java.sql.ResultSet rs = ps.executeQuery();

            javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) jTable1.getModel();
            model.setRowCount(0);

            // Actualizar nombres de columnas para clientes frecuentes
            model.setColumnIdentifiers(new String[]{"Cliente", "Teléfono", "Total Citas"});

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString("Nombre"),
                    rs.getString("Telefono"),
                    rs.getInt("Total_Citas")
                });
            }

            rs.close();
            ps.close();

        } catch (Exception e) {
            e.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Error al cargar clientes frecuentes: " + e.getMessage(),
                    "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cargarCitasPorFecha() {
        try {
            String fechaInicio = obtenerFechaDesdeOpcion(comboxRangoinicio.getSelectedItem().toString());
            String fechaFin = obtenerFechaDesdeOpcion(comboxRangofin.getSelectedItem().toString());

            String sql = "SELECT c.idCita, u.Nombre as Cliente, s.Nombre_servicio as Servicio, "
                    + "c.Fecha, c.Hora, c.Estado "
                    + "FROM cita c "
                    + "INNER JOIN usuarios u ON c.idUsuarios = u.idUsuarios "
                    + "INNER JOIN cita_has_servicios chs ON c.idCita = chs.idCita "
                    + "INNER JOIN servicios s ON chs.idServicios = s.idServicios "
                    + "WHERE c.Fecha BETWEEN ? AND ? "
                    + "ORDER BY c.Fecha, c.Hora";

            java.sql.PreparedStatement ps = conexion.conectar().prepareStatement(sql);
            ps.setString(1, fechaInicio);
            ps.setString(2, fechaFin);
            java.sql.ResultSet rs = ps.executeQuery();

            javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) jTable1.getModel();
            model.setRowCount(0);
            model.setColumnIdentifiers(new String[]{"ID Cita", "Cliente", "Servicio", "Fecha", "Hora", "Estado"});

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("idCita"),
                    rs.getString("Cliente"),
                    rs.getString("Servicio"),
                    rs.getDate("Fecha"),
                    rs.getTime("Hora"),
                    rs.getString("Estado")
                });
            }

            rs.close();
            ps.close();

        } catch (Exception e) {
            e.printStackTrace();
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Error al cargar citas: " + e.getMessage(),
                    "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    private String obtenerFechaDesdeOpcion(String opcion) {
        java.time.LocalDate hoy = java.time.LocalDate.now();

        switch (opcion) {
            case "Última semana":
                return hoy.minusWeeks(1).toString();
            case "Último mes":
                return hoy.minusMonths(1).toString();
            case "Últimos 3 meses":
                return hoy.minusMonths(3).toString();
            case "Últimos 6 meses":
                return hoy.minusMonths(6).toString();
            case "Este año":
                return java.time.LocalDate.of(hoy.getYear(), 1, 1).toString();
            case "Año pasado":
                return java.time.LocalDate.of(hoy.getYear() - 1, 1, 1).toString();
            case "Todo el historial":
                return "2000-01-01";
            default:
                return hoy.toString();
        }
    }

    private void verificarEstructuraTablas() {
        try {
            // Verificar estructura de la tabla pago
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
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();
        jComboBox1 = new javax.swing.JComboBox<>();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jButton3 = new javax.swing.JButton();
        comboxRangoinicio = new javax.swing.JComboBox<>();
        comboxRangofin = new javax.swing.JComboBox<>();
        jButton1 = new javax.swing.JButton();
        jPanel10 = new javax.swing.JPanel();
        INS = new javax.swing.JLabel();
        FACE = new javax.swing.JLabel();
        WPP = new javax.swing.JLabel();
        btnEstadisticos = new javax.swing.JButton();
        btnPagos = new javax.swing.JButton();
        btnServicios = new javax.swing.JButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu4 = new javax.swing.JMenu();
        jMenu12 = new javax.swing.JMenu();
        jMenuItem10 = new javax.swing.JMenuItem();
        jMenuItem11 = new javax.swing.JMenuItem();
        jMenuItem12 = new javax.swing.JMenuItem();
        jMenuItem13 = new javax.swing.JMenuItem();
        jMenu6 = new javax.swing.JMenu();
        jMenuItem4 = new javax.swing.JMenuItem();
        jMenuItem14 = new javax.swing.JMenuItem();
        jMenu8 = new javax.swing.JMenu();
        jMenuItem6 = new javax.swing.JMenuItem();
        jMenu9 = new javax.swing.JMenu();
        jMenuItem7 = new javax.swing.JMenuItem();
        jMenu10 = new javax.swing.JMenu();
        jMenuItem8 = new javax.swing.JMenuItem();
        jMenu7 = new javax.swing.JMenu();
        jMenuItem5 = new javax.swing.JMenuItem();
        jMenu11 = new javax.swing.JMenu();
        jMenuItem9 = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setBackground(new java.awt.Color(243, 224, 255));

        jPanel1.setBackground(new java.awt.Color(243, 224, 255));

        jPanel4.setBackground(new java.awt.Color(243, 224, 255));

        jLabel9.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel9.setText("Tipo de servicio:");

        jLabel10.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel10.setText("Rango de fechas:");

        jButton2.setBackground(new java.awt.Color(255, 204, 255));
        jButton2.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jButton2.setText("Regresar");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jComboBox1.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jComboBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBox1ActionPerformed(evt);
            }
        });

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
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

        jButton3.setBackground(new java.awt.Color(255, 204, 255));
        jButton3.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jButton3.setText("Generar ");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        comboxRangoinicio.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        comboxRangofin.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jButton1.setBackground(new java.awt.Color(255, 204, 255));
        jButton1.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jButton1.setText("Exportar");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel4Layout.createSequentialGroup()
                    .addGap(20, 20, 20)
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(jLabel9)
                        .addComponent(jLabel10))
                    .addGap(7, 7, 7)
                    .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel4Layout.createSequentialGroup()
                            .addGap(12, 12, 12)
                            .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, 339, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(jPanel4Layout.createSequentialGroup()
                            .addComponent(comboxRangoinicio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGap(36, 36, 36)
                            .addComponent(comboxRangofin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addContainerGap(241, Short.MAX_VALUE))
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                    .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButton3)
                    .addGap(37, 37, 37)
                    .addComponent(jButton1)
                    .addGap(37, 37, 37)
                    .addComponent(jButton2)
                    .addContainerGap()))
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 610, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(17, 17, 17))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(50, 50, 50)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(comboxRangoinicio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(comboxRangofin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel10))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 22, Short.MAX_VALUE)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton3)
                    .addComponent(jButton1)
                    .addComponent(jButton2)))
        );

        jPanel10.setBackground(new java.awt.Color(204, 0, 204));

        INS.setText("INS");

        FACE.setText("FACE");

        WPP.setText("WPP");

        javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addGap(442, 442, 442)
                .addComponent(INS, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(50, 50, 50)
                .addComponent(WPP, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(49, 49, 49)
                .addComponent(FACE, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(INS)
                    .addComponent(WPP)
                    .addComponent(FACE))
                .addContainerGap(14, Short.MAX_VALUE))
        );

        btnEstadisticos.setText("Estadisticos");
        btnEstadisticos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEstadisticosActionPerformed(evt);
            }
        });

        btnPagos.setText("Pagos");
        btnPagos.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPagosActionPerformed(evt);
            }
        });

        btnServicios.setText("Servicios");
        btnServicios.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnServiciosActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(28, Short.MAX_VALUE)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(136, 136, 136))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addComponent(btnEstadisticos)
                .addGap(30, 30, 30)
                .addComponent(btnPagos)
                .addGap(56, 56, 56)
                .addComponent(btnServicios)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(33, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnEstadisticos)
                    .addComponent(btnPagos)
                    .addComponent(btnServicios))
                .addGap(18, 18, 18)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(39, 39, 39)
                .addComponent(jPanel10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
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

        jMenu12.setText("CATALÓGO");

        jMenuItem10.setText("Uñas");
        jMenuItem10.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem10ActionPerformed(evt);
            }
        });
        jMenu12.add(jMenuItem10);

        jMenuItem11.setText("Peinados");
        jMenuItem11.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem11ActionPerformed(evt);
            }
        });
        jMenu12.add(jMenuItem11);

        jMenuItem12.setText("Maquillaje");
        jMenuItem12.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem12ActionPerformed(evt);
            }
        });
        jMenu12.add(jMenuItem12);

        jMenuItem13.setText("Otros");
        jMenuItem13.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem13ActionPerformed(evt);
            }
        });
        jMenu12.add(jMenuItem13);

        jMenuBar1.add(jMenu12);

        jMenu6.setText("AGENDA");

        jMenuItem4.setText("CANCELAR CITA");
        jMenuItem4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem4ActionPerformed(evt);
            }
        });
        jMenu6.add(jMenuItem4);

        jMenuItem14.setText("Agendar Cita");
        jMenuItem14.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem14ActionPerformed(evt);
            }
        });
        jMenu6.add(jMenuItem14);

        jMenuBar1.add(jMenu6);

        jMenu8.setText("PAGOS");
        jMenu8.addMenuListener(new javax.swing.event.MenuListener() {
            public void menuCanceled(javax.swing.event.MenuEvent evt) {
            }
            public void menuDeselected(javax.swing.event.MenuEvent evt) {
            }
            public void menuSelected(javax.swing.event.MenuEvent evt) {
                jMenu8MenuSelected(evt);
            }
        });

        jMenuItem6.setText("PAGOS");
        jMenuItem6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem6ActionPerformed(evt);
            }
        });
        jMenu8.add(jMenuItem6);

        jMenuBar1.add(jMenu8);

        jMenu9.setText("BLOQUEO DE HORARIOS   ");
        jMenu9.addMenuListener(new javax.swing.event.MenuListener() {
            public void menuCanceled(javax.swing.event.MenuEvent evt) {
            }
            public void menuDeselected(javax.swing.event.MenuEvent evt) {
            }
            public void menuSelected(javax.swing.event.MenuEvent evt) {
                jMenu9MenuSelected(evt);
            }
        });

        jMenuItem7.setText("BLOQUEO DE HORARIOS");
        jMenuItem7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem7ActionPerformed(evt);
            }
        });
        jMenu9.add(jMenuItem7);

        jMenuBar1.add(jMenu9);

        jMenu10.setText("REPORTES  ");
        jMenu10.addMenuListener(new javax.swing.event.MenuListener() {
            public void menuCanceled(javax.swing.event.MenuEvent evt) {
            }
            public void menuDeselected(javax.swing.event.MenuEvent evt) {
            }
            public void menuSelected(javax.swing.event.MenuEvent evt) {
                jMenu10MenuSelected(evt);
            }
        });

        jMenuItem8.setText("REPORTES");
        jMenuItem8.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem8ActionPerformed(evt);
            }
        });
        jMenu10.add(jMenuItem8);

        jMenuBar1.add(jMenu10);

        jMenu7.setText("CONTACTO");

        jMenuItem5.setText("Contacto");
        jMenuItem5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem5ActionPerformed(evt);
            }
        });
        jMenu7.add(jMenuItem5);

        jMenuBar1.add(jMenu7);

        jMenu11.setText("LOGIN");
        jMenu11.addMenuListener(new javax.swing.event.MenuListener() {
            public void menuCanceled(javax.swing.event.MenuEvent evt) {
            }
            public void menuDeselected(javax.swing.event.MenuEvent evt) {
            }
            public void menuSelected(javax.swing.event.MenuEvent evt) {
                jMenu11MenuSelected(evt);
            }
        });

        jMenuItem9.setText("LOGIN");
        jMenuItem9.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem9ActionPerformed(evt);
            }
        });
        jMenu11.add(jMenuItem9);

        jMenuBar1.add(jMenu11);

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
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        // TODO add your handling code here:
        // Determinar qué tipo de reporte generar
        String tipoReporte = jComboBox1.getSelectedItem().toString();

        try {
            // Verificar qué tipo de reporte se quiere generar basado en el comboBox
            if (tipoReporte.contains("Servicios") || tipoReporte.equals("Todos los servicios")
                    || tipoReporte.equals("Servicios activos") || tipoReporte.equals("Precios de servicios")) {

                String ruta = "Reporte_Servicios_AndyNails.pdf";
                andynails.ReporteServicios.generarPDF(ruta);
                javax.swing.JOptionPane.showMessageDialog(this,
                        " Reporte de servicios generado exitosamente:\n" + ruta,
                        "Reporte Generado",
                        javax.swing.JOptionPane.INFORMATION_MESSAGE);

            } else if (tipoReporte.contains("más solicitados") || tipoReporte.contains("Estadísticas")) {

                andynails.ReporteEstadistico.generarPDF();
                javax.swing.JOptionPane.showMessageDialog(this,
                        " Reporte estadístico generado exitosamente",
                        "Reporte Generado",
                        javax.swing.JOptionPane.INFORMATION_MESSAGE);

            } else if (tipoReporte.contains("Pagos")) {

                javax.swing.JOptionPane.showMessageDialog(this,
                        " Generando reporte de pagos...\n(Pendiente de implementar)",
                        "Reporte en Desarrollo",
                        javax.swing.JOptionPane.INFORMATION_MESSAGE);

            } else {
                javax.swing.JOptionPane.showMessageDialog(this,
                        " Por favor, selecciona un tipo de reporte específico",
                        "Selección Requerida",
                        javax.swing.JOptionPane.WARNING_MESSAGE);
            }
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    " Error al generar el reporte: " + e.getMessage(),
                    "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }

    }//GEN-LAST:event_jButton1ActionPerformed

    private void jMenu4MenuSelected(javax.swing.event.MenuEvent evt) {//GEN-FIRST:event_jMenu4MenuSelected
        // TODO add your handling code here:
        //inicio
        Inicio Inicio = new Inicio();
        Inicio.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenu4MenuSelected

    private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem4ActionPerformed
        // TODO add your handling code here:
        //agendar
        NewJCitaAgendaE NewJCitaAgenda = new NewJCitaAgendaE();
        NewJCitaAgenda.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem4ActionPerformed

    private void jMenuItem6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem6ActionPerformed
        // TODO add your handling code here:
        //boton de PAGOS
        NewJCitaAgendaE NewJCitaAgenda = new NewJCitaAgendaE();
        NewJCitaAgenda.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem6ActionPerformed

    private void jMenu8MenuSelected(javax.swing.event.MenuEvent evt) {//GEN-FIRST:event_jMenu8MenuSelected
        // TODO add your handling code here:
    }//GEN-LAST:event_jMenu8MenuSelected

    private void jMenuItem7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem7ActionPerformed
        // TODO add your handling code here:
        NewJBloqueoHorario NewJBloqueoHorario = new NewJBloqueoHorario();
        NewJBloqueoHorario.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem7ActionPerformed

    private void jMenu9MenuSelected(javax.swing.event.MenuEvent evt) {//GEN-FIRST:event_jMenu9MenuSelected
        // TODO add your handling code here:
    }//GEN-LAST:event_jMenu9MenuSelected

    private void jMenuItem8ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem8ActionPerformed
        // TODO add your handling code here:

        NewJReportes NewJReportes = new NewJReportes();
        NewJReportes.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem8ActionPerformed

    private void jMenu10MenuSelected(javax.swing.event.MenuEvent evt) {//GEN-FIRST:event_jMenu10MenuSelected
        // TODO add your handling code here:
    }//GEN-LAST:event_jMenu10MenuSelected

    private void jMenuItem9ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem9ActionPerformed
        // TODO add your handling code here:
        NewJLogin NewJLogin = new NewJLogin();
        NewJLogin.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem9ActionPerformed

    private void jMenu11MenuSelected(javax.swing.event.MenuEvent evt) {//GEN-FIRST:event_jMenu11MenuSelected
        // TODO add your handling code here:
    }//GEN-LAST:event_jMenu11MenuSelected

    private void jMenuItem5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem5ActionPerformed
        // TODO add your handling code here:
        //boton de contacto
        NewJContacto NewJContacto = new NewJContacto();
        NewJContacto.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem5ActionPerformed

    private void jComboBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jComboBox1ActionPerformed

    private void btnEstadisticosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEstadisticosActionPerformed
        // TODO add your handling code here:
        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(
                new String[]{
                    "Servicios más solicitados",
                    "Ingresos por mes",
                    "Clientes frecuentes",
                    "Horarios populares"
                }
        ));

        // Actualizar etiquetas para estadísticas
        jLabel9.setText("Tipo de estadística:");
        jLabel10.setText("Rango de fechas:");
    }//GEN-LAST:event_btnEstadisticosActionPerformed

    private void btnPagosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPagosActionPerformed
        // TODO add your handling code here:
        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(
                new String[]{
                    "Pagos pendientes",
                    "Pagos completados",
                    "Historial de pagos",
                    "Métodos de pago más usados"
                }
        ));

        jLabel9.setText("Tipo de pago:");
        jLabel10.setText("Rango de fechas:");
    }//GEN-LAST:event_btnPagosActionPerformed

    private void btnServiciosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnServiciosActionPerformed
        // TODO add your handling code here:
        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(
                new String[]{
                    "Todos los servicios",
                    "Servicios activos",
                    "Servicios por categoría",
                    "Precios de servicios"
                }
        ));

        jLabel9.setText("Tipo de servicio:");
        jLabel10.setText("Rango de fechas:");
    }//GEN-LAST:event_btnServiciosActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        // TODO add your handling code here:
        String tipoSeleccionado = jComboBox1.getSelectedItem().toString();
        javax.swing.table.DefaultTableModel model = (javax.swing.table.DefaultTableModel) jTable1.getModel();
        model.setRowCount(0); // Limpiar tabla

        try {
            if (tipoSeleccionado.contains("Servicios") || tipoSeleccionado.equals("Todos los servicios")) {
                cargarServiciosDesdeBD();
            } else if (tipoSeleccionado.contains("Pagos") || tipoSeleccionado.equals("Pagos completados")) {
                cargarPagosDesdeBD();
            } else if (tipoSeleccionado.contains("más solicitados")) {
                cargarServiciosMasSolicitados();
            } else if (tipoSeleccionado.contains("Clientes frecuentes")) {
                cargarClientesFrecuentes();
            } else if (tipoSeleccionado.contains("Citas por fecha")) {
                cargarCitasPorFecha();
            } else {
                javax.swing.JOptionPane.showMessageDialog(this,
                        "Selecciona un tipo de reporte válido",
                        "Información",
                        javax.swing.JOptionPane.INFORMATION_MESSAGE);
            }

            // Mostrar mensaje de éxito
            if (model.getRowCount() > 0) {
                javax.swing.JOptionPane.showMessageDialog(this,
                        +model.getRowCount() + " registros cargados desde la base de datos",
                        "Datos Cargados",
                        javax.swing.JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    " Error al cargar datos: " + e.getMessage(),
                    "Error",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }

    }//GEN-LAST:event_jButton3ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        // TODO add your handling code here:
        NewJPanelAdministracion NewJPanelAdministracion = new NewJPanelAdministracion();
        NewJPanelAdministracion.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jMenuItem10ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem10ActionPerformed
        // TODO add your handling code here:
        NewJCatalogoUñas NewJCatalogoUñas = new NewJCatalogoUñas();
        NewJCatalogoUñas.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem10ActionPerformed

    private void jMenuItem11ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem11ActionPerformed
        // TODO add your handling code here:
        NewJCatalogoPeinado NewJCatalogoPeinado = new NewJCatalogoPeinado();
        NewJCatalogoPeinado.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem11ActionPerformed

    private void jMenuItem12ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem12ActionPerformed
        // TODO add your handling code here:
        NewJCatalogoMaq NewJCatalogoMaq = new NewJCatalogoMaq();
        NewJCatalogoMaq.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem12ActionPerformed

    private void jMenuItem13ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem13ActionPerformed
        // TODO add your handling code here:
        ConexionBD conexionCatalogo = new ConexionBD("andynails");
        NewJCatalogoGenerico catalogo = new NewJCatalogoGenerico(conexionCatalogo);
        catalogo.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem13ActionPerformed

    private void jMenuItem14ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem14ActionPerformed
        // TODO add your handling code here:
        //agendar cita
        NewJAgenC NewJAgenC = new NewJAgenC();
        NewJAgenC.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem14ActionPerformed
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
            java.util.logging.Logger.getLogger(NewJReportes.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(NewJReportes.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(NewJReportes.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(NewJReportes.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new NewJReportes().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel FACE;
    private javax.swing.JLabel INS;
    private javax.swing.JLabel WPP;
    private javax.swing.JButton btnEstadisticos;
    private javax.swing.JButton btnPagos;
    private javax.swing.JButton btnServicios;
    private javax.swing.JComboBox<String> comboxRangofin;
    private javax.swing.JComboBox<String> comboxRangoinicio;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JMenu jMenu10;
    private javax.swing.JMenu jMenu11;
    private javax.swing.JMenu jMenu12;
    private javax.swing.JMenu jMenu4;
    private javax.swing.JMenu jMenu6;
    private javax.swing.JMenu jMenu7;
    private javax.swing.JMenu jMenu8;
    private javax.swing.JMenu jMenu9;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem10;
    private javax.swing.JMenuItem jMenuItem11;
    private javax.swing.JMenuItem jMenuItem12;
    private javax.swing.JMenuItem jMenuItem13;
    private javax.swing.JMenuItem jMenuItem14;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem jMenuItem5;
    private javax.swing.JMenuItem jMenuItem6;
    private javax.swing.JMenuItem jMenuItem7;
    private javax.swing.JMenuItem jMenuItem8;
    private javax.swing.JMenuItem jMenuItem9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable1;
    // End of variables declaration//GEN-END:variables
}
