package com.sack.rpgroll.dungeons.gui.editor;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

/** Editor de metadata general: nombre, categoría, ícono, descripción, nivel, jugadores, cooldown, tags. */
public class InfoEditorGUI extends InventoryGUI {

    private static final int SIZE = 36;

    private static final int NAME_SLOT = 9;
    private static final int CATEGORY_SLOT = 10;
    private static final int ICON_SLOT = 11;
    private static final int DESCRIPTION_SLOT = 12;
    private static final int LEVEL_SLOT = 13;
    private static final int MIN_PLAYERS_SLOT = 14;
    private static final int MAX_PLAYERS_SLOT = 15;
    private static final int ESTIMATED_MINUTES_SLOT = 16;
    private static final int COOLDOWN_SLOT = 17;
    private static final int REPEATABLE_SLOT = 18;

    private static final int TAGS_START_SLOT = 20;
    private static final int ADD_TAG_SLOT = 26;
    private static final int BACK_SLOT = 31;

    private final DungeonEditorSession session;
    private final Runnable onBack;
    private final LangManager lang;

    public InfoEditorGUI(Player player, DungeonEditorSession session, Runnable onBack) {
        super(player, ComponentUtils.parse(session.chatPromptManager.lang()
                .raw("gui.editor.info.title", "id", session.original.id())), SIZE);
        this.session = session;
        this.onBack = onBack;
        this.lang = session.chatPromptManager.lang();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(ComponentUtils.parse(lang.raw("gui.editor.info.name", "value", session.displayName)))
                .setLore(ComponentUtils.parse(lang.raw("gui.editor.info.click_to_write")))
                .build());

        setItem(CATEGORY_SLOT, new ItemBuilder(Material.CHEST)
                .setName(ComponentUtils.parse(lang.raw("gui.editor.info.category", "value", session.category)))
                .setLore(ComponentUtils.parse(lang.raw("gui.editor.info.click_to_write_new")))
                .build());

