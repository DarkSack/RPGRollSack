package com.sack.rpgroll.furniture.command;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.furniture.core.CarpenterRecipe;
import com.sack.rpgroll.furniture.core.FurnitureDefinition;
import com.sack.rpgroll.furniture.core.FurnitureManager;
import com.sack.rpgroll.furniture.gui.FurnitureMenu;
import com.sack.rpgroll.furniture.item.FurnitureItems;
import com.sack.rpgroll.furniture.placed.FurnitureService;
import com.sack.rpgroll.furniture.placed.PlacedFurniture;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * /furnitureadmin:
 * <ul>
 *   <li>{@code give <jugador> <mueble>[:variante] [cantidad]} — para crates, pase, votos y NPCs.</li>
 *   <li>{@code carpenter <jugador> [estación]} — abre un carpintero (para la acción de un NPC).</li>
 *   <li>{@code list}, {@code info}, {@code nearby [radio]}, {@code remove [radio]}, {@code reload}.</li>
 * </ul>
 */
public class FurnitureAdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("give", "carpenter", "list", "info", "nearby", "remove",
            "reload");

    private final FurnitureManager manager;
    private final FurnitureItems items;
    private final FurnitureService service;
    private final LangManager lang;
    private final Runnable reload;

    public FurnitureAdminCommand(FurnitureManager manager, FurnitureItems items, FurnitureService service,
            LangManager lang, Runnable reload) {
        this.manager = manager;
        this.items = items;
        this.service = service;
        this.lang = lang;
        this.reload = reload;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "give" -> give(sender, args);
            case "carpenter" -> carpenter(sender, args);
            case "list" -> list(sender);
            case "info" -> info(sender);
            case "nearby" -> nearby(sender, args, false);
            case "remove" -> nearby(sender, args, true);
            case "reload" -> {
                reload.run();
                lang.send(sender, "admin.reloaded", "count", manager.count());
            }
            default -> lang.send(sender, "admin.help");
        }
        return true;
    }

    private void give(CommandSender sender, String[] args) {

        if (args.length < 3) {
            lang.send(sender, "admin.give_usage");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            lang.send(sender, "admin.player_not_found", "player", args[1]);
            return;
        }

        String[] ref = args[2].split(":", 2);
        Optional<FurnitureDefinition> def = manager.get(ref[0]);
        if (def.isEmpty()) {
            lang.send(sender, "admin.unknown_furniture", "id", ref[0]);
            return;
        }

        String variant = ref.length > 1 ? ref[1].toLowerCase(Locale.ROOT) : null;
        if (variant != null && def.get().variant(variant).isEmpty()) {
            lang.send(sender, "admin.unknown_variant", "id", def.get().id(), "variant", variant,
                    "variants", String.join(", ", def.get().variants().keySet()));
            return;
        }

        int amount = 1;
        if (args.length > 3) {
            try {
                amount = Math.max(1, Math.min(64 * 36, Integer.parseInt(args[3])));
            } catch (NumberFormatException e) {
                lang.send(sender, "admin.bad_number", "value", args[3]);
                return;
            }
        }

        int remaining = amount;
        while (remaining > 0) {
            int batch = Math.min(remaining, def.get().material().getMaxStackSize());
            ItemStack item = items.create(def.get(), variant, batch);
            target.getInventory().addItem(item).values().forEach(left ->
                    target.getWorld().dropItemNaturally(target.getLocation(), left));
            remaining -= batch;
        }

        lang.send(sender, "admin.given", "amount", amount, "name", def.get().displayName(def.get().resolveVariant(variant)),
                "player", target.getName());
    }

    private void carpenter(CommandSender sender, String[] args) {

        if (args.length < 2) {
            lang.send(sender, "admin.carpenter_usage");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            lang.send(sender, "admin.player_not_found", "player", args[1]);
            return;
        }
        String station = args.length > 2 ? args[2].toLowerCase(Locale.ROOT) : CarpenterRecipe.DEFAULT_STATION;
        new FurnitureMenu(target, manager, items, lang, FurnitureMenu.Mode.CARPENTER, station).open();
    }

    private void list(CommandSender sender) {

        lang.send(sender, "admin.list_header", "count", manager.count(), "placed", service.index().size());
        for (FurnitureManager.Category category : manager.categories()) {
            List<String> ids = manager.inCategory(category.id(), d -> true).stream().map(FurnitureDefinition::id).toList();
            if (!ids.isEmpty()) {
                lang.send(sender, "admin.list_line", "category", category.name(), "ids", String.join(", ", ids));
            }
        }
    }

    private void info(CommandSender sender) {

        if (!(sender instanceof Player player)) {
            lang.send(sender, "command.players_only");
            return;
        }

        Optional<PlacedFurniture> target = lookedAt(player);
        if (target.isEmpty()) {
            lang.send(player, "admin.info_none");
            return;
        }

        PlacedFurniture f = target.get();
        String owner = f.owner() == null ? "-" : Optional.ofNullable(Bukkit.getOfflinePlayer(f.owner()).getName())
                .orElse(f.owner().toString());
        lang.send(player, "admin.info", "id", f.furnitureId(), "variant", String.valueOf(f.variant()),
                "state", f.state(), "owner", owner, "yaw", Math.round(f.yaw()),
                "x", f.anchor().getX(), "y", f.anchor().getY(), "z", f.anchor().getZ());
    }

    /** Lista o quita los muebles cercanos (quitar no suelta el ítem, sí lo que guardaban). */
    private void nearby(CommandSender sender, String[] args, boolean remove) {

        if (!(sender instanceof Player player)) {
            lang.send(sender, "command.players_only");
            return;
        }

        double radius = 8;
        if (args.length > 1) {
            try {
                radius = Math.max(1, Math.min(64, Double.parseDouble(args[1])));
            } catch (NumberFormatException e) {
                lang.send(sender, "admin.bad_number", "value", args[1]);
                return;
            }
        }

        List<PlacedFurniture> found = new ArrayList<>();
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            PlacedFurniture furniture = PlacedFurniture.of(entity, service.keys());
            if (furniture != null) {
                found.add(furniture);
            }
        }

        if (remove) {
            found.forEach(f -> service.remove(f, false));
            lang.send(player, "admin.removed", "count", found.size(), "radius", (int) radius);
            return;
        }

        lang.send(player, "admin.nearby_header", "count", found.size(), "radius", (int) radius);
        for (PlacedFurniture f : found) {
            Location at = f.anchor().getLocation();
            lang.send(player, "admin.nearby_line", "id", f.furnitureId(), "x", at.getBlockX(), "y", at.getBlockY(),
                    "z", at.getBlockZ());
        }
    }

    /** El mueble que el jugador tiene delante (su barrera, su Interaction o su modelo). */
    private Optional<PlacedFurniture> lookedAt(Player player) {

        var block = player.getTargetBlockExact(6);
        if (block != null) {
            Optional<PlacedFurniture> byBlock = service.at(block);
            if (byBlock.isPresent()) {
                return byBlock;
            }
        }

        Entity entity = player.getTargetEntity(6, true);
        if (entity != null) {
            Optional<PlacedFurniture> byEntity = service.fromEntity(entity);
            if (byEntity.isPresent()) {
                return byEntity;
            }
        }

        // Lo más cercano en la dirección de la mirada, por si el modelo no tiene nada sólido delante.
        Location eye = player.getEyeLocation();
        return player.getNearbyEntities(4, 4, 4).stream()
                .filter(e -> e instanceof ItemDisplay)
                .map(e -> PlacedFurniture.of(e, service.keys()))
                .filter(java.util.Objects::nonNull)
                .min(java.util.Comparator.comparingDouble(f -> f.center().distanceSquared(eye)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        String last = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);

        if (args.length == 1) {
            return filter(SUBCOMMANDS, last);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if ((sub.equals("give") || sub.equals("carpenter")) && args.length == 2) {
            return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), last);
        }
        if (sub.equals("give") && args.length == 3) {
            List<String> refs = new ArrayList<>();
            for (FurnitureDefinition def : manager.all()) {
                refs.add(def.id());
                def.variants().keySet().forEach(v -> refs.add(def.id() + ":" + v));
            }
            return filter(refs, last);
        }
        if (sub.equals("carpenter") && args.length == 3) {
            List<String> stations = manager.all().stream()
                    .flatMap(def -> java.util.stream.Stream.concat(
                            java.util.stream.Stream.ofNullable(def.recipe()),
                            def.variants().values().stream().map(v -> v.recipe()).filter(java.util.Objects::nonNull)))
                    .map(CarpenterRecipe::station).distinct().toList();
            return filter(stations, last);
        }
        return List.of();
    }

    private static List<String> filter(List<String> options, String prefix) {
        return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(prefix)).limit(80).toList();
    }
}
