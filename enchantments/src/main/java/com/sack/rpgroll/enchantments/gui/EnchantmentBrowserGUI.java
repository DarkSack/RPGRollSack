package com.sack.rpgroll.enchantments.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.enchantments.core.CustomEnchantment;
import com.sack.rpgroll.enchantments.core.EnchantEffect;
import com.sack.rpgroll.enchantments.core.EnchantEffectType;
import com.sack.rpgroll.enchantments.core.EnchantmentManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class EnchantmentBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final EnchantmentManager enchantmentManager;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;
    private List<CustomEnchantment> enchantments;

    public EnchantmentBrowserGUI(Player player, EnchantmentManager enchantmentManager,
            ChatPromptManager chatPromptManager) {
        super(player, chatPromptManager.lang().component("enchantment_browser_gui.title"), SIZE);
        this.enchantmentManager = enchantmentManager;
        this.chatPromptManager = chatPromptManager;
        this.lang = chatPromptManager.lang();
        this.enchantments = List.copyOf(enchantmentManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < enchantments.size() && i < 36; i++) {

            CustomEnchantment enchantment = enchantments.get(i);

            setItem(i, new ItemBuilder(Material.ENCHANTED_BOOK)
                    .setName(Component.text(enchantment.id(), enchantment.rarity().color()))
                    .setLore(lang.component("enchantment_browser_gui.entry_lore_line1",
                            "rarity", enchantment.rarity(), "max_level", enchantment.maxLevel()),
                            lang.component("enchantment_browser_gui.entry_lore_line2"))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("enchantment_browser_gui.create_new"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("enchantment_browser_gui.close_button")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < enchantments.size() && slot < 36) {
            new EnchantmentEditorGUI(player, enchantments.get(slot), enchantmentManager, chatPromptManager,
                    this::reopen).open();
            return;
        }

        if (slot == NEW_SLOT) {
            promptNew();
            return;
        }

        if (slot == BACK_SLOT) {
            close();
        }
    }

    private void promptNew() {
        chatPromptManager.prompt(player, lang.raw("enchantment_browser_gui.prompt_new"), value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (enchantmentManager.exists(id)) {
                lang.send(player, "enchantment_browser_gui.already_exists");
                reopen();
                return;
            }

            CustomEnchantment enchantment = new CustomEnchantment(id, id, null, Set.of(), List.of(), 1, Map.of(),
                    List.of(), Set.of(), List.of(), 100,
                    List.of(new EnchantEffect(EnchantEffectType.MESSAGE, Map.of("value", "&7¡El encantamiento activó!"))));

            enchantmentManager.save(enchantment);
            reopen();
        });
    }

    private void reopen() {
        this.enchantments = List.copyOf(enchantmentManager.getAll());
        open();
    }

}
