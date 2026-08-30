package com.sack.rpgroll.crafting.gui;

import com.sack.rpgroll.crafting.recipe.RecipeResult;
import com.sack.rpgroll.crafting.recipe.RecipeResultType;
import com.sack.rpgroll.crafting.villager.VillagerTradeDefinition;
import com.sack.rpgroll.crafting.villager.VillagerTradeManager;
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

public class VillagerTradeBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final VillagerTradeManager tradeManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private List<VillagerTradeDefinition> trades;

    public VillagerTradeBrowserGUI(Player player, VillagerTradeManager tradeManager, ChatPromptManager chatPromptManager,
            Runnable onBack) {
        super(player, Component.text(chatPromptManager.lang().raw("gui.villager_trade.browser_title"), NamedTextColor.GOLD), SIZE);
        this.tradeManager = tradeManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
        this.trades = List.copyOf(tradeManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < trades.size() && i < 36; i++) {

            VillagerTradeDefinition trade = trades.get(i);

            setItem(i, new ItemBuilder(CustomStationBrowserGUI.parseMaterial(trade.icon()))
                    .setName(ComponentUtils.parse(trade.displayName()))
                    .setLore(Component.text(chatPromptManager.lang().raw("gui.common.id_lore", "id", trade.id()), NamedTextColor.GRAY),
                            Component.text(chatPromptManager.lang().raw("gui.villager_trade.cost_lore", "value", trade.costs().get(0).value()), NamedTextColor.GRAY),
                            Component.text(chatPromptManager.lang().raw("gui.villager_trade.result_lore", "value", trade.result().value()), NamedTextColor.AQUA),
                            Component.text(chatPromptManager.lang().raw("gui.common.click_edit"), NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text(chatPromptManager.lang().raw("gui.villager_trade.create_new"), NamedTextColor.GREEN)).build());
        setItem(BACK_SLOT, ItemBuilder.createCancelButton(chatPromptManager.lang().raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < trades.size() && slot < 36) {
            new VillagerTradeEditorGUI(player, trades.get(slot), tradeManager, chatPromptManager, this::reopen).open();
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
        chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.villager_trade.prompt_new_id"), value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (tradeManager.exists(id)) {
                player.sendMessage(Component.text(chatPromptManager.lang().raw("gui.villager_trade.already_exists"), NamedTextColor.RED));
                reopen();
                return;
            }

            tradeManager.save(new VillagerTradeDefinition(id, id, "EMERALD",
                    List.of(new RecipeResult(RecipeResultType.MATERIAL, "EMERALD", 1)),
                    new RecipeResult(RecipeResultType.MATERIAL, "PAPER", 1), 12, 1, true, List.of(), 0, null, 0, false));
            reopen();
        });
    }

    private void reopen() {
        this.trades = List.copyOf(tradeManager.getAll());
        open();
    }

}
