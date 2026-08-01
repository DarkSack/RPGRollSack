package com.sack.rpgroll.crates.core;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CrateParser implements ContentParser<Crate> {

    @Override
    public Crate parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        String displayName = config.getString("display-name", id);
        String guiTitle = config.getString("gui-title", displayName);
        boolean requireKey = config.getBoolean("require-key", true);

        Material keyMaterial = parseMaterial(config.getString("key.material"), Material.TRIPWIRE_HOOK);
        String keyDisplayName = config.getString("key.name", "Llave: " + displayName);
        List<String> keyLore = config.getStringList("key.lore");

        List<String> hologramLines = config.getStringList("hologram");

        List<CrateReward> rewards = parseRewards(config, id);

        return new Crate(id, displayName, guiTitle, requireKey, keyMaterial, keyDisplayName, keyLore,
                hologramLines, rewards);
    }

    private Material parseMaterial(String raw, Material fallback) {

        if (raw == null || raw.isBlank()) {
            return fallback;
        }

        try {
            return Material.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private List<CrateReward> parseRewards(YamlConfiguration config, String crateId) {

        List<CrateReward> rewards = new ArrayList<>();
        List<Map<?, ?>> rawRewards = config.getMapList("rewards");

        for (Map<?, ?> raw : rawRewards) {

            Object idObj = raw.get("id");
            if (idObj == null) {
                continue;
            }

            String rewardId = idObj.toString();
            String rewardName = raw.get("display-name") != null ? raw.get("display-name").toString() : rewardId;
            Material icon = parseMaterial(
                    raw.get("icon") != null ? raw.get("icon").toString() : null, Material.PAPER);

            double weight = raw.get("weight") != null ? Double.parseDouble(raw.get("weight").toString()) : 1.0;
            boolean announce = raw.get("announce") != null && Boolean.parseBoolean(raw.get("announce").toString());

            List<String> lore = raw.get("lore") instanceof List<?> rawLore
                    ? rawLore.stream().map(Object::toString).toList()
                    : List.of();

            List<CrateAction> actions = parseActions(raw);

            try {
                rewards.add(new CrateReward(rewardId, rewardName, icon, lore, weight, announce, actions));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("crate '" + crateId + "': " + e.getMessage());
            }
        }

        return rewards;
    }

    private List<CrateAction> parseActions(Map<?, ?> raw) {

        List<CrateAction> actions = new ArrayList<>();

        if (!(raw.get("actions") instanceof List<?> rawActions)) {
            return actions;
        }

        for (Object obj : rawActions) {

            if (!(obj instanceof Map<?, ?> actionMap)) {
                continue;
            }

            Object typeObj = actionMap.get("type");
            Object valueObj = actionMap.get("value");

            if (typeObj == null || valueObj == null) {
                continue;
            }

            try {
                CrateAction.CrateActionType type = CrateAction.CrateActionType.valueOf(
                        typeObj.toString().toUpperCase());
                actions.add(new CrateAction(type, valueObj.toString()));
            } catch (IllegalArgumentException ignored) {
            }
        }

        return actions;
    }

}
