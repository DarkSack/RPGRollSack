package com.sack.rpgroll.gui.character;

import com.sack.rpgroll.gameplay.stats.StatType;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;
import com.sack.rpgroll.player.stats.PlayerStats;
import com.sack.rpgroll.playerclass.ClassManager;
import com.sack.rpgroll.playerclass.PlayerClass;
import com.sack.rpgroll.race.Race;
import com.sack.rpgroll.race.RaceAttributeApplier;
import com.sack.rpgroll.race.RaceManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;

public class CharacterCreationFlow {

    private final Player player;
    private final PlayerManager playerManager;
    private final RaceManager raceManager;
    private final ClassManager classManager;
    private final RaceAttributeApplier raceAttributeApplier;

    private String selectedRace;
    private String selectedClass;

    public CharacterCreationFlow(Player player, PlayerManager playerManager, RaceManager raceManager,
            ClassManager classManager, RaceAttributeApplier raceAttributeApplier) {
        this.player = player;
        this.playerManager = playerManager;
        this.raceManager = raceManager;
        this.classManager = classManager;
        this.raceAttributeApplier = raceAttributeApplier;
    }

    public void start() {

        Optional<RPGPlayer> existingPlayer = playerManager.getPlayer(player.getUniqueId());

        if (existingPlayer.isPresent()) {
            RPGPlayer rpgPlayer = existingPlayer.get();

            if (rpgPlayer.getRace() != null && !rpgPlayer.getRace().isEmpty() &&
                    rpgPlayer.getPlayerClass() != null && !rpgPlayer.getPlayerClass().isEmpty()) {

                player.sendMessage(Component.text("Ya tienes un personaje creado.", NamedTextColor.RED));
                player.sendMessage(Component.text("Raza: ", NamedTextColor.YELLOW)
                        .append(Component.text(rpgPlayer.getRace(), NamedTextColor.WHITE)));
                player.sendMessage(Component.text("Clase: ", NamedTextColor.YELLOW)
                        .append(Component.text(rpgPlayer.getPlayerClass(), NamedTextColor.WHITE)));
                return;
            }
        }

        showRaceSelection();
    }

    private void showRaceSelection() {
        RaceSelectionGUI raceGUI = new RaceSelectionGUI(player, raceManager, this::onRaceSelected, true);
        raceGUI.open();
    }

    private void onRaceSelected(String race) {
        this.selectedRace = race;

        raceManager.get(race)
                .ifPresent(r -> player.sendMessage(Component.text("Has seleccionado la raza: ", NamedTextColor.GREEN)
                        .append(Component.text(r.displayName(), NamedTextColor.GOLD))));

        showClassSelection();
    }

    private void showClassSelection() {
        ClassSelectionGUI classGUI = new ClassSelectionGUI(player, classManager, selectedRace, this::onClassSelected,
                true);
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
            player.sendMessage(Component.text("Error al crear personaje.", NamedTextColor.RED));
            return;
        }

        RPGPlayer rpgPlayer = rpgPlayerOpt.get();
        PlayerStats finalStats = calculateInitialStats();

        RPGPlayer updatedPlayer = rpgPlayer
                .setRace(selectedRace)
                .setClass(selectedClass)
                .updateStats(finalStats);

        playerManager.savePlayer(updatedPlayer);

        Optional<Race> raceOpt = raceManager.get(selectedRace);
        Optional<PlayerClass> classOpt = classManager.get(selectedClass);

        raceOpt.ifPresent(race -> raceAttributeApplier.apply(player, race));

        String raceName = raceOpt.map(Race::displayName).orElse(selectedRace);
        String className = classOpt.map(PlayerClass::displayName).orElse(selectedClass);

        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("════════════════════════════════", NamedTextColor.GOLD));
        player.sendMessage(Component.text("  ¡Personaje creado con éxito!", NamedTextColor.GREEN));
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("  Raza: ", NamedTextColor.YELLOW)
                .append(Component.text(raceName, NamedTextColor.WHITE)));
        player.sendMessage(Component.text("  Clase: ", NamedTextColor.YELLOW)
                .append(Component.text(className, NamedTextColor.WHITE)));
        player.sendMessage(Component.text("  Nivel: ", NamedTextColor.YELLOW)
                .append(Component.text("1", NamedTextColor.WHITE)));
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("  Usa ", NamedTextColor.GRAY)
                .append(Component.text("/rpg stats", NamedTextColor.WHITE))
                .append(Component.text(" para ver tus estadísticas", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("════════════════════════════════", NamedTextColor.GOLD));
        player.sendMessage(Component.text(""));
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