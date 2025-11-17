/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package Interfaces;

import andynails.ConexionBD;
import andynails.DisponibilidadManager;
import andynails.RedesSociales;
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

/**
 *
 * @author User
 */
public class NewJCitaAgenda extends javax.swing.JFrame {

    ConexionBD conexion;
    DisponibilidadManager dispManager;
    private JFrame ventanaAnterior;


    public NewJCitaAgenda() {
        initComponents();
        inicializarComponentes();
    }

    public NewJCitaAgenda(JFrame anterior) {
        initComponents();
        this.ventanaAnterior = anterior;
        inicializarComponentes();
    }

    private void inicializarComponentes() {
        RedesSociales.configurarRedesSociales(INS, WPP, FACE);
        conexion = new ConexionBD();
        dispManager = new DisponibilidadManager(conexion);

        llenarComboRoles();
        configurarCalendario();
    }
    
    private void configurarCalendario() {
        CalCitasAgendadas.addPropertyChangeListener("calendar", evt -> {
            java.util.Calendar cal = CalCitasAgendadas.getCalendar();
            if (cal != null) {
                Date fechaSeleccionada = cal.getTime();
                cargarCitasPorFecha(fechaSeleccionada);
            }
        });
    }

    private void regresar() {
        if (ventanaAnterior != null) {
            ventanaAnterior.setVisible(true);
        }
        this.dispose();
    }

    private void llenarComboRoles() {
      comboRoles.removeAllItems();
        comboRoles.addItem("Todos");
        comboRoles.addItem("Manicurista");
        comboRoles.addItem("Estilista");
        comboRoles.addItem("Maquillista");
        comboRoles.addItem("Tatuadora"); 

        comboRoles.addActionListener(e -> {
            java.util.Calendar cal = CalCitasAgendadas.getCalendar();
            if (cal != null) {
                Date fecha = cal.getTime();
                cargarCitasPorFecha(fecha);
            }
        });
    }
    
