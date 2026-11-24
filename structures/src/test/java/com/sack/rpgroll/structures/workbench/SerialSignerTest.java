package com.sack.rpgroll.structures.workbench;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static com.sack.rpgroll.structures.workbench.WorkbenchTier.ARQUITECTO;
import static com.sack.rpgroll.structures.workbench.WorkbenchTier.CONSTRUCTOR;
import static org.junit.jupiter.api.Assertions.*;

class SerialSignerTest {
    private final UUID id = UUID.randomUUID();
    private final SerialSigner signer = new SerialSigner(new byte[32]);

    @Test void signatureRoundTrips() {
        assertTrue(signer.verify(id, CONSTRUCTOR, 7, signer.sign(id, CONSTRUCTOR, 7)));
    }

    @Test void changingTheTierBreaksTheSignature() {
        assertFalse(signer.verify(id, ARQUITECTO, 7, signer.sign(id, CONSTRUCTOR, 7)));
    }

    @Test void changingTheSerialOrIdBreaksTheSignature() {
        String sig = signer.sign(id, CONSTRUCTOR, 7);
        assertFalse(signer.verify(id, CONSTRUCTOR, 8, sig));
        assertFalse(signer.verify(UUID.randomUUID(), CONSTRUCTOR, 7, sig));
    }

    @Test void anotherServerKeyRejects() {
        byte[] other = new byte[32];
        other[0] = 1;
        assertFalse(new SerialSigner(other).verify(id, CONSTRUCTOR, 7, signer.sign(id, CONSTRUCTOR, 7)));
    }

    @Test void garbageSignatureIsRejectedNotThrown() {
        assertFalse(signer.verify(id, CONSTRUCTOR, 7, "!!no-base64!!"));
        assertFalse(signer.verify(id, CONSTRUCTOR, 7, ""));
    }

    /** Calculados aparte con Python (hmac + sha256, base64url sin relleno). */
    @Test void knownHmacVectors() {
        assertEquals("6524mKO2j5EhZUTYaie9SNKxVnF3AmRMHD8WYdDECmM",
                new SerialSigner(new byte[32]).sign(new UUID(0, 0), CONSTRUCTOR, 7));
        byte[] key = new byte[32];
        for (int i = 0; i < key.length; i++) {
            key[i] = (byte) i;
        }
        assertEquals("vg8ufqHfmf3HiDN6JtHfv7n6Z3xYeIHRQrrg7yWQjHE", new SerialSigner(key)
                .sign(UUID.fromString("12345678-1234-1234-1234-123456789abc"), ARQUITECTO, 42));
    }

    @Test void secretOfTheWrongLengthFailsInsteadOfRegenerating(@TempDir Path d) throws Exception {
        Path file = d.resolve("secret.key");
        java.nio.file.Files.write(file, new byte[31]);
        assertThrows(IllegalStateException.class, () -> ServerSecret.load(file));
        assertEquals(31, java.nio.file.Files.size(file));
    }

    @Test void secretIsWrittenWithoutLeftoversAndOwnerOnly(@TempDir Path d) throws Exception {
        Path file = d.resolve("secret.key");
        ServerSecret.load(file);
        try (var files = java.nio.file.Files.list(d)) {
            assertEquals(java.util.List.of(file), files.toList());
        }
        if (java.nio.file.FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            assertEquals("rw-------", java.nio.file.attribute.PosixFilePermissions.toString(
                    java.nio.file.Files.getPosixFilePermissions(file)));
        }
    }

    @Test void secretIsCreatedOnceAndReused(@TempDir Path d) {
        byte[] a = ServerSecret.load(d.resolve("sub/secret.key"));
        assertEquals(32, a.length);
        assertArrayEquals(a, ServerSecret.load(d.resolve("sub/secret.key")));
    }
}
