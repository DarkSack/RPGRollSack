package com.sack.rpgroll.machines.command;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.machines.MachinesPlugin;
import com.sack.rpgroll.machines.core.Ui;
import com.sack.rpgroll.machines.furnace.FurnaceService;
import com.sack.rpgroll.machines.furnace.FurnaceSettings;
import com.sack.rpgroll.machines.quarry.Quarry;
import com.sack.rpgroll.machines.quarry.QuarryService;
import com.sack.rpgroll.machines.spawner.SpawnerData;
import com.sack.rpgroll.machines.spawner.SpawnerService;
import com.sack.rpgroll.util.ItemDeliveryUtil;
import com.sack.rpgroll.util.TabCompleteUtil;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * /machines give &lt;jugador&gt; furnace|blast_furnace|smoker &lt;nivel&gt; [cantidad]
 * /machines give &lt;jugador&gt; spawner &lt;mob&gt; [cantidad]
 * /machines give &lt;jugador&gt; quarry [cantidad]
 * /machines quarries [jugador]
 * /machines info        — el bloque que miras
 * /machines reload
 */
public class MachinesCommand implements CommandExecutor, TabCompleter {

    public static final String PERMISSION = "rpgroll.machines.admin";
    private static final List<String> SUBCOMMANDS = List.of("give", "quarries", "info", "reload");
    private static final List<String> KINDS = List.of("furnace", "blast_furnace", "smoker", "spawner", "quarry");

    private final MachinesPlugin plugin;
    private final LangManager lang;
    private final FurnaceService furnaces;
    private final SpawnerService spawners;
    private final QuarryService quarries;

