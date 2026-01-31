package andynails;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ConexionBD {

    /**
     *
     * @return
     */
    // public static org.mariadb.jdbc.Connection getConnection() {
    //   throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    //}
    public static Connection getConnection() throws SQLException {
        String url = "jdbc:mariadb://localhost:3307/andynails?useSSL=false&allowPublicKeyRetrieval=true";
        String user = "root";
        String pwd = "mora";
        return DriverManager.getConnection(url, user, pwd);
    }

    private String bd;
    private String url;
    private String user;
    private String pwd;
    private Connection conexion;

    // Constructor por defecto: usa la base de datos "my_cargaacademica"
    public ConexionBD() {
        this("andynails");
    }

    // Constructor con parámetro para poder cambiar la base si se desea
    public ConexionBD(String bd) {
        this.bd = bd;
        this.url = "jdbc:mariadb://localhost:3307/" + bd + "?useSSL=false&allowPublicKeyRetrieval=true";
        this.user = "root"; // Usuario
        this.pwd = "mora";       // Contraseña de la base de datos
        this.conexion = null;
    }

    // Método para conectarse
    public Connection conectar() {
        try {
            // Cargar el driver de MariaDB
            Class.forName("org.mariadb.jdbc.Driver");

            // Establecer la conexión
            conexion = DriverManager.getConnection(url, user, pwd);
           // System.out.println("Conexión exitosa a la base de datos " + bd);
        } catch (ClassNotFoundException e) {
            System.out.println("Error al cargar el driver: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("Error de conexión: " + e.getMessage());
        }
        return conexion;
    }

    // Método para cerrar la conexión
    public void cerrar() {
        if (conexion != null) {
            try {
                conexion.close();
                System.out.println("Conexión cerrada.");
            } catch (SQLException e) {
                System.out.println("Error al cerrar la conexión: " + e.getMessage());
            }
        }
    }

    // Getter para obtener la conexión
    public Connection getConexion() throws SQLException {
        return DriverManager.getConnection(url, user, pwd);
    }

    public ResultSet consulta(String sql) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}
