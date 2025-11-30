package andynails;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class SessionManager {
    private static String tipoUsuario = null;
    
    // Método para iniciar sesión que usa tu SesionUsuario existente
    public static void iniciarSesion(int id, String usuario, String tipo) {
        SesionUsuario.iniciarSesion(id, usuario);
        tipoUsuario = tipo;
        System.out.println("SessionManager - Sesión iniciada: " + usuario + " (" + tipo + ")");
    }
    
    // Método para cerrar sesión con confirmación gráfica
   public static void cerrarSesion(JFrame ventanaActual) {
    // Crear opciones personalizadas en español
    Object[] opciones = {"Sí", "No"};
    
    int confirmacion = JOptionPane.showOptionDialog(
        ventanaActual,
        "¿Estás seguro de que deseas cerrar sesión?",
        "Confirmar cierre de sesión",
        JOptionPane.YES_NO_OPTION,
        JOptionPane.QUESTION_MESSAGE,
        null,
        opciones,  // Botones personalizados en español
        opciones[1] // Opción por defecto ("No")
    );
    
    if (confirmacion == JOptionPane.YES_OPTION) {
        // Limpiar todos los datos usando tu método existente
        SesionUsuario.cerrarSesion();
        SesionUsuario.limpiarDatosCita();
        tipoUsuario = null;
        
        // Cerrar ventana actual
        ventanaActual.dispose();
        
        // Abrir ventana de login
        Interfaces.NewJLogin login = new Interfaces.NewJLogin();
        login.setVisible(true);
        
        JOptionPane.showMessageDialog(null, "Sesión cerrada correctamente");
    }
}
    
    // Getters que combinan ambas clases
    public static int getIdUsuario() {
        return SesionUsuario.getIdUsuario();
    }
    
    public static String getUsuarioLogueado() {
        return SesionUsuario.getNombreUsuario();
    }
    
    public static String getTipoUsuario() {
        return tipoUsuario;
    }
    
    public static boolean haySesionActiva() {
        return SesionUsuario.sesionActiva();
    }
    
    // Métodos para acceder a los datos de pago y cita de SesionUsuario
    public static void setIdPagoActual(int idPago) {
        SesionUsuario.setIdPagoActual(idPago);
    }
    
    public static int getIdPagoActual() {
        return SesionUsuario.getIdPagoActual();
    }
    
    public static void setMontoTotalCita(double monto) {
        SesionUsuario.setMontoTotalCita(monto);
    }
    
    public static double getMontoTotalCita() {
        return SesionUsuario.getMontoTotalCita();
    }
    
    public static void setFechaCita(String fecha) {
        SesionUsuario.setFechaCita(fecha);
    }
    
    public static String getFechaCita() {
        return SesionUsuario.getFechaCita();
    }
    
    public static void setHoraCita(String hora) {
        SesionUsuario.setHoraCita(hora);
    }
    
    public static String getHoraCita() {
        return SesionUsuario.getHoraCita();
    }
    
    public static void setServiciosCita(java.util.List<Object[]> servicios) {
        SesionUsuario.setServiciosCita(servicios);
    }
    
    public static java.util.List<Object[]> getServiciosCita() {
        return SesionUsuario.getServiciosCita();
    }
    
    public static void limpiarDatosCita() {
        SesionUsuario.limpiarDatosCita();
    }
}