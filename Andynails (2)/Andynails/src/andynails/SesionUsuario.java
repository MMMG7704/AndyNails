package andynails;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author mgmmo
 */
public class SesionUsuario {

    private static int idUsuario = 0;
    private static String nombreUsuario = null;
    private static int idPagoActual;
    private static double montoTotalCita;
    private static String fechaCita;
    private static String horaCita;
    private static List<Object[]> serviciosCita = new ArrayList<>();

    // NUEVOS CAMPOS PARA MANEJAR CITAS RECIÉN CREADAS
    private static boolean citaRecienCreada = false;
    private static List<Integer> idsCitasCreadas = new ArrayList<>();
    private static List<Object[]> serviciosCitaRecienCreada = new ArrayList<>();
    private static String fechaCitaRecienCreada = "";
    private static String horaCitaRecienCreada = "";
    private static Map<String, List<Object[]>> citasAgrupadas = new HashMap<>();

    // MÉTODOS EXISTENTES (mantener todos los que ya tienes)...

    // === NUEVOS MÉTODOS PARA MANEJAR CITAS RECIÉN CREADAS ===
    
    public static void setCitaRecienCreada(boolean estado) {
        citaRecienCreada = estado;
        System.out.println("DEBUG - Cita recién creada: " + estado);
    }
    
    public static boolean isCitaRecienCreada() {
        return citaRecienCreada;
    }
    
    public static void agregarIdCitaCreada(int idCita) {
        if (idsCitasCreadas == null) {
            idsCitasCreadas = new ArrayList<>();
        }
        idsCitasCreadas.add(idCita);
        System.out.println("DEBUG - ID de cita agregado: " + idCita);
    }
    
