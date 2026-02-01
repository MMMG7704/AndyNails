/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package andynails;


import andynails.ConexionBD;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;

public class DisponibilidadManager {
 ConexionBD conexion;

    public DisponibilidadManager(ConexionBD conexion) {
        this.conexion = conexion;
    }

    // Obtiene servicios o roles disponibles en la BD
    public Map<Integer, String> obtenerServicios() {
        Map<Integer, String> servicios = new HashMap<>();
        String sql = "SELECT idServicios, Nombre FROM Servicios";

        try (Connection con = conexion.conectar();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                servicios.put(rs.getInt("idServicios"), rs.getString("Nombre"));
            }

        } catch (SQLException e) {
            System.err.println("Error al cargar servicios: " + e.getMessage());
        }
        return servicios;
    }

    //Verifica si un servicio está disponible en una fecha y hora
    public boolean estaDisponible(int idServicio, Date fecha, String hora) {
        String sql = "SELECT COUNT(*) FROM Cita c " +
                     "INNER JOIN Cita_has_Servicios cs ON c.idCita = cs.idCita " +
                     "WHERE c.Fecha = ? AND c.Hora = ? AND cs.idServicios = ?";
        try (Connection con = conexion.conectar();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, new java.sql.Date(fecha.getTime()));
            ps.setString(2, hora);
            ps.setInt(3, idServicio);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int count = rs.getInt(1);
                return count == 0; // disponible si no hay cita
            }

        } catch (SQLException e) {
            System.err.println("Error al verificar disponibilidad: " + e.getMessage());
        }
        return false;
    }
}


