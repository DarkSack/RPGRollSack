package com.sack.rpgroll.crafting.gui;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.crafting.station.CustomStation;
import com.sack.rpgroll.crafting.station.CustomStationManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Edita los campos escalares de una {@code CustomStation}. Las listas
 * ({@code ingredient-slots}, {@code allowed-recipe-ids}, {@code structure-requirements},
 * {@code tier-upgrades}) se editan directamente en el YAML de la estación
 * (mismo criterio en todo el ecosistema para campos de lista/mapa anidados).
 */
public class CustomStationEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int NAME_SLOT = 10;
    private static final int ICON_SLOT = 11;
    private static final int BLOCK_SLOT = 12;
    private static final int INVENTORY_SIZE_SLOT = 13;
    private static final int FUEL_SLOT_SLOT = 19;
    private static final int OUTPUT_SLOT_SLOT = 20;
    private static final int REQUIRES_FUEL_SLOT = 21;
    private static final int GUI_TITLE_SLOT = 22;
    private static final int MAX_TIER_SLOT = 14;
    private static final int SPEED_BONUS_SLOT = 15;
    private static final int FAIL_REDUCTION_SLOT = 16;
    private static final int SKILL_CATEGORY_SLOT = 23;
    private static final int EXPERIMENTATION_SLOT = 24;
    private static final int DELETE_SLOT = 31;
    private static final int BACK_SLOT = 40;

    private final CustomStationManager stationManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private CustomStation current;

    public CustomStationEditorGUI(Player player, CustomStation station, CustomStationManager stationManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.station.editor_title", "id", station.id()), NamedTextColor.GOLD), SIZE);
        this.current = station;
        this.stationManager = stationManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(CustomStation updated) {
        current = updated;
        stationManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(chatPromptManager.lang().component("gui.common.field_name", "value", current.displayName())).build());

        setItem(ICON_SLOT, new ItemBuilder(CustomStationBrowserGUI.parseMaterial(current.icon()))
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.common.field_icon", "value", current.icon()), NamedTextColor.YELLOW)).build());

        setItem(BLOCK_SLOT, new ItemBuilder(CustomStationBrowserGUI.parseMaterial(current.triggerBlockMaterial()))
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.station.field_block", "value", current.triggerBlockMaterial()), NamedTextColor.AQUA))
                .build());

        setItem(INVENTORY_SIZE_SLOT, new ItemBuilder(Material.CHEST)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.station.field_inventory_size", "value", current.inventorySize()), NamedTextColor.AQUA))
                .setLore(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.station.inventory_size_hint"), NamedTextColor.GRAY)).build());

        setItem(FUEL_SLOT_SLOT, new ItemBuilder(Material.BLAZE_POWDER)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.station.field_fuel_slot", "value", current.fuelSlot()), NamedTextColor.GOLD))
                .setLore(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.station.fuel_slot_hint"), NamedTextColor.GRAY))
                .build());

        setItem(OUTPUT_SLOT_SLOT, new ItemBuilder(Material.HOPPER)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.station.field_output_slot", "value", current.outputSlot()), NamedTextColor.AQUA))
                .setLore(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.common.click_inc_dec", "inc", "+1", "dec", "-1"), NamedTextColor.GRAY)).build());

        setItem(REQUIRES_FUEL_SLOT, new ItemBuilder(current.requiresFuel() ? Material.LIME_CONCRETE : Material.GRAY_CONCRETE)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.station.field_requires_fuel", "value", current.requiresFuel()), NamedTextColor.GOLD))
                .setLore(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.common.click_toggle"), NamedTextColor.GRAY)).build());

        setItem(GUI_TITLE_SLOT, new ItemBuilder(Material.OAK_SIGN)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.station.field_gui_title", "value", current.guiTitle()), NamedTextColor.YELLOW)).build());

        setItem(MAX_TIER_SLOT, new ItemBuilder(Material.ANVIL)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.station.field_max_tier", "value", current.maxTier()), NamedTextColor.GOLD))
                .setLore(
                        ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.station.max_tier_hint"), NamedTextColor.GRAY),
                        ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.station.max_tier_yaml_hint"), NamedTextColor.DARK_GRAY))
                .build());

        setItem(SPEED_BONUS_SLOT, new ItemBuilder(Material.SUGAR)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.station.field_speed_bonus", "value", percent(current.speedBonusPerTier())), NamedTextColor.AQUA))
                .setLore(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.station.percent_hint"), NamedTextColor.GRAY)).build());

        setItem(FAIL_REDUCTION_SLOT, new ItemBuilder(Material.SHIELD)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.station.field_fail_reduction", "value", percent(current.failReductionPerTier())), NamedTextColor.AQUA))
                .setLore(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.station.percent_hint"), NamedTextColor.GRAY)).build());

        setItem(SKILL_CATEGORY_SLOT, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.station.field_skill_category", "value", current.skillCategory()), NamedTextColor.LIGHT_PURPLE))
                .setLore(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.station.skill_category_hint"), NamedTextColor.GRAY))
                .build());

        setItem(EXPERIMENTATION_SLOT,
                new ItemBuilder(current.allowExperimentation() ? Material.LIME_CONCRETE : Material.GRAY_CONCRETE)
                        .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.station.field_allow_experimentation", "value", current.allowExperimentation()), NamedTextColor.GOLD))
                        .setLore(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.station.allow_experimentation_hint"), NamedTextColor.GRAY))
                        .build());

        setItem(DELETE_SLOT, new ItemBuilder(Material.BARRIER)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.station.delete"), NamedTextColor.RED)).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(chatPromptManager.lang().raw("gui.common.back")));
    }

    private String percent(double fraction) {
        return Math.round(fraction * 100) + "%";
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        int sign = event.getClick() == ClickType.RIGHT ? -1 : 1;

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.common.prompt_name"), value -> replace(withName(value)));
        } else if (slot == ICON_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.common.prompt_icon"), value -> replace(withIcon(value)));
        } else if (slot == BLOCK_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.station.prompt_block"),
                    value -> replace(withBlock(value)));
        } else if (slot == INVENTORY_SIZE_SLOT) {
            replace(withInventorySize(current.inventorySize() + sign * 9));
        } else if (slot == FUEL_SLOT_SLOT) {
            replace(withFuelSlot(current.fuelSlot() + sign));
        } else if (slot == OUTPUT_SLOT_SLOT) {
            replace(withOutputSlot(current.outputSlot() + sign));
        } else if (slot == REQUIRES_FUEL_SLOT) {
            replace(withRequiresFuel(!current.requiresFuel()));
        } else if (slot == GUI_TITLE_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.station.prompt_gui_title"), value -> replace(withGuiTitle(value)));
        } else if (slot == MAX_TIER_SLOT) {
            replace(withMaxTier(Math.max(1, current.maxTier() + sign)));
        } else if (slot == SPEED_BONUS_SLOT) {
            replace(withSpeedBonus(clamp01(current.speedBonusPerTier() + sign * 0.05)));
        } else if (slot == FAIL_REDUCTION_SLOT) {
            replace(withFailReduction(clamp01(current.failReductionPerTier() + sign * 0.05)));
        } else if (slot == SKILL_CATEGORY_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.station.prompt_skill_category"),
                    value -> replace(withSkillCategory(value)));
        } else if (slot == EXPERIMENTATION_SLOT) {
            replace(withAllowExperimentation(!current.allowExperimentation()));
        } else if (slot == DELETE_SLOT) {
            stationManager.delete(current.id());
            onBack.run();
        } else if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private double clamp01(double v) {
        return Math.min(1, Math.max(0, v));
    }

    /** -1 (sin combustible) siempre válido; si no, nunca por fuera de [0, inventorySize). */
    private int clampFuelSlot(int slot, int inventorySize) {
        if (slot < 0) {
            return -1;
        }
        return Math.min(inventorySize - 1, slot);
    }

    private int clampOutputSlot(int slot, int inventorySize) {
        return Math.min(inventorySize - 1, Math.max(0, slot));
    }

    private CustomStation withName(String v) {
        return rebuild(current.id(), v, current.icon(), current.triggerBlockMaterial(), current.inventorySize(),
                current.fuelSlot(), current.outputSlot(), current.requiresFuel(), current.guiTitle(),
                current.maxTier(), current.speedBonusPerTier(), current.failReductionPerTier(),
                current.skillCategory(), current.allowExperimentation());
    }

    private CustomStation withIcon(String v) {
        return rebuild(current.id(), current.displayName(), v, current.triggerBlockMaterial(), current.inventorySize(),
                current.fuelSlot(), current.outputSlot(), current.requiresFuel(), current.guiTitle(),
                current.maxTier(), current.speedBonusPerTier(), current.failReductionPerTier(),
                current.skillCategory(), current.allowExperimentation());
    }

    private CustomStation withBlock(String v) {
        return rebuild(current.id(), current.displayName(), current.icon(), v, current.inventorySize(),
                current.fuelSlot(), current.outputSlot(), current.requiresFuel(), current.guiTitle(),
                current.maxTier(), current.speedBonusPerTier(), current.failReductionPerTier(),
                current.skillCategory(), current.allowExperimentation());
    }

    private CustomStation withInventorySize(int v) {
        // CustomStation ya redondea/limita v en su constructor compacto — recalculamos
        // el tamaño real ANTES de re-clampear fuel/output, si no quedarían clampeados
        // contra el tamaño viejo.
        CustomStation probe = rebuild(current.id(), current.displayName(), current.icon(), current.triggerBlockMaterial(),
                v, current.fuelSlot(), current.outputSlot(), current.requiresFuel(), current.guiTitle(),
                current.maxTier(), current.speedBonusPerTier(), current.failReductionPerTier(),
                current.skillCategory(), current.allowExperimentation());
        int newSize = probe.inventorySize();
        return rebuild(current.id(), current.displayName(), current.icon(), current.triggerBlockMaterial(), newSize,
                clampFuelSlot(current.fuelSlot(), newSize), clampOutputSlot(current.outputSlot(), newSize),
                current.requiresFuel(), current.guiTitle(), current.maxTier(), current.speedBonusPerTier(),
                current.failReductionPerTier(), current.skillCategory(), current.allowExperimentation());
    }

    private CustomStation withFuelSlot(int v) {
        return rebuild(current.id(), current.displayName(), current.icon(), current.triggerBlockMaterial(),
                current.inventorySize(), clampFuelSlot(v, current.inventorySize()), current.outputSlot(),
                current.requiresFuel(), current.guiTitle(), current.maxTier(), current.speedBonusPerTier(),
                current.failReductionPerTier(), current.skillCategory(), current.allowExperimentation());
    }

    private CustomStation withOutputSlot(int v) {
        return rebuild(current.id(), current.displayName(), current.icon(), current.triggerBlockMaterial(),
                current.inventorySize(), current.fuelSlot(), clampOutputSlot(v, current.inventorySize()),
                current.requiresFuel(), current.guiTitle(), current.maxTier(), current.speedBonusPerTier(),
                current.failReductionPerTier(), current.skillCategory(), current.allowExperimentation());
    }

    private CustomStation withRequiresFuel(boolean v) {
        return rebuild(current.id(), current.displayName(), current.icon(), current.triggerBlockMaterial(),
                current.inventorySize(), current.fuelSlot(), current.outputSlot(), v, current.guiTitle(),
                current.maxTier(), current.speedBonusPerTier(), current.failReductionPerTier(),
                current.skillCategory(), current.allowExperimentation());
    }

    private CustomStation withGuiTitle(String v) {
        return rebuild(current.id(), current.displayName(), current.icon(), current.triggerBlockMaterial(),
                current.inventorySize(), current.fuelSlot(), current.outputSlot(), current.requiresFuel(), v,
                current.maxTier(), current.speedBonusPerTier(), current.failReductionPerTier(),
                current.skillCategory(), current.allowExperimentation());
    }

    private CustomStation withMaxTier(int v) {
        return rebuild(current.id(), current.displayName(), current.icon(), current.triggerBlockMaterial(),
                current.inventorySize(), current.fuelSlot(), current.outputSlot(), current.requiresFuel(),
                current.guiTitle(), v, current.speedBonusPerTier(), current.failReductionPerTier(),
                current.skillCategory(), current.allowExperimentation());
    }

    private CustomStation withSpeedBonus(double v) {
        return rebuild(current.id(), current.displayName(), current.icon(), current.triggerBlockMaterial(),
                current.inventorySize(), current.fuelSlot(), current.outputSlot(), current.requiresFuel(),
                current.guiTitle(), current.maxTier(), v, current.failReductionPerTier(),
                current.skillCategory(), current.allowExperimentation());
    }

    private CustomStation withFailReduction(double v) {
        return rebuild(current.id(), current.displayName(), current.icon(), current.triggerBlockMaterial(),
                current.inventorySize(), current.fuelSlot(), current.outputSlot(), current.requiresFuel(),
                current.guiTitle(), current.maxTier(), current.speedBonusPerTier(), v,
                current.skillCategory(), current.allowExperimentation());
    }

    private CustomStation withSkillCategory(String v) {
        return rebuild(current.id(), current.displayName(), current.icon(), current.triggerBlockMaterial(),
                current.inventorySize(), current.fuelSlot(), current.outputSlot(), current.requiresFuel(),
                current.guiTitle(), current.maxTier(), current.speedBonusPerTier(), current.failReductionPerTier(),
                v, current.allowExperimentation());
    }

    private CustomStation withAllowExperimentation(boolean v) {
        return rebuild(current.id(), current.displayName(), current.icon(), current.triggerBlockMaterial(),
                current.inventorySize(), current.fuelSlot(), current.outputSlot(), current.requiresFuel(),
                current.guiTitle(), current.maxTier(), current.speedBonusPerTier(), current.failReductionPerTier(),
                current.skillCategory(), v);
    }

    /** Reconstruye la estación preservando las listas actuales (solo editables por YAML). */
    private CustomStation rebuild(String id, String displayName, String icon, String triggerBlockMaterial,
            int inventorySize, int fuelSlot, int outputSlot, boolean requiresFuel, String guiTitle, int maxTier,
            double speedBonusPerTier, double failReductionPerTier, String skillCategory, boolean allowExperimentation) {
        return new CustomStation(id, displayName, icon, triggerBlockMaterial, inventorySize,
                current.ingredientSlots(), fuelSlot, outputSlot, requiresFuel, guiTitle, current.allowedRecipeIds(),
                current.structureRequirements(), maxTier, current.tierUpgrades(), speedBonusPerTier,
                failReductionPerTier, skillCategory, allowExperimentation);
    }

}
