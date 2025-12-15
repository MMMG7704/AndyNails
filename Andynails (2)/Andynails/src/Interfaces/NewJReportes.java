/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package Interfaces;

import andynails.ConexionBD;
import andynails.RedesSociales;
import andynails.ReporteEstadistico;
import andynails.ReportePagos;
import andynails.ReporteServicios;
import andynails.SessionManager;
import javax.swing.JFrame;
import javax.swing.table.DefaultTableModel;
import java.io.File;

/**
 *
 * @author fanys
 */
public class NewJReportes extends javax.swing.JFrame {

    ConexionBD conexion;
    private String carpetaReportes = "C:\\Users\\mgmmo\\Documents\\7SEMESTRE\\INGENIERIASOF\\Reportes";
 private String tipoReporteActual = "servicios"; // Puede ser: "servicios", "pagos", "estadisticos"
    private String subtipoEstadistico = "Servicios más solicitados"; // Subtipo para estadísticos
    /**
     * Creates new form NewJReportes
     */
    public NewJReportes() {
        initComponents();
        RedesSociales.configurarRedesSociales(INS, WPP, FACE);
        conexion = new ConexionBD("andynails");
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Crear carpeta de reportes si no existe
        crearCarpetaReportes();
    }

    private void crearCarpetaReportes() {
        try {
            File carpeta = new File(carpetaReportes);
            if (!carpeta.exists()) {
                boolean creada = carpeta.mkdirs();
                if (creada) {
                    System.out.println(" Carpeta de reportes creada: " + carpetaReportes);
                } else {
                    System.out.println(" No se pudo crear la carpeta de reportes");
                }
            }
        } catch (Exception e) {
            System.err.println("Error al crear carpeta de reportes: " + e.getMessage());
        }
    }


