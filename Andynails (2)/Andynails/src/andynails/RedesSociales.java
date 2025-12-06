package andynails;

import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

/**
 *
 * @author mgmmo
 */
public class RedesSociales {
    
    // URLs de tus redes sociales
    private static final String URL_INSTAGRAM = "https://www.instagram.com/andynails378?igsh=eW5haTJ4OWY3Mm14";
    private static final String URL_WHATSAPP = "https://wa.me/8442756590";
    private static final String URL_FACEBOOK = "https://www.facebook.com/share/17LcvkhEJT/";
    
    /**
     * Configura las tres redes sociales de una vez
     */
    public static void configurarRedesSociales(JLabel labelInstagram, 
                                             JLabel labelWhatsApp, 
                                             JLabel labelFacebook) {
        configurarInstagram(labelInstagram);
        configurarWhatsApp(labelWhatsApp);
        configurarFacebook(labelFacebook);
    }
    
    /**
     * Configura solo Instagram
     */
    public static void configurarInstagram(JLabel label) {
        configurarLabel(label, URL_INSTAGRAM, "Síguenos en Instagram");
    }
    
    /**
     * Configura solo WhatsApp
     */
    public static void configurarWhatsApp(JLabel label) {
        configurarLabel(label, URL_WHATSAPP, "Escríbenos por WhatsApp");
    }
    
    /**
     * Configura solo Facebook
     */
    public static void configurarFacebook(JLabel label) {
        configurarLabel(label, URL_FACEBOOK, "Visítanos en Facebook");
    }
    
    /**
     * Configura con iconos - MÉTODO PRINCIPAL QUE DEBES USAR
     */
    public static void configurarRedesSocialesConIconos(JLabel labelInstagram,
                                                       JLabel labelWhatsApp, 
                                                       JLabel labelFacebook) {
        System.out.println("=== INICIANDO CARGA DE ICONOS ===");
        configurarLabelConIcono(labelInstagram, URL_INSTAGRAM, "Instagram", "icon_inst.jpeg");
        configurarLabelConIcono(labelWhatsApp, URL_WHATSAPP, "WhatsApp", "icon_Whatsapp.jpeg");
        configurarLabelConIcono(labelFacebook, URL_FACEBOOK, "Facebook", "icon_Facebook.jpeg");
        System.out.println("=== CARGA DE ICONOS COMPLETADA ===");
    }
    
