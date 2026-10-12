package com.sack.rpgroll.traps.gui.turret;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.traps.item.ItemModels;
import com.sack.rpgroll.traps.turret.CameraView;
import com.sack.rpgroll.traps.turret.PlacedTurret;
import com.sack.rpgroll.traps.turret.PlacedTurretManager;
import com.sack.rpgroll.traps.turret.TurretAccess;
import com.sack.rpgroll.traps.turret.TurretManager;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

/**
 * Lo que se abre al usar un monitor: las cámaras de quien lo mira y las de su equipo o guild.
 * Un clic y pasa a mirar por esa cámara.
 */
public class CameraMonitorGUI extends InventoryGUI {

    private static final int SIZE = 54;

    private final LangManager lang;
    private final CameraView cameraView;
    private final List<PlacedTurret> cameras;

    public CameraMonitorGUI(Player player, TurretManager turretManager, PlacedTurretManager placedTurretManager,
            CameraView cameraView, LangManager lang) {

        super(player, lang.component("gui.camera_monitor.title"), SIZE);

        this.lang = lang;
        this.cameraView = cameraView;
        // shortcut: las 54 primeras; con más cámaras que eso por equipo hará falta paginar.
        this.cameras = placedTurretManager.getAll().stream()
                .filter(placed -> TurretAccess.isAlly(player, placed))
                .filter(placed -> turretManager.get(placed.turretId())
                        .filter(definition -> CameraView.CAMERA.equals(definition.impact().type())).isPresent())
                .limit(SIZE)
                .toList();
    }

    @Override
    public void build() {

        clear();

        if (cameras.isEmpty()) {
            setItem(22, new ItemBuilder(Material.BARRIER).setName(lang.component("gui.camera_monitor.empty")).build());
            return;
        }

        for (int i = 0; i < cameras.size(); i++) {

            PlacedTurret camera = cameras.get(i);

            setItem(i, ItemModels.apply(new ItemBuilder(Material.SPYGLASS)
                    .setName(lang.component("gui.camera_monitor.entry", "number", i + 1))
                    .setLore(lang.component("gui.camera_monitor.where", "world", camera.world(),
                            "x", camera.x(), "y", camera.y(), "z", camera.z()),
                            lang.component("gui.camera_monitor.click"))
                    .build(), "turret", camera.turretId()));
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot >= 0 && slot < cameras.size()) {
            cameraView.watch(player, cameras.get(slot));
        }
    }

}
