package com.sack.rpgroll.npcs.core;

import com.sack.rpgroll.common.menu.MenuAction;

import com.sack.rpgroll.common.content.ContentParser;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NpcParser implements ContentParser<NpcDefinition> {

    private static final Logger LOG = Logger.getLogger("RPGRoll-NPCs");

    @Override
    public NpcDefinition parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String displayName = config.getString("display-name", id);

        // Nuevo formato
        String skinValue = config.getString("skin.value", "");
        String skinSignature = config.getString("skin.signature", "");

        String pose = config.getString("pose", "STANDING");

        String world = config.getString("location.world");
        if (world == null || world.isBlank()) {
            throw new IllegalArgumentException("'" + id + "' sin campo obligatorio 'location.world'");
        }

        double x = config.getDouble("location.x");
        double y = config.getDouble("location.y");
        double z = config.getDouble("location.z");

        float yaw = (float) config.getDouble("location.yaw", 0.0);
        float pitch = (float) config.getDouble("location.pitch", 0.0);

        List<MenuAction> actions = parseActions(id, config);

        return new NpcDefinition(
                id,
                displayName,
                skinValue,
                skinSignature,
                pose,
                world,
                x,
                y,
                z,
                yaw,
                pitch,
                actions);
    }

    private List<MenuAction> parseActions(String id, YamlConfiguration config) {

        List<MenuAction> actions = new ArrayList<>();
        List<Map<?, ?>> rawList = config.getMapList("actions");

        for (Map<?, ?> raw : rawList) {

            Object typeObj = raw.get("type");
            Object valueObj = raw.get("value");

            if (typeObj == null || valueObj == null) {
                LOG.warning("'" + id + "': acción sin 'type' o sin 'value', se ignora: " + raw);
                continue;
            }

            try {

                MenuAction.ActionType type = MenuAction.ActionType.valueOf(typeObj.toString().toUpperCase());

                actions.add(new MenuAction(type, valueObj.toString()));

            } catch (IllegalArgumentException e) {
                LOG.warning("'" + id + "': tipo de acción desconocido '" + typeObj + "', se ignora.");
            }
        }

        return actions;
    }
}