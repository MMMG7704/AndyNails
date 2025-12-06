package Interfaces;

import Interfaces.NewJCitaConf;
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
    private java.util.List<Object[]> serviciosSeleccionados = new java.util.ArrayList<>();//nueva
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
        initComponents();
        jTextFieldFecha1.setEditable(false);
        jTextFieldFecha1.setFocusable(false);

        btnRegresar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRegresarActionPerformed(evt);
            }
        });

        System.out.println("=== DEBUG - ESTADO INICIAL ===");
        System.out.println("HashCode de serviciosSeleccionados: " + serviciosSeleccionados.hashCode());
        System.out.println("Número de servicios al iniciar: " + serviciosSeleccionados.size());
        debugServiciosActuales();
        RedesSociales.configurarRedesSociales(INS, WPP, FACE);

        conexion = new ConexionBD("andynails");
        cargarServiciosDesdeBD();
        restaurarServiciosDesdeSesion();
        eliminarDuplicados();

        this.idUsuario = SesionUsuario.getIdUsuario();
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

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

        configurarCalendario();
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

    public NewJAgenC() {
        initComponents();
        jTextFieldFecha1.setEditable(false);
        jTextFieldFecha1.setFocusable(false);

        btnRegresar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRegresarActionPerformed(evt);
            }
        });
        conexion = new ConexionBD("andynails");

        this.idUsuario = SesionUsuario.getIdUsuario();

        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        //this.idUsuario = 0; // nadie logueado

        cargarServiciosDesdeBD();
        restaurarServiciosDesdeSesion();
        eliminarDuplicados();

        System.out.println("DEBUG - Constructor sin parámetros - ID Usuario: " + idUsuario);
        debugCategoriasServicios();

        //serviciosSeleccionados.clear();
        debugServiciosActuales();
        System.out.println("DEBUG - Servicios limpiados al iniciar NewJAgenC");

        // Llenar combo de horas
        cbHora.removeAllItems();
        for (int hora = 9; hora <= 18; hora++) {
            cbHora.addItem(hora + ":00");
        }

        jButton4.setEnabled(true); // botón Confirmar
        cmbServicios.setEnabled(true); // combo de servicios
        configurarCalendario();

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

    private void debugServiciosActuales() {
        System.out.println("=== SERVICIOS ACTUALES EN NewJAgenC ===");
        System.out.println("Número de servicios: " + serviciosSeleccionados.size());
        for (int i = 0; i < serviciosSeleccionados.size(); i++) {
            Object[] servicio = serviciosSeleccionados.get(i);
            System.out.println("Servicio " + i + ": " + servicio[1] + " - " + servicio[2]);
            System.out.println("  Objeto completo: " + java.util.Arrays.toString(servicio));
        }
        System.out.println("======================================");
    }

