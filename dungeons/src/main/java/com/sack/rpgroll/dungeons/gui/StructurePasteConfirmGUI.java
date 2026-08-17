package com.sack.rpgroll.dungeons.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.dungeons.structure.StructureDefinition;
import com.sack.rpgroll.dungeons.structure.StructurePasteEngine;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Confirmación de pegado de una estructura de la Biblioteca — "admin
 * normal": pará donde querés la esquina/ancla, abrí esto, elegí rotación
 * si hace falta, click en Pegar. Sin coordenadas a mano (eso es
 * {@code /dungeonadmin structure paste <id> <mundo> <x> <y> <z>}, para
 * cuando sí las necesitás).
 */
public class StructurePasteConfirmGUI extends InventoryGUI {

    private static final int SIZE = 27;

    private static final int INFO_SLOT = 4;
    private static final int ROTATION_SLOT = 11;
    private static final int MIRROR_SLOT = 13;
    private static final int CONFIRM_SLOT = 15;
    private static final int BACK_SLOT = 22;

    private final StructureDefinition definition;
    private final StructurePasteEngine pasteEngine;
    private final LangManager lang;
    private final Runnable onBack;

    private StructureRotation rotation = StructureRotation.NONE;
    private Mirror mirror = Mirror.NONE;

    public StructurePasteConfirmGUI(Player player, StructureDefinition definition, StructurePasteEngine pasteEngine,
            LangManager lang, Runnable onBack) {
        super(player, ComponentUtils.parse(lang.raw("gui.pasteconfirm.title", "id", definition.id())), SIZE);
        this.definition = definition;
        this.pasteEngine = pasteEngine;
        this.lang = lang;
        this.onBack = onBack;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(INFO_SLOT, new ItemBuilder(definition.icon())
                .setName(ComponentUtils.parse(definition.displayName()).colorIfAbsent(NamedTextColor.WHITE))
                .setLore(
                        Component.text(definition.description(), NamedTextColor.GRAY),
                        ComponentUtils.parse(lang.raw("gui.pasteconfirm.centered_on_you")))
                .build());

        setItem(ROTATION_SLOT, new ItemBuilder(Material.COMPASS)
                .setName(ComponentUtils.parse(lang.raw("gui.pasteconfirm.rotation", "value", rotationLabel(rotation))))
                .setLore(ComponentUtils.parse(lang.raw("gui.pasteconfirm.rotate_hint")))
                .build());

        setItem(MIRROR_SLOT, new ItemBuilder(Material.GLASS_PANE)
                .setName(ComponentUtils.parse(lang.raw("gui.pasteconfirm.mirror_label", "value", mirrorLabel(mirror))))
                .setLore(ComponentUtils.parse(lang.raw("gui.common.click_to_change")))
                .build());

        setItem(CONFIRM_SLOT, ItemBuilder.createConfirmButton(lang.raw("gui.pasteconfirm.confirm")));
        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back")));
    }

    private String rotationLabel(StructureRotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_90 -> "90°";
            case CLOCKWISE_180 -> "180°";
            case COUNTERCLOCKWISE_90 -> "270°";
            default -> "0°";
        };
    }

    private String mirrorLabel(Mirror mirror) {
        return switch (mirror) {
            case LEFT_RIGHT -> lang.raw("gui.pasteconfirm.mirror.left_right");
            case FRONT_BACK -> lang.raw("gui.pasteconfirm.mirror.front_back");
            default -> lang.raw("gui.pasteconfirm.mirror.none");
        };
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == ROTATION_SLOT) {
            rotation = switch (rotation) {
                case NONE -> StructureRotation.CLOCKWISE_90;
                case CLOCKWISE_90 -> StructureRotation.CLOCKWISE_180;
                case CLOCKWISE_180 -> StructureRotation.COUNTERCLOCKWISE_90;
                case COUNTERCLOCKWISE_90 -> StructureRotation.NONE;
            };
            build();
            return;
        }

        if (slot == MIRROR_SLOT) {
            mirror = switch (mirror) {
                case NONE -> Mirror.LEFT_RIGHT;
                case LEFT_RIGHT -> Mirror.FRONT_BACK;
                case FRONT_BACK -> Mirror.NONE;
            };
            build();
            return;
        }

        if (slot == CONFIRM_SLOT) {
            boolean placed = pasteEngine.place(definition, player.getLocation(), rotation, mirror);
            lang.send(player, placed ? "command.dungeonadmin.structure.paste.ok"
                    : "command.dungeonadmin.structure.paste.failed", "id", definition.id());
            markSelectionMade();
            close();
            return;
        }

        if (slot == BACK_SLOT) {
            markSelectionMade();
            onBack.run();
        }
    }

}
