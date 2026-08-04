package com.sack.rpgroll.enchantments.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.enchantments.core.CustomEnchantment;
import com.sack.rpgroll.enchantments.core.EnchantCategory;
import com.sack.rpgroll.enchantments.core.EnchantEffect;
import com.sack.rpgroll.enchantments.core.EnchantEffectType;
import com.sack.rpgroll.enchantments.core.EnchantmentManager;
import com.sack.rpgroll.enchantments.core.Rarity;
import com.sack.rpgroll.enchantments.core.Trigger;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Editor de un encantamiento — identidad, rareza/nivel/probabilidad,
 * categorías y triggers (grillas clickeables) y su lista de efectos
 * (agregados vía sintaxis de una línea). "levels" (datos numéricos por
 * nivel) se preserva al guardar pero no se edita todavía desde la GUI.
 */
public class EnchantmentEditorGUI extends InventoryGUI {

    private static final int SIZE = 54;

    private static final int NAME_SLOT = 0;
    private static final int RARITY_SLOT = 1;
    private static final int MAX_LEVEL_SLOT = 2;
    private static final int CHANCE_SLOT = 3;
    private static final int CONFLICTS_SLOT = 4;
    private static final int CONDITIONS_SLOT = 5;

    private static final int CATEGORY_ROW = 9;
    private static final int TRIGGER_ROW = 18;

    private static final int EFFECTS_ROW = 27;
    private static final int ADD_EFFECT_SLOT = 44;
    private static final int BACK_SLOT = 53;

    private final EnchantmentManager enchantmentManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private CustomEnchantment current;

    public EnchantmentEditorGUI(Player player, CustomEnchantment enchantment, EnchantmentManager enchantmentManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text("Encantamiento: " + enchantment.id(), NamedTextColor.GOLD), SIZE);
        this.current = enchantment;
        this.enchantmentManager = enchantmentManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(CustomEnchantment updated) {
        current = updated;
        enchantmentManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(LegacyComponentSerializer.legacyAmpersand()
                        .deserialize("Nombre: " + current.displayName()).colorIfAbsent(NamedTextColor.YELLOW))
                .setLore(Component.text("Click para escribir uno nuevo", NamedTextColor.GRAY))
                .build());

        setItem(RARITY_SLOT, new ItemBuilder(Material.NETHER_STAR)
                .setName(Component.text("Rareza: " + current.rarity(), current.rarity().color()))
                .setLore(Component.text("Click para pasar a la siguiente", NamedTextColor.GRAY))
                .build());

