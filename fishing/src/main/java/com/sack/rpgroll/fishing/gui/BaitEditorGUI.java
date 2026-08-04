package com.sack.rpgroll.fishing.gui;

import com.sack.rpgroll.fishing.core.Bait;
import com.sack.rpgroll.fishing.core.BaitManager;
import com.sack.rpgroll.fishing.item.FishingItemFactory;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class BaitEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int NAME_SLOT = 10;
    private static final int MATERIAL_SLOT = 11;
    private static final int DESCRIPTION_SLOT = 12;
    private static final int TAGS_SLOT = 13;
    private static final int QUALITY_BONUS_SLOT = 14;
    private static final int LEGENDARY_MULT_SLOT = 15;
    private static final int GIVE_SLOT = 22;
    private static final int BACK_SLOT = 40;

    private final BaitManager baitManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private Bait current;

    public BaitEditorGUI(Player player, Bait bait, BaitManager baitManager, ChatPromptManager chatPromptManager,
            Runnable onBack) {
        super(player, Component.text("Carnada: " + bait.id(), NamedTextColor.GOLD), SIZE);
        this.current = bait;
        this.baitManager = baitManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(Bait updated) {
        current = updated;
        baitManager.save(current);
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

        setItem(MATERIAL_SLOT, new ItemBuilder(SpeciesBrowserGUI.parseMaterial(current.material()))
                .setName(Component.text("Material: " + current.material(), NamedTextColor.YELLOW)).build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(Component.text("Descripción", NamedTextColor.YELLOW))
                .setLore(ItemBuilder.toLoreLines(
                        current.description().isBlank() ? "(sin descripción)" : current.description()))
                .build());

        setItem(TAGS_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(Component.text("Tags: " + String.join(", ", current.tags()), NamedTextColor.AQUA))
                .setLore(Component.text("Escribí los tags separados por comas", NamedTextColor.GRAY)).build());

        setItem(QUALITY_BONUS_SLOT, new ItemBuilder(Material.GOLD_NUGGET)
                .setName(Component.text("Bono de calidad: +" + current.qualityBonus(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +0.5 · Click derecho: -0.5", NamedTextColor.GRAY)).build());

        setItem(LEGENDARY_MULT_SLOT, new ItemBuilder(Material.NETHER_STAR)
                .setName(Component.text(
                        String.format(java.util.Locale.ROOT, "Chance de legendario: x%.1f", current.legendaryWeightMultiplier()),
                        NamedTextColor.LIGHT_PURPLE))
                .setLore(Component.text("Click: +0.5 · Click derecho: -0.5", NamedTextColor.GRAY)).build());

        setItem(GIVE_SLOT, new ItemBuilder(Material.CHEST)
                .setName(Component.text("▶ Darme una", NamedTextColor.GREEN)).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        double sign = event.getClick() == ClickType.RIGHT ? -0.5 : 0.5;

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nuevo nombre:", value -> replace(new Bait(current.id(),
                    value, current.material(), current.description(), current.tags(), current.qualityBonus(),
                    current.legendaryWeightMultiplier())));
            return;
        }

        if (slot == MATERIAL_SLOT) {
            chatPromptManager.prompt(player, "Escribí el Material:", value -> replace(new Bait(current.id(),
                    current.displayName(), value, current.description(), current.tags(), current.qualityBonus(),
                    current.legendaryWeightMultiplier())));
            return;
        }

        if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, "Escribí la nueva descripción:", value -> replace(new Bait(current.id(),
                    current.displayName(), current.material(), value, current.tags(), current.qualityBonus(),
                    current.legendaryWeightMultiplier())));
            return;
        }

        if (slot == TAGS_SLOT) {
            chatPromptManager.prompt(player, "Escribí los tags separados por comas:", value -> {

                Set<String> tags = new HashSet<>();
                for (String entry : value.split(",")) {
                    if (!entry.isBlank()) {
                        tags.add(entry.trim().toLowerCase(Locale.ROOT));
                    }
                }

                replace(new Bait(current.id(), current.displayName(), current.material(), current.description(), tags,
                        current.qualityBonus(), current.legendaryWeightMultiplier()));
            });
            return;
        }

        if (slot == QUALITY_BONUS_SLOT) {
            replace(new Bait(current.id(), current.displayName(), current.material(), current.description(),
                    current.tags(), Math.max(0, current.qualityBonus() + sign), current.legendaryWeightMultiplier()));
            return;
        }

        if (slot == LEGENDARY_MULT_SLOT) {
            replace(new Bait(current.id(), current.displayName(), current.material(), current.description(),
                    current.tags(), current.qualityBonus(), Math.max(0.1, current.legendaryWeightMultiplier() + sign)));
            return;
        }

        if (slot == GIVE_SLOT) {
            player.getInventory().addItem(FishingItemFactory.createBait(current));
            player.sendMessage(Component.text("✔ Te diste una carnada '" + current.displayName() + "'.",
                    NamedTextColor.GREEN));
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