    private void cargarServiciosDesdeBD() {
    try {
        String sql = "SELECT Nombre_servicio, Descripcion, Precio " +
                     "FROM servicios " +
                     "ORDER BY Nombre_servicio";
    
        java.sql.PreparedStatement ps = conexion.conectar().prepareStatement(sql);
        java.sql.ResultSet rs = ps.executeQuery();

        // SOLO MOSTRAR EN CONSOLA O EN UN MENSAJE
        System.out.println("=== SERVICIOS CARGADOS ===");
        int contador = 0;
        
        while (rs.next()) {
            contador++;
            System.out.println(contador + ". " + rs.getString("Nombre_servicio") + 
                             " - $" + rs.getDouble("Precio"));
        }
        
        rs.close();
        ps.close();
        
        // Mostrar mensaje al usuario
        javax.swing.JOptionPane.showMessageDialog(this,
            " " + contador + " servicios cargados correctamente",
            "Datos Cargados",
            javax.swing.JOptionPane.INFORMATION_MESSAGE);

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
        String sql = "SELECT p.idPago, IFNULL(u.Nombre, 'No asignado') as Cliente, " +
                    "p.Monto, p.Fecha_pago, p.Estado_pago " +
                    "FROM pago p " +
                    "LEFT JOIN cita c ON p.idPago = c.Pago_idPago " +
                    "LEFT JOIN usuarios u ON c.idUsuarios = u.idUsuarios " +
                    "ORDER BY p.Fecha_pago DESC";

        java.sql.PreparedStatement ps = conexion.conectar().prepareStatement(sql);
        java.sql.ResultSet rs = ps.executeQuery();

        // SOLO MOSTRAR EN CONSOLA
        System.out.println("=== PAGOS CARGADOS ===");
        int contador = 0;
        double total = 0;
        
        while (rs.next()) {
            contador++;
            double monto = rs.getDouble("Monto");
            total += monto;
            System.out.println(contador + ". ID: " + rs.getInt("idPago") + 
                             " - Cliente: " + rs.getString("Cliente") +
                             " - $" + monto);
        }
        
        rs.close();
        ps.close();
        
        // Mostrar mensaje al usuario
        javax.swing.JOptionPane.showMessageDialog(this,
            " " + contador + " pagos cargados\n" +
            " Total: $" + String.format("%.2f", total),
            "Datos Cargados",
            javax.swing.JOptionPane.INFORMATION_MESSAGE);

    } catch (Exception e) {
        e.printStackTrace();
        javax.swing.JOptionPane.showMessageDialog(this,
            " Error al cargar pagos: " + e.getMessage(),
            "Error",
            javax.swing.JOptionPane.ERROR_MESSAGE);
    }
}

private void cargarServiciosMasSolicitados() {
    try {
        String sql = "SELECT s.Nombre_servicio, COUNT(chs.idServicios) as Total_Citas, " +
                    "SUM(s.Precio) as Ingreso_Total " +
                    "FROM cita_has_servicios chs " +
                    "INNER JOIN servicios s ON chs.idServicios = s.idServicios " +
                    "GROUP BY s.Nombre_servicio " +
                    "ORDER BY Total_Citas DESC LIMIT 10";
        
        java.sql.PreparedStatement ps = conexion.conectar().prepareStatement(sql);
        java.sql.ResultSet rs = ps.executeQuery();

        // SOLO MOSTRAR EN CONSOLA
        System.out.println("=== SERVICIOS MÁS SOLICITADOS ===");
        int contador = 0;
        
        while (rs.next()) {
            contador++;
            System.out.println(contador + ". " + rs.getString("Nombre_servicio") + 
                             " - Veces: " + rs.getInt("Total_Citas") +
                             " - Ingreso: $" + rs.getDouble("Ingreso_Total"));
        }
        
        rs.close();
        ps.close();
        
        // Mostrar mensaje al usuario
        javax.swing.JOptionPane.showMessageDialog(this,
            " " + contador + " servicios populares cargados",
            "Datos Cargados",
            javax.swing.JOptionPane.INFORMATION_MESSAGE);

    } catch (Exception e) {
        e.printStackTrace();
        javax.swing.JOptionPane.showMessageDialog(this,
            " Error al cargar servicios más solicitados: " + e.getMessage(),
            "Error",
            javax.swing.JOptionPane.ERROR_MESSAGE);
    }
}
    
    
    private void actualizarBotonSeleccionado() {
    // Resetear todos los botones
    btnServicios.setBackground(null);
    btnPagos.setBackground(null);
    btnEstadisticos.setBackground(null);
    
    // Colorear el botón seleccionado
    switch (tipoReporteActual) {
        case "servicios":
            btnServicios.setBackground(new java.awt.Color(204, 255, 204)); // Verde claro
            break;
        case "pagos":
            btnPagos.setBackground(new java.awt.Color(255, 255, 204)); // Amarillo claro
            break;
        case "estadisticos":
            btnEstadisticos.setBackground(new java.awt.Color(204, 204, 255)); // Azul claro
            break;
    }
}
    
private void generarOExportarReporte() {
    try {
        // Verificar que se haya seleccionado un tipo
        if (tipoReporteActual == null || tipoReporteActual.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this,
                " Por favor, seleccione primero un tipo de reporte",
                "Selección Requerida",
                javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        String mensaje = "";
        
        // Ejecutar según el tipo
        switch (tipoReporteActual) {
            case "servicios":
                // ESTE SÍ RETORNA boolean
                if (ReporteServicios.generarPDF(carpetaReportes)) {
                    mensaje = " Reporte de servicios generado exitosamente!";
                } else {
                    throw new Exception("No se pudo generar el reporte de servicios");
                }
                break;
                
            case "pagos":
                // ESTE ES void - solo llamar
                ReportePagos.generarReportePagos(carpetaReportes);
                mensaje = " Reporte de pagos generado exitosamente!";
                break;
                
            case "estadisticos":
                // ESTE ES void - solo llamar
                ReporteEstadistico.generarReporteEstadistico(carpetaReportes);
                mensaje = " Reporte estadístico generado exitosamente!";
                break;
                
            default:
                throw new Exception("Tipo de reporte no reconocido: " + tipoReporteActual);
        }
        
        // Mostrar mensaje de éxito
        mensaje += "\n Guardado en: " + carpetaReportes;
        javax.swing.JOptionPane.showMessageDialog(this,
            mensaje,
            " Reporte Generado",
            javax.swing.JOptionPane.INFORMATION_MESSAGE);
        
    } catch (Exception e) {
        javax.swing.JOptionPane.showMessageDialog(this,
            "❌ Error: " + e.getMessage(),
            "Error",
            javax.swing.JOptionPane.ERROR_MESSAGE);
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
        jButton2 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();
        jButton1 = new javax.swing.JButton();
        btnEstadisticos = new javax.swing.JButton();
        btnPagos = new javax.swing.JButton();
        btnServicios = new javax.swing.JButton();
        jPanel10 = new javax.swing.JPanel();
        INS = new javax.swing.JLabel();
        FACE = new javax.swing.JLabel();
        WPP = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
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
        jMenu19 = new javax.swing.JMenu();
        jMenuItemCerrarSecion6 = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setBackground(new java.awt.Color(243, 224, 255));

        jPanel1.setBackground(new java.awt.Color(243, 224, 255));

        jPanel4.setBackground(new java.awt.Color(243, 224, 255));

        jLabel9.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jLabel9.setText("Generar Reportes:");

        jButton2.setBackground(new java.awt.Color(255, 204, 255));
        jButton2.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jButton2.setText("Regresar");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jButton3.setBackground(new java.awt.Color(255, 204, 255));
        jButton3.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jButton3.setText("Generar ");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        jButton1.setBackground(new java.awt.Color(255, 204, 255));
        jButton1.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        jButton1.setText("Exportar");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });

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

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(67, 67, 67)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jButton3)
                        .addGap(82, 82, 82)
                        .addComponent(jButton1))
                    .addComponent(jLabel9)
                    .addComponent(btnServicios, javax.swing.GroupLayout.PREFERRED_SIZE, 92, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnPagos, javax.swing.GroupLayout.PREFERRED_SIZE, 92, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnEstadisticos))
                .addGap(96, 96, 96)
                .addComponent(jButton2)
                .addContainerGap(100, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel9)
                .addGap(27, 27, 27)
                .addComponent(btnEstadisticos)
                .addGap(32, 32, 32)
                .addComponent(btnPagos)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 33, Short.MAX_VALUE)
                .addComponent(btnServicios)
                .addGap(21, 21, 21)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton3)
                    .addComponent(jButton1)
                    .addComponent(jButton2))
                .addContainerGap())
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
                .addGap(165, 165, 165)
                .addComponent(INS, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(191, 191, 191)
                .addComponent(WPP, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(185, 185, 185)
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

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/Img/logo.jpg"))); // NOI18N

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(31, 31, 31)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 239, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 187, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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
            .addComponent(jPanel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
   generarOExportarReporte();
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

    private void btnEstadisticosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEstadisticosActionPerformed
      // Cambiar tipo de reporte actual
    tipoReporteActual = "estadisticos";
    subtipoEstadistico = "Servicios más solicitados"; // Valor por defecto
    
    // Actualizar etiqueta
    jLabel9.setText("Generando reporte estadístico...");
    
    // Cargar datos automáticamente (el primero por defecto)
    cargarServiciosMasSolicitados();
    
    // Mostrar mensaje
    javax.swing.JOptionPane.showMessageDialog(this,
        " Datos estadísticos cargados.\n" +
        "Pulsa 'Generar' para crear el PDF.",
        "Estadísticos Seleccionados",
        javax.swing.JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_btnEstadisticosActionPerformed

    private void btnPagosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPagosActionPerformed
 // Cambiar tipo de reporte actual
    tipoReporteActual = "pagos";
    
    // Actualizar etiqueta
    jLabel9.setText("Generando reporte de pagos...");
    
    // Cargar datos automáticamente
    cargarPagosDesdeBD();
    
    // Mostrar mensaje
    javax.swing.JOptionPane.showMessageDialog(this,
        " Datos de pagos cargados.\n" +
        "Pulsa 'Generar' para crear el PDF.",
        "Pagos Seleccionados",
        javax.swing.JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_btnPagosActionPerformed

    private void btnServiciosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnServiciosActionPerformed
      // Cambiar tipo de reporte actual
    tipoReporteActual = "servicios";
    
    // Actualizar etiqueta
    jLabel9.setText("Generando reporte de servicios...");
    
    // Cargar datos automáticamente
    cargarServiciosDesdeBD();
    
    // Mostrar mensaje
    javax.swing.JOptionPane.showMessageDialog(this,
        "Datos de servicios cargados.\n" +
        "Pulsa 'Generar' para crear el PDF.",
        "Servicios Seleccionados",
          javax.swing.JOptionPane.INFORMATION_MESSAGE);
    }//GEN-LAST:event_btnServiciosActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
generarOExportarReporte();
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
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JMenu jMenu10;
    private javax.swing.JMenu jMenu11;
    private javax.swing.JMenu jMenu12;
    private javax.swing.JMenu jMenu19;
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
    private javax.swing.JMenuItem jMenuItemCerrarSecion6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel4;
    // End of variables declaration//GEN-END:variables
}
