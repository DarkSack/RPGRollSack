package com.sack.rpgroll.dungeons.structure.schematic;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.session.ClipboardHolder;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Único punto de contacto de {@code dungeons} con la API de WorldEdit.
 * <p>
 * WorldEdit es {@code compileOnly} (ver build.gradle.kts) — si el plugin no
 * está instalado en el server, sus clases no están en el classpath en
 * runtime. Por eso NINGUNA otra clase del addon debe importar
 * {@code com.sk89q.worldedit.*} directamente: todo pasa por acá, y esta
 * clase solo se toca después de que el llamador ya comprobó
 * {@link #isAvailable()}. Las firmas públicas usan exclusivamente tipos
 * Bukkit — nada de WorldEdit se filtra hacia afuera.
 */
public final class SchematicBridge {

    private SchematicBridge() {
    }

    /** Debe llamarse ANTES de cualquier otro método de esta clase. */
    public static boolean isAvailable() {
        return Bukkit.getPluginManager().isPluginEnabled("WorldEdit");
    }

    /** {@code {ancho, alto, profundidad}} del schematic, sin pegarlo. */
    public static int[] readSize(File schemFile) throws IOException {

        Clipboard clipboard = readClipboard(schemFile);
        BlockVector3 dimensions = clipboard.getDimensions();

        return new int[] { dimensions.x(), dimensions.y(), dimensions.z() };
    }

    public static void pasteFromFile(File schemFile, World world, Location origin, StructureRotation rotation,
            Mirror mirror) throws IOException, WorldEditException {

        Clipboard clipboard = readClipboard(schemFile);
        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(world);

        AffineTransform transform = new AffineTransform();
        transform = transform.rotateY(rotationDegrees(rotation));
        transform = applyMirror(transform, mirror);

        ClipboardHolder holder = new ClipboardHolder(clipboard);
        holder.setTransform(transform);

        try (EditSession editSession = WorldEdit.getInstance().newEditSessionBuilder().world(weWorld).build()) {

            Operation operation = holder.createPaste(editSession)
                    .to(BukkitAdapter.asBlockVector(origin))
                    .ignoreAirBlocks(false)
                    .build();

            Operations.complete(operation);
        }
    }

    private static Clipboard readClipboard(File schemFile) throws IOException {

        ClipboardFormat format = ClipboardFormats.findByFile(schemFile);

        if (format == null) {
            throw new IOException("Formato de schematic no reconocido: " + schemFile.getName());
        }

        try (FileInputStream input = new FileInputStream(schemFile);
                ClipboardReader reader = format.getReader(input)) {
            return reader.read();
        }
    }

    private static double rotationDegrees(StructureRotation rotation) {
        return switch (rotation) {
            case NONE -> 0;
            case CLOCKWISE_90 -> 90;
            case CLOCKWISE_180 -> 180;
            case COUNTERCLOCKWISE_90 -> 270;
        };
    }

    private static AffineTransform applyMirror(AffineTransform transform, Mirror mirror) {
        return switch (mirror) {
            case NONE -> transform;
            case LEFT_RIGHT -> transform.scale(BlockVector3.at(-1, 1, 1).toVector3());
            case FRONT_BACK -> transform.scale(BlockVector3.at(1, 1, -1).toVector3());
        };
    }

}
