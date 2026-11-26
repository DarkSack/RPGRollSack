package com.sack.rpgroll.structures.command;

import com.sack.rpgroll.common.command.Senders;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.structures.StructuresPlugin;
import com.sack.rpgroll.structures.catalog.StructureDefinition;
import com.sack.rpgroll.structures.config.CategorySettings.Category;
import com.sack.rpgroll.structures.gui.CategoriesMenu;
import com.sack.rpgroll.structures.gui.DetailMenu;
import com.sack.rpgroll.structures.gui.HelpMenu;
import com.sack.rpgroll.structures.gui.ListMenu;
import com.sack.rpgroll.structures.gui.ProjectMenu;
import com.sack.rpgroll.structures.gui.StructuresMenus;
import com.sack.rpgroll.structures.project.Project;
import com.sack.rpgroll.structures.workbench.WorkbenchTier;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * {@code /structures [help|open|list|info <id>|status|cancel]}. open es el catálogo sin mesa (solo mirar), cancel
 * abre la confirmación de la obra activa del jugador.
 */
public final class StructuresCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBS = List.of("help", "open", "list", "info", "status", "cancel");

    private final StructuresPlugin plugin;

    public StructuresCommand(StructuresPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = Senders.asPlayer(sender);
        if (player == null) {
            plugin.lang().send(sender, "command.player-only");
            return true;
        }
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "help" -> new HelpMenu(player, plugin, 0).open();
            case "open" -> new CategoriesMenu(player, plugin, null, WorkbenchTier.CONSTRUCTOR).open();
            case "list" -> list(player);
            case "info" -> info(player, args);
            case "status" -> status(player);
            case "cancel" -> cancel(player);
            default -> plugin.lang().send(sender, "command.usage");
        }
        return true;
    }

    /** Las categorías que el jugador puede ver, con sus estructuras. */
    private List<Category> visible(Player p) {
        return plugin.categories().categories().stream()
                .filter(c -> CategoriesMenu.allowed(p, c.id()) && !plugin.structures().inCategory(c.id()).isEmpty())
                .toList();
    }

    private void list(Player p) {
        LangManager lang = plugin.lang();
        List<Category> cats = visible(p);
        if (cats.isEmpty()) {
            lang.send(p, "catalog.empty");
            return;
        }
        String language = plugin.settings().language();
        for (Category c : cats) {
            lang.send(p, "catalog.list-category", "category", lang.raw("category." + c.id()));
            for (StructureDefinition d : plugin.structures().inCategory(c.id())) {
                // Sin el permiso de la estructura se ve, pero marcada como bloqueada.
                lang.send(p, ListMenu.permitted(p, d) ? "catalog.list-entry" : "catalog.list-entry-locked", "id",
                        d.id(), "name", d.name(language), "tier", StructuresMenus.tierName(plugin, d.tier()));
            }
        }
    }

    private void info(Player p, String[] args) {
        LangManager lang = plugin.lang();
        if (args.length < 2) {
            lang.send(p, "command.info-usage");
            return;
        }
        Optional<StructureDefinition> def = plugin.structures().get(args[1].toLowerCase(Locale.ROOT));
        if (def.isEmpty()) {
            lang.send(p, "catalog.unknown", "id", args[1]);
        } else if (!CategoriesMenu.allowed(p, def.get().category())) {
            lang.send(p, "catalog.locked-category");
        } else if (!ListMenu.permitted(p, def.get())) {
            lang.send(p, "catalog.locked-permission");
        } else {
            new DetailMenu(p, plugin, null, WorkbenchTier.CONSTRUCTOR, def.get()).open();
        }
    }

    private void status(Player p) {
        LangManager lang = plugin.lang();
        List<Project> mine = plugin.projects().ofPlayer(p.getUniqueId());
        if (mine.isEmpty()) {
            lang.send(p, "build.status-none");
            return;
        }
        String language = plugin.settings().language();
        for (Project pr : mine) {
            lang.send(p, "build.status-entry", "structure",
                    plugin.structures().get(pr.structure()).map(d -> d.name(language)).orElse(pr.structure()),
                    "state", lang.raw("menu.state." + pr.state().name().toLowerCase(Locale.ROOT)),
                    "percent", StructuresMenus.percent(pr.cursor(), pr.total()), "world", pr.world(),
                    "x", pr.x(), "y", pr.y(), "z", pr.z());
        }
    }

    private void cancel(Player p) {
        List<Project> mine = plugin.projects().ofPlayer(p.getUniqueId());
        if (mine.isEmpty()) {
            plugin.lang().send(p, "build.status-none");
        } else if (mine.size() > 1) {
            status(p);
            plugin.lang().send(p, "build.cancel-which");
        } else {
            WorkbenchTier tier = plugin.service().byId(mine.getFirst().bench())
                    .map(b -> b.tier()).orElse(WorkbenchTier.CONSTRUCTOR);
            // Sin mesa que mirar: aquí solo llega el dueño de la obra (ofPlayer), que recupera lo suyo.
            ProjectMenu.confirmCancel(p, plugin, mine.getFirst(), tier, null, p::closeInventory);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return SUBS;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("info") && sender instanceof Player p) {
            return visible(p).stream().flatMap(c -> plugin.structures().inCategory(c.id()).stream())
                    .map(StructureDefinition::id).toList();
        }
        return List.of();
    }
}
