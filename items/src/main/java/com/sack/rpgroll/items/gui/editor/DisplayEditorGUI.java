package com.sack.rpgroll.items.gui.editor;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.items.gui.ItemMaterialTraits;
import com.sack.rpgroll.items.gui.MaterialPickerGUI;
import com.sack.rpgroll.items.gui.RarityPickerGUI;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Editor de apariencia: material, nombre, lore, rareza, brillo, custom
 * model data y flags de ítem. Color de cuero/poción, textura de cabeza y
 * trim de armadura viven en {@link MaterialExtrasEditorGUI}, accesible
 * solo si el material actual admite al menos uno de esos tres.
 */
public class DisplayEditorGUI extends InventoryGUI {

    private static final int SIZE = 54;
    private static final int PREVIEW_SLOT = 4;

    private static final int MATERIAL_SLOT = 10;
    private static final int RENAME_SLOT = 11;
    private static final int LORE_SLOT = 12;
    private static final int CMD_SLOT = 13;
    private static final int RARITY_SLOT = 14;
    private static final int GLOW_SLOT = 15;
    private static final int UNBREAKABLE_SLOT = 16;

    private static final int MATERIAL_EXTRAS_SLOT = 22;

    private static final int FLAGS_LABEL_SLOT = 27;
    private static final int FLAGS_START_SLOT = 28;
    private static final String[] TOGGLEABLE_FLAGS = {
            "HIDE_ENCHANTS", "HIDE_ATTRIBUTES", "HIDE_UNBREAKABLE", "HIDE_DYE",
            "HIDE_ARMOR_TRIM", "HIDE_POTION_EFFECTS", "HIDE_ADDITIONAL_TOOLTIP", "HIDE_DESTROYS"
    };

    private static final int BACK_SLOT = 49;

    private final EditorSession session;
    private final LangManager lang;
    private final Runnable onBack;

    public DisplayEditorGUI(Player player, EditorSession session, Runnable onBack) {
        super(player, session.chatPromptManager.lang().component("editor.display.title", "id", session.original.id()), SIZE);
        this.session = session;
        this.lang = session.chatPromptManager.lang();
        this.onBack = onBack;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        // Fila de "extras de material" (18-26) y fila de control (36-53, salvo
        // Volver) usan un vidrio distinto al filler gris de contenido — marca a
        // simple vista qué es una sección propia y qué es navegación.
        for (int slot = 18; slot <= 26; slot++) {
            setItem(slot, sectionGlass());
        }
        for (int slot = 36; slot < SIZE; slot++) {
            setItem(slot, controlGlass());
        }

        setItem(PREVIEW_SLOT, session.preview());

        setItem(MATERIAL_SLOT, new ItemBuilder(session.material)
                .setName(lang.component("editor.display.material_label", "material", session.material.name()))
                .setLore(lang.component("editor.display.click_to_choose"))
                .build());

        setItem(RENAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(lang.component("editor.display.rename"))
                .setLore(lang.component("editor.display.current", "value", session.displayName))
                .build());

        setItem(LORE_SLOT, new ItemBuilder(Material.WRITABLE_BOOK)
                .setName(lang.component("editor.display.edit_lore"))
                .setLore(loreSummary())
                .build());

        setItem(CMD_SLOT, new ItemBuilder(Material.PAPER)
                .setName(lang.component("editor.display.cmd_label"))
                .setLore(Component.text(session.customModelData == null ? lang.raw("editor.display.cmd_undefined")
                        : String.valueOf(session.customModelData), NamedTextColor.GRAY),
                        lang.component("editor.display.cmd_hint"))
                .build());

        setItem(RARITY_SLOT, new ItemBuilder(Material.NETHER_STAR)
                .setName(lang.component("editor.display.rarity_label", "rarity", session.rarityId))
                .setLore(lang.component("editor.display.click_to_choose"))
                .build());

        setItem(GLOW_SLOT, new ItemBuilder(glowMaterial())
                .setName(lang.component("editor.display.glow_label", "value", glowLabel()))
                .setLore(lang.component("editor.display.glow_hint"))
                .build());

        setItem(UNBREAKABLE_SLOT, new ItemBuilder(session.unbreakable ? Material.NETHERITE_INGOT : Material.IRON_INGOT)
                .setName(lang.component("editor.display.unbreakable_label",
                        "value", session.unbreakable ? "SI" : "NO"))
                .build());

        boolean hasExtras = ItemMaterialTraits.hasAnyMaterialExtras(session.material);

        setItem(MATERIAL_EXTRAS_SLOT, new ItemBuilder(hasExtras ? Material.CHEST : Material.BARRIER)
                .setName(lang.component("editor.display.material_extras")
                        .color(hasExtras ? NamedTextColor.YELLOW : NamedTextColor.GRAY))
                .setLore(hasExtras
                        ? lang.component("editor.display.material_extras_desc")
                        : lang.component("editor.display.material_extras_unsupported",
                                "material", session.material.name()))
                .build());

        setItem(FLAGS_LABEL_SLOT, new ItemBuilder(Material.LIGHT_GRAY_STAINED_GLASS_PANE)
                .setName(lang.component("editor.display.flags_label"))
                .setLore(lang.component("editor.display.flags_desc"))
                .build());

        for (int i = 0; i < TOGGLEABLE_FLAGS.length; i++) {

            String flag = TOGGLEABLE_FLAGS[i];
            boolean active = session.flags.contains(flag);

            setItem(FLAGS_START_SLOT + i, new ItemBuilder(active ? Material.LIME_DYE : Material.GRAY_DYE)
                    .setName(Component.text(flag, active ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                    .build());
        }

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("editor.common.back")));
    }

