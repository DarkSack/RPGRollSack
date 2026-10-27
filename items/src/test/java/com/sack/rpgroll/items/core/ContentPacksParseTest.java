package com.sack.rpgroll.items.core;

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
 * Los ítems de los packs de contenido (RPGRoll/packs/<pack>/RPGRoll-Items/) se leen con el mismo
 * parser que el plugin: un material, un cooldown o un item-model mal escrito en un pack rompe aquí
 * y no en el servidor de quien lo compra.
 */
class ContentPacksParseTest {

    @Test
    void everyPackItemParses() throws IOException {
        Path packs = Path.of("..", "packs");
        List<Path> files;
        try (Stream<Path> walk = Files.walk(packs)) {
            files = walk.filter(p -> p.toString().replace('\\', '/').contains("/RPGRoll-Items/"))
                    .filter(p -> p.toString().endsWith(".yml"))
                    .toList();
        }
        assertTrue(files.size() > 40, "no se encontraron los ítems de los packs en " + packs.toAbsolutePath());

        ItemParser parser = new ItemParser();
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
