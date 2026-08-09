package com.sack.rpgroll.magic.command;

import com.sack.rpgroll.magic.core.RuneManager;
import com.sack.rpgroll.magic.core.Spell;
import com.sack.rpgroll.magic.core.SpellManager;
import com.sack.rpgroll.magic.engine.CastResult;
import com.sack.rpgroll.magic.engine.SpellCastEngine;
import com.sack.rpgroll.magic.gui.ChatPromptManager;
import com.sack.rpgroll.magic.gui.SpellbookGUI;
import com.sack.rpgroll.magic.runtime.PlayerSpellbook;
import com.sack.rpgroll.magic.runtime.SpellbookManager;
import com.sack.rpgroll.util.TabCompleteUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/**
 * /magic spellbook
 * /magic select <id>
 * /magic cast <id>
 * /magic cooldowns
 */
public class MagicCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("spellbook", "select", "cast", "cooldowns");

    private final SpellManager spellManager;
    private final RuneManager runeManager;
    private final SpellbookManager spellbookManager;
    private final SpellCastEngine engine;
    private final ChatPromptManager chatPromptManager;

    public MagicCommand(SpellManager spellManager, RuneManager runeManager, SpellbookManager spellbookManager,
            SpellCastEngine engine, ChatPromptManager chatPromptManager) {
        this.spellManager = spellManager;
        this.runeManager = runeManager;
        this.spellbookManager = spellbookManager;
        this.engine = engine;
        this.chatPromptManager = chatPromptManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo jugadores pueden usar este comando.");
            return true;
        }

        if (args.length < 1) {
            sendUsage(player);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "spellbook" -> new SpellbookGUI(player, spellbookManager, spellManager, runeManager,
                    chatPromptManager).open();
            case "select" -> handleSelect(player, args);
            case "cast" -> handleCast(player, args);
            case "cooldowns" -> handleCooldowns(player);
            default -> sendUsage(player);
        }

        return true;
    }

    private void sendUsage(Player player) {
        player.sendMessage(Component.text(
                "Uso: /magic <spellbook|select <id>|cast <id>|cooldowns>", NamedTextColor.RED));
    }

    private void handleSelect(Player player, String[] args) {

        if (args.length < 2) {
            player.sendMessage(Component.text("Uso: /magic select <id>", NamedTextColor.RED));
            return;
        }

        PlayerSpellbook spellbook = spellbookManager.getOrLoad(player);

        if (!spellbook.select(args[1])) {
            player.sendMessage(Component.text("No sabés ese hechizo todavía.", NamedTextColor.RED));
            return;
        }

        player.sendMessage(Component.text("✔ Hechizo seleccionado: " + args[1], NamedTextColor.GREEN));
    }

    private void handleCast(Player player, String[] args) {

        if (args.length < 2) {
            player.sendMessage(Component.text("Uso: /magic cast <id>", NamedTextColor.RED));
            return;
        }

        var spellOpt = spellManager.get(args[1]);

        if (spellOpt.isEmpty()) {
            player.sendMessage(Component.text("No existe un hechizo con id: " + args[1], NamedTextColor.RED));
            return;
        }

        PlayerSpellbook spellbook = spellbookManager.getOrLoad(player);
        CastResult result = engine.cast(spellOpt.get(), player, spellbook, null);

        if (!result.success()) {
            result.reasons().forEach(reason -> player.sendMessage(Component.text("✘ " + reason, NamedTextColor.RED)));
        }
    }

    private void handleCooldowns(Player player) {

        PlayerSpellbook spellbook = spellbookManager.getOrLoad(player);
        long now = System.currentTimeMillis();

        var onCooldown = spellbook.allCooldowns().entrySet().stream()
                .filter(entry -> entry.getValue() > now)
                .toList();

        if (onCooldown.isEmpty()) {
            player.sendMessage(Component.text("No tenés ningún hechizo en cooldown.", NamedTextColor.GRAY));
            return;
        }

        player.sendMessage(Component.text("Cooldowns activos:", NamedTextColor.GOLD));

        for (var entry : onCooldown) {

            String spellId = entry.getKey();
            String displayName = spellManager.get(spellId).map(Spell::displayName).orElse(spellId);
            double secondsLeft = (entry.getValue() - now) / 1000.0;

            player.sendMessage(Component.text(
                    String.format(Locale.ROOT, "• %s: %.1fs", displayName, secondsLeft), NamedTextColor.WHITE));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
        }

        if (args.length == 2 && List.of("select", "cast").contains(args[0].toLowerCase(Locale.ROOT))
                && sender instanceof Player player) {
            return TabCompleteUtil.filter(args[1], spellbookManager.getOrLoad(player).allLearned());
        }

        return List.of();
    }

}
