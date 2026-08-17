package com.sack.rpgroll.crates.command;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.crates.CratesPlugin;
import com.sack.rpgroll.crates.core.Crate;
import com.sack.rpgroll.crates.core.CrateManager;
import com.sack.rpgroll.crates.gui.ChatPromptManager;
import com.sack.rpgroll.crates.gui.CrateBrowserGUI;
import com.sack.rpgroll.crates.hologram.DecentHologramsHook;
import com.sack.rpgroll.crates.key.CrateKeyItem;
import com.sack.rpgroll.crates.location.PlacedCrate;
import com.sack.rpgroll.crates.location.PlacedCrateManager;
import com.sack.rpgroll.util.TabCompleteUtil;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

/**
 * /crate setlocation <crateId>   — registra el bloque que estás mirando
 * /crate removelocation          — quita el registro del bloque que estás mirando
 * /crate givekey <jugador> <crateId> [cantidad]
 * /crate list
 * /crate reload
 */
public class CrateAdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("setlocation", "removelocation", "givekey", "list",
            "reload", "browser");

    private static final double HOLOGRAM_Y_OFFSET = 1.6;
    private static final int LOOK_RANGE = 8;

    private final CratesPlugin plugin;
    private final CrateManager crateManager;
    private final PlacedCrateManager placedCrateManager;
    private final DecentHologramsHook hologramsHook;
    private final CrateKeyItem crateKeyItem;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;

    public CrateAdminCommand(
            CratesPlugin plugin,
            CrateManager crateManager,
            PlacedCrateManager placedCrateManager,
            DecentHologramsHook hologramsHook,
            CrateKeyItem crateKeyItem,
            ChatPromptManager chatPromptManager,
            LangManager lang) {

        this.plugin = plugin;
        this.crateManager = crateManager;
        this.placedCrateManager = placedCrateManager;
        this.hologramsHook = hologramsHook;
        this.crateKeyItem = crateKeyItem;
        this.chatPromptManager = chatPromptManager;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("rpgrollcrates.admin.*")) {
            lang.send(sender, "general.no_permission");
            return true;
        }

        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "setlocation" -> {
                Player player = requirePlayer(sender);
                if (player != null) {
                    handleSetLocation(player, args);
                }
            }
            case "removelocation" -> {
                Player player = requirePlayer(sender);
                if (player != null) {
                    handleRemoveLocation(player);
                }
            }
            case "givekey" -> handleGiveKey(sender, args);
            case "list" -> handleList(sender);
            case "reload" -> handleReload(sender);
            case "browser" -> {
                Player player = requirePlayer(sender);
                if (player != null) {
                    new CrateBrowserGUI(player, crateManager, chatPromptManager, lang).open();
                }
            }
            default -> sendUsage(sender);
        }

        return true;
    }

    /** @return el sender como Player, o null (ya avisado) si no lo es — usado por los subcomandos que necesitan mirar un bloque o abrir una GUI. */
    private Player requirePlayer(CommandSender sender) {

        if (sender instanceof Player player) {
            return player;
        }

        lang.send(sender, "general.player_only");
        return null;
    }

    private void sendUsage(CommandSender sender) {
        lang.send(sender, "admin.usage");
    }

    private void handleSetLocation(Player player, String[] args) {

        if (args.length < 2) {
            lang.send(player, "admin.setlocation_usage");
            return;
        }

        String crateId = args[1];
        Optional<Crate> crateOpt = crateManager.get(crateId);

        if (crateOpt.isEmpty()) {
            lang.send(player, "admin.crate_not_found", "id", crateId);
            return;
        }

        Block target = player.getTargetBlockExact(LOOK_RANGE);

        if (target == null || target.isEmpty()) {
            lang.send(player, "admin.must_look_at_block");
            return;
        }

        Location blockLocation = target.getLocation();

        if (placedCrateManager.findAt(blockLocation).isPresent()) {
            lang.send(player, "admin.block_already_registered");
            return;
        }

        PlacedCrate placed = placedCrateManager.add(crateId, blockLocation);

        Location hologramLocation = blockLocation.clone().add(0.5, HOLOGRAM_Y_OFFSET, 0.5);
        hologramsHook.createOrUpdate(placed.hologramName(), hologramLocation, crateOpt.get().hologramLines());

        lang.send(player, "admin.setlocation_success", "crate", crateId, "placement", placed.placementId());
    }

    private void handleRemoveLocation(Player player) {

        Block target = player.getTargetBlockExact(LOOK_RANGE);

        if (target == null) {
            lang.send(player, "admin.must_look_at_block");
            return;
        }

        Optional<PlacedCrate> placedOpt = placedCrateManager.findAt(target.getLocation());

        if (placedOpt.isEmpty()) {
            lang.send(player, "admin.block_not_registered");
            return;
        }

        PlacedCrate placed = placedOpt.get();
        hologramsHook.remove(placed.hologramName());
        placedCrateManager.remove(placed.placementId());

        lang.send(player, "admin.removelocation_success");
    }

    private static final int MAX_GIVE_AMOUNT = 6400;

    private void handleGiveKey(CommandSender sender, String[] args) {

        if (args.length < 3) {
            lang.send(sender, "admin.givekey_usage");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);

        if (target == null) {
            lang.send(sender, "admin.player_not_found", "player", args[1]);
            return;
        }

        String crateId = args[2];
        Optional<Crate> crateOpt = crateManager.get(crateId);

        if (crateOpt.isEmpty()) {
            lang.send(sender, "admin.crate_not_found", "id", crateId);
            return;
        }

        int amount = 1;

        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                lang.send(sender, "admin.invalid_amount");
                return;
            }
        }

        if (amount < 1 || amount > MAX_GIVE_AMOUNT) {
            lang.send(sender, "admin.amount_out_of_range", "max", MAX_GIVE_AMOUNT);
            return;
        }

        boolean fullyDelivered = com.sack.rpgroll.util.ItemDeliveryUtil.deliver(
                target, crateKeyItem.create(crateOpt.get(), amount));

        lang.send(sender, fullyDelivered ? "admin.givekey_success" : "admin.givekey_success_overflow",
                "amount", amount, "crate", crateId, "player", target.getName());
    }

    private void handleList(CommandSender sender) {

        if (crateManager.count() == 0) {
            lang.send(sender, "admin.list_empty");
            return;
        }

        lang.send(sender, "admin.list_header");

        for (Crate crate : crateManager.getAll()) {

            long placements = placedCrateManager.getAll().stream()
                    .filter(p -> p.crateId().equals(crate.id()))
                    .count();

            lang.send(sender, "admin.list_entry", "id", crate.id(), "name", crate.displayName(),
                    "count", placements);
        }
    }

    private void handleReload(CommandSender sender) {

        plugin.reloadConfig();
        lang.reload(plugin.getConfig().getString("language", "es"));

        crateManager.reload();

        for (PlacedCrate placed : placedCrateManager.getAll()) {

            Optional<Crate> crateOpt = crateManager.get(placed.crateId());

            if (crateOpt.isEmpty()) {
                continue;
            }

            var world = Bukkit.getWorld(placed.world());
            if (world == null) {
                continue;
            }

            Location hologramLocation = new Location(
                    world, placed.x() + 0.5, placed.y() + HOLOGRAM_Y_OFFSET, placed.z() + 0.5);

            hologramsHook.createOrUpdate(placed.hologramName(), hologramLocation, crateOpt.get().hologramLines());
        }

        lang.send(sender, "admin.reload_success", "crates", crateManager.count(),
                "locations", placedCrateManager.getAll().size());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
        }

        String sub = args[0].toLowerCase();

        if (args.length == 2) {
            return switch (sub) {
                case "setlocation" -> TabCompleteUtil.filter(args[1], crateIds());
                case "givekey" -> TabCompleteUtil.onlinePlayerNames(args[1]);
                default -> List.of();
            };
        }

        if (args.length == 3 && "givekey".equals(sub)) {
            return TabCompleteUtil.filter(args[2], crateIds());
        }

        return List.of();
    }

    private List<String> crateIds() {
        return crateManager.getAll().stream().map(Crate::id).toList();
    }

}
