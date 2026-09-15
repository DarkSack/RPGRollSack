package com.sack.rpgroll.licensing;

/**
 * Resultado de validar la licencia contra el proveedor configurado.
 * {@code UNKNOWN} representa un fallo de red/API — no es lo mismo que una
 * licencia inválida, y se trata con gracia (ver {@link LicenseCache}) para
 * no bloquear a un comprador legítimo por una caída temporal del servicio.
 *
 * @param proof la validación firmada que la respalda, si el canal firma (solo
 *              el servidor propio). Es lo que se guarda para el período de gracia.
 */
public record LicenseResult(Status status, String message, LicenseProof proof) {

    public enum Status {
        VALID,
        INVALID,
        UNKNOWN
    }

    public LicenseResult(Status status, String message) {
        this(status, message, null);
    }

    public static LicenseResult valid(String message) {
        return new LicenseResult(Status.VALID, message);
    }

    static LicenseResult validSigned(String message, LicenseProof proof) {
        return new LicenseResult(Status.VALID, message, proof);
    }

    public static LicenseResult invalid(String message) {
        return new LicenseResult(Status.INVALID, message);
    }

    public static LicenseResult unknown(String message) {
        return new LicenseResult(Status.UNKNOWN, message);
    }

    public boolean isValid() {
        return status == Status.VALID;
    }

}
