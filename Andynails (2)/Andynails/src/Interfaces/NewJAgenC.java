package Interfaces;

import andynails.ConexionBD;
import andynails.RedesSociales;
import andynails.SesionUsuario;
import javax.swing.JFrame;
import javax.swing.ImageIcon;
import java.awt.Image;
import javax.swing.Timer;
import com.toedter.calendar.JCalendar;
import javax.swing.JOptionPane;
import org.mariadb.jdbc.Connection;
import java.sql.SQLException;

/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
/**
 *
 * @author User
 */
public class NewJAgenC extends javax.swing.JFrame {

    // Declaro una variable para la conexión a la base de datos
    private int idCitaSeleccionada = 0; // Para guardar el ID de la cita seleccionada
    private static java.util.List<Object[]> serviciosSeleccionados = new java.util.ArrayList<>();
    private int indiceActual = 0;

    ConexionBD conexion;
    private ImageIcon imagenSeleccionada;
    private String precioSeleccionado;
    private String descripcionSeleccionada;
    private Timer timer;
    private int idUsuario; // variable de clase

    /**
     * Creates new form NewJAgenC
     */
    public NewJAgenC(int idUsuario) {
        initComponents(); // importante
                RedesSociales.configurarRedesSociales(INS, WPP, FACE);

        conexion = new ConexionBD("andynails");
        this.idUsuario = SesionUsuario.getIdUsuario();
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        //this.idUsuario = idUsuario;
        // Deshabilitar botones y combos si no hay usuario logueado
        boolean logueado = idUsuario != 0;
        jButton4.setEnabled(logueado);
        jButton4.setEnabled(false);
        cmbServicios.setEnabled(logueado);

        // Llenar combo de horas
        cbHora.removeAllItems();
        for (int hora = 9; hora <= 18; hora++) {
            cbHora.addItem(hora + ":00");
        }

        // Configurar calendario
        jDayChooser2.addPropertyChangeListener(evt -> {
            if ("day".equals(evt.getPropertyName())) {
                actualizarFecha();
            }
        });

        cbHora.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                String fecha = jTextFieldFecha1.getText().trim();
                String hora = (String) cbHora.getSelectedItem();

                if (!fecha.isEmpty() && hora != null) {
                    if (horaBloqueada(fecha, hora)) {
                        JOptionPane.showMessageDialog(NewJAgenC.this,
                                "Esta hora está bloqueada o no disponible. Por favor seleccione otra hora.",
                                "Hora no disponible",
                                JOptionPane.WARNING_MESSAGE);
                        // Opcional: resetear la selección
                        // cbHora.setSelectedIndex(-1);
                    }
                }
            }
        });

        // Mostrar servicios seleccionados si los hay
        if (!serviciosSeleccionados.isEmpty()) {
            indiceActual = 0;
            mostrarServiciosSeleccionados();
            timer = new Timer(3000, e -> siguienteServicio());
            timer.start();
        } else {
            jLabel4.setIcon(null);
            label11.setText("No hay servicios seleccionados");
            jLabel8.setText("$0");
        }
    }

    public NewJAgenC() {
        initComponents();
        conexion = new ConexionBD("andynails");

        this.idUsuario = SesionUsuario.getIdUsuario();

        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        //this.idUsuario = 0; // nadie logueado

        System.out.println("DEBUG - Constructor sin parámetros - ID Usuario: " + idUsuario);
        debugCategoriasServicios();

        // Llenar combo de horas
        cbHora.removeAllItems();
        for (int hora = 9; hora <= 18; hora++) {
            cbHora.addItem(hora + ":00");
        }

        // Configurar calendario
        jDayChooser2.addPropertyChangeListener(evt -> {
            if ("day".equals(evt.getPropertyName())) {
                actualizarFecha();
            }
        });

        jButton4.setEnabled(true); // botón Confirmar
        cmbServicios.setEnabled(true); // combo de servicios

        // Mostrar servicios seleccionados (si los hubiera)
        if (!serviciosSeleccionados.isEmpty()) {
            indiceActual = 0;
            mostrarServiciosSeleccionados();
            timer = new Timer(3000, e -> siguienteServicio());
            timer.start();
        } else {
            jLabel4.setIcon(null);
            label11.setText("No hay servicios seleccionados");
            jLabel8.setText("$0");
        }

        // Mostrar la fecha seleccionada del calendario
// En el constructor, después de initComponents():
        jDayChooser2.addPropertyChangeListener(evt -> {
            if ("day".equals(evt.getPropertyName())) {
                int dia = jDayChooser2.getDay();
                int mes = jMonthChooser1.getMonth();
                int anio = java.time.Year.now().getValue();
                if (dia > 0) {
                    java.util.Calendar cal = java.util.Calendar.getInstance();
                    cal.set(anio, mes, dia);
                    java.util.Date fechaSeleccionada = cal.getTime();

                    java.text.SimpleDateFormat formato = new java.text.SimpleDateFormat("yyyy-MM-dd");
                    String fechaFormateada = formato.format(fechaSeleccionada);
                    jTextFieldFecha1.setText(fechaFormateada);

                    // Validar si la fecha está bloqueada
                    if (fechaBloqueada(fechaFormateada)) {
                        JOptionPane.showMessageDialog(this,
                                "Esta fecha está completamente bloqueada. No se pueden agendar citas.\n\n"
                                + "Motivo: " + obtenerMotivoBloqueo(fechaFormateada),
                                "Fecha no disponible",
                                JOptionPane.WARNING_MESSAGE);
                        cbHora.setEnabled(false);
                        jButton4.setEnabled(false); // Deshabilitar botón confirmar
                    } else {
                        cbHora.setEnabled(true);
                        jButton4.setEnabled(true); // Habilitar botón confirmar
                        // Verificar horas disponibles
                        verificarHorasDisponibles(fechaFormateada);
                    }
                }
            }
        });
        if (!serviciosSeleccionados.isEmpty()) {
            indiceActual = 0;
            mostrarServiciosSeleccionados();
            timer = new Timer(3000, e -> siguienteServicio());
            timer.start();
        } else {
            jLabel4.setIcon(null);
            label11.setText("No hay servicios seleccionados");
            jLabel8.setText("$0");
        }

    }

    private String obtenerMotivoBloqueo(String fecha) {
        String sql = "SELECT Motivo FROM bloqueo_horario WHERE Fecha = ? LIMIT 1";
        try (java.sql.Connection conn = conexion.getConexion(); java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, fecha);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("Motivo");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "No especificado";
    }

    private void verificarHorasDisponibles(String fecha) {
        // Deshabilitar horas específicas que estén bloqueadas
        for (int i = 0; i < cbHora.getItemCount(); i++) {
            String hora = cbHora.getItemAt(i);
            if (horaBloqueada(fecha, hora)) {
                // Puedes marcar visualmente las horas no disponibles
                System.out.println("Hora bloqueada: " + hora + " en fecha: " + fecha);
            }
        }
    }

    // En NewJAgenC, puedes agregar este método para debug
    private void verificarEstadoSesion() {
        System.out.println("=== ESTADO DE SESIÓN ===");
        System.out.println("ID Usuario: " + SesionUsuario.getIdUsuario());
        System.out.println("Nombre: " + SesionUsuario.getNombreUsuario());
        System.out.println("Sesión activa: " + SesionUsuario.sesionActiva());
        System.out.println("=== FIN ESTADO ===");
    }

