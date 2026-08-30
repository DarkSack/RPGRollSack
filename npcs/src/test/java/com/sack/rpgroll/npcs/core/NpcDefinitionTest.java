package com.sack.rpgroll.npcs.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NpcDefinitionTest {

    private NpcDefinition definition(String skinValue, String skinSignature) {
        return new NpcDefinition("merchant", "Merchant", skinValue, skinSignature, null, "world",
                0, 64, 0, 0, 0, null);
    }

    @Test
    void blankIdIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new NpcDefinition("", "Merchant", null, null, null, "world", 0, 64, 0, 0, 0, null));
    }

    @Test
    void nullWorldIsRejected() {
        assertThrows(NullPointerException.class,
                () -> new NpcDefinition("merchant", "Merchant", null, null, null, null, 0, 64, 0, 0, 0, null));
    }

    @Test
    void nullPoseFallsBackToStanding() {
        NpcDefinition def = new NpcDefinition("merchant", "Merchant", null, null, null, "world", 0, 64, 0, 0, 0, null);
        assertEquals("STANDING", def.pose());
    }

    @Test
    void poseIsNormalizedToUppercase() {
        NpcDefinition def = new NpcDefinition("merchant", "Merchant", null, null, "sitting", "world",
                0, 64, 0, 0, 0, null);
        assertEquals("SITTING", def.pose());
    }

    @Test
    void hasCustomSkinRequiresBothValueAndSignature() {
        assertFalse(definition(null, null).hasCustomSkin());
        assertFalse(definition("value", null).hasCustomSkin());
        assertFalse(definition(null, "sig").hasCustomSkin());
        assertTrue(definition("value", "sig").hasCustomSkin());
    }

    @Test
    void nullActionsBecomesEmptyImmutableList() {
        NpcDefinition def = new NpcDefinition("merchant", "Merchant", null, null, null, "world",
                0, 64, 0, 0, 0, null);

        assertTrue(def.actions().isEmpty());
        assertThrows(UnsupportedOperationException.class,
                () -> def.actions().add(new NpcAction(NpcAction.NpcActionType.MESSAGE, "hi")));
    }

    @Test
    void actionsListIsDefensivelyCopied() {
        var mutable = new java.util.ArrayList<NpcAction>();
        mutable.add(new NpcAction(NpcAction.NpcActionType.MESSAGE, "hi"));
        NpcDefinition def = new NpcDefinition("merchant", "Merchant", null, null, null, "world",
                0, 64, 0, 0, 0, mutable);

        mutable.clear();

        assertTrue(def.actions().size() == 1);
    }
}
