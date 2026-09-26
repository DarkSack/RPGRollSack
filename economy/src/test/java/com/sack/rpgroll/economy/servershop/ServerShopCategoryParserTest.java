package com.sack.rpgroll.economy.servershop;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerShopCategoryParserTest {

    private final List<String> warnings = new ArrayList<>();
    private final ServerShopCategoryParser parser = new ServerShopCategoryParser(warnings::add);

    private ServerShopCategory parse(String... lines) throws InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString(String.join("\n", lines));
        return parser.parse(yaml);
    }

    @Test
    void readsEveryKindOfLine() throws InvalidConfigurationException {

        ServerShopCategory category = parse(
                "id: mix",
                "slot: 4",
                "premium: true",
                "items:",
                "  - material: oak_log",
                "    amount: 64",
                "    buy: 128",
                "    sell: 32",
                "  - material: DIAMOND",
                "    market: DIAMOND",
                "  - item: flame_blade",
                "    buy: 140000",
                "  - enchant: lifesteal",
                "    level: 3",
                "    buy: 90000",
                "  - book: Mending",
                "    buy: 3500",
                "  - potion: strong_healing",
                "    form: splash_potion",
                "    buy: 95");

        assertEquals(List.of(), warnings);
        assertEquals(4, category.slot());
        assertTrue(category.premium());
        assertEquals(6, category.entries().size());

        ServerShopEntry log = category.entries().get(0);
        assertEquals(ServerShopEntry.Kind.MATERIAL, log.kind());
        assertEquals("OAK_LOG", log.key());
        assertEquals(64, log.amount());
        assertTrue(log.sellable());

        assertEquals("DIAMOND", category.entries().get(1).market());
        assertFalse(category.entries().get(2).sellable());
        assertEquals(3, category.entries().get(3).level());
        assertEquals("mending", category.entries().get(4).key());
        assertEquals("SPLASH_POTION", category.entries().get(5).form());
    }

    @Test
    void badLinesAreSkippedWithAWarning() throws InvalidConfigurationException {

        ServerShopCategory category = parse(
                "id: broken",
                "items:",
                "  - material: NOT_A_BLOCK",
                "    buy: 1",
                "  - buy: 5",
                "  - material: STONE",
                "  - item: dragon_slayer",
                "    sell: 10",
                "  - potion: HEALING",
                "    form: BUCKET",
                "    buy: 5",
                "  - material: STONE",
                "    buy: diez",
                "  - material: STONE",
                "    buy: 10");

        assertEquals(1, category.entries().size());
        assertEquals(6, warnings.size());
    }

    @Test
    void sellingAboveTheBuyPriceIsCappedToAvoidFreeMoney() throws InvalidConfigurationException {

        ServerShopCategory category = parse("id: loop", "items:", "  - material: STONE", "    buy: 10",
                "    sell: 50");

        assertEquals(10, category.entries().get(0).sell());
        assertEquals(1, warnings.size());
    }

    @Test
    void emptyOptionalFieldsBecomeDefaults() throws InvalidConfigurationException {

        ServerShopCategory category = parse("id: plain", "permission: \"\"", "currency: \" \"", "items:",
                "  - material: STONE", "    buy: 10");

        assertEquals("plain", category.displayName());
        assertEquals("CHEST", category.icon());
        assertNull(category.permission());
        assertNull(category.currency());
        assertEquals(-1, category.slot());
    }

    @Test
    void shippedSectionsLoadCleanAndNeverPayMoreThanTheyCharge() {

        File folder = new File("src/main/resources/server-shop");
        File[] files = Objects.requireNonNull(folder.listFiles((dir, name) -> name.endsWith(".yml")));
        Set<Integer> slots = new HashSet<>();
        int sections = 0;

        for (File file : files) {

            ServerShopCategory category = parser.parse(YamlConfiguration.loadConfiguration(file));
            assertFalse(category.entries().isEmpty(), file.getName());

            if (file.getName().startsWith("_")) {
                continue;
            }

            sections++;
            assertTrue(slots.add(category.slot()), "slot repetido: " + file.getName());

            for (ServerShopEntry entry : category.entries()) {
                assertTrue(entry.sell() <= entry.buy() || entry.market() != null, file.getName() + " " + entry.key());
            }
        }

        assertEquals(List.of(), warnings);
        assertEquals(15, sections);
    }

}
