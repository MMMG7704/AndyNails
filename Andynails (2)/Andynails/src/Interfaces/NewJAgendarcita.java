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
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Date;





/**
 *
 * @author User
 */
public class NewJAgendarcita extends javax.swing.JFrame {
ConexionBD conexion;
private int idCita = -1;



    /**
     * Creates new form NewJCitaAgenda
     */

// Constructor principal
 public NewJAgendarcita(int idCita) {
        initComponents();
        RedesSociales.configurarRedesSociales(INS, WPP, FACE);

        
        txtnombrecliente.setEditable(false);
        txttelefono.setEditable(false);
        jTextField6.setEditable(false);
        
        setLocationRelativeTo(null);
        conexion = new ConexionBD();
        this.idCita = idCita;
        jLabel1.setText("EDITAR CITA");
        cargarDatosCita(idCita);
    }
 
 private JFrame ventanaAnterior;

public NewJAgendarcita(JFrame ventanaAnterior) {
    initComponents();
    this.ventanaAnterior = ventanaAnterior;
}


    public NewJAgendarcita() {
        initComponents();
        setLocationRelativeTo(null);
        conexion = new ConexionBD();
        jLabel1.setText("REGISTRAR NUEVA CITA");
    }
    
public NewJAgendarcita(String idCita, String nombre, String telefono, 
                        String correo, String servicio, String fecha, 
                        String hora, String estado) {
    initComponents();
    setLocationRelativeTo(null);
    jLabel1.setText("EDITAR CITA");

    // Bloquear campos de texto
    txtnumerocita.setText(idCita);
    txtnumerocita.setEditable(false);

    txtnombrecliente.setText(nombre);
    txtnombrecliente.setEditable(false);

    txttelefono.setText(telefono);
    txttelefono.setEditable(false);

    jTextField6.setText(correo);
    jTextField6.setEditable(false);

    // Servicios
    chkUnas.setSelected(servicio.equalsIgnoreCase("Uñas acrilicas"));
    chkMaquillaje.setSelected(servicio.equalsIgnoreCase("Maquillaje"));
    chkPeinado.setSelected(servicio.equalsIgnoreCase("Peinado"));

    // Bloquear edición de servicios
    chkUnas.setEnabled(false);
    chkMaquillaje.setEnabled(false);
    chkPeinado.setEnabled(false);

    // Anticipo
    chksi.setSelected(estado.equalsIgnoreCase("Sí"));
    chkno.setSelected(estado.equalsIgnoreCase("No"));

    // Bloquear edición de anticipo
    chksi.setEnabled(false);
    chkno.setEnabled(false);

    // Hora
    jComboBox3hora.setSelectedItem(hora);
    jComboBox3hora.setEnabled(false);

    // Fecha al calendario
    try {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date fechaDate = sdf.parse(fecha);
        CalCitas.setDate(fechaDate);
        CalCitas.setEnabled(false); // Bloquear edición de fecha
    } catch (Exception e) {
        System.out.println("Error al asignar fecha: " + e.getMessage());
    }
}


    
    
 private void limpiarCampos() {
        txtnombrecliente.setText("");
        jComboBox3hora.setSelectedIndex(0);
        CalCitas.setDate(null);
        chkUnas.setSelected(false);
        chkMaquillaje.setSelected(false);
        chkPeinado.setSelected(false);
 } 

private void cargarDatosCita(int idCita) {
    try (Connection con = conexion.conectar()) {
        if (con == null) {
            JOptionPane.showMessageDialog(this, "No se pudo conectar a la base de datos.");
            return;
        }

        String sql = "SELECT c.Fecha, c.Hora, u.Nombre, s.Nombre AS Servicio, c.Anticipo " +
                     "FROM Cita c " +
                     "JOIN Usuarios u ON c.idUsuarios = u.idUsuarios " +
                     "LEFT JOIN cita_has_servicios chs ON c.idCita = chs.idCita " +
                     "LEFT JOIN Servicios s ON chs.idServicios = s.idServicios " +
                     "WHERE c.idCita = ?";

        PreparedStatement ps = con.prepareStatement(sql);
        ps.setInt(1, idCita);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            txtnombrecliente.setText(rs.getString("Nombre"));
            txtnombrecliente.setEditable(false);

            // Fecha
            Date fechaDate = rs.getDate("Fecha");
            if (fechaDate != null) {
                CalCitas.setDate(fechaDate);
                CalCitas.setEnabled(false);
            }

            // Hora
            String horaStr = rs.getString("Hora");
            if (horaStr != null && horaStr.length() >= 5) {
                horaStr = horaStr.substring(0,5);
            }
            jComboBox3hora.setSelectedItem(horaStr);
            jComboBox3hora.setEnabled(false);

            // Servicio
            String servicio = rs.getString("Servicio");
            chkUnas.setSelected("Uñas acrilicas".equalsIgnoreCase(servicio));
            chkMaquillaje.setSelected("Maquillaje".equalsIgnoreCase(servicio));
            chkPeinado.setSelected("Peinado".equalsIgnoreCase(servicio));
            chkUnas.setEnabled(false);
            chkMaquillaje.setEnabled(false);
            chkPeinado.setEnabled(false);

            // Anticipo
            String anticipo = rs.getString("Anticipo"); // asumiendo que la BD tiene este campo
            chksi.setSelected("Sí".equalsIgnoreCase(anticipo));
            chkno.setSelected("No".equalsIgnoreCase(anticipo));
            chksi.setEnabled(false);
            chkno.setEnabled(false);
        }

    } catch (SQLException e) {
        JOptionPane.showMessageDialog(this, "Error al cargar cita: " + e.getMessage());
    }
}



