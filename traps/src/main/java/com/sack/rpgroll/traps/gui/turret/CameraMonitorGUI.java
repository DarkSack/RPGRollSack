package com.sack.rpgroll.traps.gui.turret;

import com.sack.rpgroll.traps.gui.TrapMenus;
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

import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Lo que se abre al usar un monitor: las cámaras de quien lo mira y las de su equipo o guild.
 * Un clic y pasa a mirar por esa cámara; clic derecho a una suya, le cambia el nombre.
 */
public class CameraMonitorGUI extends InventoryGUI {

    public static final String BACKGROUND = "camaras";

    private static final int SIZE = 54;

    private final LangManager lang;
    private final CameraView cameraView;
    private final List<PlacedTurret> cameras;
    private final Consumer<PlacedTurret> rename;

    public CameraMonitorGUI(Player player, TurretManager turretManager, PlacedTurretManager placedTurretManager,
            CameraView cameraView, LangManager lang, Consumer<PlacedTurret> rename) {

        super(player, TrapMenus.skin().title(BACKGROUND, lang.component("gui.camera_monitor.title")), SIZE);

        this.lang = lang;
        this.cameraView = cameraView;
        this.rename = rename;
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
            List<Component> lore = new ArrayList<>(List.of(
                    lang.component("gui.camera_monitor.where", "world", camera.world(),
                            "x", camera.x(), "y", camera.y(), "z", camera.z()),
                    lang.component("gui.camera_monitor.click")));

            if (player.getUniqueId().equals(camera.owner())) {
                lore.add(lang.component("gui.camera_monitor.rename"));
            }

            setItem(i, ItemModels.apply(new ItemBuilder(Material.SPYGLASS)
                    .setName(lang.component("gui.camera_monitor.entry", "name", camera.name() != null
                            ? camera.name() : lang.raw("gui.camera_monitor.unnamed", "number", i + 1)))
                    .setLore(lore)
                    .build(), "turret", camera.turretId()));
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < 0 || slot >= cameras.size()) {
            return;
        }

        PlacedTurret camera = cameras.get(slot);

        // El nombre solo se lo pone su dueño, aunque el equipo pueda mirar por ella.
        if (event.isRightClick() && player.getUniqueId().equals(camera.owner())) {
            player.closeInventory();
            rename.accept(camera);
            return;
        }

        cameraView.watch(player, camera);
    }

}
