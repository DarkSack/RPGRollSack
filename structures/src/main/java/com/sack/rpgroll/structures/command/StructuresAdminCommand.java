package com.sack.rpgroll.structures.command;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.structures.StructuresPlugin;
import com.sack.rpgroll.structures.workbench.WorkbenchStamp;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.logging.Level;

/** {@code /structuresadmin give <jugador> <nivel> [cantidad]} y {@code reload}. */
public final class StructuresAdminCommand implements CommandExecutor, TabCompleter {

    public static final String PERM_GIVE = "rpgrollstructures.admin.give";
    public static final String PERM_RELOAD = "rpgrollstructures.admin.reload";

    private static final List<String> TIERS = List.of("constructor", "ingeniero", "arquitecto");

    private final StructuresPlugin plugin;

    public StructuresAdminCommand(StructuresPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        LangManager lang = plugin.lang();
        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "give" -> give(sender, lang, Arrays.copyOfRange(args, 1, args.length));
            case "reload" -> reload(sender, lang);
            default -> lang.send(sender, "admin.usage");
        }
        return true;
    }

    private void give(CommandSender sender, LangManager lang, String[] args) {
        if (!sender.hasPermission(PERM_GIVE)) {
            lang.send(sender, "admin.no_permission");
            return;
        }
        GiveArgs.Result parsed = GiveArgs.parse(args);
        if (!parsed.ok()) {
            lang.send(sender, parsed.error());
            return;
        }
        GiveArgs g = parsed.args();
        Player target = Bukkit.getPlayerExact(g.player());
        if (target == null) {
            lang.send(sender, "admin.give.offline", "player", g.player());
            return;
        }
        String tier = g.tier().name().toLowerCase(Locale.ROOT);
        plugin.issuer().issue(g.tier(), g.amount()).whenComplete((stamps, err) -> {
            if (err != null) {
                plugin.getLogger().log(Level.SEVERE, "No se pudieron emitir las mesas", err);
                lang.send(sender, "admin.give.error");
                return;
            }
            // La base pudo tardar: si el jugador se fue (o volvió a entrar con otro objeto), no se le da a un fantasma.
            Player now = Bukkit.getPlayer(target.getUniqueId());
            if (now == null) {
                lang.send(sender, "admin.give.offline", "player", g.player());
                return;
            }
            for (WorkbenchStamp stamp : stamps) {
                ItemStack item = plugin.itemFor(stamp);
                Map<Integer, ItemStack> rest = now.getInventory().addItem(item);
                rest.values().forEach(r -> now.getWorld().dropItemNaturally(now.getLocation(), r));
            }
            lang.send(sender, "admin.give.done", "amount", g.amount(), "tier", tier, "player", now.getName());
        });
    }

    private void reload(CommandSender sender, LangManager lang) {
        if (!sender.hasPermission(PERM_RELOAD)) {
            lang.send(sender, "admin.no_permission");
            return;
        }
        Consumer<String> done = key -> plugin.lang().send(sender, key);
        try {
            plugin.reloadAll();
            done.accept("admin.reload.done");
        } catch (RuntimeException e) {
            plugin.getLogger().log(Level.SEVERE, "Fallo al recargar", e);
            done.accept("admin.reload.error");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        boolean give = sender.hasPermission(PERM_GIVE);
        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            if (give) {
                subs.add("give");
            }
            if (sender.hasPermission(PERM_RELOAD)) {
                subs.add("reload");
            }
            return subs;
        }
        if (give && args.length >= 2 && args[0].equalsIgnoreCase("give")) {
            if (args.length == 2) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            }
            if (args.length == 3) {
                return TIERS;
            }
            if (args.length == 4) {
                return List.of("1", "8", "64");
            }
        }
        return List.of();
    }
}
