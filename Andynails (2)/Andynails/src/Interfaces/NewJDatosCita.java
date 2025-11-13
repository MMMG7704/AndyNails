/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package Interfaces;

import andynails.ConexionBD;
import javax.swing.JFrame;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.JOptionPane;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.ImageIcon;

/**
 *
 * @author User
 */
public class NewJDatosCita extends javax.swing.JFrame {

    ConexionBD conexion = new ConexionBD();
    private int idCita = -1;
    private JFrame ventanaAnterior;

    public NewJDatosCita() {
        initComponents();
        // Bloquear edición de los campos
        txtnombrecliente.setEditable(false);
        txtFecha.setEditable(false);
        txthora.setEditable(false);
        txtdiseño.setEditable(false);
        txtnocita.setEditable(false);

// También los checkboxes del anticipo
        chksi.setEnabled(false);
        chkno.setEnabled(false);

// Quitar el foco de todos los campos para que no se pueda escribir ni seleccionar
        txtnombrecliente.setFocusable(false);
        txtFecha.setFocusable(false);
        txthora.setFocusable(false);
        txtdiseño.setFocusable(false);
        txtnocita.setFocusable(false);
        chksi.setFocusable(false);
        chkno.setFocusable(false);
        chkUnas.setFocusable(false);
        chkMaquillaje.setFocusable(false);
        chkPeinado.setFocusable(false);

    }

    // Constructor que recibe la ventana anterior y el idCita
    public NewJDatosCita(JFrame anterior, int idCita) {
        initComponents();
        this.ventanaAnterior = anterior;

        // Aquí puedes llamar un método que cargue los datos desde la BD
        cargarDatosCita(idCita);
    }

    public NewJDatosCita(JFrame ventanaAnterior) {
        initComponents();
        this.ventanaAnterior = ventanaAnterior;
        generarNumeroCita(); // genera automáticamente el número al abrir la ventana
    }

