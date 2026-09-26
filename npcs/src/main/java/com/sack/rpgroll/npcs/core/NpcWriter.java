package com.sack.rpgroll.npcs.core;

import com.sack.rpgroll.common.menu.MenuAction;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NpcWriter {

    private final Plugin plugin;

    public NpcWriter(Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean save(NpcEditSession session) {

        NpcDefinition definition = session.toDefinition();

        File folder = new File(plugin.getDataFolder(), "npcs");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        File file = new File(folder, definition.id() + ".yml");

        YamlConfiguration config = new YamlConfiguration();

        config.set("id", definition.id());
        config.set("display-name", definition.displayName());
        config.set("skin.value", definition.skinValue());
        config.set("skin.signature", definition.skinSignature());
        config.set("pose", definition.pose());
        config.set("location.world", definition.world());
        config.set("location.x", definition.x());
        config.set("location.y", definition.y());
        config.set("location.z", definition.z());
        config.set("location.yaw", (double) definition.yaw());
        config.set("location.pitch", (double) definition.pitch());

        List<Map<String, Object>> actionMaps = new ArrayList<>();
        for (MenuAction action : definition.actions()) {
            actionMaps.add(Map.of("type", action.type().name(), "value", action.value()));
        }
        config.set("actions", actionMaps);

        try {
            config.save(file);
            return true;
        } catch (IOException e) {
            plugin.getLogger().severe("✘ Error al guardar NPC '" + definition.id() + "': " + e.getMessage());
            return false;
        }
    }

    /**
     * Borra el fichero del NPC. Primero prueba con {@code <id>.yml}; si no
     * existe, busca el fichero cuyo campo {@code id} coincide, porque el
     * cargador acepta cualquier nombre de fichero.
     */
    public boolean delete(String npcId) {

        File folder = new File(plugin.getDataFolder(), "npcs");
        File file = new File(folder, npcId + ".yml");

        if (file.isFile()) {
            return file.delete();
        }

        File[] candidates = folder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (candidates == null) {
            return false;
        }

        for (File candidate : candidates) {
            if (npcId.equals(YamlConfiguration.loadConfiguration(candidate).getString("id"))) {
                return candidate.delete();
            }
        }

        return false;
    }

}