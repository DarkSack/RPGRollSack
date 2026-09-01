package com.sack.rpgroll.traps.command;

import org.bukkit.Bukkit;

import com.sack.rpgroll.traps.turret.TurretItem;

import com.sack.rpgroll.common.command.Senders;

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
            "remove", "list", "info", "forcetrigger", "turret", "ammo");
    private static final List<String> TURRET_SUBCOMMANDS = List.of("create", "edit", "browser", "place", "remove",
            "list", "give", "catalog");

    private static final String PERMISSION = "rpgrolltraps.admin.*";
    private static final int LOOK_RANGE = 8;

    private final TrapsPlugin plugin;
    private final TrapManager trapManager;
    private final PlacedTrapManager placedTrapManager;
    private final TrapEngine engine;
    private final TurretManager turretManager;
    private final com.sack.rpgroll.traps.ammo.AmmoManager ammoManager;
    private final PlacedTurretManager placedTurretManager;
    private final TurretEngine turretEngine;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;

    public TrapAdminCommand(TrapsPlugin plugin, TrapManager trapManager, PlacedTrapManager placedTrapManager,
            TrapEngine engine, TurretManager turretManager, PlacedTurretManager placedTurretManager,
            TurretEngine turretEngine, ChatPromptManager chatPromptManager, LangManager lang, com.sack.rpgroll.traps.ammo.AmmoManager ammoManager) {
        this.plugin = plugin;
        this.trapManager = trapManager;
        this.placedTrapManager = placedTrapManager;
        this.engine = engine;
        this.turretManager = turretManager;
        this.ammoManager = ammoManager;
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
            case "ammo" -> handleAmmo(sender, args);
            default -> sendUsage(sender);
        }

        return true;
    }

    private void sendUsage(CommandSender sender) {
        lang.send(sender, "admin.usage");
    }

    private Player requirePlayer(CommandSender sender) {
        if (Senders.asPlayer(sender) instanceof Player player) {
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
        // Sin esto, editar un YAML de munición solo surtía efecto al reiniciar.
        ammoManager.reload();
        lang.send(sender, "admin.reload.ok", "traps", trapManager.count(),
                "locations", placedTrapManager.getAll().size(),
                "turrets", turretManager.count(), "ammo", ammoManager.count());
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

        Player asPlayer = Senders.asPlayer(sender) instanceof Player p ? p : null;
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
            case "give" -> handleTurretGive(sender, args);
            case "catalog" -> handleTurretCatalog(sender);
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

        TurretDefinition turret = new TurretDefinition(id, id, "", 12.0, true, true, 20, List.of(), null, null, null,
                null, 0, -1);

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

    /**
     * /trapadmin turret give &lt;id&gt; [jugador] [cantidad]
     * <p>
     * Sin jugador se lo da a quien ejecuta, así que desde consola hay que
     * nombrarlo — por eso los dos chequeos están separados y no fundidos en
     * un solo mensaje de uso.
     */
    /** /trapadmin ammo give &lt;id&gt; [jugador] [cantidad] */
    private void handleAmmo(CommandSender sender, String[] args) {

        if (args.length < 3 || !args[1].equalsIgnoreCase("give")) {
            lang.send(sender, "admin.ammo.give_usage");
            return;
        }

        var ammoOpt = ammoManager.get(args[2]);

        if (ammoOpt.isEmpty()) {
            lang.send(sender, "admin.ammo.not_found", "id", args[2]);
            return;
        }

        Player target;

        if (args.length >= 4) {
            target = Bukkit.getPlayerExact(args[3]);

            if (target == null) {
                lang.send(sender, "admin.turret.player_not_found", "player", args[3]);
                return;
            }
        } else {
            target = Senders.asPlayer(sender);

            if (target == null) {
                lang.send(sender, "admin.ammo.give_needs_player");
                return;
            }
        }

        int amount = 1;

        if (args.length >= 5) {
            try {
                amount = Math.max(1, Integer.parseInt(args[4]));
            } catch (NumberFormatException e) {
                lang.send(sender, "admin.turret.invalid_amount", "value", args[4]);
                return;
            }
        }

        target.getInventory().addItem(
                com.sack.rpgroll.traps.ammo.AmmoItem.create(plugin, ammoOpt.get(), lang, amount));

        lang.send(sender, "admin.ammo.given", "amount", amount, "id", args[2], "player", target.getName());
    }

    /** Abre el catálogo para conseguir torretas y munición con un clic. */
    private void handleTurretCatalog(CommandSender sender) {

        Player player = Senders.asPlayer(sender);

        if (player == null) {
            lang.send(sender, "admin.turret.catalog_needs_player");
            return;
        }

        new com.sack.rpgroll.traps.gui.turret.TurretCatalogGUI(
                player, plugin, turretManager, ammoManager, lang).open();
    }

    private void handleTurretGive(CommandSender sender, String[] args) {

        if (args.length < 3) {
            // Sin id: en vez de repetir la sintaxis, se abre el catálogo, que
            // es justo lo que hace falta cuando no te sabes los ids. Desde
            // consola no hay GUI posible, así que ahí sí va el uso.
            Player player = Senders.asPlayer(sender);

            if (player != null) {
                handleTurretCatalog(sender);
            } else {
                lang.send(sender, "admin.turret.give_usage");
            }

            return;
        }

        var definitionOpt = turretManager.get(args[2]);

        if (definitionOpt.isEmpty()) {
            lang.send(sender, "admin.turret.not_found", "id", args[2]);
            return;
        }

        Player target;

        if (args.length >= 4) {
            target = Bukkit.getPlayerExact(args[3]);

            if (target == null) {
                lang.send(sender, "admin.turret.player_not_found", "player", args[3]);
                return;
            }
        } else {
            target = Senders.asPlayer(sender);

            if (target == null) {
                lang.send(sender, "admin.turret.give_needs_player");
                return;
            }
        }

        int amount = 1;

        if (args.length >= 5) {
            try {
                amount = Math.max(1, Integer.parseInt(args[4]));
            } catch (NumberFormatException e) {
                lang.send(sender, "admin.turret.invalid_amount", "value", args[4]);
                return;
            }
        }

        target.getInventory().addItem(TurretItem.create(plugin, definitionOpt.get(), lang, amount));

        lang.send(sender, "admin.turret.given",
                "amount", amount, "id", args[2], "player", target.getName());
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
                case "ammo" -> TabCompleteUtil.filter(args[1], List.of("give"));
                default -> List.of();
            };
        }

        if (args.length == 3 && sub.equals("place")) {
            return TabCompleteUtil.filter(args[2], List.of("zone"));
        }

        if (sub.equals("turret")) {
            return turretTabComplete(args);
        }

        if (sub.equals("ammo") && args.length == 3 && args[1].equalsIgnoreCase("give")) {
            return TabCompleteUtil.filter(args[2],
                    ammoManager.getAll().stream().map(a -> a.id()).toList());
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
