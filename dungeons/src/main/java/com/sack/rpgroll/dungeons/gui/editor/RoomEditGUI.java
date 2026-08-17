package com.sack.rpgroll.dungeons.gui.editor;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.util.ComponentUtils;
import com.sack.rpgroll.dungeons.core.DungeonAction;
import com.sack.rpgroll.dungeons.core.DungeonObjective;
import com.sack.rpgroll.dungeons.core.DungeonPoint;
import com.sack.rpgroll.dungeons.core.DungeonRoom;
import com.sack.rpgroll.dungeons.core.DungeonRoomType;
import com.sack.rpgroll.dungeons.core.DungeonTrigger;
import com.sack.rpgroll.dungeons.core.DungeonWave;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Edita una {@link DungeonRoom} puntual dentro de {@code session.rooms.get(index)}. */
public class RoomEditGUI extends InventoryGUI {

    private static final int SIZE = 36;

    private static final int ID_SLOT = 10;
    private static final int TYPE_SLOT = 11;
    private static final int ENTRY_POINT_SLOT = 12;
    private static final int BOSS_SLOT = 13;
    private static final int OBJECTIVES_SLOT = 19;
    private static final int WAVES_SLOT = 20;
    private static final int EVENTS_SLOT = 21;
    private static final int BACK_SLOT = 31;

    private final DungeonEditorSession session;
    private final int index;
    private final Runnable onBack;
    private final LangManager lang;

    public RoomEditGUI(Player player, DungeonEditorSession session, int index, Runnable onBack) {
        super(player, ComponentUtils.parse(session.chatPromptManager.lang()
                .raw("gui.editor.roomedit.title", "id", session.rooms.get(index).id())), SIZE);
        this.session = session;
        this.index = index;
        this.onBack = onBack;
        this.lang = session.chatPromptManager.lang();
    }

    private DungeonRoom room() {
        return session.rooms.get(index);
    }

