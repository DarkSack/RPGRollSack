package com.sack.rpgroll.dungeons.command;

import com.sack.rpgroll.common.command.Senders;

import com.sack.rpgroll.common.lang.LangManager;
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
import com.sack.rpgroll.dungeons.gui.StructureLibraryGUI;
import com.sack.rpgroll.dungeons.gui.editor.DungeonEditorHubGUI;
import com.sack.rpgroll.dungeons.gui.editor.DungeonEditorSession;
import com.sack.rpgroll.dungeons.structure.StructureDefinition;
import com.sack.rpgroll.dungeons.structure.StructureImportService;
import com.sack.rpgroll.dungeons.structure.StructureLibrary;
import com.sack.rpgroll.dungeons.structure.StructurePasteEngine;
import com.sack.rpgroll.dungeons.structure.StructureSourceType;
import com.sack.rpgroll.util.TabCompleteUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Locale;

/** /dungeonadmin create|reload|forcestop|browser|editor|structure */
public class DungeonAdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS =
            List.of("create", "reload", "forcestop", "browser", "editor", "structure");
    private static final List<String> STRUCTURE_SUBCOMMANDS =
            List.of("list", "info", "paste", "import", "importschem", "browser");

    private static final String PERMISSION = "rpgrolldungeons.admin.*";

    private final DungeonManager dungeonManager;
    private final DungeonEngine engine;
    private final ChatPromptManager chatPromptManager;
    private final Plugin plugin;
    private final StructureLibrary structureLibrary;
    private final StructurePasteEngine structurePasteEngine;
    private final StructureImportService structureImportService;
    private final LangManager lang;

    public DungeonAdminCommand(DungeonManager dungeonManager, DungeonEngine engine,
            ChatPromptManager chatPromptManager, Plugin plugin, StructureLibrary structureLibrary,
            StructurePasteEngine structurePasteEngine, StructureImportService structureImportService) {
        this.dungeonManager = dungeonManager;
        this.engine = engine;
        this.chatPromptManager = chatPromptManager;
        this.plugin = plugin;
        this.structureLibrary = structureLibrary;
        this.structurePasteEngine = structurePasteEngine;
        this.structureImportService = structureImportService;
        this.lang = chatPromptManager.lang();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission(PERMISSION)) {
            lang.send(sender, "command.dungeonadmin.no_permission");
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
            case "structure" -> handleStructure(sender, args);
            default -> sendUsage(sender);
        }

        return true;
    }

    private void sendUsage(CommandSender sender) {
        lang.send(sender, "command.dungeonadmin.usage");
    }

    private void handleCreate(CommandSender sender, String[] args) {

        if (args.length < 2) {
            lang.send(sender, "command.dungeonadmin.create.usage");
            return;
        }

        String id = args[1].toLowerCase(Locale.ROOT);

        if (dungeonManager.exists(id)) {
            lang.send(sender, "command.dungeonadmin.create.id_taken");
            return;
        }

        DungeonDefinition definition = new DungeonDefinition(id, "misc", id, "STONE_BRICKS", "", 1, 15, 1, 5, 0,
                true, List.of(), new DungeonPoint("world", 0, 64, 0, 0, 0), DungeonBounds.none(), List.of(),
                List.of(DungeonDifficulty.defaultNormal()), DungeonCheckpointPolicy.defaultPolicy(),
                DungeonReviveConfig.none(), List.of(), java.util.Map.of());

        dungeonManager.register(definition);
        lang.send(sender, "command.dungeonadmin.create.ok", "id", id);

        if (Senders.asPlayer(sender) instanceof Player player) {
            new DungeonEditorHubGUI(player,
                    new DungeonEditorSession(definition, dungeonManager, chatPromptManager, plugin)).open();
        }
    }

    private void handleReload(CommandSender sender) {
        dungeonManager.reload();
        structureLibrary.reload();
        lang.reload(plugin.getConfig().getString("language", "es"));
        lang.send(sender, "command.dungeonadmin.reload.ok", "dungeons", dungeonManager.count(),
                "structures", structureLibrary.count());
    }

    private void handleForceStop(CommandSender sender, String[] args) {

        if (args.length < 2) {
            lang.send(sender, "command.dungeonadmin.forcestop.usage");
            return;
        }

        DungeonDefinition definition = dungeonManager.get(args[1]).orElse(null);
        if (definition == null) {
            lang.send(sender, "command.dungeonadmin.not_found", "id", args[1]);
            return;
        }

        var sessionOpt = engine.getSession(definition.id());
        if (sessionOpt.isEmpty()) {
            lang.send(sender, "command.dungeonadmin.forcestop.no_run");
            return;
        }

        engine.abandon(sessionOpt.get(), definition);
        lang.send(sender, "command.dungeonadmin.forcestop.ok");
    }

    private void handleBrowser(CommandSender sender) {

        if (!(Senders.asPlayer(sender) instanceof Player player)) {
            lang.send(sender, "command.dungeonadmin.players_only");
            return;
        }

        new DungeonBrowserGUI(player, dungeonManager, chatPromptManager, plugin).open();
    }

    private void handleEditor(CommandSender sender, String[] args) {

        if (!(Senders.asPlayer(sender) instanceof Player player)) {
            lang.send(sender, "command.dungeonadmin.players_only");
            return;
        }

        if (args.length < 2) {
            lang.send(sender, "command.dungeonadmin.editor.usage");
            return;
        }

        DungeonDefinition definition = dungeonManager.get(args[1]).orElse(null);
        if (definition == null) {
            lang.send(sender, "command.dungeonadmin.not_found", "id", args[1]);
            return;
        }

        new DungeonEditorHubGUI(player, new DungeonEditorSession(definition, dungeonManager, chatPromptManager,
                plugin)).open();
    }

    // ============ estructuras (biblioteca) ============

    private void handleStructure(CommandSender sender, String[] args) {

        if (args.length < 2) {
            sendStructureUsage(sender);
            return;
        }

        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "list" -> handleStructureList(sender);
            case "info" -> handleStructureInfo(sender, args);
            case "paste" -> handleStructurePaste(sender, args);
            case "import" -> handleStructureImport(sender, args);
            case "importschem" -> handleStructureImportSchem(sender, args);
            case "browser" -> handleStructureBrowser(sender);
            default -> sendStructureUsage(sender);
        }
    }

    private void sendStructureUsage(CommandSender sender) {
        lang.send(sender, "command.dungeonadmin.structure.usage");
    }

    private void handleStructureList(CommandSender sender) {

        lang.send(sender, "command.dungeonadmin.structure.list.header", "count", structureLibrary.count());

        for (StructureDefinition def : structureLibrary.getAll()) {
            lang.send(sender, "command.dungeonadmin.structure.list.entry", "id", def.id(),
                    "source", def.sourceType().name(), "name", def.displayName());
        }
    }

    private void handleStructureInfo(CommandSender sender, String[] args) {

        if (args.length < 3) {
            lang.send(sender, "command.dungeonadmin.structure.info.usage");
            return;
        }

        StructureDefinition def = structureLibrary.get(args[2]).orElse(null);
        if (def == null) {
            lang.send(sender, "command.dungeonadmin.structure.not_found", "id", args[2]);
            return;
        }

        String size = switch (def.sourceType()) {
            case NATIVE -> {
                var vector = structurePasteEngine.nativeSize(def.id());
                yield vector != null
                        ? vector.getBlockX() + "x" + vector.getBlockY() + "x" + vector.getBlockZ()
                        : lang.raw("command.dungeonadmin.structure.info.unreadable_nbt");
            }
            case SCHEMATIC -> {
                var vector = structurePasteEngine.schematicSize(def.id());
                yield vector != null
                        ? vector.getBlockX() + "x" + vector.getBlockY() + "x" + vector.getBlockZ()
                        : lang.raw("command.dungeonadmin.structure.info.unreadable_nbt");
            }
            case CUSTOM -> def.width() + "x" + def.height() + "x" + def.depth();
        };

        sender.sendMessage(Component.text(def.displayName() + " (" + def.id() + ")", NamedTextColor.GOLD));
        lang.send(sender, "command.dungeonadmin.structure.info.source", "source", def.sourceType());
        lang.send(sender, "command.dungeonadmin.structure.info.size", "size", size);
        sender.sendMessage(Component.text(def.description(), NamedTextColor.GRAY));
    }

    private void handleStructurePaste(CommandSender sender, String[] args) {

        if (args.length < 3) {
            lang.send(sender, "command.dungeonadmin.structure.paste.usage");
            return;
        }

        StructureDefinition def = structureLibrary.get(args[2]).orElse(null);
        if (def == null) {
            lang.send(sender, "command.dungeonadmin.structure.not_found", "id", args[2]);
            return;
        }

        Location origin;
        int nextArg = 3;

        if (args.length >= 7) {

            World world = Bukkit.getWorld(args[3]);
            if (world == null) {
                lang.send(sender, "command.dungeonadmin.structure.paste.world_not_found", "world", args[3]);
                return;
            }

            try {
                double x = Double.parseDouble(args[4]);
                double y = Double.parseDouble(args[5]);
                double z = Double.parseDouble(args[6]);
                origin = new Location(world, x, y, z);
                nextArg = 7;
            } catch (NumberFormatException e) {
                lang.send(sender, "command.dungeonadmin.structure.paste.invalid_coords");
                return;
            }

        } else if (Senders.asPlayer(sender) instanceof Player player) {
            origin = player.getLocation();
        } else {
            lang.send(sender, "command.dungeonadmin.structure.paste.console_needs_coords");
            return;
        }

        StructureRotation rotation = StructureRotation.NONE;

        if (args.length > nextArg) {
            try {
                rotation = StructureRotation.valueOf(args[nextArg].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                lang.send(sender, "command.dungeonadmin.structure.paste.invalid_rotation");
                return;
            }
        }

        boolean placed = structurePasteEngine.place(def, origin, rotation, Mirror.NONE);
        lang.send(sender, placed ? "command.dungeonadmin.structure.paste.ok"
                : "command.dungeonadmin.structure.paste.failed", "id", def.id());
    }

    private void handleStructureImport(CommandSender sender, String[] args) {

        if (args.length < 4) {
            lang.send(sender, "command.dungeonadmin.structure.import.usage");
            lang.send(sender, "command.dungeonadmin.structure.import.usage_hint");
            return;
        }

        String vanillaName = args[2];
        String newId = args[3].toLowerCase(Locale.ROOT);
        String displayName = args.length > 4 ? String.join(" ", List.of(args).subList(4, args.length)) : newId;
        Player requestedBy = Senders.asPlayer(sender) instanceof Player player ? player : null;

        StructureImportService.Result result =
                structureImportService.importFromVanilla(vanillaName, newId, displayName, requestedBy);

        switch (result) {
            case OK -> lang.send(sender, "command.dungeonadmin.structure.import.ok", "id", newId);
            case ID_TAKEN -> lang.send(sender, "command.dungeonadmin.structure.import.id_taken");
            case VANILLA_NOT_FOUND -> lang.send(sender, "command.dungeonadmin.structure.import.vanilla_not_found",
                    "name", vanillaName);
            case IO_ERROR -> lang.send(sender, "command.dungeonadmin.structure.import.io_error");
        }
    }

    private void handleStructureImportSchem(CommandSender sender, String[] args) {

        if (args.length < 4) {
            lang.send(sender, "command.dungeonadmin.structure.importschem.usage");
            lang.send(sender, "command.dungeonadmin.structure.importschem.usage_hint");
            return;
        }

        String fileName = args[2];
        String newId = args[3].toLowerCase(Locale.ROOT);
        String displayName = args.length > 4 ? String.join(" ", List.of(args).subList(4, args.length)) : newId;
        Player requestedBy = Senders.asPlayer(sender) instanceof Player player ? player : null;

        StructureImportService.Result result =
                structureImportService.importFromSchematic(fileName, newId, displayName, requestedBy);

        switch (result) {
            case OK -> lang.send(sender, "command.dungeonadmin.structure.importschem.ok", "id", newId);
            case ID_TAKEN -> lang.send(sender, "command.dungeonadmin.structure.importschem.id_taken");
            case WORLDEDIT_MISSING -> lang.send(sender, "command.dungeonadmin.structure.importschem.worldedit_missing");
            case FILE_NOT_FOUND -> lang.send(sender, "command.dungeonadmin.structure.importschem.file_not_found",
                    "file", fileName);
            case IO_ERROR -> lang.send(sender, "command.dungeonadmin.structure.importschem.io_error");
            case VANILLA_NOT_FOUND -> throw new IllegalStateException("importFromSchematic never returns VANILLA_NOT_FOUND");
        }
    }

    private void handleStructureBrowser(CommandSender sender) {

        if (!(Senders.asPlayer(sender) instanceof Player player)) {
            lang.send(sender, "command.dungeonadmin.structure.browser.players_only");
            return;
        }

        new StructureLibraryGUI(player, structureLibrary, structurePasteEngine, lang).open();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (args.length == 2 && List.of("forcestop", "editor").contains(sub)) {
            return TabCompleteUtil.filter(args[1],
                    dungeonManager.getAll().stream().map(DungeonDefinition::id).toList());
        }

        if (sub.equals("structure")) {
            return structureTabComplete(args);
        }

        return List.of();
    }

    private List<String> structureTabComplete(String[] args) {

        if (args.length == 2) {
            return TabCompleteUtil.filter(args[1], STRUCTURE_SUBCOMMANDS);
        }

        String structureSub = args[1].toLowerCase(Locale.ROOT);

        if (args.length == 3 && List.of("info", "paste").contains(structureSub)) {
            return TabCompleteUtil.filter(args[2],
                    structureLibrary.getAll().stream().map(StructureDefinition::id).toList());
        }

        if (args.length == 3 && structureSub.equals("importschem")) {
            return TabCompleteUtil.filter(args[2], availableSchematicNames());
        }

        if (args.length == 4 && structureSub.equals("paste")) {
            return TabCompleteUtil.worldNames(args[3]);
        }

        return List.of();
    }

    /** Nombres (sin extensión) de los .schem en la carpeta schematics/ de WorldEdit, si está instalado. */
    private List<String> availableSchematicNames() {

        Plugin worldEdit = Bukkit.getPluginManager().getPlugin("WorldEdit");

        if (worldEdit == null) {
            return List.of();
        }

        java.io.File folder = new java.io.File(worldEdit.getDataFolder(), "schematics");
        String[] files = folder.list((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".schem"));

        if (files == null) {
            return List.of();
        }

        return java.util.Arrays.stream(files).map(name -> name.substring(0, name.length() - ".schem".length())).toList();
    }

}
