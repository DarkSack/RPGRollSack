package com.sack.rpgroll.structures.workbench;

import com.sack.rpgroll.structures.storage.WorkbenchRecord;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Lista de mesas del PDC del chunk ({@code rpgroll-structures:benches}): {@code id@x,y,z:MATERIAL}, con el bloque
 * que se puso. Sigue leyendo el formato viejo, sin material ({@code id@x,y,z}). Solo en el hilo principal.
 */
final class BenchChunks {

    /** Una entrada leída. {@code material} es null en el formato viejo o si el nombre no es un material. */
    record Entry(String raw, String id, String x, String y, String z, Material material) {}

    private final NamespacedKey key;

    BenchChunks(Plugin plugin) {
        this.key = new NamespacedKey(plugin, "benches");
    }

    static String entry(WorkbenchRecord r, Material block) {
        return r.id() + "@" + r.x() + "," + r.y() + "," + r.z() + ":" + block.name();
    }

    static Entry parse(String raw) {
        int at = raw.indexOf('@');
        String id = raw.substring(0, Math.max(0, at));
        String rest = at < 0 ? "" : raw.substring(at + 1);
        int colon = rest.indexOf(':');
        Material material = colon < 0 ? null : Material.getMaterial(rest.substring(colon + 1));
        String[] c = (colon < 0 ? rest : rest.substring(0, colon)).split(",");
        if (c.length != 3) {
            c = new String[] {"?", "?", "?"};
        }
        return new Entry(raw, id, c[0], c[1], c[2], material);
    }

    List<String> stored(Chunk chunk) {
        List<String> l = chunk.getPersistentDataContainer().get(key, PersistentDataType.LIST.strings());
        return l == null ? new ArrayList<>() : new ArrayList<>(l);
    }

    void store(Chunk chunk, List<String> entries) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        if (entries.isEmpty()) {
            pdc.remove(key);
        } else {
            pdc.set(key, PersistentDataType.LIST.strings(), entries);
        }
    }

    /** Apunta la mesa con su bloque actual ({@code block} != null) o la quita (null). */
    void mark(Chunk chunk, WorkbenchRecord r, Material block) {
        List<String> entries = stored(chunk);
        String prefix = r.id() + "@";
        entries.removeIf(e -> e.startsWith(prefix));
        if (block != null) {
            entries.add(entry(r, block));
        }
        store(chunk, entries);
    }
}
