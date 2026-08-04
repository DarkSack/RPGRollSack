package com.sack.rpgroll.fishing.gui;

import com.sack.rpgroll.fishing.core.FishingRod;
import com.sack.rpgroll.fishing.core.FishingRodManager;
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

public class RodEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int NAME_SLOT = 10;
    private static final int MATERIAL_SLOT = 11;
    private static final int DESCRIPTION_SLOT = 12;
    private static final int DURABILITY_SLOT = 13;
    private static final int CAST_POWER_SLOT = 14;
    private static final int REEL_SPEED_SLOT = 15;
    private static final int PRECISION_SLOT = 16;
    private static final int RESISTANCE_SLOT = 19;
    private static final int LUCK_SLOT = 20;
    private static final int CATEGORIES_SLOT = 21;
    private static final int GIVE_SLOT = 22;
    private static final int BACK_SLOT = 40;

    private final FishingRodManager rodManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private FishingRod current;

    public RodEditorGUI(Player player, FishingRod rod, FishingRodManager rodManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text("Caña: " + rod.id(), NamedTextColor.GOLD), SIZE);
        this.current = rod;
        this.rodManager = rodManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(FishingRod updated) {
        current = updated;
        rodManager.save(current);
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

        setItem(DURABILITY_SLOT, new ItemBuilder(Material.ANVIL)
                .setName(Component.text("Durabilidad: " + current.durability(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +8 · Click derecho: -8", NamedTextColor.GRAY)).build());

        setItem(CAST_POWER_SLOT, new ItemBuilder(Material.FISHING_ROD)
                .setName(Component.text(String.format(Locale.ROOT, "Alcance: x%.2f", current.castPower()),
                        NamedTextColor.AQUA))
                .setLore(Component.text("Click: +0.1 · Click derecho: -0.1", NamedTextColor.GRAY)).build());

        setItem(REEL_SPEED_SLOT, new ItemBuilder(Material.STRING)
                .setName(Component.text(String.format(Locale.ROOT, "Velocidad de recogida: x%.2f", current.reelSpeed()),
                        NamedTextColor.LIGHT_PURPLE))
                .setLore(Component.text("Click: +0.1 · Click derecho: -0.1", NamedTextColor.GRAY)).build());

        setItem(PRECISION_SLOT, new ItemBuilder(Material.SPYGLASS)
                .setName(Component.text(String.format(Locale.ROOT, "Precisión: +%.1f", current.precision()),
                        NamedTextColor.AQUA))
                .setLore(Component.text("Click: +0.5 · Click derecho: -0.5", NamedTextColor.GRAY)).build());

        setItem(RESISTANCE_SLOT, new ItemBuilder(Material.SHIELD)
                .setName(Component.text(String.format(Locale.ROOT, "Resistencia: x%.2f", current.resistance()),
                        NamedTextColor.GREEN))
                .setLore(Component.text("Más fallos permitidos en el minijuego. Click: +0.1 · Click derecho: -0.1",
                        NamedTextColor.GRAY))
                .build());

        setItem(LUCK_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text(String.format(Locale.ROOT, "Suerte: x%.2f", current.luckBonus()),
                        NamedTextColor.YELLOW))
                .setLore(Component.text("Bono a especies no comunes. Click: +0.1 · Click derecho: -0.1", NamedTextColor.GRAY))
                .build());

        setItem(CATEGORIES_SLOT, new ItemBuilder(Material.TROPICAL_FISH)
                .setName(Component.text("Especies favoritas: " + String.join(", ", current.preferredCategories()),
                        NamedTextColor.GOLD))
                .setLore(Component.text("Escribí categorías separadas por comas", NamedTextColor.GRAY)).build());

        setItem(GIVE_SLOT, new ItemBuilder(Material.CHEST)
                .setName(Component.text("▶ Darme una", NamedTextColor.GREEN)).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        double sign = event.getClick() == ClickType.RIGHT ? -0.1 : 0.1;
        int intSign = event.getClick() == ClickType.RIGHT ? -1 : 1;

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nuevo nombre:", value -> replace(new FishingRod(current.id(),
                    value, current.material(), current.description(), current.durability(), current.castPower(),
                    current.reelSpeed(), current.precision(), current.resistance(), current.luckBonus(),
                    current.preferredCategories())));
            return;
        }

        if (slot == MATERIAL_SLOT) {
            chatPromptManager.prompt(player, "Escribí el Material:", value -> replace(new FishingRod(current.id(),
                    current.displayName(), value, current.description(), current.durability(), current.castPower(),
                    current.reelSpeed(), current.precision(), current.resistance(), current.luckBonus(),
                    current.preferredCategories())));
            return;
        }

        if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, "Escribí la nueva descripción:", value -> replace(new FishingRod(
                    current.id(), current.displayName(), current.material(), value, current.durability(),
                    current.castPower(), current.reelSpeed(), current.precision(), current.resistance(),
                    current.luckBonus(), current.preferredCategories())));
            return;
        }

        if (slot == DURABILITY_SLOT) {
            replace(new FishingRod(current.id(), current.displayName(), current.material(), current.description(),
                    Math.max(1, current.durability() + intSign * 8), current.castPower(), current.reelSpeed(),
                    current.precision(), current.resistance(), current.luckBonus(), current.preferredCategories()));
            return;
        }

        if (slot == CAST_POWER_SLOT) {
            replace(new FishingRod(current.id(), current.displayName(), current.material(), current.description(),
                    current.durability(), Math.max(0.1, current.castPower() + sign), current.reelSpeed(),
                    current.precision(), current.resistance(), current.luckBonus(), current.preferredCategories()));
            return;
        }

        if (slot == REEL_SPEED_SLOT) {
            replace(new FishingRod(current.id(), current.displayName(), current.material(), current.description(),
                    current.durability(), current.castPower(), Math.max(0.1, current.reelSpeed() + sign),
                    current.precision(), current.resistance(), current.luckBonus(), current.preferredCategories()));
            return;
        }

        if (slot == PRECISION_SLOT) {
            replace(new FishingRod(current.id(), current.displayName(), current.material(), current.description(),
                    current.durability(), current.castPower(), current.reelSpeed(),
                    Math.max(0, current.precision() + sign * 5), current.resistance(), current.luckBonus(),
                    current.preferredCategories()));
            return;
        }

        if (slot == RESISTANCE_SLOT) {
            replace(new FishingRod(current.id(), current.displayName(), current.material(), current.description(),
                    current.durability(), current.castPower(), current.reelSpeed(), current.precision(),
                    Math.max(0.1, current.resistance() + sign), current.luckBonus(), current.preferredCategories()));
            return;
        }

        if (slot == LUCK_SLOT) {
            replace(new FishingRod(current.id(), current.displayName(), current.material(), current.description(),
                    current.durability(), current.castPower(), current.reelSpeed(), current.precision(),
                    current.resistance(), Math.max(0.1, current.luckBonus() + sign), current.preferredCategories()));
            return;
        }

        if (slot == CATEGORIES_SLOT) {
            chatPromptManager.prompt(player, "Escribí las categorías favoritas separadas por comas:", value -> {

                Set<String> categories = new HashSet<>();
                for (String entry : value.split(",")) {
                    if (!entry.isBlank()) {
                        categories.add(entry.trim().toLowerCase(Locale.ROOT));
                    }
                }

                replace(new FishingRod(current.id(), current.displayName(), current.material(), current.description(),
                        current.durability(), current.castPower(), current.reelSpeed(), current.precision(),
                        current.resistance(), current.luckBonus(), categories));
            });
            return;
        }

        if (slot == GIVE_SLOT) {
            player.getInventory().addItem(FishingItemFactory.createRod(current));
            player.sendMessage(Component.text("✔ Te diste una caña '" + current.displayName() + "'.", NamedTextColor.GREEN));
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
