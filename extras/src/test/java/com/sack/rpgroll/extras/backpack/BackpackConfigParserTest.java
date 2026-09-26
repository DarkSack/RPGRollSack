package com.sack.rpgroll.extras.backpack;

import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackpackConfigParserTest {

    private final List<String> warnings = new ArrayList<>();

    private BackpackSettings parse(String yaml) throws InvalidConfigurationException {
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(yaml);
        return new BackpackConfigParser(warnings::add).parse(config);
    }

    @Test
    void shippedFileLoadsCleanWithSevenTiersInOrder() {

        YamlConfiguration config = YamlConfiguration.loadConfiguration(new InputStreamReader(
                getClass().getResourceAsStream("/backpacks.yml"), StandardCharsets.UTF_8));
        BackpackSettings settings = new BackpackConfigParser(warnings::add).parse(config);

        assertEquals(List.of(), warnings);
        assertTrue(settings.enabled());
        assertEquals(List.of("cuero", "hierro", "oro", "diamante", "netherita_fragmentada", "netherita", "espacial"),
                settings.tiers().stream().map(BackpackTier::id).toList());

        int previousSlots = 0;
        for (BackpackTier tier : settings.tiers()) {
            assertTrue(tier.slots() > previousSlots, tier.id() + " debe tener más espacios que el anterior");
            previousSlots = tier.slots();
            assertNotNull(tier.recipe(), tier.id());
            assertEquals(tier.index() > 0, tier.recipe().usesBackpack(), tier.id());
            assertFalse(tier.texture().isBlank(), tier.id());
        }

        BackpackRecipe base = settings.tiers().get(0).recipe();
        assertEquals(Material.CHEST, base.ingredients().get('C').material());
        assertEquals(Material.LEATHER, base.ingredients().get('L').material());

        BackpackIngredient space = settings.tier("espacial").orElseThrow().recipe().ingredients().get('M');
        assertEquals(BackpackIngredient.Kind.ITEM, space.kind());
        assertEquals("mineral_espacial", space.itemId());

        assertEquals(List.of(2, 3, 4, 5, 6), settings.gui().tabSlots());
        assertEquals(0, settings.gui().infoSlot());
        assertEquals(8, settings.gui().bindSlot());
    }

    @Test
    void ingredientSpecs() {
        assertEquals(BackpackIngredient.BACKPACK, BackpackIngredient.parse("backpack"));
        assertEquals(BackpackIngredient.BACKPACK, BackpackIngredient.parse("Mochila"));
        assertEquals(Material.IRON_INGOT, BackpackIngredient.parse("iron_ingot").material());
        assertEquals("gema", BackpackIngredient.parse("item:gema").itemId());
        assertThrows(IllegalArgumentException.class, () -> BackpackIngredient.parse("item:"));
        assertThrows(IllegalArgumentException.class, () -> BackpackIngredient.parse("NOT_A_MATERIAL"));
        assertThrows(IllegalArgumentException.class, () -> BackpackIngredient.parse("AIR"));
    }

    @Test
    void badRecipesLeaveTheTierWithoutRecipe() throws InvalidConfigurationException {

        BackpackSettings settings = parse("""
                tiers:
                  first:
                    slots: 9
                    recipe:
                      shape: ["LBL"]
                      ingredients: {L: LEATHER, B: backpack}
                  second:
                    slots: 18
                    recipe:
                      shape: ["XXX", "XBX", "XXX"]
                      ingredients: {B: backpack}
                  third:
                    slots: 27
                    recipe:
                      shape: ["BB"]
                      ingredients: {B: backpack}
                  fourth:
                    slots: 36
                    recipe:
                      enabled: false
                """);

        assertEquals(4, settings.tiers().size());
        settings.tiers().forEach(tier -> assertNull(tier.recipe(), tier.id()));
        assertEquals(3, warnings.size(), warnings.toString());
    }

    @Test
    void slotsAreCappedByTheTabCount() throws InvalidConfigurationException {

        BackpackSettings settings = parse("""
                gui:
                  tabs:
                    slots: [3, 4, 9]
                tiers:
                  huge:
                    slots: 500
                  none:
                    slots: 0
                """);

        assertEquals(List.of(3, 4), settings.gui().tabSlots());
        assertEquals(90, settings.tier("huge").orElseThrow().slots());
        assertEquals(9, settings.tier("none").orElseThrow().slots());
        assertEquals(3, warnings.size(), warnings.toString());
    }

    @Test
    void clashingButtonsAreRemoved() throws InvalidConfigurationException {

        BackpackSettings settings = parse("""
                gui:
                  info: {slot: 4}
                  bind: {slot: 4}
                  tabs: {slots: [4]}
                tiers:
                  a: {slots: 9}
                """);

        assertEquals(-1, settings.gui().infoSlot());
        assertEquals(-1, settings.gui().bindSlot());
        assertEquals(2, warnings.size());
    }

    @Test
    void noTiersDisables() throws InvalidConfigurationException {
        assertFalse(parse("settings: {enabled: true}").enabled());
        assertFalse(parse("settings: {enabled: false}\ntiers: {a: {slots: 9}}").enabled());
    }

    @Test
    void pages() {
        assertEquals(1, BackpackPages.pages(18));
        assertEquals(1, BackpackPages.pages(45));
        assertEquals(2, BackpackPages.pages(46));
        assertEquals(3, BackpackPages.pages(135));

        assertEquals(2, BackpackPages.contentRows(18));
        assertEquals(3, BackpackPages.contentRows(20));
        assertEquals(5, BackpackPages.contentRows(72));
        assertEquals(27, BackpackPages.inventorySize(18));
        assertEquals(54, BackpackPages.inventorySize(72));

        assertEquals(45, BackpackPages.slotsOnPage(72, 0));
        assertEquals(27, BackpackPages.slotsOnPage(72, 1));
        assertEquals(0, BackpackPages.slotsOnPage(72, 2));
    }

    @Test
    void texturesAcceptHashUrlOrBase64() {

        String hash = "40b1b53674918391a07a9d00582c058f9280bc526a716c796ee5eab4be10a760";
        String fromHash = BackpackTextures.toBase64(hash);
        String json = new String(Base64.getDecoder().decode(fromHash), StandardCharsets.UTF_8);

        assertEquals("{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/" + hash + "\"}}}", json);
        assertEquals(fromHash, BackpackTextures.toBase64("https://textures.minecraft.net/texture/" + hash));
        assertEquals("eyJ0ZXh0dXJlcyI6e319", BackpackTextures.toBase64("eyJ0ZXh0dXJlcyI6e319"));
        assertEquals("", BackpackTextures.toBase64("  "));
    }

}