    private void cargarDatosCita(int idCita) {
        try (Connection con = conexion.conectar()) {
            String sql = """
            SELECT 
                c.idCita,
                CONCAT(u.Nombre, ' ', u.Paterno, ' ', u.Materno) AS nombre_completo,
                c.Fecha,
                c.Hora,
                c.Estado,
                s.Nombre_servicio
            FROM cita c
            JOIN usuarios u ON u.idUsuarios = c.idUsuarios
            LEFT JOIN cita_has_servicios cs ON cs.idCita = c.idCita
            LEFT JOIN servicios s ON s.idServicios = cs.idServicios
            WHERE c.idCita = ?
        """;

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, idCita);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        // Mostrar datos
                        txtnombrecliente.setText(rs.getString("nombre_completo"));
                        txtFecha.setText(rs.getString("Fecha"));
                        txthora.setText(rs.getString("Hora"));
                        txtdiseño.setText(rs.getString("Nombre_servicio") != null ? rs.getString("Nombre_servicio") : "Sin servicio");

                        // Marcar checkboxes según servicio
                        chkUnas.setSelected(false);
                        chkMaquillaje.setSelected(false);
                        chkPeinado.setSelected(false);

                        String servicio = rs.getString("Nombre_servicio");
                        if (servicio != null) {
                            String serv = servicio.toLowerCase();
                            if (serv.contains("uña") || serv.contains("uñas") || serv.contains("manicure") || serv.contains("pedicure")) {
                                chkUnas.setSelected(true);
                            }
                            if (serv.contains("maquill")) {
                                chkMaquillaje.setSelected(true);
                            }
                            if (serv.contains("peinado")) {
                                chkPeinado.setSelected(true);
                            }
                        }

                        // Marcar estado anticipo
                        String estado = rs.getString("Estado");
                        chksi.setSelected("Pagado".equalsIgnoreCase(estado) || "Anticipado".equalsIgnoreCase(estado));
                        chkno.setSelected(!chksi.isSelected());

                        // Número de cita
                        String sqlNum = "SELECT COUNT(*) AS num FROM cita WHERE Fecha = ? AND Hora <= ?";
                        try (PreparedStatement ps2 = con.prepareStatement(sqlNum)) {
                            ps2.setString(1, rs.getString("Fecha"));
                            ps2.setString(2, rs.getString("Hora"));
                            try (ResultSet rs2 = ps2.executeQuery()) {
                                if (rs2.next()) {
                                    txtnocita.setText(String.valueOf(rs2.getInt("num")));
                                }
                            }
                        }

                        // BLOQUEAR EDICIÓN: campos de texto y checkboxes
                        txtnombrecliente.setEditable(false);
                        txtFecha.setEditable(false);
                        txthora.setEditable(false);
                        txtdiseño.setEditable(false);
                        txtnocita.setEditable(false);

                        chksi.setEnabled(false);
                        chkno.setEnabled(false);
                        chkUnas.setEnabled(false);
                        chkMaquillaje.setEnabled(false);
                        chkPeinado.setEnabled(false);
                    } else {
                        JOptionPane.showMessageDialog(this, "No se encontró la cita con el ID especificado.");
                    }
                }
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar datos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void generarNumeroCita() {
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conexion = new ConexionBD();
            conn = conexion.getConnection();

            // Si el campo txtFecha está vacío, usar la fecha actual
            String fechaTexto = txtFecha.getText().trim();
            if (fechaTexto.isEmpty()) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                fechaTexto = sdf.format(new Date());
                txtFecha.setText(fechaTexto);
            }

            // Contar cuántas citas existen para esa fecha
            String sql = "SELECT COUNT(*) AS total FROM cita WHERE Fecha = ?";
            ps = conn.prepareStatement(sql);
            ps.setString(1, fechaTexto);
            rs = ps.executeQuery();

            if (rs.next()) {
                int total = rs.getInt("total");
                int numeroCita = total + 1; // siguiente en orden
                txtnocita.setText(String.valueOf(numeroCita));
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Error al generar número de cita: " + e.getMessage(),
                    "Error SQL",
                    JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (ps != null) {
                    ps.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
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
        jLabel1 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        txtnocita = new javax.swing.JTextField();
        txtnombrecliente = new javax.swing.JTextField();
        chkUnas = new javax.swing.JCheckBox();
        chkPeinado = new javax.swing.JCheckBox();
        chkMaquillaje = new javax.swing.JCheckBox();
        jLabel13 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        chksi = new javax.swing.JCheckBox();
        chkno = new javax.swing.JCheckBox();
        jLabel17 = new javax.swing.JLabel();
        jLabel18 = new javax.swing.JLabel();
        btnRegresar = new javax.swing.JButton();
        txthora = new javax.swing.JTextField();
        txtFecha = new javax.swing.JTextField();
        txtdiseño = new javax.swing.JTextField();
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

        jLabel1.setText("Nombre del cliente:");

        jLabel4.setText("Fecha");

        jLabel5.setText("Servicio a realizar:");

        jLabel8.setText("Numero de cita:");

        txtnocita.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtnocitaActionPerformed(evt);
            }
        });

        txtnombrecliente.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtnombreclienteActionPerformed(evt);
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

        jLabel13.setText("Diseño selecionado:");

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

        txthora.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txthoraActionPerformed(evt);
            }
        });

        txtFecha.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtFechaActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(45, 45, 45)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(jLabel1)
                                            .addComponent(jLabel8)
                                            .addComponent(jLabel5)
                                            .addComponent(jLabel13))
                                        .addGap(62, 62, 62)
                                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(txtnombrecliente, javax.swing.GroupLayout.PREFERRED_SIZE, 364, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(txtnocita, javax.swing.GroupLayout.PREFERRED_SIZE, 73, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(txtdiseño, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                .addComponent(btnRegresar)
                                                .addGroup(jPanel1Layout.createSequentialGroup()
                                                    .addComponent(chkUnas)
                                                    .addGap(18, 18, 18)
                                                    .addComponent(chkMaquillaje)
                                                    .addGap(18, 18, 18)
                                                    .addComponent(chkPeinado)))))
                                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                                            .addGap(168, 168, 168)
                                            .addComponent(chksi)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(chkno))
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(jLabel4)
                                                .addComponent(jLabel15)
                                                .addComponent(jLabel16)
                                                .addComponent(jLabel17))
                                            .addGap(64, 64, 64)
                                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                                    .addComponent(txtFecha, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addComponent(txthora, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addComponent(jLabel18))
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 13, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(347, 347, 347)
                                .addComponent(jLabel2)))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
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
                    .addComponent(txtnocita, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(chkUnas)
                    .addComponent(chkMaquillaje)
                    .addComponent(chkPeinado))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel13)
                    .addComponent(txtdiseño, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtFecha, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4))
                .addGap(15, 15, 15)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel15)
                    .addComponent(txthora, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel16)
                    .addComponent(chksi)
                    .addComponent(chkno))
                .addGap(23, 23, 23)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel17)
                    .addComponent(jLabel18)
                    .addComponent(btnRegresar))
                .addGap(18, 18, 18)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
            .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
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


    private void txtnocitaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtnocitaActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtnocitaActionPerformed

    private void txtnombreclienteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtnombreclienteActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtnombreclienteActionPerformed

    private void chkMaquillajeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_chkMaquillajeActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_chkMaquillajeActionPerformed

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

    private void txtFechaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtFechaActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtFechaActionPerformed

    private void txthoraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txthoraActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txthoraActionPerformed

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
            java.util.logging.Logger.getLogger(NewJDatosCita.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(NewJDatosCita.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(NewJDatosCita.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(NewJDatosCita.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
      

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new NewJDatosCita().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel FACE;
    private javax.swing.JLabel INS;
    private javax.swing.JLabel WPP;
    private javax.swing.JButton btnRegresar;
    private javax.swing.JCheckBox chkMaquillaje;
    private javax.swing.JCheckBox chkPeinado;
    private javax.swing.JCheckBox chkUnas;
    private javax.swing.JCheckBox chkno;
    private javax.swing.JCheckBox chksi;
    private javax.swing.JLabel jLabel1;
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
    private javax.swing.JTextField txtFecha;
    private javax.swing.JTextField txtdiseño;
    private javax.swing.JTextField txthora;
    private javax.swing.JTextField txtnocita;
    private javax.swing.JTextField txtnombrecliente;
    // End of variables declaration//GEN-END:variables
}
