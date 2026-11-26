package com.sack.rpgroll.structures.catalog;

import com.sack.rpgroll.common.structure.BlockSource;
import com.sack.rpgroll.common.structure.Rotation;
import com.sack.rpgroll.common.structure.SourceBlock;
import org.bukkit.block.data.BlockData;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RotatedCacheTest {

    static StructureDefinition def(BlockData data) {
        List<SourceBlock> blocks = List.of(new SourceBlock(0, 0, 0, data, null), new SourceBlock(1, 0, 0, data, null));
        BlockSource src = new BlockSource() {
            public int width() { return 2; }
            public int height() { return 1; }
            public int length() { return 1; }
            public List<SourceBlock> blocks() { return blocks; }
        };
        return new StructureDefinition("casa", Map.of(), "casas", null, null, null, src, List.of(), 0, false,
                BuildOrder.BOTTOM_UP, List.of(), 1, 1, null, "p");
    }

    @Test void rotatedListIsSharedUntilCleared() {
        BlockData data = mock(BlockData.class);
        when(data.clone()).thenReturn(data);
        StructureDefinition d = def(data);
        List<SourceBlock> a = RotatedCache.get(d, Rotation.CW_90);
        assertSame(a, RotatedCache.get(d, Rotation.CW_90), "vista previa y motor comparten la lista girada");
        verify(data, times(2)).clone(); // un giro por bloque, una sola vez
        assertNotSame(a, RotatedCache.get(d, Rotation.CW_180));
        assertThrows(UnsupportedOperationException.class, () -> a.add(null));

        RotatedCache.clear();
        assertNotSame(a, RotatedCache.get(d, Rotation.CW_90));
    }

    @Test void otherSourceWithSameIdIsNotConfused() {
        BlockData data = mock(BlockData.class);
        when(data.clone()).thenReturn(data);
        // Tras recargar, otra estructura con el mismo id (y los mismos bloques) es otra entrada.
        assertNotSame(RotatedCache.get(def(data), Rotation.NONE), RotatedCache.get(def(data), Rotation.NONE));
    }

    @Test void replacingTheCatalogClearsTheCache() {
        BlockData data = mock(BlockData.class);
        when(data.clone()).thenReturn(data);
        StructureDefinition d = def(data);
        List<SourceBlock> a = RotatedCache.get(d, Rotation.CW_90);
        new StructureRegistry().replace(new StructureLoader.LoadResult(List.of(d), List.of()));
        assertNotSame(a, RotatedCache.get(d, Rotation.CW_90));
    }
}
