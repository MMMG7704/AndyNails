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

    ConexionBD conexion;

    /**
     * Creates new form NewJRegistro
     */
    public NewJPanelAdministracionRec() {
        initComponents();
        RedesSociales.configurarRedesSociales(INS, WPP, FACE);

        conexion = new ConexionBD("andynails");// Inicializo la conexión a la base de datos
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        cargarCategorias();
        //   JDateChooser dateChooser = new JDateChooser();
        // dateChooser.setDateFormatString("yyyy-MM-dd");
        //dateChooser.setDate(new Date());
        //this.add(dateChooser); // Agregar al JFrame
        //dateChooser.setBounds(20, 20, 120, 30); // Posición
        DefaultTableModel modelo = new DefaultTableModel();
        modelo.addColumn("Hora");
        modelo.addColumn("Lunes");
        modelo.addColumn("Martes");
        modelo.addColumn("Miércoles");
        modelo.addColumn("Jueves");
        modelo.addColumn("Viernes");
        modelo.addColumn("Sábado");
        modelo.addColumn("Domingo");

// Horas manuales
        modelo.addRow(new Object[]{"9:00", "", "", "", "", "", "", ""});
        modelo.addRow(new Object[]{"10:00", "", "", "", "", "", "", ""});
        modelo.addRow(new Object[]{"11:00", "", "", "", "", "", "", ""});
        modelo.addRow(new Object[]{"12:00", "", "", "", "", "", "", ""});
        modelo.addRow(new Object[]{"13:00", "", "", "", "", "", "", ""});
        modelo.addRow(new Object[]{"14:00", "", "", "", "", "", "", ""});
        modelo.addRow(new Object[]{"15:00", "", "", "", "", "", "", ""});
        modelo.addRow(new Object[]{"16:00", "", "", "", "", "", "", ""});
        modelo.addRow(new Object[]{"17:00", "", "", "", "", "", "", ""});
        modelo.addRow(new Object[]{"18:00", "", "", "", "", "", "", ""});
        modelo.addRow(new Object[]{"19:00", "", "", "", "", "", "", ""});
        modelo.addRow(new Object[]{"20:00", "", "", "", "", "", "", ""});

        tabla.setModel(modelo);
        tabla.setDefaultRenderer(Object.class, new AgendaRenderer());

        cargarAgendaSemanal("Maquillaje");

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

    private void cargarAgendaSemanal(String servicioSeleccionado) {
        try (Connection con = conexion.conectar()) {
            LocalDate hoy = LocalDate.now();
            LocalDate inicioSemana = hoy.with(DayOfWeek.MONDAY);
            LocalDate finSemana = hoy.with(DayOfWeek.SUNDAY);

            // Mostrar rango de semana arriba
            lblfecha.setText("Semana del " + inicioSemana + " al " + finSemana);

            // --- Encabezados de días con fecha ---
            String[] diasSemana = new String[8];
            diasSemana[0] = "Hora";
            DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM");

            for (int i = 0; i < 7; i++) {
                LocalDate dia = inicioSemana.plusDays(i);
                String nombreDia = dia.getDayOfWeek().getDisplayName(java.time.format.TextStyle.FULL, new java.util.Locale("es", "ES"));
                nombreDia = Character.toUpperCase(nombreDia.charAt(0)) + nombreDia.substring(1); // Mayúscula inicial
                diasSemana[i + 1] = nombreDia + " (" + dia.format(formato) + ")";
            }

            // --- Modelo de tabla ---
            DefaultTableModel modelo = new DefaultTableModel(diasSemana, 0);

            String[] horas = {"9:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00", "18:00", "19:00", "20:00"};
            for (String h : horas) {
                modelo.addRow(new Object[]{h, "", "", "", "", "", "", ""});
            }

            String sql = """
            SELECT c.Fecha, c.Hora,
                   s.Nombre_servicio,
                   p.Estado_pago
            FROM cita c
            JOIN cita_has_servicios chs ON c.idCita = chs.idCita
            JOIN servicios s ON chs.idServicios = s.idServicios
            LEFT JOIN pago p ON chs.Pago_idPago = p.idPago
            WHERE c.Fecha BETWEEN ? AND ?
              AND s.Nombre_servicio LIKE ?
            ORDER BY c.Fecha, c.Hora;
        """;

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setDate(1, java.sql.Date.valueOf(inicioSemana));
            ps.setDate(2, java.sql.Date.valueOf(finSemana));
            ps.setString(3, "%" + servicioSeleccionado + "%");
            ResultSet rs = ps.executeQuery();

            int totalCitasHoy = 0, pendientes = 0, confirmadas = 0;
            LocalDate hoyFecha = LocalDate.now();

            while (rs.next()) {
                LocalDate fecha = rs.getDate("Fecha").toLocalDate();
                String hora = rs.getTime("Hora").toString().substring(0, 5);
                String servicio = rs.getString("Nombre_servicio");
                String estado = rs.getString("Estado_pago");

                int diaColumna = fecha.getDayOfWeek().getValue(); // Lunes=1 ... Domingo=7
                int filaHora = -1;

                for (int i = 0; i < horas.length; i++) {
                    if (horas[i].equals(hora)) {
                        filaHora = i;
                        break;
                    }
                }

                if (filaHora != -1 && diaColumna <= 7) {
                    String textoCelda = servicio;
                    Object actual = modelo.getValueAt(filaHora, diaColumna);
                    if (actual != null && !actual.toString().isEmpty()) {
                        textoCelda = actual + " | " + textoCelda;
                    }
                    modelo.setValueAt(textoCelda, filaHora, diaColumna);
                }

                // Contadores del panel lateral
                if (fecha.equals(hoyFecha)) {
                    totalCitasHoy++;
                    if (estado == null || estado.equalsIgnoreCase("Pendiente")) {
                        pendientes++;
                    } else if (estado.equalsIgnoreCase("Validado")) {
                        confirmadas++;
                    }
                }
            }

            tabla.setModel(modelo);
            tabla.setDefaultRenderer(Object.class, new AgendaRenderer());

            // Totales panel lateral
            jTable1.setValueAt(totalCitasHoy, 0, 1);
            jTable1.setValueAt(pendientes, 1, 1);
            jTable1.setValueAt(confirmadas, 2, 1);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al cargar agenda: " + e.getMessage());
        }
    }

    private void cargarCategorias() {
        try {
            Connection conn = new ConexionBD().conectar();
            String sql = "SELECT Nombre_categoria FROM categoria_Servicio";
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            cmbservicio.removeAllItems(); // Limpia el ComboBox antes de llenarlo

            while (rs.next()) {
                cmbservicio.addItem(rs.getString("Nombre_categoria"));
            }

            rs.close();
            ps.close();
            conn.close();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al cargar categorías: " + e.getMessage());
        }
    }

    private void cmbservicioActionPerformed(java.awt.event.ActionEvent evt) {
        if (cmbservicio.getSelectedItem() != null) {
            String categoriaSeleccionada = cmbservicio.getSelectedItem().toString();
            cargarAgendaPorCategoria(categoriaSeleccionada);
        }
    }

    private void cargarAgendaPorCategoria(String categoria) {

        try {
            Connection conn = new ConexionBD().conectar();
            String sql = """
            SELECT c.idCita, u.Nombre AS Cliente, s.Nombre_servicio, c.Fecha, c.Hora, c.Estado
            FROM cita c
            JOIN usuarios u ON c.idUsuarios = u.idUsuarios
            JOIN cita_has_servicios chs ON c.idCita = chs.idCita
            JOIN servicios s ON chs.idServicios = s.idServicios
            JOIN categoria_servicio cs ON s.idServicios = cs.idServicios
            WHERE cs.Nombre_categoria = ?
            ORDER BY c.Fecha, c.Hora;
        """;

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, categoria);
            ResultSet rs = ps.executeQuery();

            DefaultTableModel modelo = (DefaultTableModel) tabla.getModel();
            modelo.setRowCount(0);

            while (rs.next()) {
                Object[] fila = {
                    rs.getInt("idCita"),
                    rs.getString("Cliente"),
                    rs.getString("Nombre_servicio"),
                    rs.getDate("Fecha"),
                    rs.getTime("Hora"),
                    rs.getString("Estado")
                };
                modelo.addRow(fila);
            }

            rs.close();
            ps.close();
            conn.close();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error al cargar agenda: " + e.getMessage());
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
        jLabel3 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        tabla = new javax.swing.JTable();
        jLabel6 = new javax.swing.JLabel();
        lblfecha = new javax.swing.JLabel();
        cmbservicio = new javax.swing.JComboBox<>();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenuInicio = new javax.swing.JMenu();
        jMenuCitas = new javax.swing.JMenu();
        menuBuscarCitas = new javax.swing.JMenuItem();
        menuCitas = new javax.swing.JMenuItem();
        menuAgendarCita = new javax.swing.JMenuItem();
        jMenuPagos = new javax.swing.JMenu();
        menuPagoRestante = new javax.swing.JMenuItem();
        jMenuLogin = new javax.swing.JMenu();
        jMenuItem6 = new javax.swing.JMenuItem();

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
        jScrollPane1.setViewportView(jTable1);

        jLabel3.setText("Bienvenid@, Recepcionista! Hoy es ");

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

        lblfecha.setText("jLabel7");

        cmbservicio.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Maquillaje", "Uñas", "Peinados", " " }));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 930, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 422, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(46, 46, 46))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(29, 29, 29)
                .addComponent(cmbservicio, javax.swing.GroupLayout.PREFERRED_SIZE, 166, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(318, 318, 318)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel21)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblfecha, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel6)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(28, 28, 28)
                .addComponent(jLabel21)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(jLabel6)
                            .addComponent(lblfecha))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 39, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(cmbservicio, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)))
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, 241, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(64, 64, 64))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 136, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(15, 15, 15)
                                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 313, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(20, 20, 20)))
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

        menuPagoRestante.setText("Pago Restante");
        menuPagoRestante.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuPagoRestanteActionPerformed(evt);
            }
        });
        jMenuPagos.add(menuPagoRestante);

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

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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

    private void menuPagoRestanteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuPagoRestanteActionPerformed
        // TODO add your handling code here:
        NewJPagoRestante pago = new NewJPagoRestante(this);
        pago.setVisible(true);
        this.setVisible(false);
    }//GEN-LAST:event_menuPagoRestanteActionPerformed

    private void menuAgendarCitaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuAgendarCitaActionPerformed
        // TODO add your handling code here:
        NewJAgendarcita agendar = new NewJAgendarcita(this);
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
    private javax.swing.JComboBox<String> cmbservicio;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
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
    private javax.swing.JMenuItem jMenuItem6;
    private javax.swing.JMenu jMenuLogin;
    private javax.swing.JMenu jMenuPagos;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTable jTable1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField jTextField2;
    private javax.swing.JLabel lblfecha;
    private javax.swing.JMenuItem menuAgendarCita;
    private javax.swing.JMenuItem menuBuscarCitas;
    private javax.swing.JMenuItem menuCitas;
    private javax.swing.JMenuItem menuPagoRestante;
    private javax.swing.JTable tabla;
    // End of variables declaration//GEN-END:variables
}
