package com.sack.rpgroll.items.gui.editor;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Editor de encantamientos vanilla + RPGRoll-Enchantments (custom). */
public class EnchantmentsEditorGUI extends InventoryGUI {

    private static final int SIZE = 54;
    private static final int VANILLA_START = 0;
    private static final int VANILLA_ROWS_SLOTS = 18;
    private static final int ADD_VANILLA_SLOT = 26;

    private static final int CUSTOM_START = 27;
    private static final int CUSTOM_ROWS_SLOTS = 18;
    private static final int ADD_CUSTOM_SLOT = 44;

    private static final int BACK_SLOT = 49;

    private final EditorSession session;
    private final LangManager lang;
    private final Runnable onBack;
    private List<String> vanillaKeys;
    private List<String> customKeys;

    public EnchantmentsEditorGUI(Player player, EditorSession session, Runnable onBack) {
        super(player, session.chatPromptManager.lang().component("editor.enchantments.title", "id", session.original.id()), SIZE);
        this.session = session;
        this.lang = session.chatPromptManager.lang();
        this.onBack = onBack;
        refreshKeys();
    }

    private void refreshKeys() {
        vanillaKeys = new ArrayList<>(session.vanillaEnchantments.keySet());
        customKeys = new ArrayList<>(session.customEnchantments.keySet());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        // Fila 18-26 separa "vanilla" de "RPGRoll-Enchantments"; 45-53 es la
        // fila de control (solo Volver acá).
        for (int slot = 18; slot <= 26; slot++) {
            setItem(slot, new ItemBuilder(Material.PURPLE_STAINED_GLASS_PANE)
                    .setName(lang.component("editor.enchantments.custom_section")).build());
        }
        for (int slot = 45; slot < SIZE; slot++) {
            setItem(slot, new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                    .setName(Component.text(" ", NamedTextColor.GRAY)).build());
        }

        for (int i = 0; i < vanillaKeys.size() && i < VANILLA_ROWS_SLOTS; i++) {

            String key = vanillaKeys.get(i);
            int level = session.vanillaEnchantments.get(key);

            setItem(VANILLA_START + i, new ItemBuilder(Material.ENCHANTED_BOOK)
                    .setName(Component.text(key + " " + level, NamedTextColor.AQUA))
                    .setLore(lang.component("editor.common.shift_click_remove"))
                    .build());
        }

        setItem(ADD_VANILLA_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("editor.enchantments.add_vanilla"))
                .build());

        for (int i = 0; i < customKeys.size() && i < CUSTOM_ROWS_SLOTS; i++) {

            String key = customKeys.get(i);
            int level = session.customEnchantments.get(key);

            setItem(CUSTOM_START + i, new ItemBuilder(Material.BOOK)
                    .setName(Component.text(key + " " + level, NamedTextColor.LIGHT_PURPLE))
                    .setLore(lang.component("editor.enchantments.custom_section").color(NamedTextColor.DARK_GRAY),
                            lang.component("editor.common.shift_click_remove"))
                    .build());
        }

        setItem(ADD_CUSTOM_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("editor.enchantments.add_custom"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("editor.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot >= VANILLA_START && slot < VANILLA_START + vanillaKeys.size() && slot < VANILLA_ROWS_SLOTS) {

            if (event.isShiftClick()) {
                session.vanillaEnchantments.remove(vanillaKeys.get(slot - VANILLA_START));
                refreshKeys();
                build();
            }
            return;
        }

        if (slot == ADD_VANILLA_SLOT) {
            promptAdd(true);
            return;
        }

        if (slot >= CUSTOM_START && slot < CUSTOM_START + customKeys.size() && slot - CUSTOM_START < CUSTOM_ROWS_SLOTS) {

            if (event.isShiftClick()) {
                session.customEnchantments.remove(customKeys.get(slot - CUSTOM_START));
                refreshKeys();
                build();
            }
            return;
        }

        if (slot == ADD_CUSTOM_SLOT) {
            promptAdd(false);
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void promptAdd(boolean vanilla) {
        session.chatPromptManager.prompt(player,
                lang.raw("editor.enchantments.prompt_add", "example", vanilla
                        ? lang.raw("editor.enchantments.example_vanilla")
                        : lang.raw("editor.enchantments.example_custom")),
                value -> {

                    String[] parts = value.trim().split("\\s+");

                    if (parts.length != 2) {
                        lang.send(player, "editor.common.invalid_format");
                        return;
                    }

                    try {
                        int level = Integer.parseInt(parts[1]);
                        Map<String, Integer> target = vanilla ? session.vanillaEnchantments : session.customEnchantments;
                        target.put(vanilla ? parts[0].toUpperCase(Locale.ROOT) : parts[0].toLowerCase(Locale.ROOT),
                                level);
                    } catch (NumberFormatException e) {
                        lang.send(player, "editor.enchantments.invalid_level");
                        return;
                    }

                    refreshKeys();
                    build();
                });
    }

}
