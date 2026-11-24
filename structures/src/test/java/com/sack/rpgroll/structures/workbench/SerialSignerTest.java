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

    @Test void secretIsCreatedOnceAndReused(@TempDir Path d) {
        byte[] a = ServerSecret.load(d.resolve("sub/secret.key"));
        assertEquals(32, a.length);
        assertArrayEquals(a, ServerSecret.load(d.resolve("sub/secret.key")));
    }
}
