package com.sack.rpgroll.magic.command;

import com.sack.rpgroll.magic.core.CatalystManager;
import com.sack.rpgroll.magic.core.GrimoireManager;
import com.sack.rpgroll.magic.core.RuneManager;
import com.sack.rpgroll.magic.core.SchoolManager;
import com.sack.rpgroll.magic.core.SpellManager;
import com.sack.rpgroll.magic.gui.ChatPromptManager;
import com.sack.rpgroll.magic.gui.MagicBrowserGUI;
import com.sack.rpgroll.magic.item.MagicItemFactory;
import com.sack.rpgroll.util.TabCompleteUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

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

    public MagicAdminCommand(SchoolManager schoolManager, SpellManager spellManager, GrimoireManager grimoireManager,
            RuneManager runeManager, CatalystManager catalystManager, ChatPromptManager chatPromptManager) {
        this.schoolManager = schoolManager;
        this.spellManager = spellManager;
        this.grimoireManager = grimoireManager;
        this.runeManager = runeManager;
        this.catalystManager = catalystManager;
        this.chatPromptManager = chatPromptManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("rpgrollmagic.admin.*")) {
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
            case "givecatalyst" -> handleGiveCatalyst(sender, args);
            case "givegrimoire" -> handleGiveGrimoire(sender, args);
            default -> sendUsage(sender);
        }

        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text(
                "Uso: /magicadmin <browser|reload|givecatalyst <id>|givegrimoire <id>>", NamedTextColor.RED));
    }

    private void handleBrowser(CommandSender sender) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo jugadores pueden abrir el Magic Studio.", NamedTextColor.RED));
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

        sender.sendMessage(Component.text("✔ Recargado: " + schoolManager.count() + " escuela(s), "
                + spellManager.count() + " hechizo(s), " + grimoireManager.count() + " grimorio(s), "
                + runeManager.count() + " runa(s), " + catalystManager.count() + " catalizador(es).",
                NamedTextColor.GREEN));
    }

    private void handleGiveCatalyst(CommandSender sender, String[] args) {

        if (!(sender instanceof Player player) || args.length < 2) {
            sender.sendMessage(Component.text("Uso: /magicadmin givecatalyst <id>", NamedTextColor.RED));
            return;
        }

        var catalystOpt = catalystManager.get(args[1]);

        if (catalystOpt.isEmpty()) {
            sender.sendMessage(Component.text("No existe un catalizador con id: " + args[1], NamedTextColor.RED));
            return;
        }

        player.getInventory().addItem(MagicItemFactory.createCatalyst(catalystOpt.get()));
        sender.sendMessage(Component.text("✔ Entregado.", NamedTextColor.GREEN));
    }

    private void handleGiveGrimoire(CommandSender sender, String[] args) {

        if (!(sender instanceof Player player) || args.length < 2) {
            sender.sendMessage(Component.text("Uso: /magicadmin givegrimoire <id>", NamedTextColor.RED));
            return;
        }

        var grimoireOpt = grimoireManager.get(args[1]);

        if (grimoireOpt.isEmpty()) {
            sender.sendMessage(Component.text("No existe un grimorio con id: " + args[1], NamedTextColor.RED));
            return;
        }

        player.getInventory().addItem(MagicItemFactory.createGrimoire(grimoireOpt.get()));
        sender.sendMessage(Component.text("✔ Entregado.", NamedTextColor.GREEN));
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