        setItem(MAX_LEVEL_SLOT, new ItemBuilder(Material.ANVIL)
                .setName(Component.text("Nivel máximo: " + current.maxLevel(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +1 · Click derecho: -1", NamedTextColor.GRAY))
                .build());

        setItem(CHANCE_SLOT, new ItemBuilder(Material.GOLD_NUGGET)
                .setName(Component.text("Probabilidad: " + current.chance() + "%", NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +5 · Click derecho: -5", NamedTextColor.GRAY))
                .build());

        setItem(CONFLICTS_SLOT, new ItemBuilder(Material.BARRIER)
                .setName(Component.text("Conflictos: " + String.join(", ", current.conflicts()), NamedTextColor.YELLOW))
                .setLore(Component.text("Click para escribir la lista separada por comas", NamedTextColor.GRAY))
                .build());

        setItem(CONDITIONS_SLOT, new ItemBuilder(Material.BOOK)
                .setName(Component.text("Condiciones: " + current.conditions().size(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click para escribir la lista completa, una por línea", NamedTextColor.GRAY))
                .build());

        EnchantCategory[] categories = EnchantCategory.values();
        for (int i = 0; i < categories.length && i < 9; i++) {
            boolean active = current.categories().contains(categories[i]);
            setItem(CATEGORY_ROW + i, new ItemBuilder(active ? Material.LIME_DYE : Material.GRAY_DYE)
                    .setName(Component.text(categories[i].name(), active ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                    .setLore(Component.text("Click para alternar", NamedTextColor.DARK_GRAY))
                    .build());
        }

        Trigger[] triggers = Trigger.values();
        for (int i = 0; i < triggers.length && i < 9; i++) {
            boolean active = current.triggers().contains(triggers[i]);
            setItem(TRIGGER_ROW + i, new ItemBuilder(active ? Material.LIME_DYE : Material.GRAY_DYE)
                    .setName(Component.text(triggers[i].name(), active ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                    .setLore(Component.text("Click para alternar", NamedTextColor.DARK_GRAY))
                    .build());
        }

        List<EnchantEffect> effects = current.effects();
        for (int i = 0; i < effects.size() && i < 8; i++) {
            EnchantEffect effect = effects.get(i);
            setItem(EFFECTS_ROW + i, new ItemBuilder(Material.POTION)
                    .setName(Component.text(effect.type().name(), NamedTextColor.WHITE))
                    .setLore(Component.text(effect.params().toString(), NamedTextColor.GRAY),
                            Component.text("Shift-click para quitar", NamedTextColor.DARK_GRAY))
                    .build());
        }

        setItem(ADD_EFFECT_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Agregar efecto", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        ClickType click = event.getClick();

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nuevo nombre:", value -> replace(new CustomEnchantment(
                    current.id(), value, current.rarity(), current.categories(), current.allowedItems(),
                    current.maxLevel(), current.levelData(), current.conflicts(), current.triggers(),
                    current.conditions(), current.chance(), current.effects())));
            return;
        }

        if (slot == RARITY_SLOT) {
            Rarity[] values = Rarity.values();
            Rarity next = values[(current.rarity().ordinal() + 1) % values.length];
            replace(new CustomEnchantment(current.id(), current.displayName(), next, current.categories(),
                    current.allowedItems(), current.maxLevel(), current.levelData(), current.conflicts(),
                    current.triggers(), current.conditions(), current.chance(), current.effects()));
            return;
        }

        if (slot == MAX_LEVEL_SLOT) {
            int delta = click == ClickType.RIGHT ? -1 : 1;
            replace(new CustomEnchantment(current.id(), current.displayName(), current.rarity(),
                    current.categories(), current.allowedItems(), Math.max(1, current.maxLevel() + delta),
                    current.levelData(), current.conflicts(), current.triggers(), current.conditions(),
                    current.chance(), current.effects()));
            return;
        }

        if (slot == CHANCE_SLOT) {
            double delta = click == ClickType.RIGHT ? -5 : 5;
            replace(new CustomEnchantment(current.id(), current.displayName(), current.rarity(),
                    current.categories(), current.allowedItems(), current.maxLevel(), current.levelData(),
                    current.conflicts(), current.triggers(), current.conditions(),
                    Math.max(1, Math.min(100, current.chance() + delta)), current.effects()));
            return;
        }

        if (slot == CONFLICTS_SLOT) {
            chatPromptManager.prompt(player, "Escribí los ids en conflicto separados por comas:", value -> {
                List<String> conflicts = value.isBlank() ? List.of()
                        : List.of(value.toUpperCase(Locale.ROOT).split("\\s*,\\s*"));
                replace(new CustomEnchantment(current.id(), current.displayName(), current.rarity(),
                        current.categories(), current.allowedItems(), current.maxLevel(), current.levelData(),
                        conflicts, current.triggers(), current.conditions(), current.chance(), current.effects()));
            });
            return;
        }

        if (slot == CONDITIONS_SLOT) {
            chatPromptManager.prompt(player, "Escribí las condiciones separadas por ';':", value -> {
                List<String> conditions = value.isBlank() ? List.of() : List.of(value.split("\\s*;\\s*"));
                replace(new CustomEnchantment(current.id(), current.displayName(), current.rarity(),
                        current.categories(), current.allowedItems(), current.maxLevel(), current.levelData(),
                        current.conflicts(), current.triggers(), conditions, current.chance(), current.effects()));
            });
            return;
        }

        EnchantCategory[] categories = EnchantCategory.values();
        if (slot >= CATEGORY_ROW && slot < CATEGORY_ROW + Math.min(categories.length, 9)) {
            EnchantCategory category = categories[slot - CATEGORY_ROW];
            Set<EnchantCategory> updated = new LinkedHashSet<>(current.categories());
            if (!updated.remove(category)) {
                updated.add(category);
            }
            replace(new CustomEnchantment(current.id(), current.displayName(), current.rarity(), updated,
                    current.allowedItems(), current.maxLevel(), current.levelData(), current.conflicts(),
                    current.triggers(), current.conditions(), current.chance(), current.effects()));
            return;
        }

        Trigger[] triggers = Trigger.values();
        if (slot >= TRIGGER_ROW && slot < TRIGGER_ROW + Math.min(triggers.length, 9)) {
            Trigger trigger = triggers[slot - TRIGGER_ROW];
            Set<Trigger> updated = new LinkedHashSet<>(current.triggers());
            if (!updated.remove(trigger)) {
                updated.add(trigger);
            }
            replace(new CustomEnchantment(current.id(), current.displayName(), current.rarity(), current.categories(),
                    current.allowedItems(), current.maxLevel(), current.levelData(), current.conflicts(), updated,
                    current.conditions(), current.chance(), current.effects()));
            return;
        }

        if (slot >= EFFECTS_ROW && slot < EFFECTS_ROW + Math.min(current.effects().size(), 8)) {

            if (event.isShiftClick()) {

                if (current.effects().size() <= 1) {
                    player.sendMessage(Component.text("El encantamiento necesita al menos un efecto.",
                            NamedTextColor.RED));
                    return;
                }

                List<EnchantEffect> updated = new ArrayList<>(current.effects());
                updated.remove(slot - EFFECTS_ROW);
                replace(new CustomEnchantment(current.id(), current.displayName(), current.rarity(),
                        current.categories(), current.allowedItems(), current.maxLevel(), current.levelData(),
                        current.conflicts(), current.triggers(), current.conditions(), current.chance(), updated));
            }
            return;
        }

        if (slot == ADD_EFFECT_SLOT) {
            promptAddEffect();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void promptAddEffect() {
        chatPromptManager.prompt(player, "Escribí: TIPO;clave=valor;clave2=valor2 (tipos: "
                + String.join(", ", java.util.Arrays.stream(EnchantEffectType.values()).map(Enum::name).toList())
                + "):", value -> {

                    String[] parts = value.split(";");

                    EnchantEffectType type;
                    try {
                        type = EnchantEffectType.valueOf(parts[0].trim().toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(Component.text("Tipo de efecto inválido.", NamedTextColor.RED));
                        return;
                    }

                    java.util.Map<String, String> params = new java.util.LinkedHashMap<>();
                    for (int i = 1; i < parts.length; i++) {
                        String[] kv = parts[i].split("=", 2);
                        if (kv.length == 2) {
                            params.put(kv[0].trim(), kv[1].trim());
                        }
                    }

                    List<EnchantEffect> updated = new ArrayList<>(current.effects());
                    updated.add(new EnchantEffect(type, params));

                    replace(new CustomEnchantment(current.id(), current.displayName(), current.rarity(),
                            current.categories(), current.allowedItems(), current.maxLevel(), current.levelData(),
                            current.conflicts(), current.triggers(), current.conditions(), current.chance(),
                            updated));
                });
    }

}
