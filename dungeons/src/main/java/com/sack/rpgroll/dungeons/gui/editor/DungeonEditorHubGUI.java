package com.sack.rpgroll.dungeons.gui.editor;

import com.sack.rpgroll.util.ComponentUtils;

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
        super(player, ComponentUtils.parse(session.chatPromptManager.lang()
                .raw("gui.editor.hub.title", "id", session.original.id())), SIZE);
        this.session = session;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(PREVIEW_SLOT, previewIcon());

        var lang = session.chatPromptManager.lang();

        setItem(INFO_SLOT, categoryButton(Material.NAME_TAG, lang.raw("gui.editor.hub.info.name"),
                lang.raw("gui.editor.hub.info.description")));
        setItem(REGION_SLOT, categoryButton(Material.MAP, lang.raw("gui.editor.hub.region.name"),
                lang.raw("gui.editor.hub.region.description")));
        setItem(ROOMS_SLOT, categoryButton(Material.OAK_DOOR, lang.raw("gui.editor.hub.rooms.name"),
                lang.raw("gui.editor.hub.rooms.description")));
        setItem(DIFFICULTIES_SLOT, categoryButton(Material.NETHER_STAR, lang.raw("gui.editor.hub.difficulties.name"),
                lang.raw("gui.editor.hub.difficulties.description")));
        setItem(LOOT_SLOT, categoryButton(Material.CHEST, lang.raw("gui.editor.hub.loot.name"),
                lang.raw("gui.editor.hub.loot.description")));
        setItem(TRIGGERS_SLOT, categoryButton(Material.COMPARATOR, lang.raw("gui.editor.hub.triggers.name"),
                lang.raw("gui.editor.hub.triggers.description")));
        setItem(CHECKPOINTS_SLOT, categoryButton(Material.RESPAWN_ANCHOR, lang.raw("gui.editor.hub.checkpoints.name"),
                lang.raw("gui.editor.hub.checkpoints.description")));

        setItem(SAVE_SLOT, ItemBuilder.createConfirmButton(lang.raw("gui.common.save")));
        setItem(CANCEL_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.cancel")));
    }

    private ItemStack previewIcon() {

        Material material;
        try {
            material = Material.valueOf(session.icon.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            material = Material.STONE_BRICKS;
        }

        return new ItemBuilder(material)
                .setName(ComponentUtils.parse(session.displayName)
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
            session.chatPromptManager.lang().send(player, "gui.editor.hub.save.ok");
        } catch (java.io.IOException e) {
            session.chatPromptManager.lang().send(player, "gui.editor.hub.save.error", "error", e.getMessage());
        }

        markSelectionMade();
        close();
    }

}
