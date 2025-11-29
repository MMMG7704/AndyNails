/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package andynails;

/**
 *
 * @author mgmmo
 */
public class SesionUsuario {
    private static int idUsuario = 0;  // Inicializar en 0
    private static String nombreUsuario = null;
        private static int idPagoActual; // 
            private static double montoTotalCita; // 
 private static String fechaCita;
    private static String horaCita;
    private static java.util.List<Object[]> serviciosCita = new java.util.ArrayList<>();


    public static void iniciarSesion(int id, String nombre) {
        idUsuario = id;
        nombreUsuario = nombre;
        System.out.println("Sesión iniciada - ID: " + id + ", Nombre: " + nombre);
    }

    
    public static int getIdUsuario() {
        return idUsuario;
    }

    public static String getNombreUsuario() {
       // return nombreUsuario;
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
    
    public static void setServiciosCita(java.util.List<Object[]> servicios) {
        serviciosCita = new java.util.ArrayList<>(servicios);
    }
    
    public static java.util.List<Object[]> getServiciosCita() {
        return serviciosCita;
    }
    
    
    // Limpiar datos de la cita después de usarlos
    public static void limpiarDatosCita() {
         fechaCita = null;
    horaCita = null;
    montoTotalCita = 0.0;
    if (serviciosCita != null) {
        serviciosCita.clear();
    } else {
        serviciosCita = new java.util.ArrayList<>();
    }
    System.out.println("DEBUG - Datos de cita limpiados en SesionUsuario");
    }

    }

