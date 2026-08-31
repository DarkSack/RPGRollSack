package com.sack.rpgroll.chat.command;

import com.sack.rpgroll.common.command.Senders;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.chat.emote.EmoteDefinition;
import com.sack.rpgroll.chat.emote.EmoteManager;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.util.TabCompleteUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

/** /emote &lt;nombre&gt; [jugador] — spec: /wave, /laugh, /sit, /cry, /dance. */
public class EmoteCommand implements CommandExecutor, TabCompleter {

    private final EmoteManager emoteManager;
    private final LangManager lang;

    public EmoteCommand(EmoteManager emoteManager, LangManager lang) {
        this.emoteManager = emoteManager;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(Senders.asPlayer(sender) instanceof Player player)) {
            lang.send(sender, "common.players_only");
            return true;
        }

        String emoteId = label.equalsIgnoreCase("emote")
                ? (args.length > 0 ? args[0] : null)
                : label;

        if (emoteId == null) {
            lang.send(player, "emote.usage");
            return true;
        }

        EmoteDefinition emote = emoteManager.get(emoteId).orElse(null);

        if (emote == null) {
            lang.send(player, "emote.not_found");
            return true;
        }

        String targetName = label.equalsIgnoreCase("emote") && args.length > 1 ? args[1]
                : !label.equalsIgnoreCase("emote") && args.length > 0 ? args[0] : null;

        Player target = targetName != null ? Bukkit.getPlayerExact(targetName) : null;

        String template = target != null && emote.targetTemplate() != null && !emote.targetTemplate().isBlank()
                ? emote.targetTemplate()
                : emote.template();

        String text = template.replace("{player}", player.getName())
                .replace("{target}", target != null ? target.getName() : "");

        Component message = ComponentUtils.parse(text);

        for (Player recipient : recipients(player, emote.radius())) {
            recipient.sendMessage(message);
        }

        return true;
    }

    private Iterable<? extends Player> recipients(Player emitter, double radius) {

        if (radius <= 0) {
            return emitter.getWorld().getPlayers();
        }

        return emitter.getWorld().getPlayers().stream()
                .filter(p -> p.getLocation().distance(emitter.getLocation()) <= radius)
                .toList();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        boolean generic = alias.equalsIgnoreCase("emote");

        if (generic && args.length == 1) {
            return TabCompleteUtil.filter(args[0], emoteManager.getAll().stream()
                    .map(EmoteDefinition::id).toList());
        }

        if (generic && args.length == 2) {
            return TabCompleteUtil.onlinePlayerNames(args[1]);
        }

        if (!generic && args.length == 1) {
            return TabCompleteUtil.onlinePlayerNames(args[0]);
        }

        return List.of();
    }

}