// Llamar este método en el constructor después de initComponents()     
    /**
     * Constructor con parámetros (ventana desde catálogo)
     */
    public NewJAgenC(ImageIcon imagen, String descripcion, String precio) {
        this(); // Llama al constructor sin parámetros

        System.out.println("=== CONSTRUCTOR DESDE CATÁLOGO ===");
        System.out.println("Recibiendo servicio: " + descripcion + " - " + precio);

        System.out.println("Servicios ANTES de agregar:");
        debugServiciosActuales();

        boolean servicioExiste = false;
        for (Object[] servicio : serviciosSeleccionados) {
            String descExistente = (String) servicio[1];
            if (descExistente.equals(descripcion)) {
                servicioExiste = true;
                break;
            }
        }

        if (!servicioExiste) {
            this.serviciosSeleccionados.add(new Object[]{imagen, descripcion, precio});
            System.out.println("Servicio agregado: " + descripcion);

            if (!serviciosSeleccionados.isEmpty()) {
                this.indiceActual = this.serviciosSeleccionados.size() - 1;
                mostrarServiciosSeleccionados();
                iniciarCarrusel();
            }

            SesionUsuario.setServiciosCita(serviciosSeleccionados);
        } else {
            System.out.println("Servicio ya existe: " + descripcion);
            JOptionPane.showMessageDialog(this,
                    "El servicio '" + descripcion + "' ya está en tu lista.",
                    "Servicio Duplicado",
                    JOptionPane.INFORMATION_MESSAGE);
        }

        System.out.println("Servicios DESPUÉS de agregar:");
        debugServiciosActuales();
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

    // Llamar este método cuando cambie el combo de categoría
    private void cargarServiciosPorCategoria(String categoria) {
        serviciosSeleccionados.clear();
        String sql = """
        SELECT s.Imagen, s.Descripcion, s.Precio
        FROM servicios s
        JOIN categoria_servicios cs ON s.idCategoria = cs.idCategoria
        WHERE cs.NombreCategoria = ?
    """;

        try (java.sql.Connection conn = conexion.getConexion(); java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, categoria);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    byte[] imagenBytes = rs.getBytes("Imagen");
                    ImageIcon icono = null;
                    if (imagenBytes != null) {
                        Image img = new ImageIcon(imagenBytes).getImage()
                                .getScaledInstance(jLabel4.getWidth(), jLabel4.getHeight(), Image.SCALE_SMOOTH);
                        icono = new ImageIcon(img);
                    }

                    String descripcion = rs.getString("Descripcion");
                    String precio = rs.getString("Precio");

                    serviciosSeleccionados.add(new Object[]{icono, descripcion, precio});
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al cargar servicios: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }

        if (!serviciosSeleccionados.isEmpty()) {
            indiceActual = 0;
            mostrarServiciosSeleccionados();
            iniciarCarrusel();
        } else {
            jLabel4.setIcon(null);
            label11.setText("No hay servicios disponibles");
            jLabel8.setText("$0");
            if (timer != null) {
                timer.stop();
            }
        }
    }

    private void iniciarCarrusel() {
        // Detener el timer anterior si existe
        if (timer != null) {
            timer.stop();
            timer = null;
        }

        // Solo iniciar carrusel si hay más de un servicio
        if (serviciosSeleccionados.size() > 1) {
            timer = new Timer(5000, e -> siguienteServicio()); // 5 segundos en lugar de 3
            timer.start();
            System.out.println("Carrusel iniciado - Cambio cada 5 segundos");
        } else {
            // Si solo hay un servicio, asegurarse de que se muestre
            mostrarServiciosSeleccionados();
        }
    }

    private void siguienteServicio() {
        if (!serviciosSeleccionados.isEmpty() && serviciosSeleccionados.size() > 1) {
            indiceActual = (indiceActual + 1) % serviciosSeleccionados.size();
            mostrarServiciosSeleccionados();
            System.out.println("Cambiando a servicio: " + indiceActual);
        }
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
        label11.setText(descripcion + " (" + (indiceActual + 1) + "/" + serviciosSeleccionados.size() + ")");
        jLabel8.setText(precio);

        // Mostrar el total
        double total = calcularMontoTotal(serviciosSeleccionados);
        System.out.println("Total actual para " + serviciosSeleccionados.size() + " servicios: $" + total);
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

        // Fallback: mapeo manual
        return obtenerIdServicioFallback(descripcion);
    }

    private int obtenerIdServicioFallback(String descripcion) {
        // Mapeo manual basado en tu estructura de base de datos
        java.util.Map<String, Integer> mapeoFallback = new java.util.HashMap<>();

      
        if (descripcion.toLowerCase().contains("peinado")) {
            System.out.println("DEBUG - Fallback: '" + descripcion + "' -> 3 (Peinado)");
            return 3;
        }
        if (descripcion.toLowerCase().contains("maquillaje")) {
            System.out.println("DEBUG - Fallback: '" + descripcion + "' -> 2 (Maquillaje)");
            return 2;
        }
        if (descripcion.toLowerCase().contains("uña") || descripcion.toLowerCase().contains("ballerina")
                || descripcion.toLowerCase().contains("cuadrada") || descripcion.toLowerCase().contains("francesa")) {
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

        if (fecha == null || fecha.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Error: No se ha seleccionado fecha para la cita.\n\n"
                    + "Por favor regrese a 'Agendar Cita' y seleccione una fecha válida.",
                    "Fecha requerida",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (hora == null || hora.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Error: No se ha seleccionado hora para la cita.\n\n"
                    + "Por favor regrese a 'Agendar Cita' y seleccione una hora.",
                    "Hora requerida",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (servicios == null || servicios.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Error: No hay servicios seleccionados.\n\n"
                    + "Por favor regrese a 'Agendar Cita' y seleccione al menos un servicio.",
                    "Servicios requeridos",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // DEBUG: Mostrar lo que se está procesando
        System.out.println("\n=== DEBUG INSERTAR CITA ===");
        System.out.println("Fecha recibida: '" + fecha + "'");
        System.out.println("Hora recibida: '" + hora + "'");
        System.out.println("Numero de servicios: " + servicios.size());
        System.out.println("ID Pago recibido: " + idPago);

        for (int i = 0; i < servicios.size(); i++) {
            Object[] servicio = servicios.get(i);
            String descripcion = servicio.length > 1 ? (String) servicio[1] : "Sin descripcion";
            String precio = servicio.length > 2 ? servicio[2].toString() : "N/A";
            System.out.println("   Servicio " + i + ": " + descripcion + " - Precio: " + precio);
        }
        System.out.println("===========================\n");

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

            // VALIDAR Y FORMATEAR FECHA
            try {
                fecha = fecha.trim();
                // Verificar formato basico de fecha
                if (!fecha.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    throw new IllegalArgumentException("Formato de fecha debe ser YYYY-MM-DD");
                }
                java.sql.Date fechaSQL = java.sql.Date.valueOf(fecha);
                psCita.setDate(1, fechaSQL);
                System.out.println("Fecha valida: " + fecha);
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(this,
                        "Formato de fecha incorrecto:\n\n"
                        + "Fecha recibida: '" + fecha + "'\n"
                        + "Formato requerido: AAAA-MM-DD\n\n"
                        + "Ejemplo: 2025-12-03",
                        "Error en formato de fecha",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            // VALIDAR Y FORMATEAR HORA
            try {
                hora = hora.trim();
                // Asegurar formato HH:MM:SS
                if (!hora.contains(":")) {
                    hora = hora + ":00:00";
                } else if (hora.length() == 5) { // HH:MM
                    hora = hora + ":00";
                } else if (hora.length() == 4 && hora.contains(":")) { // H:MM
                    hora = "0" + hora + ":00";
                }

                // Verificar formato
                if (!hora.matches("\\d{2}:\\d{2}:\\d{2}")) {
                    throw new IllegalArgumentException("Formato de hora invalido");
                }

                java.sql.Time horaSQL = java.sql.Time.valueOf(hora);
                psCita.setTime(2, horaSQL);
                System.out.println("Hora valida: " + hora);
            } catch (IllegalArgumentException e) {
                JOptionPane.showMessageDialog(this,
                        "Formato de hora incorrecto:\n\n"
                        + "Hora recibida: '" + hora + "'\n"
                        + "Formatos aceptados: HH:MM o HH:MM:SS\n\n"
                        + "Ejemplos: 09:00, 14:30:00",
                        "Error en formato de hora",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            psCita.setString(3, "Confirmada");
            psCita.setInt(4, SesionUsuario.getIdUsuario());
            psCita.setInt(5, idPago);

            int filasCita = psCita.executeUpdate();
            System.out.println("Filas afectadas en tabla cita: " + filasCita);

            // OBTENER ID DE LA CITA
            rsCita = psCita.getGeneratedKeys();
            int idCita = 0;
            if (rsCita.next()) {
                idCita = rsCita.getInt(1);
                System.out.println("Cita creada con ID: " + idCita);
            } else {
                throw new SQLException("No se pudo obtener el ID de la cita creada");
            }

            String sqlServicios = "INSERT INTO cita_has_servicios (idCita, idServicios, Pago_idPago, Monto_anticipo) VALUES (?, ?, ?, ?)";
            psServicios = conn.prepareStatement(sqlServicios);

            // Usar un Set para evitar servicios duplicados por ID
            java.util.Set<Integer> serviciosInsertados = new java.util.HashSet<>();
            int serviciosAgregados = 0;

            System.out.println("\n=== PROCESANDO SERVICIOS PARA CITA " + idCita + " ===");

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
                            String precioStr = servicio[2].toString().replace("$", "").replace(",", "").trim();
                            try {
                                precioIndividual = Double.parseDouble(precioStr);
                            } catch (NumberFormatException e) {
                                System.out.println("Error parseando precio: " + precioStr + ", usando calculo proporcional");
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
                        System.out.println("Preparado para insertar: cita=" + idCita
                                + ", servicio=" + idServicio + ", precio=$" + precioIndividual);
                    } else {
                        System.out.println("Servicio duplicado omitido: " + descripcion + " (ID: " + idServicio + ")");
                    }
                } else {
                    System.out.println("ERROR: No se encontro ID valido para servicio: " + descripcion);
                }
            }

            // Ejecutar batch solo si hay servicios validos
            if (serviciosAgregados > 0) {
                int[] resultados = psServicios.executeBatch();
                System.out.println("Servicios insertados en cita_has_servicios: " + resultados.length);

                conn.commit();

                // Mostrar resumen al usuario
                StringBuilder mensaje = new StringBuilder();
                mensaje.append("¡Cita agendada exitosamente!\n\n");
                mensaje.append("ID de Cita: ").append(idCita).append("\n");
                mensaje.append("Fecha: ").append(fecha).append("\n");
                mensaje.append("Hora: ").append(hora.substring(0, 5)).append("\n");
                mensaje.append("Servicios: ").append(serviciosAgregados).append("\n");
                mensaje.append("Total pagado: $").append(String.format("%.2f", SesionUsuario.getMontoTotalCita()));

                JOptionPane.showMessageDialog(this, mensaje.toString(), "Cita Confirmada", JOptionPane.INFORMATION_MESSAGE);
            } else {
                System.out.println("ERROR: No hay servicios validos para insertar");
                throw new SQLException("No se pudieron insertar servicios para la cita");
            }

            // Limpiar datos de la sesion
            SesionUsuario.limpiarDatosCita();
            System.out.println("Datos de cita limpiados de la sesion\n");

        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error al agendar cita en la base de datos:\n\n" + e.getMessage(),
                    "Error de Base de Datos",
                    JOptionPane.ERROR_MESSAGE);

            try {
                if (conn != null) {
                    conn.rollback();
                    System.out.println("Transaccion revertida debido a error");
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        } finally {
            // Cerrar todos los recursos
            try {
                if (rsCita != null) {
                    rsCita.close();
                }
                if (psCita != null) {
                    psCita.close();
                }
                if (psServicios != null) {
                    psServicios.close();
                }
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                    System.out.println("Conexion cerrada");
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
        System.out.println("=== CALCULANDO MONTO TOTAL ===");

        for (int i = 0; i < servicios.size(); i++) {
            Object[] servicio = servicios.get(i);
            String descripcion = (String) servicio[1];
            String precioStr = (String) servicio[2]; 

            if (precioStr == null || precioStr.trim().isEmpty()) {
                System.out.println("ADVERTENCIA: Servicio '" + descripcion + "' sin precio. Usando 0.00");
                precioStr = "0.00";
            }

            precioStr = precioStr.replace("$", "").replace(",", "").trim();

            try {
                double precio = Double.parseDouble(precioStr);
                total += precio;
                System.out.println("Servicio " + i + ": " + descripcion + " - $" + precio + " - Total acumulado: $" + total);
            } catch (NumberFormatException e) {
                System.out.println("Error parseando precio: '" + precioStr + "' para servicio: " + descripcion);
                double precioAlternativo = obtenerPrecioDesdeCategoria(descripcion);
                total += precioAlternativo;
                System.out.println("Usando precio alternativo: $" + precioAlternativo);
            }
        }
        System.out.println("MONTO TOTAL FINAL: $" + total);
        System.out.println("=================================");
        return total;
    }

// Método auxiliar para obtener precio desde categoria_servicio
    private double obtenerPrecioDesdeCategoria(String descripcionCategoria) {
        String sql = "SELECT Precio FROM categoria_servicio WHERE Nombre_categoria = ? OR Descripcion LIKE ? LIMIT 1";

        try (java.sql.Connection conn = conexion.getConexion(); java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, descripcionCategoria);
            ps.setString(2, "%" + descripcionCategoria + "%");

            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("Precio");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0.0; // Precio por defecto
    }

    // MÉTODO NUEVO PARA CONFIGURAR EL CALENDARIO CORRECTAMENTE
    private void configurarCalendario() {
        jTextFieldFecha1.setEditable(false);
        jTextFieldFecha1.setFocusable(false);

        // Configurar fecha mínima como hoy (no se pueden seleccionar fechas pasadas)
        java.util.Calendar fechaMinima = java.util.Calendar.getInstance();
        fechaMinima.set(java.util.Calendar.HOUR_OF_DAY, 0);
        fechaMinima.set(java.util.Calendar.MINUTE, 0);
        fechaMinima.set(java.util.Calendar.SECOND, 0);
        fechaMinima.set(java.util.Calendar.MILLISECOND, 0);

        // Aplicar fecha mínima al JDayChooser
        jDayChooser2.setMinSelectableDate(fechaMinima.getTime());
        jDayChooser2.setMaxSelectableDate(null); // No hay límite máximo

        System.out.println("DEBUG - Fecha mínima establecida: "
                + new java.text.SimpleDateFormat("yyyy-MM-dd").format(fechaMinima.getTime()));

        // Actualizar calendario al cambiar el mes
        jMonthChooser1.addPropertyChangeListener("month", evt -> {
            int mesSeleccionado = jMonthChooser1.getMonth();
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.set(java.util.Calendar.MONTH, mesSeleccionado);
            cal.set(java.util.Calendar.YEAR, java.util.Calendar.getInstance().get(java.util.Calendar.YEAR));
            jDayChooser2.setMonth(cal.get(java.util.Calendar.MONTH));
            jDayChooser2.setYear(cal.get(java.util.Calendar.YEAR));
            actualizarFechaDesdeCalendario(); // Actualizar fecha inmediatamente
        });

        // Mostrar fecha al seleccionar un día
        jDayChooser2.addPropertyChangeListener("day", evt -> {
            actualizarFechaDesdeCalendario();
        });

        // Inicializar con fecha de hoy
        java.util.Calendar hoy = java.util.Calendar.getInstance();
        jDayChooser2.setDay(hoy.get(java.util.Calendar.DAY_OF_MONTH));
        jMonthChooser1.setMonth(hoy.get(java.util.Calendar.MONTH));
        actualizarFechaDesdeCalendario();
    }

// MÉTODO NUEVO PARA ACTUALIZAR LA FECHA DESDE EL CALENDARIO
    private void actualizarFechaDesdeCalendario() {
        try {
            int dia = jDayChooser2.getDay();
            int mes = jMonthChooser1.getMonth();
            int anio = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);

            if (dia > 0) {
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.set(anio, mes, dia);
                java.util.Date fechaSeleccionada = cal.getTime();

                // Verificar si la fecha es pasada
                java.util.Calendar hoy = java.util.Calendar.getInstance();
                hoy.set(java.util.Calendar.HOUR_OF_DAY, 0);
                hoy.set(java.util.Calendar.MINUTE, 0);
                hoy.set(java.util.Calendar.SECOND, 0);
                hoy.set(java.util.Calendar.MILLISECOND, 0);

                java.text.SimpleDateFormat formato = new java.text.SimpleDateFormat("yyyy-MM-dd");
                String fechaFormateada = formato.format(fechaSeleccionada);

                // Comparar si la fecha seleccionada es anterior a hoy
                if (fechaSeleccionada.before(hoy.getTime())) {
                    JOptionPane.showMessageDialog(this,
                            "No puedes seleccionar una fecha pasada.\n\n"
                            + "Fecha seleccionada: " + fechaFormateada + "\n"
                            + "Fecha actual: " + formato.format(hoy.getTime()) + "\n\n"
                            + "Por favor selecciona una fecha actual o futura.",
                            "Fecha inválida",
                            JOptionPane.WARNING_MESSAGE);

                    // Restablecer a la fecha de hoy
                    jDayChooser2.setDay(hoy.get(java.util.Calendar.DAY_OF_MONTH));
                    jMonthChooser1.setMonth(hoy.get(java.util.Calendar.MONTH));

                    // Actualizar con fecha de hoy
                    fechaSeleccionada = hoy.getTime();
                    fechaFormateada = formato.format(fechaSeleccionada);
                    System.out.println("DEBUG - Fecha corregida a hoy: " + fechaFormateada);
                }

                jTextFieldFecha1.setText(fechaFormateada);

                System.out.println("DEBUG - Fecha actualizada: " + fechaFormateada
                        + " (Día: " + dia + ", Mes: " + mes + ", Año: " + anio + ")");

                // Validar si la fecha está bloqueada
                if (fechaBloqueada(fechaFormateada)) {
                    JOptionPane.showMessageDialog(this,
                            "Esta fecha está completamente bloqueada. No se pueden agendar citas.\n\n"
                            + "Motivo: " + obtenerMotivoBloqueo(fechaFormateada),
                            "Fecha no disponible",
                            JOptionPane.WARNING_MESSAGE);
                    cbHora.setEnabled(false);
                    jButton4.setEnabled(false);
                } else {
                    cbHora.setEnabled(true);
                    jButton4.setEnabled(true);
                    verificarHorasDisponibles(fechaFormateada);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void finalizarAgendado(int idPago) {
        // Este método se llama después de que el usuario completa el pago
        insertarCitaYServicios(idPago);
    }

    ////Nuevo que agregue
    private void cargarServiciosDesdeBD() {
        if (conexion == null) {
            System.out.println("ERROR: Conexión es null, inicializando...");
            conexion = new ConexionBD("andynails");
        }

        cmbServicios.removeAllItems();
        cmbServicios.addItem("Seleccione un servicio");

        // Solo obtener servicios principales SIN PRECIO
        String sql = "SELECT idServicios, Nombre_servicio FROM servicios WHERE Nombre_servicio IS NOT NULL AND Nombre_servicio != '' ORDER BY Nombre_servicio";

        try (java.sql.Connection conn = conexion.getConexion(); java.sql.PreparedStatement ps = conn.prepareStatement(sql); java.sql.ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String nombreServicio = rs.getString("Nombre_servicio");
                if (nombreServicio != null && !nombreServicio.trim().isEmpty()) {
                    cmbServicios.addItem(nombreServicio.trim());
                }
            }

            System.out.println("DEBUG - Servicios cargados desde BD: " + (cmbServicios.getItemCount() - 1));

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error al cargar servicios: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            cargarServiciosPorDefecto();
        }
    }

    private void eliminarDuplicados() {
        java.util.List<Object[]> serviciosUnicos = new java.util.ArrayList<>();
        java.util.Set<String> descripciones = new java.util.HashSet<>();

        for (Object[] servicio : serviciosSeleccionados) {
            String descripcion = (String) servicio[1];
            if (!descripciones.contains(descripcion)) {
                descripciones.add(descripcion);
                serviciosUnicos.add(servicio);
            }
        }

        serviciosSeleccionados.clear();
        serviciosSeleccionados.addAll(serviciosUnicos);

        System.out.println("DEBUG - Duplicados eliminados. Servicios únicos: " + serviciosSeleccionados.size());
    }

    private void cargarServiciosPorDefecto() {
        cmbServicios.addItem("Uñas");
        cmbServicios.addItem("Maquillaje");
        cmbServicios.addItem("Peinado");
        cmbServicios.addItem("Tatuajes");
        cmbServicios.addItem("otros");
    }

    private void restaurarServiciosDesdeSesion() {
        java.util.List<Object[]> serviciosGuardados = SesionUsuario.getServiciosCita();

        if (serviciosGuardados != null && !serviciosGuardados.isEmpty()) {
            serviciosSeleccionados.clear();

            for (Object[] servicio : serviciosGuardados) {
                String descripcion = (String) servicio[1];
                boolean existe = false;

                for (Object[] existente : serviciosSeleccionados) {
                    if (((String) existente[1]).equals(descripcion)) {
                        existe = true;
                        break;
                    }
                }

                if (!existe) {
                    serviciosSeleccionados.add(servicio);
                }
            }

            System.out.println("DEBUG - Servicios restaurados desde sesión: " + serviciosSeleccionados.size());

            // Actualizar la interfaz
            if (!serviciosSeleccionados.isEmpty()) {
                indiceActual = 0;
                mostrarServiciosSeleccionados();
                iniciarCarrusel();
            }

            SesionUsuario.setServiciosCita(new java.util.ArrayList<>());
        }
    }

    private void eliminarServicioActual() {
        if (!serviciosSeleccionados.isEmpty() && indiceActual < serviciosSeleccionados.size()) {
            Object[] servicio = serviciosSeleccionados.get(indiceActual);
            String nombreServicio = (String) servicio[1];

            int confirmacion = JOptionPane.showConfirmDialog(this,
                    "¿Eliminar servicio: " + nombreServicio + "?",
                    "Confirmar Eliminación",
                    JOptionPane.YES_NO_OPTION);

            if (confirmacion == JOptionPane.YES_OPTION) {
                serviciosSeleccionados.remove(indiceActual);

                // Ajustar el índice actual
                if (serviciosSeleccionados.isEmpty()) {
                    indiceActual = 0;
                    jLabel4.setIcon(null);
                    label11.setText("No hay servicios seleccionados");
                    jLabel8.setText("$0");
                    if (timer != null) {
                        timer.stop();
                    }
                } else {
                    if (indiceActual >= serviciosSeleccionados.size()) {
                        indiceActual = serviciosSeleccionados.size() - 1;
                    }
                    mostrarServiciosSeleccionados();
                    iniciarCarrusel();
                }

                // Actualizar sesión
                SesionUsuario.setServiciosCita(serviciosSeleccionados);

                System.out.println("Servicio eliminado. Total: " + serviciosSeleccionados.size());
            }
        } else {
            JOptionPane.showMessageDialog(this,
                    "No hay servicios para eliminar.",
                    "Información",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    @Override
    public void setVisible(boolean visible) {
        if (visible) {
            // Cuando la ventana se hace visible, restaurar servicios
            restaurarServiciosDesdeSesion();
        }
        super.setVisible(visible);
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
        btnRegresar = new javax.swing.JButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenu12 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenuItem3 = new javax.swing.JMenuItem();
        jMenuItem7 = new javax.swing.JMenuItem();
        jMenu3 = new javax.swing.JMenu();
        jMenuItem4 = new javax.swing.JMenuItem();
        jMenu4 = new javax.swing.JMenu();
        jMenuItem5 = new javax.swing.JMenuItem();
        jMenu6 = new javax.swing.JMenu();
        jMenuItem6 = new javax.swing.JMenuItem();
        jMenu19 = new javax.swing.JMenu();
        jMenuItemCerrarSecion = new javax.swing.JMenuItem();

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

        cmbServicios.setToolTipText("Uñas,\nMaquillaje,\nPeinados,\n");
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
                .addGap(187, 187, 187)
                .addComponent(FACE, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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

        btnRegresar.setBackground(new java.awt.Color(255, 204, 255));
        btnRegresar.setText("Regresar");
        btnRegresar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRegresarActionPerformed(evt);
            }
        });

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
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel3Layout.createSequentialGroup()
                            .addGap(26, 26, 26)
                            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jTextFieldFecha1, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(cbHora, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(cmbServicios, javax.swing.GroupLayout.PREFERRED_SIZE, 113, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(67, 67, 67)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jButton4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btnRegresar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap(75, Short.MAX_VALUE))
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
                        .addGap(95, 95, 95)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel1)
                            .addComponent(jTextFieldFecha1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(36, 36, 36)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel2)
                            .addComponent(cbHora, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(35, 35, 35)
                        .addComponent(cmbServicios, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jButton4)
                        .addGap(18, 18, 18)
                        .addComponent(btnRegresar))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(8, 8, 8)
                        .addComponent(jLabel11)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(panel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jMonthChooser1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jDayChooser2, javax.swing.GroupLayout.PREFERRED_SIZE, 188, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(42, 42, 42)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 27, Short.MAX_VALUE)
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
        jMenuBar1.add(jMenu1);

        jMenu12.setText("CATALÓGO");

        jMenuItem1.setText("Uñas");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu12.add(jMenuItem1);

        jMenuItem2.setText("Peinados");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu12.add(jMenuItem2);

        jMenuItem3.setText("Maquillaje");
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem3ActionPerformed(evt);
            }
        });
        jMenu12.add(jMenuItem3);

        jMenuItem7.setText("Otros");
        jMenuItem7.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem7ActionPerformed(evt);
            }
        });
        jMenu12.add(jMenuItem7);

        jMenuBar1.add(jMenu12);

        jMenu3.setText("AGENDAR CITA");

        jMenuItem4.setText("Agendar Cita");
        jMenuItem4.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem4ActionPerformed(evt);
            }
        });
        jMenu3.add(jMenuItem4);

        jMenuBar1.add(jMenu3);

        jMenu4.setText("CONTACTO");

        jMenuItem5.setText("Contacto");
        jMenuItem5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem5ActionPerformed(evt);
            }
        });
        jMenu4.add(jMenuItem5);

        jMenuBar1.add(jMenu4);

        jMenu6.setText("LOGIN");

        jMenuItem6.setText("Login");
        jMenuItem6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem6ActionPerformed(evt);
            }
        });
        jMenu6.add(jMenuItem6);

        jMenuBar1.add(jMenu6);

        jMenu19.setText("CERRAR SESIÓN");

        jMenuItemCerrarSecion.setText("Cerrar sesión");
        jMenuItemCerrarSecion.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemCerrarSecionActionPerformed(evt);
            }
        });
        jMenu19.add(jMenuItemCerrarSecion);

        jMenuBar1.add(jMenu19);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

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

        debugServiciosActuales();
        // Validaciones básicas
        if (!SesionUsuario.sesionActiva()) {
            JOptionPane.showMessageDialog(this,
                    "Debes iniciar sesión o registrarte antes de agendar una cita.",
                    "Acceso denegado", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String fecha = jTextFieldFecha1.getText().trim();
        String hora = (String) cbHora.getSelectedItem();

        // ===== VALIDACIÓN DE FECHA VACÍA =====
        if (fecha == null || fecha.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Error: No has seleccionado una fecha para la cita.\n\n"
                    + "Por favor selecciona una fecha en el calendario antes de confirmar.",
                    "Fecha requerida",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // ===== VALIDACIÓN DE FECHA PASADA =====
        try {
            // Usar LocalDate para comparar solo fecha (sin hora)
            java.time.LocalDate fechaSeleccionada = java.time.LocalDate.parse(fecha);
            java.time.LocalDate hoy = java.time.LocalDate.now();

            // Verificar si la fecha es anterior a hoy
            if (fechaSeleccionada.isBefore(hoy)) {
                JOptionPane.showMessageDialog(this,
                        "Error: No puedes agendar una cita en una fecha pasada.\n\n"
                        + "Fecha seleccionada: " + fecha + "\n"
                        + "Fecha actual: " + hoy + "\n\n"
                        + "Por favor selecciona una fecha actual o futura.",
                        "Fecha inválida",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (java.time.format.DateTimeParseException e) {
            JOptionPane.showMessageDialog(this,
                    "Error: Formato de fecha inválido.\n\n"
                    + "Fecha: '" + fecha + "'\n"
                    + "Formato requerido: AAAA-MM-DD\n\n"
                    + "Ejemplo: 2025-12-03",
                    "Formato de fecha inválido",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

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

        // ===== VALIDACIÓN ADICIONAL: Si la fecha es hoy, verificar la hora =====
        try {
            java.time.LocalDate fechaSeleccionada = java.time.LocalDate.parse(fecha);
            java.time.LocalDate hoy = java.time.LocalDate.now();

            if (fechaSeleccionada.isEqual(hoy)) {
                // Si la fecha es hoy, verificar que la hora no sea pasada
                String horaSeleccionada = hora.split(":")[0]; // Obtener solo la hora (ej: "14" de "14:00")
                int horaActual = java.time.LocalTime.now().getHour();

                if (Integer.parseInt(horaSeleccionada) < horaActual) {
                    JOptionPane.showMessageDialog(this,
                            "No puedes agendar una cita en una hora pasada para el día de hoy.\n\n"
                            + "Hora seleccionada: " + hora + "\n"
                            + "Hora actual: " + String.format("%02d:00", horaActual) + "\n\n"
                            + "Por favor selecciona una hora futura.",
                            "Hora inválida",
                            JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
        } catch (Exception e) {
            System.out.println("Advertencia al validar hora para hoy: " + e.getMessage());
            // Continuar si hay algún error en esta validación secundaria
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
        String servicioSeleccionado = cmbServicios.getSelectedItem().toString();

        // No hacer nada si es la opción por defecto
        if ("Seleccione un servicio".equals(servicioSeleccionado)) {
            return;
        }

        // Guardar los servicios seleccionados actuales en SesionUsuario antes de abrir el catálogo
        SesionUsuario.setServiciosCita(serviciosSeleccionados);
        SesionUsuario.setFechaCita(jTextFieldFecha1.getText().trim());
        SesionUsuario.setHoraCita((String) cbHora.getSelectedItem());

        System.out.println("DEBUG - Guardando servicios antes de abrir catálogo: " + serviciosSeleccionados.size());

        // Abrir la interfaz correspondiente según el servicio seleccionado
        try {
            switch (servicioSeleccionado) {
                case "Uñas":
                    NewJCatalogoUñas catalogoUñas = new NewJCatalogoUñas();
                    catalogoUñas.setVisible(true);
                    break;
                case "Maquillaje":
                    NewJCatalogoMaq catalogoMaq = new NewJCatalogoMaq();
                    catalogoMaq.setVisible(true);
                    break;
                case "Peinado":
                    NewJCatalogoPeinado catalogoPeinado = new NewJCatalogoPeinado();
                    catalogoPeinado.setVisible(true);
                    break;
                case "Tatuajes":
                case "otros":
                    ConexionBD conexionCatalogo = new ConexionBD("andynails");
                    NewJCatalogoGenerico catalogoGenerico = new NewJCatalogoGenerico(conexionCatalogo);
                    catalogoGenerico.setVisible(true);
                    break;
                default:
                    ConexionBD conexionDefault = new ConexionBD("andynails");
                    new NewJCatalogoGenerico(conexionDefault).setVisible(true);
                    break;
            }

            // Ocultar esta ventana en lugar de cerrarla
            this.setVisible(false);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error al abrir el catálogo: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }

        // Resetear el combo a la opción por defecto
        cmbServicios.setSelectedIndex(0);

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

    private void jMenu1MenuSelected(javax.swing.event.MenuEvent evt) {//GEN-FIRST:event_jMenu1MenuSelected
        // TODO add your handling code here:
        //inicio
        Inicio Inicio = new Inicio();
        Inicio.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenu1MenuSelected

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        // TODO add your handling code here:
        NewJCatalogoUñas NewJCatalogoUñas = new NewJCatalogoUñas();
        NewJCatalogoUñas.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        // TODO add your handling code here:
        NewJCatalogoPeinado NewJCatalogoPeinado = new NewJCatalogoPeinado();
        NewJCatalogoPeinado.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem3ActionPerformed
        // TODO add your handling code here:
        NewJCatalogoMaq NewJCatalogoMaq = new NewJCatalogoMaq();
        NewJCatalogoMaq.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem3ActionPerformed

    private void jMenuItem7ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem7ActionPerformed
        // TODO add your handling code here:
        ConexionBD conexionCatalogo = new ConexionBD("andynails");
        NewJCatalogoGenerico catalogo = new NewJCatalogoGenerico(conexionCatalogo);
        catalogo.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem7ActionPerformed

    private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem4ActionPerformed
        // TODO add your handling code here:
        //agendar cita
        NewJAgenC NewJAgenC = new NewJAgenC();
        NewJAgenC.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem4ActionPerformed

    private void jMenuItem5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem5ActionPerformed
        // TODO add your handling code here:
        //boton de contacto
        NewJContacto NewJContacto = new NewJContacto();
        NewJContacto.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem5ActionPerformed

    private void jMenuItem6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem6ActionPerformed
        // TODO add your handling code here:
        //login
        NewJLogin NewJLogin = new NewJLogin();
        NewJLogin.setVisible(true);
        this.dispose(); // cierra la actual
    }//GEN-LAST:event_jMenuItem6ActionPerformed

    private void btnRegresarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRegresarActionPerformed
        // TODO add your handling code here:
        // Guardar servicios seleccionados actuales en la sesión
        if (serviciosSeleccionados != null && !serviciosSeleccionados.isEmpty()) {
            SesionUsuario.setServiciosCita(serviciosSeleccionados);
            System.out.println("DEBUG - Servicios guardados en sesión al regresar: " + serviciosSeleccionados.size());
        }

        // Guardar fecha y hora actuales si están seleccionadas
        String fecha = jTextFieldFecha1.getText().trim();
        String hora = (String) cbHora.getSelectedItem();

        if (!fecha.isEmpty()) {
            SesionUsuario.setFechaCita(fecha);
        }

        if (hora != null && !hora.isEmpty()) {
            SesionUsuario.setHoraCita(hora);
        }

        // Abrir la ventana anterior (Inicio o Mis Citas dependiendo del contexto)
        try {
            // Verificar si hay usuario logueado
            if (SesionUsuario.sesionActiva() && SesionUsuario.getIdUsuario() > 0) {
                // Si hay usuario logueado, abrir Mis Citas
                NewJMiscitasCi misCitas = new NewJMiscitasCi();
                misCitas.setVisible(true);
            } else {
                // Si no hay usuario logueado, abrir Inicio
                Inicio inicio = new Inicio();
                inicio.setVisible(true);
            }

            // Cerrar esta ventana
            this.dispose();

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error al regresar: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnRegresarActionPerformed

    private void jMenuItemCerrarSecionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemCerrarSecionActionPerformed
        // TODO add your handling code here:
        andynails.SessionManager.cerrarSesion(this);
    }//GEN-LAST:event_jMenuItemCerrarSecionActionPerformed

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
    private javax.swing.JButton btnRegresar;
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
    private javax.swing.JMenu jMenu12;
    private javax.swing.JMenu jMenu19;
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
    private javax.swing.JMenuItem jMenuItemCerrarSecion;
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
