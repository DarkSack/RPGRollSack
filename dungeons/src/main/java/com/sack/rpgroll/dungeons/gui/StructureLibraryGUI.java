package com.sack.rpgroll.dungeons.gui;

import com.sack.rpgroll.dungeons.structure.StructureDefinition;
import com.sack.rpgroll.dungeons.structure.StructureLibrary;
import com.sack.rpgroll.dungeons.structure.StructurePasteEngine;
import com.sack.rpgroll.dungeons.structure.StructureSourceType;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

/**
 * Biblioteca de Estructuras: grid paginado de todo lo cargado desde
 * {@code structures/*.yml} (CUSTOM y NATIVE mezclados). Click abre la
 * confirmación de pegado — sin edición acá, las estructuras se autoran
 * por YAML (CUSTOM) o Structure Block + {@code /dungeonadmin structure
 * import} (NATIVE), no con un formulario de GUI.
 */
public class StructureLibraryGUI extends PaginatedGUI {

    private static final int SIZE = 54;
    private static final int CONTENT_SLOTS = 36;

    private static final int PREV_SLOT = 45;
    private static final int BACK_SLOT = 49;
    private static final int NEXT_SLOT = 53;

    private final StructureLibrary library;
    private final StructurePasteEngine pasteEngine;

    private List<StructureDefinition> entries;

    public StructureLibraryGUI(Player player, StructureLibrary library, StructurePasteEngine pasteEngine) {
        super(player, Component.text("Biblioteca de Estructuras", NamedTextColor.GOLD), SIZE, CONTENT_SLOTS);
        this.library = library;
        this.pasteEngine = pasteEngine;
        refresh();
    }

    private void refresh() {
        entries = library.getAll().stream()
                .sorted((a, b) -> a.id().compareToIgnoreCase(b.id()))
                .toList();
        page = 0;
    }

    @Override
    protected int totalItemCount() {
        return entries.size();
    }

    @Override
    protected void renderItem(int contentSlot, int absoluteIndex) {

        StructureDefinition def = entries.get(absoluteIndex);
        boolean native_ = def.sourceType() == StructureSourceType.NATIVE;

        String size = native_
                ? "Tamaño real al pegar"
                : def.width() + "x" + def.height() + "x" + def.depth() + " (X·Y·Z)";

        setItem(contentSlot, new ItemBuilder(def.icon())
                .setName(ComponentUtils.parse(def.displayName()).colorIfAbsent(NamedTextColor.WHITE))
                .setLore(
                        Component.text(def.id(), NamedTextColor.DARK_GRAY),
                        Component.text(native_ ? "Fuente: Structure Block" : "Fuente: propia (YAML)",
                                native_ ? NamedTextColor.LIGHT_PURPLE : NamedTextColor.AQUA),
                        Component.text(size, NamedTextColor.GRAY),
                        Component.text(def.description(), NamedTextColor.GRAY),
                        Component.text("Click para pegar", NamedTextColor.YELLOW))
                .build());
    }

    @Override
    protected void renderExtras() {

        setItem(PREV_SLOT, hasPreviousPage()
                ? new ItemBuilder(Material.ARROW).setName(Component.text("« Anterior", NamedTextColor.YELLOW)).build()
                : ItemBuilder.createFiller());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Cerrar"));

        setItem(NEXT_SLOT, hasNextPage()
                ? new ItemBuilder(Material.ARROW).setName(Component.text("Siguiente »", NamedTextColor.YELLOW)).build()
                : ItemBuilder.createFiller());

        for (int slot = CONTENT_SLOTS; slot < SIZE; slot++) {
            if (slot != PREV_SLOT && slot != BACK_SLOT && slot != NEXT_SLOT) {
                setItem(slot, ItemBuilder.createFiller());
            }
        }
    }

    @Override
    protected void onItemClick(InventoryClickEvent event, int absoluteIndex) {

        StructureDefinition def = entries.get(absoluteIndex);
        new StructurePasteConfirmGUI(player, def, pasteEngine, () -> {
            refresh();
            open();
        }).open();
    }

    @Override
    protected void onExtraClick(InventoryClickEvent event) {

        switch (event.getSlot()) {
            case PREV_SLOT -> previousPage();
            case NEXT_SLOT -> nextPage();
            case BACK_SLOT -> close();
            default -> {
            }
        }
    }

}
