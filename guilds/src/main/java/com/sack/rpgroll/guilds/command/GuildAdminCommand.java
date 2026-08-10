package com.sack.rpgroll.guilds.command;

import com.sack.rpgroll.guilds.GuildServices;
import com.sack.rpgroll.guilds.gui.guild.GuildBrowserGUI;
import com.sack.rpgroll.guilds.gui.guild.GuildQuestAdminBrowserGUI;
import com.sack.rpgroll.guilds.guild.GuildManager;
import com.sack.rpgroll.guilds.guild.quest.GuildQuestManager;
import com.sack.rpgroll.util.TabCompleteUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/**
 * /guildadmin reload|browser [guilds|quests]|delete &lt;id&gt;. El browser
 * "quests" abre el editor visual de definiciones de quest de guild
 * (crear/editar), separado del navegador de guilds en sí.
 */
public class GuildAdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("reload", "browser", "delete");

    private final GuildManager guildManager;
    private final GuildQuestManager questManager;
    private final GuildServices services;

    public GuildAdminCommand(GuildManager guildManager, GuildQuestManager questManager, GuildServices services) {
        this.guildManager = guildManager;
        this.questManager = questManager;
        this.services = services;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload" -> {
                questManager.reload();
                sender.sendMessage(Component.text("✔ Quests de guild recargadas: " + questManager.count(),
                        NamedTextColor.GREEN));
            }
            case "browser" -> {

                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Solo un jugador puede abrir el navegador.", NamedTextColor.RED));
                    return true;
                }

                String target = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "guilds";

                if (target.equals("quests")) {
                    new GuildQuestAdminBrowserGUI(player, questManager, services.chatPromptManager()).open();
                } else {
                    new GuildBrowserGUI(player, services).open();
                }
            }
            case "delete" -> {
                if (args.length < 2) {
                    sender.sendMessage(Component.text("Uso: /guildadmin delete <id>", NamedTextColor.YELLOW));
                    return true;
                }
                if (!guildManager.exists(args[1])) {
                    sender.sendMessage(Component.text("No existe esa guild.", NamedTextColor.RED));
                    return true;
                }
                guildManager.disband(args[1]);
                sender.sendMessage(Component.text("✔ Guild eliminada.", NamedTextColor.GREEN));
            }
            default -> sendUsage(sender);
        }

        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Uso: /guildadmin <reload|browser [guilds|quests]|delete> [args]",
                NamedTextColor.YELLOW));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
        }

        if (args.length == 2) {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "browser" -> TabCompleteUtil.filter(args[1], List.of("guilds", "quests"));
                case "delete" -> TabCompleteUtil.filter(args[1],
                        guildManager.getAll().stream().map(guild -> guild.id()).toList());
                default -> List.of();
            };
        }

        return List.of();
    }

}
