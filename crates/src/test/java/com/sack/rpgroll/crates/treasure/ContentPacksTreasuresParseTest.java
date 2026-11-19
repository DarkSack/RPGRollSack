package com.sack.rpgroll.crates.treasure;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Los tesoros de los packs de contenido (RPGRoll/packs/<pack>/RPGRoll-Crates/treasures/) se leen con
 * el mismo parser que el plugin: un rango, una acción o un campo mal escrito rompe aquí y no en el
 * servidor de quien compra el pack.
 */
class ContentPacksTreasuresParseTest {

    @Test
    void everyPackTreasureParses() throws IOException {
        Path packs = Path.of("..", "packs");
        List<Path> files;
        try (Stream<Path> walk = Files.walk(packs)) {
            files = walk.filter(p -> p.toString().replace('\\', '/').contains("/RPGRoll-Crates/treasures/"))
                    .filter(p -> p.toString().endsWith(".yml"))
                    .toList();
        }
        assertTrue(files.size() >= 4, "no se encontraron los tesoros de los packs en " + packs.toAbsolutePath());

        TreasureParser parser = new TreasureParser();
        List<String> errors = new ArrayList<>();
        for (Path file : files) {
            try {
                parser.parse(YamlConfiguration.loadConfiguration(file.toFile()));
            } catch (RuntimeException e) {
                errors.add(file + ": " + e.getMessage());
            }
        }
        assertTrue(errors.isEmpty(), String.join("\n", errors));
    }
}
