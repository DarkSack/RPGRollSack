package com.sack.rpgroll.structures.catalog;

import com.sack.rpgroll.common.structure.BlockSource;
import com.sack.rpgroll.common.structure.BlockSources;
import com.sack.rpgroll.common.structure.Rotation;
import com.sack.rpgroll.common.structure.SourceBlock;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bloques girados de cada estructura, compartidos por la vista previa y el motor: girar clona cada BlockData. La
 * clave lleva el {@link BlockSource} mismo (no solo el id): tras recargar, una estructura con el mismo id y otro
 * esquemático no recibe la lista vieja aunque la entrada se cree mientras se recarga. Se vacía al recargar el
 * catálogo ({@link StructureRegistry#replace}). Sirve desde cualquier hilo.
 */
public final class RotatedCache {

    private record Key(String id, Rotation rotation, BlockSource source) {
    }

    private static final Map<Key, List<SourceBlock>> CACHE = new ConcurrentHashMap<>();

    private RotatedCache() {
    }

    /** Lista inmutable; quien la ordene, que copie. */
    public static List<SourceBlock> get(StructureDefinition def, Rotation r) {
        return CACHE.computeIfAbsent(new Key(def.id(), r, def.blocks()),
                k -> Collections.unmodifiableList(BlockSources.rotate(k.source(), k.rotation())));
    }

    public static void clear() {
        CACHE.clear();
    }
}
