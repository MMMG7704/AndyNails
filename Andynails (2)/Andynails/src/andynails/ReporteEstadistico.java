package andynails;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities; 
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

public class ReporteEstadistico {

    // Colores corporativos de AndyNails
    private static final Color COLOR_PRINCIPAL = new Color(204, 0, 204); // Morado AndyNails
    private static final Color COLOR_SECUNDARIO = new Color(255, 182, 193); // Rosa
    private static final Color COLOR_TERCIARIO = new Color(255, 215, 0); // Dorado
    
    public static void generarPDF() {
        ConexionBD conexion = new ConexionBD();
        Connection conn = conexion.conectar();

        String sql = "SELECT s.Nombre_servicio, COUNT(chs.idServicios) AS total " +
                     "FROM cita_has_servicios chs " +
                     "INNER JOIN servicios s ON chs.idServicios = s.idServicios " +
                     "GROUP BY s.Nombre_servicio " +
                     "ORDER BY total DESC";

        try {
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            DefaultPieDataset dataset = new DefaultPieDataset();
            Map<String, Integer> datos = new HashMap<>();
            int totalCitas = 0;

            while (rs.next()) {
                String nombre = rs.getString("Nombre_servicio");
                int total = rs.getInt("total");
                dataset.setValue(nombre, total);
                datos.put(nombre, total);
                totalCitas += total;
            }

            // Crear la gráfica de pastel mejorada
            JFreeChart chart = ChartFactory.createPieChart(
                    null, // Sin título, lo pondremos en el PDF
                    dataset,
                    true,
                    true,
                    false
            );
            
            chart.setBackgroundPaint(Color.WHITE);
            chart.setBorderVisible(false);
            
            PiePlot plot = (PiePlot) chart.getPlot();
            plot.setBackgroundPaint(Color.WHITE);
            plot.setOutlineVisible(false);
            plot.setLabelBackgroundPaint(Color.WHITE);
            plot.setLabelFont(new Font("SansSerif", Font.BOLD, 12));
            plot.setLabelPaint(new Color(60, 60, 60));
            
            // Colores personalizados para cada servicio
            Color[] coloresServicios = {
                new Color(204, 0, 204),   // Morado - Maquillaje
                new Color(255, 105, 180), // Rosa fuerte - Uñas
                new Color(147, 112, 219), // Morado medio - Peinado
                new Color(255, 215, 0),   // Dorado - Otros servicios
                new Color(173, 216, 230), // Azul claro
                new Color(144, 238, 144)  // Verde claro
            };
            
            int colorIndex = 0;
            for (Object key : dataset.getKeys()) {
                if (colorIndex < coloresServicios.length) {
                    plot.setSectionPaint(key.toString(), coloresServicios[colorIndex]);
                    colorIndex++;
                }
            }

            String rutaImagen = "grafico_estadistico_temp.png";
            ChartUtilities.saveChartAsPNG(new File(rutaImagen), chart, 650, 450);

            //  CREAR PDF PROFESIONAL
            Document document = new Document();
            String rutaPDF = "Reporte_Estadistico_AndyNails_" + 
                           LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
            
            PdfWriter.getInstance(document, new FileOutputStream(rutaPDF));
            document.open();

            com.itextpdf.text.Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, 
                new com.itextpdf.text.BaseColor(COLOR_PRINCIPAL.getRGB()));
            com.itextpdf.text.Font subtituloFont = FontFactory.getFont(FontFactory.HELVETICA, 12, 
                com.itextpdf.text.BaseColor.GRAY);
            com.itextpdf.text.Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11);
            com.itextpdf.text.Font destacadoFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);

            // Título principal
            Paragraph titulo = new Paragraph("REPORTE ESTADÍSTICO - ANDYNAILS", tituloFont);
            titulo.setAlignment(Element.ALIGN_CENTER);
            titulo.setSpacingAfter(20);
            document.add(titulo);

            // Información de la empresa
            Paragraph infoEmpresa = new Paragraph("AndyNails Studio - Sistema de Gestión de Citas", subtituloFont);
            infoEmpresa.setAlignment(Element.ALIGN_CENTER);
            infoEmpresa.setSpacingAfter(5);
            document.add(infoEmpresa);

            // Fecha de generación
            String fechaGeneracion = LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy 'a las' HH:mm"));
            Paragraph fecha = new Paragraph("Generado el: " + fechaGeneracion, subtituloFont);
            fecha.setAlignment(Element.ALIGN_CENTER);
            fecha.setSpacingAfter(20);
            document.add(fecha);

            //  RESUMEN EJECUTIVO
            Paragraph resumenTitulo = new Paragraph("RESUMEN EJECUTIVO", destacadoFont);
            resumenTitulo.setSpacingAfter(10);
            document.add(resumenTitulo);

            Paragraph resumen = new Paragraph(
                "Total de citas analizadas: " + totalCitas + "\n" +
                "Servicios monitoreados: " + datos.size() + "\n" +
                "Período: Datos históricos completos", normalFont);
            resumen.setSpacingAfter(20);
            document.add(resumen);

            // 🔹 GRÁFICO
            Paragraph graficoTitulo = new Paragraph("DISTRIBUCIÓN DE SERVICIOS MÁS SOLICITADOS", destacadoFont);
            graficoTitulo.setAlignment(Element.ALIGN_CENTER);
            graficoTitulo.setSpacingAfter(15);
            document.add(graficoTitulo);

            Image grafico = Image.getInstance(rutaImagen);
            grafico.setAlignment(Element.ALIGN_CENTER);
            grafico.scaleToFit(500, 350);
            document.add(grafico);

            document.add(new Paragraph(" "));

            //  TABLA DETALLADA
            Paragraph tablaTitulo = new Paragraph("DETALLE POR SERVICIO", destacadoFont);
            tablaTitulo.setSpacingAfter(10);
            document.add(tablaTitulo);

            // Crear tabla
            PdfPTable tabla = new PdfPTable(3);
            tabla.setWidthPercentage(100);
            tabla.setSpacingBefore(10);
            tabla.setSpacingAfter(20);

            // Encabezados de tabla
            agregarCeldaTabla(tabla, "SERVICIO", true, COLOR_PRINCIPAL);
            agregarCeldaTabla(tabla, "CANTIDAD", true, COLOR_PRINCIPAL);
            agregarCeldaTabla(tabla, "PORCENTAJE", true, COLOR_PRINCIPAL);

            // Datos de la tabla
            for (Map.Entry<String, Integer> entry : datos.entrySet()) {
                String servicio = entry.getKey();
                int cantidad = entry.getValue();
                double porcentaje = totalCitas > 0 ? (cantidad * 100.0 / totalCitas) : 0;
                
                agregarCeldaTabla(tabla, servicio, false, null);
                agregarCeldaTabla(tabla, String.valueOf(cantidad), false, null);
                agregarCeldaTabla(tabla, String.format("%.1f%%", porcentaje), false, null);
            }

            document.add(tabla);

            // ANÁLISIS Y OBSERVACIONES
            Paragraph analisisTitulo = new Paragraph("ANÁLISIS Y OBSERVACIONES", destacadoFont);
            analisisTitulo.setSpacingAfter(10);
            document.add(analisisTitulo);

            // Encontrar servicio más popular
            String servicioMasPopular = datos.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("No disponible");

            Paragraph analisis = new Paragraph(
                "• El servicio más solicitado es: " + servicioMasPopular + "\n" +
                "• Este reporte ayuda a identificar tendencias de preferencia de los clientes.\n" +
                "• Los datos pueden utilizarse para optimizar inventario y capacitación.\n" +
                "• Se recomienda revisar periódicamente para identificar cambios en las preferencias.",
                normalFont);
            analisis.setSpacingAfter(20);
            document.add(analisis);

            // PIE DE PÁGINA
            Paragraph piePagina = new Paragraph(
                "--- Reporte generado automáticamente por AndyNails System ---", 
                FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, com.itextpdf.text.BaseColor.GRAY));
            piePagina.setAlignment(Element.ALIGN_CENTER);
            document.add(piePagina);

            document.close();

            // Limpiar archivo temporal
            new File(rutaImagen).delete();
            
            System.out.println(" Reporte profesional generado exitosamente: " + rutaPDF);
            System.out.println(" Total de citas analizadas: " + totalCitas);
            System.out.println(" Servicios diferentes: " + datos.size());

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(" Error al generar el reporte: " + e.getMessage());
        }
    }

    // Método auxiliar para agregar celdas a la tabla
    private static void agregarCeldaTabla(PdfPTable tabla, String texto, boolean isHeader, Color color) {
        PdfPCell celda = new PdfPCell(new Phrase(texto));
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        celda.setPadding(8);
        
        if (isHeader) {
            celda.setBackgroundColor(new com.itextpdf.text.BaseColor(color.getRGB()));
            celda.setPhrase(new Phrase(texto, 
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, com.itextpdf.text.BaseColor.WHITE)));
        } else {
            celda.setBackgroundColor(com.itextpdf.text.BaseColor.WHITE);
        }
        
        tabla.addCell(celda);
    }

    public static void main(String[] args) {
        generarPDF();
    }
}