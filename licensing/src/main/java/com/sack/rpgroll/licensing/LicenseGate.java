package com.sack.rpgroll.licensing;

import org.bukkit.plugin.Plugin;

/**
 * Punto de entrada de la verificación de licencia. Cada módulo la llama al
 * arrancar y se deshabilita si devuelve {@code false}.
 * <p>
 * Vive en {@code :common}, que se empaqueta dentro de cada addon, así que
 * los 24 productos verifican su propia compra de forma independiente: un
 * comprador del core no obtiene los addons que no pagó.
 * <p>
 * El {@code resourceId} lo aporta cada módulo desde su propia constante
 * generada al compilar — ver {@code LicenseIdentity}.
 */
public final class LicenseGate {

    private LicenseGate() {
    }

    /**
     * Los tres valores salen de la clase {@code LicenseIdentity} que Gradle
     * genera para cada módulo — no hay forma correcta de llamar a esto con
     * literales escritos a mano.
     *
     * @param resourceId  id del producto en voxel.shop (numérico)
     * @param productSlug id del producto en la tienda propia (slug). NO es el
     *                    mismo valor que {@code resourceId}: son dos catálogos
     *                    distintos, y confundirlos deja al comprador con el
     *                    plugin apagado
     * @param verifyToken token del servicio propio, generado al compilar. Vacío
     *                    = no se manda cabecera.
     * @return true si el plugin puede seguir arrancando
     */
    public static boolean verify(Plugin plugin, String resourceId, String productSlug,
                                 String verifyToken) {

        LicenseSettings.selfHostedToken = verifyToken == null ? "" : verifyToken;


        if (Boolean.getBoolean("rpgroll.devmode")) {
            plugin.getLogger().warning(
                    "✔ Chequeo de licencia OMITIDO (-Drpgroll.devmode=true) — NO usar en producción.");
            return true;
        }

        LicenseResult result = new LicenseManager(plugin, resourceId, productSlug).check();

        if (result.isValid()) {
            plugin.getLogger().info("✔ Licencia verificada: " + result.message());
            return true;
        }

        String name = plugin.getName();

        plugin.getLogger().severe("==================================");
        plugin.getLogger().severe("✘ " + name + " no pudo verificar tu licencia:");
        plugin.getLogger().severe("  " + result.message());
        plugin.getLogger().severe("  El plugin se va a deshabilitar.");
        plugin.getLogger().severe("  Si compraste en voxel.shop, descargá el jar desde tu panel de compras");
        plugin.getLogger().severe("  (la copia que baja de ahí ya trae tu clave incrustada).");
        plugin.getLogger().severe("  Si fue una venta directa, revisá la clave en");
        plugin.getLogger().severe("  plugins/" + name + "/license.yml");
        plugin.getLogger().severe("==================================");

        return false;
    }

}
