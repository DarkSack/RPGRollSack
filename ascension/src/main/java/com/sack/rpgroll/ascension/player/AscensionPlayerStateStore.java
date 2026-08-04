package com.sack.rpgroll.ascension.player;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Persiste el {@link AscensionPlayerState} de cada jugador en su propio
 * archivo plugins/RPGRoll-Ascension/playerdata/&lt;uuid&gt;.yml — self
 * contained, no depende de la BD de :core.
 */
public class AscensionPlayerStateStore {

    private final Plugin plugin;
    private final File folder;

    public AscensionPlayerStateStore(Plugin plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "playerdata");

        if (!folder.exists()) {
            folder.mkdirs();
        }
    }

    public AscensionPlayerState load(UUID uuid) {

        AscensionPlayerState state = new AscensionPlayerState(uuid);
        File file = new File(folder, uuid + ".yml");

        if (!file.exists()) {
            return state;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        state.setCurrentEvolutionId(config.getString("evolution"));
        state.setCurrentSpecializationId(config.getString("specialization"));
        state.getUnlockedTalents().addAll(config.getStringList("unlocked-talents"));
        state.addTalentPoints(config.getInt("talent-points", 0));

        for (int i = 0; i < config.getInt("prestige-count", 0); i++) {
            state.incrementPrestige();
        }
        for (int i = 0; i < config.getInt("legacy-count", 0); i++) {
            state.incrementLegacy();
        }
        state.addPermanentExpBonusPercent(config.getDouble("permanent-exp-bonus-percent", 0));

        loadIntMap(config, "affinity-experience", state::addAffinityExperience);
        loadIntMap(config, "mastery-experience", state::addMasteryExperience);
        loadIntMap(config, "reputation", state::addReputation);

        state.getUnlockedAchievements().addAll(config.getStringList("unlocked-achievements"));
        state.getUnlockedTitles().addAll(config.getStringList("unlocked-titles"));
        state.setActiveTitle(config.getString("active-title"));

        return state;
    }

    private void loadIntMap(YamlConfiguration config, String path, java.util.function.BiConsumer<String, Integer> consumer) {

        var section = config.getConfigurationSection(path);
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            consumer.accept(key, section.getInt(key));
        }
    }

    public void save(AscensionPlayerState state) {

        YamlConfiguration config = new YamlConfiguration();

        if (state.getCurrentEvolutionId() != null) {
            config.set("evolution", state.getCurrentEvolutionId());
        }
        if (state.getCurrentSpecializationId() != null) {
            config.set("specialization", state.getCurrentSpecializationId());
        }

        config.set("unlocked-talents", state.getUnlockedTalents().stream().toList());
        config.set("talent-points", state.getAvailableTalentPoints());
        config.set("prestige-count", state.getPrestigeCount());
        config.set("legacy-count", state.getLegacyCount());
        config.set("permanent-exp-bonus-percent", state.getPermanentExpBonusPercent());

        state.getAffinityExperience().forEach((key, value) -> config.set("affinity-experience." + key, value));
        state.getMasteryExperience().forEach((key, value) -> config.set("mastery-experience." + key, value));
        state.getReputation().forEach((key, value) -> config.set("reputation." + key, value));

        config.set("unlocked-achievements", state.getUnlockedAchievements().stream().toList());
        config.set("unlocked-titles", state.getUnlockedTitles().stream().toList());

        if (state.getActiveTitle() != null) {
            config.set("active-title", state.getActiveTitle());
        }

        try {
            config.save(new File(folder, state.uuid() + ".yml"));
        } catch (IOException e) {
            plugin.getLogger().warning("✘ Error guardando progreso de Ascension de " + state.uuid() + ": "
                    + e.getMessage());
        }
    }

}
