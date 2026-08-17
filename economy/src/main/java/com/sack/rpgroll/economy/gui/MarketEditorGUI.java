package com.sack.rpgroll.economy.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.economy.market.MarketProduct;
import com.sack.rpgroll.economy.market.MarketProductManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

public class MarketEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int NAME_SLOT = 10;
    private static final int ICON_SLOT = 11;
    private static final int CURRENCY_SLOT = 12;
    private static final int CATEGORY_SLOT = 13;
    private static final int BASE_PRICE_SLOT = 19;
    private static final int MIN_PRICE_SLOT = 20;
    private static final int MAX_PRICE_SLOT = 21;
    private static final int SUPPLY_WEIGHT_SLOT = 22;
    private static final int DEMAND_WEIGHT_SLOT = 23;
    private static final int VOLATILITY_SLOT = 24;
    private static final int RECOVERY_RATE_SLOT = 25;
    private static final int DELETE_SLOT = 31;
    private static final int BACK_SLOT = 40;

    private final MarketProductManager productManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private final LangManager lang;
    private MarketProduct current;

    public MarketEditorGUI(Player player, MarketProduct product, MarketProductManager productManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text(chatPromptManager.lang().raw("market.editor.title", "id", product.id()),
                NamedTextColor.GOLD), SIZE);
        this.current = product;
        this.productManager = productManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
        this.lang = chatPromptManager.lang();
    }

    private void replace(MarketProduct updated) {
        current = updated;
        productManager.save(current);
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

        setItem(ICON_SLOT, new ItemBuilder(CurrencyBrowserGUI.parseMaterial(current.icon()))
                .setName(lang.component("common.label_icon", "value", current.icon())).build());

        setItem(CURRENCY_SLOT, new ItemBuilder(Material.SUNFLOWER)
                .setName(lang.component("market.editor.currency", "value", current.currencyId() == null
                        ? lang.raw("market.editor.currency_default") : current.currencyId()))
                .build());

        setItem(CATEGORY_SLOT, new ItemBuilder(Material.HOPPER)
                .setName(lang.component("market.editor.category", "value", current.category())).build());

        setItem(BASE_PRICE_SLOT, new ItemBuilder(Material.GOLD_INGOT)
                .setName(lang.component("market.editor.base_price", "value", current.basePrice()))
                .setLore(lang.component("common.click_plus1_minus1")).build());

        setItem(MIN_PRICE_SLOT, new ItemBuilder(Material.IRON_NUGGET)
                .setName(lang.component("market.editor.min_price", "value", current.minPrice()))
                .setLore(lang.component("common.click_plus1_minus1")).build());

        setItem(MAX_PRICE_SLOT, new ItemBuilder(Material.DIAMOND)
                .setName(lang.component("market.editor.max_price", "value", current.maxPrice()))
                .setLore(lang.component("common.click_plus1_minus1")).build());

        setItem(SUPPLY_WEIGHT_SLOT, new ItemBuilder(Material.HAY_BLOCK)
                .setName(lang.component("market.editor.supply_weight", "value", current.supplyWeight()))
                .setLore(lang.component("common.click_plus01_minus01")).build());

        setItem(DEMAND_WEIGHT_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("market.editor.demand_weight", "value", current.demandWeight()))
                .setLore(lang.component("common.click_plus01_minus01")).build());

        setItem(VOLATILITY_SLOT, new ItemBuilder(Material.TNT)
                .setName(lang.component("market.editor.volatility", "value", current.volatility()))
                .setLore(lang.component("market.editor.click_plus005_minus005")).build());

        setItem(RECOVERY_RATE_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(lang.component("market.editor.recovery_rate", "value", current.recoveryRate()))
                .setLore(lang.component("common.click_plus001_minus001")).build());

        setItem(DELETE_SLOT, new ItemBuilder(Material.BARRIER)
                .setName(lang.component("market.editor.delete")).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        double sign = event.getClick() == ClickType.RIGHT ? -1 : 1;

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, lang.raw("common.prompt_new_name"), value -> replace(with(current, "name", value)));
        } else if (slot == ICON_SLOT) {
            chatPromptManager.prompt(player, lang.raw("common.prompt_material_icon"), value -> replace(with(current, "icon", value)));
        } else if (slot == CURRENCY_SLOT) {
            chatPromptManager.prompt(player, lang.raw("market.editor.prompt_currency"), value -> replace(with(current,
                    "currency", value.equalsIgnoreCase(lang.raw("common.default_keyword")) ? null : value)));
        } else if (slot == CATEGORY_SLOT) {
            chatPromptManager.prompt(player, lang.raw("market.editor.prompt_category"), value -> replace(with(current, "category", value)));
        } else if (slot == BASE_PRICE_SLOT) {
            replace(withNumber(current.basePrice() + sign, "base"));
        } else if (slot == MIN_PRICE_SLOT) {
            replace(withNumber(Math.max(0, current.minPrice() + sign), "min"));
        } else if (slot == MAX_PRICE_SLOT) {
            replace(withNumber(current.maxPrice() + sign, "max"));
        } else if (slot == SUPPLY_WEIGHT_SLOT) {
            replace(withNumber(Math.max(0.1, current.supplyWeight() + sign * 0.1), "supply"));
        } else if (slot == DEMAND_WEIGHT_SLOT) {
            replace(withNumber(Math.max(0.1, current.demandWeight() + sign * 0.1), "demand"));
        } else if (slot == VOLATILITY_SLOT) {
            replace(withNumber(Math.max(0.01, current.volatility() + sign * 0.05), "volatility"));
        } else if (slot == RECOVERY_RATE_SLOT) {
            replace(withNumber(Math.max(0, current.recoveryRate() + sign * 0.01), "recovery"));
        } else if (slot == DELETE_SLOT) {
            productManager.delete(current.id());
            onBack.run();
        } else if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private MarketProduct with(MarketProduct base, String field, String value) {
        return switch (field) {
            case "name" -> new MarketProduct(base.id(), value, base.icon(), base.currencyId(), base.basePrice(),
                    base.minPrice(), base.maxPrice(), base.supplyWeight(), base.demandWeight(), base.volatility(),
                    base.recoveryRate(), base.category());
            case "icon" -> new MarketProduct(base.id(), base.displayName(), value, base.currencyId(), base.basePrice(),
                    base.minPrice(), base.maxPrice(), base.supplyWeight(), base.demandWeight(), base.volatility(),
                    base.recoveryRate(), base.category());
            case "currency" -> new MarketProduct(base.id(), base.displayName(), base.icon(), value, base.basePrice(),
                    base.minPrice(), base.maxPrice(), base.supplyWeight(), base.demandWeight(), base.volatility(),
                    base.recoveryRate(), base.category());
            case "category" -> new MarketProduct(base.id(), base.displayName(), base.icon(), base.currencyId(),
                    base.basePrice(), base.minPrice(), base.maxPrice(), base.supplyWeight(), base.demandWeight(),
                    base.volatility(), base.recoveryRate(), value);
            default -> base;
        };
    }

    private MarketProduct withNumber(double value, String field) {
        return switch (field) {
            case "base" -> new MarketProduct(current.id(), current.displayName(), current.icon(), current.currencyId(),
                    value, current.minPrice(), current.maxPrice(), current.supplyWeight(), current.demandWeight(),
                    current.volatility(), current.recoveryRate(), current.category());
            case "min" -> new MarketProduct(current.id(), current.displayName(), current.icon(), current.currencyId(),
                    current.basePrice(), value, current.maxPrice(), current.supplyWeight(), current.demandWeight(),
                    current.volatility(), current.recoveryRate(), current.category());
            case "max" -> new MarketProduct(current.id(), current.displayName(), current.icon(), current.currencyId(),
                    current.basePrice(), current.minPrice(), value, current.supplyWeight(), current.demandWeight(),
                    current.volatility(), current.recoveryRate(), current.category());
            case "supply" -> new MarketProduct(current.id(), current.displayName(), current.icon(), current.currencyId(),
                    current.basePrice(), current.minPrice(), current.maxPrice(), value, current.demandWeight(),
                    current.volatility(), current.recoveryRate(), current.category());
            case "demand" -> new MarketProduct(current.id(), current.displayName(), current.icon(), current.currencyId(),
                    current.basePrice(), current.minPrice(), current.maxPrice(), current.supplyWeight(), value,
                    current.volatility(), current.recoveryRate(), current.category());
            case "volatility" -> new MarketProduct(current.id(), current.displayName(), current.icon(),
                    current.currencyId(), current.basePrice(), current.minPrice(), current.maxPrice(),
                    current.supplyWeight(), current.demandWeight(), value, current.recoveryRate(), current.category());
            case "recovery" -> new MarketProduct(current.id(), current.displayName(), current.icon(),
                    current.currencyId(), current.basePrice(), current.minPrice(), current.maxPrice(),
                    current.supplyWeight(), current.demandWeight(), current.volatility(), value, current.category());
            default -> current;
        };
    }

}
