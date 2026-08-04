package com.sack.rpgroll.effects.command;

import com.sack.rpgroll.effects.api.EffectsAPI;
import com.sack.rpgroll.effects.core.EffectManager;
import com.sack.rpgroll.effects.gui.ChatPromptManager;
import com.sack.rpgroll.effects.gui.EffectBrowserGUI;
import com.sack.rpgroll.effects.runtime.ActiveEffect;
import com.sack.rpgroll.effects.runtime.EffectTracker;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * /rpgeffects browser
 * /rpgeffects reload
 * /rpgeffects apply <id> <jugador>
 * /rpgeffects remove <id> <jugador>
 * /rpgeffects list [jugador]
 */
public class EffectsAdminCommand implements CommandExecutor {

    private final EffectManager effectManager;
    private final EffectTracker tracker;
    private final ChatPromptManager chatPromptManager;

    public EffectsAdminCommand(EffectManager effectManager, EffectTracker tracker, ChatPromptManager chatPromptManager) {
        this.effectManager = effectManager;
        this.tracker = tracker;
        this.chatPromptManager = chatPromptManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("rpgrolleffects.admin.*")) {
            sender.sendMessage(Component.text("No tenés permiso para usar este comando.", NamedTextColor.RED));
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
        sender.sendMessage(Component.text(
                "Uso: /rpgeffects <browser|reload|apply <id> <jugador>|remove <id> <jugador>|list [jugador]>",
                NamedTextColor.RED));
    }

    private void handleBrowser(CommandSender sender) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo jugadores pueden abrir el Effect Studio.", NamedTextColor.RED));
            return;
        }

        new EffectBrowserGUI(player, effectManager, tracker, chatPromptManager).open();
    }

    private void handleReload(CommandSender sender) {
        effectManager.reload();
        sender.sendMessage(Component.text("✔ Recargado: " + effectManager.count() + " efecto(s).",
                NamedTextColor.GREEN));
    }

    private void handleApply(CommandSender sender, String[] args) {

        if (args.length < 3) {
            sender.sendMessage(Component.text("Uso: /rpgeffects apply <id> <jugador>", NamedTextColor.RED));
            return;
        }

        String effectId = args[1];

        if (!effectManager.exists(effectId)) {
            sender.sendMessage(Component.text("No existe un efecto con id: " + effectId, NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[2]);

        if (target == null) {
            sender.sendMessage(Component.text("Jugador no encontrado: " + args[2], NamedTextColor.RED));
            return;
        }

        if (!EffectsAPI.isReady()) {
            sender.sendMessage(Component.text("RPGRoll-Effects todavía no está listo.", NamedTextColor.RED));
            return;
        }

        var reasons = EffectsAPI.get().checkConditions(effectId, target);

        if (!reasons.isEmpty()) {
            sender.sendMessage(Component.text("✘ No se pudo aplicar '" + effectId + "':", NamedTextColor.RED));
            reasons.forEach(reason -> sender.sendMessage(Component.text("  - " + reason, NamedTextColor.RED)));
            return;
        }

        EffectsAPI.get().apply(effectId, target);
        sender.sendMessage(Component.text("✔ Aplicado '" + effectId + "' a " + target.getName() + ".",
                NamedTextColor.GREEN));
    }

    private void handleRemove(CommandSender sender, String[] args) {

        if (args.length < 3) {
            sender.sendMessage(Component.text("Uso: /rpgeffects remove <id> <jugador>", NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[2]);

        if (target == null) {
            sender.sendMessage(Component.text("Jugador no encontrado: " + args[2], NamedTextColor.RED));
            return;
        }

        boolean removed = tracker.remove(target, args[1], com.sack.rpgroll.effects.runtime.RemovalReason.MANUAL);

        sender.sendMessage(removed
                ? Component.text("✔ Removido '" + args[1] + "' de " + target.getName() + ".", NamedTextColor.GREEN)
                : Component.text(target.getName() + " no tiene ese efecto activo.", NamedTextColor.GRAY));
    }

    private void handleList(CommandSender sender, String[] args) {

        Player target;

        if (args.length >= 2) {
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage(Component.text("Jugador no encontrado: " + args[1], NamedTextColor.RED));
                return;
            }
        } else if (sender instanceof Player senderPlayer) {
            target = senderPlayer;
        } else {
            sender.sendMessage(Component.text("Especificá un jugador: /rpgeffects list <jugador>", NamedTextColor.RED));
            return;
        }

        var active = tracker.getActive(target);

        if (active.isEmpty()) {
            sender.sendMessage(Component.text(target.getName() + " no tiene efectos activos.", NamedTextColor.GRAY));
            return;
        }

        sender.sendMessage(Component.text("Efectos activos en " + target.getName() + ":", NamedTextColor.GOLD));

        for (ActiveEffect activeEffect : active) {
            sender.sendMessage(Component.text("• " + activeEffect.definition().id() + " (stacks: "
                    + activeEffect.stacks() + ", restante: "
                    + (activeEffect.isPermanent() ? "permanente" : activeEffect.remainingTicks() + " ticks") + ")",
                    NamedTextColor.WHITE));
        }
    }

}
