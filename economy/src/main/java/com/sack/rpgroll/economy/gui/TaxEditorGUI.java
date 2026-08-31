package com.sack.rpgroll.economy.gui;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.economy.tax.TaxRule;
import com.sack.rpgroll.economy.tax.TaxRuleManager;
import com.sack.rpgroll.economy.tax.TaxType;
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

public class TaxEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int NAME_SLOT = 10;
    private static final int TYPE_SLOT = 11;
    private static final int RATE_SLOT = 12;
    private static final int APPLIES_TO_SLOT = 13;
    private static final int ENABLED_SLOT = 14;
    private static final int DELETE_SLOT = 31;
    private static final int BACK_SLOT = 40;

    private final TaxRuleManager taxRuleManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private final LangManager lang;
    private TaxRule current;

    public TaxEditorGUI(Player player, TaxRule rule, TaxRuleManager taxRuleManager, ChatPromptManager chatPromptManager,
            Runnable onBack) {
        super(player, ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("tax.editor.title", "id", rule.id()), NamedTextColor.GOLD), SIZE);
        this.current = rule;
        this.taxRuleManager = taxRuleManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
        this.lang = chatPromptManager.lang();
    }

    private void replace(TaxRule updated) {
        current = updated;
        taxRuleManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(lang.component("common.label_name", "value", current.displayName())).build());

        setItem(TYPE_SLOT, new ItemBuilder(Material.COMPARATOR)
                .setName(lang.component("tax.editor.type", "value", current.type()))
                .setLore(lang.component("tax.editor.click_rotate")).build());

        setItem(RATE_SLOT, new ItemBuilder(Material.GOLD_NUGGET)
                .setName(lang.component("tax.editor.rate", "value", current.ratePercent()))
                .setLore(lang.component("tax.editor.click_rate")).build());

        setItem(APPLIES_TO_SLOT, new ItemBuilder(Material.HOPPER)
                .setName(lang.component("tax.editor.applies_to", "value", current.appliesTo().isEmpty()
                        ? lang.raw("common.all") : String.join(", ", current.appliesTo())))
                .setLore(lang.component("tax.editor.applies_hint1"), lang.component("tax.editor.applies_hint2"))
                .build());

        setItem(ENABLED_SLOT, new ItemBuilder(current.enabled() ? Material.LIME_CONCRETE : Material.GRAY_CONCRETE)
                .setName(lang.component("tax.editor.enabled", "value", current.enabled()))
                .setLore(lang.component("common.click_toggle")).build());

        setItem(DELETE_SLOT, new ItemBuilder(Material.BARRIER)
                .setName(lang.component("tax.editor.delete")).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        double sign = event.getClick() == ClickType.RIGHT ? -1 : 1;

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, lang.raw("common.prompt_new_name"), value -> replace(new TaxRule(current.id(),
                    value, current.type(), current.ratePercent(), current.appliesTo(), current.enabled())));
        } else if (slot == TYPE_SLOT) {
            TaxType[] values = TaxType.values();
            TaxType next = values[(current.type().ordinal() + 1) % values.length];
            replace(new TaxRule(current.id(), current.displayName(), next, current.ratePercent(), current.appliesTo(),
                    current.enabled()));
        } else if (slot == RATE_SLOT) {
            replace(new TaxRule(current.id(), current.displayName(), current.type(),
                    Math.max(0, Math.min(100, current.ratePercent() + sign)), current.appliesTo(), current.enabled()));
        } else if (slot == APPLIES_TO_SLOT) {
            chatPromptManager.prompt(player, lang.raw("tax.editor.prompt_applies_to"), value -> {

                List<String> appliesTo = new ArrayList<>();
                for (String entry : value.split(",")) {
                    if (!entry.isBlank()) {
                        appliesTo.add(entry.trim());
                    }
                }

                replace(new TaxRule(current.id(), current.displayName(), current.type(), current.ratePercent(),
                        appliesTo, current.enabled()));
            });
        } else if (slot == ENABLED_SLOT) {
            replace(new TaxRule(current.id(), current.displayName(), current.type(), current.ratePercent(),
                    current.appliesTo(), !current.enabled()));
        } else if (slot == DELETE_SLOT) {
            taxRuleManager.delete(current.id());
            onBack.run();
        } else if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
