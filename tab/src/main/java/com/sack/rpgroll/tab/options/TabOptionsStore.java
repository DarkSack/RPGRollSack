package com.sack.rpgroll.tab.options;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

/**
 * Guarda en el propio jugador (PersistentDataContainer) qué bloques de su cabecera y pie quiere ver. Sobrevive a
 * reinicios y no necesita base de datos; cada servidor de la red guarda lo suyo.
 */
public final class TabOptionsStore {

    private final NamespacedKey key;

    public TabOptionsStore(Plugin plugin) {
        this.key = new NamespacedKey(plugin, "tab_blocks");
    }

    public BlockChoices get(Player player) {
        return BlockChoices.parse(player.getPersistentDataContainer().get(key, PersistentDataType.STRING));
    }

    public void set(Player player, BlockChoices choices) {
        player.getPersistentDataContainer().set(key, PersistentDataType.STRING, choices.serialize());
    }
}
