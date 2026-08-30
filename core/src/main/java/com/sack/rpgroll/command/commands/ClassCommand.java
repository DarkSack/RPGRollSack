package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.api.playerclass.ClassManager;
import com.sack.rpgroll.api.playerclass.PlayerClass;
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

    public ClassCommand(RPGRoll plugin) {
        this.plugin = plugin;
    }

    private ClassManager classManager() {
        return plugin.getBootstrap().getServices().get(ClassManager.class);
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

        String playerClassId = rpgPlayer.getPlayerClass();

        if (playerClassId == null || playerClassId.isEmpty()) {
            lang.send(player, "class.no_class");
            lang.send(player, "class_command.hint_select");
            lang.send(player, "class_command.hint_list");
        } else {
            String displayName = classManager().get(playerClassId).map(PlayerClass::displayName)
                    .orElse(playerClassId);
            lang.send(player, "class.current", "class", displayName);
        }

    }

    private void showAvailableClasses(Player player, LangManager lang) {

        lang.send(player, "class_command.list_header");

        for (PlayerClass playerClass : classManager().getAll()) {
            lang.send(player, "class_command.list_entry", "class", playerClass.displayName());
        }

        lang.send(player, "class_command.list_footer");
        lang.send(player, "class_command.list_hint");

    }

    private void changeClass(Player player, PlayerManager playerManager, RPGPlayer rpgPlayer, String newClassId,
            LangManager lang) {

        Optional<PlayerClass> classOpt = classManager().get(newClassId);

        if (classOpt.isEmpty()) {
            lang.send(player, "class_command.invalid_class", "class", newClassId);
            lang.send(player, "class_command.hint_list");
            return;
        }

        // Verificar si ya tiene clase
        if (rpgPlayer.getPlayerClass() != null && !rpgPlayer.getPlayerClass().isEmpty()) {
            lang.send(player, "class_command.already_selected");
            lang.send(player, "class_command.change_not_allowed");
            return;
        }

        PlayerClass playerClass = classOpt.get();

        // Actualizar y guardar — se persiste el id (mismo criterio que CharacterCreationFlow/AdminSetClassCommand).
        RPGPlayer updatedPlayer = rpgPlayer.setClass(playerClass.id());
        playerManager.savePlayer(updatedPlayer);

        lang.send(player, "class.select", "class", playerClass.displayName());

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
            List<String> options = new java.util.ArrayList<>(
                    classManager().getAll().stream().map(PlayerClass::id).toList());
            options.add("list");
            return options;
        }
        return List.of();
    }

    @Override
    public String getPermission() {
        return "rpgroll.player.class";
    }

}
