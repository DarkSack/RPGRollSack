package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.RPGCommand;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.player.RPGPlayer;

import com.sack.rpgroll.player.PlayerManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

/**
 * Comando para ver o cambiar la raza del jugador.
 * Uso: /rpg race [nombre]
 */
public class RaceCommand implements RPGCommand {

    private final RPGRoll plugin;

    // Razas disponibles por ahora (placeholder hasta implementar sistema completo)
    private static final List<String> AVAILABLE_RACES = List.of(
            "Humano", "Elfo", "Enano", "Orco", "Halfling", "Tiefling", "Dracónido");

    public RaceCommand(RPGRoll plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

        Player player = (Player) sender;
        LangManager lang = plugin.getBootstrap().getServices().get(LangManager.class);

        try {

            PlayerManager playerManager = plugin.getBootstrap()
                    .getServices()
                    .get(PlayerManager.class);

            Optional<RPGPlayer> rpgPlayer = playerManager.getPlayer(player.getUniqueId());

            if (rpgPlayer.isEmpty()) {
                lang.send(player, "race_command.profile_load_error");
                lang.send(player, "race_command.data_load_error");
                return;
            }

            // Sin argumentos: mostrar raza actual
            if (args.length == 0) {
                showCurrentRace(player, rpgPlayer.get(), lang);
                return;
            }

            // Con argumento "list": mostrar razas disponibles
            if (args[0].equalsIgnoreCase("list")) {
                showAvailableRaces(player, lang);
                return;
            }

            // Con argumento: cambiar raza
            String newRace = args[0];
            changeRace(player, playerManager, rpgPlayer.get(), newRace, lang);

        } catch (Exception exception) {

            lang.send(player, "race_command.process_error");
            exception.printStackTrace();

        }

    }

    private void showCurrentRace(Player player, RPGPlayer rpgPlayer, LangManager lang) {

        String playerRace = rpgPlayer.getRace();

        if (playerRace == null || playerRace.isEmpty()) {
            lang.send(player, "race.no_race");
            lang.send(player, "race_command.hint_select");
            lang.send(player, "race_command.hint_list");
        } else {
            lang.send(player, "race.current", "race", playerRace);
        }

    }

    private void showAvailableRaces(Player player, LangManager lang) {

        lang.send(player, "race_command.list_header");

        for (String raceName : AVAILABLE_RACES) {
            lang.send(player, "race_command.list_entry", "race", raceName);
        }

        lang.send(player, "race_command.list_footer");
        lang.send(player, "race_command.list_hint");

    }

    private void changeRace(Player player, PlayerManager playerManager, RPGPlayer rpgPlayer, String newRace,
            LangManager lang) {

        // Validar que la raza exista
        boolean validRace = AVAILABLE_RACES.stream()
                .anyMatch(r -> r.equalsIgnoreCase(newRace));

        if (!validRace) {
            lang.send(player, "race_command.invalid_race", "race", newRace);
            lang.send(player, "race_command.hint_list");
            return;
        }

        // Verificar si ya tiene raza
        String currentRace = rpgPlayer.getRace();

        // Verificar si puede cambiar de raza
        if (currentRace != null && !currentRace.isEmpty()) {
            // TODO: Verificar configuración allow_race_change
            lang.send(player, "race_command.already_selected");
            lang.send(player, "race_command.change_not_allowed");
            return;
        }

        // Capitalizar correctamente el nombre de la raza
        String formattedRace = AVAILABLE_RACES.stream()
                .filter(r -> r.equalsIgnoreCase(newRace))
                .findFirst()
                .orElse(newRace);

        // Actualizar y guardar
        RPGPlayer updatedPlayer = rpgPlayer.setRace(formattedRace);
        playerManager.savePlayer(updatedPlayer);

        lang.send(player, "race.select", "race", formattedRace);

    }

    @Override
    public String getName() {
        return "race";
    }

    @Override
    public String getDescription() {
        return "Muestra o cambia tu raza";
    }

    @Override
    public String getUsage() {
        return "/rpg race [nombre|list]";
    }

    @Override
    public List<String> getAliases() {
        return List.of("raza", "r");
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        if (args.length <= 1) {
            List<String> options = new java.util.ArrayList<>(AVAILABLE_RACES);
            options.add("list");
            return options;
        }
        return List.of();
    }

}
