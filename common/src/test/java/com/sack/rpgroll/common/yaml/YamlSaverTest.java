package com.sack.rpgroll.common.yaml;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class YamlSaverTest {

    @TempDir
    File folder;

    @Test
    void replacesTheFileAndLeavesNoTemporaryBehind() throws IOException {
        File file = new File(new File(folder, "wallets"), "a.yml");

        YamlConfiguration first = new YamlConfiguration();
        first.set("balance", 10);
        YamlSaver.save(first, file);

        YamlConfiguration second = new YamlConfiguration();
        second.set("balance", 25);
        YamlSaver.save(second, file);

        assertEquals(25, YamlConfiguration.loadConfiguration(file).getInt("balance"));
        assertArrayEquals(new String[] {"a.yml"}, file.getParentFile().list());
    }
}
