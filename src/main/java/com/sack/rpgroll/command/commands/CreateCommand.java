package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.RPGCommand;
import com.sack.rpgroll.gui.character.CharacterCreationFlow;
import com.sack.rpgroll.player.PlayerManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Comando para iniciar la creación de personaje.
 * Uso: /rpg create
 */
public class CreateCommand implements RPGCommand {

    private final RPGRoll plugin;

    public CreateCommand(RPGRoll plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

        Player player = (Player) sender;

        try {

            PlayerManager playerManager = plugin.getBootstrap()
                    .getServices()
                    .get(PlayerManager.class);

            // Iniciar flujo de creación
            CharacterCreationFlow flow = new CharacterCreationFlow(player, playerManager);
            flow.start();

        } catch (Exception exception) {
            player.sendMessage(
                    Component.text("Ocurrió un error al iniciar la creación de personaje.", NamedTextColor.RED));
            player.sendMessage(Component.text("Error al iniciar creación de personaje.", NamedTextColor.RED));
            exception.printStackTrace();

        }

    }

    @Override
    public String getName() {
        return "create";
    }

    @Override
    public String getDescription() {
        return "Inicia la creación de tu personaje";
    }

    @Override
    public String getUsage() {
        return "/rpg create";
    }

    @Override
    public List<String> getAliases() {
        return List.of("crear", "nuevo");
    }

}
