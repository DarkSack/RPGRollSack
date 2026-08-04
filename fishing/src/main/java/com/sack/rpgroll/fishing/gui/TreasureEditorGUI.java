package com.sack.rpgroll.fishing.gui;

import com.sack.rpgroll.fishing.core.FishRarity;
import com.sack.rpgroll.fishing.core.Treasure;
import com.sack.rpgroll.fishing.core.TreasureManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

public class TreasureEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int NAME_SLOT = 10;
    private static final int ICON_SLOT = 11;
    private static final int DESCRIPTION_SLOT = 12;
    private static final int RARITY_SLOT = 13;
    private static final int REWARD_SLOT = 14;
    private static final int WEIGHT_SLOT = 15;
    private static final int BACK_SLOT = 40;

    private final TreasureManager treasureManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private Treasure current;

    public TreasureEditorGUI(Player player, Treasure treasure, TreasureManager treasureManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text("Tesoro: " + treasure.id(), NamedTextColor.GOLD), SIZE);
        this.current = treasure;
        this.treasureManager = treasureManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(Treasure updated) {
        current = updated;
        treasureManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(Component.text("Nombre: " + current.displayName(), NamedTextColor.YELLOW)).build());

        setItem(ICON_SLOT, new ItemBuilder(SpeciesBrowserGUI.parseMaterial(current.icon()))
                .setName(Component.text("Ícono: " + current.icon(), NamedTextColor.YELLOW)).build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(Component.text("Descripción", NamedTextColor.YELLOW))
                .setLore(ItemBuilder.toLoreLines(
                        current.description().isBlank() ? "(sin descripción)" : current.description()))
                .build());

        setItem(RARITY_SLOT, new ItemBuilder(Material.NETHER_STAR)
                .setName(Component.text("Rareza: " + current.rarity(), NamedTextColor.LIGHT_PURPLE))
                .setLore(Component.text("Click para pasar a la siguiente", NamedTextColor.GRAY)).build());

        setItem(REWARD_SLOT, new ItemBuilder(SpeciesBrowserGUI.parseMaterial(current.rewardMaterial()),
                current.rewardAmount())
                .setName(Component.text("Recompensa: " + current.rewardAmount() + "x " + current.rewardMaterial(),
                        NamedTextColor.GOLD))
                .setLore(Component.text("Escribí: MATERIAL,CANTIDAD", NamedTextColor.GRAY)).build());

        setItem(WEIGHT_SLOT, new ItemBuilder(Material.GOLD_NUGGET)
                .setName(Component.text("Peso relativo: " + current.weight(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +1 · Click derecho: -1", NamedTextColor.GRAY)).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        int sign = event.getClick() == ClickType.RIGHT ? -1 : 1;

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nuevo nombre:", value -> replace(new Treasure(current.id(),
                    value, current.icon(), current.description(), current.rarity(), current.rewardMaterial(),
                    current.rewardAmount(), current.weight())));
            return;
        }

        if (slot == ICON_SLOT) {
            chatPromptManager.prompt(player, "Escribí el Material del ícono:", value -> replace(new Treasure(
                    current.id(), current.displayName(), value, current.description(), current.rarity(),
                    current.rewardMaterial(), current.rewardAmount(), current.weight())));
            return;
        }

        if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, "Escribí la nueva descripción:", value -> replace(new Treasure(
                    current.id(), current.displayName(), current.icon(), value, current.rarity(),
                    current.rewardMaterial(), current.rewardAmount(), current.weight())));
            return;
        }

        if (slot == RARITY_SLOT) {
            FishRarity[] values = FishRarity.values();
            FishRarity next = values[(current.rarity().ordinal() + 1) % values.length];
            replace(new Treasure(current.id(), current.displayName(), current.icon(), current.description(), next,
                    current.rewardMaterial(), current.rewardAmount(), current.weight()));
            return;
        }

        if (slot == REWARD_SLOT) {
            chatPromptManager.prompt(player, "Escribí: MATERIAL,CANTIDAD:", value -> {

                String[] parts = value.split(",");
                String material = parts[0].trim().toUpperCase(java.util.Locale.ROOT);
                int amount = parts.length > 1 ? parseIntOr(parts[1].trim(), 1) : 1;

                replace(new Treasure(current.id(), current.displayName(), current.icon(), current.description(),
                        current.rarity(), material, amount, current.weight()));
            });
            return;
        }

        if (slot == WEIGHT_SLOT) {
            replace(new Treasure(current.id(), current.displayName(), current.icon(), current.description(),
                    current.rarity(), current.rewardMaterial(), current.rewardAmount(),
                    Math.max(0.01, current.weight() + sign)));
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private int parseIntOr(String raw, int fallback) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

}
