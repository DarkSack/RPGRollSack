package com.sack.rpgroll.effects.gui;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.effects.core.EffectCategory;
import com.sack.rpgroll.effects.core.EffectDefinition;
import com.sack.rpgroll.effects.core.EffectManager;
import com.sack.rpgroll.effects.core.EffectRarity;
import com.sack.rpgroll.effects.runtime.EffectTracker;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Locale;

/**
 * Pantalla principal de un efecto — identidad (nombre/ícono/color/categoría
 * /rareza/descripción/duración/prioridad/visible) más accesos a las 3
 * subpantallas (condiciones, componentes, stacking/tags/conflictos) y un
 * botón "Probar" que lo aplica sobre vos mismo ahí mismo, sin salir de la
 * GUI ni pasar por el chequeo de condiciones (es una prueba, no un uso real).
 */
public class EffectEditorHubGUI extends InventoryGUI {

    private static final int SIZE = 54;

    private static final int ICON_SLOT = 0;
    private static final int COLOR_SLOT = 1;
    private static final int CATEGORY_SLOT = 2;
    private static final int RARITY_SLOT = 3;
    private static final int NAME_SLOT = 4;
    private static final int DESCRIPTION_SLOT = 5;
    private static final int DURATION_SLOT = 6;
    private static final int PRIORITY_SLOT = 7;
    private static final int VISIBLE_SLOT = 8;

    private static final int CONDITIONS_SLOT = 19;
    private static final int COMPONENTS_SLOT = 20;
    private static final int STACKING_SLOT = 21;

    private static final int TEST_SLOT = 49;
    private static final int BACK_SLOT = 53;

    private final EffectManager effectManager;
    private final EffectTracker tracker;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private EffectDefinition current;

    public EffectEditorHubGUI(Player player, EffectDefinition effect, EffectManager effectManager,
            EffectTracker tracker, ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text("Efecto: " + effect.id(), NamedTextColor.GOLD), SIZE);
        this.current = effect;
        this.effectManager = effectManager;
        this.tracker = tracker;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(EffectDefinition updated) {
        current = updated;
        effectManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int slot = 9; slot < 18; slot++) {
            setItem(slot, sectionGlass());
        }

        for (int slot = 45; slot < SIZE; slot++) {
            setItem(slot, new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                    .setName(Component.text(" ", NamedTextColor.GRAY)).build());
        }

