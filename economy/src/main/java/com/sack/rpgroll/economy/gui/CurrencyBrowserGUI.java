package com.sack.rpgroll.economy.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.economy.currency.Currency;
import com.sack.rpgroll.economy.currency.CurrencyManager;
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

public class CurrencyBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final CurrencyManager currencyManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private final LangManager lang;
    private List<Currency> currencies;

    public CurrencyBrowserGUI(Player player, CurrencyManager currencyManager, ChatPromptManager chatPromptManager,
            Runnable onBack) {
        super(player, ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("currency.browser.title"), NamedTextColor.GOLD), SIZE);
        this.currencyManager = currencyManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
        this.lang = chatPromptManager.lang();
        this.currencies = List.copyOf(currencyManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < currencies.size() && i < 36; i++) {

            Currency currency = currencies.get(i);

            setItem(i, new ItemBuilder(parseMaterial(currency.icon()))
                    .setName(ComponentUtils.parse(currency.displayName()))
                    .setLore(lang.component("currency.browser.lore_id", "id", currency.id()),
                            lang.component("currency.browser.lore_symbol", "symbol", currency.symbol()),
                            currency.isBase() ? lang.component("currency.browser.lore_base") : Component.empty(),
                            lang.component("common.click_edit"))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("currency.browser.new_button")).build());
        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < currencies.size() && slot < 36) {
            new CurrencyEditorGUI(player, currencies.get(slot), currencyManager, chatPromptManager, this::reopen).open();
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
        chatPromptManager.prompt(player, lang.raw("currency.browser.prompt_new_id"), value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (currencyManager.exists(id)) {
                lang.send(player, "currency.browser.duplicate_id");
                reopen();
                return;
            }

            currencyManager.save(new Currency(id, id, "$", 2, "SUNFLOWER", "GOLD", 0, 0, null, 1.0, false));
            reopen();
        });
    }

    static Material parseMaterial(String raw) {
        try {
            return Material.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return Material.SUNFLOWER;
        }
    }

    private void reopen() {
        this.currencies = List.copyOf(currencyManager.getAll());
        open();
    }

}
