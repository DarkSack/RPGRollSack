package com.sack.rpgroll.npcs.core;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Solo se testean las ramas que no dependen de {@code RPGRollAPI.get()} —
 * HAS_JOB/MIN_JOB_LEVEL/MIN_LEVEL/HAS_RACE/HAS_CLASS requieren que RPGRoll
 * (core) esté completamente inicializado como plugin real; fuera de un
 * servidor Paper corriendo, esa llamada estática solo lanza
 * IllegalStateException, así que esas ramas no son testeables acá.
 */
class NpcConditionTest {

    private Player playerWithInventory(ItemStack... contents) {
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getContents()).thenReturn(contents);
        return player;
    }

    @Test
    void unknownConditionTypeIsFalse() {
        Player player = playerWithInventory();
        assertFalse(NpcCondition.evaluate(player, "NOT_A_CONDITION:foo"));
    }

    private ItemStack stack(Material material, int amount) {
        ItemStack stack = mock(ItemStack.class);
        when(stack.getType()).thenReturn(material);
        when(stack.getAmount()).thenReturn(amount);
        return stack;
    }

    @Test
    void hasItemTrueWhenInventoryContainsEnoughOfMaterial() {
        ItemStack diamond = stack(Material.DIAMOND, 3);
        Player player = playerWithInventory(diamond);

        assertTrue(NpcCondition.evaluate(player, "HAS_ITEM:DIAMOND"));
        assertTrue(NpcCondition.evaluate(player, "HAS_ITEM:DIAMOND,3"));
        assertFalse(NpcCondition.evaluate(player, "HAS_ITEM:DIAMOND,4"));
    }

    @Test
    void hasItemSumsAmountsAcrossMultipleStacks() {
        ItemStack a = stack(Material.DIAMOND, 2);
        ItemStack b = stack(Material.DIAMOND, 2);
        Player player = playerWithInventory(a, b);

        assertTrue(NpcCondition.evaluate(player, "HAS_ITEM:DIAMOND,4"));
        assertFalse(NpcCondition.evaluate(player, "HAS_ITEM:DIAMOND,5"));
    }

    @Test
    void hasItemIgnoresNullSlotsAndOtherMaterials() {
        ItemStack gold = stack(Material.GOLD_INGOT, 5);
        Player player = playerWithInventory(null, gold, null);

        assertFalse(NpcCondition.evaluate(player, "HAS_ITEM:DIAMOND"));
    }

    @Test
    void hasItemWithInvalidMaterialIsFalse() {
        Player player = playerWithInventory();
        assertFalse(NpcCondition.evaluate(player, "HAS_ITEM:NOT_A_MATERIAL"));
    }

    @Test
    void andCombinationRequiresAllConditionsTrue() {
        ItemStack diamond = stack(Material.DIAMOND, 1);
        Player player = playerWithInventory(diamond);

        assertTrue(NpcCondition.evaluate(player, "HAS_ITEM:DIAMOND && HAS_ITEM:DIAMOND,1"));
        assertFalse(NpcCondition.evaluate(player, "HAS_ITEM:DIAMOND && HAS_ITEM:DIAMOND,2"));
    }

    @Test
    void orCombinationRequiresAtLeastOneConditionTrue() {
        ItemStack diamond = stack(Material.DIAMOND, 1);
        Player player = playerWithInventory(diamond);

        assertTrue(NpcCondition.evaluate(player, "HAS_ITEM:DIAMOND,5 || HAS_ITEM:DIAMOND,1"));
        assertFalse(NpcCondition.evaluate(player, "HAS_ITEM:DIAMOND,5 || HAS_ITEM:DIAMOND,6"));
    }
}