// Llamar este método en el constructor después de initComponents()     
    /**
     * Constructor con parámetros (ventana desde catálogo)
     */
    public NewJAgenC(ImageIcon imagen, String descripcion, String precio) {
        this();
        serviciosSeleccionados.add(new Object[]{imagen, descripcion, precio});
        indiceActual = serviciosSeleccionados.size() - 1;
        mostrarServiciosSeleccionados();

    }

    private void actualizarFecha() {
        int dia = jDayChooser2.getDay();
        int mes = jMonthChooser1.getMonth();
        int anio = java.time.Year.now().getValue();
        if (dia > 0) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.set(anio, mes, dia);
            java.util.Date fechaSeleccionada = cal.getTime();
            java.text.SimpleDateFormat formato = new java.text.SimpleDateFormat("yyyy-MM-dd");
            String fechaFormateada = formato.format(fechaSeleccionada);
            jTextFieldFecha1.setText(fechaFormateada);

            if (fechaBloqueada(fechaFormateada)) {
                JOptionPane.showMessageDialog(this,
                        "Esta fecha está bloqueada, no se pueden agendar citas.",
                        "Fecha no disponible",
                        JOptionPane.WARNING_MESSAGE);
                cbHora.setEnabled(false);
            }
        }
    }

    private boolean fechaBloqueada(String fecha) {
        // Verificar en la tabla bloqueo_horario
        String sqlBloqueo = "SELECT COUNT(*) FROM bloqueo_horario WHERE Fecha = ?";

        try (java.sql.Connection conn = conexion.getConexion(); java.sql.PreparedStatement psBloqueo = conn.prepareStatement(sqlBloqueo)) {

            psBloqueo.setString(1, fecha);
            try (java.sql.ResultSet rsBloqueo = psBloqueo.executeQuery()) {
                if (rsBloqueo.next() && rsBloqueo.getInt(1) > 0) {
                    return true; // La fecha está completamente bloqueada
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean horaBloqueada(String fecha, String hora) {
        // Convertir la hora a Time para comparaciones
        java.sql.Time horaTime = java.sql.Time.valueOf(hora + ":00");

        // Verificar en bloqueo_horario (rangos de tiempo)
        String sqlBloqueo = "SELECT COUNT(*) FROM bloqueo_horario WHERE Fecha = ? "
                + "AND ? BETWEEN Hora_inicio AND Hora_fin";

        // Verificar en cita - MODIFICADO: verificar por servicio específico
        String sqlCita = """
        SELECT COUNT(*) FROM cita c 
        JOIN cita_has_servicios chs ON c.idCita = chs.idCita 
        WHERE c.Fecha = ? AND c.Hora = ? 
        AND c.Estado IN ('confirmada', 'reservada')
        AND chs.idServicios = ?
        """;

        try (java.sql.Connection conn = conexion.getConexion(); java.sql.PreparedStatement psBloqueo = conn.prepareStatement(sqlBloqueo); java.sql.PreparedStatement psCita = conn.prepareStatement(sqlCita)) {

            // Verificar en bloqueo_horario
            psBloqueo.setString(1, fecha);
            psBloqueo.setTime(2, horaTime);
            try (java.sql.ResultSet rsBloqueo = psBloqueo.executeQuery()) {
                if (rsBloqueo.next() && rsBloqueo.getInt(1) > 0) {
                    return true; // La hora está dentro de un rango bloqueado
                }
            }

            // Verificar en cita para CADA servicio seleccionado
            for (Object[] servicio : serviciosSeleccionados) {
                String descripcion = (String) servicio[1];
                int idServicio = obtenerIdServicioPorDescripcion(descripcion);

                if (idServicio > 0) {
                    psCita.setString(1, fecha);
                    psCita.setTime(2, horaTime);
                    psCita.setInt(3, idServicio);

                    try (java.sql.ResultSet rsCita = psCita.executeQuery()) {
                        if (rsCita.next() && rsCita.getInt(1) > 0) {
                            System.out.println("Hora ocupada para servicio: " + descripcion + " - ID: " + idServicio);
                            return true; // Este servicio ya está ocupado a esta hora
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean clienteTieneCitaMismaHora(String fecha, String hora) {
        int idUsuario = SesionUsuario.getIdUsuario();
        if (idUsuario == 0) {
            return false;
        }

        String sql = "SELECT COUNT(*) FROM cita WHERE Fecha = ? AND Hora = ? AND idUsuarios = ? AND Estado IN ('confirmada', 'reservada')";

        try (java.sql.Connection conn = conexion.getConexion(); java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, fecha);
            ps.setTime(2, java.sql.Time.valueOf(hora + ":00"));
            ps.setInt(3, idUsuario);

            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Muestra el servicio actual según el índice
     */
    private void mostrarServiciosSeleccionados() {
        if (serviciosSeleccionados.isEmpty()) {
            jLabel4.setIcon(null);
            label11.setText("No hay servicios seleccionados");
            jLabel8.setText("$0");
            return;
        }

        Object[] servicio = serviciosSeleccionados.get(indiceActual);
        ImageIcon imagen = (ImageIcon) servicio[0];
        String descripcion = (String) servicio[1];
        String precio = (String) servicio[2];

        jLabel4.setIcon(escalarImagen(imagen, jLabel4.getWidth(), jLabel4.getHeight()));
        label11.setText(descripcion);
        jLabel8.setText(precio);
    }

    /**
     * Método para escalar correctamente la imagen
     */
    private ImageIcon escalarImagen(ImageIcon icon, int ancho, int alto) {
        if (icon == null) {
            return null;
        }
        Image img = icon.getImage();
        Image nueva = img.getScaledInstance(ancho, alto, Image.SCALE_SMOOTH);
        return new ImageIcon(nueva);
    }

private int obtenerIdServicioPorDescripcion(String descripcion) {
    java.sql.Connection conn = null;
    java.sql.PreparedStatement ps = null;
    java.sql.ResultSet rs = null;
    
    try {
        conn = ConexionBD.getConnection();
        
        System.out.println("DEBUG - Buscando ID para: '" + descripcion + "'");
        
        // PRIMERO buscar en categoria_servicio para obtener el idServicios relacionado
        String sql = """
            SELECT cs.idServicios 
            FROM categoria_servicio cs 
            WHERE cs.Nombre_categoria LIKE ? OR cs.Descripcion LIKE ?
            LIMIT 1
            """;
        ps = conn.prepareStatement(sql);
        ps.setString(1, "%" + descripcion + "%");
        ps.setString(2, "%" + descripcion + "%");
        rs = ps.executeQuery();

        if (rs.next()) {
            int idServicio = rs.getInt("idServicios");
            System.out.println("DEBUG - Encontrado en categoria_servicio: '" + descripcion + "' -> idServicios: " + idServicio);
            return idServicio;
        }

        // SI no encuentra, buscar directamente en servicios
        rs.close();
        ps.close();
        
        sql = "SELECT idServicios FROM servicios WHERE Nombre_servicio LIKE ? OR Descripcion LIKE ?";
        ps = conn.prepareStatement(sql);
        ps.setString(1, "%" + descripcion + "%");
        ps.setString(2, "%" + descripcion + "%");
        rs = ps.executeQuery();

        if (rs.next()) {
            int idServicio = rs.getInt("idServicios");
            System.out.println("DEBUG - Encontrado en servicios: '" + descripcion + "' -> " + idServicio);
            return idServicio;
        }

    } catch (Exception e) {
        System.out.println("ERROR en obtenerIdServicioPorDescripcion: " + e.getMessage());
        e.printStackTrace();
    } finally {
        try {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
            if (conn != null) conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    // Fallback: mapeo manual
    return obtenerIdServicioFallback(descripcion);
}

private int obtenerIdServicioFallback(String descripcion) {
    // Mapeo manual basado en tu estructura de base de datos
    java.util.Map<String, Integer> mapeoFallback = new java.util.HashMap<>();
    
    // Basado en tu estructura:
    // Servicio "Uñas" -> idServicios = 1
    // Servicio "Maquillaje" -> idServicios = 2  
    // Servicio "Peinado" -> idServicios = 3
    
    if (descripcion.toLowerCase().contains("peinado")) {
        System.out.println("DEBUG - Fallback: '" + descripcion + "' -> 3 (Peinado)");
        return 3;
    }
    if (descripcion.toLowerCase().contains("maquillaje")) {
        System.out.println("DEBUG - Fallback: '" + descripcion + "' -> 2 (Maquillaje)");
        return 2;
    }
    if (descripcion.toLowerCase().contains("uña") || descripcion.toLowerCase().contains("ballerina") || 
        descripcion.toLowerCase().contains("cuadrada") || descripcion.toLowerCase().contains("francesa")) {
        System.out.println("DEBUG - Fallback: '" + descripcion + "' -> 1 (Uñas)");
        return 1;
    }
    
    System.out.println("DEBUG - Fallback por defecto: '" + descripcion + "' -> 3");
    return 3; // Por defecto peinado
}

    private int buscarCategoriaAlternativa(String descripcion) {
        int id = 0;

        // Mapeo para casos donde el nombre no coincide exactamente
        java.util.Map<String, String> mapeoCategorias = new java.util.HashMap<>();
        mapeoCategorias.put("Ballerina", "Ballerina");
        mapeoCategorias.put("Cuadradas", "Cuadradas");
        mapeoCategorias.put("Francesa", "Francesa");
        mapeoCategorias.put("Maquillaje", "Maquillaje Social"); // Ejemplo
        mapeoCategorias.put("Peinados", "Peinado Social"); // Ejemplo

        String nombreCategoria = mapeoCategorias.get(descripcion);
        if (nombreCategoria == null) {
            nombreCategoria = descripcion;
        }

        String sql = "SELECT idServicios FROM categoria_servicio WHERE Nombre_categoria LIKE ? LIMIT 1";

        try (java.sql.Connection con = conexion.getConexion(); java.sql.PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, "%" + nombreCategoria + "%");
            java.sql.ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                id = rs.getInt("idServicios");
                System.out.println("DEBUG - ID encontrado (búsqueda alternativa) para '" + descripcion + "': " + id);
            } else {
                System.out.println("DEBUG - Categoría no encontrada incluso con búsqueda alternativa: " + descripcion);
                // Si no se encuentra, usar valores por defecto según el tipo
                if (descripcion.toLowerCase().contains("uña") || descripcion.equals("Ballerina")
                        || descripcion.equals("Cuadradas") || descripcion.equals("Francesa")) {
                    id = 1; // Uñas
                } else if (descripcion.toLowerCase().contains("maquillaje")) {
                    id = 2; // Maquillaje
                } else if (descripcion.toLowerCase().contains("peinado")) {
                    id = 3; // Peinado
                }
                System.out.println("DEBUG - Usando ID por defecto para '" + descripcion + "': " + id);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return id;
    }

    private int buscarServicioAlternativo(String descripcion) {
        int id = 0;

        // Mapeo entre las descripciones que usas y los nombres en la BD
        java.util.Map<String, String> mapeoServicios = new java.util.HashMap<>();
        mapeoServicios.put("Ballerina", "Uñas Ballerina");
        mapeoServicios.put("Cuadradas", "Uñas Cuadradas");
        mapeoServicios.put("Francesa", "Uñas Francesa");
        mapeoServicios.put("Maquillaje", "Maquillaje Social");
        mapeoServicios.put("Peinados", "Peinado Elegante");

        String nombreReal = mapeoServicios.get(descripcion);
        if (nombreReal == null) {
            nombreReal = descripcion; // Si no hay mapeo, usa la original
        }

        // ✅ CORRECCIÓN: Usar el nombre correcto de la columna
        String sql = "SELECT idServicios FROM servicios WHERE Nombre_servicio LIKE ?";

        try (java.sql.Connection con = conexion.getConexion(); java.sql.PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, "%" + nombreReal + "%");
            java.sql.ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                id = rs.getInt("idServicios");
                System.out.println("DEBUG - ID encontrado (búsqueda alternativa) para '" + descripcion + "': " + id);
            } else {
                System.out.println("DEBUG - Servicio no encontrado incluso con búsqueda alternativa: " + descripcion);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return id;
    }

    private void debugCategoriasServicios() {
        System.out.println("=== CATEGORÍAS DE SERVICIOS DISPONIBLES ===");
        String sql = "SELECT cs.idCategoria_Servicio, cs.Nombre_categoria, s.Nombre_servicio, cs.Precio "
                + "FROM categoria_servicio cs "
                + "JOIN servicios s ON cs.idServicios = s.idServicios";

        try (java.sql.Connection con = conexion.getConexion(); java.sql.PreparedStatement ps = con.prepareStatement(sql); java.sql.ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int idCat = rs.getInt("idCategoria_Servicio");
                String nombreCat = rs.getString("Nombre_categoria");
                String nombreServ = rs.getString("Nombre_servicio");
                double precio = rs.getDouble("Precio");
                System.out.println("Categoría ID: " + idCat + " | '" + nombreCat + "' | Servicio: " + nombreServ + " | Precio: $" + precio);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("===========================================");
    }

private void insertarCitaYServicios(int idPago) {
    String fecha = SesionUsuario.getFechaCita();
    String hora = SesionUsuario.getHoraCita();
    java.util.List<Object[]> servicios = SesionUsuario.getServiciosCita();

    if (fecha == null || hora == null || servicios.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Error: No hay datos de cita guardados.");
        return;
    }

    java.sql.Connection conn = null;
    java.sql.PreparedStatement psCita = null;
    java.sql.PreparedStatement psServicios = null;
    java.sql.ResultSet rsCita = null;
    
    try {
        conn = ConexionBD.getConnection();
        conn.setAutoCommit(false);
        
        // 1. INSERTAR CITA
        String sqlCita = "INSERT INTO cita (Fecha, Hora, Estado, idUsuarios, Pago_idPago) VALUES (?, ?, ?, ?, ?)";
        psCita = conn.prepareStatement(sqlCita, java.sql.Statement.RETURN_GENERATED_KEYS);
        
        psCita.setDate(1, java.sql.Date.valueOf(fecha));
        psCita.setTime(2, java.sql.Time.valueOf(hora + ":00"));
        psCita.setString(3, "Confirmada");
        psCita.setInt(4, SesionUsuario.getIdUsuario());
        psCita.setInt(5, idPago);
        
        psCita.executeUpdate();
        
        // OBTENER ID DE LA CITA
        rsCita = psCita.getGeneratedKeys();
        int idCita = 0;
        if (rsCita.next()) {
            idCita = rsCita.getInt(1);
            System.out.println("DEBUG - Cita creada con ID: " + idCita);
        } else {
            throw new SQLException("No se pudo obtener el ID de la cita creada");
        }
        
        // 2. INSERTAR SERVICIOS DE LA CITA - CORREGIDO PARA EVITAR DUPLICADOS
        String sqlServicios = "INSERT INTO cita_has_servicios (idCita, idServicios, Pago_idPago, Monto_anticipo) VALUES (?, ?, ?, ?)";
        psServicios = conn.prepareStatement(sqlServicios);
        
        // Usar un Set para evitar servicios duplicados por ID
        java.util.Set<Integer> serviciosInsertados = new java.util.HashSet<>();
        int serviciosAgregados = 0;
        
        System.out.println("=== PROCESANDO SERVICIOS PARA CITA " + idCita + " ===");
        
        for (Object[] servicio : servicios) {
            String descripcion = (String) servicio[1];
            int idServicio = obtenerIdServicioPorDescripcion(descripcion);
            
            System.out.println("Procesando servicio: '" + descripcion + "' -> ID: " + idServicio);
            
            if (idServicio > 0) {
                // Verificar que no sea un duplicado
                if (!serviciosInsertados.contains(idServicio)) {
                    serviciosInsertados.add(idServicio);
                    
                    // Calcular precio individual para este servicio
                    double precioIndividual = 0.0;
                    if (servicio.length > 2 && servicio[2] != null) {
                        String precioStr = servicio[2].toString().replace("$", "").trim();
                        try {
                            precioIndividual = Double.parseDouble(precioStr);
                        } catch (NumberFormatException e) {
                            System.out.println("Error parseando precio: " + precioStr);
                            precioIndividual = SesionUsuario.getMontoTotalCita() / servicios.size();
                        }
                    } else {
                        precioIndividual = SesionUsuario.getMontoTotalCita() / servicios.size();
                    }
                    
                    // Insertar servicio
                    psServicios.setInt(1, idCita);
                    psServicios.setInt(2, idServicio);
                    psServicios.setInt(3, idPago);
                    psServicios.setBigDecimal(4, java.math.BigDecimal.valueOf(precioIndividual));
                    psServicios.addBatch();
                    
                    serviciosAgregados++;
                    System.out.println("DEBUG - Preparado para insertar: cita=" + idCita + 
                                      ", servicio=" + idServicio + ", precio=" + precioIndividual);
                } else {
                    System.out.println("DEBUG - Servicio duplicado omitido: " + descripcion + " (ID: " + idServicio + ")");
                }
            } else {
                System.out.println("ERROR: No se encontró ID válido para servicio: " + descripcion);
            }
        }
        
        // Ejecutar batch solo si hay servicios válidos
        if (serviciosAgregados > 0) {
            int[] resultados = psServicios.executeBatch();
            System.out.println("DEBUG - Servicios insertados en cita_has_servicios: " + resultados.length);
            
            conn.commit();
            JOptionPane.showMessageDialog(this, "¡Cita agendada exitosamente!\nID de Cita: " + idCita + 
                    "\nServicios agregados: " + serviciosAgregados);
        } else {
            System.out.println("ERROR: No hay servicios válidos para insertar");
            throw new SQLException("No se pudieron insertar servicios para la cita");
        }
        
        // Limpiar datos de la sesión
        SesionUsuario.limpiarDatosCita();
        
    } catch (java.sql.SQLException e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, "Error al agendar cita: " + e.getMessage());
        
        try {
            if (conn != null) conn.rollback();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    } finally {
        // Cerrar todos los recursos
        try {
            if (rsCita != null) rsCita.close();
            if (psCita != null) psCita.close();
            if (psServicios != null) psServicios.close();
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}


    private java.util.List<String> obtenerServiciosOcupados(String fecha, String hora) {
        java.util.List<String> serviciosOcupados = new java.util.ArrayList<>();

        String sql = """
        SELECT DISTINCT s.Nombre_servicio 
        FROM cita c 
        JOIN cita_has_servicios chs ON c.idCita = chs.idCita 
        JOIN servicios s ON chs.idServicios = s.idServicios 
        WHERE c.Fecha = ? AND c.Hora = ? AND c.Estado IN ('confirmada', 'reservada')
        """;

        try (java.sql.Connection conn = conexion.getConexion(); java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, fecha);
            ps.setTime(2, java.sql.Time.valueOf(hora + ":00"));

            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    serviciosOcupados.add(rs.getString("Nombre_servicio"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return serviciosOcupados;
    }

    private double calcularMontoTotal(java.util.List<Object[]> servicios) {
        double total = 0.0;
        for (Object[] servicio : servicios) {
            String precioStr = (String) servicio[2]; // El precio está en la posición 2
            // Extraer el número del string (ej: "$500" -> 500)
            precioStr = precioStr.replace("$", "").trim();
            try {
                total += Double.parseDouble(precioStr);
            } catch (NumberFormatException e) {
                System.out.println("Error parseando precio: " + precioStr);
            }
        }
        return total;
    }

    public void finalizarAgendado(int idPago) {
        // Este método se llama después de que el usuario completa el pago
        insertarCitaYServicios(idPago);
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
        jMenuBar2 = new javax.swing.JMenuBar();
        jMenu5 = new javax.swing.JMenu();
        jMenu7 = new javax.swing.JMenu();
        jMenuBar3 = new javax.swing.JMenuBar();
        jMenu8 = new javax.swing.JMenu();
        jMenu9 = new javax.swing.JMenu();
        jMenuBar4 = new javax.swing.JMenuBar();
        jMenu10 = new javax.swing.JMenu();
        jMenu11 = new javax.swing.JMenu();
        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        panel6 = new java.awt.Panel();
        label5 = new java.awt.Label();
        label7 = new java.awt.Label();
        label11 = new java.awt.Label();
        jLabel4 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jButton4 = new javax.swing.JButton();
        cmbServicios = new javax.swing.JComboBox<>();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jTextFieldFecha1 = new javax.swing.JTextField();
        cbHora = new javax.swing.JComboBox<>();
        jDayChooser2 = new com.toedter.calendar.JDayChooser();
        jMonthChooser1 = new com.toedter.calendar.JMonthChooser();
        jPanel5 = new javax.swing.JPanel();
        INS = new javax.swing.JLabel();
        FACE = new javax.swing.JLabel();
        WPP = new javax.swing.JLabel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenu2 = new javax.swing.JMenu();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();
        jMenu6 = new javax.swing.JMenu();
        jMenuItem5 = new javax.swing.JMenuItem();
        jMenuItem4 = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        jMenuItem6 = new javax.swing.JMenuItem();
        jMenu4 = new javax.swing.JMenu();
        jMenuItem7 = new javax.swing.JMenuItem();

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
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

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 652, Short.MAX_VALUE)
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 436, Short.MAX_VALUE)
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setBackground(new java.awt.Color(243, 224, 255));

        jPanel3.setBackground(new java.awt.Color(243, 224, 255));

        jLabel11.setFont(new java.awt.Font("Serif", 3, 18)); // NOI18N
        jLabel11.setText("Servicio");

        panel6.setBackground(new java.awt.Color(242, 242, 242));

        label5.setText("label5");

        label7.setBackground(new java.awt.Color(252, 237, 237));
        label7.setText("Precio");

        label11.setText("Descripció");

        jLabel4.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        jLabel8.setText("$500");
        jLabel8.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        javax.swing.GroupLayout panel6Layout = new javax.swing.GroupLayout(panel6);
        panel6.setLayout(panel6Layout);
        panel6Layout.setHorizontalGroup(
            panel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panel6Layout.createSequentialGroup()
                .addGroup(panel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panel6Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(panel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(label7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(label11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(panel6Layout.createSequentialGroup()
                        .addGap(33, 33, 33)
                        .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 208, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(35, Short.MAX_VALUE))
            .addGroup(panel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(panel6Layout.createSequentialGroup()
                    .addContainerGap(172, Short.MAX_VALUE)
                    .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(34, 34, 34)))
        );
        panel6Layout.setVerticalGroup(
            panel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 214, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(label11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(24, 24, 24)
                .addComponent(label7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addGroup(panel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panel6Layout.createSequentialGroup()
                    .addContainerGap(274, Short.MAX_VALUE)
                    .addComponent(jLabel8)
                    .addGap(12, 12, 12)))
        );

        jButton4.setBackground(new java.awt.Color(255, 204, 255));
        jButton4.setText("Confirmar");
        jButton4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton4ActionPerformed(evt);
            }
        });

        cmbServicios.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Uñas", "Maquillaje", "Peinados" }));
        cmbServicios.setToolTipText("Otros Servicios");
        cmbServicios.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbServiciosActionPerformed(evt);
            }
        });

        jLabel1.setText("Fecha");

        jLabel2.setText("Hora");

        jTextFieldFecha1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldFecha1ActionPerformed(evt);
            }
        });

        cbHora.setFont(new java.awt.Font("Dialog", 0, 12)); // NOI18N
        cbHora.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cbHoraActionPerformed(evt);
            }
        });

        jPanel5.setBackground(new java.awt.Color(204, 0, 204));

        INS.setText("INS");

        FACE.setText("FACE");

        WPP.setText("WPP");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(115, 115, 115)
                .addComponent(INS, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(109, 109, 109)
                .addComponent(WPP, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(FACE, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(79, 79, 79))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(INS)
                    .addComponent(WPP)
                    .addComponent(FACE))
                .addContainerGap(14, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(71, 71, 71)
                .addComponent(panel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(22, 22, 22)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jDayChooser2, javax.swing.GroupLayout.PREFERRED_SIZE, 314, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jMonthChooser1, javax.swing.GroupLayout.PREFERRED_SIZE, 103, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(26, 26, 26)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(32, 32, 32)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(cmbServicios, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButton4))
                        .addGap(41, 41, 41))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(cbHora, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(27, 27, 27))
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jTextFieldFecha1, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))))
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(160, 160, 160)
                .addComponent(jLabel11)
                .addGap(0, 0, Short.MAX_VALUE))
            .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addGap(95, 95, 95)
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel1)
                                    .addComponent(jTextFieldFecha1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jMonthChooser1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(13, 13, 13)))
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addGap(36, 36, 36)
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(jLabel2)
                                    .addComponent(cbHora, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(35, 35, 35)
                                .addComponent(cmbServicios, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jDayChooser2, javax.swing.GroupLayout.PREFERRED_SIZE, 157, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 61, Short.MAX_VALUE)
                        .addComponent(jButton4)
                        .addGap(58, 58, 58))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(8, 8, 8)
                        .addComponent(jLabel11)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(panel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jMenu1.setText("INICIO");
        jMenu1.addMenuListener(new javax.swing.event.MenuListener() {
            public void menuCanceled(javax.swing.event.MenuEvent evt) {
            }
            public void menuDeselected(javax.swing.event.MenuEvent evt) {
            }
            public void menuSelected(javax.swing.event.MenuEvent evt) {
                jMenu1MenuSelected(evt);
            }
        });
        jMenu1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenu1ActionPerformed(evt);
            }
        });
        jMenuBar1.add(jMenu1);

        jMenu2.setText("CATALÓGO");

        jMenuItem2.setText("UÑAS");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItem2);

        jMenuItem1.setText("PEINADO");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItem1);

        jMenuItem3.setText("MAQUILLAJES");
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem3ActionPerformed(evt);
            }
        });
        jMenu2.add(jMenuItem3);

        jMenuBar1.add(jMenu2);

        jMenu6.setText("AGENDAR CITA");

        jMenuItem5.setText("Agendar cita");
        jMenuItem5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem5ActionPerformed(evt);
            }
        });
        jMenu6.add(jMenuItem5);

        jMenuItem4.setText("Cancelar Cita");
        jMenuItem4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem4ActionPerformed(evt);
            }
        });
        jMenu6.add(jMenuItem4);

        jMenuBar1.add(jMenu6);

        jMenu3.setText("CONTACTO");

        jMenuItem6.setText("Contacto");
        jMenuItem6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem6ActionPerformed(evt);
            }
        });
        jMenu3.add(jMenuItem6);

        jMenuBar1.add(jMenu3);

        jMenu4.setText("LOGIN");

        jMenuItem7.setText("Login");
        jMenuItem7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem7ActionPerformed(evt);
            }
        });
        jMenu4.add(jMenuItem7);

        jMenuBar1.add(jMenu4);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
 private void siguienteServicio() {
        if (serviciosSeleccionados.isEmpty()) {
            return;
        }
        indiceActual = (indiceActual + 1) % serviciosSeleccionados.size();
        mostrarServiciosSeleccionados();
    }
    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        // TODO add your handling code here:
        //para arir uñas
        NewJCatalogoUñas NewJCatalogoUñas = new NewJCatalogoUñas();
        NewJCatalogoUñas.setVisible(true);
        this.dispose(); // cierra la actual

    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void jMenu1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenu1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jMenu1ActionPerformed

    private void jMenu1MenuSelected(javax.swing.event.MenuEvent evt) {//GEN-FIRST:event_jMenu1MenuSelected
        // TODO add your handling code here:
        //inicio
        Inicio Inicio = new Inicio();
        Inicio.setVisible(true);
        this.dispose(); // cierra la actual

    }//GEN-LAST:event_jMenu1MenuSelected

    private void jMenuItem5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem5ActionPerformed
        // TODO add your handling code here:
        //agendar cita
        NewJAgenC NewJAgenC = new NewJAgenC(); // <-- pasar idUsuario actual
        NewJAgenC.setVisible(true);
        this.dispose();

    }//GEN-LAST:event_jMenuItem5ActionPerformed

    private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem4ActionPerformed
        // TODO add your handling code here:
        //agendar cancelar
        NewJCancelarC NewJCancelarC = new NewJCancelarC();
        NewJCancelarC.setVisible(true);
        this.dispose(); // cierra la actual


    }//GEN-LAST:event_jMenuItem4ActionPerformed

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

    private void jMenuItem6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem6ActionPerformed
        // TODO add your handling code here:
        //boton de contacto
        NewJContacto NewJContacto = new NewJContacto();
        NewJContacto.setVisible(true);
        this.dispose(); // cierra la actual


    }//GEN-LAST:event_jMenuItem6ActionPerformed

    private void jMenuItem7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem7ActionPerformed
        // TODO add your handling code here:
        //login
        NewJLogin NewJLogin = new NewJLogin();
        NewJLogin.setVisible(true);
        this.dispose(); // cierra la actual


    }//GEN-LAST:event_jMenuItem7ActionPerformed

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        // Debug de servicios seleccionados
        System.out.println("=== DEBUG SERVICIOS SELECCIONADOS ===");
        for (int i = 0; i < serviciosSeleccionados.size(); i++) {
            Object[] servicio = serviciosSeleccionados.get(i);
            String descripcion = (String) servicio[1];
            int idServicio = obtenerIdServicioPorDescripcion(descripcion);
            System.out.println("Servicio " + i + ": '" + descripcion + "' -> ID Servicio: " + idServicio);
        }
        System.out.println("=====================================");

        // Validaciones básicas
        if (!SesionUsuario.sesionActiva()) {
            JOptionPane.showMessageDialog(this,
                    "Debes iniciar sesión o registrarte antes de agendar una cita.",
                    "Acceso denegado", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String fecha = jTextFieldFecha1.getText().trim();
        String hora = (String) cbHora.getSelectedItem();

        // Validación final de bloqueos
        if (fechaBloqueada(fecha)) {
            JOptionPane.showMessageDialog(this,
                    "ERROR: Esta fecha está completamente bloqueada.\n\n"
                    + "Motivo: " + obtenerMotivoBloqueo(fecha) + "\n\n"
                    + "Por favor seleccione otra fecha para su cita.",
                    "Fecha Bloqueada",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (hora == null || hora.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debes seleccionar una hora para la cita.");
            return;
        }

        // Validar que la cliente no tenga ya una cita a la misma hora
        if (clienteTieneCitaMismaHora(fecha, hora)) {
            JOptionPane.showMessageDialog(this,
                    "ERROR: Ya tienes una cita agendada para esta fecha y hora.\n\n"
                    + "Puedes agendar múltiples servicios el mismo día, pero deben ser en horas diferentes.",
                    "Hora Ocupada",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Validar que los servicios no estén ocupados por otras clientas
        if (horaBloqueada(fecha, hora)) {
            // Obtener servicios ocupados para mostrar detalles
            java.util.List<String> serviciosOcupados = obtenerServiciosOcupados(fecha, hora);
            String mensajeError;

            if (!serviciosOcupados.isEmpty()) {
                mensajeError = "ERROR: Los siguientes servicios ya están ocupados en esta hora:\n\n";
                for (String servicio : serviciosOcupados) {
                    mensajeError += "• " + servicio + "\n";
                }
                mensajeError += "\nPor favor seleccione otra hora.";
            } else {
                mensajeError = "ERROR: Esta hora no está disponible.\n\n"
                        + "La hora seleccionada (" + hora + ") está bloqueada o ya fue reservada.\n"
                        + "Por favor seleccione otra hora.";
            }

            JOptionPane.showMessageDialog(this,
                    mensajeError,
                    "Hora No Disponible",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Si pasa todas las validaciones, proceder con el agendamiento
        // Calcular monto total y GUARDAR DATOS EN SESION
        double montoTotal = calcularMontoTotal(serviciosSeleccionados);
        SesionUsuario.setMontoTotalCita(montoTotal);
        SesionUsuario.setFechaCita(fecha);
        SesionUsuario.setHoraCita(hora);
        SesionUsuario.setServiciosCita(serviciosSeleccionados);

        System.out.println("=== DATOS GUARDADOS PARA CITA ===");
        System.out.println("Monto total: $" + montoTotal);
        System.out.println("Fecha: " + SesionUsuario.getFechaCita());
        System.out.println("Hora: " + SesionUsuario.getHoraCita());
        System.out.println("Servicios: " + serviciosSeleccionados.size());

        // Abrir ventana de confirmación de cita
        NewJCitaConf confirmacionWindow = new NewJCitaConf();
        confirmacionWindow.setVisible(true);

        // Cerrar esta ventana
        this.dispose();

    }//GEN-LAST:event_jButton4ActionPerformed

    private void cmbServiciosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbServiciosActionPerformed
        // TODO add your handling code here:
        String servicioSeleccionado = cmbServicios.getSelectedItem().toString();

        switch (servicioSeleccionado) {
            case "Uñas":
                new NewJCatalogoUñas().setVisible(true);
                break;
            case "Maquillaje":
                new NewJCatalogoMaq().setVisible(true);
                break;
            case "Peinados":
                new NewJCatalogoPeinado().setVisible(true);
                break;
        }

        // Cierra la ventana actual si lo deseas:
        this.dispose();
    }//GEN-LAST:event_cmbServiciosActionPerformed

    private void cbHoraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbHoraActionPerformed
        // TODO add your handling code here:
        String fecha = jTextFieldFecha1.getText().trim();
        String hora = (String) cbHora.getSelectedItem();

        if (!fecha.isEmpty() && hora != null) {
            // Verificar disponibilidad general
            if (horaBloqueada(fecha, hora)) {
                JOptionPane.showMessageDialog(this,
                        "Esta hora no está disponible para uno o más servicios seleccionados.\n"
                        + "Por favor seleccione otra hora.",
                        "Hora no disponible",
                        JOptionPane.WARNING_MESSAGE);
            }

            // Verificar si la cliente ya tiene cita a esta hora
            if (clienteTieneCitaMismaHora(fecha, hora)) {
                JOptionPane.showMessageDialog(this,
                        "Ya tienes una cita agendada para esta hora.\n"
                        + "Puedes agendar servicios adicionales en horas diferentes.",
                        "Hora Ocupada",
                        JOptionPane.WARNING_MESSAGE);
            }
        }
    }//GEN-LAST:event_cbHoraActionPerformed

    private void jTextFieldFecha1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldFecha1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextFieldFecha1ActionPerformed

    /**
     * *
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
            java.util.logging.Logger.getLogger(NewJAgenC.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(NewJAgenC.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(NewJAgenC.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(NewJAgenC.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new NewJAgenC().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel FACE;
    private javax.swing.JLabel INS;
    private javax.swing.JLabel WPP;
    private javax.swing.JComboBox<String> cbHora;
    private javax.swing.JComboBox<String> cmbServicios;
    private javax.swing.JButton jButton4;
    private com.toedter.calendar.JDayChooser jDayChooser2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu10;
    private javax.swing.JMenu jMenu11;
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
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JMenuItem jMenuItem4;
    private javax.swing.JMenuItem jMenuItem5;
    private javax.swing.JMenuItem jMenuItem6;
    private javax.swing.JMenuItem jMenuItem7;
    private com.toedter.calendar.JMonthChooser jMonthChooser1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JTextField jTextFieldFecha1;
    private java.awt.Label label11;
    private java.awt.Label label5;
    private java.awt.Label label7;
    private java.awt.Panel panel6;
    // End of variables declaration//GEN-END:variables
}