        setItem(ICON_SLOT, new ItemBuilder(parseMaterial(current.icon()))
                .setName(Component.text("Ícono: " + current.icon(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click para escribir un Material nuevo", NamedTextColor.GRAY))
                .build());

        setItem(COLOR_SLOT, new ItemBuilder(Material.LIME_DYE)
                .setName(Component.text("Color: " + current.color(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click para escribir un color nuevo (ej. RED, #55FF55)", NamedTextColor.GRAY))
                .build());

        setItem(CATEGORY_SLOT, new ItemBuilder(Material.HOPPER)
                .setName(Component.text("Categoría: " + current.category(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click para ciclar", NamedTextColor.GRAY))
                .build());

        setItem(RARITY_SLOT, new ItemBuilder(Material.NETHER_STAR)
                .setName(Component.text("Rareza: " + current.rarity(), current.rarity().color()))
                .setLore(Component.text("Click para ciclar", NamedTextColor.GRAY))
                .build());

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(ComponentUtils.parse("Nombre: " + current.displayName()).colorIfAbsent(NamedTextColor.YELLOW))
                .setLore(Component.text("Click para escribir uno nuevo", NamedTextColor.GRAY))
                .build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(Component.text("Descripción", NamedTextColor.YELLOW))
                .setLore(ItemBuilder.toLoreLines(
                        current.description().isBlank() ? "(sin descripción)" : current.description()))
                .build());

        setItem(DURATION_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(Component.text(
                        "Duración: " + (current.durationTicks() <= 0 ? "Permanente" : current.durationTicks() + " ticks"),
                        NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +20 · Shift-click: +200", NamedTextColor.GRAY),
                        Component.text("Click derecho: -20 · Shift-click derecho: -200", NamedTextColor.GRAY),
                        Component.text("(0 = permanente)", NamedTextColor.DARK_GRAY))
                .build());

        setItem(PRIORITY_SLOT, new ItemBuilder(Material.LADDER)
                .setName(Component.text("Prioridad: " + current.priority(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +1 · Click derecho: -1", NamedTextColor.GRAY))
                .build());

        setItem(VISIBLE_SLOT, new ItemBuilder(current.visible() ? Material.LIME_DYE : Material.GRAY_DYE)
                .setName(Component.text("Visible en HUD: " + (current.visible() ? "Sí" : "No"),
                        current.visible() ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                .setLore(Component.text("Click para alternar", NamedTextColor.GRAY))
                .build());

        setItem(CONDITIONS_SLOT, new ItemBuilder(Material.BOOK)
                .setName(Component.text("Condiciones", NamedTextColor.LIGHT_PURPLE))
                .setLore(Component.text(current.conditions().size() + " condición(es)", NamedTextColor.GRAY),
                        Component.text("Click para editar", NamedTextColor.YELLOW))
                .build());

        setItem(COMPONENTS_SLOT, new ItemBuilder(Material.BLAZE_POWDER)
                .setName(Component.text("Componentes", NamedTextColor.LIGHT_PURPLE))
                .setLore(Component.text(current.components().size() + " componente(s)", NamedTextColor.GRAY),
                        Component.text("Click para editar", NamedTextColor.YELLOW))
                .build());

        setItem(STACKING_SLOT, new ItemBuilder(Material.CHEST)
                .setName(Component.text("Stacking / Tags / Conflictos", NamedTextColor.LIGHT_PURPLE))
                .setLore(Component.text("Modo: " + current.stackingMode(), NamedTextColor.GRAY),
                        Component.text(current.tags().size() + " tag(s), " + current.conflicts().size()
                                + " conflicto(s)", NamedTextColor.GRAY),
                        Component.text("Click para editar", NamedTextColor.YELLOW))
                .build());

        setItem(TEST_SLOT, new ItemBuilder(Material.FIREWORK_ROCKET)
                .setName(Component.text("▶ Probar en vos mismo", NamedTextColor.GREEN))
                .setLore(Component.text("Ignora las condiciones — solo para previsualizar", NamedTextColor.GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    private org.bukkit.inventory.ItemStack sectionGlass() {
        return new ItemBuilder(Material.PURPLE_STAINED_GLASS_PANE)
                .setName(Component.text(" ", NamedTextColor.GRAY)).build();
    }

    private Material parseMaterial(String raw) {
        try {
            return Material.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Material.NETHER_STAR;
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        ClickType click = event.getClick();
        boolean shift = event.isShiftClick();

        switch (slot) {
            case ICON_SLOT -> promptIcon();
            case COLOR_SLOT -> promptColor();
            case CATEGORY_SLOT -> cycleCategory();
            case RARITY_SLOT -> cycleRarity();
            case NAME_SLOT -> promptName();
            case DESCRIPTION_SLOT -> promptDescription();
            case DURATION_SLOT -> adjustDuration(click, shift);
            case PRIORITY_SLOT -> adjustPriority(click);
            case VISIBLE_SLOT -> toggleVisible();
            case CONDITIONS_SLOT -> new EffectConditionsEditorGUI(player, current, effectManager, chatPromptManager,
                    this::reopenFromSub).open();
            case COMPONENTS_SLOT -> new EffectComponentsEditorGUI(player, current, effectManager, chatPromptManager,
                    this::reopenFromSub).open();
            case STACKING_SLOT -> new EffectStackingEditorGUI(player, current, effectManager, chatPromptManager,
                    this::reopenFromSub).open();
            case TEST_SLOT -> testFire();
            case BACK_SLOT -> onBack.run();
            default -> {
            }
        }
    }

    private void reopenFromSub() {
        var refreshed = effectManager.get(current.id());
        refreshed.ifPresent(value -> current = value);
        build();
    }

    private void testFire() {
        tracker.apply(player, current, player);
        player.sendMessage(Component.text("▶ Probando '" + current.id() + "' en vos mismo...", NamedTextColor.GREEN));
    }

    private void promptIcon() {
        chatPromptManager.prompt(player, "Escribí el Material del ícono (ej. NETHER_STAR):", value -> {
            try {
                Material.valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                player.sendMessage(Component.text("Material inválido: " + value, NamedTextColor.RED));
                build();
                return;
            }
            replace(new EffectDefinition(current.id(), current.displayName(), value.trim().toUpperCase(Locale.ROOT),
                    current.color(), current.category(), current.rarity(), current.description(),
                    current.durationTicks(), current.priority(), current.visible(), current.conditions(),
                    current.tags(), current.conflicts(), current.stackingMode(), current.maxStacks(),
                    current.upgradeToEffectId(), current.components()));
        });
    }

    private void promptColor() {
        chatPromptManager.prompt(player, "Escribí el color (nombre o #hex):",
                value -> replace(new EffectDefinition(current.id(), current.displayName(), current.icon(), value,
                        current.category(), current.rarity(), current.description(), current.durationTicks(),
                        current.priority(), current.visible(), current.conditions(), current.tags(),
                        current.conflicts(), current.stackingMode(), current.maxStacks(),
                        current.upgradeToEffectId(), current.components())));
    }

    private void cycleCategory() {
        EffectCategory[] values = EffectCategory.values();
        EffectCategory next = values[(current.category().ordinal() + 1) % values.length];
        replace(new EffectDefinition(current.id(), current.displayName(), current.icon(), current.color(), next,
                current.rarity(), current.description(), current.durationTicks(), current.priority(),
                current.visible(), current.conditions(), current.tags(), current.conflicts(),
                current.stackingMode(), current.maxStacks(), current.upgradeToEffectId(), current.components()));
    }

    private void cycleRarity() {
        EffectRarity[] values = EffectRarity.values();
        EffectRarity next = values[(current.rarity().ordinal() + 1) % values.length];
        replace(new EffectDefinition(current.id(), current.displayName(), current.icon(), current.color(),
                current.category(), next, current.description(), current.durationTicks(), current.priority(),
                current.visible(), current.conditions(), current.tags(), current.conflicts(),
                current.stackingMode(), current.maxStacks(), current.upgradeToEffectId(), current.components()));
    }

    private void promptName() {
        chatPromptManager.prompt(player, "Escribí el nuevo nombre:",
                value -> replace(new EffectDefinition(current.id(), value, current.icon(), current.color(),
                        current.category(), current.rarity(), current.description(), current.durationTicks(),
                        current.priority(), current.visible(), current.conditions(), current.tags(),
                        current.conflicts(), current.stackingMode(), current.maxStacks(),
                        current.upgradeToEffectId(), current.components())));
    }

    private void promptDescription() {
        chatPromptManager.prompt(player, "Escribí la nueva descripción:",
                value -> replace(new EffectDefinition(current.id(), current.displayName(), current.icon(),
                        current.color(), current.category(), current.rarity(), value, current.durationTicks(),
                        current.priority(), current.visible(), current.conditions(), current.tags(),
                        current.conflicts(), current.stackingMode(), current.maxStacks(),
                        current.upgradeToEffectId(), current.components())));
    }

    private void adjustDuration(ClickType click, boolean shift) {

        int delta = shift ? 200 : 20;
        if (click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT) {
            delta = -delta;
        }

        int newDuration = Math.max(0, current.durationTicks() + delta);

        replace(new EffectDefinition(current.id(), current.displayName(), current.icon(), current.color(),
                current.category(), current.rarity(), current.description(), newDuration, current.priority(),
                current.visible(), current.conditions(), current.tags(), current.conflicts(),
                current.stackingMode(), current.maxStacks(), current.upgradeToEffectId(), current.components()));
    }

    private void adjustPriority(ClickType click) {

        int delta = click == ClickType.RIGHT ? -1 : 1;

        replace(new EffectDefinition(current.id(), current.displayName(), current.icon(), current.color(),
                current.category(), current.rarity(), current.description(), current.durationTicks(),
                current.priority() + delta, current.visible(), current.conditions(), current.tags(),
                current.conflicts(), current.stackingMode(), current.maxStacks(), current.upgradeToEffectId(),
                current.components()));
    }

    private void toggleVisible() {
        replace(new EffectDefinition(current.id(), current.displayName(), current.icon(), current.color(),
                current.category(), current.rarity(), current.description(), current.durationTicks(),
                current.priority(), !current.visible(), current.conditions(), current.tags(), current.conflicts(),
                current.stackingMode(), current.maxStacks(), current.upgradeToEffectId(), current.components()));
    }

}
