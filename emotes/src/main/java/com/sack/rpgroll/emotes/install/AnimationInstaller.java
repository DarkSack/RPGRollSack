package com.sack.rpgroll.emotes.install;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Copia las animaciones a la carpeta {@code players/} de BetterModel: la de fábrica, con los bytes del jar,
 * y los {@code .bbmodel} de {@code plugins/RPGRoll-Emotes/animations/} (los de los packs).
 * <p>
 * Guarda en {@code data/installed-hashes.yml} el hash de lo que instaló. Un destino que falta o que sigue
 * siendo la copia del plugin se reemplaza; uno que el admin editó se deja como está y se avisa.
 * No recarga BetterModel: de eso se encarga {@code EmotesPlugin}, para que esta clase no dependa de él.
 */
public final class AnimationInstaller {

    /** Nombre reservado del modelo de fábrica: solo el del jar se instala con él. */
    public static final String FACTORY_FILE = "rpgroll_emotes.bbmodel";

    private static final String EXTENSION = ".bbmodel";

    private final File pluginData;
    private final File betterModelPlayers;
    private final byte[] factoryModel;
    private final Consumer<String> warn;

    /**
     * @param factoryModel el {@code animations/rpgroll_emotes.bbmodel} del jar, o null si no viene
     */
    public AnimationInstaller(File pluginData, File betterModelPlayers, byte[] factoryModel, Consumer<String> warn) {
        this.pluginData = pluginData;
        this.betterModelPlayers = betterModelPlayers;
        this.factoryModel = factoryModel;
        this.warn = warn;
    }

    /** @return true si copió o reemplazó algún archivo (hay que recargar BetterModel) */
    public boolean install() {
        Map<String, byte[]> sources = sources();
        if (sources.isEmpty()) {
            return false;
        }
        File hashesFile = new File(pluginData, "data/installed-hashes.yml");
        YamlConfiguration hashes = new YamlConfiguration();
        // Los nombres de archivo llevan puntos: con el separador por defecto se partirían en secciones.
        hashes.options().pathSeparator('/');
        if (!loadHashes(hashesFile, hashes)) {
            return false;
        }

        boolean changed = false;
        for (Map.Entry<String, byte[]> source : sources.entrySet()) {
            String name = source.getKey();
            Path target = betterModelPlayers.toPath().resolve(name);
            try {
                String newHash = sha256(source.getValue());
                if (Files.exists(target)) {
                    String current = sha256(Files.readAllBytes(target));
                    if (current.equals(newHash)) {
                        hashes.set(name, newHash);
                        continue;
                    }
                    if (!current.equals(hashes.getString(name))) {
                        warn.accept("La animación " + target + " tiene cambios tuyos y no se tocó. Si nunca la"
                                + " editaste, bórrala y se vuelve a copiar la versión nueva.");
                        continue;
                    }
                }
                Files.createDirectories(betterModelPlayers.toPath());
                Files.write(target, source.getValue());
                hashes.set(name, newHash);
                changed = true;
            } catch (IOException e) {
                warn.accept("No se pudo instalar la animación " + name + ": " + e.getMessage());
            }
        }

        try {
            Files.createDirectories(hashesFile.getParentFile().toPath());
            hashes.save(hashesFile);
        } catch (IOException e) {
            warn.accept("No se pudo guardar " + hashesFile + ": " + e.getMessage());
        }
        return changed;
    }

    /** La de fábrica primero; luego las de la carpeta, salvo una con el nombre reservado distinta de la del jar. */
    private Map<String, byte[]> sources() {
        Map<String, byte[]> out = new LinkedHashMap<>();
        if (factoryModel != null) {
            out.put(FACTORY_FILE, factoryModel);
        }
        File[] files = new File(pluginData, "animations")
                .listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(EXTENSION));
        if (files == null) {
            return out;
        }
        Arrays.sort(files);
        for (File f : files) {
            try {
                byte[] bytes = Files.readAllBytes(f.toPath());
                if (f.getName().equalsIgnoreCase(FACTORY_FILE)) {
                    // La copia que deja ResourceCopier es igual a la del jar; otra cosa viene de un pack o del admin.
                    if (factoryModel == null || !Arrays.equals(bytes, factoryModel)) {
                        warn.accept("Se ignora " + f + ": " + FACTORY_FILE + " es el nombre reservado del modelo de"
                                + " fábrica. Ponle otro nombre a tu modelo (y a su «model» en los emotes).");
                    }
                    continue;
                }
                out.put(f.getName(), bytes);
            } catch (IOException e) {
                warn.accept("No se pudo leer la animación " + f + ": " + e.getMessage());
            }
        }
        return out;
    }

    /**
     * Carga el registro. Uno con YAML roto se aparta a {@code .corrupto-<ts>} y se empieza de cero.
     *
     * @return false si no se puede seguir sin arriesgarse a pisar animaciones editadas
     */
    private boolean loadHashes(File hashesFile, YamlConfiguration hashes) {
        if (!hashesFile.isFile()) {
            return true;
        }
        try {
            hashes.loadFromString(Files.readString(hashesFile.toPath()));
            return true;
        } catch (InvalidConfigurationException e) {
            File backup = new File(hashesFile.getParentFile(),
                    hashesFile.getName() + ".corrupto-" + System.currentTimeMillis());
            if (!hashesFile.renameTo(backup)) {
                warn.accept("Registro de animaciones ilegible (" + hashesFile + ") y no se pudo apartar:"
                        + " no se instala nada hasta arreglarlo.");
                return false;
            }
            // Con el registro vacío, un destino distinto cuenta como editado: no se pisa nada que ya exista.
            warn.accept("Registro de animaciones ilegible; copia en " + backup.getName() + ". Se crea uno nuevo.");
            return true;
        } catch (IOException e) {
            warn.accept("No se pudo leer " + hashesFile + " (" + e.getMessage() + "): no se instala nada.");
            return false;
        }
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 siempre existe en la JVM", e);
        }
    }
}
