package com.sack.rpgroll.guilds.guild;

import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Registro en memoria (respaldado por {@link GuildStore}) de todas las guilds del servidor. */
public class GuildManager {

    private static final long INVITE_EXPIRY_MILLIS = 120_000;

    private final GuildStore store;
    private final Map<String, Guild> guildsById = new LinkedHashMap<>();
    private final Map<UUID, PendingGuildInvite> invitesByTarget = new LinkedHashMap<>();

    private record PendingGuildInvite(String guildId, UUID inviterId, long expiresAtMillis) {
    }

    public GuildManager(Plugin plugin) {
        this.store = new GuildStore(plugin);
    }

    public void invite(UUID inviterId, String guildId, UUID targetId) {
        invitesByTarget.put(targetId, new PendingGuildInvite(guildId.toLowerCase(Locale.ROOT), inviterId,
                System.currentTimeMillis() + INVITE_EXPIRY_MILLIS));
    }

    public enum AcceptResult {
        OK,
        NO_INVITE,
        EXPIRED,
        GUILD_GONE,
        ALREADY_IN_GUILD
    }

    public AcceptResult accept(UUID targetId, String guildId) {

        PendingGuildInvite invite = invitesByTarget.get(targetId);

        if (invite == null || !invite.guildId().equalsIgnoreCase(guildId)) {
            return AcceptResult.NO_INVITE;
        }

        invitesByTarget.remove(targetId);

        if (System.currentTimeMillis() > invite.expiresAtMillis()) {
            return AcceptResult.EXPIRED;
        }

        if (findByMember(targetId).isPresent()) {
            return AcceptResult.ALREADY_IN_GUILD;
        }

        Guild guild = guildsById.get(invite.guildId());

        if (guild == null) {
            return AcceptResult.GUILD_GONE;
        }

        guild.addMember(targetId, GuildRole.RECRUIT);
        save(guild);
        return AcceptResult.OK;
    }

    public void decline(UUID targetId) {
        invitesByTarget.remove(targetId);
    }

    public void initialize() {
        guildsById.clear();
        for (Guild guild : store.loadAll()) {
            guildsById.put(guild.id(), guild);
        }
    }

    public Optional<Guild> get(String id) {
        return Optional.ofNullable(guildsById.get(id.toLowerCase(Locale.ROOT)));
    }

    public boolean exists(String id) {
        return guildsById.containsKey(id.toLowerCase(Locale.ROOT));
    }

    public Collection<Guild> getAll() {
        return guildsById.values();
    }

    public int count() {
        return guildsById.size();
    }

    public Optional<Guild> findByMember(UUID playerId) {
        return guildsById.values().stream()
                .filter(guild -> guild.isMember(playerId))
                .findFirst();
    }

    public Guild create(String id, String name, UUID founderId) {

        String normalizedId = id.toLowerCase(Locale.ROOT);
        Guild guild = new Guild(normalizedId, name, founderId);
        guildsById.put(normalizedId, guild);
        save(guild);
        return guild;
    }

    public void disband(String id) {
        String normalizedId = id.toLowerCase(Locale.ROOT);
        guildsById.remove(normalizedId);
        store.delete(normalizedId);
    }

    public void save(Guild guild) {
        store.save(guild);
    }

    public void saveAll() {
        guildsById.values().forEach(store::save);
    }

}