    public static List<Integer> getIdsCitasCreadas() {
        if (idsCitasCreadas == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(idsCitasCreadas);
    }
    
    public static void guardarDatosCitaRecienCreada() {
        // Guardar copia de los datos de la cita actual
        if (serviciosCita != null && !serviciosCita.isEmpty()) {
            serviciosCitaRecienCreada = new ArrayList<>();
            for (Object[] servicio : serviciosCita) {
                Object[] copia = new Object[servicio.length];
                System.arraycopy(servicio, 0, copia, 0, servicio.length);
                serviciosCitaRecienCreada.add(copia);
            }
            fechaCitaRecienCreada = fechaCita != null ? fechaCita : "";
            horaCitaRecienCreada = horaCita != null ? horaCita : "";
            
            System.out.println("DEBUG - Datos de cita guardados para mostrar detalles");
            System.out.println("  Servicios: " + serviciosCitaRecienCreada.size());
            System.out.println("  Fecha: " + fechaCitaRecienCreada);
            System.out.println("  Hora: " + horaCitaRecienCreada);
        }
    }
    
    public static List<Object[]> getServiciosCitaRecienCreada() {
        if (serviciosCitaRecienCreada == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(serviciosCitaRecienCreada);
    }
    
    public static String getFechaCitaRecienCreada() {
        return fechaCitaRecienCreada != null ? fechaCitaRecienCreada : "";
    }
    
    public static String getHoraCitaRecienCreada() {
        return horaCitaRecienCreada != null ? horaCitaRecienCreada : "";
    }
    
    public static void limpiarIdsCitasCreadas() {
        if (idsCitasCreadas != null) {
            idsCitasCreadas.clear();
        }
    }
    
    public static void limpiarDatosCitaRecienCreada() {
        if (serviciosCitaRecienCreada != null) {
            serviciosCitaRecienCreada.clear();
        }
        fechaCitaRecienCreada = "";
        horaCitaRecienCreada = "";
        citaRecienCreada = false;
        limpiarIdsCitasCreadas();
        System.out.println("DEBUG - Datos de cita recién creada limpiados");
    }
    
    // Modificar el método limpiarDatosCita existente para que no limpie todo
    public static void limpiarDatosCita() {
        // NO limpiar serviciosCita, fechaCita, horaCita aquí
        // Solo limpiar montoTotalCita
        montoTotalCita = 0.0;
        
        System.out.println("DEBUG - Datos parciales de cita limpiados (solo monto)");
    }
    
    // Nuevo método para limpiar todo cuando sea necesario
    public static void limpiarTodoDatosCita() {
        fechaCita = null;
        horaCita = null;
        montoTotalCita = 0.0;
        if (serviciosCita != null) {
            serviciosCita.clear();
        } else {
            serviciosCita = new ArrayList<>();
        }
        
        // También limpiar datos de cita recién creada
        limpiarDatosCitaRecienCreada();
        
        System.out.println("DEBUG - Todos los datos de cita limpiados");
    }
    
    // Método para obtener citas agrupadas (ya lo tienes, solo asegurar)
    public static Map<String, List<Object[]>> getCitasAgrupadas() {
        citasAgrupadas.clear();
        List<Object[]> servicios = getServiciosCita();

        if (servicios != null) {
            for (Object[] servicio : servicios) {
                String fecha = servicio.length > 3 ? (String) servicio[3] : "";
                String hora = servicio.length > 4 ? (String) servicio[4] : "";

                if (!fecha.isEmpty() && !hora.isEmpty()) {
                    String clave = fecha + "|" + hora;
                    if (!citasAgrupadas.containsKey(clave)) {
                        citasAgrupadas.put(clave, new ArrayList<>());
                    }
                    citasAgrupadas.get(clave).add(servicio);
                }
            }
        }
        return citasAgrupadas;
    }
    
    public static boolean datosCitaCompletos() {
        List<Object[]> servicios = getServiciosCita();
        if (servicios == null || servicios.isEmpty()) {
            return false;
        }

        for (Object[] servicio : servicios) {
            String fecha = servicio.length > 3 ? (String) servicio[3] : "";
            String hora = servicio.length > 4 ? (String) servicio[4] : "";

            if (fecha == null || fecha.trim().isEmpty()
                    || hora == null || hora.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }
    
    
    // Los demás métodos existentes se mantienen igual...
    public static void iniciarSesion(int id, String nombre) {
        idUsuario = id;
        nombreUsuario = nombre;
        System.out.println("Sesión iniciada - ID: " + id + ", Nombre: " + nombre);
    }

    public static int getIdUsuario() {
        return idUsuario;
    }

    public static String getNombreUsuario() {
        return nombreUsuario != null ? nombreUsuario : "Invitado";
    }

    public static void cerrarSesion() {
        System.out.println("Cerrando sesión - ID anterior: " + idUsuario);
        idUsuario = 0;
        nombreUsuario = null;
    }

    public static boolean sesionActiva() {
        return idUsuario != 0 && nombreUsuario != null;
    }

    public static void setIdPagoActual(int idPago) {
        idPagoActual = idPago;
    }

    public static int getIdPagoActual() {
        return idPagoActual;
    }

    public static void setMontoTotalCita(double monto) {
        montoTotalCita = monto;
    }

    public static double getMontoTotalCita() {
        return montoTotalCita;
    }

    public static void setFechaCita(String fecha) {
        fechaCita = fecha;
    }

    public static String getFechaCita() {
        return fechaCita;
    }

    public static void setHoraCita(String hora) {
        horaCita = hora;
    }

    public static String getHoraCita() {
        return horaCita;
    }

    public static void setServiciosCita(List<Object[]> servicios) {
        if (servicios == null) {
            serviciosCita = new ArrayList<>();
        } else {
            serviciosCita = new ArrayList<>(servicios);
        }
    }

    public static List<Object[]> getServiciosCita() {
        return serviciosCita;
    }
    
  public static void limpiarServiciosCita() {
    // Eliminar servicios de la sesión después de confirmar
    if (serviciosCita != null) {
        serviciosCita.clear();
        System.out.println("DEBUG - Servicios limpiados de sesión");
    }
  }
    
}