package com.sack.rpgroll.crafting.gui;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.crafting.condition.RecipeCondition;
import com.sack.rpgroll.crafting.recipe.RecipeResult;
import com.sack.rpgroll.crafting.recipe.RecipeResultType;
import com.sack.rpgroll.crafting.villager.VillagerTradeDefinition;
import com.sack.rpgroll.crafting.villager.VillagerTradeManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

/** Edita una {@code VillagerTradeDefinition} completa (1 o 2 costos). */
public class VillagerTradeEditorGUI extends InventoryGUI {

    private static final int SIZE = 54;

    private static final int NAME_SLOT = 10;
    private static final int ICON_SLOT = 11;
    private static final int COST1_TYPE_SLOT = 12;
    private static final int COST1_VALUE_SLOT = 13;
    private static final int COST1_AMOUNT_SLOT = 14;
    private static final int COST2_TYPE_SLOT = 15;
    private static final int COST2_VALUE_SLOT = 16;
    private static final int COST2_AMOUNT_SLOT = 17;
    private static final int RESULT_TYPE_SLOT = 19;
    private static final int RESULT_VALUE_SLOT = 20;
    private static final int RESULT_AMOUNT_SLOT = 21;
    private static final int MAX_USES_SLOT = 22;
    private static final int VILLAGER_XP_SLOT = 23;
    private static final int REWARDS_EXP_SLOT = 24;
    private static final int QUALITY_ENABLED_SLOT = 25;
    private static final int TOGGLE_COST2_SLOT = 26;
    private static final int CONDITIONS_SLOT = 28;
    private static final int XP_AMOUNT_SLOT = 29;
    private static final int ECONOMY_CURRENCY_SLOT = 30;
    private static final int ECONOMY_COST_SLOT = 31;
    private static final int DELETE_SLOT = 40;
    private static final int BACK_SLOT = 49;

    private final VillagerTradeManager tradeManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private VillagerTradeDefinition current;

