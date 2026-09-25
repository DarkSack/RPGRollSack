package com.sack.rpgroll.items.rarity;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.plugin.java.JavaPlugin;

public class RarityManager extends ContentManager<Rarity> {

    private static final Rarity FALLBACK = new Rarity("common", "Común", NamedTextColor.WHITE, false, null, null);

    public RarityManager(JavaPlugin itemsPlugin) {
        super(owningPlugin(), new YamlLoader(itemsPlugin), "rarities", "rareza", new RarityParser());
    }

    public Rarity getOrFallback(String id) {
        return get(id).orElse(FALLBACK);
    }

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(RarityManager.class);
    }

}