    private void replace(DungeonRoom updated) {
        List<DungeonRoom> list = new ArrayList<>(session.rooms);
        list.set(index, updated);
        session.rooms = list;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        DungeonRoom room = room();

        setItem(ID_SLOT, new ItemBuilder(Material.PAPER)
                .setName(ComponentUtils.parse(lang.raw("gui.editor.roomedit.id", "value", room.id())))
                .setLore(ComponentUtils.parse(lang.raw("gui.editor.info.click_to_write")))
                .build());

        setItem(TYPE_SLOT, new ItemBuilder(Material.COMPARATOR)
                .setName(ComponentUtils.parse(lang.raw("gui.editor.roomedit.type.label", "value", room.type())))
                .setLore(ComponentUtils.parse(lang.raw("gui.editor.roomedit.type.hint")))
                .build());

        setItem(ENTRY_POINT_SLOT, new ItemBuilder(Material.MAP)
                .setName(ComponentUtils.parse(lang.raw("gui.editor.roomedit.entry_point.label")))
                .setLore(Component.text(pointText(room.entryPoint()), NamedTextColor.GRAY),
                        ComponentUtils.parse(lang.raw("gui.editor.roomedit.entry_point.hint")))
                .build());

        setItem(BOSS_SLOT, new ItemBuilder(Material.WITHER_SKELETON_SKULL)
                .setName(ComponentUtils.parse(lang.raw("gui.editor.roomedit.boss.label", "value",
                        room.hasBoss() ? room.bossMobId() : lang.raw("gui.editor.roomedit.boss.none"))))
                .setLore(ComponentUtils.parse(lang.raw("gui.editor.roomedit.boss.hint")),
                        ComponentUtils.parse(lang.raw("gui.editor.roomedit.boss.click_hint")))
                .build());

        setItem(OBJECTIVES_SLOT, new ItemBuilder(Material.TARGET)
                .setName(ComponentUtils.parse(lang.raw("gui.editor.roomedit.objectives",
                        "value", room.objectives().size())))
                .build());

        setItem(WAVES_SLOT, new ItemBuilder(Material.SPAWNER)
                .setName(ComponentUtils.parse(lang.raw("gui.editor.roomedit.waves", "value", room.waves().size())))
                .build());

        setItem(EVENTS_SLOT, new ItemBuilder(Material.REDSTONE_TORCH)
                .setName(ComponentUtils.parse(lang.raw("gui.editor.roomedit.events.label")))
                .setLore(ComponentUtils.parse(lang.raw("gui.editor.roomedit.events.hint")))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back")));
    }

    private String pointText(DungeonPoint point) {
        return String.format("%s %.0f, %.0f, %.0f", point.world(), point.x(), point.y(), point.z());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == ID_SLOT) {
            session.chatPromptManager.prompt(player, "gui.editor.roomedit.prompt.id", value -> {
                replace(withId(room(), value.trim().toLowerCase().replace(' ', '_')));
                build();
            });
            return;
        }

        if (slot == TYPE_SLOT) {
            DungeonRoomType[] values = DungeonRoomType.values();
            replace(withType(room(), values[(room().type().ordinal() + 1) % values.length]));
            build();
            return;
        }

        if (slot == ENTRY_POINT_SLOT) {
            replace(withEntryPoint(room(), DungeonPoint.fromLocation(player.getLocation())));
            build();
            return;
        }

        if (slot == BOSS_SLOT) {
            if (event.isShiftClick()) {
                replace(withBoss(room(), null));
                build();
                return;
            }
            session.chatPromptManager.prompt(player, "gui.editor.roomedit.prompt.boss", value -> {
                replace(withBoss(room(), value.trim()));
                build();
            });
            return;
        }

        if (slot == OBJECTIVES_SLOT) {
            List<DungeonObjective> objectives = new ArrayList<>(room().objectives());
            new ObjectivesEditorGUI(player, session, objectives, () -> {
                replace(withObjectives(room(), objectives));
                syncAndReopen();
            }).open();
            return;
        }

        if (slot == WAVES_SLOT) {
            List<DungeonWave> waves = new ArrayList<>(room().waves());
            new WavesEditorGUI(player, session, waves, () -> {
                replace(withWaves(room(), waves));
                syncAndReopen();
            }).open();
            return;
        }

        if (slot == EVENTS_SLOT) {
            Map<DungeonTrigger, List<DungeonAction>> events = new EnumMap<>(room().events());
            new RoomEventsEditorGUI(player, session, events, () -> {
                replace(withEvents(room(), events));
                syncAndReopen();
            }).open();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    /**
     * Reabre esta pantalla después de que un sub-editor (objetivos/oleadas/
     * eventos) ya volcó sus cambios a la sala vía {@code replace(...)} — el
     * orden importa: como {@link DungeonRoom} copia sus listas a inmutables
     * en el constructor compacto, hay que reemplazar recién cuando el
     * sub-editor terminó de mutar su lista, no antes de abrirlo.
     */
    private void syncAndReopen() {
        new RoomEditGUI(player, session, index, onBack).open();
    }

    private DungeonRoom withId(DungeonRoom room, String id) {
        return new DungeonRoom(id, room.type(), room.entryPoint(), room.objectives(), room.waves(),
                room.bossMobId(), room.events());
    }

    private DungeonRoom withType(DungeonRoom room, DungeonRoomType type) {
        return new DungeonRoom(room.id(), type, room.entryPoint(), room.objectives(), room.waves(),
                room.bossMobId(), room.events());
    }

    private DungeonRoom withEntryPoint(DungeonRoom room, DungeonPoint entryPoint) {
        return new DungeonRoom(room.id(), room.type(), entryPoint, room.objectives(), room.waves(),
                room.bossMobId(), room.events());
    }

    private DungeonRoom withBoss(DungeonRoom room, String bossMobId) {
        return new DungeonRoom(room.id(), room.type(), room.entryPoint(), room.objectives(), room.waves(),
                bossMobId, room.events());
    }

    private DungeonRoom withObjectives(DungeonRoom room, List<DungeonObjective> objectives) {
        return new DungeonRoom(room.id(), room.type(), room.entryPoint(), objectives, room.waves(),
                room.bossMobId(), room.events());
    }

    private DungeonRoom withWaves(DungeonRoom room, List<DungeonWave> waves) {
        return new DungeonRoom(room.id(), room.type(), room.entryPoint(), room.objectives(), waves,
                room.bossMobId(), room.events());
    }

    private DungeonRoom withEvents(DungeonRoom room, Map<DungeonTrigger, List<DungeonAction>> events) {
        return new DungeonRoom(room.id(), room.type(), room.entryPoint(), room.objectives(), room.waves(),
                room.bossMobId(), events);
    }

}
