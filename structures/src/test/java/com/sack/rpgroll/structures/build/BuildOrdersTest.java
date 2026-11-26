package com.sack.rpgroll.structures.build;

import com.sack.rpgroll.common.structure.SourceBlock;
import com.sack.rpgroll.structures.catalog.BuildOrder;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BuildOrdersTest {

    private static BlockData data(Material m) {
        BlockData d = org.mockito.Mockito.mock(BlockData.class);
        org.mockito.Mockito.when(d.getMaterial()).thenReturn(m);
        org.mockito.Mockito.when(d.isOccluding()).thenReturn(m == Material.STONE);
        return d;
    }

    private static SourceBlock b(int x, int y, int z) {
        return new SourceBlock(x, y, z, data(Material.STONE), null);
    }

    private static List<String> coords(List<SourceBlock> l) {
        List<String> r = new ArrayList<>();
        for (SourceBlock s : l) {
            r.add(s.x() + "," + s.y() + "," + s.z());
        }
        return r;
    }

    @Test
    void bottomUpIsYThenZThenX() {
        List<SourceBlock> in = List.of(b(1, 1, 0), b(0, 0, 1), b(1, 0, 0), b(0, 0, 0), b(0, 1, 0));
        assertEquals(List.of("0,0,0", "1,0,0", "0,0,1", "0,1,0", "1,1,0"),
                coords(BuildOrders.order(in, BuildOrder.BOTTOM_UP, List.of())));
    }

    @Test
    void tilesGoLastInTheirLayer() {
        SourceBlock tile = new SourceBlock(0, 0, 0, data(Material.CHEST), org.mockito.Mockito.mock(BlockState.class));
        List<SourceBlock> in = List.of(tile, b(1, 0, 0), b(0, 1, 0));
        List<SourceBlock> out = BuildOrders.order(in, BuildOrder.BOTTOM_UP, List.of());
        assertEquals(List.of("1,0,0", "0,0,0", "0,1,0"), coords(out));
    }

    @Test
    void layersZigzag() {
        List<SourceBlock> in = new ArrayList<>();
        for (int z = 0; z < 2; z++) {
            for (int x = 0; x < 2; x++) {
                in.add(b(x, 0, z));
            }
        }
        assertEquals(List.of("0,0,0", "1,0,0", "1,0,1", "0,0,1"),
                coords(BuildOrders.order(in, BuildOrder.LAYERS, List.of())));
    }

    @Test
    void centerOutStartsInMiddle() {
        List<SourceBlock> in = List.of(b(0, 0, 0), b(1, 0, 0), b(2, 0, 0));
        assertEquals(List.of("1,0,0", "0,0,0", "2,0,0"),
                coords(BuildOrders.order(in, BuildOrder.CENTER_OUT, List.of())));
    }

    @Test
    void stagesPutsTorchesLast() {
        SourceBlock torch = new SourceBlock(0, 0, 0, data(Material.TORCH), null);
        SourceBlock glass = new SourceBlock(1, 0, 0, data(Material.GLASS), null);
        SourceBlock stone = new SourceBlock(2, 0, 0, data(Material.STONE), null);
        List<SourceBlock> out = BuildOrders.order(List.of(torch, glass, stone), BuildOrder.STAGES, List.of());
        assertEquals(List.of(stone, glass, torch), out);
    }

    @Test
    void customGroupsFirst() {
        SourceBlock glass = new SourceBlock(0, 0, 0, data(Material.GLASS), null);
        SourceBlock stone = new SourceBlock(1, 0, 0, data(Material.STONE), null);
        List<SourceBlock> out = BuildOrders.order(List.of(stone, glass), BuildOrder.CUSTOM, List.of(Material.GLASS));
        assertEquals(List.of(glass, stone), out);
    }

    @Test
    void orderIsDeterministic() {
        List<SourceBlock> in = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            in.add(b(i % 4, i % 3, i % 5));
        }
        for (BuildOrder o : BuildOrder.values()) {
            assertEquals(BuildOrders.order(in, o, List.of(Material.STONE)), BuildOrders.order(in, o, List.of(Material.STONE)));
        }
    }

    /** Decorar-ordenar-desdecorar: la clave cara (material, opacidad) se calcula una vez por bloque. */
    @Test
    void expensiveKeysAreComputedOncePerBlock() {
        for (BuildOrder o : List.of(BuildOrder.STAGES, BuildOrder.CUSTOM)) {
            BlockData d = data(Material.STONE);
            List<SourceBlock> in = new ArrayList<>();
            for (int i = 0; i < 200; i++) {
                in.add(new SourceBlock(i % 7, i % 5, i % 11, d, null));
            }
            BuildOrders.order(in, o, List.of(Material.GLASS));
            org.mockito.Mockito.verify(d, org.mockito.Mockito.times(200)).getMaterial();
        }
    }
}
