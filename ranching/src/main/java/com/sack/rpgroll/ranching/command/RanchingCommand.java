package com.sack.rpgroll.ranching.command;

import com.sack.rpgroll.common.command.Senders;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.ranching.core.animal.AnimalManager;
import com.sack.rpgroll.ranching.core.breeds.BreedManager;
import com.sack.rpgroll.ranching.core.species.SpeciesManager;
import com.sack.rpgroll.ranching.gui.AnimalDetailGUI;
import com.sack.rpgroll.ranching.gui.ChatPromptManager;
import com.sack.rpgroll.util.TabCompleteUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.List;

/** /ranching inspect — abre la ficha del animal al que estás mirando. */
public class RanchingCommand implements CommandExecutor, TabCompleter {

    private final AnimalManager animalManager;
    private final SpeciesManager speciesManager;
    private final BreedManager breedManager;
    private final ChatPromptManager chatPromptManager;

    public RanchingCommand(AnimalManager animalManager, SpeciesManager speciesManager, BreedManager breedManager,
            ChatPromptManager chatPromptManager) {
        this.animalManager = animalManager;
        this.speciesManager = speciesManager;
        this.breedManager = breedManager;
        this.chatPromptManager = chatPromptManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(Senders.asPlayer(sender) instanceof Player player)) {
            sender.sendMessage(chatPromptManager.lang().raw("command.player_only"));
            return true;
        }

        if (args.length < 1 || !args[0].equalsIgnoreCase("inspect")) {
            player.sendMessage(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("command.ranching.usage_inspect"), NamedTextColor.RED));
            return true;
        }

        Entity target = player.getTargetEntity(6);
        var animal = target != null ? animalManager.resolve(target) : java.util.Optional.<com.sack.rpgroll.ranching.core.animal.Animal>empty();

        if (animal.isEmpty()) {
            player.sendMessage(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("command.ranching.look_at_animal"), NamedTextColor.RED));
            return true;
        }

        new AnimalDetailGUI(player, animal.get(), speciesManager, breedManager, chatPromptManager, player::closeInventory).open();
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], List.of("inspect"));
        }

        return List.of();
    }

}