    public MachinesCommand(MachinesPlugin plugin, LangManager lang, FurnaceService furnaces, SpawnerService spawners,
            QuarryService quarries) {
        this.plugin = plugin;
        this.lang = lang;
        this.furnaces = furnaces;
        this.spawners = spawners;
        this.quarries = quarries;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission(PERMISSION)) {
            lang.send(sender, "general.no_permission");
            return true;
        }
        switch (args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT)) {
            case "give" -> give(sender, args);
            case "quarries" -> listQuarries(sender, args);
            case "info" -> info(sender);
            case "reload" -> {
                plugin.reload();
                lang.send(sender, "command.reloaded");
            }
            default -> lang.send(sender, "command.usage");
        }
        return true;
    }

    private void give(CommandSender sender, String[] args) {

        if (args.length < 3) {
            lang.send(sender, "command.usage");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            lang.send(sender, "command.player_not_found", "player", args[1]);
            return;
        }
        String kind = args[2].toLowerCase(Locale.ROOT);
        ItemStack item;
        int amountIndex;
        switch (kind) {
            case "furnace", "blast_furnace", "smoker" -> {
                if (args.length < 4) {
                    lang.send(sender, "command.usage");
                    return;
                }
                var tier = furnaces.settings().tier(args[3]);
                if (tier.isEmpty()) {
                    lang.send(sender, "command.unknown_tier", "tier", args[3]);
                    return;
                }
                item = furnaces.item(Material.valueOf(kind.toUpperCase(Locale.ROOT)), tier.get(), 1);
                amountIndex = 4;
            }
            case "spawner" -> {
                if (args.length < 4) {
                    lang.send(sender, "command.usage");
                    return;
                }
                EntityType mob = SpawnerService.entity(args[3].contains(":") ? args[3] : "minecraft:" + args[3]);
                if (mob == null || !mob.isSpawnable() || !mob.isAlive()) {
                    lang.send(sender, "command.unknown_mob", "mob", args[3]);
                    return;
                }
                item = spawners.item(mob, SpawnerData.NONE, 1);
                amountIndex = 4;
            }
            case "quarry" -> {
                item = quarries.item(null, 1);
                amountIndex = 3;
            }
            default -> {
                lang.send(sender, "command.usage");
                return;
            }
        }
        int amount = 1;
        if (args.length > amountIndex) {
            try {
                amount = Math.clamp(Integer.parseInt(args[amountIndex]), 1, 64);
            } catch (NumberFormatException e) {
                lang.send(sender, "command.bad_amount");
                return;
            }
        }
        item.setAmount(amount);
        ItemDeliveryUtil.deliver(target, item);
        lang.send(sender, "command.given", "amount", amount, "kind", kind, "player", target.getName());
    }

    private void listQuarries(CommandSender sender, String[] args) {
        String filter = args.length > 1 ? args[1] : null;
        List<Quarry> list = quarries.store().all().stream()
                .filter(q -> filter == null || q.ownerName().equalsIgnoreCase(filter)).toList();
        lang.send(sender, "command.quarries_header", "count", list.size());
        for (Quarry q : list) {
            lang.send(sender, "command.quarries_line", "owner", q.ownerName(), "world", q.world(), "x", q.x(),
                    "y", q.y(), "z", q.z(), "state", lang.raw("quarry.state." + q.status().name().toLowerCase(Locale.ROOT)),
                    "depth", depth(q));
        }
    }

    private void info(CommandSender sender) {

        if (!(sender instanceof Player player)) {
            lang.send(sender, "general.player_only");
            return;
        }
        Block block = player.getTargetBlockExact(6);
        if (block == null) {
            lang.send(sender, "command.info_nothing");
            return;
        }
        if (FurnaceSettings.FURNACES.contains(block.getType())) {
            lang.send(sender, "command.info_furnace", "tier", furnaces.rawTier(block).orElse("-"));
            return;
        }
        var spawner = spawners.spawner(block);
        if (spawner.isPresent()) {
            SpawnerData data = spawners.data(spawner.get());
            lang.send(sender, "command.info_spawner", "mob", String.valueOf(spawner.get().getSpawnedType()),
                    "speed", Ui.roman(data.speed()), "count", Ui.roman(data.count()), "range", Ui.roman(data.range()),
                    "stack", data.stack());
            return;
        }
        var quarry = quarries.at(block);
        if (quarry.isPresent()) {
            Quarry q = quarry.get();
            lang.send(sender, "command.info_quarry", "owner", q.ownerName(),
                    "state", lang.raw("quarry.state." + q.status().name().toLowerCase(Locale.ROOT)), "depth", depth(q),
                    "side", quarries.side(q));
            return;
        }
        lang.send(sender, "command.info_nothing");
    }

    /** La altura por la que va, sin pasarse del fondo del mundo cuando ya terminó. */
    private static int depth(Quarry quarry) {
        var world = Bukkit.getWorld(quarry.world());
        return world == null ? quarry.cursorY() : Math.max(world.getMinHeight(), quarry.cursorY());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (!sender.hasPermission(PERMISSION)) {
            return List.of();
        }
        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("give")) {
            if (args.length == 2) {
                return TabCompleteUtil.onlinePlayerNames(args[1]);
            }
            if (args.length == 3) {
                return TabCompleteUtil.filter(args[2], KINDS);
            }
            if (args.length == 4) {
                String kind = args[2].toLowerCase(Locale.ROOT);
                if (kind.equals("spawner")) {
                    List<String> mobs = new ArrayList<>();
                    Arrays.stream(EntityType.values()).filter(t -> t != EntityType.UNKNOWN && t.isSpawnable() && t.isAlive())
                            .forEach(t -> mobs.add(t.getKey().getKey()));
                    return TabCompleteUtil.filter(args[3], mobs);
                }
                if (List.of("furnace", "blast_furnace", "smoker").contains(kind)) {
                    return TabCompleteUtil.filter(args[3], furnaces.settings().tiers().stream().map(t -> t.id()).toList());
                }
            }
        }
        if (sub.equals("quarries") && args.length == 2) {
            return TabCompleteUtil.onlinePlayerNames(args[1]);
        }
        return List.of();
    }
}
