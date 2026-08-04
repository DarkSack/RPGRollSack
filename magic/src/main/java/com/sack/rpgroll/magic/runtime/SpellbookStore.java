package com.sack.rpgroll.magic.runtime;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Persiste el {@link PlayerSpellbook} de cada jugador en su propio archivo
 * plugins/RPGRoll-Magic/playerdata/&lt;uuid&gt;.yml — mismo patrón que
 * QuestPlayerStateStore de RPGRoll-Quests, self-contained (sin BD).
 */
public class SpellbookStore {

    private final Plugin plugin;
    private final File folder;

    public SpellbookStore(Plugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "playerdata");

        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    public PlayerSpellbook load(UUID uuid) {

        PlayerSpellbook spellbook = new PlayerSpellbook(uuid);
        File file = new File(folder, uuid + ".yml");

        if (!file.exists()) {
            return spellbook;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        for (String spellId : config.getStringList("learned")) {
            spellbook.learn(spellId);
        }

        String selected = config.getString("selected");
        if (selected != null && spellbook.knows(selected)) {
            spellbook.select(selected);
        }

        var runesSection = config.getConfigurationSection("runes");
        if (runesSection != null) {
            for (String spellId : runesSection.getKeys(false)) {
                for (String runeId : runesSection.getStringList(spellId)) {
                    spellbook.attachRune(spellId, runeId);
                }
            }
        }

        var cooldownsSection = config.getConfigurationSection("cooldowns");
        long now = System.currentTimeMillis();
        if (cooldownsSection != null) {
            for (String spellId : cooldownsSection.getKeys(false)) {
                long endMillis = cooldownsSection.getLong(spellId);
                if (endMillis > now) {
                    spellbook.startCooldown(spellId, endMillis - now, now);
                }
            }
        }

        return spellbook;
    }

    public void save(PlayerSpellbook spellbook) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("learned", List.copyOf(spellbook.allLearned()));
        config.set("selected", spellbook.selectedSpellId());

        for (var entry : spellbook.allAttachedRunes().entrySet()) {
            if (!entry.getValue().isEmpty()) {
                config.set("runes." + entry.getKey(), entry.getValue());
            }
        }

        long now = System.currentTimeMillis();
        for (var entry : spellbook.allCooldowns().entrySet()) {
            if (entry.getValue() > now) {
                config.set("cooldowns." + entry.getKey(), entry.getValue());
            }
        }

        try {
            config.save(new File(folder, spellbook.uuid() + ".yml"));
        } catch (IOException e) {
            plugin.getLogger().warning("✘ Error guardando el spellbook de " + spellbook.uuid() + ": "
                    + e.getMessage());
        }
    }

}
