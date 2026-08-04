package com.sack.rpgroll.sackeffects.command;

import com.sack.rpgroll.sackeffects.core.EffectDefinition;
import com.sack.rpgroll.sackeffects.core.EffectManager;
import com.sack.rpgroll.sackeffects.engine.EffectContext;
import com.sack.rpgroll.sackeffects.engine.EffectEngine;
import com.sack.rpgroll.sackeffects.gui.ChatPromptManager;
import com.sack.rpgroll.sackeffects.gui.EffectBrowserGUI;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * /sackeffects browser
 * /sackeffects reload
 * /sackeffects test <id> [jugador]
 */
public class SackEffectsAdminCommand implements CommandExecutor {

    private final EffectManager effectManager;
    private final EffectEngine engine;
    private final ChatPromptManager chatPromptManager;

    public SackEffectsAdminCommand(EffectManager effectManager, EffectEngine engine, ChatPromptManager chatPromptManager) {
        this.effectManager = effectManager;
        this.engine = engine;
        this.chatPromptManager = chatPromptManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("sackeffects.admin.*")) {
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
            case "test" -> handleTest(sender, args);
            default -> sendUsage(sender);
        }

        return true;
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text(
                "Uso: /sackeffects <browser|reload|test <id> [jugador]>", NamedTextColor.RED));
    }

    private void handleBrowser(CommandSender sender) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo jugadores pueden abrir el navegador de efectos.",
                    NamedTextColor.RED));
            return;
        }

        new EffectBrowserGUI(player, effectManager, engine, chatPromptManager).open();
    }

    private void handleReload(CommandSender sender) {
        effectManager.reload();
        sender.sendMessage(Component.text("✔ Recargado: " + effectManager.count() + " efecto(s).",
                NamedTextColor.GREEN));
    }

    private void handleTest(CommandSender sender, String[] args) {

        if (args.length < 2) {
            sender.sendMessage(Component.text("Uso: /sackeffects test <id> [jugador]", NamedTextColor.RED));
            return;
        }

        String effectId = args[1];
        Optional<EffectDefinition> effectOpt = effectManager.get(effectId);

        if (effectOpt.isEmpty()) {
            sender.sendMessage(Component.text("No existe un efecto con id: " + effectId, NamedTextColor.RED));
            return;
        }

        Player target;

        if (args.length >= 3) {
            target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage(Component.text("Jugador no encontrado: " + args[2], NamedTextColor.RED));
                return;
            }
        } else if (sender instanceof Player senderPlayer) {
            target = senderPlayer;
        } else {
            sender.sendMessage(Component.text("Especificá un jugador: /sackeffects test <id> <jugador>",
                    NamedTextColor.RED));
            return;
        }

        engine.play(effectOpt.get(), EffectContext.of(target));
        sender.sendMessage(Component.text("▶ Reproduciendo '" + effectId + "' en " + target.getName() + ".",
                NamedTextColor.GREEN));
    }

}
