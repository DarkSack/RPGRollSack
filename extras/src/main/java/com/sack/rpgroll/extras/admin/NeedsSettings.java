package com.sack.rpgroll.extras.admin;

import com.sack.rpgroll.extras.stat.StatDefinition;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Cálculos del menú de admin de necesidades y la escritura de sus YAML. */
public final class NeedsSettings {

    static final double[] SPEEDS = {0.25, 0.5, 1, 1.5, 2, 3};
    static final int MIN_IDLE_SECONDS = 60;

    private NeedsSettings() {
    }

    /** El siguiente paso de velocidad; tras el último vuelve al primero. */
    public static double nextSpeed(double current) {
        for (double step : SPEEDS) {
            if (step > current + 1e-9) {
                return step;
            }
        }
        return SPEEDS[0];
    }

    /** Quita el valor si está; si no, lo agrega al final. */
    public static List<String> toggle(List<String> list, String value) {
        List<String> out = new ArrayList<>(list);
        if (!out.remove(value)) {
            out.add(value);
        }
        return out;
    }

    public static int idleStep(int seconds, int delta) {
        return Math.max(MIN_IDLE_SECONDS, seconds + delta);
    }

    /**
     * Valor "sano": las necesidades que bajan (empiezan llenas, como la sed) se llenan; las que
     * suben (empiezan en 0, como el estrés o la fatiga) se vacían.
     */
    public static double cureValue(StatDefinition stat) {
        return stat.start() <= 0 ? 0 : stat.max();
    }

    /** El YAML de la necesidad: {@code <id>.yml} si existe; si no, cualquiera (también en subcarpetas) con ese {@code id}. */
    public static File findStatFile(File dir, String id) {
        File direct = new File(dir, id + ".yml");
        if (direct.isFile()) {
            return direct;
        }
        try (var files = java.nio.file.Files.walk(dir.toPath())) {
            return files.filter(p -> p.toString().endsWith(".yml"))
                    .map(java.nio.file.Path::toFile)
                    .filter(f -> id.equals(YamlConfiguration.loadConfiguration(f).getString("id")))
                    .findFirst().orElse(null);
        } catch (IOException e) {
            return null;
        }
    }

    /** Cambia una clave del YAML y lo guarda; Paper conserva los comentarios del archivo. */
    public static void write(File file, String path, Object value) throws IOException {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        yaml.set(path, value);
        yaml.save(file);
    }
}
