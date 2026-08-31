package com.sack.rpgroll.npcs.command;

import com.sack.rpgroll.common.command.Senders;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.npcs.RPGRollNPCs;
import com.sack.rpgroll.npcs.core.*;
import com.sack.rpgroll.npcs.gui.NpcAdminGUI;
import com.sack.rpgroll.npcs.gui.NpcMenuBrowserGUI;
import com.sack.rpgroll.npcs.integration.MineSkinClient;
import com.sack.rpgroll.npcs.listener.ChatPromptManager;
import com.sack.rpgroll.util.ComponentUtils;
import com.sack.rpgroll.util.TabCompleteUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class NpcAdminCommand implements CommandExecutor, TabCompleter {

        private static final List<String> SUBCOMMANDS = List.of("create", "edit", "list", "delete", "reload",
                        "menus");

        private final RPGRollNPCs plugin;
        private final NpcManager npcManager;
        private final NpcSpawnManager spawnManager;
        private final NpcSessionManager sessionManager;
        private final ChatPromptManager chatPromptManager;
        private final NpcWriter writer;
        private final MineSkinClient mineSkinClient;
        private final NpcMenuManager menuManager;
        private final LangManager langManager;

        public NpcAdminCommand(
                        RPGRollNPCs plugin,
                        NpcManager npcManager,
                        NpcSpawnManager spawnManager,
                        NpcSessionManager sessionManager,
                        ChatPromptManager chatPromptManager,
                        MineSkinClient mineSkinClient,
                        NpcWriter writer,
                        NpcMenuManager menuManager,
                        LangManager langManager) {

                this.plugin = plugin;
                this.npcManager = npcManager;
                this.spawnManager = spawnManager;
                this.sessionManager = sessionManager;
                this.chatPromptManager = chatPromptManager;
                this.mineSkinClient = mineSkinClient;
                this.writer = writer;
                this.menuManager = menuManager;
                this.langManager = langManager;
        }

        @Override
        public boolean onCommand(
                        CommandSender sender,
                        Command command,
                        String label,
                        String[] args) {

                if (!sender.hasPermission("rpgrollnpcs.admin.*")) {

                        langManager.send(sender, "general.no_permission");

                        return true;
                }

                if (args.length < 1) {

                        langManager.send(sender, "command.usage");

                        return true;
                }

                switch (args[0].toLowerCase()) {

                        case "menus" -> {

                                Player player = requirePlayer(sender);
                                if (player == null) {
                                        return true;
                                }

                                new NpcMenuBrowserGUI(player, menuManager, chatPromptManager, langManager).open();
                        }

                        case "create" -> {

                                Player player = requirePlayer(sender);
                                if (player == null) {
                                        return true;
                                }

                                if (args.length < 2) {

                                        langManager.send(player, "command.create.usage");

                                        return true;
                                }

                                String id = args[1];

                                if (npcManager.exists(id)) {

                                        langManager.send(player, "command.create.already_exists");

                                        return true;
                                }

                                NpcEditSession session = new NpcEditSession(id, true);

                                session.setLocation(
                                                player.getLocation());

                                sessionManager.start(
                                                player.getUniqueId(),
                                                session);

                                new NpcAdminGUI(
                                                player,
                                                session,
                                                sessionManager,
                                                chatPromptManager,
                                                npcManager,
                                                spawnManager,
                                                mineSkinClient,
                                                writer,
                                                langManager).open();

                        }

                        case "edit" -> {

                                Player player = requirePlayer(sender);
                                if (player == null) {
                                        return true;
                                }

                                if (args.length < 2) {
                                        langManager.send(player, "command.edit.usage");
                                        return true;
                                }

                                var npc = npcManager.get(args[1]);

                                if (npc.isEmpty()) {

                                        langManager.send(player, "command.not_found");

                                        return true;
                                }

                                NpcEditSession session = NpcEditSession.fromDefinition(
                                                npc.get());

                                sessionManager.start(
                                                player.getUniqueId(),
                                                session);

                                new NpcAdminGUI(
                                                player,
                                                session,
                                                sessionManager,
                                                chatPromptManager,
                                                npcManager,
                                                spawnManager,
                                                mineSkinClient,
                                                writer,
                                                langManager).open();

                        }

                        case "list" -> {

                                if (npcManager.count() == 0) {

                                        langManager.send(sender, "command.list.empty");

                                        return true;
                                }

                                langManager.send(sender, "command.list.header");

                                npcManager.getAll()
                                                .forEach(npc -> sender.sendMessage(
                                                                Component.text("• " + npc.id() + " (",
                                                                                NamedTextColor.WHITE)
                                                                        .append(ComponentUtils
                                                                                        .parse(npc.displayName()))
                                                                        .append(Component.text(")",
                                                                                NamedTextColor.WHITE))));

                        }

                        case "delete" -> {

                                if (args.length < 2) {
                                        langManager.send(sender, "command.delete.usage");

                                        return true;
                                }

                                String id = args[1];

                                if (!npcManager.exists(id)) {

                                        langManager.send(sender, "command.not_found");

                                        return true;
                                }

                                writer.delete(id);

                                npcManager.reload();

                                spawnManager.despawnAllForEveryone();

                                npcManager.getAll()
                                                .forEach(spawnManager::register);

                                org.bukkit.Bukkit.getOnlinePlayers()
                                                .forEach(playerOnline -> spawnManager.updateVisibility(
                                                                playerOnline,
                                                                npcManager.getAll()));

                                langManager.send(sender, "command.delete.success", "id", id);

                        }
                        case "reload" -> {

                                plugin.reloadConfig();
                                langManager.reload(plugin.getConfig().getString("language", "es"));

                                npcManager.reload();
                                menuManager.reload();

                                spawnManager.despawnAllForEveryone();

                                for (var npc : npcManager.getAll()) {
                                        spawnManager.register(npc);
                                }

                                for (var online : org.bukkit.Bukkit.getOnlinePlayers()) {
                                        spawnManager.updateVisibility(online, npcManager.getAll());
                                }

                                langManager.send(sender, "command.reload.success",
                                                "npcCount", npcManager.count(),
                                                "menuCount", menuManager.count());
                        }

                        default -> langManager.send(sender, "command.usage");

                }

                return true;
        }

        /** @return el sender como Player, o null (ya avisado) si no lo es — usado por los subcomandos que abren una GUI o necesitan una ubicación. */
        private Player requirePlayer(CommandSender sender) {

                if (Senders.asPlayer(sender) instanceof Player player) {
                        return player;
                }

                langManager.send(sender, "general.player_only");
                return null;
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

                if (args.length == 1) {
                        return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
                }

                if (args.length == 2 && List.of("edit", "delete").contains(args[0].toLowerCase())) {
                        return TabCompleteUtil.filter(args[1],
                                        npcManager.getAll().stream().map(npc -> npc.id()).toList());
                }

                return List.of();
        }
}
