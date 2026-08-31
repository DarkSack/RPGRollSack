package com.sack.rpgroll.effects.command;

import com.sack.rpgroll.common.command.Senders;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.effects.api.EffectsAPI;
import com.sack.rpgroll.effects.core.EffectManager;
import com.sack.rpgroll.effects.gui.ChatPromptManager;
import com.sack.rpgroll.effects.gui.EffectBrowserGUI;
import com.sack.rpgroll.effects.runtime.ActiveEffect;
import com.sack.rpgroll.effects.runtime.EffectTracker;
import com.sack.rpgroll.util.TabCompleteUtil;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * /rpgeffects browser
 * /rpgeffects reload
 * /rpgeffects apply <id> <jugador>
 * /rpgeffects remove <id> <jugador>
 * /rpgeffects list [jugador]
 */
public class EffectsAdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("browser", "reload", "apply", "remove", "list");

    private final EffectManager effectManager;
    private final EffectTracker tracker;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;
    private final Plugin plugin;

    public EffectsAdminCommand(EffectManager effectManager, EffectTracker tracker, ChatPromptManager chatPromptManager,
            LangManager lang, Plugin plugin) {
        this.effectManager = effectManager;
        this.tracker = tracker;
        this.chatPromptManager = chatPromptManager;
        this.lang = lang;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("rpgrolleffects.admin.*")) {
            lang.send(sender, "general.no_permission");
            return true;
        }

        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "browser" -> handleBrowser(sender);
            case "reload" -> handleReload(sender);
            case "apply" -> handleApply(sender, args);
            case "remove" -> handleRemove(sender, args);
            case "list" -> handleList(sender, args);
            default -> sendUsage(sender);
        }

        return true;
    }

    private void sendUsage(CommandSender sender) {
        lang.send(sender, "command.usage");
    }

    private void handleBrowser(CommandSender sender) {

        if (!(Senders.asPlayer(sender) instanceof Player player)) {
            lang.send(sender, "command.browser_player_only");
            return;
        }

        new EffectBrowserGUI(player, effectManager, tracker, chatPromptManager).open();
    }

    private void handleReload(CommandSender sender) {
        effectManager.reload();
        plugin.reloadConfig();
        lang.reload(plugin.getConfig().getString("language", "es"));
        lang.send(sender, "command.reload_success", "count", effectManager.count());
    }

    private void handleApply(CommandSender sender, String[] args) {

        if (args.length < 3) {
            lang.send(sender, "command.apply_usage");
            return;
        }

        String effectId = args[1];

        if (!effectManager.exists(effectId)) {
            lang.send(sender, "command.effect_not_found", "id", effectId);
            return;
        }

        Player target = Bukkit.getPlayerExact(args[2]);

        if (target == null) {
            lang.send(sender, "command.player_not_found", "name", args[2]);
            return;
        }

        if (!EffectsAPI.isReady()) {
            lang.send(sender, "command.not_ready");
            return;
        }

        var reasons = EffectsAPI.get().checkConditions(effectId, target);

        if (!reasons.isEmpty()) {
            lang.send(sender, "command.apply_blocked_header", "id", effectId);
            reasons.forEach(reason -> lang.send(sender, "command.apply_blocked_reason", "reason", reason));
            return;
        }

        EffectsAPI.get().apply(effectId, target);
        lang.send(sender, "command.apply_success", "id", effectId, "player", target.getName());
    }

    private void handleRemove(CommandSender sender, String[] args) {

        if (args.length < 3) {
            lang.send(sender, "command.remove_usage");
            return;
        }

        Player target = Bukkit.getPlayerExact(args[2]);

        if (target == null) {
            lang.send(sender, "command.player_not_found", "name", args[2]);
            return;
        }

        boolean removed = tracker.remove(target, args[1], com.sack.rpgroll.effects.runtime.RemovalReason.MANUAL);

        if (removed) {
            lang.send(sender, "command.remove_success", "id", args[1], "player", target.getName());
        } else {
            lang.send(sender, "command.remove_not_active", "player", target.getName());
        }
    }

    private void handleList(CommandSender sender, String[] args) {

        Player target;

        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                lang.send(sender, "command.player_not_found", "name", args[1]);
                return;
            }
        } else if (Senders.asPlayer(sender) instanceof Player senderPlayer) {
            target = senderPlayer;
        } else {
            lang.send(sender, "command.list_console_no_target");
            return;
        }

        var active = tracker.getActive(target);

        if (active.isEmpty()) {
            lang.send(sender, "command.list_no_active", "player", target.getName());
            return;
        }

        lang.send(sender, "command.list_header", "player", target.getName());

        for (ActiveEffect activeEffect : active) {

            String remaining = activeEffect.isPermanent() ? lang.raw("command.remaining_permanent")
                    : lang.raw("command.remaining_ticks", "ticks", activeEffect.remainingTicks());

            lang.send(sender, "command.list_entry", "id", activeEffect.definition().id(), "stacks",
                    activeEffect.stacks(), "remaining", remaining);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
        }

        String sub = args[0].toLowerCase();

        if (args.length == 2) {
            return switch (sub) {
                case "apply", "remove" -> TabCompleteUtil.filter(args[1], effectManager.getAll().stream()
                        .map(effect -> effect.id()).toList());
                case "list" -> TabCompleteUtil.onlinePlayerNames(args[1]);
                default -> List.of();
            };
        }

        if (args.length == 3 && List.of("apply", "remove").contains(sub)) {
            return TabCompleteUtil.onlinePlayerNames(args[2]);
        }

        return List.of();
    }

}
