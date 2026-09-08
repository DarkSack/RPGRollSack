package com.sack.rpgroll.licensing;

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

    /**
     * Qué identificador de producto espera este canal.
     * <p>
     * Los dos canales tienen catálogos propios y NO comparten identificador: en
     * voxel.shop un producto es un número ({@code 10170}), y en la tienda
     * propia es un slug ({@code mobs}). Mandar el equivocado no falla de forma
     * ruidosa — la tienda propia responde {@code not-covered}, que es
     * {@code valid:false}, o sea licencia inválida y sin período de gracia: el
     * plugin de alguien que pagó no arranca.
     *
     * @return true si {@link #validate} espera el slug de la tienda propia en
     *         vez del id del marketplace
     */
    default boolean usesProductSlug() {
        return false;
    }

}
