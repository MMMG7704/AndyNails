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
import java.awt.FlowLayout;
import javax.swing.JOptionPane;
import org.mariadb.jdbc.Connection;
import java.sql.SQLException;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextField;

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
    private boolean esNuevaApertura = true; // Variable para controlar si es una nueva apertura
    private boolean procesandoCambioFecha = false; // Para evitar mensajes repetidos
    private boolean inicializacionDesdeCatalogo = false;

    /**
     * Creates new form NewJAgenC
     */
    public NewJAgenC(int idUsuario) {
        initComponents();
        this.idUsuario = idUsuario;
        init();
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
        this.idUsuario = SesionUsuario.getIdUsuario();

        // VERIFICAR si hay una cita pendiente en sesión
        serviciosSeleccionados = new java.util.ArrayList<>();
        limpiarTodoAlInicio();

        init();
    }

    private boolean citaYaGuardada(String fecha, String hora, String descripcion) {
        int idUsuario = SesionUsuario.getIdUsuario();
        if (idUsuario == 0) {
            return false;
        }

        String sql = """
        SELECT COUNT(*) FROM cita c 
        JOIN cita_has_servicios chs ON c.idCita = chs.idCita 
        JOIN servicios s ON chs.idServicios = s.idServicios 
        WHERE c.idUsuarios = ? 
        AND c.Fecha = ? 
        AND c.Hora = ? 
        AND s.Nombre_servicio LIKE ? 
        AND c.Estado IN ('confirmada', 'reservada')
    """;

        try (java.sql.Connection conn = conexion.getConexion(); java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idUsuario);
            ps.setString(2, fecha);
            ps.setTime(3, java.sql.Time.valueOf(hora + ":00"));
            ps.setString(4, "%" + descripcion + "%");

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

    private void init() {
        System.out.println("=== INICIALIZANDO NewJAgenC ===");
        System.out.println("Inicialización desde catálogo: " + inicializacionDesdeCatalogo);

        // Configurar componentes básicos
        jTextFieldFecha1.setEditable(false);
        jTextFieldFecha1.setFocusable(false);

        // SOLO limpiar si NO viene del catálogo
        if (!inicializacionDesdeCatalogo) {
            // Restaurar servicios desde sesión si existen
            java.util.List<Object[]> serviciosGuardados = SesionUsuario.getServiciosCita();
            if (serviciosGuardados != null && !serviciosGuardados.isEmpty()) {
                System.out.println("DEBUG - Restaurando servicios desde sesión: " + serviciosGuardados.size());
                serviciosSeleccionados = new java.util.ArrayList<>(serviciosGuardados);
            } else {
                limpiarEstadoCompleto();
            }
        }
        // Inicializar conexión
        conexion = new ConexionBD("andynails");

        // Configurar cerrado
        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Configurar controles del carrusel
        configurarControlesCarrusel();

        // Cargar servicios desde BD
        cargarServiciosDesdeBD();

        // Configurar calendario
        configurarCalendario();

        // Configurar redes sociales
        RedesSociales.configurarRedesSociales(INS, WPP, FACE);

        // Llenar combo de horas
        llenarComboHoras();

        // Configurar listeners
        configurarListeners();

        // Configurar estado inicial
        estadoInicial();

        // Si hay servicios, mostrarlos
        if (!serviciosSeleccionados.isEmpty()) {
            indiceActual = Math.min(indiceActual, serviciosSeleccionados.size() - 1);
            mostrarServiciosSeleccionados();
            iniciarCarrusel();
        }

        System.out.println("=== INICIALIZACIÓN COMPLETADA ===");
        debugServiciosActuales();
    }

    private String obtenerFechaActual() {
        java.text.SimpleDateFormat formato = new java.text.SimpleDateFormat("yyyy-MM-dd");
        return formato.format(new java.util.Date());
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
                System.out.println("Hora bloqueada: " + hora + " en fecha: " + fecha);
            }
        }
    }

    private String obtenerFechaSeleccionada() {
        if (jCalendar1 != null && jCalendar1.getCalendar() != null) {
            java.text.SimpleDateFormat formato = new java.text.SimpleDateFormat("yyyy-MM-dd");
            try {
                return formato.format(jCalendar1.getCalendar().getTime());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Fallback: verificar si jTextFieldFecha1 existe y tiene valor
        if (jTextFieldFecha1 != null && !jTextFieldFecha1.getText().trim().isEmpty()) {
            return jTextFieldFecha1.getText().trim();
        }

        // Si no hay nada, devolver la fecha actual
        java.text.SimpleDateFormat formato = new java.text.SimpleDateFormat("yyyy-MM-dd");
        return formato.format(new java.util.Date());
    }

    private void limpiarEstadoCompleto() {
        System.out.println("DEBUG - Limpiando estado completo");

        // Limpiar lista de servicios
        serviciosSeleccionados.clear();

        // Limpiar calendario (establecer fecha actual)
        java.util.Calendar hoy = java.util.Calendar.getInstance();
        jCalendar1.setCalendar(hoy);

        // Limpiar hora
        cbHora.removeAllItems();

        // Limpiar labels
        jLabel4.setIcon(null);
        jLabel5.setText("No hay servicios seleccionados");
        jLabel8.setText("$0");

        // Detener timer si existe
        if (timer != null) {
            timer.stop();
            timer = null;
        }

        // Resetear índice
        indiceActual = 0;

        // NO limpiar datos en sesión aquí, solo cuando el usuario confirme
        System.out.println("DEBUG - Estado limpiado completamente");
    }

    private void llenarComboHoras() {
        cbHora.removeAllItems();
        for (int hora = 9; hora <= 18; hora++) {
            cbHora.addItem(String.format("%02d:00", hora));
        }
        // Establecer hora predeterminada
        cbHora.setSelectedItem("10:00");
    }

   private void configurarListeners() {
    btnRegresar.addActionListener(e -> btnRegresarActionPerformed(e));
    
    cbHora.addActionListener(e -> {
        String fecha = jTextFieldFecha1.getText().trim();
        String hora = (String) cbHora.getSelectedItem();
        
        if (!fecha.isEmpty() && hora != null) {
            System.out.println("Cambio de hora: " + fecha + " " + hora);
            
            // Primero verificar advertencias
            verificarAdvertenciasFechaHora();
            
            // Luego actualizar el servicio actual
            if (!serviciosSeleccionados.isEmpty()) {
                actualizarFechaHoraServicioActual();
                mostrarServiciosSeleccionados(); // Actualizar display
            }
        }
    });
}

    private void estadoInicial() {
        // Habilitar/deshabilitar controles según login
        boolean logueado = idUsuario > 0;
        jButton4.setEnabled(logueado);
        cmbServicios.setEnabled(logueado);

        // Establecer fecha actual en campo de texto
        actualizarFechaDesdeJCalendar();

        // Si no hay usuario logueado, mostrar mensaje
        if (!logueado) {
            jLabel5.setText("Inicie sesión para agendar citas");
        }
    }

    private void verificarHoraYActualizar(String fecha, String hora) {
        if (!serviciosSeleccionados.isEmpty() && indiceActual < serviciosSeleccionados.size()) {
            Object[] servicioActual = serviciosSeleccionados.get(indiceActual);
            String descripcionServicioActual = (String) servicioActual[1];

            if (horaBloqueadaParaServicio(fecha, hora, descripcionServicioActual)) {
                JOptionPane.showMessageDialog(this,
                        "Esta hora no está disponible para el servicio actual: " + descripcionServicioActual,
                        "Hora no disponible",
                        JOptionPane.WARNING_MESSAGE);
                // Restaurar hora anterior
                String horaAnterior = servicioActual.length > 4 ? (String) servicioActual[4] : "10:00";
                cbHora.setSelectedItem(horaAnterior);
                return;
            }

            if (clienteTieneCitaMismaHora(fecha, hora)) {
                JOptionPane.showMessageDialog(this,
                        "Ya tienes una cita agendada para esta hora.",
                        "Hora Ocupada",
                        JOptionPane.WARNING_MESSAGE);
            }

            // Actualizar fecha/hora del servicio actual
            actualizarFechaHoraServicioActual();
        }
    }

    private void actualizarFechaHoraServicioActual() {
        if (!serviciosSeleccionados.isEmpty() && indiceActual < serviciosSeleccionados.size()) {
            Object[] servicio = serviciosSeleccionados.get(indiceActual);

            String fechaActual = jTextFieldFecha1.getText().trim();
            if (fechaActual.isEmpty()) {
                fechaActual = obtenerFechaActual();
            }

            String horaActual = (String) cbHora.getSelectedItem();
            if (horaActual == null) {
                horaActual = "10:00";
            }

            // Asegurarse de que el array tiene 5 elementos
            if (servicio.length < 5) {
                Object[] nuevoArray = new Object[5];
                System.arraycopy(servicio, 0, nuevoArray, 0, Math.min(servicio.length, 5));
                servicio = nuevoArray;
                serviciosSeleccionados.set(indiceActual, servicio);
            }

            // Actualizar fecha y hora
            servicio[3] = fechaActual;
            servicio[4] = horaActual;

            System.out.println("Actualizado servicio " + indiceActual + ": " + servicio[1]
                    + " - Fecha: " + fechaActual + " - Hora: " + horaActual);
        }
    }

    private void debugServiciosActuales() {
        System.out.println("=== SERVICIOS ACTUALES EN NewJAgenC ===");
        System.out.println("Número de servicios: " + serviciosSeleccionados.size());
        for (int i = 0; i < serviciosSeleccionados.size(); i++) {
            Object[] servicio = serviciosSeleccionados.get(i);
            String descripcion = (String) servicio[1];
            String precio = (String) servicio[2];
            String fecha = servicio.length > 3 ? (String) servicio[3] : "Sin fecha";
            String hora = servicio.length > 4 ? (String) servicio[4] : "Sin hora";

            System.out.println("Servicio " + i + ": " + descripcion);
            System.out.println("  Precio: " + precio);
            System.out.println("  Fecha específica: " + fecha);
            System.out.println("  Hora específica: " + hora);
        }
        System.out.println("Fecha general en sesión: " + SesionUsuario.getFechaCita());
        System.out.println("Hora general en sesión: " + SesionUsuario.getHoraCita());
        System.out.println("======================================");
    }

    private void initDesdeCatalogo(ImageIcon imagen, String descripcion, String precio) {
        System.out.println("=== INICIALIZANDO DESDE CATÁLOGO ===");

        // Configurar componentes básicos (sin limpiar)
        jTextFieldFecha1.setEditable(false);
        jTextFieldFecha1.setFocusable(false);

        // NO limpiar el estado aquí - mantener servicios existentes
        if (conexion == null) {
            conexion = new ConexionBD("andynails");
        }

        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        configurarControlesCarrusel();

        // Solo cargar servicios desde BD si no están ya cargados
        if (cmbServicios.getItemCount() == 0) {
            cargarServiciosDesdeBD();
        }

        configurarCalendario();
        RedesSociales.configurarRedesSociales(INS, WPP, FACE);

        // Solo llenar horas si están vacías
        if (cbHora.getItemCount() == 0) {
            llenarComboHoras();
        }

        configurarListeners();

        // Agregar el nuevo servicio
        agregarNuevoServicioDesdeCatalogo(imagen, descripcion, precio);

        System.out.println("=== INICIALIZACIÓN DESDE CATÁLOGO COMPLETADA ===");
        debugServiciosActuales();
    }

    private void agregarNuevoServicioDesdeCatalogo(ImageIcon imagen, String descripcion, String precio) {
        System.out.println("=== AGREGANDO NUEVO SERVICIO DESDE CATÁLOGO ===");
        System.out.println("Servicios actuales ANTES de agregar: " + serviciosSeleccionados.size());

        // Verificar duplicados
        boolean existe = false;
        for (Object[] servicio : serviciosSeleccionados) {
            if (((String) servicio[1]).equals(descripcion)) {
                existe = true;
                break;
            }
        }

        if (existe) {
            int respuesta = JOptionPane.showConfirmDialog(this,
                    "Este servicio ya está agregado. ¿Desea agregarlo de nuevo?",
                    "Servicio duplicado",
                    JOptionPane.YES_NO_OPTION);

            if (respuesta == JOptionPane.NO_OPTION) {
                return;
            }
        }

        // Obtener fecha y hora actuales del calendario
        String fechaActual = jTextFieldFecha1.getText().trim();
        if (fechaActual.isEmpty()) {
            fechaActual = obtenerFechaActual();
        }

        String horaActual = (String) cbHora.getSelectedItem();
        if (horaActual == null) {
            horaActual = "10:00";
        }

        // Verificar disponibilidad
        if (horaBloqueadaParaServicio(fechaActual, horaActual, descripcion)) {
            JOptionPane.showMessageDialog(this,
                    "La hora actual no está disponible para este servicio.\nPor favor seleccione otra hora antes de confirmar.",
                    "Hora no disponible",
                    JOptionPane.WARNING_MESSAGE);
        }

        // Crear nuevo servicio
        Object[] nuevoServicio = new Object[5];
        nuevoServicio[0] = imagen;
        nuevoServicio[1] = descripcion;
        nuevoServicio[2] = precio;
        nuevoServicio[3] = fechaActual;
        nuevoServicio[4] = horaActual;

        // AGREGAR a la lista existente, NO reemplazar
        serviciosSeleccionados.add(nuevoServicio);

        System.out.println("Servicio agregado: " + descripcion);
        System.out.println("Servicios DESPUÉS de agregar: " + serviciosSeleccionados.size());

        // Actualizar interfaz
        if (!serviciosSeleccionados.isEmpty()) {
            indiceActual = serviciosSeleccionados.size() - 1;
            mostrarServiciosSeleccionados();
            iniciarCarrusel();
        }

        debugServiciosActuales();
    }

    private void configurarControlesCarrusel() {
        jLabel4.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                siguienteServicio();
            }
        });

        jLabel5.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                servicioAnterior();
            }
        });
    }

    private void servicioAnterior() {
        if (serviciosSeleccionados.size() > 1) {
            indiceActual = (indiceActual - 1 + serviciosSeleccionados.size()) % serviciosSeleccionados.size();
            mostrarServiciosSeleccionados();
        }
    }

    private boolean fechaBloqueada(String fecha) {
        String sqlBloqueo = "SELECT COUNT(*) FROM bloqueo_horario WHERE Fecha = ?";

        try (java.sql.Connection conn = conexion.getConexion(); java.sql.PreparedStatement psBloqueo = conn.prepareStatement(sqlBloqueo)) {

            psBloqueo.setString(1, fecha);
            try (java.sql.ResultSet rsBloqueo = psBloqueo.executeQuery()) {
                if (rsBloqueo.next() && rsBloqueo.getInt(1) > 0) {
                    return true;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean horaBloqueada(String fecha, String hora) {
        java.sql.Time horaTime = java.sql.Time.valueOf(hora + ":00");

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

            psBloqueo.setString(1, fecha);
            psBloqueo.setTime(2, horaTime);
            try (java.sql.ResultSet rsBloqueo = psBloqueo.executeQuery()) {
                if (rsBloqueo.next() && rsBloqueo.getInt(1) > 0) {
                    return true;
                }
            }

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
                            return true;
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // MÉTODO NUEVO: Solo verifica el servicio específico
    private boolean horaBloqueadaParaServicio(String fecha, String hora, String descripcionServicio) {
        if (descripcionServicio == null || descripcionServicio.isEmpty()) {
            return false; // No hay servicio para verificar
        }

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
                    System.out.println("Hora bloqueada por horario general: " + fecha + " " + hora);
                    return true;
                }
            }

            // Verificar en cita solo para el servicio específico
            int idServicio = obtenerIdServicioPorDescripcion(descripcionServicio);

            if (idServicio > 0) {
                psCita.setString(1, fecha);
                psCita.setTime(2, horaTime);
                psCita.setInt(3, idServicio);

                try (java.sql.ResultSet rsCita = psCita.executeQuery()) {
                    if (rsCita.next() && rsCita.getInt(1) > 0) {
                        System.out.println("Hora ocupada para servicio: " + descripcionServicio + " - ID: " + idServicio);
                        return true;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

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
            jLabel5.setText("No hay servicios disponibles");
            jLabel8.setText("$0");
            if (timer != null) {
                timer.stop();
            }
        }
    }

    private void iniciarCarrusel() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }

        if (serviciosSeleccionados.size() > 1) {
            timer = new Timer(5000, e -> siguienteServicio());
            timer.start();
            System.out.println("Carrusel iniciado - Cambio cada 5 segundos");
        } else {
            mostrarServiciosSeleccionados();
        }
    }

    private void siguienteServicio() {
        if (!serviciosSeleccionados.isEmpty() && serviciosSeleccionados.size() > 1) {
            if (timer != null) {
                timer.stop();
                new Timer(10000, e -> {
                    if (timer != null) {
                        timer.start();
                    }
                }).start();
            }

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

private void mostrarServiciosSeleccionados() {
    if (serviciosSeleccionados.isEmpty()) {
        jLabel4.setIcon(null);
        jLabel5.setText("<html><b>No hay servicios seleccionados</b><br><br>" +
                       "Agrega servicios desde el catálogo</html>");
        jLabel8.setText("$0");
        return;
    }

    Object[] servicio = serviciosSeleccionados.get(indiceActual);
    ImageIcon imagen = (ImageIcon) servicio[0];
    String descripcion = (String) servicio[1];
    String precio = (String) servicio[2];
    String fecha = servicio.length > 3 ? (String) servicio[3] : "";
    String hora = servicio.length > 4 ? (String) servicio[4] : "";

    // Escalar y mostrar imagen
    if (imagen != null) {
        Image img = imagen.getImage();
        Image scaledImg = img.getScaledInstance(jLabel4.getWidth(), jLabel4.getHeight(), Image.SCALE_SMOOTH);
        jLabel4.setIcon(new ImageIcon(scaledImg));
    } else {
        jLabel4.setIcon(null);
    }

    // Construir texto HTML para jLabel5
    StringBuilder info = new StringBuilder();
    info.append("<html><div style='font-family: Arial; font-size: 12px;'>");
    info.append("<b style='font-size: 14px; color: #333;'>").append(descripcion).append("</b><br><br>");
    
    info.append("<div style='background-color: #f0f0f0; padding: 5px; border-radius: 5px;'>");
    info.append("<b>Posición:</b> ").append(indiceActual + 1).append(" de ").append(serviciosSeleccionados.size()).append("<br>");
    
    if (!fecha.isEmpty() && !hora.isEmpty()) {
        info.append("<b>Fecha:</b> ").append(formatearFechaBonita(fecha)).append("<br>");
        info.append("<b>Hora:</b> ").append(hora).append("<br>");
        
        // Sincronizar controles
        if (!fecha.equals(jTextFieldFecha1.getText().trim())) {
            try {
                java.text.SimpleDateFormat formato = new java.text.SimpleDateFormat("yyyy-MM-dd");
                java.util.Date fechaServicio = formato.parse(fecha);
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.setTime(fechaServicio);
                jCalendar1.setCalendar(cal);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        if (!hora.equals(cbHora.getSelectedItem())) {
            cbHora.setSelectedItem(hora);
        }
    } else {
        info.append("<i style='color: #666;'>Fecha/Hora no asignadas</i><br>");
        info.append("<small>Selecciona fecha y hora en el calendario</small>");
    }
    
    info.append("</div></div></html>");
    
    jLabel5.setText(info.toString());
    
    // Mostrar precio
    if (precio != null && !precio.isEmpty()) {
        jLabel8.setText(precio.startsWith("$") ? precio : "$" + precio);
    } else {
        jLabel8.setText("$0");
    }
    
    // Mostrar el total
    double total = calcularMontoTotal(serviciosSeleccionados);
    System.out.println("Total actual: $" + total);
}

// Método auxiliar para formatear fecha
private String formatearFechaBonita(String fecha) {
    try {
        java.text.SimpleDateFormat formatoEntrada = new java.text.SimpleDateFormat("yyyy-MM-dd");
        java.text.SimpleDateFormat formatoSalida = new java.text.SimpleDateFormat("dd/MM/yyyy");
        java.util.Date fechaObj = formatoEntrada.parse(fecha);
        return formatoSalida.format(fechaObj);
    } catch (Exception e) {
        return fecha;
    }
}

    private void actualizarCalendarioConFechaHora(String fecha, String hora) {
        try {
            if (!fecha.equals(jTextFieldFecha1.getText())) {
                java.text.SimpleDateFormat formato = new java.text.SimpleDateFormat("yyyy-MM-dd");
                java.util.Date fechaObj = formato.parse(fecha);
                java.util.Calendar cal = java.util.Calendar.getInstance();
                cal.setTime(fechaObj);
                jTextFieldFecha1.setText(fecha);
            }

            if (hora != null && !hora.equals(cbHora.getSelectedItem())) {
                cbHora.setSelectedItem(hora);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ImageIcon escalarImagen(ImageIcon icon, int ancho, int alto) {
        if (icon == null) {
            return null;
        }
        Image img = icon.getImage();
        Image nueva = img.getScaledInstance(ancho, alto, Image.SCALE_SMOOTH);
        return new ImageIcon(nueva);
    }

    private int obtenerIdServicioPorDescripcion(String descripcion) {
        String descLower = descripcion.toLowerCase().trim();

        if (descLower.contains("tatuaje")) {
            System.out.println("OBTENIENDO ID: Tatuajes -> 15");
            return 15;
        }
        if (descLower.contains("masaje")) {
            System.out.println("OBTENIENDO ID: Masajes -> 16");
            return 16;
        }
        if (descLower.contains("uña") || descLower.contains("unas")) {
            System.out.println("OBTENIENDO ID: Uñas -> 1");
            return 1;
        }
        if (descLower.contains("maquillaje")) {
            System.out.println("OBTENIENDO ID: Maquillaje -> 2");
            return 2;
        }
        if (descLower.contains("peinado")) {
            System.out.println("OBTENIENDO ID: Peinado -> 3");
            return 3;
        }
        if (descLower.contains("otro")) {
            System.out.println("OBTENIENDO ID: Otros -> 13");
            return 13;
        }

        System.out.println("OBTENIENDO ID: '" + descripcion + "' -> 13 (por defecto)");
        return 13;
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
        java.util.List<Object[]> servicios = SesionUsuario.getServiciosCita();

        if (servicios == null || servicios.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Error: No hay servicios seleccionados.",
                    "Servicios requeridos",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        java.util.Map<String, java.util.List<Object[]>> citasAgrupadas = new java.util.HashMap<>();

        for (Object[] servicio : servicios) {
            String fecha = servicio.length > 3 ? (String) servicio[3] : null;
            String hora = servicio.length > 4 ? (String) servicio[4] : null;

            if (fecha == null || hora == null) {
                JOptionPane.showMessageDialog(this,
                        "Error: Algunos servicios no tienen fecha/hora asignada.",
                        "Datos incompletos",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            String clave = fecha + "|" + hora;
            citasAgrupadas.computeIfAbsent(clave, k -> new java.util.ArrayList<>()).add(servicio);
        }

        System.out.println("=== CREANDO " + citasAgrupadas.size() + " CITAS ===");

        int citasCreadas = 0;
        for (java.util.Map.Entry<String, java.util.List<Object[]>> entrada : citasAgrupadas.entrySet()) {
            String[] partes = entrada.getKey().split("\\|");
            String fecha = partes[0];
            String hora = partes[1];
            java.util.List<Object[]> serviciosCita = entrada.getValue();

            System.out.println("Creando cita para " + fecha + " " + hora
                    + " con " + serviciosCita.size() + " servicios");

            if (crearCitaIndividual(fecha, hora, serviciosCita, idPago)) {
                citasCreadas++;
            }
        }

        if (citasCreadas > 0) {
            JOptionPane.showMessageDialog(this,
                    "¡" + citasCreadas + " cita(s) agendada(s) exitosamente!",
                    "Citas Confirmadas",
                    JOptionPane.INFORMATION_MESSAGE);

            SesionUsuario.limpiarDatosCita();
        }
    }

    private boolean crearCitaIndividual(String fecha, String hora,
            java.util.List<Object[]> servicios, int idPago) {
        java.sql.Connection conn = null;

        try {
            conn = ConexionBD.getConnection();
            conn.setAutoCommit(false);

            String sqlCita = "INSERT INTO cita (Fecha, Hora, Estado, idUsuarios, Pago_idPago) VALUES (?, ?, ?, ?, ?)";
            java.sql.PreparedStatement psCita = conn.prepareStatement(sqlCita, java.sql.Statement.RETURN_GENERATED_KEYS);

            psCita.setDate(1, java.sql.Date.valueOf(fecha));
            psCita.setTime(2, java.sql.Time.valueOf(hora + ":00"));
            psCita.setString(3, "Confirmada");
            psCita.setInt(4, SesionUsuario.getIdUsuario());
            psCita.setInt(5, idPago);

            psCita.executeUpdate();
            java.sql.ResultSet rs = psCita.getGeneratedKeys();
            int idCita = 0;
            if (rs.next()) {
                idCita = rs.getInt(1);
            }

            String sqlServicios = "INSERT INTO cita_has_servicios (idCita, idServicios, Pago_idPago, Monto_anticipo) VALUES (?, ?, ?, ?)";
            java.sql.PreparedStatement psServicios = conn.prepareStatement(sqlServicios);

            for (Object[] servicio : servicios) {
                String descripcion = (String) servicio[1];
                int idServicio = obtenerIdServicioPorDescripcion(descripcion);

                if (idServicio > 0) {
                    double precio = 0.0;
                    if (servicio.length > 2 && servicio[2] != null) {
                        String precioStr = servicio[2].toString().replace("$", "").trim();
                        try {
                            precio = Double.parseDouble(precioStr);
                        } catch (NumberFormatException e) {
                            precio = obtenerPrecioPorDefecto(descripcion);
                        }
                    }

                    psServicios.setInt(1, idCita);
                    psServicios.setInt(2, idServicio);
                    psServicios.setInt(3, idPago);
                    psServicios.setBigDecimal(4, java.math.BigDecimal.valueOf(precio));
                    psServicios.addBatch();
                }
            }

            psServicios.executeBatch();
            conn.commit();

            System.out.println("Cita " + idCita + " creada para " + fecha + " " + hora);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            return false;
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private double calcularMontoTotal(java.util.List<Object[]> servicios) {
        double total = 0.0;
        System.out.println("=== CALCULANDO MONTO TOTAL ===");

        for (int i = 0; i < servicios.size(); i++) {
            Object[] servicio = servicios.get(i);
            String descripcion = (String) servicio[1];
            String precioStr = (String) servicio[2];

            if (precioStr == null || precioStr.trim().isEmpty() || precioStr.equals("$0")) {
                System.out.println("ADVERTENCIA: Servicio '" + descripcion + "' sin precio. Buscando en BD...");

                double precioDesdeBD = obtenerPrecioDesdeCategoria(descripcion);
                if (precioDesdeBD == 0.0) {
                    precioDesdeBD = obtenerPrecioPorDefecto(descripcion);
                    System.out.println("Usando precio por defecto para '" + descripcion + "': $" + precioDesdeBD);
                } else {
                    System.out.println("Precio obtenido de BD para '" + descripcion + "': $" + precioDesdeBD);
                }

                precioStr = "$" + precioDesdeBD;
                servicio[2] = precioStr;
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
                System.out.println("Usando precio alternativo desde categoría: $" + precioAlternativo);
            }
        }
        System.out.println("MONTO TOTAL FINAL: $" + total);
        System.out.println("=================================");
        return total;
    }

    private double obtenerPrecioPorDefecto(String descripcion) {
        descripcion = descripcion.toLowerCase().trim();

        if (descripcion.contains("tatuaje")) {
            return 450.00;
        }
        if (descripcion.contains("uña") || descripcion.contains("ballerina")
                || descripcion.contains("francesa") || descripcion.contains("cuadrada")) {
            return 350.00;
        }
        if (descripcion.contains("maquillaje")) {
            return 500.00;
        }
        if (descripcion.contains("peinado")) {
            return 300.00;
        }
        if (descripcion.contains("masaje")) {
            return 1700.00;
        }
        if (descripcion.contains("boda")) {
            return 1500.00;
        }
        if (descripcion.contains("social")) {
            return 500.00;
        }

        return 250.00;
    }

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
        return 0.0;
    }

private void configurarCalendario() {
    jTextFieldFecha1.setEditable(false);
    jTextFieldFecha1.setFocusable(false);

    if (jCalendar1 != null) {
        java.util.Calendar fechaMinima = java.util.Calendar.getInstance();
        fechaMinima.set(java.util.Calendar.HOUR_OF_DAY, 0);
        fechaMinima.set(java.util.Calendar.MINUTE, 0);
        fechaMinima.set(java.util.Calendar.SECOND, 0);
        fechaMinima.set(java.util.Calendar.MILLISECOND, 0);

        jCalendar1.setMinSelectableDate(fechaMinima.getTime());

        jCalendar1.addPropertyChangeListener("calendar", evt -> {
            if (!procesandoCambioFecha && "calendar".equals(evt.getPropertyName())) {
                procesandoCambioFecha = true;
                try {
                    actualizarFechaDesdeJCalendar();
                    
                    // NUEVO: Verificar advertencias después de cambiar la fecha
                    verificarAdvertenciasFechaHora();
                    
                    if (!serviciosSeleccionados.isEmpty()) {
                        actualizarFechaHoraServicioActual();
                        mostrarServiciosSeleccionados(); // Actualizar display
                    }
                } finally {
                    procesandoCambioFecha = false;
                }
            }
        });

        jCalendar1.setCalendar(fechaMinima);
        actualizarFechaDesdeJCalendar();
    }
}


private void verificarAdvertenciasFechaHora() {
    String fecha = obtenerFechaSeleccionada();
    String hora = (String) cbHora.getSelectedItem();
    
    if (fecha == null || fecha.isEmpty() || hora == null) {
        return;
    }
    
    System.out.println("Verificando advertencias para: " + fecha + " " + hora);
    
    // 1. Verificar si la fecha está bloqueada completamente
    if (fechaBloqueada(fecha)) {
        String motivo = obtenerMotivoBloqueo(fecha);
        JOptionPane.showMessageDialog(this,
            "⚠️ FECHA BLOQUEADA ⚠️\n\n" +
            "La fecha " + fecha + " está completamente bloqueada.\n" +
            "Motivo: " + motivo + "\n\n" +
            "Por favor seleccione otra fecha.",
            "Fecha no disponible",
            JOptionPane.WARNING_MESSAGE);
        return;
    }
    
    // 2. Verificar si es fecha pasada (ya lo hace actualizarFechaDesdeJCalendar)
    
    // 3. Verificar si hay servicios seleccionados para verificar disponibilidad específica
    if (!serviciosSeleccionados.isEmpty() && indiceActual < serviciosSeleccionados.size()) {
        Object[] servicioActual = serviciosSeleccionados.get(indiceActual);
        String descripcionServicioActual = (String) servicioActual[1];
        
        // 4. Verificar si la hora está bloqueada para el servicio actual
        if (horaBloqueadaParaServicio(fecha, hora, descripcionServicioActual)) {
            JOptionPane.showMessageDialog(this,
                "⚠️ HORA NO DISPONIBLE ⚠️\n\n" +
                "La hora " + hora + " no está disponible para:\n" +
                descripcionServicioActual + "\n\n" +
                "Por favor seleccione otra hora.",
                "Hora ocupada",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // 5. Verificar si el cliente ya tiene una cita en esa misma hora
        if (clienteTieneCitaMismaHora(fecha, hora)) {
            JOptionPane.showMessageDialog(this,
                "⚠️ CITA DUPLICADA ⚠️\n\n" +
                "Ya tienes una cita agendada para:\n" +
                "Fecha: " + fecha + "\n" +
                "Hora: " + hora + "\n\n" +
                "Por favor seleccione otra hora o fecha.",
                "Hora ocupada",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // 6. Si todo está bien, mostrar confirmación
        System.out.println("Fecha y hora válidas para: " + descripcionServicioActual);
    }
}


    private void actualizarFechaDesdeJCalendar() {
        try {
            java.util.Date fechaSeleccionada = jCalendar1.getDate();

            if (fechaSeleccionada == null) {
                return;
            }

            java.text.SimpleDateFormat formato = new java.text.SimpleDateFormat("yyyy-MM-dd");
            String fechaFormateada = formato.format(fechaSeleccionada);

            java.util.Calendar hoy = java.util.Calendar.getInstance();
            hoy.set(java.util.Calendar.HOUR_OF_DAY, 0);
            hoy.set(java.util.Calendar.MINUTE, 0);
            hoy.set(java.util.Calendar.SECOND, 0);
            hoy.set(java.util.Calendar.MILLISECOND, 0);

            String fechaHoy = formato.format(hoy.getTime());

            // Verificar si la fecha seleccionada es pasada
            if (fechaSeleccionada.before(hoy.getTime())) {
                JOptionPane.showMessageDialog(this,
                        "No puedes seleccionar una fecha pasada.\n\n"
                        + "Fecha seleccionada: " + fechaFormateada + "\n"
                        + "Fecha actual: " + fechaHoy + "\n\n"
                        + "Por favor selecciona una fecha actual o futura.",
                        "Fecha inválida",
                        JOptionPane.WARNING_MESSAGE);

                // Establecer fecha de hoy
                jCalendar1.setDate(hoy.getTime());
                fechaFormateada = fechaHoy;
                System.out.println("DEBUG - Fecha corregida a hoy: " + fechaFormateada);
            }

            jTextFieldFecha1.setText(fechaFormateada);

            System.out.println("DEBUG - Fecha actualizada desde jCalendar1: " + fechaFormateada);

            if (fechaBloqueada(fechaFormateada)) {
                String motivo = obtenerMotivoBloqueo(fechaFormateada);
                JOptionPane.showMessageDialog(this,
                        "Esta fecha está completamente bloqueada. No se pueden agendar citas.\n\n"
                        + "Motivo: " + motivo,
                        "Fecha no disponible",
                        JOptionPane.WARNING_MESSAGE);
                cbHora.setEnabled(false);
                jButton4.setEnabled(false);
            } else {
                cbHora.setEnabled(true);
                jButton4.setEnabled(idUsuario > 0);
                verificarHorasDisponibles(fechaFormateada);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void finalizarAgendado(int idPago) {
        insertarCitaYServicios(idPago);

        // LIMPIAR datos locales después de guardar
        serviciosSeleccionados.clear();
        indiceActual = 0;

        // Actualizar interfaz
        jLabel4.setIcon(null);
        jLabel5.setText("No hay servicios seleccionados");
        jLabel8.setText("$0");

        // Detener timer
        if (timer != null) {
            timer.stop();
            timer = null;
        }

        System.out.println("DEBUG - Datos locales limpiados después de guardar cita");
    }

    private void cargarServiciosDesdeBD() {
        if (conexion == null) {
            System.out.println("ERROR: Conexión es null, inicializando...");
            conexion = new ConexionBD("andynails");
        }

        cmbServicios.removeAllItems();
        cmbServicios.addItem("Seleccione un servicio");

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
        // Este método ahora NO se llama automáticamente
        // Solo se llama si realmente queremos restaurar desde sesión
        System.out.println("DEBUG - NO restaurando servicios automáticamente desde sesión");
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

                if (serviciosSeleccionados.isEmpty()) {
                    indiceActual = 0;
                    jLabel4.setIcon(null);
                    jLabel5.setText("No hay servicios seleccionados");
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
            // Actualizar información de usuario
            idUsuario = SesionUsuario.getIdUsuario();
            boolean logueado = idUsuario > 0;
            jButton4.setEnabled(logueado);
            cmbServicios.setEnabled(logueado);

            // Mostrar servicios si hay
            if (!serviciosSeleccionados.isEmpty()) {
                indiceActual = Math.min(indiceActual, serviciosSeleccionados.size() - 1);
                mostrarServiciosSeleccionados();
                iniciarCarrusel();
            }

            System.out.println("DEBUG - NewJAgenC se hace visible. Servicios: " + serviciosSeleccionados.size());
        }
        super.setVisible(visible);
    }

    private void debugComponentesCalendario() {
        System.out.println("=== DEBUG CALENDARIOS ===");
        System.out.println("jCalendar1 es null? " + (jCalendar1 == null));

        if (jCalendar1 != null) {
            System.out.println("Fecha en jCalendar1: " + jCalendar1.getDate());
        }
        System.out.println("=== FIN DEBUG ===");
    }

    public NewJAgenC(ImageIcon imagen, String descripcion, String precio) {
        initComponents();
        this.idUsuario = SesionUsuario.getIdUsuario();

        // Empezar con lista vacía
        serviciosSeleccionados = new java.util.ArrayList<>();

        // Inicializar normalmente
        init();

        // Luego agregar el servicio del catálogo
        String fechaActual = jTextFieldFecha1.getText().trim();
        if (fechaActual.isEmpty()) {
            fechaActual = obtenerFechaActual();
        }

        String horaActual = (String) cbHora.getSelectedItem();
        if (horaActual == null) {
            horaActual = "10:00";
        }

        Object[] nuevoServicio = new Object[5];
        nuevoServicio[0] = imagen;
        nuevoServicio[1] = descripcion;
        nuevoServicio[2] = precio;
        nuevoServicio[3] = fechaActual;
        nuevoServicio[4] = horaActual;

        serviciosSeleccionados.add(nuevoServicio);

        // Mostrar
        if (!serviciosSeleccionados.isEmpty()) {
            this.indiceActual = this.serviciosSeleccionados.size() - 1;
            mostrarServiciosSeleccionados();
            iniciarCarrusel();
        }

        // Guardar en sesión
        SesionUsuario.setServiciosCita(serviciosSeleccionados);

        System.out.println("DEBUG - Nueva cita con servicio desde catálogo");
    }

    public void actualizarDesdeCatalogo(ImageIcon imagen, String descripcion, String precio) {
        System.out.println("DEBUG - Actualizando desde catálogo");

        // VERIFICAR si el servicio YA EXISTE en la lista actual
        boolean servicioExiste = false;
        for (Object[] servicio : serviciosSeleccionados) {
            String descripcionExistente = (String) servicio[1];
            if (descripcionExistente.equals(descripcion)) {
                servicioExiste = true;

                int respuesta = JOptionPane.showConfirmDialog(this,
                        "Este servicio ya está en tu lista de citas.\n"
                        + "¿Deseas agregarlo como un servicio adicional?",
                        "Servicio duplicado",
                        JOptionPane.YES_NO_OPTION);

                if (respuesta == JOptionPane.NO_OPTION) {
                    return;
                }
                break;
            }
        }
        // Agregar nuevo servicio
        String fechaActual = jTextFieldFecha1.getText().trim();
        if (fechaActual.isEmpty()) {
            fechaActual = obtenerFechaActual();
        }

        String horaActual = (String) cbHora.getSelectedItem();
        if (horaActual == null) {
            horaActual = "10:00";
        }

        Object[] nuevoServicio = new Object[5];
        nuevoServicio[0] = imagen;
        nuevoServicio[1] = descripcion;
        nuevoServicio[2] = precio;
        nuevoServicio[3] = fechaActual;
        nuevoServicio[4] = horaActual;

        // Verificar duplicado
        boolean existe = false;
        for (Object[] servicio : serviciosSeleccionados) {
            if (((String) servicio[1]).equals(descripcion)) {
                existe = true;
                break;
            }
        }

        if (!existe) {
            serviciosSeleccionados.add(nuevoServicio);
            System.out.println("DEBUG - Servicio agregado: " + descripcion);
        }

        // Actualizar interfaz
        if (!serviciosSeleccionados.isEmpty()) {
            indiceActual = serviciosSeleccionados.size() - 1;
            mostrarServiciosSeleccionados();
            iniciarCarrusel();
        }

        // Guardar en sesión
        SesionUsuario.setServiciosCita(serviciosSeleccionados);

        System.out.println("DEBUG - Servicios totales DESPUÉS: " + serviciosSeleccionados.size());
        debugServiciosActuales();

        // Restaurar ventana
        this.setState(java.awt.Frame.NORMAL);
        this.toFront();
    }

    private void limpiarDespuesDeGuardar() {
        // Limpiar lista local
        serviciosSeleccionados.clear();
        indiceActual = 0;

        // Limpiar sesión SOLO si las citas se guardaron exitosamente
        // Esto debe llamarse DESPUÉS de que NewJCitaConf confirme el pago
        // Limpiar interfaz
        jLabel4.setIcon(null);
        jLabel5.setText("No hay servicios seleccionados");
        jLabel8.setText("$0");

        // Detener timer
        if (timer != null) {
            timer.stop();
            timer = null;
        }

        // Resetear calendario
        java.util.Calendar hoy = java.util.Calendar.getInstance();
        jCalendar1.setCalendar(hoy);

        // Resetear hora
        cbHora.setSelectedItem("10:00");

        System.out.println("DEBUG - Estado limpiado después de guardar");
    }

    private void verificarYGuardarServicios() {
        System.out.println("=== VERIFICANDO SERVICIOS DUPLICADOS ===");

        // Crear una lista sin duplicados
        java.util.List<Object[]> serviciosUnicos = new java.util.ArrayList<>();
        java.util.Set<String> descripcionesVistas = new java.util.HashSet<>();

        for (Object[] servicio : serviciosSeleccionados) {
            String descripcion = (String) servicio[1];

            if (!descripcionesVistas.contains(descripcion)) {
                descripcionesVistas.add(descripcion);
                serviciosUnicos.add(servicio);
                System.out.println("Servicio único agregado: " + descripcion);
            } else {
                System.out.println("Servicio duplicado omitido: " + descripcion);
            }
        }

        // Actualizar la lista
        serviciosSeleccionados = serviciosUnicos;
        System.out.println("Servicios después de eliminar duplicados: " + serviciosSeleccionados.size());
    }

    private boolean horaBloqueadaParaServicioGeneral(String fecha, String hora, String descripcionServicio) {
        // Este método SOLO verifica bloqueos generales, NO las citas del usuario actual

        java.sql.Time horaTime = java.sql.Time.valueOf(hora + ":00");

        // 1. Verificar en bloqueo_horario (rangos de tiempo)
        String sqlBloqueo = "SELECT COUNT(*) FROM bloqueo_horario WHERE Fecha = ? "
                + "AND ? BETWEEN Hora_inicio AND Hora_fin";

        // 2. Verificar si hay otras citas (de otros usuarios) para este servicio
        String sqlCitaOtros = """
        SELECT COUNT(*) FROM cita c 
        JOIN cita_has_servicios chs ON c.idCita = chs.idCita 
        JOIN servicios s ON chs.idServicios = s.idServicios 
        WHERE c.Fecha = ? AND c.Hora = ? 
        AND c.Estado IN ('confirmada', 'reservada')
        AND s.Nombre_servicio LIKE ?
        AND c.idUsuarios != ?  -- Excluir al usuario actual
    """;

        try (java.sql.Connection conn = conexion.getConexion(); java.sql.PreparedStatement psBloqueo = conn.prepareStatement(sqlBloqueo); java.sql.PreparedStatement psCitaOtros = conn.prepareStatement(sqlCitaOtros)) {

            // Verificar en bloqueo_horario
            psBloqueo.setString(1, fecha);
            psBloqueo.setTime(2, horaTime);
            try (java.sql.ResultSet rsBloqueo = psBloqueo.executeQuery()) {
                if (rsBloqueo.next() && rsBloqueo.getInt(1) > 0) {
                    System.out.println("Hora bloqueada por horario general: " + fecha + " " + hora);
                    return true;
                }
            }

            // Verificar citas de otros usuarios
            int idUsuarioActual = SesionUsuario.getIdUsuario();
            int idServicio = obtenerIdServicioPorDescripcion(descripcionServicio);

            if (idServicio > 0) {
                psCitaOtros.setString(1, fecha);
                psCitaOtros.setTime(2, horaTime);
                psCitaOtros.setString(3, "%" + descripcionServicio + "%");
                psCitaOtros.setInt(4, idUsuarioActual);

                try (java.sql.ResultSet rsCita = psCitaOtros.executeQuery()) {
                    if (rsCita.next() && rsCita.getInt(1) > 0) {
                        System.out.println("Hora ocupada por otro usuario para: " + descripcionServicio);
                        return true;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private void limpiarTodoAlInicio() {
        System.out.println("=== LIMPIANDO TODO AL INICIO ===");

        // Limpiar lista local
        serviciosSeleccionados.clear();
        indiceActual = 0;

        // Limpiar sesión (por si acaso)
        SesionUsuario.limpiarServiciosCita();
        SesionUsuario.setMontoTotalCita(0);

        // Limpiar interfaz
        jLabel4.setIcon(null);
        jLabel5.setText("No hay servicios seleccionados");
        jLabel8.setText("$0");

        // Resetear controles
        java.util.Calendar hoy = java.util.Calendar.getInstance();
        jCalendar1.setCalendar(hoy);
        cbHora.setSelectedItem("10:00");

        // Detener timer
        if (timer != null) {
            timer.stop();
            timer = null;
        }

        System.out.println("Estado completamente limpiado");
    }

    private void cambiarVentana(JFrame nuevaVentana) {
    if (nuevaVentana == null) return;
    
    // Configurar el cierre de la nueva ventana
    nuevaVentana.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    
    // Añadir listener para cuando se cierre la nueva ventana
    nuevaVentana.addWindowListener(new java.awt.event.WindowAdapter() {
        @Override
        public void windowClosed(java.awt.event.WindowEvent e) {
            System.out.println("Nueva ventana cerrada");
        }
        
        @Override
        public void windowOpened(java.awt.event.WindowEvent e) {
            // Cuando la nueva ventana se abre, cerrar esta
            javax.swing.SwingUtilities.invokeLater(() -> {
                dispose();
            });
        }
    });
    
    // Mostrar la nueva ventana
    nuevaVentana.setVisible(true);
    nuevaVentana.setLocationRelativeTo(null);
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
        Jlabel = new java.awt.Label();
        jLabel4 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        btnAnterior = new javax.swing.JButton();
        btnSiguiente = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        cmbServicios = new javax.swing.JComboBox<>();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jTextFieldFecha1 = new javax.swing.JTextField();
        cbHora = new javax.swing.JComboBox<>();
        jPanel5 = new javax.swing.JPanel();
        INS = new javax.swing.JLabel();
        FACE = new javax.swing.JLabel();
        WPP = new javax.swing.JLabel();
        btnRegresar = new javax.swing.JButton();
        jCalendar1 = new com.toedter.calendar.JCalendar();
        jLabel3 = new javax.swing.JLabel();
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

        Jlabel.setText("Descripció");

        jLabel4.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        jLabel8.setText("$500");
        jLabel8.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));

        jLabel5.setText("jLabel5");

        btnAnterior.setText("jButton1");
        btnAnterior.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAnteriorActionPerformed(evt);
            }
        });

        btnSiguiente.setText("jButton1");
        btnSiguiente.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSiguienteActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panel6Layout = new javax.swing.GroupLayout(panel6);
        panel6.setLayout(panel6Layout);
        panel6Layout.setHorizontalGroup(
            panel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(Jlabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(panel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(label7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnAnterior))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(panel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnSiguiente))
                .addGap(42, 42, 42))
            .addGroup(panel6Layout.createSequentialGroup()
                .addGap(0, 36, Short.MAX_VALUE)
                .addGroup(panel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 207, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 188, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(24, Short.MAX_VALUE))
        );
        panel6Layout.setVerticalGroup(
            panel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panel6Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 214, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(33, 33, 33)
                .addGroup(panel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(panel6Layout.createSequentialGroup()
                        .addComponent(Jlabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 101, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(label7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jLabel8))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 23, Short.MAX_VALUE)
                .addGroup(panel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnAnterior)
                    .addComponent(btnSiguiente))
                .addContainerGap())
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

        jLabel3.setText("Agregar otro servicio");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(71, 71, 71)
                .addComponent(panel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(50, 50, 50)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(30, 30, 30)
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(cbHora, javax.swing.GroupLayout.PREFERRED_SIZE, 102, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jTextFieldFecha1, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 136, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(50, 50, 50)
                                .addComponent(cmbServicios, javax.swing.GroupLayout.PREFERRED_SIZE, 113, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(42, 42, 42)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(btnRegresar, javax.swing.GroupLayout.PREFERRED_SIZE, 172, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(68, 68, 68)
                                .addComponent(jButton4, javax.swing.GroupLayout.PREFERRED_SIZE, 172, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jCalendar1, javax.swing.GroupLayout.PREFERRED_SIZE, 345, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(207, Short.MAX_VALUE))
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
                        .addGap(24, 24, 24)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(jTextFieldFecha1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(32, 32, 32)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel2)
                            .addComponent(cbHora, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(30, 30, 30)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(cmbServicios, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel3))
                        .addGap(35, 35, 35)
                        .addComponent(jCalendar1, javax.swing.GroupLayout.PREFERRED_SIZE, 207, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnRegresar)
                            .addComponent(jButton4))
                        .addGap(33, 33, 33))
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
            .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButton4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton4ActionPerformed
        // Verificar que todos los servicios tengan fecha/hora
        verificarYGuardarServicios();

        System.out.println("=== INICIANDO CONFIRMACIÓN DE CITA ===");
        System.out.println("Servicios a confirmar: " + serviciosSeleccionados.size());

        // Verificar que hay servicios
        if (serviciosSeleccionados.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No hay servicios seleccionados.\nPor favor agrega al menos un servicio.",
                    "Sin servicios",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Verificar que todos los servicios tengan fecha/hora
        boolean todosCompletos = true;
        StringBuilder errores = new StringBuilder();

        for (Object[] servicio : serviciosSeleccionados) {
            String fecha = servicio.length > 3 ? (String) servicio[3] : "";
            String hora = servicio.length > 4 ? (String) servicio[4] : "";
            String descripcion = (String) servicio[1];

            if (fecha.isEmpty() || hora.isEmpty()) {
                todosCompletos = false;
                errores.append("• ").append(descripcion).append(": Sin fecha/hora asignada\n");
            }
        }

        if (!todosCompletos) {
            JOptionPane.showMessageDialog(this,
                    "Los siguientes servicios no tienen fecha/hora asignada:\n\n"
                    + errores.toString() + "\n"
                    + "Por favor navega con el carrusel y asigna fecha/hora a cada servicio.",
                    "Fechas/Horas incompletas",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Verificar disponibilidad (SOLO para citas YA GUARDADAS, no las actuales)
        for (Object[] servicio : serviciosSeleccionados) {
            String fecha = (String) servicio[3];
            String hora = (String) servicio[4];
            String descripcion = (String) servicio[1];

            // Solo verificar si YA existe una cita para este servicio
            if (citaYaGuardada(fecha, hora, descripcion)) {
                int respuesta = JOptionPane.showConfirmDialog(this,
                        "YA TIENES UNA CITA PARA ESTE SERVICIO:\n\n"
                        + "Servicio: " + descripcion + "\n"
                        + "Fecha: " + fecha + "\n"
                        + "Hora: " + hora + "\n\n"
                        + "¿Deseas agendar otra cita para el mismo servicio?",
                        "Cita duplicada",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);

                if (respuesta == JOptionPane.NO_OPTION) {
                    return;
                }
            }

            // Verificar disponibilidad general (bloqueos)
            if (horaBloqueadaParaServicioGeneral(fecha, hora, descripcion)) {
                JOptionPane.showMessageDialog(this,
                        "HORA NO DISPONIBLE\n\n"
                        + "La hora " + hora + " no está disponible para:\n"
                        + descripcion + "\n\n"
                        + "Motivo: La hora está bloqueada o ocupada por otra cita.",
                        "Hora no disponible",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // DEBUG: Mostrar lo que se va a guardar
        System.out.println("=== CONFIRMANDO Y GUARDANDO ===");
        for (int i = 0; i < serviciosSeleccionados.size(); i++) {
            Object[] servicio = serviciosSeleccionados.get(i);
            System.out.println("Cita " + i + ": " + servicio[1]
                    + " | Fecha: " + servicio[3]
                    + " | Hora: " + servicio[4]
                    + " | Precio: " + servicio[2]);
        }

        // Guardar en sesión TEMPORALMENTE
        SesionUsuario.setServiciosCita(serviciosSeleccionados);

        double montoTotal = calcularMontoTotal(serviciosSeleccionados);
        SesionUsuario.setMontoTotalCita(montoTotal);

        System.out.println("Monto total: $" + montoTotal);
        System.out.println("Servicios guardados en sesión: " + SesionUsuario.getServiciosCita().size());

        // Crear ventana de confirmación
        NewJCitaConf confirmacionWindow = new NewJCitaConf();
        confirmacionWindow.setVisible(true);

        // IMPORTANTE: NO limpiar aquí todavía - se limpiará después del pago
        // this.dispose(); // Opcional: puedes cerrar o minimizar
        System.out.println("=== CONFIRMACIÓN COMPLETADA ===");
                this.dispose();


    }//GEN-LAST:event_jButton4ActionPerformed

    private void cmbServiciosActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbServiciosActionPerformed
        String servicioSeleccionado = cmbServicios.getSelectedItem().toString();

        if ("Seleccione un servicio".equals(servicioSeleccionado)) {
            return;
        }

        System.out.println("DEBUG - Guardando estado antes de abrir catálogo");

        // GUARDAR TODOS los servicios actuales ANTES de abrir catálogo
        // Actualizar fecha/hora del servicio actual
        if (!serviciosSeleccionados.isEmpty()) {
            actualizarFechaHoraServicioActual();
        }

        // Guardar en sesión
        SesionUsuario.setServiciosCita(serviciosSeleccionados);
        System.out.println("Servicios guardados en sesión: " + serviciosSeleccionados.size());

        System.out.println("DEBUG - Abriendo catálogo para: " + servicioSeleccionado);

        try {
            // Crear catálogo pero NO cerrar esta ventana todavía
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
                    NewJCatalogoGenerico catalogoDefault = new NewJCatalogoGenerico(conexionDefault);
                    catalogoDefault.setVisible(true);
                    break;
            }

            // SOLO minimizar esta ventana en lugar de ocultarla
            this.setState(java.awt.Frame.ICONIFIED);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error al abrir el catálogo: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }

        cmbServicios.setSelectedIndex(0);
    }//GEN-LAST:event_cmbServiciosActionPerformed

    private void cbHoraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cbHoraActionPerformed
        String fecha = jTextFieldFecha1.getText().trim();
        String hora = (String) cbHora.getSelectedItem();

        if (!fecha.isEmpty() && hora != null) {
            System.out.println("Cambio de hora detectado: " + fecha + " " + hora);

            // Solo verificar si hay servicios seleccionados
            if (!serviciosSeleccionados.isEmpty() && indiceActual < serviciosSeleccionados.size()) {
                Object[] servicioActual = serviciosSeleccionados.get(indiceActual);
                String descripcionServicioActual = (String) servicioActual[1];

                System.out.println("Servicio actual: " + descripcionServicioActual);

                // Verificar disponibilidad general (NO verificar citas propias)
                if (horaBloqueadaParaServicioGeneral(fecha, hora, descripcionServicioActual)) {
                    JOptionPane.showMessageDialog(this,
                            "HORA NO DISPONIBLE\n\n"
                            + "La hora " + hora + " no está disponible para:\n"
                            + descripcionServicioActual + "\n\n"
                            + "Por favor selecciona otra hora.",
                            "Hora no disponible",
                            JOptionPane.WARNING_MESSAGE);

                    // Restaurar hora anterior
                    String horaAnterior = servicioActual.length > 4 ? (String) servicioActual[4] : "10:00";
                    cbHora.setSelectedItem(horaAnterior);
                    return;
                }

                // Actualizar fecha/hora del servicio actual
                actualizarFechaHoraServicioActual();

                System.out.println("Hora actualizada para servicio: " + descripcionServicioActual);
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
        System.out.println("DEBUG - Regresando sin guardar en sesión");

        try {
            if (SesionUsuario.sesionActiva() && SesionUsuario.getIdUsuario() > 0) {
                NewJMiscitasCi misCitas = new NewJMiscitasCi();
                misCitas.setVisible(true);
            } else {
                Inicio inicio = new Inicio();
                inicio.setVisible(true);
            }

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

    private void btnAnteriorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAnteriorActionPerformed
        // TODO add your handling code here:
        // Detener el timer automático si está activo
        if (timer != null && timer.isRunning()) {
            timer.stop();
        }

        // Ir al servicio anterior
        servicioAnterior();

        // Si quieres que el timer se reinicie después de un tiempo
        if (serviciosSeleccionados.size() > 1) {
            // Opcional: reiniciar timer después de 10 segundos
            Timer restartTimer = new Timer(10000, e -> {
                if (timer != null) {
                    timer.start();
                }
                ((Timer) e.getSource()).stop();
            });
            restartTimer.setRepeats(false);
            restartTimer.start();
        }
    }//GEN-LAST:event_btnAnteriorActionPerformed

    private void btnSiguienteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSiguienteActionPerformed
        // TODO add your handling code here:
        if (timer != null && timer.isRunning()) {
            timer.stop();
        }

        // Ir al siguiente servicio
        siguienteServicio();

        // Si quieres que el timer se reinicie después de un tiempo
        if (serviciosSeleccionados.size() > 1) {
            // Opcional: reiniciar timer después de 10 segundos
            Timer restartTimer = new Timer(10000, e -> {
                if (timer != null) {
                    timer.start();
                }
                ((Timer) e.getSource()).stop();
            });
            restartTimer.setRepeats(false);
            restartTimer.start();
        }
    }//GEN-LAST:event_btnSiguienteActionPerformed

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
    private java.awt.Label Jlabel;
    private javax.swing.JLabel WPP;
    private javax.swing.JButton btnAnterior;
    private javax.swing.JButton btnRegresar;
    private javax.swing.JButton btnSiguiente;
    private javax.swing.JComboBox<String> cbHora;
    private javax.swing.JComboBox<String> cmbServicios;
    private javax.swing.JButton jButton4;
    private com.toedter.calendar.JCalendar jCalendar1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
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
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JTextField jTextFieldFecha1;
    private java.awt.Label label5;
    private java.awt.Label label7;
    private java.awt.Panel panel6;
    // End of variables declaration//GEN-END:variables
}
