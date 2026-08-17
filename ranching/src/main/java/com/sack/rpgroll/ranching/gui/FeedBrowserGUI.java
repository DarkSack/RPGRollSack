package com.sack.rpgroll.ranching.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.ranching.core.nutrition.Feed;
import com.sack.rpgroll.ranching.core.nutrition.FeedManager;
import com.sack.rpgroll.ranching.core.nutrition.FeedQuality;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FeedBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final FeedManager feedManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private List<Feed> feeds;

    public FeedBrowserGUI(Player player, FeedManager feedManager, ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text(chatPromptManager.lang().raw("gui.hub.feeds"), NamedTextColor.GOLD), SIZE);
        this.feedManager = feedManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
        this.feeds = List.copyOf(feedManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < feeds.size() && i < 36; i++) {

            Feed feed = feeds.get(i);

            setItem(i, new ItemBuilder(SpeciesBrowserGUI.parseMaterial(feed.icon(), Material.WHEAT))
                    .setName(Component.text(feed.displayName(), NamedTextColor.YELLOW))
                    .setLore(Component.text(chatPromptManager.lang().raw("gui.browser.id_line", "id", feed.id()), NamedTextColor.GRAY),
                            Component.text(chatPromptManager.lang().raw("gui.feed.browser.quality_line", "quality", feed.quality()), NamedTextColor.GRAY),
                            Component.text(chatPromptManager.lang().raw("gui.common.click_to_edit"), NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text(chatPromptManager.lang().raw("gui.feed.browser.new"), NamedTextColor.GREEN)).build());
        setItem(BACK_SLOT, ItemBuilder.createCancelButton(chatPromptManager.lang().raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < feeds.size() && slot < 36) {
            new FeedEditorGUI(player, feeds.get(slot), feedManager, chatPromptManager, this::reopen).open();
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
        chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.feed.browser.prompt_new_id"), value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (feedManager.exists(id)) {
                player.sendMessage(Component.text(chatPromptManager.lang().raw("gui.feed.browser.already_exists"), NamedTextColor.RED));
                reopen();
                return;
            }

            feedManager.save(new Feed(id, id, "WHEAT", "", FeedQuality.COMMON, Set.of(), 20, 0, 0, 0));
            reopen();
        });
    }

    private void reopen() {
        this.feeds = List.copyOf(feedManager.getAll());
        open();
    }

}
