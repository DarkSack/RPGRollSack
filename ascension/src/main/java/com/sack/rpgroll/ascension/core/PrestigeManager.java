package com.sack.rpgroll.ascension.core;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

public class PrestigeManager extends ContentManager<PrestigeLevel> {

    private final PrestigeDefinitionWriter writer;

    public PrestigeManager(JavaPlugin ascensionPlugin) {
        super(owningPlugin(), new YamlLoader(ascensionPlugin), "prestige", "prestigio", new PrestigeParser());
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

    /** El propio módulo: la carga se anuncia con su nombre y no hace falta el core. */
    private static JavaPlugin owningPlugin() {
        return JavaPlugin.getProvidingPlugin(PrestigeManager.class);
    }

}
