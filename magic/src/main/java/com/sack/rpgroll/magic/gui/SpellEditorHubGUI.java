package com.sack.rpgroll.magic.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.magic.core.SchoolManager;
import com.sack.rpgroll.magic.core.Spell;
import com.sack.rpgroll.magic.core.SpellCastTrigger;
import com.sack.rpgroll.magic.core.SpellCost;
import com.sack.rpgroll.magic.core.SpellManager;
import com.sack.rpgroll.magic.core.SpellRarity;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

/**
 * Identidad, escuela, rareza/nivel, costo, cast/cooldown/trigger y su lugar
 * en el árbol de progresión de un hechizo. El pipeline de componentes vive
 * en su propia pantalla ({@link SpellComponentsEditorGUI}) — es demasiado
 * variado para un formulario lineal.
 */
public class SpellEditorHubGUI extends InventoryGUI {

    private static final int SIZE = 54;

    private static final int NAME_SLOT = 0;
    private static final int ICON_SLOT = 1;
    private static final int COLOR_SLOT = 2;
    private static final int SCHOOL_SLOT = 3;
    private static final int RARITY_SLOT = 4;
    private static final int LEVEL_SLOT = 5;
    private static final int DESCRIPTION_SLOT = 6;

    private static final int MANA_SLOT = 9;
    private static final int HEALTH_COST_SLOT = 10;
    private static final int XP_COST_SLOT = 11;
    private static final int REAGENT_SLOT = 12;

    private static final int CAST_TIME_SLOT = 18;
    private static final int COOLDOWN_SLOT = 19;
    private static final int TRIGGER_SLOT = 20;
    private static final int TREE_PARENT_SLOT = 21;
    private static final int TREE_TIER_SLOT = 22;

    private static final int COMPONENTS_SLOT = 31;
    private static final int BACK_SLOT = 49;

    private final SpellManager spellManager;
    private final SchoolManager schoolManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private Spell current;