    /**
     * Método privado para configuración común de todos los labels
     */
    private static void configurarLabel(JLabel label, String url, String tooltip) {
        if (label == null) {
            System.out.println("Error: El label es null");
            return;
        }
        
        // Configuración básica
        label.setCursor(new Cursor(Cursor.HAND_CURSOR));
        label.setToolTipText(tooltip);
        
        // Agregar el comportamiento de click
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                abrirEnlace(url);
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                // Efecto hover - cambia a blanco
                label.setForeground(java.awt.Color.WHITE);
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                // Vuelve al color original morado claro
                label.setForeground(new java.awt.Color(200, 200, 255));
            }
        });
    }
    
    /**
     * Método para configurar label con icono - CON RUTAS CORREGIDAS
     */
    private static void configurarLabelConIcono(JLabel label, String url, String tooltip, String nombreArchivo) {
        System.out.println("Configurando: " + tooltip + " con archivo: " + nombreArchivo);
        
        // Intentar cargar la imagen desde la carpeta img
        boolean imagenCargada = cargarImagenDesdeImg(label, nombreArchivo, tooltip);
        
        // Si no se cargó, usar texto
        if (!imagenCargada) {
            System.out.println("No se pudo cargar imagen para: " + tooltip);
            establecerTextoFallback(label, tooltip);
        }
        
        // Configurar alineación específica
        configurarAlineacion(label);
        
        // Configurar comportamiento de click
        configurarLabel(label, url, tooltip);
    }
    
    /**
     * Cargar imagen desde la carpeta img en src
     */
    private static boolean cargarImagenDesdeImg(JLabel label, String nombreArchivo, String tooltip) {
        try {
            // Ruta corregida: desde la carpeta img en src
            String ruta = "/img/" + nombreArchivo;
            System.out.println(" Intentando cargar desde: " + ruta);
            
            URL imgURL = RedesSociales.class.getResource(ruta);
            System.out.println(" URL encontrada: " + imgURL);
            
            if (imgURL != null) {
                ImageIcon iconoOriginal = new ImageIcon(imgURL);
                System.out.println(" Tamaño original: " + iconoOriginal.getIconWidth() + "x" + iconoOriginal.getIconHeight());
                
                // Redimensionar a tamaño pequeño (16x16)
                ImageIcon iconoRedimensionado = new ImageIcon(iconoOriginal.getImage()
                        .getScaledInstance(16, 16, java.awt.Image.SCALE_SMOOTH));
                
                label.setIcon(iconoRedimensionado);
                label.setText(""); // Limpiar texto
                System.out.println(" Imagen cargada exitosamente: " + ruta);
                return true;
            } else {
                System.out.println(" No se encontró en: " + ruta);
                
                // Intentar alternativa: desde sistema de archivos
                return cargarImagenDesdeSistemaArchivos(label, nombreArchivo, tooltip);
            }
            
        } catch (Exception e) {
            System.out.println(" Error cargando imagen: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Intentar cargar imagen desde sistema de archivos como último recurso
     */
    private static boolean cargarImagenDesdeSistemaArchivos(JLabel label, String nombreArchivo, String tooltip) {
        try {
            // Ruta desde la raíz del proyecto
            String rutaProyecto = System.getProperty("user.dir");
            String rutaImagen = rutaProyecto + "/src/img/" + nombreArchivo;
            
            System.out.println(" Intentando cargar desde sistema: " + rutaImagen);
            
            java.io.File archivo = new java.io.File(rutaImagen);
            if (archivo.exists()) {
                ImageIcon iconoOriginal = new ImageIcon(rutaImagen);
                System.out.println(" Tamaño desde sistema: " + iconoOriginal.getIconWidth() + "x" + iconoOriginal.getIconHeight());
                
                ImageIcon iconoRedimensionado = new ImageIcon(iconoOriginal.getImage()
                        .getScaledInstance(16, 16, java.awt.Image.SCALE_SMOOTH));
                
                label.setIcon(iconoRedimensionado);
                label.setText("");
                System.out.println(" Imagen cargada desde sistema: " + rutaImagen);
                return true;
            } else {
                System.out.println(" Archivo no existe en sistema: " + rutaImagen);
                return false;
            }
            
        } catch (Exception e) {
            System.out.println(" Error cargando desde sistema: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Configurar alineación específica: alignmentX=0.0, alignmentY=0.5
     */
    private static void configurarAlineacion(JLabel label) {
        // alignmentX=0.0 (LEFT) - alignmentY=0.5 (CENTER)
        label.setHorizontalAlignment(JLabel.LEFT);
        label.setVerticalAlignment(JLabel.CENTER);
        
        // Para mayor control, también puedes usar:
        label.setAlignmentX(0.0f); // LEFT alignment
        label.setAlignmentY(0.5f); // CENTER alignment
    }
    
    /**
     * Fallback para texto
     */
    private static void establecerTextoFallback(JLabel label, String tooltip) {
        String texto = "";
        switch(tooltip) {
            case "Instagram": 
                texto = "INS"; 
                break;
            case "WhatsApp": 
                texto = "WPP"; 
                break;
            case "Facebook": 
                texto = "FACE"; 
                break;
        }
        label.setText(texto);
        System.out.println(" Usando texto como fallback: " + texto);
        
        // Aplicar también la alineación al texto
        configurarAlineacion(label);
    }
    
    /**
     * Método para abrir enlaces en el navegador
     */
    private static void abrirEnlace(String url) {
        try {
            Desktop desktop = Desktop.getDesktop();
            URI uri = new URI(url);
            desktop.browse(uri);
        } catch (Exception ex) {
            System.err.println("Error al abrir enlace: " + url);
            ex.printStackTrace();
        }
    }
    
    /**
     * Método para verificar que las imágenes existen
     */
    public static void verificarImagenes() {
        System.out.println("=== VERIFICANDO IMÁGENES ===");
        String[] archivos = {"icon_inst.jpeg", "icon_Whatsapp.jpeg", "icon_Facebook.jpeg"};
        
        for (String archivo : archivos) {
            String ruta = "/img/" + archivo;
            URL url = RedesSociales.class.getResource(ruta);
            if (url != null) {
                System.out.println("✅ " + archivo + " - ENCONTRADO");
            } else {
                System.out.println("❌ " + archivo + " - NO ENCONTRADO");
            }
        }
        System.out.println("============================");
    }
}