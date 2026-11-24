package com.sack.rpgroll.structures.workbench;

import com.sack.rpgroll.structures.storage.WorkbenchRecord;
import org.bukkit.entity.Player;

/** Gancho del clic derecho en una mesa con acceso; la Task 6 enchufa aquí el menú principal. */
@FunctionalInterface
public interface BenchOpener {
    BenchOpener NONE = (player, bench) -> {};

    void open(Player player, WorkbenchRecord bench);
}
