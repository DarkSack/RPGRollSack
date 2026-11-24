package com.sack.rpgroll.structures.workbench;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WorkbenchRecipeTest {
    static final Map<Character, String> KEYS = Map.of('P', "minecraft:oak_planks", 'T', "minecraft:crafting_table");

    @Test void validRecipeHasNoProblem() {
        assertTrue(WorkbenchRecipe.problem(List.of("PPP", "PTP", "P P"), KEYS).isEmpty());
    }

    @Test void shapeLetterWithoutKeyIsAProblem() {
        assertTrue(WorkbenchRecipe.problem(List.of("PPP", "PXP"), KEYS).orElseThrow().contains("'X'"));
    }

    @Test void unusedKeyIsAProblem() {
        assertTrue(WorkbenchRecipe.problem(List.of("PPP"), KEYS).orElseThrow().contains("'T'"));
    }

    @Test void ingredientWithACraftingRemainderIsAProblem() {
        var keys = Map.of('P', "minecraft:oak_planks", 'W', "minecraft:water_bucket");
        assertTrue(WorkbenchRecipe.problem(List.of("PWP"), keys, id -> id.equals("minecraft:water_bucket"))
                .orElseThrow().contains("'W'"));
        assertTrue(WorkbenchRecipe.problem(List.of("PWP"), keys, id -> false).isEmpty());
    }

    @Test void badDimensionsAreAProblem() {
        assertTrue(WorkbenchRecipe.problem(List.of(), KEYS).isPresent());
        assertTrue(WorkbenchRecipe.problem(List.of("PPPP"), KEYS).isPresent());
        assertTrue(WorkbenchRecipe.problem(List.of("PP", "PTP"), KEYS).isPresent());
    }

    @Test void classifiesEveryClickType() {
        for (org.bukkit.event.inventory.ClickType c : org.bukkit.event.inventory.ClickType.values()) {
            WorkbenchRecipe.Action empty = WorkbenchRecipe.classify(c, true);
            WorkbenchRecipe.Action busy = WorkbenchRecipe.classify(c, false);
            switch (c) {
                case LEFT, RIGHT -> {
                    assertEquals(WorkbenchRecipe.Action.NORMAL, empty, c.name());
                    assertEquals(WorkbenchRecipe.Action.CANCEL, busy, c.name());
                }
                case SHIFT_LEFT, SHIFT_RIGHT -> {
                    assertEquals(WorkbenchRecipe.Action.ONE_BY_ONE, empty, c.name());
                    assertEquals(WorkbenchRecipe.Action.ONE_BY_ONE, busy, c.name());
                }
                default -> {
                    assertEquals(WorkbenchRecipe.Action.CANCEL, empty, c.name());
                    assertEquals(WorkbenchRecipe.Action.CANCEL, busy, c.name());
                }
            }
        }
    }
}
