package com.sack.rpgroll.structures.workbench;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;

/** Firma HMAC-SHA256 de la identidad de una mesa: un ítem con PDC copiado o inventado no la pasa. */
public final class SerialSigner {
    private static final String ALGORITHM = "HmacSHA256";

    private final byte[] key;

    public SerialSigner(byte[] key) {
        this.key = key.clone();
    }

    public String sign(UUID id, WorkbenchTier tier, long serial) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(key, ALGORITHM));
            byte[] out = mac.doFinal((id + "|" + tier.level() + "|" + serial).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 no disponible", e);
        }
    }

    public boolean verify(UUID id, WorkbenchTier tier, long serial, String signature) {
        if (signature == null) {
            return false;
        }
        return MessageDigest.isEqual(sign(id, tier, serial).getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }
}
