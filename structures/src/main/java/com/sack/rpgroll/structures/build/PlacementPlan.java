package com.sack.rpgroll.structures.build;

import com.sack.rpgroll.common.structure.SourceBlock;

import java.util.List;

/** Lista ya ordenada de bloques de una obra; el cursor guardado es un índice en ella. */
public record PlacementPlan(List<SourceBlock> ordered) {

    public PlacementPlan {
        ordered = List.copyOf(ordered);
    }

    public int size() {
        return ordered.size();
    }

    public SourceBlock at(int index) {
        return ordered.get(index);
    }
}