    private List<Component> loreSummary() {

        List<Component> lore = new ArrayList<>();

        if (session.lore.isEmpty()) {
            lore.add(lang.component("editor.display.no_lore"));
        } else {
            for (String line : session.lore) {
                lore.add(Component.text(line, NamedTextColor.GRAY));
            }
        }

        lore.add(lang.component("editor.display.lore_click_replace"));

        return lore;
    }

    private Material glowMaterial() {
        if (session.glowOverride == null) {
            return Material.GLOWSTONE;
        }
        return session.glowOverride ? Material.GLOWSTONE_DUST : Material.GUNPOWDER;
    }

    private String glowLabel() {
        if (session.glowOverride == null) {
            return lang.raw("editor.display.glow_inherit");
        }
        return session.glowOverride ? "SI" : "NO";
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);

        switch (event.getSlot()) {
            case MATERIAL_SLOT -> openMaterialPicker();
            case RENAME_SLOT -> promptRename();
            case LORE_SLOT -> promptLore();
            case CMD_SLOT -> handleCustomModelData(event);
            case RARITY_SLOT -> openRarityPicker();
            case GLOW_SLOT -> cycleGlow();
            case UNBREAKABLE_SLOT -> {
                session.unbreakable = !session.unbreakable;
                build();
            }
            case MATERIAL_EXTRAS_SLOT -> openMaterialExtras();
            case BACK_SLOT -> onBack.run();
            default -> handleFlagClick(event.getSlot());
        }
    }

    private void handleFlagClick(int slot) {

        int index = slot - FLAGS_START_SLOT;
        if (index < 0 || index >= TOGGLEABLE_FLAGS.length) {
            return;
        }

        String flag = TOGGLEABLE_FLAGS[index];

        if (!session.flags.remove(flag)) {
            session.flags.add(flag);
        }

        build();
    }

    private void openMaterialPicker() {
        new MaterialPickerGUI(player, session.chatPromptManager, material -> {
            session.material = material;
            reopen();
        }, this::reopen).open();
    }

    private void openRarityPicker() {
        new RarityPickerGUI(player, session.rarityManager, lang, rarityId -> {
            session.rarityId = rarityId;
            reopen();
        }, this::reopen).open();
    }

    private void reopen() {
        new DisplayEditorGUI(player, session, onBack).open();
    }

    private void promptRename() {
        session.chatPromptManager.prompt(player, lang.raw("editor.display.prompt_rename"), value -> {
            session.displayName = value;
            build();
        });
    }

    private void promptLore() {
        session.chatPromptManager.prompt(player, lang.raw("editor.display.prompt_lore"), value -> {
            session.lore = new ArrayList<>(List.of(value.split("\\|")));
            build();
        });
    }

    private void handleCustomModelData(InventoryClickEvent event) {

        if (event.isShiftClick()) {
            session.customModelData = null;
            build();
            return;
        }

        session.chatPromptManager.prompt(player, lang.raw("editor.display.prompt_cmd"), value -> {
            try {
                session.customModelData = Integer.parseInt(value.trim());
            } catch (NumberFormatException e) {
                lang.send(player, "editor.common.invalid_number");
                return;
            }
            build();
        });
    }

    private void cycleGlow() {

        if (session.glowOverride == null) {
            session.glowOverride = true;
        } else if (session.glowOverride) {
            session.glowOverride = false;
        } else {
            session.glowOverride = null;
        }

        build();
    }

    private void openMaterialExtras() {

        if (!ItemMaterialTraits.hasAnyMaterialExtras(session.material)) {
            lang.send(player, "editor.display.material_extras_error", "material", session.material.name());
            return;
        }

        new MaterialExtrasEditorGUI(player, session, this::reopen).open();
    }

    private org.bukkit.inventory.ItemStack sectionGlass() {
        return new ItemBuilder(Material.ORANGE_STAINED_GLASS_PANE)
                .setName(Component.text(" ", NamedTextColor.GRAY))
                .build();
    }

    private org.bukkit.inventory.ItemStack controlGlass() {
        return new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                .setName(Component.text(" ", NamedTextColor.GRAY))
                .build();
    }

}
