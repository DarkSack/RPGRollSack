package com.sack.rpgroll.npcs.command;

import com.sack.rpgroll.npcs.core.*;
import com.sack.rpgroll.npcs.gui.NpcAdminGUI;
import com.sack.rpgroll.npcs.gui.NpcMenuBrowserGUI;
import com.sack.rpgroll.npcs.integration.MineSkinClient;
import com.sack.rpgroll.npcs.listener.ChatPromptManager;
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

        private final NpcManager npcManager;
        private final NpcSpawnManager spawnManager;
        private final NpcSessionManager sessionManager;
        private final ChatPromptManager chatPromptManager;
        private final NpcWriter writer;
        private final MineSkinClient mineSkinClient;
        private final NpcMenuManager menuManager;

        public NpcAdminCommand(
                        NpcManager npcManager,
                        NpcSpawnManager spawnManager,
                        NpcSessionManager sessionManager,
                        ChatPromptManager chatPromptManager,
                        MineSkinClient mineSkinClient,
                        NpcWriter writer,
                        NpcMenuManager menuManager) {

                this.npcManager = npcManager;
                this.spawnManager = spawnManager;
                this.sessionManager = sessionManager;
                this.chatPromptManager = chatPromptManager;
                this.mineSkinClient = mineSkinClient;
                this.writer = writer;
                this.menuManager = menuManager;
        }

        @Override
        public boolean onCommand(
                        CommandSender sender,
                        Command command,
                        String label,
                        String[] args) {

                if (!sender.hasPermission("rpgrollnpcs.admin.*")) {

                        sender.sendMessage(
                                        Component.text(
                                                        "No tienes permiso para usar este comando.",
                                                        NamedTextColor.RED));

                        return true;
                }

                if (args.length < 1) {

                        sender.sendMessage(
                                        Component.text(
                                                        "Uso: /npc <create|edit|list|delete|reload|menus> [id]",
                                                        NamedTextColor.RED));

                        return true;
                }

                switch (args[0].toLowerCase()) {

                        case "menus" -> {

                                Player player = requirePlayer(sender);
                                if (player == null) {
                                        return true;
                                }

                                new NpcMenuBrowserGUI(player, menuManager, chatPromptManager).open();
                        }

                        case "create" -> {

                                Player player = requirePlayer(sender);
                                if (player == null) {
                                        return true;
                                }

                                if (args.length < 2) {

                                        player.sendMessage(
                                                        Component.text(
                                                                        "Uso: /npc create <id>",
                                                                        NamedTextColor.RED));

                                        return true;
                                }

                                String id = args[1];

                                if (npcManager.exists(id)) {

                                        player.sendMessage(
                                                        Component.text(
                                                                        "Ya existe un NPC con ese id.",
                                                                        NamedTextColor.RED));

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
                                                writer).open();

                        }

                        case "edit" -> {

                                Player player = requirePlayer(sender);
                                if (player == null) {
                                        return true;
                                }

                                if (args.length < 2) {
                                        player.sendMessage(
                                                        Component.text(
                                                                        "Uso: /npc edit <id>",
                                                                        NamedTextColor.RED));
                                        return true;
                                }

                                var npc = npcManager.get(args[1]);

                                if (npc.isEmpty()) {

                                        player.sendMessage(
                                                        Component.text(
                                                                        "No existe ese NPC.",
                                                                        NamedTextColor.RED));

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
                                                writer).open();

                        }

                        case "list" -> {

                                if (npcManager.count() == 0) {

                                        sender.sendMessage(
                                                        Component.text(
                                                                        "No hay NPCs creados.",
                                                                        NamedTextColor.GRAY));

                                        return true;
                                }

                                sender.sendMessage(
                                                Component.text(
                                                                "NPCs existentes:",
                                                                NamedTextColor.GOLD));

                                npcManager.getAll()
                                                .forEach(npc -> sender.sendMessage(
                                                                Component.text(
                                                                                "• "
                                                                                                + npc.id()
                                                                                                + " ("
                                                                                                + npc.displayName()
                                                                                                + ")",
                                                                                NamedTextColor.WHITE)));

                        }

                        case "delete" -> {

                                if (args.length < 2) {
                                        sender.sendMessage(
                                                        Component.text(
                                                                        "Uso: /npc delete <id>",
                                                                        NamedTextColor.RED));

                                        return true;
                                }

                                String id = args[1];

                                if (!npcManager.exists(id)) {

                                        sender.sendMessage(
                                                        Component.text(
                                                                        "No existe ese NPC.",
                                                                        NamedTextColor.RED));

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

                                sender.sendMessage(
                                                Component.text(
                                                                "✔ NPC eliminado: " + id,
                                                                NamedTextColor.GREEN));

                        }
                        case "reload" -> {

                                npcManager.reload();
                                menuManager.reload();

                                spawnManager.despawnAllForEveryone();

                                for (var npc : npcManager.getAll()) {
                                        spawnManager.register(npc);
                                }

                                for (var online : org.bukkit.Bukkit.getOnlinePlayers()) {
                                        spawnManager.updateVisibility(online, npcManager.getAll());
                                }

                                sender.sendMessage(Component.text(
                                                "✔ Recargado: " + npcManager.count() + " NPC(s), " + menuManager.count()
                                                                + " menú(s).",
                                                NamedTextColor.GREEN));
                        }

                        default -> sender.sendMessage(
                                        Component.text(
                                                        "Acción inválida.",
                                                        NamedTextColor.RED));

                }

                return true;
        }

        /** @return el sender como Player, o null (ya avisado) si no lo es — usado por los subcomandos que abren una GUI o necesitan una ubicación. */
        private Player requirePlayer(CommandSender sender) {

                if (sender instanceof Player player) {
                        return player;
                }

                sender.sendMessage(Component.text("Este subcomando solo puede ser usado por jugadores.", NamedTextColor.RED));
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
