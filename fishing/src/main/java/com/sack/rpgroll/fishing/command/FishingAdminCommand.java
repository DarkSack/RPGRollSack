package com.sack.rpgroll.fishing.command;

import com.sack.rpgroll.fishing.core.BaitManager;
import com.sack.rpgroll.fishing.core.FishSpeciesManager;
import com.sack.rpgroll.fishing.core.FishingRegionManager;
import com.sack.rpgroll.fishing.core.FishingRodManager;
import com.sack.rpgroll.fishing.core.JunkManager;
import com.sack.rpgroll.fishing.core.TreasureManager;
import com.sack.rpgroll.fishing.gui.ChatPromptManager;
import com.sack.rpgroll.fishing.gui.FishingBrowserGUI;
import com.sack.rpgroll.fishing.item.FishingItemFactory;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /fishingadmin browser
 * /fishingadmin reload
 * /fishingadmin giverod <id>
 * /fishingadmin givebait <id>
 */
public class FishingAdminCommand implements CommandExecutor {

    private final FishSpeciesManager speciesManager;
    private final FishingRodManager rodManager;
    private final BaitManager baitManager;
    private final TreasureManager treasureManager;
    private final JunkManager junkManager;
    private final FishingRegionManager regionManager;
    private final ChatPromptManager chatPromptManager;

    public FishingAdminCommand(FishSpeciesManager speciesManager, FishingRodManager rodManager,
            BaitManager baitManager, TreasureManager treasureManager, JunkManager junkManager,
            FishingRegionManager regionManager, ChatPromptManager chatPromptManager) {
        this.speciesManager = speciesManager;
        this.rodManager = rodManager;
        this.baitManager = baitManager;
        this.treasureManager = treasureManager;
        this.junkManager = junkManager;
        this.regionManager = regionManager;
        this.chatPromptManager = chatPromptManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("rpgrollfishing.admin.*")) {
            sender.sendMessage(Component.text("No tenés permiso para usar este comando.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "browser" -> handleBrowser(sender);
            case "reload" -> handleReload(sender);
            case "giverod" -> handleGiveRod(sender, args);
            case "givebait" -> handleGiveBait(sender, args);
            default -> sendUsage(sender);
        }

        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text(
                "Uso: /fishingadmin <browser|reload|giverod <id>|givebait <id>>", NamedTextColor.RED));
    }

    private void handleBrowser(CommandSender sender) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo jugadores pueden abrir el Fishing Studio.", NamedTextColor.RED));
            return;
        }

        new FishingBrowserGUI(player, speciesManager, rodManager, baitManager, treasureManager, junkManager,
                regionManager, chatPromptManager).open();
    }

    private void handleReload(CommandSender sender) {

        speciesManager.reload();
        rodManager.reload();
        baitManager.reload();
        treasureManager.reload();
        junkManager.reload();
        regionManager.reload();

        sender.sendMessage(Component.text("✔ Recargado: " + speciesManager.count() + " especie(s), "
                + rodManager.count() + " caña(s), " + baitManager.count() + " carnada(s), "
                + treasureManager.count() + " tesoro(s), " + junkManager.count() + " basura(s), "
                + regionManager.count() + " región(es).", NamedTextColor.GREEN));
    }

    private void handleGiveRod(CommandSender sender, String[] args) {

        if (!(sender instanceof Player player) || args.length < 2) {
            sender.sendMessage(Component.text("Uso: /fishingadmin giverod <id>", NamedTextColor.RED));
            return;
        }

        var rodOpt = rodManager.get(args[1]);

        if (rodOpt.isEmpty()) {
            sender.sendMessage(Component.text("No existe una caña con id: " + args[1], NamedTextColor.RED));
            return;
        }

        player.getInventory().addItem(FishingItemFactory.createRod(rodOpt.get()));
        sender.sendMessage(Component.text("✔ Entregada.", NamedTextColor.GREEN));
    }

    private void handleGiveBait(CommandSender sender, String[] args) {

        if (!(sender instanceof Player player) || args.length < 2) {
            sender.sendMessage(Component.text("Uso: /fishingadmin givebait <id>", NamedTextColor.RED));
            return;
        }

        var baitOpt = baitManager.get(args[1]);

        if (baitOpt.isEmpty()) {
            sender.sendMessage(Component.text("No existe una carnada con id: " + args[1], NamedTextColor.RED));
            return;
        }

        player.getInventory().addItem(FishingItemFactory.createBait(baitOpt.get()));
        sender.sendMessage(Component.text("✔ Entregada.", NamedTextColor.GREEN));
    }

}
