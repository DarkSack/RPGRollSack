package com.sack.rpgroll.dungeons.gui.editor;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.dungeons.core.DungeonPoint;
import com.sack.rpgroll.dungeons.core.DungeonRoom;
import com.sack.rpgroll.util.ComponentUtils;

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
    private final LangManager lang;
    private int selectedIndex = -1;

    public RoomsEditorGUI(Player player, DungeonEditorSession session, Runnable onBack) {
        super(player, ComponentUtils.parse(session.chatPromptManager.lang()
                .raw("gui.editor.rooms.title", "id", session.original.id())), SIZE);
        this.session = session;
        this.onBack = onBack;
        this.lang = session.chatPromptManager.lang();
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

            String waveAndBoss = lang.raw("gui.editor.rooms.item.stats", "objectives", room.objectives().size(),
                    "waves", room.waves().size())
                    + (room.hasBoss() ? lang.raw("gui.editor.rooms.item.boss", "boss", room.bossMobId()) : "");

            setItem(i, new ItemBuilder(selected ? Material.LIME_STAINED_GLASS_PANE : iconFor(room))
                    .setName(Component.text((i + 1) + ". " + room.id() + " (" + room.type() + ")",
                            NamedTextColor.YELLOW))
                    .setLore(
                            Component.text(waveAndBoss, NamedTextColor.GRAY),
                            ComponentUtils.parse(lang.raw("gui.editor.rooms.item.edit_remove_hint")),
                            ComponentUtils.parse(lang.raw("gui.editor.rooms.item.select_hint")))
                    .build());
        }

        setItem(ADD_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(ComponentUtils.parse(lang.raw("gui.editor.rooms.add")))
                .build());

        setItem(MOVE_UP_SLOT, new ItemBuilder(Material.ARROW)
                .setName(ComponentUtils.parse(lang.raw("gui.editor.rooms.move_up")))
                .setLore(ComponentUtils.parse(lang.raw("gui.editor.rooms.move_hint")))
                .build());

        setItem(MOVE_DOWN_SLOT, new ItemBuilder(Material.ARROW)
                .setName(ComponentUtils.parse(lang.raw("gui.editor.rooms.move_down")))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back")));
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
        session.chatPromptManager.prompt(player, "gui.editor.rooms.prompt.new_id", value -> {

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
