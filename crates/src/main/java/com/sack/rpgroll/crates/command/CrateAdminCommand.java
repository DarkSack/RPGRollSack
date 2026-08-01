package com.sack.rpgroll.crates.command;

import com.sack.rpgroll.crates.core.Crate;
import com.sack.rpgroll.crates.core.CrateManager;
import com.sack.rpgroll.crates.hologram.DecentHologramsHook;
import com.sack.rpgroll.crates.key.CrateKeyItem;
import com.sack.rpgroll.crates.location.PlacedCrate;
import com.sack.rpgroll.crates.location.PlacedCrateManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * /crate setlocation <crateId>   — registra el bloque que estás mirando
 * /crate removelocation          — quita el registro del bloque que estás mirando
 * /crate givekey <jugador> <crateId> [cantidad]
 * /crate list
 * /crate reload
 */
public class CrateAdminCommand implements CommandExecutor {

    private static final double HOLOGRAM_Y_OFFSET = 1.6;
    private static final int LOOK_RANGE = 8;

    private final CrateManager crateManager;
    private final PlacedCrateManager placedCrateManager;
    private final DecentHologramsHook hologramsHook;
    private final CrateKeyItem crateKeyItem;

    public CrateAdminCommand(
            CrateManager crateManager,
            PlacedCrateManager placedCrateManager,
            DecentHologramsHook hologramsHook,
            CrateKeyItem crateKeyItem) {

        this.crateManager = crateManager;
        this.placedCrateManager = placedCrateManager;
        this.hologramsHook = hologramsHook;
        this.crateKeyItem = crateKeyItem;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo jugadores pueden usar este comando.");
            return true;
        }

        if (!player.hasPermission("rpgrollcrates.admin.*")) {
            player.sendMessage(Component.text("No tienes permiso para usar este comando.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            sendUsage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "setlocation" -> handleSetLocation(player, args);
            case "removelocation" -> handleRemoveLocation(player);
            case "givekey" -> handleGiveKey(player, args);
            case "list" -> handleList(player);
            case "reload" -> handleReload(player);
            default -> sendUsage(player);
        }

        return true;
    }

    private void sendUsage(Player player) {
        player.sendMessage(Component.text(
                "Uso: /crate <setlocation|removelocation|givekey|list|reload> [args]", NamedTextColor.RED));
    }

    private void handleSetLocation(Player player, String[] args) {

        if (args.length < 2) {
            player.sendMessage(Component.text("Uso: /crate setlocation <crateId>", NamedTextColor.RED));
            return;
        }

        String crateId = args[1];
        Optional<Crate> crateOpt = crateManager.get(crateId);

        if (crateOpt.isEmpty()) {
            player.sendMessage(Component.text("No existe un crate con id: " + crateId, NamedTextColor.RED));
            return;
        }

        Block target = player.getTargetBlockExact(LOOK_RANGE);

        if (target == null || target.isEmpty()) {
            player.sendMessage(Component.text("Tenés que estar mirando el bloque del crate.", NamedTextColor.RED));
            return;
        }

        Location blockLocation = target.getLocation();

        if (placedCrateManager.findAt(blockLocation).isPresent()) {
            player.sendMessage(Component.text("Ese bloque ya tiene un crate registrado.", NamedTextColor.RED));
            return;
        }

        PlacedCrate placed = placedCrateManager.add(crateId, blockLocation);

        Location hologramLocation = blockLocation.clone().add(0.5, HOLOGRAM_Y_OFFSET, 0.5);
        hologramsHook.createOrUpdate(placed.hologramName(), hologramLocation, crateOpt.get().hologramLines());

        player.sendMessage(Component.text(
                "✔ Crate '" + crateId + "' registrado en este bloque (id: " + placed.placementId() + ").",
                NamedTextColor.GREEN));
    }

    private void handleRemoveLocation(Player player) {

        Block target = player.getTargetBlockExact(LOOK_RANGE);

        if (target == null) {
            player.sendMessage(Component.text("Tenés que estar mirando el bloque del crate.", NamedTextColor.RED));
            return;
        }

        Optional<PlacedCrate> placedOpt = placedCrateManager.findAt(target.getLocation());

        if (placedOpt.isEmpty()) {
            player.sendMessage(Component.text("Ese bloque no tiene un crate registrado.", NamedTextColor.RED));
            return;
        }

        PlacedCrate placed = placedOpt.get();
        hologramsHook.remove(placed.hologramName());
        placedCrateManager.remove(placed.placementId());

        player.sendMessage(Component.text("✔ Crate eliminado de este bloque.", NamedTextColor.GREEN));
    }

    private void handleGiveKey(Player player, String[] args) {

        if (args.length < 3) {
            player.sendMessage(Component.text(
                    "Uso: /crate givekey <jugador> <crateId> [cantidad]", NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);

        if (target == null) {
            player.sendMessage(Component.text("Jugador no encontrado: " + args[1], NamedTextColor.RED));
            return;
        }

        String crateId = args[2];
        Optional<Crate> crateOpt = crateManager.get(crateId);

        if (crateOpt.isEmpty()) {
            player.sendMessage(Component.text("No existe un crate con id: " + crateId, NamedTextColor.RED));
            return;
        }

        int amount = 1;

        if (args.length >= 4) {
            try {
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException ignored) {
            }
        }

        target.getInventory().addItem(crateKeyItem.create(crateOpt.get(), amount));

        player.sendMessage(Component.text(
                "✔ " + amount + " llave(s) de '" + crateId + "' entregadas a " + target.getName(),
                NamedTextColor.GREEN));
    }

    private void handleList(Player player) {

        if (crateManager.count() == 0) {
            player.sendMessage(Component.text("No hay crates definidos.", NamedTextColor.GRAY));
            return;
        }

        player.sendMessage(Component.text("Crates disponibles:", NamedTextColor.GOLD));

        for (Crate crate : crateManager.getAll()) {

            long placements = placedCrateManager.getAll().stream()
                    .filter(p -> p.crateId().equals(crate.id()))
                    .count();

            player.sendMessage(Component.text(
                    "• " + crate.id() + " (" + crate.displayName() + ") — " + placements + " ubicación(es)",
                    NamedTextColor.WHITE));
        }
    }

    private void handleReload(Player player) {

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

        player.sendMessage(Component.text(
                "✔ Recargado: " + crateManager.count() + " crate(s), " + placedCrateManager.getAll().size()
                        + " ubicación(es).",
                NamedTextColor.GREEN));
    }

}
