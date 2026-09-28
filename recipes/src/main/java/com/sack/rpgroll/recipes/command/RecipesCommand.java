package com.sack.rpgroll.recipes.command;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.recipes.RecipesPlugin;
import com.sack.rpgroll.recipes.book.RecipeBook;
import com.sack.rpgroll.recipes.gui.Viewer;
import com.sack.rpgroll.recipes.index.IndexService;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * {@code /recetas} abre el catálogo; {@code /recetas <texto>} lo abre ya buscando.
 * Subcomandos: buscar, mano, usos, libro, fuentes y recargar.
 */
public final class RecipesCommand implements TabExecutor {

    private static final List<String> PLAYER_SUBS = List.of("buscar", "mano", "usos", "libro");
    private static final List<String> ADMIN_SUBS = List.of("fuentes", "recargar");

    private final RecipesPlugin plugin;
    private final LangManager lang;
    private final Viewer viewer;
    private final IndexService indexes;
    private final RecipeBook book;

    public RecipesCommand(RecipesPlugin plugin, LangManager lang, Viewer viewer, IndexService indexes, RecipeBook book) {
        this.plugin = plugin;
        this.lang = lang;
        this.viewer = viewer;
        this.indexes = indexes;
        this.book = book;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "fuentes" -> {
                if (admin(sender)) {
                    sources(sender);
                }
                return true;
            }
            case "recargar" -> {
                if (admin(sender)) {
                    plugin.reload();
                    lang.send(sender, "msg.reloaded", "recipes", indexes.current().recipes().size());
                }
                return true;
            }
            case "libro" -> {
                giveBook(sender, args);
                return true;
            }
            default -> {
            }
        }

        if (!(sender instanceof Player player)) {
            lang.send(sender, "msg.player_only");
            return true;
        }
        if (!player.hasPermission("rpgrollrecipes.use")) {
            lang.send(player, "msg.no_permission");
            return true;
        }

        switch (sub) {
            case "" -> viewer.openCatalog(player, "");
            case "mano" -> viewer.showRecipes(player, player.getInventory().getItemInMainHand(), Viewer.Mode.MAKE, false);
            case "usos" -> viewer.showRecipes(player, player.getInventory().getItemInMainHand(), Viewer.Mode.USES, false);
            case "buscar" -> viewer.openCatalog(player, String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
            default -> viewer.openCatalog(player, String.join(" ", args));
        }
        return true;
    }

    private void giveBook(CommandSender sender, String[] args) {

        Player target;
        if (args.length > 1) {
            if (!admin(sender)) {
                return;
            }
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                lang.send(sender, "msg.player_not_found", "name", args[1]);
                return;
            }
        } else if (sender instanceof Player player) {
            if (!player.hasPermission("rpgrollrecipes.book")) {
                lang.send(player, "msg.no_permission");
                return;
            }
            target = player;
        } else {
            lang.send(sender, "msg.player_only");
            return;
        }

        book.give(target);
        lang.send(sender, "msg.book_given", "name", target.getName());
    }

    private void sources(CommandSender sender) {
        var index = indexes.current();
        IndexService.Report report = indexes.report();
        lang.send(sender, "sources.header", "recipes", index.recipes().size(),
                "items", index.catalog().size(), "millis", report.millis());
        for (Map.Entry<String, Integer> entry : report.bySource().entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).toList()) {
            lang.send(sender, "sources.line", "source", entry.getKey(), "count", entry.getValue());
        }
        lang.send(sender, report.brewingFromServer() ? "sources.brewing_server" : "sources.brewing_table");
        if (report.special() > 0) {
            lang.send(sender, "sources.special", "count", report.special());
        }
        for (String error : report.sourceErrors()) {
            lang.send(sender, "sources.error", "error", error);
        }
    }

    private boolean admin(CommandSender sender) {
        if (sender.hasPermission("rpgrollrecipes.admin")) {
            return true;
        }
        lang.send(sender, "msg.no_permission");
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> options = new ArrayList<>();
        if (args.length == 1) {
            options.addAll(PLAYER_SUBS);
            if (sender.hasPermission("rpgrollrecipes.admin")) {
                options.addAll(ADMIN_SUBS);
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("libro") && sender.hasPermission("rpgrollrecipes.admin")) {
            Bukkit.getOnlinePlayers().forEach(player -> options.add(player.getName()));
        }
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}
