package andynails;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.awt.Desktop;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReporteServicios {

    // Colores corporativos de AndyNails
    private static final BaseColor COLOR_PRINCIPAL = new BaseColor(204, 0, 204); // Morado
    private static final BaseColor COLOR_SECUNDARIO = new BaseColor(255, 182, 193); // Rosa
    private static final BaseColor COLOR_TERCIARIO = new BaseColor(255, 215, 0); // Dorado
    
    private static final DecimalFormat df = new DecimalFormat("#,##0.00");

    public static void generarPDF(String rutaSalida) {
        try {
            Connection con = ConexionBD.getConnection();
            
            // OBTENER DATOS ESTADÍSTICOS
            Map<String, Object> estadisticas = obtenerEstadisticasServicios(con);
            List<Map<String, Object>> servicios = obtenerServiciosCompletos(con);
            List<Map<String, Object>> serviciosPopulares = obtenerServiciosPopulares(con);

            // CREAR DOCUMENTO PROFESIONAL
            Document doc = new Document();
            PdfWriter.getInstance(doc, new FileOutputStream(rutaSalida));
            doc.open();

            // AGREGAR CONTENIDO PROFESIONAL
            agregarEncabezado(doc);
            agregarResumenEjecutivo(doc, estadisticas);
            agregarCatalogoCompleto(doc, servicios);
            if (!serviciosPopulares.isEmpty()) {
                agregarServiciosPopulares(doc, serviciosPopulares);
            }
            agregarAnalisisRecomendaciones(doc, servicios);
            agregarPiePagina(doc);

            doc.close();

            // VERIFICAR Y ABRIR PDF
            File pdf = new File(rutaSalida);
            if (pdf.exists()) {
                System.out.println("Reporte de servicios generado exitosamente: " + rutaSalida);
                System.out.println("Total de servicios: " + servicios.size());
                
                // Abrir automáticamente el PDF
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(pdf);
                }
            } else {
                System.out.println("No se pudo generar el archivo PDF.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error al generar reporte: " + e.getMessage());
        }
    }

    // OBTENER ESTADÍSTICAS DE SERVICIOS
    private static Map<String, Object> obtenerEstadisticasServicios(Connection con) throws Exception {
        Map<String, Object> stats = new HashMap<>();
        
        String sql = "SELECT " +
                     "COUNT(*) as total_servicios, " +
                     "AVG(Precio) as precio_promedio, " +
                     "MIN(Precio) as precio_minimo, " +
                     "MAX(Precio) as precio_maximo, " +
                     "SUM(Precio) as valor_total_inventario " +
                     "FROM servicios";
        
        PreparedStatement ps = con.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        
        if (rs.next()) {
            stats.put("totalServicios", rs.getInt("total_servicios"));
            stats.put("precioPromedio", rs.getDouble("precio_promedio"));
            stats.put("precioMinimo", rs.getDouble("precio_minimo"));
            stats.put("precioMaximo", rs.getDouble("precio_maximo"));
            stats.put("valorTotal", rs.getDouble("valor_total_inventario"));
        }
        
        rs.close();
        ps.close();
        return stats;
    }

    // OBTENER LISTA COMPLETA DE SERVICIOS - CORREGIDO
    private static List<Map<String, Object>> obtenerServiciosCompletos(Connection con) throws Exception {
        List<Map<String, Object>> servicios = new ArrayList<>();
        
        String sql = "SELECT s.idServicios, s.Nombre_servicio, s.Descripcion, s.Precio, " +
                     "COUNT(chs.idServicios) as veces_solicitado " +
                     "FROM servicios s " +
                     "LEFT JOIN cita_has_servicios chs ON s.idServicios = chs.idServicios " +
                     "GROUP BY s.idServicios, s.Nombre_servicio, s.Descripcion, s.Precio " +
                     "ORDER BY s.Nombre_servicio";
        
        PreparedStatement ps = con.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        
        while (rs.next()) {
            Map<String, Object> servicio = new HashMap<>();
            servicio.put("id", rs.getInt("idServicios"));
            servicio.put("nombre", rs.getString("Nombre_servicio"));
            servicio.put("descripcion", rs.getString("Descripcion"));
            servicio.put("precio", rs.getDouble("Precio"));
            servicio.put("vecesSolicitado", rs.getInt("veces_solicitado"));
            servicios.add(servicio);
        }
        
        rs.close();
        ps.close();
        return servicios;
    }

    // OBTENER SERVICIOS MÁS POPULARES - CORREGIDO
    private static List<Map<String, Object>> obtenerServiciosPopulares(Connection con) throws Exception {
        List<Map<String, Object>> populares = new ArrayList<>();
        
        String sql = "SELECT s.Nombre_servicio, COUNT(chs.idServicios) as total_citas, " +
                     "s.Precio " +
                     "FROM servicios s " +
                     "LEFT JOIN cita_has_servicios chs ON s.idServicios = chs.idServicios " +
                     "GROUP BY s.Nombre_servicio, s.Precio " +
                     "ORDER BY total_citas DESC LIMIT 5";
        
        PreparedStatement ps = con.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();
        
        while (rs.next()) {
            Map<String, Object> servicio = new HashMap<>();
            servicio.put("nombre", rs.getString("Nombre_servicio"));
            servicio.put("totalCitas", rs.getInt("total_citas"));
            servicio.put("precio", rs.getDouble("Precio"));
            populares.add(servicio);
        }
        
        rs.close();
        ps.close();
        return populares;
    }

    // ENCABEZADO PROFESIONAL
    private static void agregarEncabezado(Document doc) throws Exception {
        // Título principal
        Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, COLOR_PRINCIPAL);
        Paragraph titulo = new Paragraph("CATÁLOGO DE SERVICIOS - ANDYNAILS", tituloFont);
        titulo.setAlignment(Element.ALIGN_CENTER);
        titulo.setSpacingAfter(15);
        doc.add(titulo);

        // Subtítulo
        Font subtituloFont = FontFactory.getFont(FontFactory.HELVETICA, 14, BaseColor.GRAY);
        Paragraph subtitulo = new Paragraph("Portafolio Completo de Servicios de Belleza", subtituloFont);
        subtitulo.setAlignment(Element.ALIGN_CENTER);
        subtitulo.setSpacingAfter(5);
        doc.add(subtitulo);

        // Fecha de generación
        String fechaGeneracion = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy 'a las' HH:mm"));
        Font fechaFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.GRAY);
        Paragraph fecha = new Paragraph("Generado el: " + fechaGeneracion, fechaFont);
        fecha.setAlignment(Element.ALIGN_CENTER);
        fecha.setSpacingAfter(20);
        doc.add(fecha);
    }

    // RESUMEN EJECUTIVO
    private static void agregarResumenEjecutivo(Document doc, Map<String, Object> estadisticas) throws Exception {
        Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, COLOR_PRINCIPAL);
        Paragraph titulo = new Paragraph("RESUMEN EJECUTIVO", tituloFont);
        titulo.setSpacingAfter(10);
        doc.add(titulo);

        int totalServicios = (Integer) estadisticas.getOrDefault("totalServicios", 0);
        double precioPromedio = (Double) estadisticas.getOrDefault("precioPromedio", 0.0);
        double precioMinimo = (Double) estadisticas.getOrDefault("precioMinimo", 0.0);
        double precioMaximo = (Double) estadisticas.getOrDefault("precioMaximo", 0.0);
        double valorTotal = (Double) estadisticas.getOrDefault("valorTotal", 0.0);

        Font contenidoFont = FontFactory.getFont(FontFactory.HELVETICA, 11);
        String resumen = "Total de Servicios Ofrecidos: " + totalServicios + "\n" +
                        "Rango de Precios: $" + df.format(precioMinimo) + " - $" + df.format(precioMaximo) + "\n" +
                        "Precio Promedio: $" + df.format(precioPromedio) + "\n" +
                        "Valor Total del Portafolio: $" + df.format(valorTotal);

        Paragraph contenido = new Paragraph(resumen, contenidoFont);
        contenido.setSpacingAfter(20);
        doc.add(contenido);
    }

    // CATÁLOGO COMPLETO DE SERVICIOS
    private static void agregarCatalogoCompleto(Document doc, List<Map<String, Object>> servicios) throws Exception {
        Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, COLOR_PRINCIPAL);
        Paragraph titulo = new Paragraph("CATÁLOGO COMPLETO DE SERVICIOS", tituloFont);
        titulo.setSpacingAfter(10);
        doc.add(titulo);

        // Crear tabla profesional
        PdfPTable tabla = new PdfPTable(4); // ✅ Cambiado a 4 columnas (sin categoría)
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(10);
        tabla.setSpacingAfter(20);

        // Encabezados de tabla
        agregarCeldaHeader(tabla, "ID");
        agregarCeldaHeader(tabla, "SERVICIO");
        agregarCeldaHeader(tabla, "PRECIO");
        agregarCeldaHeader(tabla, "SOLICITADO");

        // Llenar tabla con datos
        for (Map<String, Object> servicio : servicios) {
            agregarCeldaNormal(tabla, servicio.get("id").toString());
            agregarCeldaNormal(tabla, (String) servicio.get("nombre"));
            agregarCeldaNormal(tabla, "$" + df.format(servicio.get("precio")));
            
            int vecesSolicitado = (Integer) servicio.getOrDefault("vecesSolicitado", 0);
            String solicitudes = vecesSolicitado + " veces";
            agregarCeldaNormal(tabla, solicitudes);
        }

        doc.add(tabla);
    }

    // SERVICIOS MÁS POPULARES
    private static void agregarServiciosPopulares(Document doc, List<Map<String, Object>> serviciosPopulares) throws Exception {
        Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, COLOR_PRINCIPAL);
        Paragraph titulo = new Paragraph("SERVICIOS MÁS SOLICITADOS", tituloFont);
        titulo.setSpacingAfter(10);
        doc.add(titulo);

        PdfPTable tabla = new PdfPTable(3); // ✅ Cambiado a 3 columnas (sin categoría)
        tabla.setWidthPercentage(80);
        tabla.setHorizontalAlignment(Element.ALIGN_CENTER);
        tabla.setSpacingBefore(10);
        tabla.setSpacingAfter(20);

        // Encabezados
        agregarCeldaHeader(tabla, "POSICIÓN");
        agregarCeldaHeader(tabla, "SERVICIO");
        agregarCeldaHeader(tabla, "SOLICITUDES");

        // Datos con ranking
        int posicion = 1;
        for (Map<String, Object> servicio : serviciosPopulares) {
            agregarCeldaNormal(tabla, posicion + "°");
            agregarCeldaNormal(tabla, (String) servicio.get("nombre"));
            agregarCeldaNormal(tabla, servicio.get("totalCitas") + " citas");
            
            posicion++;
        }

        doc.add(tabla);
    }

    // ANÁLISIS Y RECOMENDACIONES
    private static void agregarAnalisisRecomendaciones(Document doc, List<Map<String, Object>> servicios) throws Exception {
        Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, COLOR_PRINCIPAL);
        Paragraph titulo = new Paragraph("ANÁLISIS Y RECOMENDACIONES", tituloFont);
        titulo.setSpacingAfter(10);
        doc.add(titulo);

        // Calcular métricas simples
        long serviciosConDescripcion = servicios.stream()
            .filter(s -> {
                String desc = (String) s.get("descripcion");
                return desc != null && desc.length() > 10;
            })
            .count();
        
        double porcentajeConDescripcion = servicios.isEmpty() ? 0 : (serviciosConDescripcion * 100.0) / servicios.size();

        Font contenidoFont = FontFactory.getFont(FontFactory.HELVETICA, 11);
        String analisis = "Métricas del Catálogo:\n" +
                         "Total de servicios registrados: " + servicios.size() + "\n" +
                         "Servicios con descripción completa: " + String.format("%.1f%%", porcentajeConDescripcion) + "\n" +
                         "Rango de precios diversificado\n\n" +
                         
                         "Recomendaciones Estratégicas:\n" +
                         "Mantener actualizadas las descripciones de servicios\n" +
                         "Considerar promociones para servicios menos solicitados\n" +
                         "Evaluar precios basados en popularidad y costo\n" +
                         "Promocionar servicios premium de mayor valor\n\n" +
                         
                         "Oportunidades de Crecimiento:\n" +
                         "Desarrollar paquetes combinados de servicios\n" +
                         "Crear servicios estacionales o temáticos\n" +
                         "Implementar programas de fidelización";

        Paragraph contenido = new Paragraph(analisis, contenidoFont);
        contenido.setSpacingAfter(20);
        doc.add(contenido);
    }

    // PIE DE PÁGINA
    private static void agregarPiePagina(Document doc) throws Exception {
        Font pieFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, BaseColor.GRAY);
        Paragraph pie = new Paragraph(
            "Sistema de Gestión AndyNails - Catálogo de Servicios Actualizado", 
            pieFont);
        pie.setAlignment(Element.ALIGN_CENTER);
        doc.add(pie);
    }

    // MÉTODOS AUXILIARES PARA TABLAS
    private static void agregarCeldaHeader(PdfPTable tabla, String texto) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, 
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.WHITE)));
        celda.setBackgroundColor(COLOR_PRINCIPAL);
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        celda.setPadding(8);
        celda.setBorderWidth(1);
        tabla.addCell(celda);
    }

    private static void agregarCeldaNormal(PdfPTable tabla, String texto) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, 
            FontFactory.getFont(FontFactory.HELVETICA, 9)));
        celda.setPadding(6);
        celda.setBorderWidth(0.5f);
        celda.setBorderColor(BaseColor.LIGHT_GRAY);
        tabla.addCell(celda);
    }

    // MÉTODO MAIN DE PRUEBA
    public static void main(String[] args) {
        String ruta = "C:\\Users\\mgmmo\\Documents\\7SEMESTRE\\INGENIERIASOF\\Reportes\\" +
                     "Catalogo_Servicios_AndyNails_" + 
                     LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
        
        generarPDF(ruta);
    }
}