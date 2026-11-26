package com.sack.rpgroll.structures.gui;

import com.sack.rpgroll.structures.StructuresPlugin;
import com.sack.rpgroll.structures.preview.PreviewSession;
import com.sack.rpgroll.structures.storage.WorkbenchRecord;
import com.sack.rpgroll.structures.workbench.WorkbenchTier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Optional;
import java.util.UUID;

/**
 * Con una vista previa activa, shift + clic derecho con la mano vacía reabre sus mandos (salvo sobre una mesa). Sin
 * shift no se toca: puertas y palancas siguen funcionando.
 */
public final class PreviewReopenListener implements Listener {

    private final StructuresPlugin plugin;

    public PreviewReopenListener(StructuresPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {
        Action a = event.getAction();
        if (event.getHand() != EquipmentSlot.HAND || a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player p = event.getPlayer();
        if (!p.isSneaking() || !p.getInventory().getItemInMainHand().isEmpty()) {
            return;
        }
        // Sobre una mesa manda su propio menú (WorkbenchBlocks).
        if (event.getClickedBlock() != null && plugin.service().at(event.getClickedBlock()).isPresent()) {
            return;
        }
        Optional<PreviewSession> s = plugin.preview().session(p);
        if (s.isEmpty()) {
            return;
        }
        event.setCancelled(true);
        UUID bench = s.get().bench();
        // Sin acceso a la mesa (se lo quitaron con la vista activa) se avisa y no se abre.
        StructuresMenus.withAccess(p, plugin, bench, null, () -> {
            WorkbenchTier tier = plugin.service().byId(bench).map(WorkbenchRecord::tier)
                    .orElse(WorkbenchTier.CONSTRUCTOR);
            new PreviewMenu(p, plugin, bench, tier).open();
        });
    }
}
