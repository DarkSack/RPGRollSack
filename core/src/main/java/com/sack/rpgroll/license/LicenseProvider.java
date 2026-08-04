package com.sack.rpgroll.license;

/**
 * Verifica una clave de licencia contra un marketplace/servicio externo.
 * Cada marketplace (Polymart, BuiltByBit, etc.) implementa su propio
 * proveedor — {@link LicenseManager} no conoce los detalles de ninguno.
 */
public interface LicenseProvider {

    /**
     * @param licenseKey clave de licencia del comprador (nunca hardcodeada —
     *                    viene de config.yml, reemplazada por el marketplace
     *                    al momento de la descarga)
     * @param resourceId id público del recurso en el marketplace
     */
    LicenseResult validate(String licenseKey, String resourceId);

    /** Nombre del proveedor, usado en logs. */
    String name();

}
