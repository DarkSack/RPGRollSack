package com.sack.rpgroll.items.gui.editor;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.items.core.ArmorTrimDef;
import com.sack.rpgroll.items.gui.ItemMaterialTraits;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Campos que solo tienen sentido para ciertos materiales: color de cuero/
 * poción, textura de cabeza y trim de armadura. Cada botón solo se dibuja
 * si {@link ItemMaterialTraits} confirma que el material actual del ítem
 * realmente lo soporta — así nunca aparece "textura de cabeza" al editar
 * una espada. Se abre desde {@link DisplayEditorGUI}, que ya filtra si
 * mostrar el botón de entrada según {@link ItemMaterialTraits#hasAnyMaterialExtras}.
 */
public class MaterialExtrasEditorGUI extends InventoryGUI {

    private static final int SIZE = 27;
    private static final int PREVIEW_SLOT = 4;

    private static final int COLOR_SLOT = 11;
    private static final int SKULL_SLOT = 13;
    private static final int TRIM_SLOT = 15;

    private static final int BACK_SLOT = 22;

    private final EditorSession session;
    private final LangManager lang;
    private final Runnable onBack;

    public MaterialExtrasEditorGUI(Player player, EditorSession session, Runnable onBack) {
        super(player, session.chatPromptManager.lang().component("editor.material_extras.title", "id", session.original.id()), SIZE);
        this.session = session;
        this.lang = session.chatPromptManager.lang();
        this.onBack = onBack;
    }

    private boolean showColor() {
        return ItemMaterialTraits.supportsDyeColor(session.material);
    }

    private boolean showSkull() {
        return ItemMaterialTraits.supportsSkullTexture(session.material);
    }

    private boolean showTrim() {
        return ItemMaterialTraits.supportsArmorTrim(session.material);
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(PREVIEW_SLOT, session.preview());

        if (showColor()) {
            setItem(COLOR_SLOT, new ItemBuilder(Material.LEATHER_CHESTPLATE)
                    .setName(lang.component("editor.material_extras.color_label"))
                    .setLore(ComponentUtils.parseWithDefault(session.dyeColor == null ? lang.raw("editor.rules.undefined") : session.dyeColor, NamedTextColor.GRAY),
                            lang.component("editor.material_extras.color_click"))
                    .build());
        }

        if (showSkull()) {
            setItem(SKULL_SLOT, new ItemBuilder(Material.PLAYER_HEAD)
                    .setName(lang.component("editor.material_extras.skull_label"))
                    .setLore(ComponentUtils.parseWithDefault(session.skullTexture == null ? lang.raw("editor.rules.undefined")
                                    : lang.raw("editor.material_extras.skull_defined"), NamedTextColor.GRAY),
                            lang.component("editor.material_extras.skull_click"))
                    .build());
        }

        if (showTrim()) {
            setItem(TRIM_SLOT, new ItemBuilder(Material.IRON_CHESTPLATE)
                    .setName(lang.component("editor.material_extras.trim_label"))
                    .setLore(session.trim == null
                            ? ComponentUtils.parseWithDefault(lang.raw("editor.rules.undefined"), NamedTextColor.GRAY)
                            : Component.text(session.trim.material() + " / " + session.trim.pattern(),
                                    NamedTextColor.GRAY))
                    .build());
        }

        if (!showColor() && !showSkull() && !showTrim()) {
            setItem(13, new ItemBuilder(Material.BARRIER)
                    .setName(lang.component("editor.material_extras.no_extras", "material", session.material.name()))
                    .setLore(lang.component("editor.material_extras.no_extras_desc"))
                    .build());
        }

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("editor.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == COLOR_SLOT && showColor()) {
            promptColor();
            return;
        }

        if (slot == SKULL_SLOT && showSkull()) {
            promptSkullTexture();
            return;
        }

        if (slot == TRIM_SLOT && showTrim()) {
            promptTrim();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void reopen() {
        new MaterialExtrasEditorGUI(player, session, onBack).open();
    }

    private void promptColor() {
        session.chatPromptManager.prompt(player, lang.raw("editor.material_extras.prompt_color"), value -> {
            session.dyeColor = value.trim().equalsIgnoreCase("borrar") ? null : value.trim();
            reopen();
        });
    }

    private void promptSkullTexture() {
        session.chatPromptManager.prompt(player, lang.raw("editor.material_extras.prompt_skull"), value -> {
            session.skullTexture = value.trim().equalsIgnoreCase("borrar") ? null : value.trim();
            reopen();
        });
    }

    private void promptTrim() {
        session.chatPromptManager.prompt(player, lang.raw("editor.material_extras.prompt_trim"), value -> {

            if (value.trim().equalsIgnoreCase("borrar")) {
                session.trim = null;
                reopen();
                return;
            }

            String[] parts = value.trim().split("\\s+");

            if (parts.length != 2) {
                lang.send(player, "editor.common.invalid_format");
                return;
            }

            session.trim = new ArmorTrimDef(parts[0].toUpperCase(), parts[1].toUpperCase());
            reopen();
        });
    }

}
