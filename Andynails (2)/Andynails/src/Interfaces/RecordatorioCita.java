package Interfaces;

import andynails.ConexionBD;
import java.sql.*;
import java.time.LocalDate;
import java.util.function.Predicate;

public class RecordatorioCita {

    public static void enviarRecordatorios(Predicate<String> permitirEnvio) {

        try (Connection con = ConexionBD.getConnection()) {

            LocalDate manana = LocalDate.now().plusDays(1);

            // 👇 SOLO ENVÍA A CITAS QUE AÚN NO HAN SIDO NOTIFICADAS
            String sql = "SELECT c.idCita, u.Nombre, u.Paterno, u.Materno, u.Correo, c.Fecha, c.Hora " +
                    "FROM cita c " +
                    "JOIN usuarios u ON c.idUsuarios = u.idUsuarios " +
                    "WHERE c.Fecha = ? AND c.Estado = 'Confirmada'";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setDate(1, Date.valueOf(manana));

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                String idCita = rs.getString("idCita");

                // Validar si se permite el envío (siempre true en esta versión)
                if (!permitirEnvio.test(idCita)) {
                    continue;
                }

                String nombre = rs.getString("Nombre");
                String paterno = rs.getString("Paterno");
                String materno = rs.getString("Materno");
                String correo = rs.getString("Correo");
                String fecha = rs.getDate("Fecha").toString();
                String hora = rs.getTime("Hora").toString();

                String nombreCompleto = nombre + " " + paterno + " " + materno;

                System.out.println("📨 Enviando recordatorio a: " + nombreCompleto + " (" + correo + ")");

                // 👉 Enviar email
                RecordatorioEmail_1.enviarRecordatorio(correo, nombreCompleto, fecha, hora);

                // 👉 MARCAR LA CITA COMO NOTIFICADA EN LA BD
                PreparedStatement upd = con.prepareStatement(
                        "UPDATE cita SET Estado = 'Notificado' WHERE idCita = ?"
                );
                upd.setString(1, idCita);
                upd.executeUpdate();
                upd.close();

                System.out.println("✔ Cita " + idCita + " marcada como Notificada.");
            }

            rs.close();
            ps.close();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
