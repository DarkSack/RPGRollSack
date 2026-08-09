package com.sack.rpgroll.dungeons.command;

import com.sack.rpgroll.dungeons.core.DungeonBounds;
import com.sack.rpgroll.dungeons.core.DungeonCheckpointPolicy;
import com.sack.rpgroll.dungeons.core.DungeonDefinition;
import com.sack.rpgroll.dungeons.core.DungeonDifficulty;
import com.sack.rpgroll.dungeons.core.DungeonManager;
import com.sack.rpgroll.dungeons.core.DungeonPoint;
import com.sack.rpgroll.dungeons.core.DungeonReviveConfig;
import com.sack.rpgroll.dungeons.engine.DungeonEngine;
import com.sack.rpgroll.dungeons.gui.ChatPromptManager;
import com.sack.rpgroll.dungeons.gui.DungeonBrowserGUI;
import com.sack.rpgroll.dungeons.gui.editor.DungeonEditorHubGUI;
import com.sack.rpgroll.dungeons.gui.editor.DungeonEditorSession;
import com.sack.rpgroll.util.TabCompleteUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Locale;

/** /dungeonadmin create|reload|forcestop|browser|editor */
public class DungeonAdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("create", "reload", "forcestop", "browser", "editor");

    private static final String PERMISSION = "rpgrolldungeons.admin.*";

    private final DungeonManager dungeonManager;
    private final DungeonEngine engine;
    private final ChatPromptManager chatPromptManager;
    private final Plugin plugin;

    public DungeonAdminCommand(DungeonManager dungeonManager, DungeonEngine engine,
            ChatPromptManager chatPromptManager, Plugin plugin) {
        this.dungeonManager = dungeonManager;
        this.engine = engine;
        this.chatPromptManager = chatPromptManager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(Component.text("No tenés permiso para usar este comando.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> handleCreate(sender, args);
            case "reload" -> handleReload(sender);
            case "forcestop" -> handleForceStop(sender, args);
            case "browser" -> handleBrowser(sender);
            case "editor" -> handleEditor(sender, args);
            default -> sendUsage(sender);
        }

        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Uso: /dungeonadmin <create|reload|forcestop|browser|editor> [args]",
                NamedTextColor.YELLOW));
    }

    private void handleCreate(CommandSender sender, String[] args) {

        if (args.length < 2) {
            sender.sendMessage(Component.text("Uso: /dungeonadmin create <id>", NamedTextColor.YELLOW));
            return;
        }

        String id = args[1].toLowerCase(Locale.ROOT);

        if (dungeonManager.exists(id)) {
            sender.sendMessage(Component.text("Ya existe una mazmorra con ese id.", NamedTextColor.RED));
            return;
        }

        DungeonDefinition definition = new DungeonDefinition(id, "misc", id, "STONE_BRICKS", "", 1, 15, 1, 5, 0,
                true, List.of(), new DungeonPoint("world", 0, 64, 0, 0, 0), DungeonBounds.none(), List.of(),
                List.of(DungeonDifficulty.defaultNormal()), DungeonCheckpointPolicy.defaultPolicy(),
                DungeonReviveConfig.none(), List.of(), java.util.Map.of());

        dungeonManager.register(definition);
        sender.sendMessage(Component.text("✔ Mazmorra '" + id + "' creada — abrila con /dungeonadmin editor " + id
                + " (o /dungeonadmin browser) para configurarla.", NamedTextColor.GREEN));

        if (sender instanceof Player player) {
            new DungeonEditorHubGUI(player,
                    new DungeonEditorSession(definition, dungeonManager, chatPromptManager, plugin)).open();
        }
    }

    private void handleReload(CommandSender sender) {
        dungeonManager.reload();
        sender.sendMessage(Component.text("✔ Recargado: " + dungeonManager.count() + " mazmorra(s).",
                NamedTextColor.GREEN));
    }

    private void handleForceStop(CommandSender sender, String[] args) {

        if (args.length < 2) {
            sender.sendMessage(Component.text("Uso: /dungeonadmin forcestop <id>", NamedTextColor.YELLOW));
            return;
        }

        DungeonDefinition definition = dungeonManager.get(args[1]).orElse(null);
        if (definition == null) {
            sender.sendMessage(Component.text("No existe la mazmorra '" + args[1] + "'.", NamedTextColor.RED));
            return;
        }

        var sessionOpt = engine.getSession(definition.id());
        if (sessionOpt.isEmpty()) {
            sender.sendMessage(Component.text("Esa mazmorra no tiene ninguna corrida activa.", NamedTextColor.RED));
            return;
        }

        engine.abandon(sessionOpt.get(), definition);
        sender.sendMessage(Component.text("✔ Corrida detenida.", NamedTextColor.GREEN));
    }

    private void handleBrowser(CommandSender sender) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo un jugador puede abrir el navegador.", NamedTextColor.RED));
            return;
        }

        new DungeonBrowserGUI(player, dungeonManager, chatPromptManager, plugin).open();
    }

    private void handleEditor(CommandSender sender, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo un jugador puede abrir el editor.", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(Component.text("Uso: /dungeonadmin editor <id>", NamedTextColor.YELLOW));
            return;
        }

        DungeonDefinition definition = dungeonManager.get(args[1]).orElse(null);
        if (definition == null) {
            sender.sendMessage(Component.text("No existe la mazmorra '" + args[1] + "'.", NamedTextColor.RED));
            return;
        }

        new DungeonEditorHubGUI(player, new DungeonEditorSession(definition, dungeonManager, chatPromptManager,
                plugin)).open();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
        }

        if (args.length == 2
                && List.of("forcestop", "editor").contains(args[0].toLowerCase(Locale.ROOT))) {
            return TabCompleteUtil.filter(args[1],
                    dungeonManager.getAll().stream().map(DungeonDefinition::id).toList());
        }

        return List.of();
    }

}
