package andynails;

import java.awt.Color;
import java.awt.Font;
import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

public class ReportePagos {

    // Colores corporativos
    private static final Color COLOR_PRINCIPAL = new Color(204, 0, 204); // Morado AndyNails
    private static final Color COLOR_EXITOSO = new Color(76, 175, 80);   // Verde
    private static final Color COLOR_PENDIENTE = new Color(255, 152, 0); // Naranja
    private static final Color COLOR_RECHAZADO = new Color(244, 67, 54); // Rojo
    
    private static final DecimalFormat df = new DecimalFormat("#,##0.00");

    public static void generarReportePagos() {
        ConexionBD conexion = new ConexionBD();
        Connection conn = conexion.conectar();

        try {
            // DATOS PRINCIPALES
            Map<String, Object> estadisticas = obtenerEstadisticasGenerales(conn);
            List<Map<String, Object>> pagosRecientes = obtenerPagosRecientes(conn);
            Map<String, Double> ingresosPorMes = obtenerIngresosPorMes(conn);
            Map<String, Integer> pagosPorEstado = obtenerPagosPorEstado(conn);

            //  CREAR GRÁFICOS
            String rutaGraficoBarras = crearGraficoBarras(ingresosPorMes);
            String rutaGraficoPastel = crearGraficoPastel(pagosPorEstado);

            //  CREAR PDF PROFESIONAL
            crearPDFCompleto(estadisticas, pagosRecientes, ingresosPorMes, pagosPorEstado, 
                           rutaGraficoBarras, rutaGraficoPastel);

            // LIMPIAR ARCHIVOS TEMPORALES
            new File(rutaGraficoBarras).delete();
            new File(rutaGraficoPastel).delete();

            System.out.println(" Reporte de pagos generado exitosamente!");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error al generar reporte de pagos: " + e.getMessage());
        }
    }

    //  OBTENER ESTADÍSTICAS GENERALES
    private static Map<String, Object> obtenerEstadisticasGenerales(Connection conn) throws Exception {
        Map<String, Object> stats = new HashMap<>();

        String sql = "SELECT " +
                     "COUNT(*) as total_pagos, " +
                     "SUM(CASE WHEN Estado_pago = 'Validado' THEN Monto ELSE 0 END) as total_ingresos, " +
                     "AVG(CASE WHEN Estado_pago = 'Validado' THEN Monto ELSE NULL END) as promedio_pago, " +
                     "COUNT(CASE WHEN Estado_pago = 'Validado' THEN 1 END) as pagos_validados, " +
                     "COUNT(CASE WHEN Estado_pago = 'Pendiente' THEN 1 END) as pagos_pendientes, " +
                     "COUNT(CASE WHEN Estado_pago = 'Rechazado' THEN 1 END) as pagos_rechazados, " +
                     "MAX(Monto) as pago_maximo " +
                     "FROM pago WHERE Estado_pago != 'Cancelado'";

        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            stats.put("totalPagos", rs.getInt("total_pagos"));
            stats.put("totalIngresos", rs.getDouble("total_ingresos"));
            stats.put("promedioPago", rs.getDouble("promedio_pago"));
            stats.put("pagosValidados", rs.getInt("pagos_validados"));
            stats.put("pagosPendientes", rs.getInt("pagos_pendientes"));
            stats.put("pagosRechazados", rs.getInt("pagos_rechazados"));
            stats.put("pagoMaximo", rs.getDouble("pago_maximo"));
        }

