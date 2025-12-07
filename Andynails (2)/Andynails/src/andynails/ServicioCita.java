/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package andynails;

import javax.swing.ImageIcon;

/**
 *
 * @author mgmmo
 */
// En NewJAgenC.java, agrega esta clase interna
class ServicioCita {
    ImageIcon imagen;
    String descripcion;
    String precio;
    String fecha;  // Fecha específica para ESTE servicio
    String hora;   // Hora específica para ESTE servicio
    
    ServicioCita(ImageIcon imagen, String descripcion, String precio, String fecha, String hora) {
        this.imagen = imagen;
        this.descripcion = descripcion;
        this.precio = precio;
        this.fecha = fecha;
        this.hora = hora;
    }
    
    // Getters
    public ImageIcon getImagen() { return imagen; }
    public String getDescripcion() { return descripcion; }
    public String getPrecio() { return precio; }
    public String getFecha() { return fecha; }
    public String getHora() { return hora; }
    
    // Setters para fecha/hora
    public void setFecha(String fecha) { this.fecha = fecha; }
    public void setHora(String hora) { this.hora = hora; }
}