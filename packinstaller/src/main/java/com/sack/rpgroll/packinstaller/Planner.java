package com.sack.rpgroll.packinstaller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Construye el {@link Plan}. No escribe absolutamente nada.
 *
 * <p>Toda la validación vive acá y ocurre antes de tocar el disco, para que el
 * peor final posible sea una lista de problemas en pantalla y un servidor
 * exactamente igual que antes de abrir el programa.
 */
final class Planner {

    private final PackSource pack;
    private final Path carpetaPlugins;

    /**
     * Carpeta que envuelve al pack dentro del zip, si la hay.
     *
     * <p>Vacío cuando el pack está en la raíz, que es lo esperado.
     */
    private String envoltorio = "";

    Planner(PackSource pack, Path carpetaPlugins) {
        this.pack = pack;
        this.carpetaPlugins = carpetaPlugins;
    }

    Plan planificar() throws IOException {
        List<Plan.Aviso> avisos = new ArrayList<>();
        List<Plan.Entrada> entradas = new ArrayList<>();

        validarCarpetaPlugins(avisos);

        List<PackSource.Archivo> archivos = pack.archivos();

        if (archivos.isEmpty()) {
            avisos.add(error("El pack no contiene ningún archivo."));
            return new Plan(pack.nombre(), carpetaPlugins, entradas, avisos);
        }

        envoltorio = detectarEnvoltorio(archivos);

        if (!envoltorio.isEmpty()) {
            avisos.add(new Plan.Aviso(Plan.Aviso.Nivel.NOTA,
                    "El pack viene dentro de la carpeta '" + envoltorio + "'. Se entra en ella sola."));
        }

        Set<String> carpetasVistas = new LinkedHashSet<>();

        for (PackSource.Archivo archivo : archivos) {
            if (!PackSource.rutaSegura(archivo.rutaRelativa())) {
                avisos.add(error("Ruta peligrosa en el pack, se ignora: " + archivo.rutaRelativa()));
                continue;
            }

            String relativa = sinEnvoltorio(archivo.rutaRelativa());

            if (relativa.isEmpty()) {
                continue;
            }

            String carpeta = carpetaRaizDe(relativa);

            // Un archivo suelto en la raíz es documentación (LEEME.md y
            // compañía), no contenido de ningún plugin. Se lista para que se
            // vea que no se perdió, pero no se instala: nadie quiere un
            // LEEME.md dentro de plugins/.
            if (carpeta.isEmpty()) {
                entradas.add(new Plan.Entrada(archivo, "—", null, Plan.Accion.DOCUMENTACION));
                continue;
            }

            if (carpetasVistas.add(carpeta)) {
                revisarNombreDeCarpeta(carpeta, avisos);
            }

            Path destinoPlugin = carpetaPlugins.resolve(carpeta);

            if (!Files.isDirectory(destinoPlugin)) {
                entradas.add(new Plan.Entrada(archivo, carpeta, null, Plan.Accion.SIN_PLUGIN));
                continue;
            }

            Path destino = destinoPlugin.resolve(dentroDelPluginDe(relativa));

            // Cinturón y tirantes: aunque `rutaSegura` ya filtró los "..", se
            // comprueba el resultado final. Un enlace simbólico dentro de
            // plugins/ podría sacar la ruta fuera sin que aparezca ningún "..".
            if (!destino.normalize().startsWith(carpetaPlugins.normalize())) {
                avisos.add(error("Destino fuera de la carpeta de plugins: " + archivo.rutaRelativa()));
                continue;
            }

            entradas.add(new Plan.Entrada(archivo, carpeta, destino, decidir(archivo, destino)));
        }

        avisarSobreOmitidos(entradas, avisos);
        comprobarEspacio(entradas, avisos);

        return new Plan(pack.nombre(), carpetaPlugins, entradas, avisos);
    }

