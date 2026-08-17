package com.sack.rpgroll.magic.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.magic.core.MagicSchool;
import com.sack.rpgroll.magic.core.SchoolManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SchoolEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int NAME_SLOT = 10;
    private static final int COLOR_SLOT = 11;
    private static final int ICON_SLOT = 12;
    private static final int DESCRIPTION_SLOT = 13;
    private static final int CAST_SOUND_SLOT = 19;
    private static final int CAST_EFFECT_SLOT = 20;
    private static final int RACE_AFFINITIES_SLOT = 22;
    private static final int CLASS_AFFINITIES_SLOT = 23;
    private static final int BACK_SLOT = 40;

    private final SchoolManager schoolManager;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;
    private final Runnable onBack;
    private MagicSchool current;

    public SchoolEditorGUI(Player player, MagicSchool school, SchoolManager schoolManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, chatPromptManager.lang().component("gui.school_editor.title", "id", school.id()), SIZE);
        this.current = school;
        this.schoolManager = schoolManager;
        this.chatPromptManager = chatPromptManager;
        this.lang = chatPromptManager.lang();
        this.onBack = onBack;
    }

    private void replace(MagicSchool updated) {
        current = updated;
        schoolManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(lang.component("gui.common.name_label", "name", current.displayName()))
                .build());

        setItem(COLOR_SLOT, new ItemBuilder(Material.PAPER)
                .setName(Component.text(lang.raw("gui.school_editor.color_label", "color", current.color()),
                        SchoolBrowserGUI.parseColor(current.color())))
                .setLore(lang.component("gui.school_editor.color_lore"))
                .build());

        setItem(ICON_SLOT, new ItemBuilder(SchoolBrowserGUI.parseMaterial(current.icon()))
                .setName(lang.component("gui.common.icon_label", "icon", current.icon()))
                .build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(lang.component("gui.common.description_title"))
                .setLore(ItemBuilder.toLoreLines(
                        current.description().isBlank() ? lang.raw("gui.common.no_description")
                                : current.description()))
                .build());

        setItem(CAST_SOUND_SLOT, new ItemBuilder(Material.NOTE_BLOCK)
                .setName(lang.component("gui.school_editor.cast_sound_label", "value",
                        current.castSoundOnCast() == null ? lang.raw("gui.common.none") : current.castSoundOnCast()))
                .setLore(lang.component("gui.school_editor.cast_sound_lore"))
                .build());

        setItem(CAST_EFFECT_SLOT, new ItemBuilder(Material.BLAZE_POWDER)
                .setName(lang.component("gui.school_editor.cast_effect_label", "value",
                        current.castEffectId() == null ? lang.raw("gui.common.none") : current.castEffectId()))
                .setLore(lang.component("gui.school_editor.cast_effect_lore"))
                .build());

        setItem(RACE_AFFINITIES_SLOT, new ItemBuilder(Material.ZOMBIE_HEAD)
                .setName(lang.component("gui.school_editor.race_affinities_label", "count",
                        current.raceAffinities().size()))
                .setLore(affinityLore(current.raceAffinities()))
                .build());

        setItem(CLASS_AFFINITIES_SLOT, new ItemBuilder(Material.IRON_SWORD)
                .setName(lang.component("gui.school_editor.class_affinities_label", "count",
                        current.classAffinities().size()))
                .setLore(affinityLore(current.classAffinities()))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back")));
    }

    private List<Component> affinityLore(Map<String, Double> affinities) {

        List<Component> lore = new java.util.ArrayList<>();

        for (var entry : affinities.entrySet()) {
            lore.add(lang.component("gui.school_editor.affinity_line", "key", entry.getKey(),
                    "sign", entry.getValue() >= 0 ? "+" : "", "percent", Math.round(entry.getValue() * 100)));
        }

        lore.add(lang.component("gui.school_editor.affinity_lore"));

        return lore;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.common.prompt_name"), value -> replace(new MagicSchool(
                    current.id(), value, current.color(), current.icon(), current.description(),
                    current.castSoundOnCast(), current.castEffectId(), current.raceAffinities(),
                    current.classAffinities())));
            return;
        }

        if (slot == COLOR_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.school_editor.prompt_color"), value -> replace(
                    new MagicSchool(current.id(), current.displayName(), value, current.icon(),
                            current.description(), current.castSoundOnCast(), current.castEffectId(),
                            current.raceAffinities(), current.classAffinities())));
            return;
        }

        if (slot == ICON_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.common.prompt_icon"), value -> replace(new MagicSchool(
                    current.id(), current.displayName(), current.color(), value, current.description(),
                    current.castSoundOnCast(), current.castEffectId(), current.raceAffinities(),
                    current.classAffinities())));
            return;
        }

        if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.common.prompt_description"), value -> replace(new MagicSchool(
                    current.id(), current.displayName(), current.color(), current.icon(), value,
                    current.castSoundOnCast(), current.castEffectId(), current.raceAffinities(),
                    current.classAffinities())));
            return;
        }

        if (slot == CAST_SOUND_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.school_editor.prompt_cast_sound"), value -> replace(
                    new MagicSchool(current.id(), current.displayName(), current.color(), current.icon(),
                            current.description(), value.equalsIgnoreCase("ninguno") ? null : value,
                            current.castEffectId(), current.raceAffinities(), current.classAffinities())));
            return;
        }

        if (slot == CAST_EFFECT_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.school_editor.prompt_cast_effect"), value -> replace(
                    new MagicSchool(current.id(), current.displayName(), current.color(), current.icon(),
                            current.description(), current.castSoundOnCast(),
                            value.equalsIgnoreCase("ninguno") ? null : value, current.raceAffinities(),
                            current.classAffinities())));
            return;
        }

        if (slot == RACE_AFFINITIES_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.school_editor.prompt_race_affinities"),
                    value -> replace(new MagicSchool(current.id(), current.displayName(), current.color(),
                            current.icon(), current.description(), current.castSoundOnCast(),
                            current.castEffectId(), parseAffinities(value), current.classAffinities())));
            return;
        }

        if (slot == CLASS_AFFINITIES_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.school_editor.prompt_class_affinities"),
                    value -> replace(new MagicSchool(current.id(), current.displayName(), current.color(),
                            current.icon(), current.description(), current.castSoundOnCast(),
                            current.castEffectId(), current.raceAffinities(), parseAffinities(value))));
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private Map<String, Double> parseAffinities(String raw) {

        Map<String, Double> result = new LinkedHashMap<>();

        for (String pair : raw.split(",")) {

            String[] kv = pair.split("=", 2);

            if (kv.length == 2) {
                try {
                    result.put(kv[0].trim().toLowerCase(java.util.Locale.ROOT), Double.parseDouble(kv[1].trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return result;
    }

}
