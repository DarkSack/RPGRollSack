package com.sack.rpgroll.mobs.gui.editor;

import com.sack.rpgroll.common.lang.LangManager;

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
import java.util.Locale;

/**
 * Editor de stats del mob (health, damage, defense, speed, armor, crítico,
 * esquive, precisión...). Click izquierdo +1, shift+izquierdo +10, click
 * derecho -1, shift+derecho -10.
 */
public class MobStatsEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int ADD_CUSTOM_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final MobEditorSession session;
    private final Runnable onBack;
    private final List<String> statIds;
    private final LangManager lang;

    public MobStatsEditorGUI(Player player, MobEditorSession session, Runnable onBack) {
        super(player, session.chatPromptManager.lang().component("gui.stats.title", "id", session.original.id()),
                SIZE);
        this.session = session;
        this.onBack = onBack;
        this.statIds = new ArrayList<>(session.statRegistry.all());
        this.statIds.sort(String::compareTo);
        this.lang = session.chatPromptManager.lang();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < statIds.size() && i < 36; i++) {
            setItem(i, statItem(statIds.get(i), session.stats.getOrDefault(statIds.get(i), 0.0)));
        }

        setItem(ADD_CUSTOM_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("gui.stats.add_custom"))
                .setLore(lang.component("gui.stats.add_custom_hint"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back_button")));
    }

    private org.bukkit.inventory.ItemStack statItem(String statId, double value) {
        return new ItemBuilder(value > 0 ? Material.REDSTONE : Material.GUNPOWDER)
                .setName(Component.text(formatName(statId) + ": " + formatNumber(value), NamedTextColor.RED))
                .setLore(lang.component("gui.common.click_plus1_10"),
                        lang.component("gui.common.click_minus1_10"))
                .build();
    }

    private String formatName(String key) {

        String[] parts = key.toLowerCase(Locale.ROOT).split("_");
        StringBuilder result = new StringBuilder();

        for (String part : parts) {
            if (!result.isEmpty()) {
                result.append(" ");
            }
            if (!part.isEmpty()) {
                result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
            }
        }

        return result.toString();
    }

    private String formatNumber(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

    private double delta(ClickType click) {
        return switch (click) {
            case LEFT -> 1;
            case SHIFT_LEFT -> 10;
            case RIGHT -> -1;
            case SHIFT_RIGHT -> -10;
            default -> 0;
        };
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < statIds.size() && slot < 36) {
            adjustStat(statIds.get(slot), event.getClick());
            return;
        }

        if (slot == ADD_CUSTOM_SLOT) {
            promptCustomStat();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void adjustStat(String statId, ClickType click) {

        double delta = delta(click);
        if (delta == 0) {
            return;
        }

        double updated = session.stats.getOrDefault(statId, 0.0) + delta;

        if (updated == 0) {
            session.stats.remove(statId);
        } else {
            session.stats.put(statId, updated);
        }

        build();
    }

    private void promptCustomStat() {
        session.chatPromptManager.prompt(player, "gui.stats.prompt_custom", value -> {

            String[] parts = value.trim().split("\\s+");

            if (parts.length != 2) {
                lang.send(player, "gui.common.invalid_format");
                return;
            }

            try {
                String statId = parts[0].toLowerCase(Locale.ROOT);
                session.stats.put(statId, Double.parseDouble(parts[1]));
                session.statRegistry.register(statId);
            } catch (NumberFormatException e) {
                lang.send(player, "gui.common.invalid_number");
                return;
            }

            reopen();
        });
    }

    private void reopen() {
        this.statIds.clear();
        this.statIds.addAll(session.statRegistry.all());
        this.statIds.sort(String::compareTo);
        open();
    }

}
