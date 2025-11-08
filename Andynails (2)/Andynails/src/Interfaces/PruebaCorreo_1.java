package Interfaces;

public class PruebaCorreo_1 {
    public static void main(String[] args) {
        // 📧 Cambia estos datos por los reales
        String destinatario = "gbetancourt122@gmail"; // el correo al que quieres enviar la prueba
        String fecha = "25/10/2025";
        String hora = "3:00 PM";

        // Llama al método de tu clase RecordatorioEmail
        RecordatorioEmail.enviarRecordatorio(destinatario, fecha, hora);
    }
}
