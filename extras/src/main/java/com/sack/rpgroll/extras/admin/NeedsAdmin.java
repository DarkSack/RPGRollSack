package com.sack.rpgroll.extras.admin;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.extras.condition.ConditionManager;
import com.sack.rpgroll.extras.condition.ConditionRuntime;
import com.sack.rpgroll.extras.stat.StatDefinition;
import com.sack.rpgroll.extras.stat.StatEngine;
import com.sack.rpgroll.extras.stat.StatManager;
import com.sack.rpgroll.extras.temperature.BodyTemperatureEngine;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

/** Lo que el menú de admin de necesidades necesita: los motores, los archivos y la recarga. */
public record NeedsAdmin(JavaPlugin plugin, StatManager stats, StatEngine engine, ConditionManager conditions,
        ConditionRuntime runtime, BodyTemperatureEngine temperature, Runnable reload, LangManager lang) {

    public File configFile() {
        return new File(plugin.getDataFolder(), "config.yml");
    }

    public File temperatureFile() {
        return new File(plugin.getDataFolder(), "temperature.yml");
    }

    public File statFile(String id) {
        return NeedsSettings.findStatFile(new File(plugin.getDataFolder(), "stats"), id);
    }

    public boolean temperatureEnabled() {
        return com.sack.rpgroll.extras.temperature.TemperatureSettingsLoader.enabled(plugin);
    }

    /** Guarda el cambio y recarga Extras igual que {@code /extrasadmin reload}. */
    public boolean save(Player who, File file, String path, Object value) {
        if (file == null) {
            lang.send(who, "admin_menu.save_failed", "file", "?");
            return false;
        }
        try {
            NeedsSettings.write(file, path, value);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "No se pudo guardar " + file.getName(), e);
            lang.send(who, "admin_menu.save_failed", "file", file.getName());
            return false;
        }
        reload.run();
        return true;
    }

    /** Pone las necesidades en su valor sano, quita las conditions y la temperatura vuelve a la normal. */
    public void cure(Player target) {
        for (StatDefinition stat : stats.getAll()) {
            engine.set(target, stat.id(), NeedsSettings.cureValue(stat));
        }
        java.util.List.copyOf(runtime.activeConditions(target)).forEach(id -> runtime.remove(target, id));
        temperature.clear(target);
    }
}
