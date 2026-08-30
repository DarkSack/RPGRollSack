package com.sack.rpgroll.crafting.ingredient;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IngredientMatcherTest {

    private final IngredientMatcher matcher = new IngredientMatcher(null);

    private ItemStack stackOf(Material material, int amount) {
        ItemStack stack = mock(ItemStack.class);
        when(stack.getType()).thenReturn(material);
        when(stack.getAmount()).thenReturn(amount);
        when(stack.hasItemMeta()).thenReturn(false);
        return stack;
    }

    @Test
    void matchesReturnsFalseForNullOrAirOrNullSpec() {
        assertFalse(matcher.matches(null, IngredientSpec.material("STONE", 1)));
        assertFalse(matcher.matches(stackOf(Material.AIR, 1), IngredientSpec.material("STONE", 1)));
        assertFalse(matcher.matches(stackOf(Material.STONE, 1), null));
    }

    @Test
    void anyTypeAcceptsAnyNonAirMaterial() {
        IngredientSpec spec = new IngredientSpec(IngredientType.ANY, null, 1, null);

        assertTrue(matcher.matches(stackOf(Material.DIRT, 1), spec));
        assertTrue(matcher.matches(stackOf(Material.DIAMOND, 1), spec));
    }

    @Test
    void materialTypeRequiresExactMaterialMatch() {
        IngredientSpec spec = IngredientSpec.material("IRON_INGOT", 1);

        assertTrue(matcher.matches(stackOf(Material.IRON_INGOT, 1), spec));
        assertFalse(matcher.matches(stackOf(Material.GOLD_INGOT, 1), spec));
    }

    @Test
    void materialTypeRejectsInvalidMaterialName() {
        IngredientSpec spec = IngredientSpec.material("NOT_A_REAL_MATERIAL", 1);

        assertFalse(matcher.matches(stackOf(Material.IRON_INGOT, 1), spec));
    }

    @Test
    void matchesWithAmountAlsoChecksStackSize() {
        IngredientSpec spec = IngredientSpec.material("IRON_INGOT", 3);

        assertTrue(matcher.matchesWithAmount(stackOf(Material.IRON_INGOT, 5), spec));
        assertFalse(matcher.matchesWithAmount(stackOf(Material.IRON_INGOT, 2), spec));
    }

    @Test
    void itemIdTypeMatchesAgainstPersistedIdentity() {
        NamespacedKey key = new NamespacedKey("rpgroll", "item_id");

        ItemStack stack = mock(ItemStack.class);
        ItemMeta meta = mock(ItemMeta.class);
        PersistentDataContainer container = mock(PersistentDataContainer.class);

        when(stack.getType()).thenReturn(Material.STICK);
        when(stack.hasItemMeta()).thenReturn(true);
        when(stack.getItemMeta()).thenReturn(meta);
        when(meta.getPersistentDataContainer()).thenReturn(container);
        when(container.get(key, PersistentDataType.STRING)).thenReturn("magic_wand");

        IngredientSpec spec = IngredientSpec.itemId("magic_wand", 1);
        IngredientSpec wrongSpec = IngredientSpec.itemId("other_item", 1);

        assertTrue(matcher.matches(stack, spec));
        assertFalse(matcher.matches(stack, wrongSpec));
    }

    @Test
    void minQualityRequiresRankAtOrAboveResolvedThreshold() {
        QualityRankResolver resolver = qualityId -> switch (qualityId) {
            case "fine" -> 2;
            case "rough" -> 0;
            case "legendary" -> 4;
            default -> 0;
        };
        IngredientMatcher matcherWithResolver = new IngredientMatcher(resolver);

        NamespacedKey qualityKey = new NamespacedKey("rpgroll", "crafting_quality");
        ItemStack stack = mock(ItemStack.class);
        ItemMeta meta = mock(ItemMeta.class);
        PersistentDataContainer container = mock(PersistentDataContainer.class);

        when(stack.getType()).thenReturn(Material.IRON_SWORD);
        when(stack.hasItemMeta()).thenReturn(true);
        when(stack.getItemMeta()).thenReturn(meta);
        when(meta.getPersistentDataContainer()).thenReturn(container);
        when(container.get(qualityKey, PersistentDataType.STRING)).thenReturn("fine");

        IngredientSpec metQuality = new IngredientSpec(IngredientType.MATERIAL, "IRON_SWORD", 1, "fine");
        IngredientSpec unmetQuality = new IngredientSpec(IngredientType.MATERIAL, "IRON_SWORD", 1, "legendary");

        assertTrue(matcherWithResolver.matches(stack, metQuality));
        assertFalse(matcherWithResolver.matches(stack, unmetQuality));
    }

    @Test
    void countAvailableSumsMatchingStacksAcrossInventory() {
        ItemStack[] contents = {
                stackOf(Material.IRON_INGOT, 3),
                stackOf(Material.GOLD_INGOT, 5),
                stackOf(Material.IRON_INGOT, 2),
                null
        };
        Inventory inventory = mock(Inventory.class);
        when(inventory.getContents()).thenReturn(contents);

        assertEquals(5, matcher.countAvailable(inventory, IngredientSpec.material("IRON_INGOT", 1)));
    }

    @Test
    void tryConsumeFailsWithoutTouchingInventoryWhenInsufficient() {
        ItemStack[] contents = { stackOf(Material.IRON_INGOT, 1) };
        Inventory inventory = mock(Inventory.class);
        when(inventory.getContents()).thenReturn(contents);

        assertFalse(matcher.tryConsume(inventory, IngredientSpec.material("IRON_INGOT", 5)));
    }

    @Test
    void tryConsumeAndCaptureReturnsEmptyWhenInsufficient() {
        ItemStack[] contents = { stackOf(Material.IRON_INGOT, 1) };
        Inventory inventory = mock(Inventory.class);
        when(inventory.getContents()).thenReturn(contents);

        assertTrue(matcher.tryConsumeAndCapture(inventory, IngredientSpec.material("IRON_INGOT", 5)).isEmpty());
    }
}
