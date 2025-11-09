/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package Interfaces;

import andynails.ConexionBD;
import andynails.RedesSociales;
import java.sql.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import com.toedter.calendar.JCalendar;
import java.text.SimpleDateFormat;

/**
 *
 * @author User
 */
public class NewJCitaAgenda extends javax.swing.JFrame {

    ConexionBD conexion;
        DefaultTableModel modeloTabla;


    /**
     * Creates new form NewJCitaAgenda
     */
    public NewJCitaAgenda() {
 initComponents();
        conexion = new ConexionBD("andynails"); // Corregí el nombre de la BD
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        RedesSociales.configurarRedesSociales(INS, WPP, FACE);
        
        // Cargar datos en la tabla al iniciar
        cargarDatosEnTabla();
        
        // Agregar listener al calendario para búsqueda por fecha
      //jCalendar2.addPropertyChangeListener("date", evt -> buscarCitasPorFecha()); 
    }

private void cargarDatosEnTabla() {
    try {
        // Obtener el modelo de la tabla
        modeloTabla = (DefaultTableModel) jTablecontenidocitas.getModel();
        
        // Limpiar la tabla antes de cargar nuevos datos
        modeloTabla.setRowCount(0);
        
        // Consulta SQL adaptada a tu estructura de BD
        String consulta = "SELECT " +
                         "u.Nombre, " +
                         "u.Paterno, " + 
                         "u.Materno, " +
                         "u.Telefono, " +
                         "u.Correo, " +
                         "u.fecha_registro, " +
                         "COUNT(c.idCita) as numero_citas " +
                         "FROM usuarios u " +
                         "LEFT JOIN cita c ON u.idUsuarios = c.idUsuarios " +
                         "WHERE u.Tipo_Usuario_idTipo_Usuario = 2 " + // Solo clientes
                         "GROUP BY u.idUsuarios " +
                         "ORDER BY u.fecha_registro DESC";
        
        Connection conn = conexion.getConnection();
        PreparedStatement pst = conn.prepareStatement(consulta);
        ResultSet rs = pst.executeQuery();
        
        int contador = 0;
        // Llenar la tabla con los datos
        while (rs.next()) {
            contador++;
            
            // Construir nombre completo de forma segura
            String nombre = rs.getString("Nombre");
            String paterno = rs.getString("Paterno");
            String materno = rs.getString("Materno");
            
            String nombreCompleto = (nombre != null ? nombre : "") + " " + 
                                  (paterno != null ? paterno : "") + " " + 
                                  (materno != null ? materno : "");
            
            String telefono = rs.getString("Telefono");
            String correo = rs.getString("Correo");
            
            // OBTENER fecha_registro COMO STRING - SIN getDate()
            String fechaRegistroStr = rs.getString("fecha_registro");
            if (fechaRegistroStr == null) {
                fechaRegistroStr = "Sin fecha";
            }
            
            int numeroCitas = rs.getInt("numero_citas");
            
            // Agregar fila a la tabla
            modeloTabla.addRow(new Object[]{
                nombreCompleto.trim(), 
                telefono != null ? telefono : "", 
                correo != null ? correo : "", 
                fechaRegistroStr, // Usar el String directamente
                numeroCitas
            });
        }
        
        rs.close();
        pst.close();
        
        if (contador > 0) {
            JOptionPane.showMessageDialog(this, "Se cargaron " + contador + " clientes correctamente", 
                                    "Éxito", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "No se encontraron clientes", 
                                    "Información", JOptionPane.INFORMATION_MESSAGE);
        }
        
    } catch (SQLException e) {
        JOptionPane.showMessageDialog(this, "Error al cargar datos: " + e.getMessage(), 
                                    "Error", JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Error inesperado: " + e.getMessage(), 
                                    "Error", JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
    }
}
    
        private void registrarCita() {
        try {
            // Formulario simple para registrar nueva cita
            String telefono = JOptionPane.showInputDialog(this, "Ingrese el teléfono del cliente:");
            if (telefono == null || telefono.trim().isEmpty()) {
                return;
            }
            
            // Verificar si el cliente existe
            String verificarCliente = "SELECT idUsuarios FROM usuarios WHERE Telefono = ? AND Tipo_Usuario_idTipo_Usuario = 2";
            Connection conn = conexion.getConnection();
            PreparedStatement pstVerificar = conn.prepareStatement(verificarCliente);
            pstVerificar.setString(1, telefono);
            ResultSet rs = pstVerificar.executeQuery();
            
            if (!rs.next()) {
                JOptionPane.showMessageDialog(this, "Cliente no encontrado. Registre al cliente primero.", 
                                            "Error", JOptionPane.ERROR_MESSAGE);
                rs.close();
                pstVerificar.close();
                return;
            }
            
            int idUsuario = rs.getInt("idUsuarios");
            rs.close();
            pstVerificar.close();
            
            // Solicitar datos de la cita
            String fecha = JOptionPane.showInputDialog(this, "Ingrese la fecha (YYYY-MM-DD):");
            String hora = JOptionPane.showInputDialog(this, "Ingrese la hora (HH:MM:SS):");
            String estado = "Confirmada"; // Estado por defecto
            
            // Insertar nueva cita
            String insertarCita = "INSERT INTO cita (idUsuarios, Fecha, Hora, Estado, Pago_idPago) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement pstInsert = conn.prepareStatement(insertarCita);
            pstInsert.setInt(1, idUsuario);
            pstInsert.setString(2, fecha);
            pstInsert.setString(3, hora);
            pstInsert.setString(4, estado);
            pstInsert.setInt(5, 1); // Pago por defecto
            
            int filasAfectadas = pstInsert.executeUpdate();
            pstInsert.close();
            
            if (filasAfectadas > 0) {
                JOptionPane.showMessageDialog(this, "Cita registrada correctamente", 
                                            "Éxito", JOptionPane.INFORMATION_MESSAGE);
                // Recargar datos
                cargarDatosEnTabla();
            }
            
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error al registrar cita: " + e.getMessage(), 
                                        "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage(), 
                                        "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

private void editarCita() {
    try {
        // Solicitar teléfono del cliente 
        String telefono = JOptionPane.showInputDialog(this, "Ingrese el teléfono del cliente a editar:");
        if (telefono == null || telefono.trim().isEmpty()) {
            return;
        }
        
        // Verificar si el cliente existe
        String verificarCliente = "SELECT idUsuarios, Nombre FROM usuarios WHERE Telefono = ? AND Tipo_Usuario_idTipo_Usuario = 2";
        Connection conn = conexion.getConnection();
        PreparedStatement pstVerificar = conn.prepareStatement(verificarCliente);
        pstVerificar.setString(1, telefono);
        ResultSet rsVerificar = pstVerificar.executeQuery();
        
        if (!rsVerificar.next()) {
            JOptionPane.showMessageDialog(this, "Cliente no encontrado con el teléfono: " + telefono, 
                                        "Error", JOptionPane.ERROR_MESSAGE);
            rsVerificar.close();
            pstVerificar.close();
            return;
        }
        
        String nombreCliente = rsVerificar.getString("Nombre");
        int idUsuario = rsVerificar.getInt("idUsuarios");
        rsVerificar.close();
        pstVerificar.close();
        
        // Mostrar citas del cliente
        String consultaCitas = "SELECT c.Fecha, c.Hora, c.Estado " +
                              "FROM cita c " +
                              "WHERE c.idUsuarios = ?";
        PreparedStatement pst = conn.prepareStatement(consultaCitas);
        pst.setInt(1, idUsuario);
        ResultSet rs = pst.executeQuery();
        
        StringBuilder citas = new StringBuilder("Citas de " + nombreCliente + " (Tel: " + telefono + "):\n");
        int contadorCitas = 0;
        while (rs.next()) {
            contadorCitas++;
            String fechaCita = rs.getString("Fecha");
            citas.append(contadorCitas).append(". Fecha: ").append(fechaCita)
                 .append(" - Hora: ").append(rs.getString("Hora"))
                 .append(" - Estado: ").append(rs.getString("Estado"))
                 .append("\n");
        }
        rs.close();
        pst.close();
        
        if (contadorCitas == 0) {
            JOptionPane.showMessageDialog(this, "El cliente no tiene citas registradas", 
                                        "Información", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // Solicitar número de cita a editar (por posición)
        String numeroCitaStr = JOptionPane.showInputDialog(this, 
            citas.toString() + "\nIngrese el número de la cita a editar (1, 2, 3...):");
        if (numeroCitaStr == null || numeroCitaStr.trim().isEmpty()) {
            return;
        }
        
        int numeroCita = Integer.parseInt(numeroCitaStr);
        
        // Obtener la cita específica por posición
        pst = conn.prepareStatement(consultaCitas);
        pst.setInt(1, idUsuario);
        rs = pst.executeQuery();
        
        String fechaActual = "";
        String horaActual = "";
        String estadoActual = "";
        int contador = 0;
        
        while (rs.next()) {
            contador++;
            if (contador == numeroCita) {
                fechaActual = rs.getString("Fecha");
                horaActual = rs.getString("Hora");
                estadoActual = rs.getString("Estado");
                break;
            }
        }
        rs.close();
        pst.close();
        
        if (fechaActual.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Número de cita inválido", 
                                        "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Solicitar nuevos datos con valores actuales como sugerencia
        String nuevaFecha = JOptionPane.showInputDialog(this, "Nueva fecha (YYYY-MM-DD):", fechaActual);
        String nuevaHora = JOptionPane.showInputDialog(this, "Nueva hora (HH:MM:SS):", horaActual);
        String nuevoEstado = JOptionPane.showInputDialog(this, "Nuevo estado:", estadoActual);
        
        // Actualizar cita usando teléfono y fecha/hora
        String actualizarCita = "UPDATE cita c " +
                               "JOIN usuarios u ON c.idUsuarios = u.idUsuarios " +
                               "SET c.Fecha = ?, c.Hora = ?, c.Estado = ? " +
                               "WHERE u.Telefono = ? AND c.Fecha = ? AND c.Hora = ?";
        PreparedStatement pstUpdate = conn.prepareStatement(actualizarCita);
        pstUpdate.setString(1, nuevaFecha);
        pstUpdate.setString(2, nuevaHora);
        pstUpdate.setString(3, nuevoEstado);
        pstUpdate.setString(4, telefono);
        pstUpdate.setString(5, fechaActual);
        pstUpdate.setString(6, horaActual);
        
        int filasAfectadas = pstUpdate.executeUpdate();
        pstUpdate.close();
        
        if (filasAfectadas > 0) {
            JOptionPane.showMessageDialog(this, "Cita actualizada correctamente", 
                                        "Éxito", JOptionPane.INFORMATION_MESSAGE);
            cargarDatosEnTabla();
        } else {
            JOptionPane.showMessageDialog(this, "No se pudo actualizar la cita", 
                                        "Error", JOptionPane.ERROR_MESSAGE);
        }
        
    } catch (SQLException e) {
        JOptionPane.showMessageDialog(this, "Error al editar cita: " + e.getMessage(), 
                                    "Error", JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
    } catch (NumberFormatException e) {
        JOptionPane.showMessageDialog(this, "Número de cita inválido", 
                                    "Error", JOptionPane.ERROR_MESSAGE);
    }
}

            
private void eliminarCita() {
    try {
        // Solicitar teléfono del cliente
        String telefono = JOptionPane.showInputDialog(this, "Ingrese el teléfono del cliente para eliminar cita:");
        if (telefono == null || telefono.trim().isEmpty()) {
            return;
        }
        
        // Verificar si el cliente existe
        String verificarCliente = "SELECT idUsuarios, Nombre FROM usuarios WHERE Telefono = ? AND Tipo_Usuario_idTipo_Usuario = 2";
        Connection conn = conexion.getConnection();
        PreparedStatement pstVerificar = conn.prepareStatement(verificarCliente);
        pstVerificar.setString(1, telefono);
        ResultSet rsVerificar = pstVerificar.executeQuery();
        
        if (!rsVerificar.next()) {
            JOptionPane.showMessageDialog(this, "Cliente no encontrado con el teléfono: " + telefono, 
                                        "Error", JOptionPane.ERROR_MESSAGE);
            rsVerificar.close();
            pstVerificar.close();
            return;
        }
        
        String nombreCliente = rsVerificar.getString("Nombre");
        int idUsuario = rsVerificar.getInt("idUsuarios");
        rsVerificar.close();
        pstVerificar.close();
        
        // Mostrar citas del cliente
        String consultaCitas = "SELECT c.Fecha, c.Hora, c.Estado " +
                              "FROM cita c " +
                              "WHERE c.idUsuarios = ?";
        PreparedStatement pst = conn.prepareStatement(consultaCitas);
        pst.setInt(1, idUsuario);
        ResultSet rs = pst.executeQuery();
        
        StringBuilder citas = new StringBuilder("Citas de " + nombreCliente + " (Tel: " + telefono + "):\n");
        int contadorCitas = 0;
        while (rs.next()) {
            contadorCitas++;
            String fechaCita = rs.getString("Fecha");
            citas.append(contadorCitas).append(". Fecha: ").append(fechaCita)
                 .append(" - Hora: ").append(rs.getString("Hora"))
                 .append(" - Estado: ").append(rs.getString("Estado"))
                 .append("\n");
        }
        rs.close();
        pst.close();
        
        if (contadorCitas == 0) {
            JOptionPane.showMessageDialog(this, "El cliente no tiene citas registradas", 
                                        "Información", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        
        // Solicitar número de cita a eliminar
        String numeroCitaStr = JOptionPane.showInputDialog(this, 
            citas.toString() + "\nIngrese el número de la cita a eliminar (1, 2, 3...):");
        if (numeroCitaStr == null || numeroCitaStr.trim().isEmpty()) {
            return;
        }
        
        int numeroCita = Integer.parseInt(numeroCitaStr);
        
        // Obtener la cita específica por posición
        pst = conn.prepareStatement(consultaCitas);
        pst.setInt(1, idUsuario);
        rs = pst.executeQuery();
        
        String fechaEliminar = "";
        String horaEliminar = "";
        int contador = 0;
        
        while (rs.next()) {
            contador++;
            if (contador == numeroCita) {
                fechaEliminar = rs.getString("Fecha");
                horaEliminar = rs.getString("Hora");
                break;
            }
        }
        rs.close();
        pst.close();
        
        if (fechaEliminar.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Número de cita inválido", 
                                        "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int confirmacion = JOptionPane.showConfirmDialog(this, 
            "¿Está seguro de eliminar la cita?\n" +
            "Cliente: " + nombreCliente + "\n" +
            "Fecha: " + fechaEliminar + "\n" +
            "Hora: " + horaEliminar, 
            "Confirmar Eliminación", 
            JOptionPane.YES_NO_OPTION);
        
        if (confirmacion == JOptionPane.YES_OPTION) {
            // Eliminar usando teléfono y fecha/hora
            String eliminarCita = "DELETE c FROM cita c " +
                                 "JOIN usuarios u ON c.idUsuarios = u.idUsuarios " +
                                 "WHERE u.Telefono = ? AND c.Fecha = ? AND c.Hora = ?";
            PreparedStatement pstDelete = conn.prepareStatement(eliminarCita);
            pstDelete.setString(1, telefono);
            pstDelete.setString(2, fechaEliminar);
            pstDelete.setString(3, horaEliminar);
            
            int filasAfectadas = pstDelete.executeUpdate();
            pstDelete.close();
            
            if (filasAfectadas > 0) {
                JOptionPane.showMessageDialog(this, "Cita eliminada correctamente", 
                                            "Éxito", JOptionPane.INFORMATION_MESSAGE);
                cargarDatosEnTabla();
            } else {
                JOptionPane.showMessageDialog(this, "No se pudo eliminar la cita", 
                                            "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        
    } catch (SQLException e) {
        JOptionPane.showMessageDialog(this, "Error al eliminar cita: " + e.getMessage(), 
                                    "Error", JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
    } catch (NumberFormatException e) {
        JOptionPane.showMessageDialog(this, "Número de cita inválido", 
                                    "Error", JOptionPane.ERROR_MESSAGE);
    }

    }

    /**
     * Método para buscar citas por fecha (usando el JCalendar)
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
        btneditarcita = new javax.swing.JButton();
        btnregistrarcita = new javax.swing.JButton();
        btneliminarcita = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTablecontenidocitas = new javax.swing.JTable();
        jCalendar3 = new com.toedter.calendar.JCalendar();
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
        jLabel2.setText("CITAS AGENDADAS");

        btneditarcita.setBackground(new java.awt.Color(255, 204, 255));
        btneditarcita.setText("Editar cita");
        btneditarcita.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btneditarcitaActionPerformed(evt);
            }
        });

        btnregistrarcita.setBackground(new java.awt.Color(255, 204, 255));
        btnregistrarcita.setText("Registar cita");
        btnregistrarcita.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnregistrarcitaActionPerformed(evt);
            }
        });

        btneliminarcita.setBackground(new java.awt.Color(255, 204, 255));
        btneliminarcita.setText("Eliminar cita");
        btneliminarcita.setToolTipText("");
        btneliminarcita.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btneliminarcitaActionPerformed(evt);
            }
        });

        jTablecontenidocitas.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "Nombre del Cliente", "Teléfono", "Correo Electrónico", "Fecha Registro", "Número de citas"
            }
        ));
        jScrollPane1.setViewportView(jTablecontenidocitas);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(28, 28, 28)
                        .addComponent(btnregistrarcita, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(93, 93, 93)
                        .addComponent(btneditarcita, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(72, 72, 72)
                        .addComponent(btneliminarcita)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(41, 41, 41)
                        .addComponent(jCalendar3, javax.swing.GroupLayout.PREFERRED_SIZE, 206, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 43, Short.MAX_VALUE)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 566, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(jLabel2)
                .addGap(360, 360, 360))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(jLabel2)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(26, 26, 26)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 233, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 60, Short.MAX_VALUE)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnregistrarcita)
                            .addComponent(btneliminarcita)
                            .addComponent(btneditarcita))
                        .addGap(18, 18, 18)
                        .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(51, 51, 51)
                        .addComponent(jCalendar3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(0, 0, Short.MAX_VALUE))))
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
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btneditarcitaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btneditarcitaActionPerformed
        // TODO add your handling code here:
                editarCita();

    }//GEN-LAST:event_btneditarcitaActionPerformed

    private void btneliminarcitaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btneliminarcitaActionPerformed
        // TODO add your handling code here:
                eliminarCita();

    }//GEN-LAST:event_btneliminarcitaActionPerformed

    private void btnregistrarcitaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnregistrarcitaActionPerformed
        // TODO add your handling code here:
                registrarCita();

    }//GEN-LAST:event_btnregistrarcitaActionPerformed

    private void jMenuItem5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem5ActionPerformed
        // TODO add your handling code here:
        //boton de contacto
        NewJContacto NewJContacto = new NewJContacto();
        NewJContacto.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem5ActionPerformed

    private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem3ActionPerformed
        // TODO add your handling code here:
        //para maquillaje
        NewJCatalogoMaq NewJCatalogoMaq = new NewJCatalogoMaq();
        NewJCatalogoMaq.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem3ActionPerformed

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        // TODO add your handling code here:
        //para abrir peinados
        NewJCatalogoPeinado NewJCatalogoPeinado = new NewJCatalogoPeinado();
        NewJCatalogoPeinado.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        // TODO add your handling code here:
        //para arir uñas
        NewJCatalogoUñas NewJCatalogoUñas = new NewJCatalogoUñas();
        NewJCatalogoUñas.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void jMenu4MenuSelected(javax.swing.event.MenuEvent evt) {//GEN-FIRST:event_jMenu4MenuSelected
        // TODO add your handling code here:
        //inicio
        Inicio Inicio = new Inicio();
        Inicio.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenu4MenuSelected

    private void jMenuItem6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem6ActionPerformed
        // TODO add your handling code here:
        NewJAgendarcita NewJAgendarcita = new NewJAgendarcita();
        NewJAgendarcita.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem6ActionPerformed

    private void jMenuItem7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem7ActionPerformed
        // TODO add your handling code here:
        JOptionPane.showMessageDialog(this, "Funcionalidad de cancelar cita");
    }//GEN-LAST:event_jMenuItem7ActionPerformed

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

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new NewJCitaAgenda().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel FACE;
    private javax.swing.JLabel INS;
    private javax.swing.JLabel WPP;
    private javax.swing.JButton btneditarcita;
    private javax.swing.JButton btneliminarcita;
    private javax.swing.JButton btnregistrarcita;
    private com.toedter.calendar.JCalendar jCalendar1;
    private com.toedter.calendar.JCalendar jCalendar3;
    private javax.swing.JLabel jLabel2;
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
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTablecontenidocitas;
    // End of variables declaration//GEN-END:variables

}