    public VillagerTradeEditorGUI(Player player, VillagerTradeDefinition trade, VillagerTradeManager tradeManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.villager_trade.editor_title", "id", trade.id()), NamedTextColor.GOLD), SIZE);
        this.current = trade;
        this.tradeManager = tradeManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(VillagerTradeDefinition updated) {
        current = updated;
        tradeManager.save(current);
        build();
    }

    private RecipeResult cost1() {
        return current.costs().get(0);
    }

    private RecipeResult cost2() {
        return current.costs().size() > 1 ? current.costs().get(1) : null;
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

        RecipeResult cost1 = cost1();
        setItem(COST1_TYPE_SLOT, new ItemBuilder(Material.CHEST)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.villager_trade.field_cost1_type", "value", cost1.type()), NamedTextColor.AQUA))
                .setLore(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.common.toggle_material_item_hint"), NamedTextColor.GRAY)).build());
        setItem(COST1_VALUE_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.villager_trade.field_cost1_value", "value", cost1.value()), NamedTextColor.YELLOW)).build());
        setItem(COST1_AMOUNT_SLOT, new ItemBuilder(Material.HOPPER)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.villager_trade.field_cost1_amount", "value", cost1.amount()), NamedTextColor.AQUA))
                .setLore(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.common.click_inc_dec", "inc", "+1", "dec", "-1"), NamedTextColor.GRAY)).build());

        RecipeResult cost2 = cost2();
        if (cost2 != null) {
            setItem(COST2_TYPE_SLOT, new ItemBuilder(Material.CHEST)
                    .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.villager_trade.field_cost2_type", "value", cost2.type()), NamedTextColor.AQUA))
                    .setLore(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.common.toggle_material_item_hint"), NamedTextColor.GRAY)).build());
            setItem(COST2_VALUE_SLOT, new ItemBuilder(Material.GOLD_INGOT)
                    .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.villager_trade.field_cost2_value", "value", cost2.value()), NamedTextColor.YELLOW)).build());
            setItem(COST2_AMOUNT_SLOT, new ItemBuilder(Material.HOPPER)
                    .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.villager_trade.field_cost2_amount", "value", cost2.amount()), NamedTextColor.AQUA))
                    .setLore(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.common.click_inc_dec", "inc", "+1", "dec", "-1"), NamedTextColor.GRAY)).build());
        }

        setItem(TOGGLE_COST2_SLOT, new ItemBuilder(cost2 != null ? Material.BARRIER : Material.LIME_CONCRETE)
                .setName(Component.text(cost2 != null ? chatPromptManager.lang().raw("gui.villager_trade.remove_cost2") : chatPromptManager.lang().raw("gui.villager_trade.add_cost2"),
                        cost2 != null ? NamedTextColor.RED : NamedTextColor.GREEN))
                .build());

        setItem(RESULT_TYPE_SLOT, new ItemBuilder(Material.CHEST)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.common.result_type", "value", current.result().type()), NamedTextColor.AQUA))
                .setLore(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.common.toggle_material_item_hint"), NamedTextColor.GRAY)).build());
        setItem(RESULT_VALUE_SLOT, new ItemBuilder(Material.PAPER)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.common.result_value", "value", current.result().value()), NamedTextColor.YELLOW)).build());
        setItem(RESULT_AMOUNT_SLOT, new ItemBuilder(Material.HOPPER)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.common.result_amount", "value", current.result().amount()), NamedTextColor.AQUA))
                .setLore(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.common.click_inc_dec", "inc", "+1", "dec", "-1"), NamedTextColor.GRAY)).build());

        setItem(MAX_USES_SLOT, new ItemBuilder(Material.VILLAGER_SPAWN_EGG)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.villager_trade.field_max_uses", "value", current.maxUses()), NamedTextColor.AQUA))
                .setLore(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.common.click_inc_dec", "inc", "+1", "dec", "-1"), NamedTextColor.GRAY)).build());

        setItem(VILLAGER_XP_SLOT, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.villager_trade.field_villager_xp", "value", current.villagerExperience()), NamedTextColor.AQUA))
                .setLore(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.common.click_inc_dec", "inc", "+1", "dec", "-1"), NamedTextColor.GRAY)).build());

        setItem(REWARDS_EXP_SLOT, new ItemBuilder(current.rewardsExperience() ? Material.LIME_CONCRETE : Material.GRAY_CONCRETE)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.villager_trade.field_rewards_experience", "value", current.rewardsExperience()), NamedTextColor.GOLD))
                .setLore(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.common.click_toggle"), NamedTextColor.GRAY)).build());

        setItem(QUALITY_ENABLED_SLOT, new ItemBuilder(current.qualityEnabled() ? Material.LIME_CONCRETE : Material.GRAY_CONCRETE)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.villager_trade.field_quality_enabled", "value", current.qualityEnabled()), NamedTextColor.GOLD))
                .setLore(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.common.click_toggle"), NamedTextColor.GRAY)).build());

        setItem(CONDITIONS_SLOT, new ItemBuilder(Material.COMPARATOR)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.common.conditions_count", "count", current.conditions().size()), NamedTextColor.AQUA))
                .setLore(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.common.click_edit_list"), NamedTextColor.GRAY)).build());

        setItem(XP_AMOUNT_SLOT, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.villager_trade.field_xp_amount", "value", current.xpAmount()), NamedTextColor.AQUA))
                .setLore(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.recipe.plus5_minus5_hint"), NamedTextColor.GRAY)).build());

        setItem(ECONOMY_CURRENCY_SLOT, new ItemBuilder(Material.GOLD_NUGGET)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.villager_trade.field_economy_currency", "value",
                        current.economyCurrencyId() == null ? chatPromptManager.lang().raw("gui.recipe.currency_base") : current.economyCurrencyId()), NamedTextColor.YELLOW))
                .setLore(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.common.click_retype"), NamedTextColor.GRAY)).build());

        setItem(ECONOMY_COST_SLOT, new ItemBuilder(Material.SUNFLOWER)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.villager_trade.field_economy_cost", "value", current.economyCost()), NamedTextColor.AQUA))
                .setLore(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.villager_trade.economy_cost_hint"), NamedTextColor.GRAY)).build());

        setItem(DELETE_SLOT, new ItemBuilder(Material.BARRIER)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.villager_trade.delete"), NamedTextColor.RED)).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(chatPromptManager.lang().raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        int sign = event.getClick() == ClickType.RIGHT ? -1 : 1;

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.common.prompt_name"), value -> replace(withDisplayName(value)));
        } else if (slot == ICON_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.common.prompt_icon"), value -> replace(withIcon(value)));
        } else if (slot == COST1_TYPE_SLOT) {
            replace(withCost(0, toggledType(cost1())));
        } else if (slot == COST1_VALUE_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.villager_trade.prompt_cost1_value"),
                    value -> replace(withCost(0, new RecipeResult(cost1().type(), value, cost1().amount()))));
        } else if (slot == COST1_AMOUNT_SLOT) {
            replace(withCost(0, new RecipeResult(cost1().type(), cost1().value(), Math.max(1, cost1().amount() + sign))));
        } else if (slot == COST2_TYPE_SLOT && cost2() != null) {
            replace(withCost(1, toggledType(cost2())));
        } else if (slot == COST2_VALUE_SLOT && cost2() != null) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.villager_trade.prompt_cost2_value"),
                    value -> replace(withCost(1, new RecipeResult(cost2().type(), value, cost2().amount()))));
        } else if (slot == COST2_AMOUNT_SLOT && cost2() != null) {
            RecipeResult c2 = cost2();
            replace(withCost(1, new RecipeResult(c2.type(), c2.value(), Math.max(1, c2.amount() + sign))));
        } else if (slot == TOGGLE_COST2_SLOT) {
            replace(withToggledCost2());
        } else if (slot == RESULT_TYPE_SLOT) {
            replace(withResult(toggledType(current.result())));
        } else if (slot == RESULT_VALUE_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.common.prompt_result_value"), value -> replace(
                    withResult(new RecipeResult(current.result().type(), value, current.result().amount()))));
        } else if (slot == RESULT_AMOUNT_SLOT) {
            replace(withResult(new RecipeResult(current.result().type(), current.result().value(),
                    Math.max(1, current.result().amount() + sign))));
        } else if (slot == MAX_USES_SLOT) {
            replace(withMaxUses(Math.max(1, current.maxUses() + sign)));
        } else if (slot == VILLAGER_XP_SLOT) {
            replace(withVillagerExperience(Math.max(0, current.villagerExperience() + sign)));
        } else if (slot == REWARDS_EXP_SLOT) {
            replace(withRewardsExperience(!current.rewardsExperience()));
        } else if (slot == QUALITY_ENABLED_SLOT) {
            replace(withQualityEnabled(!current.qualityEnabled()));
        } else if (slot == CONDITIONS_SLOT) {
            new RecipeConditionsEditorGUI(player, chatPromptManager.lang().raw("gui.conditions.title", "id", current.id()), current.conditions(),
                    chatPromptManager, this::replaceConditions, this::reopen).open();
        } else if (slot == XP_AMOUNT_SLOT) {
            replace(withXpAmount(Math.max(0, current.xpAmount() + sign * 5)));
        } else if (slot == ECONOMY_CURRENCY_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.villager_trade.prompt_economy_currency"),
                    value -> replace(withEconomyCurrency(value.isBlank() ? null : value)));
        } else if (slot == ECONOMY_COST_SLOT) {
            replace(withEconomyCost(Math.max(0, current.economyCost() + sign * 10)));
        } else if (slot == DELETE_SLOT) {
            tradeManager.delete(current.id());
            onBack.run();
        } else if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private RecipeResult toggledType(RecipeResult r) {
        RecipeResultType next = r.type() == RecipeResultType.MATERIAL ? RecipeResultType.ITEM_ID : RecipeResultType.MATERIAL;
        return new RecipeResult(next, r.value(), r.amount());
    }

    private void replaceConditions(List<RecipeCondition> updated) {
        replace(withConditions(updated));
    }

    private void reopen() {
        open();
    }

    private VillagerTradeDefinition withCost(int index, RecipeResult v) {
        List<RecipeResult> costs = new ArrayList<>(current.costs());
        costs.set(index, v);
        return rebuild(current.displayName(), current.icon(), costs, current.result(), current.maxUses(),
                current.villagerExperience(), current.rewardsExperience(), current.conditions(), current.xpAmount(),
                current.economyCurrencyId(), current.economyCost(), current.qualityEnabled());
    }

    private VillagerTradeDefinition withToggledCost2() {
        List<RecipeResult> costs = new ArrayList<>(current.costs());
        if (costs.size() > 1) {
            costs.remove(1);
        } else {
            costs.add(new RecipeResult(RecipeResultType.MATERIAL, "GOLD_INGOT", 1));
        }
        return rebuild(current.displayName(), current.icon(), costs, current.result(), current.maxUses(),
                current.villagerExperience(), current.rewardsExperience(), current.conditions(), current.xpAmount(),
                current.economyCurrencyId(), current.economyCost(), current.qualityEnabled());
    }

    private VillagerTradeDefinition withDisplayName(String v) {
        return rebuild(v, current.icon(), current.costs(), current.result(), current.maxUses(),
                current.villagerExperience(), current.rewardsExperience(), current.conditions(), current.xpAmount(),
                current.economyCurrencyId(), current.economyCost(), current.qualityEnabled());
    }

    private VillagerTradeDefinition withIcon(String v) {
        return rebuild(current.displayName(), v, current.costs(), current.result(), current.maxUses(),
                current.villagerExperience(), current.rewardsExperience(), current.conditions(), current.xpAmount(),
                current.economyCurrencyId(), current.economyCost(), current.qualityEnabled());
    }

    private VillagerTradeDefinition withResult(RecipeResult v) {
        return rebuild(current.displayName(), current.icon(), current.costs(), v, current.maxUses(),
                current.villagerExperience(), current.rewardsExperience(), current.conditions(), current.xpAmount(),
                current.economyCurrencyId(), current.economyCost(), current.qualityEnabled());
    }

    private VillagerTradeDefinition withMaxUses(int v) {
        return rebuild(current.displayName(), current.icon(), current.costs(), current.result(), v,
                current.villagerExperience(), current.rewardsExperience(), current.conditions(), current.xpAmount(),
                current.economyCurrencyId(), current.economyCost(), current.qualityEnabled());
    }

    private VillagerTradeDefinition withVillagerExperience(int v) {
        return rebuild(current.displayName(), current.icon(), current.costs(), current.result(), current.maxUses(),
                v, current.rewardsExperience(), current.conditions(), current.xpAmount(),
                current.economyCurrencyId(), current.economyCost(), current.qualityEnabled());
    }

    private VillagerTradeDefinition withRewardsExperience(boolean v) {
        return rebuild(current.displayName(), current.icon(), current.costs(), current.result(), current.maxUses(),
                current.villagerExperience(), v, current.conditions(), current.xpAmount(),
                current.economyCurrencyId(), current.economyCost(), current.qualityEnabled());
    }

    private VillagerTradeDefinition withConditions(List<RecipeCondition> v) {
        return rebuild(current.displayName(), current.icon(), current.costs(), current.result(), current.maxUses(),
                current.villagerExperience(), current.rewardsExperience(), v, current.xpAmount(),
                current.economyCurrencyId(), current.economyCost(), current.qualityEnabled());
    }

    private VillagerTradeDefinition withXpAmount(double v) {
        return rebuild(current.displayName(), current.icon(), current.costs(), current.result(), current.maxUses(),
                current.villagerExperience(), current.rewardsExperience(), current.conditions(), v,
                current.economyCurrencyId(), current.economyCost(), current.qualityEnabled());
    }

    private VillagerTradeDefinition withEconomyCurrency(String v) {
        return rebuild(current.displayName(), current.icon(), current.costs(), current.result(), current.maxUses(),
                current.villagerExperience(), current.rewardsExperience(), current.conditions(), current.xpAmount(),
                v, current.economyCost(), current.qualityEnabled());
    }

    private VillagerTradeDefinition withEconomyCost(double v) {
        return rebuild(current.displayName(), current.icon(), current.costs(), current.result(), current.maxUses(),
                current.villagerExperience(), current.rewardsExperience(), current.conditions(), current.xpAmount(),
                current.economyCurrencyId(), v, current.qualityEnabled());
    }

    private VillagerTradeDefinition withQualityEnabled(boolean v) {
        return rebuild(current.displayName(), current.icon(), current.costs(), current.result(), current.maxUses(),
                current.villagerExperience(), current.rewardsExperience(), current.conditions(), current.xpAmount(),
                current.economyCurrencyId(), current.economyCost(), v);
    }

    private VillagerTradeDefinition rebuild(String displayName, String icon, List<RecipeResult> costs, RecipeResult result,
            int maxUses, int villagerExperience, boolean rewardsExperience, List<RecipeCondition> conditions,
            double xpAmount, String economyCurrencyId, double economyCost, boolean qualityEnabled) {
        return new VillagerTradeDefinition(current.id(), displayName, icon, costs, result, maxUses, villagerExperience,
                rewardsExperience, conditions, xpAmount, economyCurrencyId, economyCost, qualityEnabled);
    }

}
