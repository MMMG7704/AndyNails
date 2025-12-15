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
    private static final BaseColor COLOR_PRINCIPAL = new BaseColor(204, 0, 204);
    private static final BaseColor COLOR_SECUNDARIO = new BaseColor(255, 182, 193);
    private static final BaseColor COLOR_TERCIARIO = new BaseColor(255, 215, 0);
    
    private static final DecimalFormat df = new DecimalFormat("#,##0.00");

    // MÉTODO PRINCIPAL - PARA QUE FUNCIONE CON TU CÓDIGO ACTUAL
    public static boolean generarPDF(String carpetaDestino) {
        try {
            System.out.println("Iniciando generación de reporte de servicios...");
            
            // SI LA RUTA ES UNA CARPETA, AGREGAR NOMBRE DE ARCHIVO
            String rutaArchivo;
            if (carpetaDestino.toLowerCase().endsWith(".pdf")) {
                rutaArchivo = carpetaDestino;
            } else {
                if (carpetaDestino.endsWith(File.separator)) {
                    rutaArchivo = carpetaDestino + "Reporte_Servicios_" + 
                                 LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
                } else {
                    rutaArchivo = carpetaDestino + File.separator + "Reporte_Servicios_" + 
                                 LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
                }
            }
            
            System.out.println("Ruta destino: " + rutaArchivo);
            
            Connection con = ConexionBD.getConnection();
            
            // OBTENER DATOS
            Map<String, Object> estadisticas = obtenerEstadisticasServicios(con);
            List<Map<String, Object>> servicios = obtenerServiciosCompletos(con);
            List<Map<String, Object>> serviciosPopulares = obtenerServiciosPopulares(con);

            // CREAR DOCUMENTO
            Document doc = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(doc, new FileOutputStream(rutaArchivo));
            doc.open();

            // AGREGAR CONTENIDO
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
            File pdf = new File(rutaArchivo);
            if (pdf.exists()) {
                System.out.println("✅ Reporte de servicios generado exitosamente");
                System.out.println("📊 Total de servicios: " + servicios.size());
                
                try {
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(pdf);
                    }
                } catch (Exception e) {
                    System.out.println("No se pudo abrir automáticamente");
                }
                
                return true;
            } else {
                System.out.println("❌ No se pudo generar el archivo PDF.");
                return false;
            }

        } catch (Exception e) {
            System.err.println("❌ Error al generar reporte de servicios: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // OBTENER ESTADÍSTICAS DE SERVICIOS - SIMPLIFICADO
    private static Map<String, Object> obtenerEstadisticasServicios(Connection con) throws Exception {
        Map<String, Object> stats = new HashMap<>();
        
        String sql = "SELECT " +
                     "COUNT(*) as total_servicios, " +
                     "AVG(Precio) as precio_promedio, " +
                     "MIN(Precio) as precio_minimo, " +
                     "MAX(Precio) as precio_maximo, " +
                     "SUM(Precio) as valor_total_inventario " +
                     "FROM servicios";
        
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            if (rs.next()) {
                stats.put("totalServicios", rs.getInt("total_servicios"));
                stats.put("precioPromedio", rs.getDouble("precio_promedio"));
                stats.put("precioMinimo", rs.getDouble("precio_minimo"));
                stats.put("precioMaximo", rs.getDouble("precio_maximo"));
                stats.put("valorTotal", rs.getDouble("valor_total_inventario"));
                stats.put("categorias", 1);
            }
        }
        return stats;
    }

    // OBTENER LISTA COMPLETA DE SERVICIOS - SIMPLIFICADO
    private static List<Map<String, Object>> obtenerServiciosCompletos(Connection con) throws Exception {
        List<Map<String, Object>> servicios = new ArrayList<>();
        
        // CONSULTA SEGURA SIN CATEGORÍAS
        String sql = "SELECT " +
                     "s.idServicios, " +
                     "s.Nombre_servicio, " +
                     "s.Descripcion, " +
                     "s.Precio, " +
                     "COUNT(chs.idServicios) as veces_solicitado " +
                     "FROM servicios s " +
                     "LEFT JOIN cita_has_servicios chs ON s.idServicios = chs.idServicios " +
                     "GROUP BY s.idServicios, s.Nombre_servicio, s.Descripcion, s.Precio " +
                     "ORDER BY s.Nombre_servicio";
        
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                Map<String, Object> servicio = new HashMap<>();
                servicio.put("id", rs.getInt("idServicios"));
                servicio.put("nombre", rs.getString("Nombre_servicio"));
                servicio.put("descripcion", rs.getString("Descripcion"));
                servicio.put("precio", rs.getDouble("Precio"));
                servicio.put("categoria", "General");
                servicio.put("vecesSolicitado", rs.getInt("veces_solicitado"));
                servicios.add(servicio);
            }
        }
        return servicios;
    }

    // OBTENER SERVICIOS MÁS POPULARES - SIMPLIFICADO
    private static List<Map<String, Object>> obtenerServiciosPopulares(Connection con) throws Exception {
        List<Map<String, Object>> populares = new ArrayList<>();
        
        String sql = "SELECT " +
                     "s.Nombre_servicio, " +
                     "COUNT(chs.idServicios) as total_citas, " +
                     "s.Precio, " +
                     "SUM(s.Precio) as ingreso_total " +
                     "FROM servicios s " +
                     "LEFT JOIN cita_has_servicios chs ON s.idServicios = chs.idServicios " +
                     "GROUP BY s.Nombre_servicio, s.Precio " +
                     "HAVING total_citas > 0 " +
                     "ORDER BY total_citas DESC " +
                     "LIMIT 10";
        
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            
            while (rs.next()) {
                Map<String, Object> servicio = new HashMap<>();
                servicio.put("nombre", rs.getString("Nombre_servicio"));
                servicio.put("categoria", "General");
                servicio.put("totalCitas", rs.getInt("total_citas"));
                servicio.put("precio", rs.getDouble("Precio"));
                servicio.put("ingresoTotal", rs.getDouble("ingreso_total"));
                populares.add(servicio);
            }
        }
        return populares;
    }

    // ENCABEZADO
    private static void agregarEncabezado(Document doc) throws Exception {
        Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 24, COLOR_PRINCIPAL);
        Paragraph titulo = new Paragraph("ANDYNAILS SPA & BEAUTY", tituloFont);
        titulo.setAlignment(Element.ALIGN_CENTER);
        titulo.setSpacingAfter(5);
        doc.add(titulo);

        Font subtituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, COLOR_SECUNDARIO);
        Paragraph subtitulo = new Paragraph("REPORTE DE SERVICIOS", subtituloFont);
        subtitulo.setAlignment(Element.ALIGN_CENTER);
        subtitulo.setSpacingAfter(15);
        doc.add(subtitulo);

        PdfPTable linea = new PdfPTable(1);
        linea.setWidthPercentage(50);
        PdfPCell celdaLinea = new PdfPCell();
        celdaLinea.setBackgroundColor(COLOR_TERCIARIO);
        celdaLinea.setFixedHeight(2);
        celdaLinea.setBorder(Rectangle.NO_BORDER);
        linea.addCell(celdaLinea);
        linea.setSpacingAfter(10);
        doc.add(linea);

        String fechaGeneracion = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy '•' HH:mm:ss"));
        Font fechaFont = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.DARK_GRAY);
        Paragraph fecha = new Paragraph("📅 Reporte generado: " + fechaGeneracion, fechaFont);
        fecha.setAlignment(Element.ALIGN_CENTER);
        fecha.setSpacingAfter(20);
        doc.add(fecha);
    }

    // RESUMEN EJECUTIVO
    private static void agregarResumenEjecutivo(Document doc, Map<String, Object> estadisticas) throws Exception {
        Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, COLOR_PRINCIPAL);
        Paragraph titulo = new Paragraph("📊 RESUMEN EJECUTIVO", tituloFont);
        titulo.setSpacingAfter(15);
        doc.add(titulo);

        PdfPTable tablaResumen = new PdfPTable(2);
        tablaResumen.setWidthPercentage(80);
        tablaResumen.setHorizontalAlignment(Element.ALIGN_CENTER);
        tablaResumen.setSpacingBefore(10);
        tablaResumen.setSpacingAfter(20);

        agregarFilaResumen(tablaResumen, "Total de Servicios", 
                          estadisticas.getOrDefault("totalServicios", 0).toString());
        agregarFilaResumen(tablaResumen, "Precio Promedio", 
                          "$" + df.format(estadisticas.getOrDefault("precioPromedio", 0.0)));
        agregarFilaResumen(tablaResumen, "Precio Mínimo", 
                          "$" + df.format(estadisticas.getOrDefault("precioMinimo", 0.0)));
        agregarFilaResumen(tablaResumen, "Precio Máximo", 
                          "$" + df.format(estadisticas.getOrDefault("precioMaximo", 0.0)));
        agregarFilaResumen(tablaResumen, "Valor Total", 
                          "$" + df.format(estadisticas.getOrDefault("valorTotal", 0.0)));

        doc.add(tablaResumen);
    }

    private static void agregarFilaResumen(PdfPTable tabla, String titulo, String valor) {
        PdfPCell celdaTitulo = new PdfPCell(new Phrase(titulo));
        celdaTitulo.setBackgroundColor(new BaseColor(240, 240, 240));
        celdaTitulo.setPadding(8);
        
        PdfPCell celdaValor = new PdfPCell(new Phrase(valor));
        celdaValor.setPadding(8);
        
        tabla.addCell(celdaTitulo);
        tabla.addCell(celdaValor);
    }

    // CATÁLOGO COMPLETO
    private static void agregarCatalogoCompleto(Document doc, List<Map<String, Object>> servicios) throws Exception {
        Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, COLOR_PRINCIPAL);
        Paragraph titulo = new Paragraph("📋 CATÁLOGO DE SERVICIOS", tituloFont);
        titulo.setSpacingAfter(10);
        doc.add(titulo);

        PdfPTable tabla = new PdfPTable(5);
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(10);
        tabla.setSpacingAfter(20);

        // Encabezados
        String[] headers = {"ID", "SERVICIO", "PRECIO", "SOLICITADO", "ESTADO"};
        for (String header : headers) {
            PdfPCell celda = new PdfPCell(new Phrase(header));
            celda.setBackgroundColor(COLOR_PRINCIPAL);
            celda.setHorizontalAlignment(Element.ALIGN_CENTER);
            celda.setPadding(6);
            tabla.addCell(celda);
        }

        // Datos
        for (int i = 0; i < servicios.size(); i++) {
            Map<String, Object> servicio = servicios.get(i);
            
            // Color de fondo alternado
            BaseColor fondo = (i % 2 == 0) ? BaseColor.WHITE : new BaseColor(250, 250, 250);
            
            // ID
            agregarCelda(tabla, servicio.get("id").toString(), fondo, Element.ALIGN_CENTER);
            
            // Nombre del servicio
            agregarCelda(tabla, (String) servicio.get("nombre"), fondo, Element.ALIGN_LEFT);
            
            // Precio
            agregarCelda(tabla, "$" + df.format(servicio.get("precio")), 
                        fondo, Element.ALIGN_RIGHT);
            
            // Veces solicitado
            int veces = (Integer) servicio.getOrDefault("vecesSolicitado", 0);
            String solicitado = veces == 0 ? "Nunca" : veces + " veces";
            agregarCelda(tabla, solicitado, fondo, Element.ALIGN_CENTER);
            
            // Estado
            String estado = "";
            if (veces == 0) estado = "Nuevo";
            else if (veces <= 3) estado = "Bajo";
            else if (veces <= 10) estado = "Medio";
            else estado = "Alto";
            agregarCelda(tabla, estado, fondo, Element.ALIGN_CENTER);
        }

        doc.add(tabla);
    }

    private static void agregarCelda(PdfPTable tabla, String texto, BaseColor fondo, int alineacion) {
        PdfPCell celda = new PdfPCell(new Phrase(texto));
        celda.setBackgroundColor(fondo);
        celda.setHorizontalAlignment(alineacion);
        celda.setPadding(4);
        tabla.addCell(celda);
    }

    // SERVICIOS MÁS POPULARES
    private static void agregarServiciosPopulares(Document doc, List<Map<String, Object>> serviciosPopulares) throws Exception {
        Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, COLOR_PRINCIPAL);
        Paragraph titulo = new Paragraph("🏆 SERVICIOS MÁS SOLICITADOS", tituloFont);
        titulo.setSpacingAfter(10);
        doc.add(titulo);

        PdfPTable tabla = new PdfPTable(4);
        tabla.setWidthPercentage(95);
        tabla.setHorizontalAlignment(Element.ALIGN_CENTER);
        tabla.setSpacingBefore(10);
        tabla.setSpacingAfter(20);

        // Encabezados
        String[] headers = {"#", "SERVICIO", "VEZES", "INGRESO"};
        for (String header : headers) {
            PdfPCell celda = new PdfPCell(new Phrase(header));
            celda.setBackgroundColor(COLOR_SECUNDARIO);
            celda.setHorizontalAlignment(Element.ALIGN_CENTER);
            celda.setPadding(6);
            tabla.addCell(celda);
        }

        // Datos
        for (int i = 0; i < serviciosPopulares.size(); i++) {
            Map<String, Object> servicio = serviciosPopulares.get(i);
            
            // Posición
            PdfPCell celdaPos = new PdfPCell(new Phrase((i + 1) + "°"));
            celdaPos.setHorizontalAlignment(Element.ALIGN_CENTER);
            celdaPos.setPadding(4);
            tabla.addCell(celdaPos);
            
            // Nombre
            PdfPCell celdaNombre = new PdfPCell(new Phrase((String) servicio.get("nombre")));
            celdaNombre.setPadding(4);
            tabla.addCell(celdaNombre);
            
            // Veces
            PdfPCell celdaVeces = new PdfPCell(new Phrase(servicio.get("totalCitas").toString()));
            celdaVeces.setHorizontalAlignment(Element.ALIGN_CENTER);
            celdaVeces.setPadding(4);
            tabla.addCell(celdaVeces);
            
            // Ingreso
            PdfPCell celdaIngreso = new PdfPCell(new Phrase("$" + df.format(servicio.get("ingresoTotal"))));
            celdaIngreso.setHorizontalAlignment(Element.ALIGN_RIGHT);
            celdaIngreso.setPadding(4);
            tabla.addCell(celdaIngreso);
        }

        doc.add(tabla);
    }

    // ANÁLISIS Y RECOMENDACIONES
    private static void agregarAnalisisRecomendaciones(Document doc, List<Map<String, Object>> servicios) throws Exception {
        Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, COLOR_PRINCIPAL);
        Paragraph titulo = new Paragraph("💡 ANÁLISIS", tituloFont);
        titulo.setSpacingAfter(10);
        doc.add(titulo);

        // Calcular métricas simples
        long sinSolicitudes = servicios.stream()
            .filter(s -> ((Integer) s.getOrDefault("vecesSolicitado", 0)) == 0)
            .count();
        
        long conDescripcion = servicios.stream()
            .filter(s -> {
                String desc = (String) s.get("descripcion");
                return desc != null && desc.length() > 10;
            })
            .count();

        Font contenidoFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
        String analisis = "Resumen del catálogo:\n" +
                         "• Total servicios: " + servicios.size() + "\n" +
                         "• Servicios sin demanda: " + sinSolicitudes + "\n" +
                         "• Con descripción completa: " + conDescripcion + "\n\n" +
                         "Recomendaciones:\n" +
                         "1. Evaluar servicios sin demanda\n" +
                         "2. Mejorar descripciones\n" +
                         "3. Crear paquetes promocionales";

        Paragraph contenido = new Paragraph(analisis, contenidoFont);
        contenido.setSpacingAfter(20);
        doc.add(contenido);
    }

    // PIE DE PÁGINA
    private static void agregarPiePagina(Document doc) throws Exception {
        PdfPTable linea = new PdfPTable(1);
        linea.setWidthPercentage(100);
        PdfPCell celdaLinea = new PdfPCell();
        celdaLinea.setBackgroundColor(COLOR_TERCIARIO);
        celdaLinea.setFixedHeight(1);
        celdaLinea.setBorder(Rectangle.NO_BORDER);
        linea.addCell(celdaLinea);
        linea.setSpacingBefore(10);
        linea.setSpacingAfter(10);
        doc.add(linea);

        Font pieFont = FontFactory.getFont(FontFactory.HELVETICA, 8, BaseColor.DARK_GRAY);
        Paragraph pie = new Paragraph(
            "© " + LocalDateTime.now().getYear() + " - AndyNails SPA & Beauty | " +
            "Reporte generado el " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), 
            pieFont);
        pie.setAlignment(Element.ALIGN_CENTER);
        doc.add(pie);
    }

    // MÉTODO MAIN DE PRUEBA
    public static void main(String[] args) {
        System.out.println("=== GENERADOR DE REPORTES ANDYNAILS ===");
        
        String carpeta = "C:\\Users\\mgmmo\\Documents\\7SEMESTRE\\INGENIERIASOF\\Reportes";
        boolean exito = generarPDF(carpeta);
        
        if (exito) {
            System.out.println(" Reporte generado exitosamente!");
        } else {
            System.out.println(" Error al generar el reporte");
        }
    }
}