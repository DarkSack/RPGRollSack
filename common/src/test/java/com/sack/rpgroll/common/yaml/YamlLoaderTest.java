package com.sack.rpgroll.common.yaml;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class YamlLoaderTest {

    @TempDir
    File dataFolder;

    private YamlLoader loader;

    @BeforeEach
    void setUp() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.getDataFolder()).thenReturn(dataFolder);
        loader = new YamlLoader(plugin);
    }

    private void write(String relativePath, String contents) throws IOException {
        File file = new File(dataFolder, relativePath);
        file.getParentFile().mkdirs();
        Files.writeString(file.toPath(), contents, StandardCharsets.UTF_8);
    }

    private List<String> idsIn(List<YamlConfiguration> configs) {
        return configs.stream().map(config -> config.getString("id")).filter(id -> id != null).sorted().toList();
    }

    @Test
    void missingFolderYieldsEmptyList() {
        assertTrue(loader.loadAllInFolder("does-not-exist").isEmpty());
    }

    @Test
    void yamlFilesInFolderAreLoaded() throws Exception {
        write("market/diamond.yml", "id: DIAMOND");
        write("market/wheat.yml", "id: WHEAT");

        assertEquals(List.of("DIAMOND", "WHEAT"), idsIn(loader.loadAllInFolder("market")));
    }

    @Test
    void underscorePrefixedFilesAreSkippedAsInternalState() throws Exception {
        write("market/diamond.yml", "id: DIAMOND");
        write("market/_state.yml", "DIAMOND:\n  supply: 40");

        assertEquals(List.of("DIAMOND"), idsIn(loader.loadAllInFolder("market")));
        assertEquals(1, loader.loadAllInFolder("market").size());
    }

    @Test
    void nonYamlFilesAreIgnored() throws Exception {
        write("market/diamond.yml", "id: DIAMOND");
        write("market/notes.txt", "not yaml");
        write("market/backup.yml.bak", "id: STALE");

        assertEquals(1, loader.loadAllInFolder("market").size());
    }

    @Test
    void recursiveLoadPicksUpNestedFolders() throws Exception {
        write("items/sword/flame_blade.yml", "id: FLAME_BLADE");
        write("items/armor/knight_helmet.yml", "id: KNIGHT_HELMET");

        assertEquals(List.of("FLAME_BLADE", "KNIGHT_HELMET"),
                idsIn(loader.loadAllInFolderRecursive("items")));
    }

    @Test
    void recursiveLoadAlsoSkipsUnderscorePrefixedFiles() throws Exception {
        write("items/sword/flame_blade.yml", "id: FLAME_BLADE");
        write("items/_state.yml", "counter: 3");
        write("items/sword/_cache.yml", "counter: 9");

        assertEquals(List.of("FLAME_BLADE"), idsIn(loader.loadAllInFolderRecursive("items")));
        assertEquals(1, loader.loadAllInFolderRecursive("items").size());
    }

    @Test
    void nonRecursiveLoadIgnoresSubfolders() throws Exception {
        write("items/top_level.yml", "id: TOP");
        write("items/sword/nested.yml", "id: NESTED");

        assertEquals(List.of("TOP"), idsIn(loader.loadAllInFolder("items")));
    }
}
