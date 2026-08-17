package com.sack.rpgroll.gui.character;

import com.sack.rpgroll.api.stats.StatType;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gameplay.combat.CombatStats;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;
import com.sack.rpgroll.player.stats.PlayerStats;
import com.sack.rpgroll.api.playerclass.ClassManager;
import com.sack.rpgroll.api.playerclass.PlayerClass;
import com.sack.rpgroll.api.race.Race;
import com.sack.rpgroll.race.RaceAttributeApplier;
import com.sack.rpgroll.api.race.RaceManager;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;

public class CharacterCreationFlow {

    private final Player player;
    private final PlayerManager playerManager;
    private final RaceManager raceManager;
    private final ClassManager classManager;
    private final RaceAttributeApplier raceAttributeApplier;
    private final LangManager lang;

    private String selectedRace;
    private String selectedClass;

    public CharacterCreationFlow(Player player, PlayerManager playerManager, RaceManager raceManager,
            ClassManager classManager, RaceAttributeApplier raceAttributeApplier, LangManager lang) {
        this.player = player;
        this.playerManager = playerManager;
        this.raceManager = raceManager;
        this.classManager = classManager;
        this.raceAttributeApplier = raceAttributeApplier;
        this.lang = lang;
    }

    public void start() {

        Optional<RPGPlayer> existingPlayer = playerManager.getPlayer(player.getUniqueId());

        if (existingPlayer.isPresent()) {
            RPGPlayer rpgPlayer = existingPlayer.get();

            if (rpgPlayer.getRace() != null && !rpgPlayer.getRace().isEmpty() &&
                    rpgPlayer.getPlayerClass() != null && !rpgPlayer.getPlayerClass().isEmpty()) {

                lang.send(player, "character_creation_flow.already_created");
                lang.send(player, "character_creation_flow.race_label", "race", rpgPlayer.getRace());
                lang.send(player, "character_creation_flow.class_label", "class", rpgPlayer.getPlayerClass());
                return;
            }
        }

        showRaceSelection();
    }

    private void showRaceSelection() {
        RaceSelectionGUI raceGUI = new RaceSelectionGUI(player, raceManager, this::onRaceSelected, true, lang);
        raceGUI.open();
    }

    private void onRaceSelected(String race) {
        this.selectedRace = race;

        raceManager.get(race)
                .ifPresent(r -> lang.send(player, "character_creation_flow.race_selected", "race", r.displayName()));

        showClassSelection();
    }

    private void showClassSelection() {
        ClassSelectionGUI classGUI = new ClassSelectionGUI(player, classManager, selectedRace, this::onClassSelected,
                true, lang);
        classGUI.open();
    }

    private void onClassSelected(String playerClass) {
        this.selectedClass = playerClass;
        saveCharacter();
    }

    /**
     * Guarda el personaje: aplica bonos de PlayerStats (raza + clase) y
     * modificadores físicos de raza (tamaño, velocidad, vida, knockback).
     * Ambos se aplican UNA SOLA VEZ, al momento de la creación.
     */
    private void saveCharacter() {

        Optional<RPGPlayer> rpgPlayerOpt = playerManager.getPlayer(player.getUniqueId());

        if (rpgPlayerOpt.isEmpty()) {
            lang.send(player, "character_creation_flow.save_error");
            return;
        }

        RPGPlayer rpgPlayer = rpgPlayerOpt.get();
        PlayerStats finalStats = calculateInitialStats();

        CombatStats initialCombatStats = CombatStats.create(
                finalStats.getConstitutionModifier(),
                finalStats.getIntelligenceModifier(),
                finalStats.getDexterityModifier(),
                rpgPlayer.getLevel());

        RPGPlayer updatedPlayer = rpgPlayer
                .setRace(selectedRace)
                .setClass(selectedClass)
                .updateStats(finalStats)
                .updateCombatStats(initialCombatStats);

        playerManager.savePlayer(updatedPlayer);

        Optional<Race> raceOpt = raceManager.get(selectedRace);
        Optional<PlayerClass> classOpt = classManager.get(selectedClass);

        raceOpt.ifPresent(race -> raceAttributeApplier.apply(player, race));

        String raceName = raceOpt.map(Race::displayName).orElse(selectedRace);
        String className = classOpt.map(PlayerClass::displayName).orElse(selectedClass);

        player.sendMessage("");
        lang.send(player, "character_creation_flow.border");
        lang.send(player, "character_creation_flow.success_title");
        player.sendMessage("");
        lang.send(player, "character_creation_flow.success_race", "race", raceName);
        lang.send(player, "character_creation_flow.success_class", "class", className);
        lang.send(player, "character_creation_flow.success_level");
        player.sendMessage("");
        lang.send(player, "character_creation_flow.success_hint");
        lang.send(player, "character_creation_flow.border");
        player.sendMessage("");
    }

    private PlayerStats calculateInitialStats() {

        PlayerStats stats = PlayerStats.createDefault();

        Optional<Race> raceOpt = raceManager.get(selectedRace);
        Optional<PlayerClass> classOpt = classManager.get(selectedClass);

        if (raceOpt.isPresent()) {
            stats = applyBonuses(stats, raceOpt.get().baseAttributes());
        }

        if (classOpt.isPresent()) {
            stats = applyBonuses(stats, classOpt.get().baseAttributes());
        }

        return stats;
    }

    private PlayerStats applyBonuses(PlayerStats stats, Map<StatType, Integer> bonuses) {

        PlayerStats result = stats;

        for (Map.Entry<StatType, Integer> entry : bonuses.entrySet()) {
            StatType stat = entry.getKey();
            int bonus = entry.getValue();
            int currentValue = result.get(stat);
            int newValue = clamp(currentValue + bonus, PlayerStats.MIN_STAT, PlayerStats.MAX_STAT);
            result = result.with(stat, newValue);
        }

        return result;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

}
