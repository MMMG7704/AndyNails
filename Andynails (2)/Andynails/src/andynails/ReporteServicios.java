package andynails;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.awt.Desktop;

public class ReporteServicios {

    public static void generarPDF(String rutaSalida) {
        String sql = "SELECT idServicios, Nombre_servicio, Descripcion, Precio FROM servicios";

        try (Connection con = ConexionBD.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            // Crear documento PDF
            Document doc = new Document();
            PdfWriter.getInstance(doc, new FileOutputStream(rutaSalida));
            doc.open();

            // Título del reporte
            Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            doc.add(new Paragraph("Reporte de Servicios", tituloFont));
            doc.add(new Paragraph(" ")); // salto de línea

            // Crear tabla PDF
            PdfPTable tabla = new PdfPTable(4);
            tabla.setWidthPercentage(100);
            tabla.addCell("ID");
            tabla.addCell("Nombre del Servicio");
            tabla.addCell("Descripción");
            tabla.addCell("Precio");

            // Llenar la tabla con datos desde la BD
            while (rs.next()) {
                tabla.addCell(String.valueOf(rs.getInt("idServicios")));
                tabla.addCell(rs.getString("Nombre_servicio"));
                tabla.addCell(rs.getString("Descripcion"));
                tabla.addCell("$ " + rs.getString("Precio"));
            }

            doc.add(tabla);
            doc.close(); // cerrar documento

            // Verificar si se creó el archivo
            File pdf = new File(rutaSalida);
            if (pdf.exists()) {
                System.out.println("✅ El archivo PDF se creó correctamente en: " + rutaSalida);
                
                // Abrir automáticamente el PDF (solo Windows)
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(pdf);
                }

            } else {
                System.out.println("❌ No se encontró el archivo PDF. Revisa la ruta.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Método main para probar
    public static void main(String[] args) {
        String ruta = "C:\\Users\\mgmmo\\Documents\\7SEMESTRE\\INGENIERIASOF\\Reportes\\Servicios.pdf";
        generarPDF(ruta);
    }
}
