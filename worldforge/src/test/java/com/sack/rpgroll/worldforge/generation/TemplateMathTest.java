package com.sack.rpgroll.worldforge.generation;

import com.sack.rpgroll.worldforge.profile.ProfileValidator;

import org.bukkit.block.structure.StructureRotation;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplateMathTest {

    /** Minecraft's StructureTemplate.transform with the pivot at 0, written out on its own. */
    private static int[] rotate(int x, int z, StructureRotation r) {
        return switch (r) {
            case NONE -> new int[] {x, z};
            case CLOCKWISE_90 -> new int[] {-z, x};
            case CLOCKWISE_180 -> new int[] {-x, -z};
            case COUNTERCLOCKWISE_90 -> new int[] {z, -x};
        };
    }

    @Test
    void footprint_is_the_box_of_every_rotated_block() {
        int sx = 5, sz = 9;
        for (StructureRotation r : StructureRotation.values()) {
            int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
            for (int x = 0; x < sx; x++) {
                for (int z = 0; z < sz; z++) {
                    int[] p = rotate(x, z, r);
                    minX = Math.min(minX, p[0]);
                    maxX = Math.max(maxX, p[0]);
                    minZ = Math.min(minZ, p[1]);
                    maxZ = Math.max(maxZ, p[1]);
                }
            }
            assertArrayEquals(new int[] {minX, maxX, minZ, maxZ}, TemplatePopulator.footprint(sx, sz, r), r.name());
        }
    }

    @Test
    void origin_prefers_the_chunk_and_reports_when_nothing_fits() {
        Random random = new Random(3);
        for (int i = 0; i < 1000; i++) {
            int o = TemplatePopulator.origin(random, -40, 40, 16);
            assertTrue(o >= 16 && o <= 31, "inside its own chunk when the range allows it: " + o);
        }
        int o = TemplatePopulator.origin(random, 40, 45, 16);
        assertTrue(o >= 40 && o <= 45, "outside the chunk only when it has to be: " + o);
        assertEquals(TemplatePopulator.NO_FIT, TemplatePopulator.origin(random, 10, 9, 0));
    }

    @Test
    void the_widest_template_allowed_fits_in_a_populator_region_in_every_rotation() {
        // Paper gives populators one chunk of buffer around their own (LimitedRegion.getBuffer() = 16;
        // checked again on the server by the template scenario). One block of margin on each side.
        int w = ProfileValidator.MAX_TEMPLATE_WIDTH, buffer = 16, bx = 0;
        Random random = new Random(5);
        for (StructureRotation r : StructureRotation.values()) {
            int[] box = TemplatePopulator.footprint(w, w, r);
            int ox = TemplatePopulator.origin(random, bx - buffer + 1 - box[0], bx + 15 + buffer - 1 - box[1], bx);
            assertNotEquals(TemplatePopulator.NO_FIT, ox, r.name());
            assertTrue(ox + box[0] - 1 >= bx - buffer && ox + box[1] + 1 <= bx + 15 + buffer, r.name());
        }
    }
}
