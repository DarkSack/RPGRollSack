package com.sack.rpgroll.common.structure;

import java.util.List;

/** Estructura ya leída de un archivo, independiente del formato. */
public interface BlockSource {

    int width();

    int height();

    int length();

    /** Sin aire, orden indefinido. */
    List<SourceBlock> blocks();
}
