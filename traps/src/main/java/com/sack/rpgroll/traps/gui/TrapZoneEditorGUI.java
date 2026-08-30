package com.sack.rpgroll.traps.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.traps.location.PlacedTrap;
import com.sack.rpgroll.traps.location.PlacedTrapManager;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Selección de 2 puntos para una trampa de zona — clon de
 * {@code FishingRegionEditorGUI}: slots "Esquina A"/"Esquina B" capturan
 * {@code player.getLocation()} al click; Confirmar crea el PlacedTrap.
 */
public class TrapZoneEditorGUI extends InventoryGUI {

    private static final int SIZE = 27;
    private static final int CORNER_A_SLOT = 10;
    private static final int CORNER_B_SLOT = 12;
    private static final int CONFIRM_SLOT = 14;
    private static final int BACK_SLOT = 22;

    private final String trapId;
    private final PlacedTrapManager placedTrapManager;
    private final LangManager lang;
    private final Runnable onBack;
    private Location cornerA;
    private Location cornerB;

    public TrapZoneEditorGUI(Player player, String trapId, PlacedTrapManager placedTrapManager, LangManager lang,
            Runnable onBack) {
        super(player, lang.component("gui.zone.title", "id", trapId), SIZE);
        this.trapId = trapId;
        this.placedTrapManager = placedTrapManager;
        this.lang = lang;
        this.onBack = onBack;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(CORNER_A_SLOT, new ItemBuilder(Material.RED_CONCRETE)
                .setName(lang.component("gui.zone.corner_a", "value", describe(cornerA)))
                .setLore(lang.component("gui.zone.corner_hint"))
                .build());

        setItem(CORNER_B_SLOT, new ItemBuilder(Material.BLUE_CONCRETE)
                .setName(lang.component("gui.zone.corner_b", "value", describe(cornerB)))
                .setLore(lang.component("gui.zone.corner_hint"))
                .build());

        setItem(CONFIRM_SLOT, ItemBuilder.createConfirmButton(lang.raw("gui.zone.confirm")));

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back")));
    }

    private String describe(Location location) {
        return location == null ? lang.raw("gui.zone.unset")
                : String.format("(%d, %d, %d)", location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == CORNER_A_SLOT) {
            cornerA = player.getLocation();
            build();
            return;
        }

        if (slot == CORNER_B_SLOT) {
            cornerB = player.getLocation();
            build();
            return;
        }

        if (slot == CONFIRM_SLOT) {
            if (cornerA == null || cornerB == null) {
                lang.send(player, "gui.zone.missing_corners");
                return;
            }
            PlacedTrap placed = placedTrapManager.addZone(trapId, cornerA, cornerB, -1);
            lang.send(player, "gui.zone.created", "placement", placed.placementId());
            close();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