private void insertarServicio(int idCita, int idServicio) {
    String sql = "INSERT INTO cita_has_servicios (idCita, idServicios) VALUES (?, ?)";

    try (Connection con = conexion.conectar();
         PreparedStatement ps = con.prepareStatement(sql)) {

        ps.setInt(1, idCita);
        ps.setInt(2, idServicio);
        ps.executeUpdate();

    } catch (SQLException e) {
        JOptionPane.showMessageDialog(this, "Error al insertar servicio: " + e.getMessage());
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
        txttelefono = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        txtnumerocita = new javax.swing.JTextField();
        txtnombrecliente = new javax.swing.JTextField();
        jLabel12 = new javax.swing.JLabel();
        jTextField6 = new javax.swing.JTextField();
        chkUnas = new javax.swing.JCheckBox();
        chkPeinado = new javax.swing.JCheckBox();
        chkMaquillaje = new javax.swing.JCheckBox();
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
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu4 = new javax.swing.JMenu();
        jMenu5 = new javax.swing.JMenu();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenu6 = new javax.swing.JMenu();
        jMenuItem4 = new javax.swing.JMenuItem();
        jMenu7 = new javax.swing.JMenu();
        jMenuItem5 = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

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

        txttelefono.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txttelefonoActionPerformed(evt);
            }
        });

        jLabel11.setText("Telefono");

        txtnombrecliente.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtnombreclienteActionPerformed(evt);
            }
        });

        jLabel12.setText("Correo electronico");

        jTextField6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField6ActionPerformed(evt);
            }
        });

        chkUnas.setText("Uñas acrilicas");

        chkPeinado.setText("Peinado");
        chkPeinado.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkPeinadoActionPerformed(evt);
            }
        });

        chkMaquillaje.setText("Maquillaje");
        chkMaquillaje.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                chkMaquillajeActionPerformed(evt);
            }
        });

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
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                                    .addComponent(jLabel12)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jTextField6, javax.swing.GroupLayout.PREFERRED_SIZE, 364, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel1)
                                        .addComponent(jLabel11))
                                    .addGap(62, 62, 62)
                                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(txttelefono, javax.swing.GroupLayout.PREFERRED_SIZE, 364, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(txtnombrecliente, javax.swing.GroupLayout.PREFERRED_SIZE, 364, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addGroup(jPanel1Layout.createSequentialGroup()
                                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel8)
                                        .addComponent(jLabel5))
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                            .addComponent(chkUnas)
                                            .addGap(21, 21, 21)
                                            .addComponent(chkMaquillaje)
                                            .addGap(18, 18, 18)
                                            .addComponent(chkPeinado))
                                        .addComponent(txtnumerocita, javax.swing.GroupLayout.PREFERRED_SIZE, 364, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jComboBox1diseñoselecionado, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(CalCitas, javax.swing.GroupLayout.PREFERRED_SIZE, 355, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addComponent(jLabel13, javax.swing.GroupLayout.Alignment.LEADING))
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
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(txtnombrecliente, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel11)
                    .addComponent(txttelefono, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel12)
                    .addComponent(jTextField6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(txtnumerocita, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(28, 28, 28)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(chkUnas)
                    .addComponent(chkMaquillaje)
                    .addComponent(chkPeinado))
                .addGap(29, 29, 29)
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
private int obtenerIdUsuario(String nombreCliente) {
    int idUsuario = -1; // Valor por defecto si no se encuentra
    Connection con = conexion.conectar(); // Usa tu clase de conexión a la BD

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
        try { con.close(); } catch (SQLException ex) { /* ignorar */ }
    }

    return idUsuario;
}

    private void jMenuItem5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem5ActionPerformed
        // TODO add your handling code here:
        //boton de contacto
        NewJLogin NewJLogin = new NewJLogin();
        NewJLogin.setVisible(true);
        this.dispose(); // cierra la actual

    }//GEN-LAST:event_jMenuItem5ActionPerformed
