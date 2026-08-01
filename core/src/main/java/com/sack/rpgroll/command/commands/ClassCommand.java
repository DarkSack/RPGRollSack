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
                player.sendMessage(Component
                        .text("No se pudo cargar tu perfil de jugador. Por favor, inténtalo de nuevo más tarde.")
                        .color(NamedTextColor.RED));
                player.sendMessage(Component.text("Error al cargar tus datos.", NamedTextColor.RED));
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

            player.sendMessage(Component.text("Error al procesar comando de clase.", NamedTextColor.RED));
            exception.printStackTrace();

        }

    }

    private void showCurrentClass(Player player, RPGPlayer rpgPlayer) {

        String playerClass = rpgPlayer.getPlayerClass();

        if (playerClass == null || playerClass.isEmpty()) {
            player.sendMessage(Component.text("Aún no has seleccionado una clase.", NamedTextColor.YELLOW));
            player.sendMessage(Component.text("Usa " + "/rpg class <nombre>" +
                    " para seleccionar una.", NamedTextColor.GRAY));
            player.sendMessage(Component.text("Usa " + "/rpg class list" +
                    " para ver las clases disponibles.", NamedTextColor.GRAY));
        } else {
            player.sendMessage(Component.text("Tu clase actual: " + playerClass, NamedTextColor.GREEN));
        }

    }

    private void showAvailableClasses(Player player) {

        player.sendMessage(Component.text("========== Clases Disponibles ==========", NamedTextColor.GOLD));

        for (String className : AVAILABLE_CLASSES) {
            player.sendMessage(Component.text("• " + className, NamedTextColor.YELLOW));
        }

        player.sendMessage(Component.text("========================================", NamedTextColor.GOLD));
        player.sendMessage(Component.text("Usa " + "/rpg class <nombre>" +
                " para seleccionar una clase.", NamedTextColor.GRAY));

    }

    private void changeClass(Player player, PlayerManager playerManager, RPGPlayer rpgPlayer, String newClass) {

        // Validar que la clase exista
        boolean validClass = AVAILABLE_CLASSES.stream()
                .anyMatch(c -> c.equalsIgnoreCase(newClass));

        if (!validClass) {
            player.sendMessage(Component.text("Clase no válida: " + newClass, NamedTextColor.RED));
            player.sendMessage(Component.text("Usa " + "/rpg class list" +
                    " para ver las clases disponibles.", NamedTextColor.GRAY));
            return;
        }

        // Verificar si ya tiene clase
        if (rpgPlayer.getPlayerClass() != null && !rpgPlayer.getPlayerClass().isEmpty()) {
            player.sendMessage(Component.text("Ya tienes una clase seleccionada.", NamedTextColor.RED));
            player.sendMessage(
                    Component.text("El cambio de clase no está permitido actualmente.", NamedTextColor.YELLOW));
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

        player.sendMessage(Component.text("¡Has seleccionado la clase: " + formattedClass + "!", NamedTextColor.GREEN));

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
