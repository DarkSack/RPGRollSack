package com.sack.rpgroll.traps.command;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.traps.TrapsPlugin;
import com.sack.rpgroll.traps.core.TrapDefinition;
import com.sack.rpgroll.traps.core.TrapManager;
import com.sack.rpgroll.traps.core.TrapTrigger;
import com.sack.rpgroll.traps.engine.TrapEngine;
import com.sack.rpgroll.traps.gui.ChatPromptManager;
import com.sack.rpgroll.traps.gui.TrapBrowserGUI;
import com.sack.rpgroll.traps.gui.TrapEditorHubGUI;
import com.sack.rpgroll.traps.gui.TrapZoneEditorGUI;
import com.sack.rpgroll.traps.gui.turret.TurretBrowserGUI;
import com.sack.rpgroll.traps.gui.turret.TurretEditorGUI;
import com.sack.rpgroll.traps.location.PlacedTrap;
import com.sack.rpgroll.traps.location.PlacedTrapManager;
import com.sack.rpgroll.traps.turret.PlacedTurret;
import com.sack.rpgroll.traps.turret.PlacedTurretManager;
import com.sack.rpgroll.traps.turret.TurretDefinition;
import com.sack.rpgroll.traps.turret.TurretEngine;
import com.sack.rpgroll.traps.turret.TurretManager;
import com.sack.rpgroll.util.TabCompleteUtil;

import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * /trapadmin create|edit|browser|reload — definiciones (TrapDefinition)
 * /trapadmin place|remove|list|info|forcetrigger — instancias colocadas
 * Único comando de v1 (sin /trap de jugador — ver plan).
 */
