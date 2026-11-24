package com.sack.rpgroll.structures.workbench;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
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
            Path dir = file.toAbsolutePath().getParent();
            Files.createDirectories(dir);
            // Temporal con permisos de solo el dueño desde que existe (en POSIX), luego movido de golpe: nunca
            // hay un secret.key a medio escribir ni uno legible por otros usuarios.
            FileAttribute<?>[] ownerOnly = FileSystems.getDefault().supportedFileAttributeViews().contains("posix")
                    ? new FileAttribute<?>[] {PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"))}
                    : new FileAttribute<?>[0]; // Windows: la carpeta del plugin ya es del usuario del servidor
            Path tmp = Files.createTempFile(dir, "secret", ".tmp", ownerOnly);
            try {
                Files.write(tmp, key);
                Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE);
            } finally {
                Files.deleteIfExists(tmp);
            }
            return key;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
