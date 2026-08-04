package com.sack.rpgroll.dungeons.gui.editor;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
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

    public RoomEditGUI(Player player, DungeonEditorSession session, int index, Runnable onBack) {
        super(player, Component.text("Sala: " + session.rooms.get(index).id(), NamedTextColor.GOLD), SIZE);
        this.session = session;
        this.index = index;
        this.onBack = onBack;
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
                .setName(Component.text("ID: " + room.id(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click para escribir uno nuevo", NamedTextColor.GRAY))
                .build());

        setItem(TYPE_SLOT, new ItemBuilder(Material.COMPARATOR)
                .setName(Component.text("Tipo: " + room.type(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click para pasar al siguiente tipo", NamedTextColor.GRAY))
                .build());

        setItem(ENTRY_POINT_SLOT, new ItemBuilder(Material.MAP)
                .setName(Component.text("Punto de entrada", NamedTextColor.YELLOW))
                .setLore(Component.text(pointText(room.entryPoint()), NamedTextColor.GRAY),
                        Component.text("Click para fijarlo en tu posición actual", NamedTextColor.GRAY))
                .build());

        setItem(BOSS_SLOT, new ItemBuilder(Material.WITHER_SKELETON_SKULL)
                .setName(Component.text("Jefe: " + (room.hasBoss() ? room.bossMobId() : "(ninguno)"),
                        NamedTextColor.YELLOW))
                .setLore(Component.text("Id de una definición de RPGRoll-Mobs", NamedTextColor.DARK_GRAY),
                        Component.text("Click: escribir · Shift-click: quitar", NamedTextColor.GRAY))
                .build());

        setItem(OBJECTIVES_SLOT, new ItemBuilder(Material.TARGET)
                .setName(Component.text("Objetivos: " + room.objectives().size(), NamedTextColor.YELLOW))
                .build());

        setItem(WAVES_SLOT, new ItemBuilder(Material.SPAWNER)
                .setName(Component.text("Oleadas: " + room.waves().size(), NamedTextColor.YELLOW))
                .build());

        setItem(EVENTS_SLOT, new ItemBuilder(Material.REDSTONE_TORCH)
                .setName(Component.text("Eventos de esta sala", NamedTextColor.YELLOW))
                .setLore(Component.text("Acciones por trigger, solo para esta sala", NamedTextColor.GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    private String pointText(DungeonPoint point) {
        return String.format("%s %.0f, %.0f, %.0f", point.world(), point.x(), point.y(), point.z());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == ID_SLOT) {
            session.chatPromptManager.prompt(player, "Escribí el nuevo id:", value -> {
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
            session.chatPromptManager.prompt(player, "Escribí el id del mob jefe (RPGRoll-Mobs):", value -> {
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