public class TrapAdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("create", "edit", "browser", "reload", "place",
            "remove", "list", "info", "forcetrigger", "turret");
    private static final List<String> TURRET_SUBCOMMANDS = List.of("create", "edit", "browser", "place", "remove",
            "list");

    private static final String PERMISSION = "rpgrolltraps.admin.*";
    private static final int LOOK_RANGE = 8;

    private final TrapsPlugin plugin;
    private final TrapManager trapManager;
    private final PlacedTrapManager placedTrapManager;
    private final TrapEngine engine;
    private final TurretManager turretManager;
    private final PlacedTurretManager placedTurretManager;
    private final TurretEngine turretEngine;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;

    public TrapAdminCommand(TrapsPlugin plugin, TrapManager trapManager, PlacedTrapManager placedTrapManager,
            TrapEngine engine, TurretManager turretManager, PlacedTurretManager placedTurretManager,
            TurretEngine turretEngine, ChatPromptManager chatPromptManager, LangManager lang) {
        this.plugin = plugin;
        this.trapManager = trapManager;
        this.placedTrapManager = placedTrapManager;
        this.engine = engine;
        this.turretManager = turretManager;
        this.placedTurretManager = placedTurretManager;
        this.turretEngine = turretEngine;
        this.chatPromptManager = chatPromptManager;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission(PERMISSION)) {
            lang.send(sender, "general.no_permission");
            return true;
        }

        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> handleCreate(sender, args);
            case "edit" -> handleEdit(sender, args);
            case "browser" -> handleBrowser(sender);
            case "reload" -> handleReload(sender);
            case "place" -> handlePlace(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "list" -> handleList(sender);
            case "info" -> handleInfo(sender, args);
            case "forcetrigger" -> handleForceTrigger(sender, args);
            case "turret" -> handleTurret(sender, args);
            default -> sendUsage(sender);
        }

        return true;
    }

    private void sendUsage(CommandSender sender) {
        lang.send(sender, "admin.usage");
    }

    private Player requirePlayer(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        lang.send(sender, "general.player_only");
        return null;
    }

    private void handleCreate(CommandSender sender, String[] args) {

        if (args.length < 2) {
            lang.send(sender, "admin.create.usage");
            return;
        }

        String id = args[1].toLowerCase(Locale.ROOT);

        if (trapManager.exists(id)) {
            lang.send(sender, "admin.create.id_taken");
            return;
        }

        TrapDefinition trap = new TrapDefinition(id, id, "", null, TrapTrigger.PRESSURE, java.util.Map.of(), 1.5,
                List.of(), List.of(), 0, -1, List.of(), null, null);

        trapManager.save(trap);
        lang.send(sender, "admin.create.ok", "id", id);
    }

    private void handleEdit(CommandSender sender, String[] args) {

        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }

        if (args.length < 2) {
            lang.send(sender, "admin.edit.usage");
            return;
        }

        Optional<TrapDefinition> trapOpt = trapManager.get(args[1]);
        if (trapOpt.isEmpty()) {
            lang.send(sender, "admin.trap_not_found", "id", args[1]);
            return;
        }

        new TrapEditorHubGUI(player, trapOpt.get(), trapManager, chatPromptManager, player::closeInventory).open();
    }

    private void handleBrowser(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player != null) {
            new TrapBrowserGUI(player, trapManager, chatPromptManager).open();
        }
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        lang.reload(plugin.getConfig().getString("language", "es"));
        trapManager.reload();
        turretManager.reload();
        lang.send(sender, "admin.reload.ok", "traps", trapManager.count(),
                "locations", placedTrapManager.getAll().size());
    }

    private void handlePlace(CommandSender sender, String[] args) {

        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }

        if (args.length < 2) {
            lang.send(sender, "admin.place.usage");
            return;
        }

        String trapId = args[1];
        Optional<TrapDefinition> trapOpt = trapManager.get(trapId);

        if (trapOpt.isEmpty()) {
            lang.send(sender, "admin.trap_not_found", "id", trapId);
            return;
        }

        if (args.length >= 3 && args[2].equalsIgnoreCase("zone")) {
            new TrapZoneEditorGUI(player, trapId, placedTrapManager, lang, player::closeInventory).open();
            return;
        }

        Block target = player.getTargetBlockExact(LOOK_RANGE);

        if (target == null || target.isEmpty()) {
            lang.send(player, "admin.must_look_at_block");
            return;
        }

        PlacedTrap placed = placedTrapManager.addPoint(trapId, target.getLocation(), trapOpt.get().charges());
        lang.send(player, "admin.place.ok", "trap", trapId, "placement", placed.placementId());
    }

    private void handleRemove(CommandSender sender, String[] args) {

        if (args.length < 2) {
            lang.send(sender, "admin.remove.usage");
            return;
        }

        boolean removed = placedTrapManager.remove(args[1]);
        lang.send(sender, removed ? "admin.remove.ok" : "admin.remove.not_found", "placement", args[1]);
    }

    private void handleList(CommandSender sender) {

        if (placedTrapManager.getAll().isEmpty()) {
            lang.send(sender, "admin.list.empty");
            return;
        }

        lang.send(sender, "admin.list.header", "count", placedTrapManager.getAll().size());

        for (PlacedTrap placed : placedTrapManager.getAll()) {
            lang.send(sender, "admin.list.entry", "placement", placed.placementId(), "trap", placed.trapId(),
                    "state", placed.state().name());
        }
    }

    private void handleInfo(CommandSender sender, String[] args) {

        if (args.length < 2) {
            lang.send(sender, "admin.info.usage");
            return;
        }

        Optional<PlacedTrap> placedOpt = placedTrapManager.get(args[1]);
        if (placedOpt.isEmpty()) {
            lang.send(sender, "admin.remove.not_found", "placement", args[1]);
            return;
        }

        PlacedTrap placed = placedOpt.get();

        lang.send(sender, "admin.info.header", "placement", placed.placementId(), "trap", placed.trapId());
        lang.send(sender, "admin.info.state", "state", placed.state().name());
        lang.send(sender, "admin.info.charges", "value", placed.chargesRemaining() < 0 ? "∞"
                : String.valueOf(placed.chargesRemaining()));
        lang.send(sender, "admin.info.position", "world", placed.world(), "x", placed.x(), "y", placed.y(), "z",
                placed.z());
    }

    private void handleForceTrigger(CommandSender sender, String[] args) {

        if (args.length < 2) {
            lang.send(sender, "admin.forcetrigger.usage");
            return;
        }

        Player asPlayer = sender instanceof Player p ? p : null;
        boolean triggered = engine.forceTrigger(args[1], asPlayer);

        lang.send(sender, triggered ? "admin.forcetrigger.ok" : "admin.forcetrigger.failed", "placement", args[1]);
    }

    private void handleTurret(CommandSender sender, String[] args) {

        if (args.length < 2) {
            lang.send(sender, "admin.turret.usage");
            return;
        }

        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "create" -> handleTurretCreate(sender, args);
            case "edit" -> handleTurretEdit(sender, args);
            case "browser" -> handleTurretBrowser(sender);
            case "place" -> handleTurretPlace(sender, args);
            case "remove" -> handleTurretRemove(sender, args);
            case "list" -> handleTurretList(sender);
            default -> lang.send(sender, "admin.turret.usage");
        }
    }

    private void handleTurretCreate(CommandSender sender, String[] args) {

        if (args.length < 3) {
            lang.send(sender, "admin.turret.create.usage");
            return;
        }

        String id = args[2].toLowerCase(Locale.ROOT);

        if (turretManager.exists(id)) {
            lang.send(sender, "admin.turret.create.id_taken");
            return;
        }

        TurretDefinition turret = new TurretDefinition(id, id, "", 12.0, true, true, 20, List.of(), null, null, null);

        turretManager.save(turret);
        lang.send(sender, "admin.turret.create.ok", "id", id);
    }

    private void handleTurretEdit(CommandSender sender, String[] args) {

        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }

        if (args.length < 3) {
            lang.send(sender, "admin.turret.edit.usage");
            return;
        }

        Optional<TurretDefinition> turretOpt = turretManager.get(args[2]);
        if (turretOpt.isEmpty()) {
            lang.send(sender, "admin.turret.not_found", "id", args[2]);
            return;
        }

        new TurretEditorGUI(player, turretOpt.get(), turretManager, chatPromptManager, player::closeInventory).open();
    }

    private void handleTurretBrowser(CommandSender sender) {
        Player player = requirePlayer(sender);
        if (player != null) {
            new TurretBrowserGUI(player, turretManager, chatPromptManager).open();
        }
    }

    private void handleTurretPlace(CommandSender sender, String[] args) {

        Player player = requirePlayer(sender);
        if (player == null) {
            return;
        }

        if (args.length < 3) {
            lang.send(sender, "admin.turret.place.usage");
            return;
        }

        String turretId = args[2];

        if (turretManager.get(turretId).isEmpty()) {
            lang.send(sender, "admin.turret.not_found", "id", turretId);
            return;
        }

        Block target = player.getTargetBlockExact(LOOK_RANGE);

        if (target == null || target.isEmpty()) {
            lang.send(player, "admin.must_look_at_block");
            return;
        }

        PlacedTurret placed = placedTurretManager.add(turretId, target.getLocation().add(0, 1, 0));
        lang.send(player, "admin.turret.place.ok", "turret", turretId, "placement", placed.placementId());
    }

    private void handleTurretRemove(CommandSender sender, String[] args) {

        if (args.length < 3) {
            lang.send(sender, "admin.turret.remove.usage");
            return;
        }

        boolean removed = placedTurretManager.remove(args[2]);

        if (removed) {
            turretEngine.despawnVisual(args[2]);
        }

        lang.send(sender, removed ? "admin.turret.remove.ok" : "admin.turret.remove.not_found", "placement",
                args[2]);
    }

    private void handleTurretList(CommandSender sender) {

        if (placedTurretManager.getAll().isEmpty()) {
            lang.send(sender, "admin.turret.list.empty");
            return;
        }

        lang.send(sender, "admin.turret.list.header", "count", placedTurretManager.getAll().size());

        for (PlacedTurret placed : placedTurretManager.getAll()) {
            lang.send(sender, "admin.turret.list.entry", "placement", placed.placementId(), "turret",
                    placed.turretId());
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (args.length == 2) {
            return switch (sub) {
                case "edit", "place" -> TabCompleteUtil.filter(args[1], trapIds());
                case "remove", "info", "forcetrigger" -> TabCompleteUtil.filter(args[1], placementIds());
                default -> List.of();
            };
        }

        if (args.length == 3 && sub.equals("place")) {
            return TabCompleteUtil.filter(args[2], List.of("zone"));
        }

        if (sub.equals("turret")) {
            return turretTabComplete(args);
        }

        return List.of();
    }

    private List<String> turretTabComplete(String[] args) {

        if (args.length == 2) {
            return TabCompleteUtil.filter(args[1], TURRET_SUBCOMMANDS);
        }

        String turretSub = args[1].toLowerCase(Locale.ROOT);

        if (args.length == 3) {
            return switch (turretSub) {
                case "edit", "place" -> TabCompleteUtil.filter(args[2], turretIds());
                case "remove" -> TabCompleteUtil.filter(args[2], turretPlacementIds());
                default -> List.of();
            };
        }

        return List.of();
    }

    private List<String> trapIds() {
        return trapManager.getAll().stream().map(TrapDefinition::id).toList();
    }

    private List<String> placementIds() {
        return placedTrapManager.getAll().stream().map(PlacedTrap::placementId).toList();
    }

    private List<String> turretIds() {
        return turretManager.getAll().stream().map(TurretDefinition::id).toList();
    }

    private List<String> turretPlacementIds() {
        return placedTurretManager.getAll().stream().map(PlacedTurret::placementId).toList();
    }

}
