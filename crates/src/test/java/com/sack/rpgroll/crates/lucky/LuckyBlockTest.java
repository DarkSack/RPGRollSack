package com.sack.rpgroll.crates.lucky;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LuckyBlockTest {

    private static LuckyBlock parse(String yaml) throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        return new LuckyParser().parse(config);
    }

    private static List<LuckyBlock> bundled() throws IOException, URISyntaxException {
        Path folder = Path.of(LuckyBlockTest.class.getResource("/lucky").toURI());
        try (Stream<Path> files = Files.list(folder)) {
            return files.filter(path -> path.toString().endsWith(".yml")).sorted().map(path -> {
                try {
                    YamlConfiguration config = new YamlConfiguration();
                    config.loadFromString(Files.readString(path, StandardCharsets.UTF_8));
                    return new LuckyParser().parse(config);
                } catch (Exception e) {
                    throw new AssertionError(path.getFileName() + ": " + e.getMessage(), e);
                }
            }).toList();
        }
    }

    @Test
    void bundledBlocksParseWithUniqueNotesAndValidTables() throws Exception {

        List<LuckyBlock> blocks = bundled();
        assertEquals(7, blocks.size());

        Set<Integer> notes = new HashSet<>();
        Set<String> ids = new HashSet<>();
        for (LuckyBlock block : blocks) {
            assertTrue(notes.add(block.note()), "nota repetida: " + block.note());
            assertTrue(ids.add(block.id()));
            assertTrue(block.outcomes().size() >= 10, block.id() + " tiene pocos resultados");
            assertTrue(block.outcomes().stream().anyMatch(o -> o.luck() == LuckyOutcome.Luck.BAD),
                    block.id() + " sin mala suerte");

            Set<String> outcomeIds = new HashSet<>();
            for (LuckyOutcome outcome : block.outcomes()) {
                assertTrue(outcomeIds.add(outcome.id()), block.id() + ": resultado repetido " + outcome.id());
                assertFalse(outcome.actions().isEmpty(), block.id() + "/" + outcome.id() + " sin acciones");
            }
        }

        // LUCKY apunta a tipos que existen.
        for (LuckyBlock block : blocks) {
            block.outcomes().stream().flatMap(o -> o.actions().stream())
                    .filter(a -> a.type() == LuckyAction.Type.LUCKY)
                    .forEach(a -> assertTrue(ids.contains(a.text("lucky", "")), "LUCKY a un tipo que no existe"));
        }
    }

    @Test
    void cursedBlockIsHalfBadLuck() throws Exception {

        LuckyBlock cursed = bundled().stream().filter(b -> b.id().equals("maldito")).findFirst().orElseThrow();
        double bad = cursed.outcomes().stream().filter(o -> o.luck() == LuckyOutcome.Luck.BAD)
                .mapToDouble(LuckyOutcome::weight).sum();

        assertEquals(0.5, bad / cursed.totalWeight(), 0.01);
    }

    @Test
    void rollFollowsWeights() throws Exception {

        LuckyBlock block = parse("""
                id: prueba
                note: 3
                outcomes:
                  - {id: mucho, weight: 3, luck: GOOD, actions: [{type: XP, amount: 5}]}
                  - {id: poco, weight: 1, luck: BAD, actions: [{type: LAUNCH}]}
                """);
        Random random = new Random(11);
        Map<String, Integer> count = new HashMap<>();

        for (int i = 0; i < 8000; i++) {
            count.merge(block.roll(random).id(), 1, Integer::sum);
        }

        assertEquals(0.75, count.get("mucho") / 8000.0, 0.02);
        assertNotNull(block.outcome("POCO"));
    }

    @Test
    void amountsAcceptNumbersAndRanges() {

        Random random = new Random(5);
        assertEquals(3, LuckyAction.roll("3", 1, random));
        assertEquals(1, LuckyAction.roll(null, 1, random));
        assertEquals(1, LuckyAction.roll("mucho", 1, random));
        for (int i = 0; i < 500; i++) {
            int value = LuckyAction.roll("5-2", 0, random);
            assertTrue(value >= 2 && value <= 5);
        }
    }

    @Test
    void rejectsBrokenDefinitions() {

        assertThrows(IllegalArgumentException.class, () -> parse("note: 1"), "sin id");
        assertThrows(IllegalArgumentException.class, () -> parse("id: a\nnote: 25"), "nota fuera de rango");
        assertThrows(IllegalArgumentException.class, () -> parse("""
                id: a
                note: 1
                outcomes:
                  - {id: x, actions: [{type: VOLAR}]}
                """), "acción desconocida");
        assertThrows(IllegalArgumentException.class, () -> parse("""
                id: a
                note: 1
                outcomes:
                  - {id: x, luck: REGULAR, actions: []}
                """), "suerte desconocida");
    }

    @Test
    void packedPositionsAreUniqueInsideAChunk() {

        Set<Integer> seen = new HashSet<>();
        for (int y = -64; y < 320; y += 7) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    assertTrue(seen.add(LuckyStore.pack(x, y, z)));
                }
            }
        }
        assertEquals(LuckyStore.pack(3, 70, 5), LuckyStore.pack(16 * 4 + 3, 70, -16 + 5));
    }

}
