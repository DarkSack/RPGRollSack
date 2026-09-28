package com.sack.rpgroll.common.integration;

import org.bukkit.Bukkit;

/**
 * Antes de tocar la API de un plugin que es solo {@code softdepend}: si no está instalado, su clase
 * ni existe, y hasta un {@code XxxAPI.isReady()} lanza NoClassDefFoundError al cargarla. Se usa
 * siempre en cortocircuito: {@code SoftDepend.enabled("RPGRoll-Guilds") && GuildsAPI.isReady()}.
 * <p>
 * Sin servidor (tests) responde false, igual que si el plugin no estuviera.
 */
public final class SoftDepend {

    private SoftDepend() {
    }

    public static boolean enabled(String plugin) {
        return Bukkit.getServer() != null && Bukkit.getPluginManager().isPluginEnabled(plugin);
    }

}
