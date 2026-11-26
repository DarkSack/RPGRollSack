package com.sack.rpgroll.structures.catalog;

import com.sack.rpgroll.common.structure.BlockSource;
import com.sack.rpgroll.common.structure.SourceBlock;
import com.sack.rpgroll.structures.config.CategorySettings;
import com.sack.rpgroll.structures.config.WorkbenchSettings;
import com.sack.rpgroll.structures.workbench.WorkbenchTier;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** El YAML de ejemplo que explica el formato (README.txt) tiene que cargar sin problemas. */
class CatalogSmokeTest {

    @TempDir File dir;

    @Test
    void exampleIsValid() throws IOException {
        File structures = new File(dir, "structures");
        File schematics = new File(dir, "schematics");
        structures.mkdirs();
        schematics.mkdirs();
        try (InputStream in = getClass().getResourceAsStream("/structures/caseta.yml")) {
            assertNotNull(in, "falta structures/caseta.yml de ejemplo");
            Files.copy(in, new File(structures, "caseta.yml").toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        Files.writeString(new File(schematics, "caseta.nbt").toPath(), "x");
        BlockSource box = new BlockSource() {
            public int width() { return 5; }
            public int height() { return 4; }
            public int length() { return 5; }
            public List<SourceBlock> blocks() { return List.of(new SourceBlock(0, 0, 0, null, null)); }
        };
        YamlConfiguration cats;
        try (var r = new java.io.InputStreamReader(getClass().getResourceAsStream("/categories.yml"),
                java.nio.charset.StandardCharsets.UTF_8)) {
            cats = YamlConfiguration.loadConfiguration(r);
        }
        var result = StructureLoader.load(structures, schematics, CategorySettings.from(cats, w -> fail(w)),
                Map.of(WorkbenchTier.CONSTRUCTOR, new WorkbenchSettings.Size(16, 16, 16)),
                id -> true, f -> box);
        assertEquals(List.of(), result.problems());
        assertEquals(1, result.valid().size());
        assertEquals("caseta", result.valid().get(0).id());
    }
}
