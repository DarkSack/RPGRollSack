package com.sack.rpgroll.extras.command;

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

    public ExtrasAdminCommand(StatManager statManager, StatEngine statEngine, ConditionManager conditionManager,
            ConditionRuntime conditionRuntime, Runnable reloadCallback) {
        this.statManager = statManager;
        this.statEngine = statEngine;
        this.conditionManager = conditionManager;
        this.conditionRuntime = conditionRuntime;
        this.reloadCallback = reloadCallback;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission(PERMISSION)) {
            sender.sendMessage(Component.text("No tenés permiso para usar este comando.", NamedTextColor.RED));
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
        sender.sendMessage(Component.text(
                "Uso: /extrasadmin <reload|list|get|set|add|apply|remove> [args]", NamedTextColor.RED));
    }

    private void handleReload(CommandSender sender) {
        reloadCallback.run();
        sender.sendMessage(Component.text(
                "✔ RPGRoll-Extras recargado: " + statManager.count() + " stat(s), "
                        + conditionManager.count() + " condition(s).",
                NamedTextColor.GREEN));
    }

    private void handleList(CommandSender sender) {

        sender.sendMessage(Component.text("Stats:", NamedTextColor.GOLD));
        for (StatDefinition stat : statManager.getAll()) {
            sender.sendMessage(Component.text("• " + stat.id() + " (max " + stat.max() + ")", NamedTextColor.WHITE));
        }

        sender.sendMessage(Component.text("Conditions:", NamedTextColor.GOLD));
        for (ConditionDefinition condition : conditionManager.getAll()) {
            sender.sendMessage(Component.text("• " + condition.id(), NamedTextColor.WHITE));
        }
    }

    private void handleGet(CommandSender sender, String[] args) {

        if (args.length < 3) {
            sender.sendMessage(Component.text("Uso: /extrasadmin get <jugador> <stat>", NamedTextColor.RED));
            return;
        }

        Player target = requireTarget(sender, args[1]);
        if (target == null) {
            return;
        }

        double value = statEngine.get(target, args[2]);
        sender.sendMessage(Component.text(
                target.getName() + " → " + args[2] + ": " + value, NamedTextColor.GREEN));
    }

    private void handleSet(CommandSender sender, String[] args) {

        if (args.length < 4) {
            sender.sendMessage(Component.text("Uso: /extrasadmin set <jugador> <stat> <valor>", NamedTextColor.RED));
            return;
        }

        Player target = requireTarget(sender, args[1]);
        if (target == null) {
            return;
        }

        double value = parseDouble(sender, args[3]);
        if (Double.isNaN(value)) {
            return;
        }

        statEngine.set(target, args[2], value);
        sender.sendMessage(Component.text(
                "✔ " + args[2] + " de " + target.getName() + " establecido en " + value, NamedTextColor.GREEN));
    }

    private void handleAdd(CommandSender sender, String[] args) {

        if (args.length < 4) {
            sender.sendMessage(Component.text("Uso: /extrasadmin add <jugador> <stat> <cantidad>", NamedTextColor.RED));
            return;
        }

        Player target = requireTarget(sender, args[1]);
        if (target == null) {
            return;
        }

        double amount = parseDouble(sender, args[3]);
        if (Double.isNaN(amount)) {
            return;
        }

        statEngine.adjust(target, args[2], amount);
        sender.sendMessage(Component.text(
                "✔ " + args[2] + " de " + target.getName() + " ajustado en " + amount, NamedTextColor.GREEN));
    }

    private void handleApply(CommandSender sender, String[] args) {

        if (args.length < 3) {
            sender.sendMessage(Component.text("Uso: /extrasadmin apply <jugador> <condition>", NamedTextColor.RED));
            return;
        }

        Player target = requireTarget(sender, args[1]);
        if (target == null) {
            return;
        }

        var definition = conditionManager.get(args[2]);
        if (definition.isEmpty()) {
            sender.sendMessage(Component.text("No existe una condition con id: " + args[2], NamedTextColor.RED));
            return;
        }

        conditionRuntime.apply(target, definition.get());
        sender.sendMessage(Component.text(
                "✔ Condition '" + args[2] + "' aplicada a " + target.getName(), NamedTextColor.GREEN));
    }

    private void handleRemove(CommandSender sender, String[] args) {

        if (args.length < 3) {
            sender.sendMessage(Component.text("Uso: /extrasadmin remove <jugador> <condition>", NamedTextColor.RED));
            return;
        }

        Player target = requireTarget(sender, args[1]);
        if (target == null) {
            return;
        }

        conditionRuntime.remove(target, args[2]);
        sender.sendMessage(Component.text(
                "✔ Condition '" + args[2] + "' removida de " + target.getName(), NamedTextColor.GREEN));
    }

    private Player requireTarget(CommandSender sender, String name) {

        Player target = Bukkit.getPlayerExact(name);

        if (target == null) {
            sender.sendMessage(Component.text("Jugador no encontrado: " + name, NamedTextColor.RED));
        }

        return target;
    }

    private double parseDouble(CommandSender sender, String raw) {

        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Valor numérico inválido: " + raw, NamedTextColor.RED));
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
