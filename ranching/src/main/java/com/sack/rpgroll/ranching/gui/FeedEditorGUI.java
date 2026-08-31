package com.sack.rpgroll.ranching.gui;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.ranching.core.nutrition.Feed;
import com.sack.rpgroll.ranching.core.nutrition.FeedManager;
import com.sack.rpgroll.ranching.core.nutrition.FeedQuality;
import com.sack.rpgroll.ranching.item.RanchingItemFactory;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class FeedEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int NAME_SLOT = 10;
    private static final int ICON_SLOT = 11;
    private static final int DESCRIPTION_SLOT = 12;
    private static final int QUALITY_SLOT = 13;
    private static final int TAGS_SLOT = 14;
    private static final int NUTRITION_SLOT = 15;
    private static final int HEALTH_SLOT = 16;
    private static final int HAPPINESS_SLOT = 19;
    private static final int PRODUCTION_SLOT = 20;
    private static final int GIVE_SLOT = 22;
    private static final int BACK_SLOT = 40;

    private final FeedManager feedManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private Feed current;

    public FeedEditorGUI(Player player, Feed feed, FeedManager feedManager, ChatPromptManager chatPromptManager,
            Runnable onBack) {
        super(player, ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.feed.editor.title", "id", feed.id()), NamedTextColor.GOLD), SIZE);
        this.current = feed;
        this.feedManager = feedManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(Feed updated) {
        current = updated;
        feedManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        var lang = chatPromptManager.lang();

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(lang.component("gui.editor.name_line", "name", current.displayName())).build());

        setItem(ICON_SLOT, new ItemBuilder(SpeciesBrowserGUI.parseMaterial(current.icon(), Material.WHEAT))
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.editor.icon_line", "icon", current.icon()), NamedTextColor.YELLOW)).build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.editor.description_title"), NamedTextColor.YELLOW))
                .setLore(ItemBuilder.toLoreLines(current.description().isBlank() ? lang.raw("gui.editor.no_description") : current.description()))
                .build());

        setItem(QUALITY_SLOT, new ItemBuilder(Material.AMETHYST_SHARD)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.feed.editor.quality_line", "quality", current.quality()), NamedTextColor.LIGHT_PURPLE))
                .setLore(ComponentUtils.parseWithDefault(lang.raw("gui.editor.rotate_hint"), NamedTextColor.GRAY)).build());

        setItem(TAGS_SLOT, new ItemBuilder(Material.HOPPER)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.feed.editor.tags_line", "tags", String.join(", ", current.tags())), NamedTextColor.GOLD))
                .setLore(ComponentUtils.parseWithDefault(lang.raw("gui.editor.prompt_tags_hint"), NamedTextColor.GRAY)).build());

        setItem(NUTRITION_SLOT, new ItemBuilder(Material.APPLE)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.feed.editor.nutrition_line", "value", current.nutritionValue()), NamedTextColor.AQUA))
                .setLore(ComponentUtils.parseWithDefault(lang.raw("gui.editor.step5_hint"), NamedTextColor.GRAY)).build());

        setItem(HEALTH_SLOT, new ItemBuilder(Material.GOLDEN_APPLE)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.editor.health_bonus_line", "value", current.healthBonus()), NamedTextColor.RED))
                .setLore(ComponentUtils.parseWithDefault(lang.raw("gui.editor.step2_hint"), NamedTextColor.GRAY)).build());

        setItem(HAPPINESS_SLOT, new ItemBuilder(Material.CAKE)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.editor.happiness_bonus_line", "value", current.happinessBonus()), NamedTextColor.LIGHT_PURPLE))
                .setLore(ComponentUtils.parseWithDefault(lang.raw("gui.editor.step2_hint"), NamedTextColor.GRAY)).build());

        setItem(PRODUCTION_SLOT, new ItemBuilder(Material.BUCKET)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.feed.editor.production_bonus_line", "value", current.productionBonus()), NamedTextColor.AQUA))
                .setLore(ComponentUtils.parseWithDefault(lang.raw("gui.editor.step2_hint"), NamedTextColor.GRAY)).build());

        setItem(GIVE_SLOT, new ItemBuilder(Material.CHEST)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.editor.give_one"), NamedTextColor.GREEN)).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        double sign = event.getClick() == ClickType.RIGHT ? -2 : 2;

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.editor.prompt_name"), value -> replace(new Feed(current.id(), value,
                    current.icon(), current.description(), current.quality(), current.tags(), current.nutritionValue(),
                    current.healthBonus(), current.happinessBonus(), current.productionBonus())));
        } else if (slot == ICON_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.editor.prompt_icon"), value -> replace(new Feed(current.id(),
                    current.displayName(), value, current.description(), current.quality(), current.tags(),
                    current.nutritionValue(), current.healthBonus(), current.happinessBonus(), current.productionBonus())));
        } else if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.editor.prompt_description"), value -> replace(new Feed(current.id(),
                    current.displayName(), current.icon(), value, current.quality(), current.tags(),
                    current.nutritionValue(), current.healthBonus(), current.happinessBonus(), current.productionBonus())));
        } else if (slot == QUALITY_SLOT) {
            FeedQuality[] values = FeedQuality.values();
            FeedQuality next = values[(current.quality().ordinal() + 1) % values.length];
            replace(new Feed(current.id(), current.displayName(), current.icon(), current.description(), next,
                    current.tags(), current.nutritionValue(), current.healthBonus(), current.happinessBonus(),
                    current.productionBonus()));
        } else if (slot == TAGS_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.editor.prompt_tags"), value -> {

                Set<String> tags = new HashSet<>();
                for (String entry : value.split(",")) {
                    if (!entry.isBlank()) {
                        tags.add(entry.trim().toLowerCase(Locale.ROOT));
                    }
                }

                replace(new Feed(current.id(), current.displayName(), current.icon(), current.description(),
                        current.quality(), tags, current.nutritionValue(), current.healthBonus(),
                        current.happinessBonus(), current.productionBonus()));
            });
        } else if (slot == NUTRITION_SLOT) {
            replace(new Feed(current.id(), current.displayName(), current.icon(), current.description(),
                    current.quality(), current.tags(), Math.max(0, current.nutritionValue() + sign),
                    current.healthBonus(), current.happinessBonus(), current.productionBonus()));
        } else if (slot == HEALTH_SLOT) {
            replace(new Feed(current.id(), current.displayName(), current.icon(), current.description(),
                    current.quality(), current.tags(), current.nutritionValue(), Math.max(0, current.healthBonus() + sign),
                    current.happinessBonus(), current.productionBonus()));
        } else if (slot == HAPPINESS_SLOT) {
            replace(new Feed(current.id(), current.displayName(), current.icon(), current.description(),
                    current.quality(), current.tags(), current.nutritionValue(), current.healthBonus(),
                    Math.max(0, current.happinessBonus() + sign), current.productionBonus()));
        } else if (slot == PRODUCTION_SLOT) {
            replace(new Feed(current.id(), current.displayName(), current.icon(), current.description(),
                    current.quality(), current.tags(), current.nutritionValue(), current.healthBonus(),
                    current.happinessBonus(), Math.max(0, current.productionBonus() + sign)));
        } else if (slot == GIVE_SLOT) {
            player.getInventory().addItem(RanchingItemFactory.createFeed(chatPromptManager.lang(), current));
            player.sendMessage(chatPromptManager.lang().component("gui.feed.editor.gave_self", "name", current.displayName()));
        } else if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
