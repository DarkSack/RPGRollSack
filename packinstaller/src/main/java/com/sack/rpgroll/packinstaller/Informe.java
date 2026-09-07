package com.sack.rpgroll.packinstaller;

import java.awt.Color;
import java.util.Map;

/**
 * El plan, escrito para que se lea de un vistazo.
 *
 * <p>Antes era un volcado monoespaciado. Con color y jerarquía se distingue de
 * lejos lo que se instala de lo que se omite, que es la única pregunta que se
 * hace quien abre esta ventana.
 *
 * <p>Se genera HTML porque el {@code JEditorPane} de Swing lo entiende y un
 * {@code JTextArea} no admite color. El HTML de Swing es antiguo —viene a ser
 * HTML 3.2— así que nada de flexbox ni de clases: atributos de tabla y estilos
 * en línea, que es lo que sí interpreta.
 */
final class Informe {

    private Informe() {
    }

    /** Escapa lo que venga de un pack: son nombres de archivo de terceros. */
    private static String escapar(String texto) {
        return texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String color(Color color) {
        return Estilo.hex(color);
    }

    /**
     * La pantalla inicial.
     *
     * <p>Sin negritas a media frase. El renderizador de Swing descarta el
     * espacio que precede a un {@code <b>} —y también el que se pone dentro,
     * como {@code &nbsp;}— así que "la carpeta plugins" se lee "la
     * carpetaplugins". Se comprobó dibujando la ventana.
     *
     * <p>El resto del informe sí usa negrita, pero siempre al principio de una
     * línea, donde no hay espacio previo que perder.
     */
    static String bienvenida() {
        return envolver("""
                <p style="color:%s;font-size:13px;">Elige el pack y la carpeta
                plugins de tu servidor, y pulsa Revisar.</p>
                <p style="color:%s;font-size:12px;">Revisar no toca nada: solo mira
                y cuenta. Nada se copia hasta que lo confirmes.</p>
                <p style="color:%s;font-size:12px;">También puedes arrastrar el zip
                del pack sobre esta ventana.</p>
                <p style="color:%s;font-size:12px;">Los plugins que no tengas
                instalados se omiten solos.</p>
                """.formatted(color(Estilo.TEXTO), color(Estilo.TENUE),
                color(Estilo.TENUE), color(Estilo.TENUE)));
    }

    static String error(String mensaje) {
        return envolver("""
                <p style="color:%s;font-size:13px;"><b>No se pudo leer el pack.</b></p>
                <p style="color:%s;font-size:12px;">%s</p>
                """.formatted(color(Estilo.MAL), color(Estilo.TENUE), escapar(mensaje)));
    }

    // ------------------------------------------------------------------

    static String delPlan(Plan plan) {
        StringBuilder html = new StringBuilder();

        html.append(cabecera(plan));
        html.append(avisos(plan));
        html.append(cifras(plan));
        html.append(plugins(plan));
        html.append(archivos(plan));

        return envolver(html.toString());
    }

    private static String cabecera(Plan plan) {
        return """
                <p style="margin:0 0 2px 0;font-size:15px;color:%s;"><b>%s</b></p>
                <p style="margin:0 0 12px 0;font-size:11px;color:%s;">%s</p>
                """.formatted(color(Estilo.TEXTO), escapar(plan.nombrePack()),
                color(Estilo.TENUE), escapar(plan.carpetaPlugins().toString()));
    }

    private static String avisos(Plan plan) {
        if (plan.avisos().isEmpty()) {
            return "";
        }

        StringBuilder html = new StringBuilder();

        for (Plan.Aviso aviso : plan.avisos()) {
            Color tinta = switch (aviso.nivel()) {
                case ERROR -> Estilo.MAL;
                case ADVERTENCIA -> Estilo.AVISO;
                case NOTA -> Estilo.TENUE;
            };

            String etiqueta = switch (aviso.nivel()) {
                case ERROR -> "Error";
                case ADVERTENCIA -> "Aviso";
                case NOTA -> "Nota";
            };

            html.append("""
                    <p style="margin:0 0 5px 0;font-size:12px;color:%s;"><b>%s.</b>&nbsp;<span
                      style="color:%s;">%s</span></p>
                    """.formatted(color(tinta), etiqueta, color(Estilo.TEXTO),
                    escapar(aviso.mensaje())));
        }

        return html + "<div style=\"margin:10px 0;\"></div>";
    }

    /** Las cifras, en una fila: es lo que se mira primero. */
    private static String cifras(Plan plan) {
        Map<Plan.Accion, Integer> resumen = plan.resumen();
        StringBuilder celdas = new StringBuilder();

        for (Plan.Accion accion : Plan.Accion.values()) {
            Integer cuantos = resumen.get(accion);

            if (cuantos == null) {
                continue;
            }

            Color tinta = switch (accion) {
                case NUEVO -> Estilo.BIEN;
                case REEMPLAZA -> Estilo.AVISO;
                default -> Estilo.TENUE;
            };

            celdas.append("""
                    <td valign="top" style="padding:0 22px 0 0;">
                      <span style="font-size:19px;color:%s;"><b>%d</b></span><br>
                      <span style="font-size:10px;color:%s;">%s</span>
                    </td>
                    """.formatted(color(tinta), cuantos, color(Estilo.TENUE),
                    accion.etiqueta().toUpperCase()));
        }

        return "<table cellpadding=\"0\" cellspacing=\"0\"><tr>" + celdas + "</tr></table>"
                + "<div style=\"margin:14px 0;\"></div>";
    }

    private static String plugins(Plan plan) {
        StringBuilder html = new StringBuilder();

        if (!plan.pluginsAfectados().isEmpty()) {
            html.append(titulo("Se instala en"));

            for (String plugin : plan.pluginsAfectados()) {
                html.append(linea(Estilo.BIEN, "+", plugin, null));
            }
        }

        if (!plan.pluginsOmitidos().isEmpty()) {
            html.append(titulo("Se omite"));

            for (String plugin : plan.pluginsOmitidos()) {
                html.append(linea(Estilo.TENUE, "–", plugin, "no instalado"));
            }
        }

        return html.toString();
    }

    private static String archivos(Plan plan) {
        if (plan.aInstalar().isEmpty()) {
            return "";
        }

        StringBuilder html = new StringBuilder();
        html.append(titulo("Archivos (" + Formato.tamano(plan.bytesAEscribir()) + ")"));

        for (Plan.Entrada entrada : plan.aInstalar()) {
            boolean reemplaza = entrada.accion() == Plan.Accion.REEMPLAZA;

            html.append("""
                    <p style="margin:0 0 2px 0;font-size:11px;color:%s;">
                      <span style="color:%s;">%s</span>&nbsp;%s
                    </p>
                    """.formatted(color(Estilo.TENUE),
                    color(reemplaza ? Estilo.AVISO : Estilo.BIEN),
                    reemplaza ? "~" : "+",
                    escapar(entrada.archivo().rutaRelativa())));
        }

        return html.toString();
    }

    // ------------------------------------------------------------------

    static String resultado(Installer.Resultado resultado) {
        StringBuilder html = new StringBuilder();

        boolean bien = resultado.todoBien();

        html.append("""
                <p style="margin:0 0 12px 0;font-size:15px;color:%s;"><b>%s</b></p>
                """.formatted(color(bien ? Estilo.BIEN : Estilo.MAL),
                bien ? "Instalación terminada" : "Terminó con fallos"));

        html.append("""
                <table cellpadding="0" cellspacing="0"><tr>
                  <td valign="top" style="padding:0 22px 0 0;">
                    <span style="font-size:19px;color:%s;"><b>%d</b></span><br>
                    <span style="font-size:10px;color:%s;">COPIADOS</span>
                  </td>
                  <td valign="top" style="padding:0 22px 0 0;">
                    <span style="font-size:19px;color:%s;"><b>%d</b></span><br>
                    <span style="font-size:10px;color:%s;">REEMPLAZADOS</span>
                  </td>
                  <td valign="top">
                    <span style="font-size:19px;color:%s;"><b>%d</b></span><br>
                    <span style="font-size:10px;color:%s;">OMITIDOS</span>
                  </td>
                </tr></table>
                <div style="margin:14px 0;"></div>
                """.formatted(
                color(Estilo.BIEN), resultado.copiados(), color(Estilo.TENUE),
                color(Estilo.AVISO), resultado.reemplazados(), color(Estilo.TENUE),
                color(Estilo.TENUE), resultado.omitidos(), color(Estilo.TENUE)));

        if (resultado.copiaDeSeguridad() != null) {
            html.append(titulo("Copia de los originales"));
            html.append("""
                    <p style="margin:0 0 10px 0;font-size:11px;color:%s;">%s</p>
                    """.formatted(color(Estilo.TENUE),
                    escapar(resultado.copiaDeSeguridad().toString())));
        }

        if (!bien) {
            html.append(titulo("No se pudieron copiar"));

            for (String fallo : resultado.fallos()) {
                html.append(linea(Estilo.MAL, "×", fallo, null));
            }

            return envolver(html.toString());
        }

        html.append("""
                <p style="margin:4px 0 0 0;font-size:12px;color:%s;">
                  Reinicia el servidor para que los plugins lean el contenido nuevo.
                </p>
                """.formatted(color(Estilo.TEXTO)));

        return envolver(html.toString());
    }

    // ------------------------------------------------------------------

    private static String titulo(String texto) {
        return """
                <p style="margin:12px 0 5px 0;font-size:10px;color:%s;"><b>%s</b></p>
                """.formatted(color(Estilo.TENUE), escapar(texto.toUpperCase()));
    }

    private static String linea(Color tinta, String marca, String texto, String coletilla) {
        return """
                <p style="margin:0 0 3px 0;font-size:12px;color:%s;">
                  <span style="color:%s;">%s</span>&nbsp;%s%s
                </p>
                """.formatted(color(Estilo.TEXTO), color(tinta), marca, escapar(texto),
                coletilla == null ? ""
                        : "<span style=\"color:" + color(Estilo.TENUE) + ";\"> · "
                        + escapar(coletilla) + "</span>");
    }

    private static String envolver(String cuerpo) {
        return """
                <html><body style="margin:0;font-family:'Segoe UI',sans-serif;background:%s;">
                %s
                </body></html>
                """.formatted(color(Estilo.PANEL), cuerpo);
    }
}
