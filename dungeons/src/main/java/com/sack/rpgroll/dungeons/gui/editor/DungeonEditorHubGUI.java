package com.sack.rpgroll.dungeons.gui.editor;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

/**
 * Punto de entrada del Dungeon Studio: un botón por componente
 * (Información, Región/Lobby, Salas, Dificultades, Loot, Triggers,
 * Checkpoints/Revivir). Todos comparten la misma {@link DungeonEditorSession}.
 */
public class DungeonEditorHubGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int PREVIEW_SLOT = 4;

    private static final int INFO_SLOT = 10;
    private static final int REGION_SLOT = 11;
    private static final int ROOMS_SLOT = 12;
    private static final int DIFFICULTIES_SLOT = 13;
    private static final int LOOT_SLOT = 14;
    private static final int TRIGGERS_SLOT = 15;
    private static final int CHECKPOINTS_SLOT = 16;

    private static final int SAVE_SLOT = 39;
    private static final int CANCEL_SLOT = 41;

    private final DungeonEditorSession session;

    public DungeonEditorHubGUI(Player player, DungeonEditorSession session) {
        super(player, Component.text("Editando: " + session.original.id(), NamedTextColor.GOLD), SIZE);
        this.session = session;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(PREVIEW_SLOT, previewIcon());

        setItem(INFO_SLOT, categoryButton(Material.NAME_TAG, "Información",
                "Nombre, categoría, ícono, descripción, nivel, jugadores, cooldown"));
        setItem(REGION_SLOT, categoryButton(Material.MAP, "Región y Lobby",
                "Esquinas del área física y punto de aparición"));
        setItem(ROOMS_SLOT, categoryButton(Material.OAK_DOOR, "Salas",
                "Secuencia de habitaciones: objetivos, oleadas, jefe"));
        setItem(DIFFICULTIES_SLOT, categoryButton(Material.NETHER_STAR, "Dificultades",
                "Normal/Hard/Elite/Mythic/Nightmare y sus modificadores"));
        setItem(LOOT_SLOT, categoryButton(Material.CHEST, "Loot",
                "Recompensas al completar la mazmorra"));
        setItem(TRIGGERS_SLOT, categoryButton(Material.COMPARATOR, "Triggers",
                "Acciones por evento (inicio, sala, oleada, jefe...)"));
        setItem(CHECKPOINTS_SLOT, categoryButton(Material.RESPAWN_ANCHOR, "Checkpoints y Revivir",
                "Qué pasa al morir el grupo, y cómo se revive"));

        setItem(SAVE_SLOT, ItemBuilder.createConfirmButton("Guardar"));
        setItem(CANCEL_SLOT, ItemBuilder.createCancelButton("Cancelar"));
    }

    private ItemStack previewIcon() {

        Material material;
        try {
            material = Material.valueOf(session.icon.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            material = Material.STONE_BRICKS;
        }

        return new ItemBuilder(material)
                .setName(LegacyComponentSerializer.legacyAmpersand().deserialize(session.displayName)
                        .colorIfAbsent(NamedTextColor.WHITE))
                .setLore(Component.text(session.original.id(), NamedTextColor.DARK_GRAY),
                        Component.text(session.rooms.size() + " sala(s)", NamedTextColor.GRAY))
                .build();
    }

    private ItemStack categoryButton(Material material, String name, String description) {
        return new ItemBuilder(material)
                .setName(Component.text(name, NamedTextColor.YELLOW))
                .setLore(Component.text(description, NamedTextColor.GRAY))
                .build();
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);

        switch (event.getSlot()) {
            case INFO_SLOT -> new InfoEditorGUI(player, session, this::reopen).open();
            case REGION_SLOT -> new RegionEditorGUI(player, session, this::reopen).open();
            case ROOMS_SLOT -> new RoomsEditorGUI(player, session, this::reopen).open();
            case DIFFICULTIES_SLOT -> new DifficultiesEditorGUI(player, session, this::reopen).open();
            case LOOT_SLOT -> new DungeonLootEditorGUI(player, session, this::reopen).open();
            case TRIGGERS_SLOT -> new DungeonTriggersEditorGUI(player, session, this::reopen).open();
            case CHECKPOINTS_SLOT -> new CheckpointsReviveEditorGUI(player, session, this::reopen).open();
            case SAVE_SLOT -> save();
            case CANCEL_SLOT -> close();
            default -> {
            }
        }
    }

    private void reopen() {
        new DungeonEditorHubGUI(player, session).open();
    }

    private void save() {

        try {
            session.save();
            player.sendMessage(Component.text("✔ Mazmorra guardada.", NamedTextColor.GREEN));
        } catch (java.io.IOException e) {
            player.sendMessage(Component.text("✘ Error guardando la mazmorra: " + e.getMessage(),
                    NamedTextColor.RED));
        }

        markSelectionMade();
        close();
    }

}