    public SpellEditorHubGUI(Player player, Spell spell, SpellManager spellManager, SchoolManager schoolManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text("Hechizo: " + spell.id(), NamedTextColor.GOLD), SIZE);
        this.current = spell;
        this.spellManager = spellManager;
        this.schoolManager = schoolManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(Spell updated) {
        current = updated;
        spellManager.save(current);
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

        setItem(ICON_SLOT, new ItemBuilder(SchoolBrowserGUI.parseMaterial(current.icon()))
                .setName(Component.text("Ícono: " + current.icon(), NamedTextColor.YELLOW)).build());

        setItem(COLOR_SLOT, new ItemBuilder(Material.PAPER)
                .setName(Component.text("Color: " + current.color(), SchoolBrowserGUI.parseColor(current.color())))
                .build());

        setItem(SCHOOL_SLOT, new ItemBuilder(Material.ENCHANTED_BOOK)
                .setName(Component.text("Escuela: " + current.schoolId(), NamedTextColor.AQUA)).build());

        setItem(RARITY_SLOT, new ItemBuilder(Material.NETHER_STAR)
                .setName(Component.text("Rareza: " + current.rarity(), NamedTextColor.LIGHT_PURPLE))
                .setLore(Component.text("Click para pasar a la siguiente", NamedTextColor.GRAY)).build());

        setItem(LEVEL_SLOT, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(Component.text("Nivel: " + current.level(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +1 · Click derecho: -1", NamedTextColor.GRAY)).build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(Component.text("Descripción", NamedTextColor.YELLOW))
                .setLore(ItemBuilder.toLoreLines(
                        current.description().isBlank() ? "(sin descripción)" : current.description()))
                .build());

        SpellCost cost = current.cost();

        setItem(MANA_SLOT, new ItemBuilder(Material.LAPIS_LAZULI)
                .setName(Component.text("Costo de maná: " + cost.mana(), NamedTextColor.BLUE))
                .setLore(Component.text("Click: +5 · Click derecho: -5", NamedTextColor.GRAY)).build());

        setItem(HEALTH_COST_SLOT, new ItemBuilder(Material.REDSTONE)
                .setName(Component.text("Costo de vida: " + cost.health(), NamedTextColor.RED))
                .setLore(Component.text("Click: +1 · Click derecho: -1", NamedTextColor.GRAY)).build());

        setItem(XP_COST_SLOT, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(Component.text("Costo de experiencia: " + cost.experience(), NamedTextColor.GREEN))
                .setLore(Component.text("Click: +5 · Click derecho: -5", NamedTextColor.GRAY)).build());

        setItem(REAGENT_SLOT, new ItemBuilder(Material.NETHER_WART)
                .setName(Component.text(
                        "Reactivo: " + (cost.hasReagent() ? cost.reagentAmount() + "x " + cost.reagentMaterial() : "(ninguno)"),
                        NamedTextColor.YELLOW))
                .setLore(Component.text("Escribí: MATERIAL,CANTIDAD (o 'ninguno')", NamedTextColor.GRAY)).build());

        setItem(CAST_TIME_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(Component.text("Tiempo de canalización: " + current.castTimeTicks() + " ticks",
                        NamedTextColor.YELLOW))
                .setLore(Component.text("Solo aplica si el trigger es HOLD", NamedTextColor.GRAY),
                        Component.text("Click: +20 · Click derecho: -20", NamedTextColor.GRAY)).build());

        setItem(COOLDOWN_SLOT, new ItemBuilder(Material.REPEATER)
                .setName(Component.text("Cooldown: " + current.cooldownTicks() + " ticks", NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +20 · Click derecho: -20", NamedTextColor.GRAY)).build());

        setItem(TRIGGER_SLOT, new ItemBuilder(Material.STICK)
                .setName(Component.text("Trigger: " + current.trigger(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click para pasar al siguiente", NamedTextColor.GRAY)).build());

        setItem(TREE_PARENT_SLOT, new ItemBuilder(Material.LADDER)
                .setName(Component.text(
                        "Requiere antes: " + (current.treeParentId() == null ? "(ninguno)" : current.treeParentId()),
                        NamedTextColor.YELLOW))
                .setLore(Component.text("Escribí el id de otro hechizo (o 'ninguno')", NamedTextColor.GRAY)).build());

        setItem(TREE_TIER_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Nivel del árbol: " + current.treeTier(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +1 · Click derecho: -1", NamedTextColor.GRAY)).build());

        setItem(COMPONENTS_SLOT, new ItemBuilder(Material.COMMAND_BLOCK)
                .setName(Component.text("▶ Componentes (" + current.components().size() + ")", NamedTextColor.GREEN))
                .setLore(Component.text("El pipeline que se ejecuta al lanzarlo", NamedTextColor.GRAY)).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        ClickType click = event.getClick();
        int sign = click == ClickType.RIGHT ? -1 : 1;

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nuevo nombre:", value -> replace(withDisplayName(value)));
            return;
        }

        if (slot == ICON_SLOT) {
            chatPromptManager.prompt(player, "Escribí el Material del ícono:", this::withIconAndReplace);
            return;
        }

        if (slot == COLOR_SLOT) {
            chatPromptManager.prompt(player, "Escribí el color (ej. RED, AQUA):", this::withColorAndReplace);
            return;
        }

        if (slot == SCHOOL_SLOT) {
            chatPromptManager.prompt(player, "Escribí el id de la escuela:", value -> {
                if (!schoolManager.exists(value.trim().toLowerCase(Locale.ROOT))) {
                    player.sendMessage(Component.text("No existe esa escuela.", NamedTextColor.RED));
                    return;
                }
                withSchoolAndReplace(value.trim().toLowerCase(Locale.ROOT));
            });
            return;
        }

        if (slot == RARITY_SLOT) {
            SpellRarity[] values = SpellRarity.values();
            SpellRarity next = values[(current.rarity().ordinal() + 1) % values.length];
            replace(withRarity(next));
            return;
        }

        if (slot == LEVEL_SLOT) {
            replace(withLevel(Math.max(0, current.level() + sign)));
            return;
        }

        if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, "Escribí la nueva descripción:", value -> replace(withDescription(value)));
            return;
        }

        if (slot == MANA_SLOT) {
            SpellCost cost = current.cost();
            replace(withCost(new SpellCost(Math.max(0, cost.mana() + sign * 5), cost.health(), cost.experience(),
                    cost.reagentMaterial(), cost.reagentAmount())));
            return;
        }

        if (slot == HEALTH_COST_SLOT) {
            SpellCost cost = current.cost();
            replace(withCost(new SpellCost(cost.mana(), Math.max(0, cost.health() + sign), cost.experience(),
                    cost.reagentMaterial(), cost.reagentAmount())));
            return;
        }

        if (slot == XP_COST_SLOT) {
            SpellCost cost = current.cost();
            replace(withCost(new SpellCost(cost.mana(), cost.health(), Math.max(0, cost.experience() + sign * 5),
                    cost.reagentMaterial(), cost.reagentAmount())));
            return;
        }

        if (slot == REAGENT_SLOT) {
            chatPromptManager.prompt(player, "Escribí: MATERIAL,CANTIDAD (o 'ninguno'):", value -> {

                SpellCost cost = current.cost();

                if (value.equalsIgnoreCase("ninguno")) {
                    replace(withCost(new SpellCost(cost.mana(), cost.health(), cost.experience(), null, 1)));
                    return;
                }

                String[] parts = value.split(",");
                String material = parts[0].trim().toUpperCase(Locale.ROOT);
                int amount = parts.length > 1 ? parseIntOr(parts[1].trim(), 1) : 1;

                replace(withCost(new SpellCost(cost.mana(), cost.health(), cost.experience(), material, amount)));
            });
            return;
        }

        if (slot == CAST_TIME_SLOT) {
            replace(withCastTime(Math.max(0, current.castTimeTicks() + sign * 20)));
            return;
        }

        if (slot == COOLDOWN_SLOT) {
            replace(withCooldown(Math.max(0, current.cooldownTicks() + sign * 20)));
            return;
        }

        if (slot == TRIGGER_SLOT) {
            SpellCastTrigger[] values = SpellCastTrigger.values();
            SpellCastTrigger next = values[(current.trigger().ordinal() + 1) % values.length];
            replace(withTrigger(next));
            return;
        }

        if (slot == TREE_PARENT_SLOT) {
            chatPromptManager.prompt(player, "Escribí el id del hechizo anterior (o 'ninguno'):",
                    value -> replace(withTreeParent(value.equalsIgnoreCase("ninguno") ? null : value.trim())));
            return;
        }

        if (slot == TREE_TIER_SLOT) {
            replace(withTreeTier(Math.max(0, current.treeTier() + sign)));
            return;
        }

        if (slot == COMPONENTS_SLOT) {
            new SpellComponentsEditorGUI(player, current, spellManager, chatPromptManager, this::onComponentsBack)
                    .open();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void onComponentsBack() {
        current = spellManager.get(current.id()).orElse(current);
        build();
    }

    private void withIconAndReplace(String value) {
        replace(new Spell(current.id(), current.displayName(), value, current.color(), current.schoolId(),
                current.rarity(), current.level(), current.cost(), current.castTimeTicks(), current.cooldownTicks(),
                current.trigger(), current.treeParentId(), current.treeTier(), current.tags(), current.description(),
                current.components()));
    }

    private void withColorAndReplace(String value) {
        replace(new Spell(current.id(), current.displayName(), current.icon(), value, current.schoolId(),
                current.rarity(), current.level(), current.cost(), current.castTimeTicks(), current.cooldownTicks(),
                current.trigger(), current.treeParentId(), current.treeTier(), current.tags(), current.description(),
                current.components()));
    }

    private void withSchoolAndReplace(String schoolId) {
        replace(new Spell(current.id(), current.displayName(), current.icon(), current.color(), schoolId,
                current.rarity(), current.level(), current.cost(), current.castTimeTicks(), current.cooldownTicks(),
                current.trigger(), current.treeParentId(), current.treeTier(), current.tags(), current.description(),
                current.components()));
    }

    private Spell withDisplayName(String value) {
        return new Spell(current.id(), value, current.icon(), current.color(), current.schoolId(), current.rarity(),
                current.level(), current.cost(), current.castTimeTicks(), current.cooldownTicks(), current.trigger(),
                current.treeParentId(), current.treeTier(), current.tags(), current.description(),
                current.components());
    }

    private Spell withRarity(SpellRarity rarity) {
        return new Spell(current.id(), current.displayName(), current.icon(), current.color(), current.schoolId(),
                rarity, current.level(), current.cost(), current.castTimeTicks(), current.cooldownTicks(),
                current.trigger(), current.treeParentId(), current.treeTier(), current.tags(), current.description(),
                current.components());
    }

    private Spell withLevel(int level) {
        return new Spell(current.id(), current.displayName(), current.icon(), current.color(), current.schoolId(),
                current.rarity(), level, current.cost(), current.castTimeTicks(), current.cooldownTicks(),
                current.trigger(), current.treeParentId(), current.treeTier(), current.tags(), current.description(),
                current.components());
    }

    private Spell withDescription(String description) {
        return new Spell(current.id(), current.displayName(), current.icon(), current.color(), current.schoolId(),
                current.rarity(), current.level(), current.cost(), current.castTimeTicks(), current.cooldownTicks(),
                current.trigger(), current.treeParentId(), current.treeTier(), current.tags(), description,
                current.components());
    }

    private Spell withCost(SpellCost cost) {
        return new Spell(current.id(), current.displayName(), current.icon(), current.color(), current.schoolId(),
                current.rarity(), current.level(), cost, current.castTimeTicks(), current.cooldownTicks(),
                current.trigger(), current.treeParentId(), current.treeTier(), current.tags(), current.description(),
                current.components());
    }

    private Spell withCastTime(int ticks) {
        return new Spell(current.id(), current.displayName(), current.icon(), current.color(), current.schoolId(),
                current.rarity(), current.level(), current.cost(), ticks, current.cooldownTicks(), current.trigger(),
                current.treeParentId(), current.treeTier(), current.tags(), current.description(),
                current.components());
    }

    private Spell withCooldown(int ticks) {
        return new Spell(current.id(), current.displayName(), current.icon(), current.color(), current.schoolId(),
                current.rarity(), current.level(), current.cost(), current.castTimeTicks(), ticks, current.trigger(),
                current.treeParentId(), current.treeTier(), current.tags(), current.description(),
                current.components());
    }

    private Spell withTrigger(SpellCastTrigger trigger) {
        return new Spell(current.id(), current.displayName(), current.icon(), current.color(), current.schoolId(),
                current.rarity(), current.level(), current.cost(), current.castTimeTicks(), current.cooldownTicks(),
                trigger, current.treeParentId(), current.treeTier(), current.tags(), current.description(),
                current.components());
    }

    private Spell withTreeParent(String treeParentId) {
        return new Spell(current.id(), current.displayName(), current.icon(), current.color(), current.schoolId(),
                current.rarity(), current.level(), current.cost(), current.castTimeTicks(), current.cooldownTicks(),
                current.trigger(), treeParentId, current.treeTier(), current.tags(), current.description(),
                current.components());
    }

    private Spell withTreeTier(int treeTier) {
        return new Spell(current.id(), current.displayName(), current.icon(), current.color(), current.schoolId(),
                current.rarity(), current.level(), current.cost(), current.castTimeTicks(), current.cooldownTicks(),
                current.trigger(), current.treeParentId(), treeTier, current.tags(), current.description(),
                current.components());
    }

    private int parseIntOr(String raw, int fallback) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

}
