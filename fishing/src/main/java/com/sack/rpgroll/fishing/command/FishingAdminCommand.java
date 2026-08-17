package com.sack.rpgroll.fishing.command;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.fishing.FishingPlugin;
import com.sack.rpgroll.fishing.core.BaitManager;
import com.sack.rpgroll.fishing.core.FishSpeciesManager;
import com.sack.rpgroll.fishing.core.FishingRegionManager;
import com.sack.rpgroll.fishing.core.FishingRodManager;
import com.sack.rpgroll.fishing.core.JunkManager;
import com.sack.rpgroll.fishing.core.TreasureManager;
import com.sack.rpgroll.fishing.gui.ChatPromptManager;
import com.sack.rpgroll.fishing.gui.FishingBrowserGUI;
import com.sack.rpgroll.fishing.item.FishingItemFactory;
import com.sack.rpgroll.util.TabCompleteUtil;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /fishingadmin browser
 * /fishingadmin reload
 * /fishingadmin giverod <id>
 * /fishingadmin givebait <id>
 */
public class FishingAdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("browser", "reload", "giverod", "givebait");

    private final FishSpeciesManager speciesManager;
    private final FishingRodManager rodManager;
    private final BaitManager baitManager;
    private final TreasureManager treasureManager;
    private final JunkManager junkManager;
    private final FishingRegionManager regionManager;
    private final ChatPromptManager chatPromptManager;
    private final FishingPlugin plugin;
    private final LangManager lang;

    public FishingAdminCommand(FishSpeciesManager speciesManager, FishingRodManager rodManager,
            BaitManager baitManager, TreasureManager treasureManager, JunkManager junkManager,
            FishingRegionManager regionManager, ChatPromptManager chatPromptManager, FishingPlugin plugin) {
        this.speciesManager = speciesManager;
        this.rodManager = rodManager;
        this.baitManager = baitManager;
        this.treasureManager = treasureManager;
        this.junkManager = junkManager;
        this.regionManager = regionManager;
        this.chatPromptManager = chatPromptManager;
        this.plugin = plugin;
        this.lang = chatPromptManager.lang();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("rpgrollfishing.admin.*")) {
            lang.send(sender, "common.no_permission");
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
        lang.send(sender, "command.admin.usage");
    }

    private void handleBrowser(CommandSender sender) {

        if (!(sender instanceof Player player)) {
            lang.send(sender, "command.admin.players_only_studio");
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

        plugin.reloadConfig();
        lang.reload(plugin.getConfig().getString("language", "es"));

        lang.send(sender, "command.admin.reloaded", "species", speciesManager.count(), "rods", rodManager.count(),
                "baits", baitManager.count(), "treasures", treasureManager.count(), "junk", junkManager.count(),
                "regions", regionManager.count());
    }

    private void handleGiveRod(CommandSender sender, String[] args) {

        if (!(sender instanceof Player player) || args.length < 2) {
            lang.send(sender, "command.admin.giverod_usage");
            return;
        }

        var rodOpt = rodManager.get(args[1]);

        if (rodOpt.isEmpty()) {
            lang.send(sender, "command.admin.giverod_unknown", "id", args[1]);
            return;
        }

        player.getInventory().addItem(FishingItemFactory.createRod(rodOpt.get(), lang));
        lang.send(sender, "command.admin.given");
    }

    private void handleGiveBait(CommandSender sender, String[] args) {

        if (!(sender instanceof Player player) || args.length < 2) {
            lang.send(sender, "command.admin.givebait_usage");
            return;
        }

        var baitOpt = baitManager.get(args[1]);

        if (baitOpt.isEmpty()) {
            lang.send(sender, "command.admin.givebait_unknown", "id", args[1]);
            return;
        }

        player.getInventory().addItem(FishingItemFactory.createBait(baitOpt.get(), lang));
        lang.send(sender, "command.admin.given");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
        }

        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "giverod" -> TabCompleteUtil.filter(args[1], rodManager.getAll().stream()
                        .map(r -> r.id()).toList());
                case "givebait" -> TabCompleteUtil.filter(args[1], baitManager.getAll().stream()
                        .map(b -> b.id()).toList());
                default -> List.of();
            };
        }

        return List.of();
    }

}
