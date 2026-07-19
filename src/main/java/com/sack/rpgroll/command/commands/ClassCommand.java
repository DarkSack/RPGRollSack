package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.RPGCommand;
import com.sack.rpgroll.player.RPGPlayer;

import net.kyori.adventure.text.format.NamedTextColor;

import com.sack.rpgroll.player.PlayerManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

/**
 * Comando para ver o cambiar la clase del jugador.
 * Uso: /rpg class [nombre]
 */
public class ClassCommand implements RPGCommand {

    private final RPGRoll plugin;

    // Clases disponibles por ahora (placeholder hasta implementar sistema completo)
    private static final List<String> AVAILABLE_CLASSES = List.of(
            "Guerrero", "Mago", "Pícaro", "Clérigo", "Paladín", "Druida");

    public ClassCommand(RPGRoll plugin) {
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
                player.sendMessage(NamedTextColor.RED + "Error al cargar tus datos.");
                return;
            }

            // Sin argumentos: mostrar clase actual
            if (args.length == 0) {
                showCurrentClass(player, rpgPlayer.get());
                return;
            }

            // Con argumento "list": mostrar clases disponibles
            if (args[0].equalsIgnoreCase("list")) {
                showAvailableClasses(player);
                return;
            }

            // Con argumento: cambiar clase
            String newClass = args[0];
            changeClass(player, playerManager, rpgPlayer.get(), newClass);

        } catch (Exception exception) {

            player.sendMessage(NamedTextColor.RED + "Error al procesar comando de clase.");
            exception.printStackTrace();

        }

    }

    private void showCurrentClass(Player player, RPGPlayer rpgPlayer) {

        String playerClass = rpgPlayer.getPlayerClass();

        if (playerClass == null || playerClass.isEmpty()) {
            player.sendMessage(NamedTextColor.YELLOW + "Aún no has seleccionado una clase.");
            player.sendMessage(NamedTextColor.GRAY + "Usa " + NamedTextColor.WHITE + "/rpg class <nombre>" +
                    NamedTextColor.GRAY + " para seleccionar una.");
            player.sendMessage(NamedTextColor.GRAY + "Usa " + NamedTextColor.WHITE + "/rpg class list" +
                    NamedTextColor.GRAY + " para ver las clases disponibles.");
        } else {
            player.sendMessage(NamedTextColor.GREEN + "Tu clase actual: " +
                    NamedTextColor.GOLD + playerClass);
        }

    }

    private void showAvailableClasses(Player player) {

        player.sendMessage(NamedTextColor.GOLD + "========== Clases Disponibles ==========");

        for (String className : AVAILABLE_CLASSES) {
            player.sendMessage(NamedTextColor.YELLOW + "• " + NamedTextColor.WHITE + className);
        }

        player.sendMessage(NamedTextColor.GOLD + "========================================");
        player.sendMessage(NamedTextColor.GRAY + "Usa " + NamedTextColor.WHITE + "/rpg class <nombre>" +
                NamedTextColor.GRAY + " para seleccionar una clase.");

    }

    private void changeClass(Player player, PlayerManager playerManager, RPGPlayer rpgPlayer, String newClass) {

        // Validar que la clase exista
        boolean validClass = AVAILABLE_CLASSES.stream()
                .anyMatch(c -> c.equalsIgnoreCase(newClass));

        if (!validClass) {
            player.sendMessage(NamedTextColor.RED + "Clase no válida: " + newClass);
            player.sendMessage(NamedTextColor.GRAY + "Usa " + NamedTextColor.WHITE + "/rpg class list" +
                    NamedTextColor.GRAY + " para ver las clases disponibles.");
            return;
        }

        // Verificar si ya tiene clase
        if (rpgPlayer.getPlayerClass() != null && !rpgPlayer.getPlayerClass().isEmpty()) {
            player.sendMessage(NamedTextColor.RED + "Ya tienes una clase seleccionada.");
            player.sendMessage(NamedTextColor.YELLOW + "El cambio de clase no está permitido actualmente.");
            return;
        }

        // Capitalizar correctamente el nombre de la clase
        String formattedClass = AVAILABLE_CLASSES.stream()
                .filter(c -> c.equalsIgnoreCase(newClass))
                .findFirst()
                .orElse(newClass);

        // Actualizar y guardar
        RPGPlayer updatedPlayer = rpgPlayer.setClass(formattedClass);
        playerManager.savePlayer(updatedPlayer);

        player.sendMessage(NamedTextColor.GREEN + "¡Has seleccionado la clase: " +
                NamedTextColor.GOLD + formattedClass + NamedTextColor.GREEN + "!");

    }

    @Override
    public String getName() {
        return "class";
    }

    @Override
    public String getDescription() {
        return "Muestra o cambia tu clase";
    }

    @Override
    public String getUsage() {
        return "/rpg class [nombre|list]";
    }

    @Override
    public List<String> getAliases() {
        return List.of("clase", "c");
    }

}
