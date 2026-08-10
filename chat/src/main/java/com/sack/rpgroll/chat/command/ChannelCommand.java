package com.sack.rpgroll.chat.command;

import com.sack.rpgroll.chat.channel.ChannelManager;
import com.sack.rpgroll.chat.channel.ChatChannel;
import com.sack.rpgroll.chat.player.PlayerChannelState;
import com.sack.rpgroll.chat.player.PlayerChannelStateManager;
import com.sack.rpgroll.util.ComponentUtils;
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

/** /channel join|leave|list|switch|info &lt;canal&gt; */
public class ChannelCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("join", "leave", "switch", "info", "list");

    private final ChannelManager channelManager;
    private final PlayerChannelStateManager stateManager;

    public ChannelCommand(ChannelManager channelManager, PlayerChannelStateManager stateManager) {
        this.channelManager = channelManager;
        this.stateManager = stateManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo un jugador puede usar este comando.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            handleList(player);
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "join" -> handleJoin(player, args);
            case "leave" -> handleLeave(player, args);
            case "switch" -> handleSwitch(player, args);
            case "info" -> handleInfo(player, args);
            default -> handleList(player);
        }

        return true;
    }

    private void handleList(Player player) {

        PlayerChannelState state = stateManager.getOrLoad(player);

        player.sendMessage(Component.text("=== Canales ===", NamedTextColor.GOLD));

        for (ChatChannel channel : channelManager.sortedByPriority()) {

            if (channel.requiresViewPermission() && !player.hasPermission(channel.viewPermission())) {
                continue;
            }

            boolean joined = state.hasJoined(channel.id());
            boolean active = channel.id().equalsIgnoreCase(state.activeChannelId());

            player.sendMessage(Component.text(" - ", NamedTextColor.GRAY)
                    .append(ComponentUtils.parse(channel.displayName()))
                    .append(Component.text(" (" + channel.id() + ") "))
                    .append(active ? Component.text("[activo]", NamedTextColor.GREEN)
                            : joined ? Component.text("[unido]", NamedTextColor.GRAY)
                            : Component.text("[no unido]", NamedTextColor.DARK_GRAY)));
        }
    }

    private void handleJoin(Player player, String[] args) {

        if (args.length < 2) {
            player.sendMessage(Component.text("Uso: /channel join <canal>", NamedTextColor.YELLOW));
            return;
        }

        ChatChannel channel = channelManager.get(args[1]).orElse(null);

        if (channel == null) {
            player.sendMessage(Component.text("No existe ese canal.", NamedTextColor.RED));
            return;
        }

        if (channel.requiresViewPermission() && !player.hasPermission(channel.viewPermission())) {
            player.sendMessage(Component.text("No tenés permiso para ver ese canal.", NamedTextColor.RED));
            return;
        }

        stateManager.getOrLoad(player).join(channel.id());
        stateManager.save(player.getUniqueId());
        player.sendMessage(Component.text("✔ Te uniste a ", NamedTextColor.GREEN)
                .append(ComponentUtils.parse(channel.displayName()))
                .append(Component.text(".", NamedTextColor.GREEN)));
    }

    private void handleLeave(Player player, String[] args) {

        if (args.length < 2) {
            player.sendMessage(Component.text("Uso: /channel leave <canal>", NamedTextColor.YELLOW));
            return;
        }

        PlayerChannelState state = stateManager.getOrLoad(player);
        state.leave(args[1]);

        if (args[1].equalsIgnoreCase(state.activeChannelId())) {
            state.setActiveChannelId(null);
        }

        stateManager.save(player.getUniqueId());
        player.sendMessage(Component.text("Saliste del canal.", NamedTextColor.GRAY));
    }

    private void handleSwitch(Player player, String[] args) {

        if (args.length < 2) {
            player.sendMessage(Component.text("Uso: /channel switch <canal>", NamedTextColor.YELLOW));
            return;
        }

        ChatChannel channel = channelManager.get(args[1]).orElse(null);

        if (channel == null) {
            player.sendMessage(Component.text("No existe ese canal.", NamedTextColor.RED));
            return;
        }

        if (channel.requiresViewPermission() && !player.hasPermission(channel.viewPermission())) {
            player.sendMessage(Component.text("No tenés permiso para ver ese canal.", NamedTextColor.RED));
            return;
        }

        PlayerChannelState state = stateManager.getOrLoad(player);
        state.join(channel.id());
        state.setActiveChannelId(channel.id());
        stateManager.save(player.getUniqueId());

        player.sendMessage(Component.text("✔ Ahora hablás en ", NamedTextColor.GREEN)
                .append(ComponentUtils.parse(channel.displayName()))
                .append(Component.text(".", NamedTextColor.GREEN)));
    }

    private void handleInfo(Player player, String[] args) {

        if (args.length < 2) {
            player.sendMessage(Component.text("Uso: /channel info <canal>", NamedTextColor.YELLOW));
            return;
        }

        ChatChannel channel = channelManager.get(args[1]).orElse(null);

        if (channel == null) {
            player.sendMessage(Component.text("No existe ese canal.", NamedTextColor.RED));
            return;
        }

        player.sendMessage(Component.text("=== ", NamedTextColor.GOLD)
                .append(ComponentUtils.parse(channel.displayName()))
                .append(Component.text(" ===", NamedTextColor.GOLD)));
        player.sendMessage(Component.text("Alcance: " + channel.scope() + " · Prioridad: " + channel.priority(),
                NamedTextColor.GRAY));
        player.sendMessage(Component.text("Cooldown: " + channel.cooldownMillis() + "ms", NamedTextColor.GRAY));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
        }

        if (args.length == 2 && List.of("join", "leave", "switch", "info").contains(args[0].toLowerCase(Locale.ROOT))) {
            List<String> ids = channelManager.sortedByPriority().stream().map(ChatChannel::id).toList();
            return TabCompleteUtil.filter(args[1], ids);
        }

        return List.of();
    }

}
