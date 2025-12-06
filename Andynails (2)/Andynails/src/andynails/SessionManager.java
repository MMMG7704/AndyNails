package andynails;

import Interfaces.RecordatorioCita; // IMPORTANTE: Agrega esta importación
import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class SessionManager {
    private static String tipoUsuario = null;
    
    // Método para iniciar sesión (sin cambios)
    public static void iniciarSesion(int id, String usuario, String tipo) {
        SesionUsuario.iniciarSesion(id, usuario);
        tipoUsuario = tipo;
        System.out.println("SessionManager - Sesión iniciada: " + usuario + " (" + tipo + ")");
    }
    
// MÉTODO MODIFICADO PARA CERRAR SESIÓN CON RECORDATORIO Y CIERRE COMPLETO
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
        opciones,
        opciones[1]
    );
    
    if (confirmacion == JOptionPane.YES_OPTION) {
        try {
            // ENVIAR RECORDATORIOS ANTES DE CERRAR SESIÓN
            System.out.println("Enviando recordatorios de citas...");
            RecordatorioCita.enviarRecordatoriosCierreSesion();
            System.out.println("Recordatorios enviados correctamente");
            
        } catch (Exception e) {
            System.err.println("Error al enviar recordatorios: " + e.getMessage());
        }
        
        // ===== NUEVO: Guardar referencia a la ventana de Login =====
        Interfaces.NewJLogin login = new Interfaces.NewJLogin();
        
        // ===== CERRAR TODAS LAS VENTANAS MENOS LA NUEVA DE LOGIN =====
        java.awt.Window[] windows = java.awt.Window.getWindows();
        for (java.awt.Window window : windows) {
            // No cerrar la nueva ventana de login que acabamos de crear
            if (window != login && window.isDisplayable()) {
                window.dispose();
            }
        }
        
        // Limpiar todos los datos usando tu método existente
        SesionUsuario.cerrarSesion();
        SesionUsuario.limpiarDatosCita();
        tipoUsuario = null;
        
        // Mostrar la nueva ventana de login
        login.setVisible(true);
        
        JOptionPane.showMessageDialog(login, "Sesión cerrada correctamente");
    }
}    
    // Los demás métodos permanecen igual...
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