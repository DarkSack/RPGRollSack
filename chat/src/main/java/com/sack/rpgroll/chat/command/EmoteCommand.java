package com.sack.rpgroll.chat.command;

import com.sack.rpgroll.chat.emote.EmoteDefinition;
import com.sack.rpgroll.chat.emote.EmoteManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/** /emote &lt;nombre&gt; [jugador] — spec: /wave, /laugh, /sit, /cry, /dance. */
public class EmoteCommand implements CommandExecutor {

    private final EmoteManager emoteManager;

    public EmoteCommand(EmoteManager emoteManager) {
        this.emoteManager = emoteManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo un jugador puede usar este comando.", NamedTextColor.RED));
            return true;
        }

        String emoteId = label.equalsIgnoreCase("emote")
                ? (args.length > 0 ? args[0] : null)
                : label;

        if (emoteId == null) {
            player.sendMessage(Component.text("Uso: /emote <nombre> [jugador]", NamedTextColor.YELLOW));
            return true;
        }

        EmoteDefinition emote = emoteManager.get(emoteId).orElse(null);

        if (emote == null) {
            player.sendMessage(Component.text("No existe esa emote.", NamedTextColor.RED));
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

        Component message = LegacyComponentSerializer.legacyAmpersand().deserialize(text);

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

}
