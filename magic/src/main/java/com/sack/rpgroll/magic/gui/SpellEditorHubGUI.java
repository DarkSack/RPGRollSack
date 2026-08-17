package com.sack.rpgroll.magic.gui;

import com.sack.rpgroll.common.lang.LangManager;
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
    private final LangManager lang;
    private final Runnable onBack;
    private Spell current;

    public SpellEditorHubGUI(Player player, Spell spell, SpellManager spellManager, SchoolManager schoolManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, chatPromptManager.lang().component("gui.spell_editor.title", "id", spell.id()), SIZE);
        this.current = spell;
        this.spellManager = spellManager;
        this.schoolManager = schoolManager;
        this.chatPromptManager = chatPromptManager;
        this.lang = chatPromptManager.lang();
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
                .setName(lang.component("gui.common.name_label", "name", current.displayName())).build());

        setItem(ICON_SLOT, new ItemBuilder(SchoolBrowserGUI.parseMaterial(current.icon()))
                .setName(lang.component("gui.common.icon_label", "icon", current.icon())).build());

        setItem(COLOR_SLOT, new ItemBuilder(Material.PAPER)
                .setName(Component.text(lang.raw("gui.spell_editor.color_label", "color", current.color()),
                        SchoolBrowserGUI.parseColor(current.color())))
                .build());

        setItem(SCHOOL_SLOT, new ItemBuilder(Material.ENCHANTED_BOOK)
                .setName(lang.component("gui.spell_editor.school_label", "schoolId", current.schoolId())).build());

        setItem(RARITY_SLOT, new ItemBuilder(Material.NETHER_STAR)
                .setName(lang.component("gui.spell_editor.rarity_label", "rarity", current.rarity()))
                .setLore(lang.component("gui.common.click_cycle_fem")).build());

        setItem(LEVEL_SLOT, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(lang.component("gui.spell_editor.level_label", "level", current.level()))
                .setLore(lang.component("gui.common.step_1")).build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(lang.component("gui.common.description_title"))
                .setLore(ItemBuilder.toLoreLines(
                        current.description().isBlank() ? lang.raw("gui.common.no_description")
                                : current.description()))
                .build());

        SpellCost cost = current.cost();

        setItem(MANA_SLOT, new ItemBuilder(Material.LAPIS_LAZULI)
                .setName(lang.component("gui.spell_editor.mana_cost_label", "value", cost.mana()))
                .setLore(lang.component("gui.common.step_5")).build());

        setItem(HEALTH_COST_SLOT, new ItemBuilder(Material.REDSTONE)
                .setName(lang.component("gui.spell_editor.health_cost_label", "value", cost.health()))
                .setLore(lang.component("gui.common.step_1")).build());

        setItem(XP_COST_SLOT, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(lang.component("gui.spell_editor.xp_cost_label", "value", cost.experience()))
                .setLore(lang.component("gui.common.step_5")).build());

        setItem(REAGENT_SLOT, new ItemBuilder(Material.NETHER_WART)
                .setName(lang.component("gui.spell_editor.reagent_label", "value",
                        cost.hasReagent() ? cost.reagentAmount() + "x " + cost.reagentMaterial()
                                : lang.raw("gui.common.none")))
                .setLore(lang.component("gui.spell_editor.reagent_lore")).build());

        setItem(CAST_TIME_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(lang.component("gui.spell_editor.cast_time_label", "ticks", current.castTimeTicks()))
                .setLore(lang.component("gui.spell_editor.cast_time_lore"),
                        lang.component("gui.common.step_20")).build());

        setItem(COOLDOWN_SLOT, new ItemBuilder(Material.REPEATER)
                .setName(lang.component("gui.spell_editor.cooldown_label", "ticks", current.cooldownTicks()))
                .setLore(lang.component("gui.common.step_20")).build());

        setItem(TRIGGER_SLOT, new ItemBuilder(Material.STICK)
                .setName(lang.component("gui.spell_editor.trigger_label", "trigger", current.trigger()))
                .setLore(lang.component("gui.common.click_cycle_masc")).build());

        setItem(TREE_PARENT_SLOT, new ItemBuilder(Material.LADDER)
                .setName(lang.component("gui.spell_editor.tree_parent_label", "value",
                        current.treeParentId() == null ? lang.raw("gui.common.none") : current.treeParentId()))
                .setLore(lang.component("gui.spell_editor.tree_parent_lore")).build());

        setItem(TREE_TIER_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("gui.spell_editor.tree_tier_label", "tier", current.treeTier()))
                .setLore(lang.component("gui.common.step_1")).build());

        setItem(COMPONENTS_SLOT, new ItemBuilder(Material.COMMAND_BLOCK)
                .setName(lang.component("gui.spell_editor.components_label", "count", current.components().size()))
                .setLore(lang.component("gui.spell_editor.components_lore")).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        ClickType click = event.getClick();
        int sign = click == ClickType.RIGHT ? -1 : 1;

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.common.prompt_name"), value -> replace(withDisplayName(value)));
            return;
        }

        if (slot == ICON_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.common.prompt_icon"), this::withIconAndReplace);
            return;
        }

        if (slot == COLOR_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.spell_editor.prompt_color"), this::withColorAndReplace);
            return;
        }

        if (slot == SCHOOL_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.spell_editor.prompt_school"), value -> {
                if (!schoolManager.exists(value.trim().toLowerCase(Locale.ROOT))) {
                    lang.send(player, "gui.spell_editor.unknown_school");
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
            chatPromptManager.prompt(player, lang.raw("gui.common.prompt_description"), value -> replace(withDescription(value)));
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
            chatPromptManager.prompt(player, lang.raw("gui.spell_editor.prompt_reagent"), value -> {

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
            chatPromptManager.prompt(player, lang.raw("gui.spell_editor.prompt_tree_parent"),
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
