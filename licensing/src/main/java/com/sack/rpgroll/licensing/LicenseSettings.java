package com.sack.rpgroll.licensing;

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
     * URL del servicio de verificación propio, para las ventas directas
     * (Ko-fi, Patreon) — {@code /api/verify} de la tienda (Vercel + Supabase).
     * <p>
     * Antes vivía en {@code verification-web}, que quedó deprecado: la tienda
     * absorbió la verificación para que emitir y validar una licencia compartan
     * base y no haya dos servicios que mantener sincronizados.
     * <p>
     * <b>Es un dominio propio y no el {@code *.vercel.app} del despliegue.</b>
     * Esta cadena queda compilada en el bytecode de cada jar, así que es la
     * única dirección que una copia distribuida sabe consultar — para siempre.
     * Con el subdominio del proveedor, mudarse de hosting, o que el proveedor
     * cambie cómo los asigna, dejaría sin validar a todos los jars que ya están
     * en el disco de los compradores, sin ningún arreglo posible desde acá. Con
     * dominio propio eso se resuelve repuntando un registro DNS. Aparte,
     * {@code vercel.app} está en la Public Suffix List y algunos hosts de
     * Minecraft lo bloquean en bloque.
     * <p>
     * Cambiar esta constante <b>no</b> alcanza para los jars ya distribuidos.
     * Por eso el dominio anterior debe seguir respondiendo, o redirigir,
     * mientras exista alguna versión antigua en circulación.
     */
    static final String SELF_HOSTED_ENDPOINT = "https://store.sackito.online/api/verify";

    /**
     * Token que acompaña la verificación. Lo aporta cada módulo al arrancar,
     * igual que el {@code resourceId}: {@code :licensing} es interno y no
     * tiene constantes generadas propias.
     * <p>
     * No es autenticación y no hay que pretender que lo sea: el plugin corre en
     * la máquina del comprador, así que esto se puede sacar del jar. Sirve para
     * que la URL no quede a merced de cualquiera que la encuentre; quien
     * probaría claves a lo bruto lo frena el límite por IP del servidor, no
     * esto.
     */
    static String selfHostedToken = "";

    /**
     * Prefijo de las claves que emite el servidor propio ({@code issue} las
     * genera así). Es lo que distingue una venta directa de una compra en
     * voxel.shop sin darle al comprador ningún campo que elegir: la clave
     * misma dice de qué canal viene.
     */
    static final String SELF_HOSTED_KEY_PREFIX = "RPGR-";

}
