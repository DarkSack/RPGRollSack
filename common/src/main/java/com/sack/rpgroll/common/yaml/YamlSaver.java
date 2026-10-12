package com.sack.rpgroll.common.yaml;

import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Guarda un YAML sin dejarlo a medias: escribe a un temporal y lo renombra encima. Con
 * {@code config.save(file)} un cuelgue o un corte a media escritura deja el archivo vacío, y un
 * archivo vacío se lee como "sin datos" (una cartera a cero) sin avisar.
 */
public final class YamlSaver {

    private YamlSaver() {
    }

    public static void save(FileConfiguration config, File file) throws IOException {

        File parent = file.getAbsoluteFile().getParentFile();
        Files.createDirectories(parent.toPath());
        // Nombre único: dos guardados a la vez del mismo archivo no se pisan el temporal.
        File temp = File.createTempFile(file.getName() + ".", ".tmp", parent);
        try {
            config.save(temp);
            try {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp.toPath());
        }
    }
}
