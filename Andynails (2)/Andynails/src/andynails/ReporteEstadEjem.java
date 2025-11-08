package andynails;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import java.awt.Color;

public class ReporteEstadEjem {

    public static void generarPDF() {
        ConexionBD conexion = new ConexionBD();
        Connection conn = conexion.conectar();

        String sql = "SELECT s.Nombre_servicio, COUNT(chs.idServicios) AS total "
                + "FROM cita_has_servicios chs "
                + "INNER JOIN servicios s ON chs.idServicios = s.idServicios "
                + "GROUP BY s.Nombre_servicio";

        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            DefaultPieDataset dataset = new DefaultPieDataset();
            Map<String, Integer> datos = new HashMap<>();

            boolean hayDatos = false;
            while (rs.next()) {
                hayDatos = true;
                String nombre = rs.getString("Nombre_servicio");
                int total = rs.getInt("total");
                dataset.setValue(nombre, total);
                datos.put(nombre, total);
            }

            // Si no hay datos reales, agregamos ejemplos
            if (!hayDatos) {
                datos.put("Maquillaje", 15);
                datos.put("Uñas", 25);
                datos.put("Peinado", 10);
                for (Map.Entry<String, Integer> entry : datos.entrySet()) {
                    dataset.setValue(entry.getKey(), entry.getValue());
                }
            }

            // Crear la gráfica
            JFreeChart chart = ChartFactory.createPieChart(
                    "Servicios más solicitados - AndyNails",
                    dataset,
                    true,
                    true,
                    false
            );

            // Eliminar fondo gris y bordes 👇
            chart.setBackgroundPaint(Color.WHITE); // fondo general blanco
            chart.getPlot().setBackgroundPaint(Color.WHITE); // fondo del área del gráfico
            chart.setBorderVisible(false); // sin borde
            chart.getPlot().setOutlineVisible(false); // sin contorno

            // Personalización de colores
            PiePlot plot = (PiePlot) chart.getPlot();
            plot.setSectionPaint("Maquillaje", new Color(255, 182, 193));
            plot.setSectionPaint("Uñas", new Color(255, 215, 0));
            plot.setSectionPaint("Peinado", new Color(173, 216, 230));
            plot.setShadowPaint(null); // sin sombra
            plot.setLabelBackgroundPaint(Color.WHITE);
            plot.setLabelOutlinePaint(null);
            plot.setLabelShadowPaint(null);

            // Guardar imagen
            String rutaImagen = "grafico_estadistico.png";
            ChartUtilities.saveChartAsPNG(new File(rutaImagen), chart, 600, 400);

            // Crear PDF
            String rutaPDF = "C:\\Users\\User\\Downloads\\Reporte_Estadistico_Servicios.pdf";
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(rutaPDF));
            document.open();

            document.add(new Paragraph("Reporte Estadístico de Servicios más Solicitados"));
            document.add(new Paragraph(" "));

            Image grafico = Image.getInstance(rutaImagen);
            grafico.scaleToFit(500, 400);
            document.add(grafico);

            document.add(new Paragraph(" "));
            document.add(new Paragraph("Totales por servicio:"));
            for (Map.Entry<String, Integer> entry : datos.entrySet()) {
                document.add(new Paragraph("- " + entry.getKey() + ": " + entry.getValue() + " citas"));
            }

            document.close();
            System.out.println("✅ Reporte generado exitosamente: " + rutaPDF);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        generarPDF();
    }
}
