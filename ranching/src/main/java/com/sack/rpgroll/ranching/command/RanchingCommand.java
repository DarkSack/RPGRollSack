package com.sack.rpgroll.ranching.command;

import com.sack.rpgroll.ranching.core.animal.AnimalManager;
import com.sack.rpgroll.ranching.core.breeds.BreedManager;
import com.sack.rpgroll.ranching.core.species.SpeciesManager;
import com.sack.rpgroll.ranching.gui.AnimalDetailGUI;
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

    public RanchingCommand(AnimalManager animalManager, SpeciesManager speciesManager, BreedManager breedManager) {
        this.animalManager = animalManager;
        this.speciesManager = speciesManager;
        this.breedManager = breedManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo jugadores pueden usar este comando.");
            return true;
        }

        if (args.length < 1 || !args[0].equalsIgnoreCase("inspect")) {
            player.sendMessage(Component.text("Uso: /ranching inspect", NamedTextColor.RED));
            return true;
        }

        Entity target = player.getTargetEntity(6);
        var animal = target != null ? animalManager.resolve(target) : java.util.Optional.<com.sack.rpgroll.ranching.core.animal.Animal>empty();

        if (animal.isEmpty()) {
            player.sendMessage(Component.text("Mirá directamente a un animal rastreado por Ranching.", NamedTextColor.RED));
            return true;
        }

        new AnimalDetailGUI(player, animal.get(), speciesManager, breedManager, player::closeInventory).open();
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
