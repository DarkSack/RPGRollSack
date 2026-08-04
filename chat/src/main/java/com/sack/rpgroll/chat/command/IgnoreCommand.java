package com.sack.rpgroll.chat.command;

import com.sack.rpgroll.chat.ignore.IgnoreManager;
import com.sack.rpgroll.chat.ignore.PlayerIgnoreState;
import com.sack.rpgroll.guilds.GuildsAPI;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.UUID;

/** /ignore &lt;player|guild|channel&gt; &lt;add|remove|list&gt; [nombre] */
public class IgnoreCommand implements CommandExecutor {

    private final IgnoreManager ignoreManager;

    public IgnoreCommand(IgnoreManager ignoreManager) {
        this.ignoreManager = ignoreManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo un jugador puede usar este comando.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 2) {
            sendUsage(player);
            return true;
        }

        String type = args[0].toLowerCase(Locale.ROOT);
        String action = args[1].toLowerCase(Locale.ROOT);
        PlayerIgnoreState state = ignoreManager.getOrLoad(player);

        if (action.equals("list")) {
            listIgnored(player, state, type);
            return true;
        }

        if (args.length < 3) {
            sendUsage(player);
            return true;
        }

        if (!action.equals("add") && !action.equals("remove")) {
            sendUsage(player);
            return true;
        }

        boolean wantAdd = action.equals("add");
        String name = args[2];

        switch (type) {
            case "player" -> togglePlayer(player, state, name, wantAdd);
            case "guild" -> {
                applyToggle(wantAdd, state.isIgnoringGuild(name), () -> state.toggleGuild(name));
                player.sendMessage(Component.text((wantAdd ? "✔ Ahora ignorás" : "Dejaste de ignorar")
                        + " a la guild " + name + ".", NamedTextColor.GRAY));
            }
            case "channel" -> {
                applyToggle(wantAdd, state.isIgnoringChannel(name), () -> state.toggleChannel(name));
                player.sendMessage(Component.text((wantAdd ? "✔ Ahora ignorás" : "Dejaste de ignorar")
                        + " el canal " + name + ".", NamedTextColor.GRAY));
            }
            default -> sendUsage(player);
        }

        ignoreManager.save(player.getUniqueId());
        return true;
    }

    /** Los métodos de {@link PlayerIgnoreState} son toggles — esto los hace idempotentes respecto a add/remove. */
    private void applyToggle(boolean wantAdd, boolean currentlyIgnoring, Runnable toggle) {
        if (wantAdd != currentlyIgnoring) {
            toggle.run();
        }
    }

    private void togglePlayer(Player player, PlayerIgnoreState state, String name, boolean wantAdd) {

        Player target = Bukkit.getPlayerExact(name);
        UUID targetId = target != null ? target.getUniqueId() : Bukkit.getOfflinePlayer(name).getUniqueId();

        applyToggle(wantAdd, state.isIgnoringPlayer(targetId), () -> state.togglePlayer(targetId));
        player.sendMessage(Component.text((wantAdd ? "✔ Ahora ignorás" : "Dejaste de ignorar") + " a " + name + ".",
                NamedTextColor.GRAY));
    }

    private void listIgnored(Player player, PlayerIgnoreState state, String type) {

        player.sendMessage(Component.text("=== Ignorados (" + type + ") ===", NamedTextColor.GOLD));

        switch (type) {
            case "player" -> state.ignoredPlayers().forEach(uuid -> {
                String name = Bukkit.getOfflinePlayer(uuid).getName();
                player.sendMessage(Component.text(" - " + (name != null ? name : uuid), NamedTextColor.GRAY));
            });
            case "guild" -> state.ignoredGuilds().forEach(id -> {
                String name = GuildsAPI.isReady()
                        ? GuildsAPI.getGuildManager().get(id).map(g -> g.name()).orElse(id)
                        : id;
                player.sendMessage(Component.text(" - " + name, NamedTextColor.GRAY));
            });
            case "channel" -> state.ignoredChannels().forEach(id ->
                    player.sendMessage(Component.text(" - " + id, NamedTextColor.GRAY)));
            default -> sendUsage(player);
        }
    }

    private void sendUsage(Player player) {
        player.sendMessage(Component.text("Uso: /ignore <player|guild|channel> <add|remove|list> [nombre]",
                NamedTextColor.YELLOW));
    }

}
