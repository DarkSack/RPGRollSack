package com.sack.rpgroll.structures.workbench;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;

/** Clave del servidor para firmar las mesas: 32 bytes aleatorios guardados en {@code secret.key}. */
public final class ServerSecret {
    private static final int SIZE = 32;

    private ServerSecret() {}

    /** Lee la clave o la crea si falta. Un archivo de tamaño incorrecto falla en vez de regenerarse. */
    public static byte[] load(Path file) {
        try {
            if (Files.exists(file)) {
                byte[] existing = Files.readAllBytes(file);
                if (existing.length == SIZE) {
                    return existing;
                }
                // Regenerarla invalidaría todas las mesas ya fabricadas en silencio.
                throw new IllegalStateException(file + " debe medir " + SIZE + " bytes (mide " + existing.length
                        + "). Bórralo solo si aceptas invalidar las mesas ya fabricadas.");
            }
            byte[] key = new byte[SIZE];
            new SecureRandom().nextBytes(key);
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Files.write(file, key);
            try {
                Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
            } catch (UnsupportedOperationException ignored) {
                // Windows: sin permisos POSIX; la carpeta del plugin ya es del usuario del servidor.
            }
            return key;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
