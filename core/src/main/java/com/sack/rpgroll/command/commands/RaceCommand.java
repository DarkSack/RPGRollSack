package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.RPGCommand;
import com.sack.rpgroll.player.RPGPlayer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

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

        try {

            PlayerManager playerManager = plugin.getBootstrap()
                    .getServices()
                    .get(PlayerManager.class);

            Optional<RPGPlayer> rpgPlayer = playerManager.getPlayer(player.getUniqueId());

            if (rpgPlayer.isEmpty()) {
                player.sendMessage(
                        Component.text("No se encontraron estadísticas para tu personaje.", NamedTextColor.RED));
                player.sendMessage(Component.text("Error al cargar tus datos.", NamedTextColor.RED));
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

            player.sendMessage(Component.text("Error al procesar comando de raza.", NamedTextColor.RED));
            exception.printStackTrace();

        }

    }

    private void showCurrentRace(Player player, RPGPlayer rpgPlayer) {

        String playerRace = rpgPlayer.getRace();

        if (playerRace == null || playerRace.isEmpty()) {
            player.sendMessage(Component.text("Aún no has seleccionado una raza.", NamedTextColor.YELLOW));
            player.sendMessage(Component.text("Usa /rpg race <nombre> para seleccionar una.", NamedTextColor.GRAY));
            player.sendMessage(
                    Component.text("Usa /rpg race list para ver las razas disponibles.", NamedTextColor.GRAY));
        } else {
            player.sendMessage(Component.text("Tu raza actual: " + playerRace, NamedTextColor.GREEN));
        }

    }

    private void showAvailableRaces(Player player) {

        player.sendMessage(Component.text("========== Razas Disponibles ==========", NamedTextColor.GOLD));

        for (String raceName : AVAILABLE_RACES) {
            player.sendMessage(Component.text("• " + raceName, NamedTextColor.YELLOW));
        }

        player.sendMessage(Component.text("=======================================", NamedTextColor.GOLD));
        player.sendMessage(
                Component.text("Usa " + "/rpg race <nombre>" + " para seleccionar una raza.", NamedTextColor.GRAY));

    }

    private void changeRace(Player player, PlayerManager playerManager, RPGPlayer rpgPlayer, String newRace) {

        // Validar que la raza exista
        boolean validRace = AVAILABLE_RACES.stream()
                .anyMatch(r -> r.equalsIgnoreCase(newRace));

        if (!validRace) {
            player.sendMessage(Component.text("Raza no válida: " + newRace, NamedTextColor.RED));
            player.sendMessage(Component.text("Usa " + "/rpg race list" + " para ver las razas disponibles.",
                    NamedTextColor.GRAY));
            return;
        }

        // Verificar si ya tiene raza
        String currentRace = rpgPlayer.getRace();

        // Verificar si puede cambiar de raza
        if (currentRace != null && !currentRace.isEmpty()) {
            // TODO: Verificar configuración allow_race_change
            player.sendMessage(Component.text("Ya tienes una raza seleccionada.", NamedTextColor.RED));
            player.sendMessage(
                    Component.text("El cambio de raza no está permitido actualmente.", NamedTextColor.YELLOW));
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

        player.sendMessage(Component.text("¡Has seleccionado la raza: " + formattedRace + "!", NamedTextColor.GREEN));

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
