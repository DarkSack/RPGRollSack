package com.sack.rpgroll.common.resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Decide qué hacer con cada archivo que un plugin trae dentro del JAR cuando
 * ya puede existir una copia en la carpeta de datos.
 * <p>
 * Antes solo se copiaba lo que faltaba. Eso protegía las ediciones del
 * administrador, pero también hacía que <b>ninguna actualización de un
 * archivo de ejemplo llegara nunca</b> a un servidor que ya lo tuviera: un
 * comprador que actualizaba seguía con los ejemplos de su primera versión,
 * sin ninguna señal de que hubiera otros.
 * <p>
 * La diferencia entre "el admin lo editó" y "es mi copia vieja" la da un
 * registro con la huella SHA-256 de lo que el plugin escribió la última vez:
 * <ul>
 *   <li>Si lo que hay en disco coincide con esa huella, nadie lo tocó: se
 *       reemplaza por la versión nueva sin preguntar.</li>
 *   <li>Si no coincide, alguien lo editó: <b>no se toca</b>. La versión nueva
 *       se deja aparte, en {@code .rpgroll/actualizaciones/}, para comparar.</li>
 *   <li>Sin registro —servidores anteriores a esto— no hay forma de saber
 *       quién escribió el archivo, así que se asume lo prudente: si coincide
 *       con el JAR se adopta, y si no, se trata como editado.</li>
 * </ul>
 * No depende de Bukkit: recibe bytes y una carpeta, y eso se puede probar
 * sin servidor.
 */
public final class ResourceSync {

    /** Lo que pasó con un archivo, para poder resumirlo en una línea de log. */
    public enum Outcome {
        /** No existía: se escribió. */
        CREATED,
        /** Existía sin tocar y el JAR traía una versión distinta: se reemplazó. */
        UPDATED,
        /**
         * Editado por el admin y el JAR trae otra versión: se dejó aparte. Solo
         * se devuelve la vez que se aparta —o se cambia— esa versión; en los
         * arranques siguientes es {@link #UNCHANGED}, para no repetir el mismo
         * aviso en cada reinicio hasta que nadie lo lea.
         */
        KEPT_EDITED,
        /** Ya estaba al día, o editado sin que haya nada nuevo que ofrecer. */
        UNCHANGED
    }

    /** Carpeta interna. El punto la oculta y los cargadores de contenido no la leen. */
    static final String INTERNAL_DIR = ".rpgroll";
    static final String MANIFEST = "recursos.properties";
    static final String UPDATES_DIR = "actualizaciones";

    private final Path dataFolder;
    private final Path manifestFile;
    private final Properties manifest = new Properties();

    public ResourceSync(Path dataFolder) {
        this.dataFolder = dataFolder;
        this.manifestFile = dataFolder.resolve(INTERNAL_DIR).resolve(MANIFEST);

        if (Files.isRegularFile(manifestFile)) {
            try (InputStream in = Files.newInputStream(manifestFile)) {
                manifest.load(in);
            } catch (IOException e) {
                // Un registro ilegible equivale a no tener registro: se cae al
                // modo prudente, que nunca pisa nada. Peor sería no arrancar.
                manifest.clear();
            }
        }
    }

    /**
     * @param relativePath ruta dentro de la carpeta de datos, con {@code /}
     * @param packaged     el contenido que trae el JAR
     */
    public Outcome sync(String relativePath, byte[] packaged) throws IOException {

        Path destination = dataFolder.resolve(relativePath);
        String packagedHash = sha256(packaged);

        if (!Files.exists(destination)) {
            write(destination, packaged);
            manifest.setProperty(relativePath, packagedHash);
            return Outcome.CREATED;
        }

        String currentHash = sha256(Files.readAllBytes(destination));
        String recordedHash = manifest.getProperty(relativePath);

        if (currentHash.equals(packagedHash)) {
            // Al día. Si había una versión nueva apartada, ya se aplicó.
            manifest.setProperty(relativePath, packagedHash);
            Files.deleteIfExists(updateCopy(relativePath));
            return Outcome.UNCHANGED;
        }

        if (currentHash.equals(recordedHash)) {
            // Es la copia que escribimos nosotros y nadie la tocó.
            write(destination, packaged);
            manifest.setProperty(relativePath, packagedHash);
            return Outcome.UPDATED;
        }

        if (packagedHash.equals(recordedHash)) {
            // Editado por el admin, pero el JAR no trae nada nuevo desde la
            // última vez: no hay nada que ofrecerle.
            return Outcome.UNCHANGED;
        }

        // Editado (o de antes del registro) y con versión nueva en el JAR.
        Path copy = updateCopy(relativePath);

        if (Files.exists(copy) && Arrays.equals(Files.readAllBytes(copy), packaged)) {
            // Ya estaba apartada y ya se avisó.
            return Outcome.UNCHANGED;
        }

        write(copy, packaged);
        return Outcome.KEPT_EDITED;
    }

    /** Guarda el registro. Ordenado, para que se pueda leer y comparar a mano. */
    public void save() throws IOException {

        Files.createDirectories(manifestFile.getParent());

        Properties ordered = new Properties() {
            @Override
            public synchronized java.util.Set<java.util.Map.Entry<Object, Object>> entrySet() {
                return new java.util.LinkedHashSet<>(new TreeMap<>(manifest).entrySet());
            }
        };
        ordered.putAll(manifest);

        try (OutputStream out = Files.newOutputStream(manifestFile)) {
            ordered.store(out, "Huella SHA-256 de cada archivo tal como lo escribio el plugin. No editar.");
        }
    }

    /** Dónde se deja la versión nueva de un archivo que el admin editó. */
    public Path updateCopy(String relativePath) {
        return dataFolder.resolve(INTERNAL_DIR).resolve(UPDATES_DIR).resolve(relativePath);
    }

    private static void write(Path file, byte[] content) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(file, content);
    }

    static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 es obligatorio en toda JVM: esto no puede pasar.
            throw new UncheckedIOException(new IOException(e));
        }
    }
}
