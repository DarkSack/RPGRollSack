package com.sack.rpgroll.license;

/**
 * Parámetros de licenciamiento que decide EL VENDEDOR al compilar, no el
 * comprador.
 * <p>
 * Son constantes de compilación a propósito. Todo esto vivía antes en
 * {@code license.yml}, que es un archivo de texto en la carpeta del
 * servidor: cualquier comprador podía apuntar {@code endpoint} a un
 * servidor propio que respondiera siempre {@code {"valid":true}}, o cambiar
 * {@code resource-id} para validar un producto que no compró. Eso anulaba
 * la verificación por completo sin necesidad de tocar el jar.
 * <p>
 * Al ser {@code static final String}, el compilador las inlinea en el
 * bytecode: falsificarlas ya no es editar YAML sino parchear el jar, que es
 * justamente lo que la ofuscación opcional de {@code :core} encarece. No
 * existe protección perfecta del lado del cliente, pero no hay que regalar
 * un interruptor en la configuración.
 * <p>
 * <b>Antes de publicar</b>: poné {@link #RESOURCE_ID} con el id real del
 * listing y {@link #SELF_HOSTED_ENDPOINT} con la URL real del servidor de
 * licencias propio.
 */
final class LicenseSettings {

    private LicenseSettings() {
    }

    /**
     * Id público del recurso en voxel.shop — el número al final de la URL
     * del listing (ej. {@code voxel.shop/resource/rpgroll.1234} → 1234).
     */
    static final String RESOURCE_ID = "0";

    /**
     * URL del servicio de verificación propio, para las ventas directas
     * (Ko-fi, Patreon) — la función {@code /api/verify} desplegada desde el
     * repositorio privado {@code verification-web} (Vercel + Supabase).
     */
    static final String SELF_HOSTED_ENDPOINT = "https://verification-web-murex.vercel.app/api/verify";

    /**
     * Prefijo de las claves que emite el servidor propio ({@code issue} las
     * genera así). Es lo que distingue una venta directa de una compra en
     * voxel.shop sin darle al comprador ningún campo que elegir: la clave
     * misma dice de qué canal viene.
     */
    static final String SELF_HOSTED_KEY_PREFIX = "RPGR-";

}
