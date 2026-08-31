package com.sack.rpgroll.extras.command;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.extras.condition.ConditionDefinition;
import com.sack.rpgroll.extras.condition.ConditionManager;
import com.sack.rpgroll.extras.condition.ConditionRuntime;
import com.sack.rpgroll.extras.stat.StatDefinition;
import com.sack.rpgroll.extras.stat.StatEngine;
import com.sack.rpgroll.extras.stat.StatManager;
import com.sack.rpgroll.util.TabCompleteUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * /extrasadmin reload
 * /extrasadmin list
 * /extrasadmin get &lt;jugador&gt; &lt;stat&gt;
 * /extrasadmin set &lt;jugador&gt; &lt;stat&gt; &lt;valor&gt;
 * /extrasadmin add &lt;jugador&gt; &lt;stat&gt; &lt;cantidad&gt;
 * /extrasadmin apply &lt;jugador&gt; &lt;condition&gt;
 * /extrasadmin remove &lt;jugador&gt; &lt;condition&gt;
 */
public class ExtrasAdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("reload", "list", "get", "set", "add", "apply", "remove");
    private static final String PERMISSION = "rpgrollextras.admin.*";

    private final StatManager statManager;
    private final StatEngine statEngine;
    private final ConditionManager conditionManager;
    private final ConditionRuntime conditionRuntime;
    private final Runnable reloadCallback;
    private final LangManager lang;

    public ExtrasAdminCommand(StatManager statManager, StatEngine statEngine, ConditionManager conditionManager,
            ConditionRuntime conditionRuntime, Runnable reloadCallback, LangManager lang) {
        this.statManager = statManager;
        this.statEngine = statEngine;
        this.conditionManager = conditionManager;
        this.conditionRuntime = conditionRuntime;
        this.reloadCallback = reloadCallback;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission(PERMISSION)) {
            lang.send(sender, "command.no-permission");
            return true;
        }

        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "list" -> handleList(sender);
            case "get" -> handleGet(sender, args);
            case "set" -> handleSet(sender, args);
            case "add" -> handleAdd(sender, args);
            case "apply" -> handleApply(sender, args);
            case "remove" -> handleRemove(sender, args);
            default -> sendUsage(sender);
        }

        return true;
    }

    private void sendUsage(CommandSender sender) {
        lang.send(sender, "command.usage.main");
    }

    private void handleReload(CommandSender sender) {
        reloadCallback.run();
        lang.send(sender, "command.reload.success",
                "stats", statManager.count(), "conditions", conditionManager.count());
    }

    private void handleList(CommandSender sender) {

        lang.send(sender, "command.list.stats-header");
        for (StatDefinition stat : statManager.getAll()) {
            sender.sendMessage(Component.text("• " + stat.id() + " (max " + stat.max() + ")", NamedTextColor.WHITE));
        }

        lang.send(sender, "command.list.conditions-header");
        for (ConditionDefinition condition : conditionManager.getAll()) {
            sender.sendMessage(Component.text("• " + condition.id(), NamedTextColor.WHITE));
        }
    }

    private void handleGet(CommandSender sender, String[] args) {

        if (args.length < 3) {
            lang.send(sender, "command.usage.get");
            return;
        }

        Player target = requireTarget(sender, args[1]);
        if (target == null) {
            return;
        }

        if (!requireStat(sender, args[2])) {
            return;
        }

        double value = statEngine.get(target, args[2]);
        lang.send(sender, "command.get.result",
                "player", target.getName(), "stat", args[2], "value", value);
    }

    private void handleSet(CommandSender sender, String[] args) {

        if (args.length < 4) {
            lang.send(sender, "command.usage.set");
            return;
        }

        Player target = requireTarget(sender, args[1]);
        if (target == null) {
            return;
        }

        if (!requireStat(sender, args[2])) {
            return;
        }

        double value = parseDouble(sender, args[3]);
        if (Double.isNaN(value)) {
            return;
        }

        statEngine.set(target, args[2], value);
        lang.send(sender, "command.set.success",
                "stat", args[2], "player", target.getName(), "value", value);
    }

    private void handleAdd(CommandSender sender, String[] args) {

        if (args.length < 4) {
            lang.send(sender, "command.usage.add");
            return;
        }

        Player target = requireTarget(sender, args[1]);
        if (target == null) {
            return;
        }

        if (!requireStat(sender, args[2])) {
            return;
        }

        double amount = parseDouble(sender, args[3]);
        if (Double.isNaN(amount)) {
            return;
        }

        statEngine.adjust(target, args[2], amount);
        lang.send(sender, "command.add.success",
                "stat", args[2], "player", target.getName(), "amount", amount);
    }

    private void handleApply(CommandSender sender, String[] args) {

        if (args.length < 3) {
            lang.send(sender, "command.usage.apply");
            return;
        }

        Player target = requireTarget(sender, args[1]);
        if (target == null) {
            return;
        }

        var definition = conditionManager.get(args[2]);
        if (definition.isEmpty()) {
            lang.send(sender, "command.condition-not-found", "condition", args[2]);
            return;
        }

        conditionRuntime.apply(target, definition.get());
        lang.send(sender, "command.apply.success", "condition", args[2], "player", target.getName());
    }

    private void handleRemove(CommandSender sender, String[] args) {

        if (args.length < 3) {
            lang.send(sender, "command.usage.remove");
            return;
        }

        Player target = requireTarget(sender, args[1]);
        if (target == null) {
            return;
        }

        conditionRuntime.remove(target, args[2]);
        lang.send(sender, "command.remove.success", "condition", args[2], "player", target.getName());
    }

    /**
     * Valida que la stat exista antes de leerla o escribirla.
     * <p>
     * Sin esto, {@code get <jugador> no_existe} devolvía {@code 0.0} — un
     * valor inventado que hacía creer al admin que la stat existe y está en
     * cero. Las conditions ya se validaban así; las stats no.
     *
     * @return true si existe; si no, ya se le avisó al sender
     */
    private boolean requireStat(CommandSender sender, String id) {

        if (statManager.get(id).isPresent()) {
            return true;
        }

        lang.send(sender, "command.stat-not-found", "stat", id);
        return false;
    }

    private Player requireTarget(CommandSender sender, String name) {

        Player target = Bukkit.getPlayerExact(name);

        if (target == null) {
            lang.send(sender, "command.player-not-found", "player", name);
        }

        return target;
    }

    private double parseDouble(CommandSender sender, String raw) {

        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            lang.send(sender, "command.invalid-number", "value", raw);
            return Double.NaN;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
        }

        String sub = args[0].toLowerCase();

        if (args.length == 2 && List.of("get", "set", "add", "apply", "remove").contains(sub)) {
            return TabCompleteUtil.onlinePlayerNames(args[1]);
        }

        if (args.length == 3) {

            if (List.of("get", "set", "add").contains(sub)) {
                return TabCompleteUtil.filter(args[2], statManager.getAll().stream().map(StatDefinition::id).toList());
            }

            if (List.of("apply", "remove").contains(sub)) {
                return TabCompleteUtil.filter(args[2],
                        conditionManager.getAll().stream().map(ConditionDefinition::id).toList());
            }
        }

        return List.of();
    }

}
