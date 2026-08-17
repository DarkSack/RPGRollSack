package com.sack.rpgroll.dungeons.gui.editor;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.util.ComponentUtils;
import com.sack.rpgroll.dungeons.core.DungeonBounds;
import com.sack.rpgroll.dungeons.core.DungeonPoint;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Fija la región física (dos esquinas) y el punto de lobby parándose en
 * el lugar y haciendo click — sin comandos ni WorldEdit.
 */
public class RegionEditorGUI extends InventoryGUI {

    private static final int SIZE = 27;

    private static final int CORNER1_SLOT = 10;
    private static final int CORNER2_SLOT = 12;
    private static final int LOBBY_SLOT = 14;
    private static final int BACK_SLOT = 22;

    private final DungeonEditorSession session;
    private final Runnable onBack;
    private final LangManager lang;

    private Location corner1;
    private Location corner2;

    public RegionEditorGUI(Player player, DungeonEditorSession session, Runnable onBack) {
        super(player, ComponentUtils.parse(session.chatPromptManager.lang()
                .raw("gui.editor.region.title", "id", session.original.id())), SIZE);
        this.session = session;
        this.onBack = onBack;
        this.lang = session.chatPromptManager.lang();

        DungeonBounds bounds = session.bounds;
        var world = Bukkit.getWorld(bounds.world());

        if (world != null && !bounds.equals(DungeonBounds.none())) {
            this.corner1 = new Location(world, bounds.minX(), bounds.minY(), bounds.minZ());
            this.corner2 = new Location(world, bounds.maxX(), bounds.maxY(), bounds.maxZ());
        }
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        DungeonBounds bounds = session.bounds;

        setItem(CORNER1_SLOT, new ItemBuilder(Material.RED_WOOL)
                .setName(ComponentUtils.parse(lang.raw("gui.editor.region.corner1", "value", coordText(corner1))))
                .setLore(ComponentUtils.parse(lang.raw("gui.editor.region.corner.hint")))
                .build());

        setItem(CORNER2_SLOT, new ItemBuilder(Material.BLUE_WOOL)
                .setName(ComponentUtils.parse(lang.raw("gui.editor.region.corner2", "value", coordText(corner2))))
                .setLore(ComponentUtils.parse(lang.raw("gui.editor.region.corner.hint")))
                .build());

        setItem(LOBBY_SLOT, new ItemBuilder(Material.BEACON)
                .setName(ComponentUtils.parse(lang.raw("gui.editor.region.lobby.label",
                        "value", pointText(session.lobbyPoint))))
                .setLore(ComponentUtils.parse(lang.raw("gui.editor.region.lobby.hint")))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back")));
    }

    private String coordText(Location location) {
        return location == null ? lang.raw("gui.editor.region.unset")
                : String.format("%.0f, %.0f, %.0f", location.getX(), location.getY(), location.getZ());
    }

    private String pointText(DungeonPoint point) {
        return String.format("%s %.0f, %.0f, %.0f", point.world(), point.x(), point.y(), point.z());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == CORNER1_SLOT) {
            corner1 = player.getLocation();
            applyBoundsIfReady();
            build();
            return;
        }

        if (slot == CORNER2_SLOT) {
            corner2 = player.getLocation();
            applyBoundsIfReady();
            build();
            return;
        }

        if (slot == LOBBY_SLOT) {
            session.lobbyPoint = DungeonPoint.fromLocation(player.getLocation());
            build();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void applyBoundsIfReady() {

        if (corner1 == null || corner2 == null || corner1.getWorld() == null) {
            return;
        }

        session.bounds = new DungeonBounds(corner1.getWorld().getName(),
                corner1.getX(), corner1.getY(), corner1.getZ(),
                corner2.getX(), corner2.getY(), corner2.getZ());

        lang.send(player, "gui.editor.region.updated");
    }

}