    private Plan.Accion decidir(PackSource.Archivo archivo, Path destino) throws IOException {
        if (!Files.exists(destino)) {
            return Plan.Accion.NUEVO;
        }

        // Reinstalar el mismo pack no debería tocar nada. Sin esta comprobación,
        // cada reinstalación haría una copia de seguridad de archivos idénticos
        // y llenaría el servidor de carpetas de respaldo inútiles.
        return mismoContenido(archivo, destino) ? Plan.Accion.IDENTICO : Plan.Accion.REEMPLAZA;
    }

    private boolean mismoContenido(PackSource.Archivo archivo, Path destino) throws IOException {
        if (Files.size(destino) != archivo.tamano() && archivo.tamano() >= 0) {
            return false;
        }

        return Arrays.equals(sha256(pack.leer(archivo)), sha256(Files.readAllBytes(destino)));
    }

    private static byte[] sha256(byte[] datos) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(datos);
        } catch (NoSuchAlgorithmException imposible) {
            // SHA-256 es obligatorio en toda implementación de Java.
            throw new IllegalStateException(imposible);
        }
    }

    // ------------------------------------------------------------------
    // Packs envueltos en una carpeta
    // ------------------------------------------------------------------

    /**
     * Detecta el caso en que el zip incluye la carpeta del pack.
     *
     * <p>Es lo que produce «Enviar a → Carpeta comprimida» de Windows y casi
     * cualquier herramienta gráfica: en vez de {@code RPGRoll-Mobs/...}, las
     * entradas quedan como {@code reino-no-muerto/RPGRoll-Mobs/...}. Sin
     * tratarlo, el instalador no reconoce ninguna carpeta de plugin y no copia
     * absolutamente nada — con un aviso que suena a que el pack está mal.
     *
     * <p>Solo se entra cuando no hay ninguna duda: ninguna carpeta de la raíz
     * es un plugin conocido, hay <b>exactamente una</b> carpeta, y dentro de
     * ella sí aparece alguno. Con esas tres condiciones no se puede confundir
     * con un pack legítimo.
     *
     * @return el nombre de la carpeta envolvente, o cadena vacía si no la hay
     */
    private static String detectarEnvoltorio(List<PackSource.Archivo> archivos) {
        Set<String> raiz = new LinkedHashSet<>();

        for (PackSource.Archivo archivo : archivos) {
            String carpeta = archivo.carpetaRaiz();

            if (!carpeta.isEmpty()) {
                raiz.add(carpeta);
            }
        }

        if (raiz.size() != 1) {
            return "";
        }

        String unica = raiz.iterator().next();

        if (RpgRollPlugins.esConocido(unica)) {
            return "";
        }

        String prefijo = unica + "/";

        for (PackSource.Archivo archivo : archivos) {
            String resto = archivo.rutaRelativa();

            if (resto.startsWith(prefijo)) {
                String dentro = resto.substring(prefijo.length());
                int corte = dentro.indexOf('/');

                if (corte > 0 && RpgRollPlugins.esConocido(dentro.substring(0, corte))) {
                    return unica;
                }
            }
        }

        return "";
    }

    private String sinEnvoltorio(String ruta) {
        if (envoltorio.isEmpty()) {
            return ruta;
        }

        String prefijo = envoltorio + "/";
        return ruta.startsWith(prefijo) ? ruta.substring(prefijo.length()) : "";
    }

    private static String carpetaRaizDe(String ruta) {
        int corte = ruta.indexOf('/');
        return corte < 0 ? "" : ruta.substring(0, corte);
    }

    private static String dentroDelPluginDe(String ruta) {
        int corte = ruta.indexOf('/');
        return corte < 0 ? ruta : ruta.substring(corte + 1);
    }

    // ------------------------------------------------------------------
    // Validaciones
    // ------------------------------------------------------------------

    private void validarCarpetaPlugins(List<Plan.Aviso> avisos) {
        if (!Files.exists(carpetaPlugins)) {
            avisos.add(error("La carpeta no existe: " + carpetaPlugins));
            return;
        }

        if (!Files.isDirectory(carpetaPlugins)) {
            avisos.add(error("Eso no es una carpeta: " + carpetaPlugins));
            return;
        }

        if (!Files.isWritable(carpetaPlugins)) {
            avisos.add(error("No hay permiso de escritura en " + carpetaPlugins));
            return;
        }

        if (!pareceCarpetaDePlugins()) {
            // Es un error y no una advertencia a propósito. Apuntar a la carpeta
            // equivocada y volcar 116 archivos sueltos deja un desorden que hay
            // que limpiar a mano, archivo por archivo.
            avisos.add(error(
                    "Esto no parece la carpeta 'plugins' de un servidor: no hay ningún .jar "
                            + "ni ninguna carpeta de plugin dentro. Elige la carpeta 'plugins' "
                            + "que está junto a server.jar."));
        }
    }

    /** Un .jar o una carpeta de plugin conocida bastan para reconocerla. */
    private boolean pareceCarpetaDePlugins() {
        try (var contenido = Files.list(carpetaPlugins)) {
            return contenido.anyMatch(hijo -> {
                String nombre = hijo.getFileName().toString();
                return nombre.toLowerCase(java.util.Locale.ROOT).endsWith(".jar")
                        || (Files.isDirectory(hijo) && RpgRollPlugins.esConocido(nombre));
            });
        } catch (IOException error) {
            return false;
        }
    }

    private void revisarNombreDeCarpeta(String carpeta, List<Plan.Aviso> avisos) {
        if (RpgRollPlugins.esConocido(carpeta)) {
            return;
        }

        String correcto = RpgRollPlugins.corregirMayusculas(carpeta);

        if (correcto != null) {
            avisos.add(new Plan.Aviso(Plan.Aviso.Nivel.ADVERTENCIA,
                    "El pack trae la carpeta '" + carpeta + "', pero el plugin se llama '" + correcto
                            + "'. En Windows daría igual; en el Linux de un servidor, no, "
                            + "y ese contenido no se instalaría nunca."));
            return;
        }

        avisos.add(new Plan.Aviso(Plan.Aviso.Nivel.ADVERTENCIA,
                "'" + carpeta + "' no es ningún plugin de RPGRoll conocido. Si es un addon nuevo, "
                        + "no pasa nada; si es una errata, ese contenido se perdería en silencio."));
    }

    private void avisarSobreOmitidos(List<Plan.Entrada> entradas, List<Plan.Aviso> avisos) {
        entradas.stream()
                .filter(entrada -> entrada.accion() == Plan.Accion.SIN_PLUGIN)
                .map(Plan.Entrada::plugin)
                .distinct()
                .sorted()
                .forEach(plugin -> avisos.add(new Plan.Aviso(Plan.Aviso.Nivel.NOTA,
                        plugin + " no está instalado en este servidor: su contenido se omite.")));
    }

    private void comprobarEspacio(List<Plan.Entrada> entradas, List<Plan.Aviso> avisos) {
        long necesarios = entradas.stream()
                .filter(entrada -> entrada.accion().instala())
                .mapToLong(entrada -> entrada.archivo().tamano())
                .sum();

        try {
            long libres = Files.getFileStore(carpetaPlugins).getUsableSpace();

            // Se pide el doble: las copias de seguridad de lo que se reemplaza
            // ocupan aparte, y quedarse sin disco a mitad de una copia es la
            // forma más fea de fallar.
            if (libres < necesarios * 2) {
                avisos.add(error("No hay espacio suficiente: hacen falta unos "
                        + Formato.tamano(necesarios * 2) + " y quedan " + Formato.tamano(libres) + "."));
            }
        } catch (IOException error) {
            // Saber el espacio libre no siempre es posible (unidades de red).
            // No es motivo para impedir la instalación.
            avisos.add(new Plan.Aviso(Plan.Aviso.Nivel.NOTA,
                    "No se pudo comprobar el espacio libre en disco."));
        }
    }

    private static Plan.Aviso error(String mensaje) {
        return new Plan.Aviso(Plan.Aviso.Nivel.ERROR, mensaje);
    }
}
