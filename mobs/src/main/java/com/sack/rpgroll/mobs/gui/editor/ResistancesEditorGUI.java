package com.sack.rpgroll.mobs.gui.editor;

import com.sack.rpgroll.common.lang.LangManager;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.mobs.core.MobWeakness;
import com.sack.rpgroll.mobs.core.MobWeaknessType;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Editor de resistencias (% de reducción por elemento) y debilidades (% de daño extra). */
public class ResistancesEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final String[] ELEMENTS = {
            "fire", "water", "ice", "electric", "poison", "dark", "magic", "explosion", "arrow"
    };

    private static final int ADD_CUSTOM_RESISTANCE_SLOT = 17;

    private static final int WEAKNESS_START_SLOT = 18;
    private static final int ADD_WEAKNESS_SLOT = 35;

    private static final int BACK_SLOT = 44;

    private final MobEditorSession session;
    private final Runnable onBack;
    private final LangManager lang;

    public ResistancesEditorGUI(Player player, MobEditorSession session, Runnable onBack) {
        super(player, session.chatPromptManager.lang().component("gui.resistances.title", "id",
                session.original.id()), SIZE);
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

        for (int i = 0; i < ELEMENTS.length; i++) {

            String element = ELEMENTS[i];
            double value = session.resistances.getOrDefault(element, 0.0);

            setItem(i, new ItemBuilder(value > 0 ? Material.SHIELD : Material.IRON_INGOT)
                    .setName(lang.component("gui.resistances.entry_label", "element", capitalize(element), "value",
                            formatNumber(value)))
                    .setLore(lang.component("gui.common.click_plus5_25"),
                            lang.component("gui.common.click_minus5_25"))
                    .build());
        }

        setItem(ADD_CUSTOM_RESISTANCE_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("gui.resistances.custom"))
                .setLore(lang.component("gui.resistances.custom_hint"))
                .build());

        List<MobWeakness> weaknesses = session.weaknesses;

        for (int i = 0; i < weaknesses.size() && i < 17; i++) {

            MobWeakness weakness = weaknesses.get(i);

            setItem(WEAKNESS_START_SLOT + i, new ItemBuilder(Material.TNT)
                    .setName(lang.component("gui.resistances.weakness_label", "type", weakness.type(), "key",
                            weakness.key(), "value", formatNumber(weakness.extraDamagePercent())))
                    .setLore(lang.component("gui.common.shift_remove_dark"))
                    .build());
        }

        setItem(ADD_WEAKNESS_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("gui.resistances.add_weakness"))
                .setLore(lang.component("gui.resistances.add_weakness_hint1"),
                        lang.component("gui.resistances.add_weakness_hint2"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back_button")));
    }

    private String capitalize(String value) {
        return value.isEmpty() ? value : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private String formatNumber(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

    private double delta(ClickType click) {
        return switch (click) {
            case LEFT -> 5;
            case SHIFT_LEFT -> 25;
            case RIGHT -> -5;
            case SHIFT_RIGHT -> -25;
            default -> 0;
        };
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < ELEMENTS.length) {
            adjustResistance(ELEMENTS[slot], event.getClick());
            return;
        }

        if (slot == ADD_CUSTOM_RESISTANCE_SLOT) {
            promptCustomResistance();
            return;
        }

        if (slot >= WEAKNESS_START_SLOT && slot < WEAKNESS_START_SLOT + Math.min(session.weaknesses.size(), 17)) {
            if (event.isShiftClick()) {
                List<MobWeakness> updated = new ArrayList<>(session.weaknesses);
                updated.remove(slot - WEAKNESS_START_SLOT);
                session.weaknesses = updated;
                build();
            }
            return;
        }

        if (slot == ADD_WEAKNESS_SLOT) {
            promptAddWeakness();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void adjustResistance(String element, ClickType click) {

        double delta = delta(click);
        if (delta == 0) {
            return;
        }

        double updated = Math.max(0, session.resistances.getOrDefault(element, 0.0) + delta);

        if (updated == 0) {
            session.resistances.remove(element);
        } else {
            session.resistances.put(element, updated);
        }

        build();
    }

    private void promptCustomResistance() {
        session.chatPromptManager.prompt(player, "gui.resistances.prompt_custom", value -> {

            String[] parts = value.trim().split("\\s+");

            if (parts.length != 2) {
                lang.send(player, "gui.common.invalid_format");
                return;
            }

            try {
                session.resistances.put(parts[0].toLowerCase(Locale.ROOT), Double.parseDouble(parts[1]));
            } catch (NumberFormatException e) {
                lang.send(player, "gui.common.invalid_number");
                return;
            }

            build();
        });
    }

    private void promptAddWeakness() {
        session.chatPromptManager.prompt(player, "gui.resistances.prompt_weakness", value -> {

            String[] parts = value.trim().split("\\s+");

            if (parts.length != 3) {
                lang.send(player, "gui.common.invalid_format");
                return;
            }

            try {
                MobWeaknessType type = MobWeaknessType.valueOf(parts[0].toUpperCase(Locale.ROOT));
                double percent = Double.parseDouble(parts[2]);

                List<MobWeakness> updated = new ArrayList<>(session.weaknesses);
                updated.add(new MobWeakness(type, parts[1], percent));
                session.weaknesses = updated;
            } catch (IllegalArgumentException e) {
                lang.send(player, "gui.resistances.invalid_type_or_value");
                return;
            }

            build();
        });
    }

}
