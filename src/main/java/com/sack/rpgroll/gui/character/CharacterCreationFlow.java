package com.sack.rpgroll.gui.character;

import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;
import com.sack.rpgroll.race.RaceManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * Coordinador del flujo de creación de personaje.
 * Maneja la secuencia: Raza → Clase → Guardar
 */
public class CharacterCreationFlow {

    private final Player player;
    private final PlayerManager playerManager;
    private final RaceManager raceManager;

    private String selectedRace;
    private String selectedClass;

    public CharacterCreationFlow(Player player, PlayerManager playerManager, RaceManager raceManager) {
        this.player = player;
        this.playerManager = playerManager;
        this.raceManager = raceManager;
    }

    /**
     * Inicia el flujo de creación mostrando la selección de raza.
     */
    public void start() {
        // Verificar si ya tiene personaje creado
        Optional<RPGPlayer> existingPlayer = playerManager.getPlayer(player.getUniqueId());

        if (existingPlayer.isPresent()) {
            RPGPlayer rpgPlayer = existingPlayer.get();

            // Verificar si ya completó la creación
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

        // Iniciar selección de raza
        showRaceSelection();
    }

    /**
     * Muestra la GUI de selección de raza.
     */
    private void showRaceSelection() {
        RaceSelectionGUI raceGUI = new RaceSelectionGUI(player, raceManager, this::onRaceSelected, true);
        raceGUI.open();
    }

    /**
     * Callback cuando se selecciona una raza.
     */
    private void onRaceSelected(String race) {
        this.selectedRace = race;

        player.sendMessage(Component.text("Has seleccionado la raza: ", NamedTextColor.GREEN)
                .append(Component.text(race, NamedTextColor.GOLD)));

        // Continuar con selección de clase
        showClassSelection();
    }

    /**
     * Muestra la GUI de selección de clase.
     */
    private void showClassSelection() {
        ClassSelectionGUI classGUI = new ClassSelectionGUI(player, selectedRace, this::onClassSelected, true);
        classGUI.open();
    }

    /**
     * Callback cuando se selecciona una clase.
     */
    private void onClassSelected(String playerClass) {
        this.selectedClass = playerClass;

        // Guardar el personaje
        saveCharacter();
    }

    /**
     * Guarda el personaje con la raza y clase seleccionadas.
     */
    private void saveCharacter() {
        Optional<RPGPlayer> rpgPlayerOpt = playerManager.getPlayer(player.getUniqueId());

        if (rpgPlayerOpt.isEmpty()) {
            player.sendMessage(Component.text("Error al crear personaje.", NamedTextColor.RED));
            return;
        }

        RPGPlayer rpgPlayer = rpgPlayerOpt.get();

        // Aplicar raza y clase
        RPGPlayer updatedPlayer = rpgPlayer
                .setRace(selectedRace)
                .setClass(selectedClass);

        // Guardar en base de datos
        playerManager.savePlayer(updatedPlayer);

        // Mensaje de éxito
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("════════════════════════════════", NamedTextColor.GOLD));
        player.sendMessage(Component.text("  ¡Personaje creado con éxito!", NamedTextColor.GREEN));
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("  Raza: ", NamedTextColor.YELLOW)
                .append(Component.text(selectedRace, NamedTextColor.WHITE)));
        player.sendMessage(Component.text("  Clase: ", NamedTextColor.YELLOW)
                .append(Component.text(selectedClass, NamedTextColor.WHITE)));
        player.sendMessage(Component.text("  Nivel: ", NamedTextColor.YELLOW)
                .append(Component.text("1", NamedTextColor.WHITE)));
        player.sendMessage(Component.text(""));
        player.sendMessage(Component.text("  Usa ", NamedTextColor.GRAY)
                .append(Component.text("/rpg stats", NamedTextColor.WHITE))
                .append(Component.text(" para ver tus estadísticas", NamedTextColor.GRAY)));
        player.sendMessage(Component.text("════════════════════════════════", NamedTextColor.GOLD));
        player.sendMessage(Component.text(""));
    }

}