    private void cargarCitasPorFecha(Date fechaSeleccionada) {
        DefaultTableModel model = (DefaultTableModel) jTablecontenidocitas.getModel();
        model.setRowCount(0);

        if (fechaSeleccionada == null) {
            return;
        }

        String rolSeleccionado = comboRoles.getSelectedItem() != null ? comboRoles.getSelectedItem().toString() : "Todos";

        StringBuilder sql = new StringBuilder("""
            SELECT c.idCita,
                   CONCAT(u.Nombre, ' ', u.Paterno, ' ', u.Materno) AS Cliente,
                   u.Telefono, u.Correo,
                   s.Nombre_servicio AS Servicio,
                   c.Fecha, c.Hora,
                   CASE 
                      WHEN phs.Pago_idPago IS NULL THEN 'Pendiente'
                      WHEN p.Estado_pago = 'Validado' THEN 'Agendada'
                      ELSE 'Pendiente'
                   END AS Estado
            FROM Cita c
            INNER JOIN Usuarios u ON c.idUsuarios = u.idUsuarios
            INNER JOIN Cita_has_Servicios phs ON c.idCita = phs.idCita
            INNER JOIN Servicios s ON phs.idServicios = s.idServicios
            LEFT JOIN Pago p ON phs.Pago_idPago = p.idPago
            WHERE c.Fecha = ?
            """);

        if (!rolSeleccionado.equals("Todos")) {
            switch (rolSeleccionado) {
                case "Manicurista":
                    sql.append(" AND s.Nombre_servicio LIKE '%Uñas%'");
                    break;
                case "Estilista":
                    sql.append(" AND s.Nombre_servicio LIKE '%Peinado%'");
                    break;
                case "Maquillista":
                    sql.append(" AND s.Nombre_servicio LIKE '%Maquillaje%'");
                    break;
                case "Tatuadora":
                    sql.append(" AND s.Nombre_servicio LIKE '%Tatuajes%'");
                    break;
            }
        }

        sql.append(" ORDER BY c.Hora");

        try (Connection con = conexion.conectar(); 
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            ps.setDate(1, new java.sql.Date(fechaSeleccionada.getTime()));
            
            ResultSet rs = ps.executeQuery();
            
            while (rs.next()) {
              model.addRow(new Object[]{
                    rs.getString("Cliente"),
                    rs.getString("Telefono"),
                    rs.getString("Correo"),
                    rs.getString("Servicio"),
                    rs.getDate("Fecha"),
                    rs.getString("Hora"),
                    rs.getString("Estado")
                });
            }

            if (model.getRowCount() == 0) {
                if (fechaSeleccionada != null) {
                    JOptionPane.showMessageDialog(this, "No hay citas para la fecha seleccionada.", "Información", JOptionPane.INFORMATION_MESSAGE);
                }
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al cargar citas: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void actualizarTabla() {
        java.util.Calendar cal = CalCitasAgendadas.getCalendar();
        if (cal != null) {
            Date fecha = cal.getTime();
            cargarCitasPorFecha(fecha);
        }
    }    
    
        private String obtenerIdCita(String nombreCliente, String telefono, String fecha, String hora) {
        String sql = """
            SELECT c.idCita 
            FROM Cita c 
            INNER JOIN Usuarios u ON c.idUsuarios = u.idUsuarios 
            WHERE CONCAT(u.Nombre, ' ', u.Paterno, ' ', u.Materno) = ? 
            AND u.Telefono = ? 
            AND c.Fecha = ? 
            AND c.Hora = ?
            """;
        
        try (Connection con = conexion.conectar(); 
             PreparedStatement ps = con.prepareStatement(sql)) {
            
            ps.setString(1, nombreCliente);
            ps.setString(2, telefono);
            ps.setString(3, fecha);
            ps.setString(4, hora);
            
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("idCita");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al buscar ID de cita: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        return null;
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
        INS = new javax.swing.JLabel();
        FACE = new javax.swing.JLabel();
        WPP = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        btnEditarCita = new javax.swing.JButton();
        btnRegistrarCita = new javax.swing.JButton();
        btnEliminarCita = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTablecontenidocitas = new javax.swing.JTable();
        comboRoles = new javax.swing.JComboBox<>();
        btnRegresar = new javax.swing.JButton();
        CalCitasAgendadas = new com.toedter.calendar.JCalendar();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu4 = new javax.swing.JMenu();
        jMenu5 = new javax.swing.JMenu();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 973, Short.MAX_VALUE)
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
        jLabel2.setText("CITAS AGENDADAS");

        btnEditarCita.setBackground(new java.awt.Color(255, 204, 255));
        btnEditarCita.setText("Editar cita");
        btnEditarCita.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEditarCitaActionPerformed(evt);
            }
        });

        btnRegistrarCita.setBackground(new java.awt.Color(255, 204, 255));
        btnRegistrarCita.setText("Registar cita");
        btnRegistrarCita.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRegistrarCitaActionPerformed(evt);
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
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null}
            },
            new String [] {
                "Nombre del Cliente", "Teléfono", "Correo Electrónico", "Nombre de servicio", "Fecha ", "Hora", "Estado"
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

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(28, 28, 28)
                        .addComponent(btnRegistrarCita, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(93, 93, 93)
                        .addComponent(btnEditarCita, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(72, 72, 72)
                        .addComponent(btnEliminarCita))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(101, 101, 101)
                        .addComponent(CalCitasAgendadas, javax.swing.GroupLayout.PREFERRED_SIZE, 298, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(51, 51, 51)
                        .addComponent(btnRegresar))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(comboRoles, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(216, 216, 216)
                                .addComponent(jLabel2))
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 935, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(23, 23, 23)
                        .addComponent(jLabel2))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(comboRoles, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 233, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(CalCitasAgendadas, javax.swing.GroupLayout.PREFERRED_SIZE, 221, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 37, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnRegistrarCita)
                    .addComponent(btnEliminarCita)
                    .addComponent(btnEditarCita)
                    .addComponent(btnRegresar))
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

        jMenu6.setText("AGENDAR CITA");

        jMenuItem4.setText("CANCELAR CITA");
        jMenuItem4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem4ActionPerformed(evt);
            }
        });
        jMenu6.add(jMenuItem4);

        jMenuBar1.add(jMenu6);

        jMenu7.setText("CONTACTO");

        jMenuItem5.setText("Contacto");
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
        JOptionPane.showMessageDialog(this, "Selecciona una cita para editar.", "Advertencia", JOptionPane.WARNING_MESSAGE);
        return;
    }

    try {
        String idCitaStr = jTablecontenidocitas.getValueAt(fila, 7).toString(); // Asumiendo que el ID está en la columna 7
        int idCita = Integer.parseInt(idCitaStr);
        
        // Usar el constructor simple que ya tienes
        NewJAgendarcita editar = new NewJAgendarcita(idCita);
        editar.setVisible(true);
        this.dispose();
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Error al abrir edición: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
    }//GEN-LAST:event_btnEditarCitaActionPerformed

    private void btnEliminarCitaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEliminarCitaActionPerformed
        int fila = jTablecontenidocitas.getSelectedRow();
        if (fila == -1) {
            JOptionPane.showMessageDialog(this, "Selecciona una cita para eliminar.", "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // Obtener datos para buscar el ID
String nombreCliente = jTablecontenidocitas.getValueAt(fila, 0).toString();
String telefono = jTablecontenidocitas.getValueAt(fila, 1).toString();
String fecha = jTablecontenidocitas.getValueAt(fila, 4).toString();
String hora = jTablecontenidocitas.getValueAt(fila, 5).toString();            
            String idCitaStr = obtenerIdCita(nombreCliente, telefono, fecha, hora);
            if (idCitaStr == null) {
                JOptionPane.showMessageDialog(this, "No se pudo encontrar la cita seleccionada.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            int idCita = Integer.parseInt(idCitaStr);

Object[] opciones = {"Sí", "No"};
int confirm = JOptionPane.showOptionDialog(this,
        "¿Estás seguro de que deseas eliminar esta cita?",
        "Confirmar eliminación",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.WARNING_MESSAGE,
        null,
        opciones,
        opciones[1]);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }

            try (Connection con = conexion.conectar()) {
                if (con == null) {
                    JOptionPane.showMessageDialog(this, "No se pudo conectar a la base de datos.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Eliminar relaciones en cita_has_servicios
                String sql1 = "DELETE FROM cita_has_servicios WHERE idCita = ?";
                try (PreparedStatement ps1 = con.prepareStatement(sql1)) {
                    ps1.setInt(1, idCita);
                    ps1.executeUpdate();
                }

                // Eliminar la cita principal
                String sql2 = "DELETE FROM cita WHERE idCita = ?";
                try (PreparedStatement ps2 = con.prepareStatement(sql2)) {
                    ps2.setInt(1, idCita);
                    ps2.executeUpdate();
                }

                JOptionPane.showMessageDialog(this, "Cita eliminada correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                actualizarTabla();
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Error: ID de cita inválido.", "Error", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al eliminar cita: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

    }//GEN-LAST:event_btnEliminarCitaActionPerformed

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

    private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem4ActionPerformed
        // TODO add your handling code here:
        //agendar cancelar
        NewJCancelarC NewJCancelarC = new NewJCancelarC();
        NewJCancelarC.setVisible(true);
        this.dispose(); // cierra la actual

    }//GEN-LAST:event_jMenuItem4ActionPerformed

    private void jMenuItem5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem5ActionPerformed
        // TODO add your handling code here:
        //boton de contacto
        NewJContacto NewJContacto = new NewJContacto();
        NewJContacto.setVisible(true);
        this.dispose(); // cierra la actual

    }//GEN-LAST:event_jMenuItem5ActionPerformed

    private void btnRegresarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRegresarActionPerformed
        // TODO add your handling code here:

        // Regresar al panel anterior
        NewJPanelAdministracionRec anterior = new NewJPanelAdministracionRec();
        anterior.setVisible(true);
        this.dispose(); // Cierra la ventana actual

    }//GEN-LAST:event_btnRegresarActionPerformed

    private void btnRegistrarCitaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRegistrarCitaActionPerformed
        // TODO add your handling code here:
        NewJAgendarcita registrar = new NewJAgendarcita();
        registrar.setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btnRegistrarCitaActionPerformed


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

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new NewJCitaAgenda().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private com.toedter.calendar.JCalendar CalCitasAgendadas;
    private javax.swing.JLabel FACE;
    private javax.swing.JLabel INS;
    private javax.swing.JLabel WPP;
    private javax.swing.JButton btnEditarCita;
    private javax.swing.JButton btnEliminarCita;
    private javax.swing.JButton btnRegistrarCita;
    private javax.swing.JButton btnRegresar;
    private javax.swing.JComboBox<String> comboRoles;
    private com.toedter.calendar.JCalendar jCalendar1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JMenu jMenu4;
    private javax.swing.JMenu jMenu5;
    private javax.swing.JMenu jMenu6;
    private javax.swing.JMenu jMenu7;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem jMenuItem5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTablecontenidocitas;
    // End of variables declaration//GEN-END:variables

}
