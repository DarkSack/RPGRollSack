package com.sack.rpgroll.extras.sort;

import com.sack.rpgroll.extras.sort.SortWand.Target;

import org.bukkit.event.block.Action;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SortWandTest {

    @Test
    void rightClickOnAContainerSortsIt() {
        assertEquals(Target.CONTAINER, SortWand.target(Action.RIGHT_CLICK_BLOCK, true, false));
    }

    @Test
    void aContainerYouCannotOpenIsNotSorted() {
        assertEquals(Target.DENIED, SortWand.target(Action.RIGHT_CLICK_BLOCK, true, true));
    }

    @Test
    void rightClickOnAirOrAnotherBlockSortsYourInventory() {
        assertEquals(Target.OWN, SortWand.target(Action.RIGHT_CLICK_AIR, false, false));
        assertEquals(Target.OWN, SortWand.target(Action.RIGHT_CLICK_BLOCK, false, false));
    }

    @Test
    void leftClickDoesNothing() {
        assertEquals(Target.NONE, SortWand.target(Action.LEFT_CLICK_BLOCK, true, false));
        assertEquals(Target.NONE, SortWand.target(Action.LEFT_CLICK_AIR, false, false));
    }
}
