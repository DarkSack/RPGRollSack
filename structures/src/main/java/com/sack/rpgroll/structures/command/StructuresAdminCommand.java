package com.sack.rpgroll.structures.command;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.structures.StructuresPlugin;
import com.sack.rpgroll.structures.catalog.Problem;
import com.sack.rpgroll.structures.catalog.StructureLoader;
import com.sack.rpgroll.structures.integration.Integrations;
import com.sack.rpgroll.structures.project.Project;
import com.sack.rpgroll.structures.workbench.WorkbenchStamp;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * {@code /structuresadmin give <jugador> <nivel> [cantidad]}, {@code reload}, {@code validate}, {@code integrations},
 * {@code debug <id-de-obra | jugador>} y {@code delete <id-de-construcción>}.
 */
public final class StructuresAdminCommand implements CommandExecutor, TabCompleter {

    public static final String PERM_GIVE = "rpgrollstructures.admin.give";
    public static final String PERM_RELOAD = "rpgrollstructures.admin.reload";
    public static final String PERM_VALIDATE = "rpgrollstructures.admin.validate";
    public static final String PERM_ADMIN = "rpgrollstructures.admin";

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
            case "validate" -> validate(sender, lang);
            case "integrations" -> integrations(sender, lang);
            case "debug" -> debug(sender, lang, args.length > 1 ? args[1] : null);
            case "delete" -> delete(sender, lang, args.length > 1 ? args[1] : null);
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

    /** Una línea por integración: ✔ activa o ✘ con su motivo. */
    private void integrations(CommandSender sender, LangManager lang) {
        if (!sender.hasPermission(PERM_ADMIN)) {
            lang.send(sender, "admin.no_permission");
            return;
        }
        for (Integrations.Status st : plugin.integrations().report()) {
            lang.send(sender, "integrations.line", "mark", st.active() ? "✔" : "✘", "plugin", st.hook().plugin(),
                    "reason", st.reasonKey() == null ? "" : lang.raw(st.reasonKey()));
        }
    }

    /**
     * Estado de una obra (por id) o de las obras activas de un jugador (conectado, o desconectado si el servidor lo
     * recuerda): estado, cursor/total, colocados, saltados, motivo, mesa, mundo y origen.
     */
    private void debug(CommandSender sender, LangManager lang, String target) {
        if (!sender.hasPermission(PERM_ADMIN)) {
            lang.send(sender, "admin.no_permission");
            return;
        }
        if (target == null) {
            lang.send(sender, "admin.debug.usage");
            return;
        }
        List<Project> found;
        UUID id = parseUuid(target);
        if (id != null) {
            found = plugin.projects().byId(id).map(List::of).orElse(List.of());
        } else {
            Player online = Bukkit.getPlayerExact(target);
            UUID owner = online != null ? online.getUniqueId()
                    : Optional.ofNullable(Bukkit.getOfflinePlayerIfCached(target)).map(OfflinePlayer::getUniqueId)
                            .orElse(null);
            found = owner == null ? List.of() : plugin.projects().ofPlayer(owner);
        }
        if (found.isEmpty()) {
            lang.send(sender, "admin.debug.none", "target", target);
            return;
        }
        for (Project p : found) {
            lang.send(sender, "admin.debug.header", "id", p.id(), "structure", p.structure(), "state",
                    p.state().name());
            lang.send(sender, "admin.debug.progress", "cursor", p.cursor(), "total", p.total(), "placed", p.placed(),
                    "skipped", p.skipped());
            lang.send(sender, "admin.debug.reason", "reason", p.error() == null ? "-" : p.error());
            lang.send(sender, "admin.debug.where", "bench", p.bench(), "world", p.world(), "x", p.x(), "y", p.y(),
                    "z", p.z());
        }
    }

    /** Borra el registro de una construcción terminada y llama al {@code onRemoved} de su módulo; los bloques quedan. */
    private void delete(CommandSender sender, LangManager lang, String target) {
        if (!sender.hasPermission(PERM_ADMIN)) {
            lang.send(sender, "admin.no_permission");
            return;
        }
        UUID id = target == null ? null : parseUuid(target);
        if (id == null) {
            lang.send(sender, "admin.delete.usage");
            return;
        }
        // El futuro se completa en el hilo principal.
        plugin.modules().remove(id).whenComplete((ok, err) -> {
            if (err != null) {
                plugin.getLogger().log(Level.SEVERE, "No se pudo borrar la construcción " + id, err);
                lang.send(sender, "admin.delete.error");
            } else {
                lang.send(sender, ok ? "admin.delete.done" : "admin.delete.none", "id", id);
            }
        });
    }

    private static UUID parseUuid(String s) {
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Vuelve a leer structures/ sin cambiar el catálogo en uso y lista cada problema con archivo, clave y motivo. */
    private void validate(CommandSender sender, LangManager lang) {
        if (!sender.hasPermission(PERM_VALIDATE)) {
            lang.send(sender, "admin.no_permission");
            return;
        }
        StructureLoader.LoadResult r;
        try {
            r = plugin.scanCatalog();
        } catch (RuntimeException e) {
            plugin.getLogger().log(Level.SEVERE, "Fallo al validar las estructuras", e);
            lang.send(sender, "admin.validate.error");
            return;
        }
        if (r.problems().isEmpty()) {
            lang.send(sender, "admin.validate.ok", "valid", r.valid().size());
            return;
        }
        lang.send(sender, "admin.validate.header", "valid", r.valid().size(), "problems", r.problems().size());
        for (Problem p : r.problems()) {
            lang.send(sender, "admin.validate.problem", "file", p.file(), "key", p.key(), "reason", p.reason());
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
            if (sender.hasPermission(PERM_VALIDATE)) {
                subs.add("validate");
            }
            if (sender.hasPermission(PERM_ADMIN)) {
                subs.add("integrations");
                subs.add("debug");
                subs.add("delete");
            }
            return subs;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("debug") && sender.hasPermission(PERM_ADMIN)) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
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
