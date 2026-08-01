package com.sack.rpgroll.npcs.core;

import com.sack.rpgroll.common.content.ContentParser;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NpcParser implements ContentParser<NpcDefinition> {

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

        List<NpcAction> actions = parseActions(config);

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

    private List<NpcAction> parseActions(YamlConfiguration config) {

        List<NpcAction> actions = new ArrayList<>();
        List<Map<?, ?>> rawList = config.getMapList("actions");

        for (Map<?, ?> raw : rawList) {

            Object typeObj = raw.get("type");
            Object valueObj = raw.get("value");

            if (typeObj == null || valueObj == null) {
                continue;
            }

            try {

                NpcAction.NpcActionType type = NpcAction.NpcActionType.valueOf(typeObj.toString().toUpperCase());

                actions.add(new NpcAction(type, valueObj.toString()));

            } catch (IllegalArgumentException ignored) {
            }
        }

        return actions;
    }
}