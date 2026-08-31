package com.sack.rpgroll.magic.command;

import com.sack.rpgroll.common.command.Senders;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.magic.core.CatalystManager;
import com.sack.rpgroll.magic.core.GrimoireManager;
import com.sack.rpgroll.magic.core.RuneManager;
import com.sack.rpgroll.magic.core.SchoolManager;
import com.sack.rpgroll.magic.core.SpellManager;
import com.sack.rpgroll.magic.gui.ChatPromptManager;
import com.sack.rpgroll.magic.gui.MagicBrowserGUI;
import com.sack.rpgroll.magic.item.MagicItemFactory;
import com.sack.rpgroll.util.TabCompleteUtil;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /magicadmin browser
 * /magicadmin reload
 * /magicadmin givecatalyst <id>
 * /magicadmin givegrimoire <id>
 */
public class MagicAdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("browser", "reload", "givecatalyst", "givegrimoire");

    private final SchoolManager schoolManager;
    private final SpellManager spellManager;
    private final GrimoireManager grimoireManager;
    private final RuneManager runeManager;
    private final CatalystManager catalystManager;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;

    public MagicAdminCommand(SchoolManager schoolManager, SpellManager spellManager, GrimoireManager grimoireManager,
            RuneManager runeManager, CatalystManager catalystManager, ChatPromptManager chatPromptManager,
            LangManager lang) {
        this.schoolManager = schoolManager;
        this.spellManager = spellManager;
        this.grimoireManager = grimoireManager;
        this.runeManager = runeManager;
        this.catalystManager = catalystManager;
        this.chatPromptManager = chatPromptManager;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("rpgrollmagic.admin.*")) {
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
            case "givecatalyst" -> handleGiveCatalyst(sender, args);
            case "givegrimoire" -> handleGiveGrimoire(sender, args);
            default -> sendUsage(sender);
        }

        return true;
    }

    private void sendUsage(CommandSender sender) {
        lang.send(sender, "command.admin.usage");
    }

    private void handleBrowser(CommandSender sender) {

        if (!(Senders.asPlayer(sender) instanceof Player player)) {
            lang.send(sender, "command.admin.players_only_studio");
            return;
        }

        new MagicBrowserGUI(player, schoolManager, spellManager, grimoireManager, runeManager, catalystManager,
                chatPromptManager).open();
    }

    private void handleReload(CommandSender sender) {

        schoolManager.reload();
        spellManager.reload();
        grimoireManager.reload();
        runeManager.reload();
        catalystManager.reload();

        lang.send(sender, "command.admin.reloaded",
                "schools", schoolManager.count(),
                "spells", spellManager.count(),
                "grimoires", grimoireManager.count(),
                "runes", runeManager.count(),
                "catalysts", catalystManager.count());
    }

    private void handleGiveCatalyst(CommandSender sender, String[] args) {

        if (!(Senders.asPlayer(sender) instanceof Player player)) {
            lang.send(sender, "command.admin.players_only");
            return;
        }

        if (args.length < 2) {
            lang.send(sender, "command.admin.usage_givecatalyst");
            return;
        }

        var catalystOpt = catalystManager.get(args[1]);

        if (catalystOpt.isEmpty()) {
            lang.send(sender, "command.admin.unknown_catalyst", "id", args[1]);
            return;
        }

        player.getInventory().addItem(MagicItemFactory.createCatalyst(catalystOpt.get(), lang));
        lang.send(sender, "command.admin.given");
    }

    private void handleGiveGrimoire(CommandSender sender, String[] args) {

        if (!(Senders.asPlayer(sender) instanceof Player player)) {
            lang.send(sender, "command.admin.players_only");
            return;
        }

        if (args.length < 2) {
            lang.send(sender, "command.admin.usage_givegrimoire");
            return;
        }

        var grimoireOpt = grimoireManager.get(args[1]);

        if (grimoireOpt.isEmpty()) {
            lang.send(sender, "command.admin.unknown_grimoire", "id", args[1]);
            return;
        }

        player.getInventory().addItem(MagicItemFactory.createGrimoire(grimoireOpt.get(), lang));
        lang.send(sender, "command.admin.given");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
        }

        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "givecatalyst" -> TabCompleteUtil.filter(args[1],
                        catalystManager.getAll().stream().map(c -> c.id()).toList());
                case "givegrimoire" -> TabCompleteUtil.filter(args[1],
                        grimoireManager.getAll().stream().map(g -> g.id()).toList());
                default -> List.of();
            };
        }

        return List.of();
    }

}
