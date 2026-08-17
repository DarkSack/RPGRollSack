package com.sack.rpgroll.gui.admin;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.api.race.Race;
import com.sack.rpgroll.api.race.RacePhysicalModifiers;
import com.sack.rpgroll.api.stats.StatType;
import com.sack.rpgroll.race.RaceManagerImpl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.Material;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Editor de una raza — identidad, atributos base y modificadores físicos.
 * "icon" (textura base64 de cabeza) y "passive-traits" se editan como
 * texto libre vía chat, no hay selector visual de skins acá.
 */
public class RaceEditorGUI extends InventoryGUI {

    private static final int SIZE = 27;
    private static final int NAME_SLOT = 9;
    private static final int DESCRIPTION_SLOT = 10;
    private static final int ATTRIBUTES_SLOT = 11;
    private static final int PASSIVE_TRAITS_SLOT = 12;
    private static final int LORE_SLOT = 13;
    private static final int SCALE_SLOT = 14;
    private static final int SPEED_SLOT = 15;
    private static final int BACK_SLOT = 26;

    private final RaceManagerImpl raceManager;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;
    private final Runnable onBack;
    private Race current;

    public RaceEditorGUI(Player player, Race race, RaceManagerImpl raceManager, ChatPromptManager chatPromptManager,
            LangManager lang, Runnable onBack) {
        super(player, lang.component("race_editor_gui.title", "id", race.id()), SIZE);
        this.current = race;
        this.raceManager = raceManager;
        this.chatPromptManager = chatPromptManager;
        this.lang = lang;
        this.onBack = onBack;
    }

    private void replace(Race updated) {
        current = updated;
        raceManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(ComponentUtils.parse(lang.raw("race_editor_gui.name_slot_name", "name",
                        current.displayName())).colorIfAbsent(NamedTextColor.YELLOW))
                .setLore(lang.component("race_editor_gui.click_new_value"))
                .build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(lang.component("race_editor_gui.description_slot_name"))
                .setLore(ItemBuilder.toLoreLines(current.description().isBlank()
                        ? lang.raw("race_editor_gui.no_description")
                        : current.description()))
                .build());

        setItem(ATTRIBUTES_SLOT, new ItemBuilder(Material.BEACON)
                .setName(lang.component("race_editor_gui.attributes_slot_name", "attributes",
                        current.baseAttributes()))
                .setLore(lang.component("race_editor_gui.attributes_slot_lore"))
                .build());

        setItem(PASSIVE_TRAITS_SLOT, new ItemBuilder(Material.BOOK)
                .setName(lang.component("race_editor_gui.passive_traits_slot_name", "traits",
                        String.join(", ", current.passiveTraits())))
                .setLore(lang.component("race_editor_gui.passive_traits_slot_lore"))
                .build());

        setItem(LORE_SLOT, new ItemBuilder(Material.PAPER)
                .setName(lang.component("race_editor_gui.lore_slot_name", "count", current.lore().size()))
                .setLore(lang.component("race_editor_gui.lore_slot_lore"))
                .build());

        setItem(SCALE_SLOT, new ItemBuilder(Material.ARMOR_STAND)
                .setName(lang.component("race_editor_gui.scale_slot_name", "scale",
                        current.physicalModifiers().scale()))
                .setLore(lang.component("race_editor_gui.scale_slot_lore"))
                .build());

        setItem(SPEED_SLOT, new ItemBuilder(Material.FEATHER)
                .setName(lang.component("race_editor_gui.speed_slot_name", "percent",
                        current.physicalModifiers().movementSpeedPercent() * 100))
                .setLore(lang.component("race_editor_gui.speed_slot_lore"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("race_editor_gui.back_button")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        ClickType click = event.getClick();

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, lang.raw("race_editor_gui.prompt_name"), value -> replace(new Race(current.id(),
                    value, current.description(), current.baseAttributes(), current.passiveTraits(), current.icon(),
                    current.lore(), current.physicalModifiers())));
            return;
        }

        if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, lang.raw("race_editor_gui.prompt_description"), value -> replace(new Race(current.id(),
                    current.displayName(), value, current.baseAttributes(), current.passiveTraits(), current.icon(),
                    current.lore(), current.physicalModifiers())));
            return;
        }

        if (slot == ATTRIBUTES_SLOT) {
            chatPromptManager.prompt(player, lang.raw("race_editor_gui.prompt_attributes"),
                    value -> {
                        Map<StatType, Integer> attributes = parseAttributes(value);
                        replace(new Race(current.id(), current.displayName(), current.description(), attributes,
                                current.passiveTraits(), current.icon(), current.lore(),
                                current.physicalModifiers()));
                    });
            return;
        }

        if (slot == PASSIVE_TRAITS_SLOT) {
            chatPromptManager.prompt(player, lang.raw("race_editor_gui.prompt_passive_traits"), value -> {
                List<String> traits = value.isBlank() ? List.of() : List.of(value.split("\\s*,\\s*"));
                replace(new Race(current.id(), current.displayName(), current.description(),
                        current.baseAttributes(), traits, current.icon(), current.lore(),
                        current.physicalModifiers()));
            });
            return;
        }

        if (slot == LORE_SLOT) {
            chatPromptManager.prompt(player, lang.raw("race_editor_gui.prompt_lore"), value -> {
                List<String> lore = value.isBlank() ? List.of() : List.of(value.split("\\s*;\\s*"));
                replace(new Race(current.id(), current.displayName(), current.description(),
                        current.baseAttributes(), current.passiveTraits(), current.icon(), lore,
                        current.physicalModifiers()));
            });
            return;
        }

        if (slot == SCALE_SLOT) {
            double delta = click == ClickType.RIGHT ? -0.1 : 0.1;
            var physical = current.physicalModifiers();
            RacePhysicalModifiers updated = new RacePhysicalModifiers(Math.max(0.1, physical.scale() + delta),
                    physical.movementSpeedPercent(), physical.extraHealth(), physical.knockbackResistance());
            replace(new Race(current.id(), current.displayName(), current.description(), current.baseAttributes(),
                    current.passiveTraits(), current.icon(), current.lore(), updated));
            return;
        }

        if (slot == SPEED_SLOT) {
            double delta = click == ClickType.RIGHT ? -0.05 : 0.05;
            var physical = current.physicalModifiers();
            RacePhysicalModifiers updated = new RacePhysicalModifiers(physical.scale(),
                    physical.movementSpeedPercent() + delta, physical.extraHealth(), physical.knockbackResistance());
            replace(new Race(current.id(), current.displayName(), current.description(), current.baseAttributes(),
                    current.passiveTraits(), current.icon(), current.lore(), updated));
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private Map<StatType, Integer> parseAttributes(String raw) {

        Map<StatType, Integer> attributes = new HashMap<>();

        if (raw.isBlank()) {
            return attributes;
        }

        for (String part : raw.split("\\s*,\\s*")) {

            String[] kv = part.split("=", 2);
            if (kv.length != 2) {
                continue;
            }

            StatType stat = StatType.fromString(kv[0].trim());
            if (stat == null) {
                continue;
            }

            try {
                attributes.put(stat, Integer.parseInt(kv[1].trim()));
            } catch (NumberFormatException ignored) {
            }
        }

        return attributes;
    }

}