private void registrarCitaNueva() {
    try (Connection con = conexion.conectar()) {
        if (con == null) {
            JOptionPane.showMessageDialog(this, "No se pudo conectar a la base de datos.");
            return;
        }

        // Validar datos obligatorios
        if (txtnombrecliente.getText().isEmpty() || CalCitas.getDate() == null || 
            jComboBox3hora.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Por favor completa todos los campos antes de registrar la cita.");
            return;
        }

        // Convertir fecha de CalCitas a java.sql.Date
        java.util.Date fechaSeleccionada = CalCitas.getDate();
        if (fechaSeleccionada == null) {
            JOptionPane.showMessageDialog(this, "Por favor selecciona una fecha válida.");
            return;
        }
        
        java.sql.Date fechaSQL = new java.sql.Date(fechaSeleccionada.getTime());

        // Convertir la hora seleccionada
        String horaStr = jComboBox3hora.getSelectedItem().toString();
        if (!horaStr.contains(":")) {
            horaStr += ":00";
        }
        
        // Asegurar formato HH:mm:ss
        if (horaStr.length() == 5) { // Si es "HH:mm"
            horaStr += ":00";
        }
        
        java.sql.Time horaSQL = java.sql.Time.valueOf(horaStr);

        // Obtener ID del usuario
        int idUsuario = obtenerIdUsuario(txtnombrecliente.getText());
        if (idUsuario == -1) {
            JOptionPane.showMessageDialog(this, "No se encontró el usuario especificado.");
            return;
        }

        // Insertar cita
        String sql = "INSERT INTO Cita (idUsuarios, Fecha, Hora) VALUES (?, ?, ?)";
        PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        ps.setInt(1, idUsuario);
        ps.setDate(2, fechaSQL);
        ps.setTime(3, horaSQL);
        
        int filasAfectadas = ps.executeUpdate();
        
        if (filasAfectadas > 0) {
            JOptionPane.showMessageDialog(this, "Cita registrada correctamente.");
            limpiarCampos();
            this.dispose();
        }

    } catch (SQLException e) {
        JOptionPane.showMessageDialog(this, "Error al registrar cita: " + e.getMessage());
        e.printStackTrace();
    } catch (IllegalArgumentException e) {
        JOptionPane.showMessageDialog(this, "Formato de hora inválido. Use HH:mm");
    }
}

private void actualizarCita() {
    try (Connection con = conexion.conectar()) {
        if (con == null) {
            JOptionPane.showMessageDialog(this, "No se pudo conectar a la base de datos.");
            return;
        }

        // Validar fecha
        if (CalCitas.getDate() == null) {
            JOptionPane.showMessageDialog(this, "Por favor selecciona una fecha antes de continuar.");
            return;
        }

        String sql = "UPDATE Cita SET Fecha = ?, Hora = ? WHERE idCita = ?";
        PreparedStatement ps = con.prepareStatement(sql);

        // Convertir fecha
        java.util.Date fechaSeleccionada = CalCitas.getDate();
        java.sql.Date fechaSQL = new java.sql.Date(fechaSeleccionada.getTime());

        // Procesar hora
        String horaStr = jComboBox3hora.getSelectedItem().toString();
        if (!horaStr.contains(":")) {
            horaStr += ":00";
        }
        if (horaStr.length() == 5) {
            horaStr += ":00";
        }

        ps.setDate(1, fechaSQL);
        ps.setString(2, horaStr);
        ps.setInt(3, idCita);

        int filasAfectadas = ps.executeUpdate();
        
        if (filasAfectadas > 0) {
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




    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
                                         
    // Determinar si estamos creando o editando una cita
    if (idCita > 0) {
        actualizarCita();
    } else {
        registrarCitaNueva();
    }
  
    }//GEN-LAST:event_jButton2ActionPerformed

    private void jComboBox1diseñoselecionadoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox1diseñoselecionadoActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jComboBox1diseñoselecionadoActionPerformed

    private void txttelefonoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txttelefonoActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txttelefonoActionPerformed

    private void txtnombreclienteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtnombreclienteActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtnombreclienteActionPerformed

    private void jTextField6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField6ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField6ActionPerformed

    private void chkMaquillajeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkMaquillajeActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_chkMaquillajeActionPerformed

    private void jComboBox3horaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBox3horaActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jComboBox3horaActionPerformed

    private void chkPeinadoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkPeinadoActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_chkPeinadoActionPerformed

    private void chknoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chknoActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_chknoActionPerformed

    private void btnRegresarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRegresarActionPerformed
                                            
    if (ventanaAnterior != null) {
        ventanaAnterior.setVisible(true);
        this.dispose();
    } else {
        this.dispose(); // fallback para evitar el NullPointer
    }

    }//GEN-LAST:event_btnRegresarActionPerformed

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
    private javax.swing.JCheckBox chkMaquillaje;
    private javax.swing.JCheckBox chkPeinado;
    private javax.swing.JCheckBox chkUnas;
    private javax.swing.JCheckBox chkno;
    private javax.swing.JCheckBox chksi;
    private javax.swing.JButton jButton2;
    private javax.swing.JComboBox<String> jComboBox1diseñoselecionado;
    private javax.swing.JComboBox<String> jComboBox3hora;
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
    private javax.swing.JMenu jMenu4;
    private javax.swing.JMenu jMenu5;
    private javax.swing.JMenu jMenu6;
    private javax.swing.JMenu jMenu7;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem jMenuItem5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JTextField jTextField6;
    private javax.swing.JTextField txtnombrecliente;
    private javax.swing.JTextField txtnumerocita;
    private javax.swing.JTextField txttelefono;
    // End of variables declaration//GEN-END:variables
}
