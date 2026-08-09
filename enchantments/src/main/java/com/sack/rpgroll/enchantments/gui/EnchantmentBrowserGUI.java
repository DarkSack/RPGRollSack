package com.sack.rpgroll.enchantments.gui;

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
    private List<CustomEnchantment> enchantments;

    public EnchantmentBrowserGUI(Player player, EnchantmentManager enchantmentManager,
            ChatPromptManager chatPromptManager) {
        super(player, Component.text("Encantamientos RPGRoll", NamedTextColor.GOLD), SIZE);
        this.enchantmentManager = enchantmentManager;
        this.chatPromptManager = chatPromptManager;
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
                    .setLore(Component.text(enchantment.rarity() + " · Nivel máx. " + enchantment.maxLevel(),
                            NamedTextColor.GRAY),
                            Component.text("Click para editar", NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Crear encantamiento nuevo", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Cerrar"));
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
        chatPromptManager.prompt(player, "Escribí el id del nuevo encantamiento:", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (enchantmentManager.exists(id)) {
                player.sendMessage(Component.text("Ya existe un encantamiento con ese id.", NamedTextColor.RED));
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
