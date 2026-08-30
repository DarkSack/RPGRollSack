package com.sack.rpgroll.license;

/**
 * Verifica una clave de licencia contra un marketplace/servicio externo.
 * Cada canal de venta (voxel.shop, servidor propio, etc.) implementa su propio
 * proveedor — {@link LicenseManager} no conoce los detalles de ninguno.
 */
public interface LicenseProvider {

    /**
     * @param licenseKey clave de licencia del comprador — nunca hardcodeada:
     *                    sale de license.yml, donde el marketplace la sustituye
     *                    al descargar, o donde el comprador la pega a mano si
     *                    fue una venta directa
     * @param resourceId qué producto se está validando (el ecosistema son 24
     *                    módulos que se venden por separado)
     */
    LicenseResult validate(String licenseKey, String resourceId);

    /** Nombre del proveedor, usado en logs. */
    String name();

}
