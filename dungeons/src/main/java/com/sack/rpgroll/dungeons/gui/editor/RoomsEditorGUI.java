package com.sack.rpgroll.dungeons.gui.editor;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.dungeons.core.DungeonPoint;
import com.sack.rpgroll.dungeons.core.DungeonRoom;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Lista ordenada de salas — el orden de la lista ES el orden de
 * recorrido (sin ramificación). Click abre el editor de esa sala,
 * shift-click la elimina, y los botones ↑/↓ reordenan.
 */
public class RoomsEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int ADD_SLOT = 40;
    private static final int MOVE_UP_SLOT = 42;
    private static final int MOVE_DOWN_SLOT = 43;
    private static final int BACK_SLOT = 44;

    private final DungeonEditorSession session;
    private final Runnable onBack;
    private int selectedIndex = -1;

    public RoomsEditorGUI(Player player, DungeonEditorSession session, Runnable onBack) {
        super(player, Component.text("Salas: " + session.original.id(), NamedTextColor.GOLD), SIZE);
        this.session = session;
        this.onBack = onBack;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        List<DungeonRoom> rooms = session.rooms;

        for (int i = 0; i < rooms.size() && i < 36; i++) {

            DungeonRoom room = rooms.get(i);
            boolean selected = i == selectedIndex;

            setItem(i, new ItemBuilder(selected ? Material.LIME_STAINED_GLASS_PANE : iconFor(room))
                    .setName(Component.text((i + 1) + ". " + room.id() + " (" + room.type() + ")",
                            NamedTextColor.YELLOW))
                    .setLore(
                            Component.text("objetivos: " + room.objectives().size() + " · oleadas: "
                                    + room.waves().size() + (room.hasBoss() ? " · jefe: " + room.bossMobId() : ""),
                                    NamedTextColor.GRAY),
                            Component.text("Click para editar · Shift-click para quitar", NamedTextColor.DARK_GRAY),
                            Component.text("Click derecho para seleccionar (reordenar con ↑/↓)",
                                    NamedTextColor.DARK_GRAY))
                    .build());
        }

        setItem(ADD_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Agregar sala", NamedTextColor.GREEN))
                .build());

        setItem(MOVE_UP_SLOT, new ItemBuilder(Material.ARROW)
                .setName(Component.text("↑ Mover arriba", NamedTextColor.AQUA))
                .setLore(Component.text("Sobre la sala seleccionada (click derecho en una)", NamedTextColor.GRAY))
                .build());

        setItem(MOVE_DOWN_SLOT, new ItemBuilder(Material.ARROW)
                .setName(Component.text("↓ Mover abajo", NamedTextColor.AQUA))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    private Material iconFor(DungeonRoom room) {
        return switch (room.type()) {
            case ENTRANCE -> Material.OAK_DOOR;
            case COMBAT -> Material.IRON_SWORD;
            case PUZZLE -> Material.LEVER;
            case REST -> Material.RED_BED;
            case TREASURE -> Material.CHEST;
            case BOSS -> Material.WITHER_SKELETON_SKULL;
            case EVENT -> Material.TNT;
            case STORY -> Material.WRITTEN_BOOK;
            case CINEMATIC -> Material.SPYGLASS;
        };
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < session.rooms.size() && slot < 36) {

            if (event.isShiftClick()) {
                List<DungeonRoom> updated = new ArrayList<>(session.rooms);
                updated.remove(slot);
                session.rooms = updated;
                if (selectedIndex == slot) {
                    selectedIndex = -1;
                }
                build();
                return;
            }

            if (event.getClick() == ClickType.RIGHT) {
                selectedIndex = slot;
                build();
                return;
            }

            new RoomEditGUI(player, session, slot, this::reopen).open();
            return;
        }

        if (slot == ADD_SLOT) {
            promptAdd();
            return;
        }

        if (slot == MOVE_UP_SLOT) {
            move(-1);
            return;
        }

        if (slot == MOVE_DOWN_SLOT) {
            move(1);
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void move(int direction) {

        if (selectedIndex < 0) {
            return;
        }

        int target = selectedIndex + direction;
        if (target < 0 || target >= session.rooms.size()) {
            return;
        }

        List<DungeonRoom> updated = new ArrayList<>(session.rooms);
        DungeonRoom moved = updated.remove(selectedIndex);
        updated.add(target, moved);
        session.rooms = updated;
        selectedIndex = target;

        build();
    }

    private void promptAdd() {
        session.chatPromptManager.prompt(player, "Escribí el id de la nueva sala:", value -> {

            String id = value.trim().toLowerCase().replace(' ', '_');

            List<DungeonRoom> updated = new ArrayList<>(session.rooms);
            updated.add(new DungeonRoom(id, com.sack.rpgroll.dungeons.core.DungeonRoomType.COMBAT,
                    new DungeonPoint(session.bounds.world(), session.bounds.center().x(),
                            session.bounds.center().y(), session.bounds.center().z(), 0, 0),
                    List.of(), List.of(), null, Map.of()));
            session.rooms = updated;

            build();
        });
    }

    private void reopen() {
        new RoomsEditorGUI(player, session, onBack).open();
    }

}