        setItem(ICON_SLOT, new ItemBuilder(Material.ITEM_FRAME)
                .setName(ComponentUtils.parse(lang.raw("gui.editor.info.icon.label", "value", session.icon)))
                .setLore(ComponentUtils.parse(lang.raw("gui.editor.info.icon.hint")))
                .build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.BOOK)
                .setName(ComponentUtils.parse(lang.raw("gui.editor.info.description.label")))
                .setLore(ComponentUtils.parseWithDefault(session.description.isBlank()
                        ? lang.raw("gui.editor.info.description.empty") : session.description, NamedTextColor.GRAY))
                .build());

        setItem(LEVEL_SLOT, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(ComponentUtils.parse(lang.raw("gui.editor.info.level.label",
                        "value", session.recommendedLevel)))
                .setLore(ComponentUtils.parse(lang.raw("gui.editor.info.level.hint")))
                .build());

        setItem(MIN_PLAYERS_SLOT, new ItemBuilder(Material.IRON_INGOT)
                .setName(ComponentUtils.parse(lang.raw("gui.editor.info.min_players", "value", session.minPlayers)))
                .setLore(ComponentUtils.parse(lang.raw("gui.editor.info.click_updown_hint")))
                .build());

        setItem(MAX_PLAYERS_SLOT, new ItemBuilder(Material.GOLD_INGOT)
                .setName(ComponentUtils.parse(lang.raw("gui.editor.info.max_players", "value", session.maxPlayers)))
                .setLore(ComponentUtils.parse(lang.raw("gui.editor.info.click_updown_hint")))
                .build());

        setItem(ESTIMATED_MINUTES_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(ComponentUtils.parse(lang.raw("gui.editor.info.estimated_minutes.label",
                        "value", session.estimatedMinutes)))
                .setLore(ComponentUtils.parse(lang.raw("gui.editor.info.estimated_minutes.hint")))
                .build());

        setItem(COOLDOWN_SLOT, new ItemBuilder(Material.HOPPER)
                .setName(ComponentUtils.parse(lang.raw("gui.editor.info.cooldown.label",
                        "value", session.cooldownMillis / 60000)))
                .setLore(ComponentUtils.parse(lang.raw("gui.editor.info.cooldown.hint")))
                .build());

        setItem(REPEATABLE_SLOT, new ItemBuilder(session.repeatable ? Material.LIME_DYE : Material.GRAY_DYE)
                .setName(ComponentUtils.parse(lang.raw("gui.editor.info.repeatable", "value", session.repeatable)))
                .setLore(ComponentUtils.parse(lang.raw("gui.editor.info.click_to_toggle")))
                .build());

        for (int i = 0; i < session.tags.size() && i < 6; i++) {
            setItem(TAGS_START_SLOT + i, new ItemBuilder(Material.PAPER)
                    .setName(Component.text(session.tags.get(i), NamedTextColor.AQUA))
                    .setLore(ComponentUtils.parse(lang.raw("gui.editor.info.tag.remove_hint")))
                    .build());
        }

        setItem(ADD_TAG_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(ComponentUtils.parse(lang.raw("gui.editor.info.tag.add")))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back")));
    }

    private double delta(ClickType click, double small, double large) {
        return switch (click) {
            case LEFT -> small;
            case SHIFT_LEFT -> large;
            case RIGHT -> -small;
            case SHIFT_RIGHT -> -large;
            default -> 0;
        };
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == NAME_SLOT) {
            session.chatPromptManager.prompt(player, "gui.editor.info.prompt.name", value -> {
                session.displayName = value;
                build();
            });
            return;
        }

        if (slot == CATEGORY_SLOT) {
            session.chatPromptManager.prompt(player, "gui.editor.info.prompt.category", value -> {
                session.category = value.trim().toLowerCase();
                build();
            });
            return;
        }

        if (slot == ICON_SLOT) {
            session.chatPromptManager.prompt(player, "gui.editor.info.prompt.icon", value -> {
                session.icon = value.trim().toUpperCase();
                build();
            });
            return;
        }

        if (slot == DESCRIPTION_SLOT) {
            session.chatPromptManager.prompt(player, "gui.editor.info.prompt.description", value -> {
                session.description = value;
                build();
            });
            return;
        }

        if (slot == LEVEL_SLOT) {
            session.recommendedLevel = Math.max(1, session.recommendedLevel + (int) delta(event.getClick(), 1, 10));
            build();
            return;
        }

        if (slot == MIN_PLAYERS_SLOT) {
            session.minPlayers = Math.max(1, session.minPlayers + (int) delta(event.getClick(), 1, 1));
            session.maxPlayers = Math.max(session.maxPlayers, session.minPlayers);
            build();
            return;
        }

        if (slot == MAX_PLAYERS_SLOT) {
            session.maxPlayers = Math.max(session.minPlayers, session.maxPlayers + (int) delta(event.getClick(), 1, 1));
            build();
            return;
        }

        if (slot == ESTIMATED_MINUTES_SLOT) {
            session.estimatedMinutes = Math.max(1, session.estimatedMinutes + (int) delta(event.getClick(), 5, 5));
            build();
            return;
        }

        if (slot == COOLDOWN_SLOT) {
            long deltaMillis = (long) delta(event.getClick(), 30, 360) * 60_000L;
            session.cooldownMillis = Math.max(0, session.cooldownMillis + deltaMillis);
            build();
            return;
        }

        if (slot == REPEATABLE_SLOT) {
            session.repeatable = !session.repeatable;
            build();
            return;
        }

        if (slot >= TAGS_START_SLOT && slot < TAGS_START_SLOT + Math.min(session.tags.size(), 6)) {
            if (event.isShiftClick()) {
                session.tags.remove(slot - TAGS_START_SLOT);
                build();
            }
            return;
        }

        if (slot == ADD_TAG_SLOT) {
            session.chatPromptManager.prompt(player, "gui.editor.info.prompt.tag", value -> {
                List<String> updated = new ArrayList<>(session.tags);
                updated.add(value.trim());
                session.tags = updated;
                build();
            });
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
