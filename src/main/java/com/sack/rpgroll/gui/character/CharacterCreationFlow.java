package com.sack.rpgroll.gui.character;

import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * Coordinador del flujo de creación de personaje.
 * Maneja la secuencia: Raza → Clase → Guardar
 */
public class CharacterCreationFlow {

    private final Player player;
    private final PlayerManager playerManager;

    private String selectedRace;
    private String selectedClass;

    public CharacterCreationFlow(Player player, PlayerManager playerManager) {
        this.player = player;
        this.playerManager = playerManager;
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

                player.sendMessage(ChatColor.RED + "Ya tienes un personaje creado.");
                player.sendMessage(ChatColor.YELLOW + "Raza: " + ChatColor.WHITE + rpgPlayer.getRace());
                player.sendMessage(ChatColor.YELLOW + "Clase: " + ChatColor.WHITE + rpgPlayer.getPlayerClass());
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
        RaceSelectionGUI raceGUI = new RaceSelectionGUI(player, this::onRaceSelected);
        raceGUI.open();
    }

    /**
     * Callback cuando se selecciona una raza.
     */
    private void onRaceSelected(String race) {
        this.selectedRace = race;

        player.sendMessage(ChatColor.GREEN + "Has seleccionado la raza: " + ChatColor.GOLD + race);

        // Continuar con selección de clase
        showClassSelection();
    }

    /**
     * Muestra la GUI de selección de clase.
     */
    private void showClassSelection() {
        ClassSelectionGUI classGUI = new ClassSelectionGUI(player, selectedRace, this::onClassSelected);
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
            player.sendMessage(ChatColor.RED + "Error al crear personaje.");
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
        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "════════════════════════════════");
        player.sendMessage(ChatColor.GREEN + "  ¡Personaje creado con éxito!");
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "  Raza: " + ChatColor.WHITE + selectedRace);
        player.sendMessage(ChatColor.YELLOW + "  Clase: " + ChatColor.WHITE + selectedClass);
        player.sendMessage(ChatColor.YELLOW + "  Nivel: " + ChatColor.WHITE + "1");
        player.sendMessage("");
        player.sendMessage(ChatColor.GRAY + "  Usa " + ChatColor.WHITE + "/rpg stats" +
                ChatColor.GRAY + " para ver tus estadísticas");
        player.sendMessage(ChatColor.GOLD + "════════════════════════════════");
        player.sendMessage("");
    }

}
