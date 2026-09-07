package com.sack.rpgroll.packinstaller;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * Modo consola, para servidores sin escritorio.
 *
 * <pre>
 *   java -jar RPGRoll-PackInstaller.jar &lt;pack&gt; &lt;carpeta plugins&gt; [--instalar]
 * </pre>
 *
 * <p>Sin {@code --instalar} solo enseña el plan y no toca nada. Ese es el
 * valor por defecto a propósito: en una consola no hay ventana de confirmación,
 * así que la primera ejecución tiene que ser inofensiva. Copiar archivos sobre
 * un servidor ajeno debe costar una palabra de más, no una de menos.
 */
final class Cli {

    private Cli() {
    }

    static String ayuda() {
        return """
                Instalador de packs de RPGRoll

                  java -jar RPGRoll-PackInstaller.jar <pack> <carpeta-plugins> [--instalar]

                  <pack>            carpeta del pack, o su .zip
                  <carpeta-plugins> la carpeta 'plugins' del servidor

                  --instalar        copia de verdad. Sin esto solo se muestra
                                    lo que haría, sin tocar nada.

                Sin argumentos y con escritorio disponible, se abre la ventana.
                """;
    }

    static int ejecutar(String[] args) {
        if (args.length == 1 && (args[0].equals("--help") || args[0].equals("-h"))) {
            System.out.println(ayuda());
            return 0;
        }

        if (args.length < 2) {
            System.err.println(ayuda());
            return 2;
        }

        Path origen = Path.of(args[0]);
        Path plugins = Path.of(args[1]);
        boolean instalar = args.length > 2 && args[2].equals("--instalar");

        try (PackSource pack = PackSource.desde(origen)) {
            Plan plan = new Planner(pack, plugins).planificar();

            imprimirPlan(plan);

            if (plan.tieneErrores()) {
                System.err.println();
                System.err.println("No se instaló nada: hay errores que resolver primero.");
                return 1;
            }

            if (!instalar) {
                System.out.println();
                System.out.println("Esto es solo una simulación. Añade --instalar para aplicarlo.");
                return 0;
            }

            if (plan.aInstalar().isEmpty()) {
                System.out.println();
                System.out.println("No hay nada que hacer: todo está ya en su sitio.");
                return 0;
            }

            Installer.Resultado resultado = new Installer(pack).instalar(plan, ruta -> {
            });

            imprimirResultado(resultado);
            return resultado.todoBien() ? 0 : 1;
        } catch (IOException error) {
            System.err.println("Error: " + error.getMessage());
            return 1;
        }
    }

    private static void imprimirPlan(Plan plan) {
        System.out.println("Pack:    " + plan.nombrePack());
        System.out.println("Destino: " + plan.carpetaPlugins());
        System.out.println();

        for (Plan.Aviso aviso : plan.avisos()) {
            String prefijo = switch (aviso.nivel()) {
                case ERROR -> "  ERROR  ";
                case ADVERTENCIA -> "  AVISO  ";
                case NOTA -> "  nota   ";
            };
            System.out.println(prefijo + aviso.mensaje());
        }

        if (!plan.avisos().isEmpty()) {
            System.out.println();
        }

        Map<Plan.Accion, Integer> resumen = plan.resumen();

        for (Plan.Accion accion : Plan.Accion.values()) {
            Integer cuantos = resumen.get(accion);

            if (cuantos != null) {
                System.out.printf("  %-16s %s%n", accion.etiqueta(), Formato.archivos(cuantos));
            }
        }

        if (!plan.pluginsAfectados().isEmpty()) {
            System.out.println();
            System.out.println("  Se instala en: " + String.join(", ", plan.pluginsAfectados()));
        }

        if (!plan.pluginsOmitidos().isEmpty()) {
            System.out.println("  Se omite:      " + String.join(", ", plan.pluginsOmitidos())
                    + " (no instalados)");
        }
    }

    private static void imprimirResultado(Installer.Resultado resultado) {
        System.out.println();
        System.out.println("Copiados:     " + resultado.copiados());
        System.out.println("Reemplazados: " + resultado.reemplazados());

        if (resultado.copiaDeSeguridad() != null) {
            System.out.println("Respaldo en:  " + resultado.copiaDeSeguridad());
        }

        if (!resultado.todoBien()) {
            System.out.println();
            System.out.println("Fallaron " + Formato.archivos(resultado.fallos().size()) + ":");
            resultado.fallos().forEach(fallo -> System.out.println("  - " + fallo));
            return;
        }

        System.out.println();
        System.out.println("Listo. Reinicia el servidor para que los plugins lean el contenido nuevo.");
    }
}
