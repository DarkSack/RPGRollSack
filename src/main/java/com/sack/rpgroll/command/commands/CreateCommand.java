package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.RPGCommand;
import com.sack.rpgroll.gui.character.CharacterCreationFlow;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.playerclass.ClassManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import com.sack.rpgroll.race.RaceAttributeApplier;
import com.sack.rpgroll.race.RaceManager;
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

            PlayerManager playerManager = plugin.getBootstrap().getServices().get(PlayerManager.class);
            RaceManager raceManager = plugin.getBootstrap().getServices().get(RaceManager.class);
            ClassManager classManager = plugin.getBootstrap().getServices().get(ClassManager.class);
            RaceAttributeApplier raceAttributeApplier = plugin.getBootstrap().getServices()
                    .get(RaceAttributeApplier.class);

            CharacterCreationFlow flow = new CharacterCreationFlow(player, playerManager, raceManager, classManager,
                    raceAttributeApplier);
            flow.start();

        } catch (Exception exception) {
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
