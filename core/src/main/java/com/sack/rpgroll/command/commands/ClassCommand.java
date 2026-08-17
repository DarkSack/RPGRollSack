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
        LangManager lang = plugin.getBootstrap().getServices().get(LangManager.class);

        try {

            PlayerManager playerManager = plugin.getBootstrap()
                    .getServices()
                    .get(PlayerManager.class);

            Optional<RPGPlayer> rpgPlayer = playerManager.getPlayer(player.getUniqueId());

            if (rpgPlayer.isEmpty()) {
                lang.send(player, "class_command.profile_load_error");
                lang.send(player, "class_command.data_load_error");
                return;
            }

            // Sin argumentos: mostrar clase actual
            if (args.length == 0) {
                showCurrentClass(player, rpgPlayer.get(), lang);
                return;
            }

            // Con argumento "list": mostrar clases disponibles
            if (args[0].equalsIgnoreCase("list")) {
                showAvailableClasses(player, lang);
                return;
            }

            // Con argumento: cambiar clase
            String newClass = args[0];
            changeClass(player, playerManager, rpgPlayer.get(), newClass, lang);

        } catch (Exception exception) {

            lang.send(player, "class_command.process_error");
            exception.printStackTrace();

        }

    }

    private void showCurrentClass(Player player, RPGPlayer rpgPlayer, LangManager lang) {

        String playerClass = rpgPlayer.getPlayerClass();

        if (playerClass == null || playerClass.isEmpty()) {
            lang.send(player, "class.no_class");
            lang.send(player, "class_command.hint_select");
            lang.send(player, "class_command.hint_list");
        } else {
            lang.send(player, "class.current", "class", playerClass);
        }

    }

    private void showAvailableClasses(Player player, LangManager lang) {

        lang.send(player, "class_command.list_header");

        for (String className : AVAILABLE_CLASSES) {
            lang.send(player, "class_command.list_entry", "class", className);
        }

        lang.send(player, "class_command.list_footer");
        lang.send(player, "class_command.list_hint");

    }

    private void changeClass(Player player, PlayerManager playerManager, RPGPlayer rpgPlayer, String newClass,
            LangManager lang) {

        // Validar que la clase exista
        boolean validClass = AVAILABLE_CLASSES.stream()
                .anyMatch(c -> c.equalsIgnoreCase(newClass));

        if (!validClass) {
            lang.send(player, "class_command.invalid_class", "class", newClass);
            lang.send(player, "class_command.hint_list");
            return;
        }

        // Verificar si ya tiene clase
        if (rpgPlayer.getPlayerClass() != null && !rpgPlayer.getPlayerClass().isEmpty()) {
            lang.send(player, "class_command.already_selected");
            lang.send(player, "class_command.change_not_allowed");
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

        lang.send(player, "class.select", "class", formattedClass);

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

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        if (args.length <= 1) {
            List<String> options = new java.util.ArrayList<>(AVAILABLE_CLASSES);
            options.add("list");
            return options;
        }
        return List.of();
    }

}
