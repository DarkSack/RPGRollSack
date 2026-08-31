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
     * @param resourceId id del producto en el marketplace, propio de este módulo
     * @return true si el plugin puede seguir arrancando
     */
    public static boolean verify(Plugin plugin, String resourceId) {

        if (Boolean.getBoolean("rpgroll.devmode")) {
            plugin.getLogger().warning(
                    "✔ Chequeo de licencia OMITIDO (-Drpgroll.devmode=true) — NO usar en producción.");
            return true;
        }

        LicenseResult result = new LicenseManager(plugin, resourceId).check();

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
