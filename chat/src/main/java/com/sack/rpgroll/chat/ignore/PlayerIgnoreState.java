package com.sack.rpgroll.chat.ignore;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** Qué ignora un jugador — spec: "Ignorar usuarios / Ignorar Guilds / Ignorar Canales". */
public class PlayerIgnoreState {

    private final UUID uuid;
    private final Set<UUID> ignoredPlayers = new LinkedHashSet<>();
    private final Set<String> ignoredGuilds = new LinkedHashSet<>();
    private final Set<String> ignoredChannels = new LinkedHashSet<>();

    public PlayerIgnoreState(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID uuid() {
        return uuid;
    }

    public Set<UUID> ignoredPlayers() {
        return Set.copyOf(ignoredPlayers);
    }

    public Set<String> ignoredGuilds() {
        return Set.copyOf(ignoredGuilds);
    }

    public Set<String> ignoredChannels() {
        return Set.copyOf(ignoredChannels);
    }

    public boolean togglePlayer(UUID playerId) {
        if (!ignoredPlayers.remove(playerId)) {
            ignoredPlayers.add(playerId);
            return true;
        }
        return false;
    }

    public boolean toggleGuild(String guildId) {
        String id = guildId.toLowerCase(java.util.Locale.ROOT);
        if (!ignoredGuilds.remove(id)) {
            ignoredGuilds.add(id);
            return true;
        }
        return false;
    }

    public boolean toggleChannel(String channelId) {
        String id = channelId.toLowerCase(java.util.Locale.ROOT);
        if (!ignoredChannels.remove(id)) {
            ignoredChannels.add(id);
            return true;
        }
        return false;
    }

    public boolean isIgnoringPlayer(UUID playerId) {
        return ignoredPlayers.contains(playerId);
    }

    public boolean isIgnoringGuild(String guildId) {
        return guildId != null && ignoredGuilds.contains(guildId.toLowerCase(java.util.Locale.ROOT));
    }

    public boolean isIgnoringChannel(String channelId) {
        return ignoredChannels.contains(channelId.toLowerCase(java.util.Locale.ROOT));
    }

    public void restore(Set<UUID> players, Set<String> guilds, Set<String> channels) {
        ignoredPlayers.addAll(players);
        ignoredGuilds.addAll(guilds);
        ignoredChannels.addAll(channels);
    }

}
