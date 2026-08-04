package com.sack.rpgroll.guilds.guild.chat;

import com.sack.rpgroll.guilds.guild.Guild;
import com.sack.rpgroll.guilds.guild.GuildManager;
import com.sack.rpgroll.guilds.guild.diplomacy.GuildDiplomacyManager;

import io.papermc.paper.event.player.AsyncChatEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Alterna el chat de un jugador entre Guild/Oficiales/Alianza vía /guild chat &lt;canal&gt;. */
public class GuildChatListener implements Listener {

    private final GuildManager guildManager;
    private final GuildDiplomacyManager diplomacyManager;
    private final Map<UUID, GuildChatChannel> activeChannel = new ConcurrentHashMap<>();

    public GuildChatListener(GuildManager guildManager, GuildDiplomacyManager diplomacyManager) {
        this.guildManager = guildManager;
        this.diplomacyManager = diplomacyManager;
    }

    public void setChannel(Player player, GuildChatChannel channel) {
        activeChannel.put(player.getUniqueId(), channel);
    }

    public void disable(UUID playerId) {
        activeChannel.remove(playerId);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onChat(AsyncChatEvent event) {

        Player player = event.getPlayer();
        GuildChatChannel channel = activeChannel.get(player.getUniqueId());

        if (channel == null) {
            return;
        }

        Guild guild = guildManager.findByMember(player.getUniqueId()).orElse(null);

        if (guild == null) {
            activeChannel.remove(player.getUniqueId());
            return;
        }

        event.setCancelled(true);

        String message = PlainTextComponentSerializer.plainText().serialize(event.message());
        Component formatted = Component.text("[" + channel.tag() + "] ", channelColor(channel))
                .append(Component.text(guild.name() + " ", NamedTextColor.DARK_GRAY))
                .append(Component.text(player.getName() + ": ", NamedTextColor.GRAY))
                .append(Component.text(message, NamedTextColor.WHITE));

        for (UUID memberId : recipients(guild, channel)) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null) {
                member.sendMessage(formatted);
            }
        }
    }

    private java.util.Set<UUID> recipients(Guild guild, GuildChatChannel channel) {

        java.util.Set<UUID> recipients = new java.util.LinkedHashSet<>();

        switch (channel) {
            case GUILD -> recipients.addAll(guild.members().keySet());
            case OFFICERS -> guild.members().forEach((uuid, role) -> {
                if (role.canManageSettings()) {
                    recipients.add(uuid);
                }
            });
            case ALLIANCE -> {
                recipients.addAll(guild.members().keySet());
                for (String allyId : diplomacyManager.alliesOf(guild.id())) {
                    guildManager.get(allyId).ifPresent(ally -> recipients.addAll(ally.members().keySet()));
                }
            }
        }

        return recipients;
    }

    private NamedTextColor channelColor(GuildChatChannel channel) {
        return switch (channel) {
            case GUILD -> NamedTextColor.GREEN;
            case OFFICERS -> NamedTextColor.GOLD;
            case ALLIANCE -> NamedTextColor.LIGHT_PURPLE;
        };
    }

}
