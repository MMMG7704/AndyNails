package Interfaces;

import andynails.ConexionBD;

public class AndyNails {
    public static void main(String[] args) {
        // Crear la conexión (usa constructor por defecto)
        ConexionBD conexion = new ConexionBD();

        // Conectar a la base
        conexion.conectar();

        // Abrir la ventana principal
        Inicio ventana = new Inicio();
        ventana.setVisible(true);
    }
}
