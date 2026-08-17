package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.RPGCommand;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.character.CharacterCreationFlow;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.api.playerclass.ClassManager;

import com.sack.rpgroll.race.RaceAttributeApplier;
import com.sack.rpgroll.api.race.RaceManager;
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
        LangManager lang = plugin.getBootstrap().getServices().get(LangManager.class);

        try {

            PlayerManager playerManager = plugin.getBootstrap().getServices().get(PlayerManager.class);
            RaceManager raceManager = plugin.getBootstrap().getServices().get(RaceManager.class);
            ClassManager classManager = plugin.getBootstrap().getServices().get(ClassManager.class);
            RaceAttributeApplier raceAttributeApplier = plugin.getBootstrap().getServices()
                    .get(RaceAttributeApplier.class);

            CharacterCreationFlow flow = new CharacterCreationFlow(player, playerManager, raceManager, classManager,
                    raceAttributeApplier, lang);
            flow.start();

        } catch (Exception exception) {
            lang.send(player, "create_command.error");
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
