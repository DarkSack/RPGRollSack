package com.sack.rpgroll.guilds.guild;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GuildStoreStyledNameTest {

    @TempDir
    Path tempDir;

    private GuildStore store;

    @BeforeEach
    void setUp() {
        Plugin plugin = mock(Plugin.class);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        store = new GuildStore(plugin);
    }

    @Test
    void savesAndLoadsTheStyledName() {
        Guild guild = new Guild("papois", "Papois", UUID.randomUUID());
        guild.setStyledName("&#54DAF4Pa&bpois");

        store.save(guild);

        assertEquals("&#54DAF4Pa&bpois", store.loadAll().get(0).styledName());
    }

    @Test
    void aHandEditedStyleThatIsNotTheNameIsDropped() throws Exception {
        Guild guild = new Guild("papois", "Papois", UUID.randomUUID());
        store.save(guild);

        java.io.File file = tempDir.resolve("guilds").resolve("papois.yml").toFile();
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        yml.set("customization.styled-name", "&cStaff");
        yml.save(file);

        assertNull(store.loadAll().get(0).styledName());
    }

}
