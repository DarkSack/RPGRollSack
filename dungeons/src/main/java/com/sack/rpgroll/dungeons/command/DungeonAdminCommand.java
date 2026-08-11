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
    private static final List<String> STRUCTURE_SUBCOMMANDS = List.of("list", "info", "paste", "import", "browser");

    private static final String PERMISSION = "rpgrolldungeons.admin.*";

    private final DungeonManager dungeonManager;
    private final DungeonEngine engine;
    private final ChatPromptManager chatPromptManager;
    private final Plugin plugin;
    private final StructureLibrary structureLibrary;
    private final StructurePasteEngine structurePasteEngine;
    private final StructureImportService structureImportService;

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
            case "structure" -> handleStructure(sender, args);
            default -> sendUsage(sender);
        }

        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text(
                "Uso: /dungeonadmin <create|reload|forcestop|browser|editor|structure> [args]",
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
        structureLibrary.reload();
        sender.sendMessage(Component.text("✔ Recargado: " + dungeonManager.count() + " mazmorra(s), "
                + structureLibrary.count() + " estructura(s).", NamedTextColor.GREEN));
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
            case "browser" -> handleStructureBrowser(sender);
            default -> sendStructureUsage(sender);
        }
    }

    private void sendStructureUsage(CommandSender sender) {
        sender.sendMessage(Component.text(
                "Uso: /dungeonadmin structure <list|info|paste|import|browser> [args]", NamedTextColor.YELLOW));
    }

    private void handleStructureList(CommandSender sender) {

        sender.sendMessage(Component.text("Biblioteca de Estructuras (" + structureLibrary.count() + "):",
                NamedTextColor.GOLD));

        for (StructureDefinition def : structureLibrary.getAll()) {
            String source = def.sourceType() == StructureSourceType.NATIVE ? "NATIVE" : "CUSTOM";
            sender.sendMessage(Component.text("• " + def.id() + " (" + source + ") — " + def.displayName(),
                    NamedTextColor.WHITE));
        }
    }

    private void handleStructureInfo(CommandSender sender, String[] args) {

        if (args.length < 3) {
            sender.sendMessage(Component.text("Uso: /dungeonadmin structure info <id>", NamedTextColor.YELLOW));
            return;
        }

        StructureDefinition def = structureLibrary.get(args[2]).orElse(null);
        if (def == null) {
            sender.sendMessage(Component.text("No existe una estructura con id: " + args[2], NamedTextColor.RED));
            return;
        }

        boolean native_ = def.sourceType() == StructureSourceType.NATIVE;
        String size;

        if (native_) {
            var vector = structurePasteEngine.nativeSize(def.id());
            size = vector != null
                    ? vector.getBlockX() + "x" + vector.getBlockY() + "x" + vector.getBlockZ()
                    : "(no se pudo leer el .nbt)";
        } else {
            size = def.width() + "x" + def.height() + "x" + def.depth();
        }

        sender.sendMessage(Component.text(def.displayName() + " (" + def.id() + ")", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Fuente: " + def.sourceType(), NamedTextColor.WHITE));
        sender.sendMessage(Component.text("Tamaño (X·Y·Z): " + size, NamedTextColor.WHITE));
        sender.sendMessage(Component.text(def.description(), NamedTextColor.GRAY));
    }

    private void handleStructurePaste(CommandSender sender, String[] args) {

        if (args.length < 3) {
            sender.sendMessage(Component.text(
                    "Uso: /dungeonadmin structure paste <id> [mundo x y z] [rotacion]", NamedTextColor.YELLOW));
            return;
        }

        StructureDefinition def = structureLibrary.get(args[2]).orElse(null);
        if (def == null) {
            sender.sendMessage(Component.text("No existe una estructura con id: " + args[2], NamedTextColor.RED));
            return;
        }

        Location origin;
        int nextArg = 3;

        if (args.length >= 7) {

            World world = Bukkit.getWorld(args[3]);
            if (world == null) {
                sender.sendMessage(Component.text("No existe el mundo: " + args[3], NamedTextColor.RED));
                return;
            }

            try {
                double x = Double.parseDouble(args[4]);
                double y = Double.parseDouble(args[5]);
                double z = Double.parseDouble(args[6]);
                origin = new Location(world, x, y, z);
                nextArg = 7;
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Coordenadas inválidas.", NamedTextColor.RED));
                return;
            }

        } else if (sender instanceof Player player) {
            origin = player.getLocation();
        } else {
            sender.sendMessage(Component.text(
                    "Desde consola hace falta pasar mundo y coordenadas: structure paste <id> <mundo> <x> <y> <z>",
                    NamedTextColor.RED));
            return;
        }

        StructureRotation rotation = StructureRotation.NONE;

        if (args.length > nextArg) {
            try {
                rotation = StructureRotation.valueOf(args[nextArg].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                sender.sendMessage(Component.text(
                        "Rotación inválida — usá NONE, CLOCKWISE_90, CLOCKWISE_180 o COUNTERCLOCKWISE_90.",
                        NamedTextColor.RED));
                return;
            }
        }

        boolean placed = structurePasteEngine.place(def, origin, rotation, Mirror.NONE);
        sender.sendMessage(placed
                ? Component.text("✔ Estructura '" + def.id() + "' pegada.", NamedTextColor.GREEN)
                : Component.text("✘ No se pudo pegar la estructura — revisá la consola.", NamedTextColor.RED));
    }

    private void handleStructureImport(CommandSender sender, String[] args) {

        if (args.length < 4) {
            sender.sendMessage(Component.text(
                    "Uso: /dungeonadmin structure import <nombreVanilla> <nuevoId> [nombre visible...]",
                    NamedTextColor.YELLOW));
            sender.sendMessage(Component.text(
                    "nombreVanilla es el nombre que le pusiste al Structure Block al guardar (ej. rpgroll:sala1).",
                    NamedTextColor.GRAY));
            return;
        }

        String vanillaName = args[2];
        String newId = args[3].toLowerCase(Locale.ROOT);
        String displayName = args.length > 4 ? String.join(" ", List.of(args).subList(4, args.length)) : newId;
        Player requestedBy = sender instanceof Player player ? player : null;

        StructureImportService.Result result =
                structureImportService.importFromVanilla(vanillaName, newId, displayName, requestedBy);

        switch (result) {
            case OK -> sender.sendMessage(Component.text(
                    "✔ Estructura '" + newId + "' importada a la Biblioteca.", NamedTextColor.GREEN));
            case ID_TAKEN -> sender.sendMessage(Component.text(
                    "Ya existe una estructura con ese id.", NamedTextColor.RED));
            case VANILLA_NOT_FOUND -> sender.sendMessage(Component.text(
                    "No se encontró ninguna estructura vanilla guardada como '" + vanillaName + "'.",
                    NamedTextColor.RED));
            case IO_ERROR -> sender.sendMessage(Component.text(
                    "Error de I/O al importar — revisá la consola.", NamedTextColor.RED));
        }
    }

    private void handleStructureBrowser(CommandSender sender) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text(
                    "Solo un jugador puede abrir la Biblioteca de Estructuras.", NamedTextColor.RED));
            return;
        }

        new StructureLibraryGUI(player, structureLibrary, structurePasteEngine).open();
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

        if (args.length == 4 && structureSub.equals("paste")) {
            return TabCompleteUtil.worldNames(args[3]);
        }

        return List.of();
    }

}
