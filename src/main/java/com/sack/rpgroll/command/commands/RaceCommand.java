package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.RPGCommand;
import com.sack.rpgroll.player.RPGPlayer;
import com.sack.rpgroll.player.PlayerManager;
import org.bukkit.ChatColor;
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

        try {

            PlayerManager playerManager = plugin.getBootstrap()
                    .getServices()
                    .get(PlayerManager.class);

            Optional<RPGPlayer> rpgPlayer = playerManager.getPlayer(player.getUniqueId());

            if (rpgPlayer.isEmpty()) {
                player.sendMessage(ChatColor.RED + "Error al cargar tus datos.");
                return;
            }

            // Sin argumentos: mostrar raza actual
            if (args.length == 0) {
                showCurrentRace(player, rpgPlayer.get());
                return;
            }

            // Con argumento "list": mostrar razas disponibles
            if (args[0].equalsIgnoreCase("list")) {
                showAvailableRaces(player);
                return;
            }

            // Con argumento: cambiar raza
            String newRace = args[0];
            changeRace(player, playerManager, rpgPlayer.get(), newRace);

        } catch (Exception exception) {

            player.sendMessage(ChatColor.RED + "Error al procesar comando de raza.");
            exception.printStackTrace();

        }

    }

    private void showCurrentRace(Player player, RPGPlayer rpgPlayer) {

        String playerRace = rpgPlayer.getRace();

        if (playerRace == null || playerRace.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Aún no has seleccionado una raza.");
            player.sendMessage(ChatColor.GRAY + "Usa " + ChatColor.WHITE + "/rpg race <nombre>" +
                    ChatColor.GRAY + " para seleccionar una.");
            player.sendMessage(ChatColor.GRAY + "Usa " + ChatColor.WHITE + "/rpg race list" +
                    ChatColor.GRAY + " para ver las razas disponibles.");
        } else {
            player.sendMessage(ChatColor.GREEN + "Tu raza actual: " +
                    ChatColor.GOLD + playerRace);
        }

    }

    private void showAvailableRaces(Player player) {

        player.sendMessage(ChatColor.GOLD + "========== Razas Disponibles ==========");

        for (String raceName : AVAILABLE_RACES) {
            player.sendMessage(ChatColor.YELLOW + "• " + ChatColor.WHITE + raceName);
        }

        player.sendMessage(ChatColor.GOLD + "=======================================");
        player.sendMessage(ChatColor.GRAY + "Usa " + ChatColor.WHITE + "/rpg race <nombre>" +
                ChatColor.GRAY + " para seleccionar una raza.");

    }

    private void changeRace(Player player, PlayerManager playerManager, RPGPlayer rpgPlayer, String newRace) {

        // Validar que la raza exista
        boolean validRace = AVAILABLE_RACES.stream()
                .anyMatch(r -> r.equalsIgnoreCase(newRace));

        if (!validRace) {
            player.sendMessage(ChatColor.RED + "Raza no válida: " + newRace);
            player.sendMessage(ChatColor.GRAY + "Usa " + ChatColor.WHITE + "/rpg race list" +
                    ChatColor.GRAY + " para ver las razas disponibles.");
            return;
        }

        // Verificar si ya tiene raza
        String currentRace = rpgPlayer.getRace();

        // Verificar si puede cambiar de raza
        if (currentRace != null && !currentRace.isEmpty()) {
            // TODO: Verificar configuración allow_race_change
            player.sendMessage(ChatColor.RED + "Ya tienes una raza seleccionada.");
            player.sendMessage(ChatColor.YELLOW + "El cambio de raza no está permitido actualmente.");
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

        player.sendMessage(ChatColor.GREEN + "¡Has seleccionado la raza: " +
                ChatColor.GOLD + formattedRace + ChatColor.GREEN + "!");

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

}
