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
                player.sendMessage(ChatColor.RED + "Error al cargar tus datos.");
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

            player.sendMessage(ChatColor.RED + "Error al procesar comando de clase.");
            exception.printStackTrace();

        }

    }

    private void showCurrentClass(Player player, RPGPlayer rpgPlayer) {

        String playerClass = rpgPlayer.getPlayerClass();

        if (playerClass == null || playerClass.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Aún no has seleccionado una clase.");
            player.sendMessage(ChatColor.GRAY + "Usa " + ChatColor.WHITE + "/rpg class <nombre>" +
                    ChatColor.GRAY + " para seleccionar una.");
            player.sendMessage(ChatColor.GRAY + "Usa " + ChatColor.WHITE + "/rpg class list" +
                    ChatColor.GRAY + " para ver las clases disponibles.");
        } else {
            player.sendMessage(ChatColor.GREEN + "Tu clase actual: " +
                    ChatColor.GOLD + playerClass);
        }

    }

    private void showAvailableClasses(Player player) {

        player.sendMessage(ChatColor.GOLD + "========== Clases Disponibles ==========");

        for (String className : AVAILABLE_CLASSES) {
            player.sendMessage(ChatColor.YELLOW + "• " + ChatColor.WHITE + className);
        }

        player.sendMessage(ChatColor.GOLD + "========================================");
        player.sendMessage(ChatColor.GRAY + "Usa " + ChatColor.WHITE + "/rpg class <nombre>" +
                ChatColor.GRAY + " para seleccionar una clase.");

    }

    private void changeClass(Player player, PlayerManager playerManager, RPGPlayer rpgPlayer, String newClass) {

        // Validar que la clase exista
        boolean validClass = AVAILABLE_CLASSES.stream()
                .anyMatch(c -> c.equalsIgnoreCase(newClass));

        if (!validClass) {
            player.sendMessage(ChatColor.RED + "Clase no válida: " + newClass);
            player.sendMessage(ChatColor.GRAY + "Usa " + ChatColor.WHITE + "/rpg class list" +
                    ChatColor.GRAY + " para ver las clases disponibles.");
            return;
        }

        // Verificar si ya tiene clase
        if (rpgPlayer.getPlayerClass() != null && !rpgPlayer.getPlayerClass().isEmpty()) {
            player.sendMessage(ChatColor.RED + "Ya tienes una clase seleccionada.");
            player.sendMessage(ChatColor.YELLOW + "El cambio de clase no está permitido actualmente.");
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

        player.sendMessage(ChatColor.GREEN + "¡Has seleccionado la clase: " +
                ChatColor.GOLD + formattedClass + ChatColor.GREEN + "!");

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
