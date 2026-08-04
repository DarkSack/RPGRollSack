package com.sack.rpgroll.dungeons.gui.editor;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.dungeons.core.DungeonDifficulty;
import com.sack.rpgroll.dungeons.core.DungeonModifierType;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

/** Edita una {@link DungeonDifficulty} puntual dentro de {@code session.difficulties.get(index)}. */
public class DifficultyEditGUI extends InventoryGUI {

    private static final int SIZE = 36;

    private static final int NAME_SLOT = 9;
    private static final int HEALTH_SLOT = 10;
    private static final int DAMAGE_SLOT = 11;
    private static final int LOOT_SLOT = 12;

    private static final int MODIFIERS_START_SLOT = 18;
    private static final int BACK_SLOT = 31;

    private final DungeonEditorSession session;
    private final int index;
    private final Runnable onBack;
    private final List<DungeonModifierType> allModifiers = List.of(DungeonModifierType.values());

    public DifficultyEditGUI(Player player, DungeonEditorSession session, int index, Runnable onBack) {
        super(player, Component.text("Dificultad: " + session.difficulties.get(index).id(), NamedTextColor.GOLD),
                SIZE);
        this.session = session;
        this.index = index;
        this.onBack = onBack;
    }

    private DungeonDifficulty difficulty() {
        return session.difficulties.get(index);
    }

    private void replace(DungeonDifficulty updated) {
        List<DungeonDifficulty> list = new ArrayList<>(session.difficulties);
        list.set(index, updated);
        session.difficulties = list;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        DungeonDifficulty difficulty = difficulty();

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(Component.text("Nombre: " + difficulty.displayName(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click para escribir uno nuevo", NamedTextColor.GRAY))
                .build());

        setItem(HEALTH_SLOT, new ItemBuilder(Material.REDSTONE)
                .setName(Component.text("Multiplicador de vida: x" + round(difficulty.healthMultiplier()),
                        NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +0.1 · Shift-click: +0.5 · Derecho: -0.1/-0.5", NamedTextColor.GRAY))
                .build());

        setItem(DAMAGE_SLOT, new ItemBuilder(Material.IRON_SWORD)
                .setName(Component.text("Multiplicador de daño: x" + round(difficulty.damageMultiplier()),
                        NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +0.1 · Shift-click: +0.5 · Derecho: -0.1/-0.5", NamedTextColor.GRAY))
                .build());

        setItem(LOOT_SLOT, new ItemBuilder(Material.GOLD_NUGGET)
                .setName(Component.text("Multiplicador de loot: x" + round(difficulty.lootMultiplier()),
                        NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +0.1 · Shift-click: +0.5 · Derecho: -0.1/-0.5", NamedTextColor.GRAY))
                .build());

        for (int i = 0; i < allModifiers.size(); i++) {

            DungeonModifierType modifier = allModifiers.get(i);
            boolean active = difficulty.hasModifier(modifier);

            setItem(MODIFIERS_START_SLOT + i, new ItemBuilder(active ? Material.SOUL_TORCH : Material.TORCH)
                    .setName(Component.text(modifier.name(), active ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                    .setLore(Component.text("Click para alternar", NamedTextColor.DARK_GRAY))
                    .build());
        }

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private double delta(ClickType click) {
        return switch (click) {
            case LEFT -> 0.1;
            case SHIFT_LEFT -> 0.5;
            case RIGHT -> -0.1;
            case SHIFT_RIGHT -> -0.5;
            default -> 0;
        };
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == NAME_SLOT) {
            session.chatPromptManager.prompt(player, "Escribí el nuevo nombre:", value -> {
                replace(withName(difficulty(), value));
                build();
            });
            return;
        }

        if (slot == HEALTH_SLOT) {
            double updated = Math.max(0.1, round(difficulty().healthMultiplier() + delta(event.getClick())));
            replace(withHealth(difficulty(), updated));
            build();
            return;
        }

        if (slot == DAMAGE_SLOT) {
            double updated = Math.max(0.1, round(difficulty().damageMultiplier() + delta(event.getClick())));
            replace(withDamage(difficulty(), updated));
            build();
            return;
        }

        if (slot == LOOT_SLOT) {
            double updated = Math.max(0.1, round(difficulty().lootMultiplier() + delta(event.getClick())));
            replace(withLoot(difficulty(), updated));
            build();
            return;
        }

        if (slot >= MODIFIERS_START_SLOT && slot < MODIFIERS_START_SLOT + allModifiers.size()) {

            DungeonModifierType modifier = allModifiers.get(slot - MODIFIERS_START_SLOT);
            List<DungeonModifierType> modifiers = new ArrayList<>(difficulty().modifiers());

            if (modifiers.contains(modifier)) {
                modifiers.remove(modifier);
            } else {
                modifiers.add(modifier);
            }

            replace(withModifiers(difficulty(), modifiers));
            build();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private DungeonDifficulty withName(DungeonDifficulty d, String name) {
        return new DungeonDifficulty(d.id(), name, d.healthMultiplier(), d.damageMultiplier(), d.lootMultiplier(),
                d.modifiers());
    }

    private DungeonDifficulty withHealth(DungeonDifficulty d, double value) {
        return new DungeonDifficulty(d.id(), d.displayName(), value, d.damageMultiplier(), d.lootMultiplier(),
                d.modifiers());
    }

    private DungeonDifficulty withDamage(DungeonDifficulty d, double value) {
        return new DungeonDifficulty(d.id(), d.displayName(), d.healthMultiplier(), value, d.lootMultiplier(),
                d.modifiers());
    }

    private DungeonDifficulty withLoot(DungeonDifficulty d, double value) {
        return new DungeonDifficulty(d.id(), d.displayName(), d.healthMultiplier(), d.damageMultiplier(), value,
                d.modifiers());
    }

    private DungeonDifficulty withModifiers(DungeonDifficulty d, List<DungeonModifierType> modifiers) {
        return new DungeonDifficulty(d.id(), d.displayName(), d.healthMultiplier(), d.damageMultiplier(),
                d.lootMultiplier(), modifiers);
    }

}
