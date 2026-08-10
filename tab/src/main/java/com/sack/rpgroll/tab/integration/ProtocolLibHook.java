package com.sack.rpgroll.tab.integration;

import org.bukkit.Bukkit;

/**
 * Único punto de contacto con ProtocolLib. Igual que
 * {@code PlaceholderApiBridge}: aislado en su propia clase para que la JVM
 * nunca intente resolver las clases de ProtocolLib si el plugin no está
 * instalado — {@link #isPresent()} se comprueba ANTES de tocar cualquier
 * otra cosa de este paquete.
 * <p>
 * Sin ProtocolLib, RPGRoll-TAB sigue funcionando completo excepto por: la
 * visibilidad del tablist por-viewer independiente de la visibilidad real
 * de la entidad ({@link com.sack.rpgroll.tab.visibility.VisibilityEngine}).
 * Todo lo demás (header/footer, sorting, teams, nametag, belowname,
 * scoreboard, bossbar) es 100% nativo de Paper y no depende de esto.
 */
public final class ProtocolLibHook {

    private ProtocolLibHook() {
    }

    public static boolean isPresent() {
        return Bukkit.getPluginManager().getPlugin("ProtocolLib") != null;
    }

}
