package Interfaces;

import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.util.Properties;

public class RecordatorioEmail {

    public static void enviarRecordatorio(String destinatario, String fechaCita, String horaCita) {
        String remitente = "AndyNailss@outlook.com";
        String contrasena = "andynails123";
        String asunto = "Recordatorio de tu cita - Andy Nails";
        String cuerpo = "Hola 💅,\n\nTe recordamos que tienes una cita el " + fechaCita +
                        " a las " + horaCita + ".\n\n¡Te esperamos!\n\nAtte: Andy Nails 💖";

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.office365.com");
        props.put("mail.smtp.port", "587");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(remitente, contrasena);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(remitente));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
            message.setSubject(asunto);
            message.setText(cuerpo);

            Transport.send(message);
            System.out.println("Recordatorio enviado a " + destinatario);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
