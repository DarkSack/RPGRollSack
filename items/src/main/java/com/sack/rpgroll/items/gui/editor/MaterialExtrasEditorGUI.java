package com.sack.rpgroll.items.gui.editor;

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
    private final Runnable onBack;

    public MaterialExtrasEditorGUI(Player player, EditorSession session, Runnable onBack) {
        super(player, Component.text("Extras de material: " + session.original.id(), NamedTextColor.GOLD), SIZE);
        this.session = session;
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
                    .setName(Component.text("Color (cuero/poción)", NamedTextColor.YELLOW))
                    .setLore(Component.text(session.dyeColor == null ? "(sin definir)" : session.dyeColor,
                            NamedTextColor.GRAY),
                            Component.text("Click para escribir", NamedTextColor.DARK_GRAY))
                    .build());
        }

        if (showSkull()) {
            setItem(SKULL_SLOT, new ItemBuilder(Material.PLAYER_HEAD)
                    .setName(Component.text("Textura de cabeza", NamedTextColor.YELLOW))
                    .setLore(Component.text(session.skullTexture == null ? "(sin definir)" : "(definida)",
                            NamedTextColor.GRAY),
                            Component.text("Click para pegar la textura", NamedTextColor.DARK_GRAY))
                    .build());
        }

        if (showTrim()) {
            setItem(TRIM_SLOT, new ItemBuilder(Material.IRON_CHESTPLATE)
                    .setName(Component.text("Trim de armadura", NamedTextColor.YELLOW))
                    .setLore(session.trim == null
                            ? Component.text("(sin definir)", NamedTextColor.GRAY)
                            : Component.text(session.trim.material() + " / " + session.trim.pattern(),
                                    NamedTextColor.GRAY))
                    .build());
        }

        if (!showColor() && !showSkull() && !showTrim()) {
            setItem(13, new ItemBuilder(Material.BARRIER)
                    .setName(Component.text("Sin extras para " + session.material.name(), NamedTextColor.RED))
                    .setLore(Component.text("Este material no admite color, textura de cabeza ni trim.",
                            NamedTextColor.GRAY))
                    .build());
        }

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
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
        session.chatPromptManager.prompt(player,
                "Escribí el color en hex (ej. FF0000), o 'borrar' para quitarlo:", value -> {
                    session.dyeColor = value.trim().equalsIgnoreCase("borrar") ? null : value.trim();
                    reopen();
                });
    }

    private void promptSkullTexture() {
        session.chatPromptManager.prompt(player,
                "Pegá la textura base64 (de minecraft-heads.com), o 'borrar' para quitarla:", value -> {
                    session.skullTexture = value.trim().equalsIgnoreCase("borrar") ? null : value.trim();
                    reopen();
                });
    }

    private void promptTrim() {
        session.chatPromptManager.prompt(player,
                "Escribí: <material> <patrón> (ej. REDSTONE COAST), o 'borrar' para quitarlo:", value -> {

                    if (value.trim().equalsIgnoreCase("borrar")) {
                        session.trim = null;
                        reopen();
                        return;
                    }

                    String[] parts = value.trim().split("\\s+");

                    if (parts.length != 2) {
                        player.sendMessage(Component.text("Formato inválido.", NamedTextColor.RED));
                        return;
                    }

                    session.trim = new ArmorTrimDef(parts[0].toUpperCase(), parts[1].toUpperCase());
                    reopen();
                });
    }

}