        rs.close();
        ps.close();
        return stats;
    }

    //  OBTENER PAGOS RECIENTES
    private static List<Map<String, Object>> obtenerPagosRecientes(Connection conn) throws Exception {
        List<Map<String, Object>> pagos = new ArrayList<>();

        String sql = "SELECT p.idPago, p.Monto, p.Estado_pago, p.Fecha_pago, " +
                     "CONCAT(u.Nombre, ' ', u.Paterno) as cliente, " +
                     "s.Nombre_servicio as servicio " +
                     "FROM pago p " +
                     "LEFT JOIN cita_has_servicios chs ON p.idPago = chs.Pago_idPago " +
                     "LEFT JOIN servicios s ON chs.idServicios = s.idServicios " +
                     "LEFT JOIN cita c ON chs.idCita = c.idCita " +
                     "LEFT JOIN usuarios u ON c.idUsuarios = u.idUsuarios " +
                     "ORDER BY p.Fecha_pago DESC LIMIT 10";

        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            Map<String, Object> pago = new HashMap<>();
            pago.put("id", rs.getInt("idPago"));
            pago.put("monto", rs.getDouble("Monto"));
            pago.put("estado", rs.getString("Estado_pago"));
            pago.put("fecha", rs.getDate("Fecha_pago"));
            pago.put("cliente", rs.getString("cliente"));
            pago.put("servicio", rs.getString("servicio"));
            pagos.add(pago);
        }

        rs.close();
        ps.close();
        return pagos;
    }

    //  OBTENER INGRESOS POR MES
    private static Map<String, Double> obtenerIngresosPorMes(Connection conn) throws Exception {
        Map<String, Double> ingresos = new HashMap<>();

        String sql = "SELECT DATE_FORMAT(Fecha_pago, '%Y-%m') as mes, " +
                     "SUM(Monto) as total " +
                     "FROM pago " +
                     "WHERE Estado_pago = 'Validado' " +
                     "GROUP BY DATE_FORMAT(Fecha_pago, '%Y-%m') " +
                     "ORDER BY mes DESC LIMIT 12";

        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            ingresos.put(rs.getString("mes"), rs.getDouble("total"));
        }

        rs.close();
        ps.close();
        return ingresos;
    }

    //  OBTENER PAGOS POR ESTADO
    private static Map<String, Integer> obtenerPagosPorEstado(Connection conn) throws Exception {
        Map<String, Integer> estados = new HashMap<>();

        String sql = "SELECT Estado_pago, COUNT(*) as cantidad " +
                     "FROM pago " +
                     "GROUP BY Estado_pago";

        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            estados.put(rs.getString("Estado_pago"), rs.getInt("cantidad"));
        }

        rs.close();
        ps.close();
        return estados;
    }

    //  CREAR GRÁFICO DE BARRAS - INGRESOS POR MES
    private static String crearGraficoBarras(Map<String, Double> ingresosPorMes) throws Exception {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        // Ordenar meses y agregar al dataset
        ingresosPorMes.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByKey().reversed())
            .forEach(entry -> {
                String[] partes = entry.getKey().split("-");
                String mesLabel = partes[1] + "/" + partes[0].substring(2);
                dataset.addValue(entry.getValue(), "Ingresos", mesLabel);
            });

        JFreeChart chart = ChartFactory.createBarChart(
            "INGRESOS MENSUALES - ANDYNAILS",
            "Mes",
            "Monto ($)",
            dataset,
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        );

        // PERSONALIZAR GRÁFICO
        chart.setBackgroundPaint(Color.WHITE);
        chart.setBorderVisible(false);

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.getRenderer().setSeriesPaint(0, COLOR_PRINCIPAL);

        // Personalizar ejes
        plot.getDomainAxis().setLabelFont(new Font("SansSerif", Font.BOLD, 12));
        plot.getRangeAxis().setLabelFont(new Font("SansSerif", Font.BOLD, 12));

        String rutaImagen = "grafico_ingresos_temp.png";
        ChartUtilities.saveChartAsPNG(new File(rutaImagen), chart, 700, 400);
        return rutaImagen;
    }

    // CREAR GRÁFICO DE PASTEL - ESTADOS DE PAGO
    private static String crearGraficoPastel(Map<String, Integer> pagosPorEstado) throws Exception {
        org.jfree.data.general.DefaultPieDataset dataset = new org.jfree.data.general.DefaultPieDataset();

        pagosPorEstado.forEach((estado, cantidad) -> {
            dataset.setValue(estado + " (" + cantidad + ")", cantidad);
        });

        JFreeChart chart = ChartFactory.createPieChart(
            null,
            dataset,
            true,
            true,
            false
        );

        //  PERSONALIZAR GRÁFICO
        chart.setBackgroundPaint(Color.WHITE);
        chart.setBorderVisible(false);

        org.jfree.chart.plot.PiePlot plot = (org.jfree.chart.plot.PiePlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.setLabelFont(new Font("SansSerif", Font.BOLD, 11));

        // Asignar colores según estado
        plot.setSectionPaint("Validado (" + pagosPorEstado.getOrDefault("Validado", 0) + ")", COLOR_EXITOSO);
        plot.setSectionPaint("Pendiente (" + pagosPorEstado.getOrDefault("Pendiente", 0) + ")", COLOR_PENDIENTE);
        plot.setSectionPaint("Rechazado (" + pagosPorEstado.getOrDefault("Rechazado", 0) + ")", COLOR_RECHAZADO);

        String rutaImagen = "grafico_estados_temp.png";
        ChartUtilities.saveChartAsPNG(new File(rutaImagen), chart, 500, 350);
        return rutaImagen;
    }

    //  CREAR PDF COMPLETO
    private static void crearPDFCompleto(Map<String, Object> estadisticas, 
                                       List<Map<String, Object>> pagosRecientes,
                                       Map<String, Double> ingresosPorMes,
                                       Map<String, Integer> pagosPorEstado,
                                       String rutaGraficoBarras,
                                       String rutaGraficoPastel) throws Exception {
        
        Document document = new Document();
        String rutaPDF = "Reporte_Pagos_AndyNails_" + 
                       LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
        
        PdfWriter.getInstance(document, new FileOutputStream(rutaPDF));
        document.open();

        //  FUENTES
        com.itextpdf.text.Font tituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 20, 
            new com.itextpdf.text.BaseColor(COLOR_PRINCIPAL.getRGB()));
        com.itextpdf.text.Font subtituloFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
        com.itextpdf.text.Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 11);
        com.itextpdf.text.Font destacadoFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);

        //  ENCABEZADO
        agregarEncabezado(document, tituloFont, subtituloFont);

        //  RESUMEN EJECUTIVO
        agregarResumenEjecutivo(document, estadisticas, destacadoFont, normalFont);

        // GRÁFICOS
        agregarGraficos(document, rutaGraficoBarras, rutaGraficoPastel, destacadoFont);

        //  PAGOS RECIENTES
        agregarPagosRecientes(document, pagosRecientes, destacadoFont, normalFont);

        //  ANÁLISIS MENSUAL
        agregarAnalisisMensual(document, ingresosPorMes, destacadoFont, normalFont);

        //  PIE DE PÁGINA
        agregarPiePagina(document);

        document.close();
        System.out.println(" PDF generado: " + rutaPDF);
    }

    //  ENCABEZADO DEL REPORTE
    private static void agregarEncabezado(Document document, 
                                        com.itextpdf.text.Font tituloFont,
                                        com.itextpdf.text.Font subtituloFont) throws Exception {
        
        Paragraph titulo = new Paragraph("REPORTE FINANCIERO - ANDYNAILS", tituloFont);
        titulo.setAlignment(Element.ALIGN_CENTER);
        titulo.setSpacingAfter(15);
        document.add(titulo);

        Paragraph subtitulo = new Paragraph("Análisis Detallado de Pagos y Transacciones", subtituloFont);
        subtitulo.setAlignment(Element.ALIGN_CENTER);
        subtitulo.setSpacingAfter(5);
        document.add(subtitulo);

        String fechaGeneracion = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy 'a las' HH:mm"));
        Paragraph fecha = new Paragraph("Generado el: " + fechaGeneracion, 
            FontFactory.getFont(FontFactory.HELVETICA, 10, com.itextpdf.text.BaseColor.GRAY));
        fecha.setAlignment(Element.ALIGN_CENTER);
        fecha.setSpacingAfter(20);
        document.add(fecha);
    }

    //  RESUMEN EJECUTIVO
    private static void agregarResumenEjecutivo(Document document, 
                                              Map<String, Object> estadisticas,
                                              com.itextpdf.text.Font destacadoFont,
                                              com.itextpdf.text.Font normalFont) throws Exception {
        
        Paragraph titulo = new Paragraph("RESUMEN EJECUTIVO", destacadoFont);
        titulo.setSpacingAfter(10);
        document.add(titulo);

        double totalIngresos = (Double) estadisticas.getOrDefault("totalIngresos", 0.0);
        int totalPagos = (Integer) estadisticas.getOrDefault("totalPagos", 0);
        int pagosValidados = (Integer) estadisticas.getOrDefault("pagosValidados", 0);
        double promedioPago = (Double) estadisticas.getOrDefault("promedioPago", 0.0);

        String resumen = "• Total de Ingresos: $" + df.format(totalIngresos) + "\n" +
                        "• Total de Transacciones: " + totalPagos + " pagos\n" +
                        "• Pagos Validados: " + pagosValidados + " transacciones\n" +
                        "• Ticket Promedio: $" + df.format(promedioPago) + "\n" +
                        "• Tasa de Éxito: " + (totalPagos > 0 ? 
                            String.format("%.1f%%", (pagosValidados * 100.0 / totalPagos)) : "0%");

        Paragraph contenido = new Paragraph(resumen, normalFont);
        contenido.setSpacingAfter(20);
        document.add(contenido);
    }

    //  AGREGAR GRÁFICOS
    private static void agregarGraficos(Document document, 
                                      String rutaGraficoBarras, 
                                      String rutaGraficoPastel,
                                      com.itextpdf.text.Font destacadoFont) throws Exception {
        
        // Gráfico de barras - Ingresos mensuales
        Paragraph tituloBarras = new Paragraph("EVOLUCIÓN DE INGRESOS MENSUALES", destacadoFont);
        tituloBarras.setAlignment(Element.ALIGN_CENTER);
        tituloBarras.setSpacingAfter(10);
        document.add(tituloBarras);

        Image graficoBarras = Image.getInstance(rutaGraficoBarras);
        graficoBarras.setAlignment(Element.ALIGN_CENTER);
        graficoBarras.scaleToFit(500, 300);
        document.add(graficoBarras);

        document.add(new Paragraph(" "));

        // Gráfico de pastel - Estados de pago
        Paragraph tituloPastel = new Paragraph("DISTRIBUCIÓN POR ESTADO DE PAGO", destacadoFont);
        tituloPastel.setAlignment(Element.ALIGN_CENTER);
        tituloPastel.setSpacingAfter(10);
        document.add(tituloPastel);

        Image graficoPastel = Image.getInstance(rutaGraficoPastel);
        graficoPastel.setAlignment(Element.ALIGN_CENTER);
        graficoPastel.scaleToFit(400, 280);
        document.add(graficoPastel);

        document.add(new Paragraph(" "));
    }

    //  PAGOS RECIENTES
    private static void agregarPagosRecientes(Document document, 
                                            List<Map<String, Object>> pagosRecientes,
                                            com.itextpdf.text.Font destacadoFont,
                                            com.itextpdf.text.Font normalFont) throws Exception {
        
        Paragraph titulo = new Paragraph("ÚLTIMOS PAGOS REGISTRADOS", destacadoFont);
        titulo.setSpacingAfter(10);
        document.add(titulo);

        PdfPTable tabla = new PdfPTable(5);
        tabla.setWidthPercentage(100);
        tabla.setSpacingBefore(10);

        // Encabezados
        agregarCeldaTabla(tabla, "ID PAGO", true, COLOR_PRINCIPAL);
        agregarCeldaTabla(tabla, "CLIENTE", true, COLOR_PRINCIPAL);
        agregarCeldaTabla(tabla, "MONTO", true, COLOR_PRINCIPAL);
        agregarCeldaTabla(tabla, "ESTADO", true, COLOR_PRINCIPAL);
        agregarCeldaTabla(tabla, "FECHA", true, COLOR_PRINCIPAL);

        // Datos
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        for (Map<String, Object> pago : pagosRecientes) {
            agregarCeldaTabla(tabla, pago.get("id").toString(), false, null);
            agregarCeldaTabla(tabla, (String) pago.getOrDefault("cliente", "N/A"), false, null);
            agregarCeldaTabla(tabla, "$" + df.format(pago.get("monto")), false, null);
            
            // Color según estado
            String estado = (String) pago.get("estado");
            Color colorEstado = estado.equals("Validado") ? COLOR_EXITOSO : 
                               estado.equals("Pendiente") ? COLOR_PENDIENTE : COLOR_RECHAZADO;
            agregarCeldaTabla(tabla, estado, false, colorEstado);
            
            String fecha = pago.get("fecha") != null ? 
                sdf.format(pago.get("fecha")) : "N/A";
            agregarCeldaTabla(tabla, fecha, false, null);
        }

        document.add(tabla);
        document.add(new Paragraph(" "));
    }

    //  ANÁLISIS MENSUAL
    private static void agregarAnalisisMensual(Document document, 
                                             Map<String, Double> ingresosPorMes,
                                             com.itextpdf.text.Font destacadoFont,
                                             com.itextpdf.text.Font normalFont) throws Exception {
        
        Paragraph titulo = new Paragraph("ANÁLISIS MENSUAL DETALLADO", destacadoFont);
        titulo.setSpacingAfter(10);
        document.add(titulo);

        if (ingresosPorMes.isEmpty()) {
            document.add(new Paragraph("No hay datos suficientes para el análisis mensual.", normalFont));
            return;
        }

        double totalAnual = ingresosPorMes.values().stream().mapToDouble(Double::doubleValue).sum();
        Map.Entry<String, Double> mesMaximo = ingresosPorMes.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .orElse(null);

        String analisis = "• Total Anual: $" + df.format(totalAnual) + "\n" +
                         "• Mejor Mes: " + (mesMaximo != null ? 
                             mesMaximo.getKey() + " ($" + df.format(mesMaximo.getValue()) + ")" : "N/A") + "\n" +
                         "• Promedio Mensual: $" + df.format(totalAnual / ingresosPorMes.size()) + "\n" +
                         "• Meses Analizados: " + ingresosPorMes.size();

        Paragraph contenido = new Paragraph(analisis, normalFont);
        contenido.setSpacingAfter(10);
        document.add(contenido);

        // Tabla de ingresos mensuales
        PdfPTable tablaMeses = new PdfPTable(2);
        tablaMeses.setWidthPercentage(60);
        tablaMeses.setHorizontalAlignment(Element.ALIGN_CENTER);

        agregarCeldaTabla(tablaMeses, "MES", true, COLOR_PRINCIPAL);
        agregarCeldaTabla(tablaMeses, "INGRESOS", true, COLOR_PRINCIPAL);

        ingresosPorMes.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByKey().reversed())
            .forEach(entry -> {
                String[] partes = entry.getKey().split("-");
                String mesLabel = partes[1] + "/" + partes[0];
                agregarCeldaTabla(tablaMeses, mesLabel, false, null);
                agregarCeldaTabla(tablaMeses, "$" + df.format(entry.getValue()), false, null);
            });

        document.add(tablaMeses);
    }

    //  PIE DE PÁGINA
    private static void agregarPiePagina(Document document) throws Exception {
        document.add(new Paragraph(" "));
        Paragraph pie = new Paragraph(
            "--- Sistema de Gestión AndyNails - Módulo de Reportes Financieros ---", 
            FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, com.itextpdf.text.BaseColor.GRAY));
        pie.setAlignment(Element.ALIGN_CENTER);
        document.add(pie);
    }

    //  MÉTODO AUXILIAR PARA CELDAS DE TABLA
    private static void agregarCeldaTabla(PdfPTable tabla, String texto, boolean isHeader, Color color) {
        PdfPCell celda = new PdfPCell(new Phrase(texto));
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        celda.setPadding(6);
        
        if (isHeader) {
            celda.setBackgroundColor(new com.itextpdf.text.BaseColor(
                (color != null ? color : COLOR_PRINCIPAL).getRGB()));
            celda.setPhrase(new Phrase(texto, 
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, com.itextpdf.text.BaseColor.WHITE)));
        } else if (color != null) {
            celda.setBackgroundColor(new com.itextpdf.text.BaseColor(color.getRGB()));
            celda.setPhrase(new Phrase(texto, 
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, com.itextpdf.text.BaseColor.WHITE)));
        }
        
        tabla.addCell(celda);
    }

    //  MÉTODO PRINCIPAL
    public static void main(String[] args) {
        generarReportePagos();
    }
}