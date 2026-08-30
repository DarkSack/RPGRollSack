package com.sack.rpgroll.guilds.guild;

import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GuildManagerTest {

    @TempDir
    Path tempDir;

    private GuildManager manager;

    @BeforeEach
    void setUp() {
        Plugin plugin = mock(Plugin.class);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));

        manager = new GuildManager(plugin);
    }

    @Test
    void createRegistersGuildNormalizedToLowercaseId() {
        UUID founder = UUID.randomUUID();
        manager.create("Crypt-Guild", "Crypt Guild", founder);

        assertTrue(manager.exists("crypt-guild"));
        assertTrue(manager.exists("CRYPT-GUILD"));
        assertTrue(manager.get("crypt-guild").isPresent());
    }

    @Test
    void findByMemberLocatesTheOwningGuild() {
        UUID founder = UUID.randomUUID();
        manager.create("crypt", "Crypt", founder);

        assertTrue(manager.findByMember(founder).isPresent());
        assertEquals("crypt", manager.findByMember(founder).get().id());
        assertFalse(manager.findByMember(UUID.randomUUID()).isPresent());
    }

    @Test
    void disbandRemovesGuildFromRegistry() {
        UUID founder = UUID.randomUUID();
        manager.create("crypt", "Crypt", founder);

        manager.disband("crypt");

        assertFalse(manager.exists("crypt"));
        assertEquals(0, manager.count());
    }

    @Test
    void inviteAcceptAddsPlayerAsRecruit() {
        UUID founder = UUID.randomUUID();
        UUID recruit = UUID.randomUUID();
        manager.create("crypt", "Crypt", founder);

        manager.invite(founder, "crypt", recruit);
        GuildManager.AcceptResult result = manager.accept(recruit, "crypt");

        assertEquals(GuildManager.AcceptResult.OK, result);
        assertEquals(GuildRole.RECRUIT, manager.get("crypt").orElseThrow().roleOf(recruit));
    }

    @Test
    void acceptWithoutInviteFails() {
        assertEquals(GuildManager.AcceptResult.NO_INVITE, manager.accept(UUID.randomUUID(), "crypt"));
    }

    @Test
    void acceptFailsWhenAlreadyInAGuild() {
        UUID founder = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        manager.create("crypt", "Crypt", founder);
        manager.create("other-guild", "Other", other);

        manager.invite(founder, "crypt", other);

        assertEquals(GuildManager.AcceptResult.ALREADY_IN_GUILD, manager.accept(other, "crypt"));
    }

    @Test
    void acceptFailsWhenGuildWasDisbandedBeforeAccepting() {
        UUID founder = UUID.randomUUID();
        UUID recruit = UUID.randomUUID();
        manager.create("crypt", "Crypt", founder);
        manager.invite(founder, "crypt", recruit);

        manager.disband("crypt");

        assertEquals(GuildManager.AcceptResult.GUILD_GONE, manager.accept(recruit, "crypt"));
    }

    @Test
    void declineRemovesPendingInvite() {
        UUID founder = UUID.randomUUID();
        UUID recruit = UUID.randomUUID();
        manager.create("crypt", "Crypt", founder);
        manager.invite(founder, "crypt", recruit);

        manager.decline(recruit);

        assertEquals(GuildManager.AcceptResult.NO_INVITE, manager.accept(recruit, "crypt"));
    }

    @Test
    void saveAndReinitializeRoundTripsGuildDataFromDisk() {
        UUID founder = UUID.randomUUID();
        Guild guild = manager.create("crypt", "Crypt Guild", founder);
        guild.addExperience(500);
        manager.save(guild);

        Plugin plugin = mock(Plugin.class);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test2"));
        GuildManager reloaded = new GuildManager(plugin);
        reloaded.initialize();

        assertTrue(reloaded.exists("crypt"));
        assertEquals(500, reloaded.get("crypt").orElseThrow().experience());
        assertTrue(reloaded.get("crypt").orElseThrow().isMember(founder));
    }
}
