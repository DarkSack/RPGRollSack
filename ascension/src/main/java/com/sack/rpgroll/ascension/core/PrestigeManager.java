package com.sack.rpgroll.ascension.core;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

public class PrestigeManager extends ContentManager<PrestigeLevel> {

    private final PrestigeDefinitionWriter writer;

    public PrestigeManager(JavaPlugin ascensionPlugin) {
        super(resolveCoreInstance(), new YamlLoader(ascensionPlugin), "prestige", "prestigio", new PrestigeParser());
        this.writer = new PrestigeDefinitionWriter(ascensionPlugin.getDataFolder());
    }

    public void save(PrestigeLevel level) {
        writer.save(level);
        reload();
    }

    public Optional<PrestigeLevel> getByNumber(int number) {
        return get(String.valueOf(number));
    }

    /** Suma acumulada de bonos de experiencia de todos los prestigios alcanzados hasta {@code count}. */
    public double cumulativeExpBonus(int count) {

        double total = 0;

        for (int i = 1; i <= count; i++) {
            total += getByNumber(i).map(PrestigeLevel::expBonusPercent).orElse(0.0);
        }

        return total;
    }

    private static JavaPlugin resolveCoreInstance() {

        Plugin corePlugin = Bukkit.getPluginManager().getPlugin("RPGRoll");

        if (!(corePlugin instanceof JavaPlugin javaPlugin)) {
            throw new IllegalStateException("No se pudo resolver la instancia de RPGRoll.");
        }

        return javaPlugin;
    }

}
