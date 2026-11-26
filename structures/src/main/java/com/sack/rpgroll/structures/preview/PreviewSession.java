package com.sack.rpgroll.structures.preview;

import com.sack.rpgroll.common.structure.Rotation;
import com.sack.rpgroll.common.structure.SourceBlock;
import com.sack.rpgroll.structures.catalog.RotatedCache;
import com.sack.rpgroll.structures.catalog.StructureDefinition;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Vista previa de un jugador: dónde, girada cuánto y qué bloques falsos ya se le enviaron. */
public final class PreviewSession {

    public enum Mode { BLOCKS, OUTLINE }

    private final StructureDefinition def;
    private final UUID bench;
    private final long startedAt = System.currentTimeMillis();
    private Location minCorner;
    private Rotation rotation = Rotation.NONE;
    private Mode mode = Mode.BLOCKS;
    private List<SourceBlock> rotated;
    /** Posiciones con bloque falso ya enviado. */
    final Set<PreviewService.Pos> sent = new HashSet<>();
    /** Hay que volver a validar y reenviar. */
    volatile boolean dirty = true;
    /** Ya se le avisó de que, en Bedrock, solo ve el contorno. */
    boolean bedrockNotified;
    volatile CheckResult last = new CheckResult(List.of(), null);

    PreviewSession(StructureDefinition def, UUID bench, Location minCorner) {
        this.def = def;
        this.bench = bench;
        this.minCorner = minCorner;
        refreshBlocks();
    }

    private void refreshBlocks() {
        // Girada una vez por estructura y giro (la comparte el motor); aquí solo se copia para ordenar.
        List<SourceBlock> l = new ArrayList<>(RotatedCache.get(def, rotation));
        // Los más bajos primero: es lo que se envía si hay más bloques que el tope.
        l.sort(Comparator.comparingInt(SourceBlock::y).thenComparingInt(SourceBlock::x)
                .thenComparingInt(SourceBlock::z));
        rotated = l;
    }

    public Location minCorner() {
        return minCorner.clone();
    }

    public Rotation rotation() {
        return rotation;
    }

    public StructureDefinition def() {
        return def;
    }

    public UUID bench() {
        return bench;
    }

    public Mode mode() {
        return mode;
    }

    public int width() {
        return rotation.rotatedWidth(def.blocks().width(), def.blocks().length());
    }

    public int height() {
        return def.blocks().height();
    }

    public int length() {
        return rotation.rotatedLength(def.blocks().width(), def.blocks().length());
    }

    public Footprint footprint() {
        return Footprint.of(minCorner.getBlockX(), minCorner.getBlockY(), minCorner.getBlockZ(), width(), height(),
                length());
    }

    List<SourceBlock> rotatedBlocks() {
        return rotated;
    }

    long startedAt() {
        return startedAt;
    }

    void rotate() {
        rotation = rotation.next();
        refreshBlocks();
        dirty = true;
    }

    void move(int dx, int dy, int dz) {
        minCorner = minCorner.clone().add(dx, dy, dz);
        dirty = true;
    }

    void toggleMode() {
        mode = mode == Mode.BLOCKS ? Mode.OUTLINE : Mode.BLOCKS;
        dirty = true;
    }
}
