package com.sack.rpgroll.economy.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.economy.tax.TaxRule;
import com.sack.rpgroll.economy.tax.TaxRuleManager;
import com.sack.rpgroll.economy.tax.TaxType;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

public class TaxBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final TaxRuleManager taxRuleManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private final LangManager lang;
    private List<TaxRule> rules;

    public TaxBrowserGUI(Player player, TaxRuleManager taxRuleManager, ChatPromptManager chatPromptManager,
            Runnable onBack) {
        super(player, Component.text(chatPromptManager.lang().raw("tax.browser.title"), NamedTextColor.GOLD), SIZE);
        this.taxRuleManager = taxRuleManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
        this.lang = chatPromptManager.lang();
        this.rules = List.copyOf(taxRuleManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < rules.size() && i < 36; i++) {

            TaxRule rule = rules.get(i);

            setItem(i, new ItemBuilder(rule.enabled() ? Material.GOLD_NUGGET : Material.IRON_NUGGET)
                    .setName(ComponentUtils.parse(rule.displayName()))
                    .setLore(lang.component("currency.browser.lore_id", "id", rule.id()),
                            lang.component("tax.browser.lore_type", "type", rule.type()),
                            lang.component("tax.browser.lore_rate", "rate", rule.ratePercent()),
                            Component.text(rule.enabled() ? lang.raw("common.active") : lang.raw("common.inactive"),
                                    rule.enabled() ? NamedTextColor.GREEN : NamedTextColor.RED),
                            lang.component("common.click_edit"))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("tax.browser.new_button")).build());
        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < rules.size() && slot < 36) {
            new TaxEditorGUI(player, rules.get(slot), taxRuleManager, chatPromptManager, this::reopen).open();
            return;
        }

        if (slot == NEW_SLOT) {
            promptNew();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void promptNew() {
        chatPromptManager.prompt(player, lang.raw("tax.browser.prompt_new_id"), value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (taxRuleManager.exists(id)) {
                lang.send(player, "tax.browser.duplicate_id");
                reopen();
                return;
            }

            taxRuleManager.save(new TaxRule(id, id, TaxType.SALE, 0, List.of(), true));
            reopen();
        });
    }

    private void reopen() {
        this.rules = List.copyOf(taxRuleManager.getAll());
        open();
    }

}
