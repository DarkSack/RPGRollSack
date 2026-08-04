package com.sack.rpgroll.items.rarity;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class RarityManager extends ContentManager<Rarity> {

    private static final Rarity FALLBACK = new Rarity("common", "Común", NamedTextColor.WHITE, false, null, null);

    public RarityManager(JavaPlugin itemsPlugin) {
        super(resolveCoreInstance(), new YamlLoader(itemsPlugin), "rarities", "rareza", new RarityParser());
    }

    public Rarity getOrFallback(String id) {
        return get(id).orElse(FALLBACK);
    }

    private static JavaPlugin resolveCoreInstance() {

        Plugin corePlugin = Bukkit.getPluginManager().getPlugin("RPGRoll");

        if (!(corePlugin instanceof JavaPlugin javaPlugin)) {
            throw new IllegalStateException("No se pudo resolver la instancia de RPGRoll.");
        }

        return javaPlugin;
    }

}
