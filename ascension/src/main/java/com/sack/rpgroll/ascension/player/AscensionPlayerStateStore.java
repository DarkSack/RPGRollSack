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
        loadIntMap(config, "achievement-progress", state::setAchievementProgress);
        var distinct = config.getConfigurationSection("achievement-distinct");
        if (distinct != null) {
            for (String key : distinct.getKeys(false)) {
                distinct.getStringList(key).forEach(value -> state.addAchievementDistinct(key, value));
            }
        }
        state.getClaimedFactionRanks().addAll(config.getStringList("claimed-faction-ranks"));
        state.getJobEvolutions().addAll(config.getStringList("job-evolutions"));
        state.getUnlockedSecrets().addAll(config.getStringList("unlocked-secrets"));
        var bonus = config.getConfigurationSection("bonus-stats");
        if (bonus != null) {
            for (String key : bonus.getKeys(false)) {
                state.addBonusStat(key, bonus.getDouble(key));
            }
        }

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
        // Las claves "logro#índice" llevan '#', que YAML admite como clave de
        // mapa sin problema, pero no '.': por eso se separa con '#'.
        state.getAchievementProgress().forEach((key, value) -> config.set("achievement-progress." + key, value));
        state.getAchievementDistinct().forEach((key, values) ->
                config.set("achievement-distinct." + key, values.stream().sorted().toList()));
        config.set("claimed-faction-ranks", state.getClaimedFactionRanks().stream().sorted().toList());
        config.set("job-evolutions", state.getJobEvolutions().stream().sorted().toList());
        config.set("unlocked-secrets", state.getUnlockedSecrets().stream().sorted().toList());
        state.getBonusStats().forEach((key, value) -> config.set("bonus-stats." + key, value));

        try {
            config.save(new File(folder, state.uuid() + ".yml"));
        } catch (IOException e) {
            plugin.getLogger().warning("✘ Error guardando progreso de Ascension de " + state.uuid() + ": "
                    + e.getMessage());
        }
    }

}
