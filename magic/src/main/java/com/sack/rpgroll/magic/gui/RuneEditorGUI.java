package com.sack.rpgroll.magic.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.magic.core.Rune;
import com.sack.rpgroll.magic.core.RuneManager;
import com.sack.rpgroll.magic.core.RuneModifierType;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class RuneEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int NAME_SLOT = 10;
    private static final int ICON_SLOT = 11;
    private static final int DESCRIPTION_SLOT = 12;
    private static final int TYPE_SLOT = 13;
    private static final int PARAMS_SLOT = 14;
    private static final int BACK_SLOT = 40;

    private final RuneManager runeManager;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;
    private final Runnable onBack;
    private Rune current;

    public RuneEditorGUI(Player player, Rune rune, RuneManager runeManager, ChatPromptManager chatPromptManager,
            Runnable onBack) {
        super(player, chatPromptManager.lang().component("gui.rune_editor.title", "id", rune.id()), SIZE);
        this.current = rune;
        this.runeManager = runeManager;
        this.chatPromptManager = chatPromptManager;
        this.lang = chatPromptManager.lang();
        this.onBack = onBack;
    }

    private void replace(Rune updated) {
        current = updated;
        runeManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(lang.component("gui.common.name_label", "name", current.displayName())).build());

        setItem(ICON_SLOT, new ItemBuilder(SchoolBrowserGUI.parseMaterial(current.icon()))
                .setName(lang.component("gui.common.icon_label", "icon", current.icon())).build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(lang.component("gui.common.description_title"))
                .setLore(ItemBuilder.toLoreLines(
                        current.description().isBlank() ? lang.raw("gui.common.no_description")
                                : current.description()))
                .build());

        setItem(TYPE_SLOT, new ItemBuilder(Material.NETHER_STAR)
                .setName(lang.component("gui.rune_editor.type_label", "type", current.type()))
                .setLore(lang.component("gui.common.click_cycle_masc")).build());

        setItem(PARAMS_SLOT, new ItemBuilder(Material.COMMAND_BLOCK)
                .setName(lang.component("gui.rune_editor.params_label", "params", current.params()))
                .setLore(lang.component("gui.rune_editor.params_lore"),
                        paramsHelpFor(current.type()))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back")));
    }

    private Component paramsHelpFor(RuneModifierType type) {
        String key = switch (type) {
            case EXTRA_PROJECTILES -> "gui.rune_editor.params_help_extra_projectiles";
            case PIERCING -> "gui.rune_editor.params_help_piercing";
            case EXPLOSIVE -> "gui.rune_editor.params_help_explosive";
            case APPLY_EFFECT -> "gui.rune_editor.params_help_apply_effect";
            case COST_MODIFIER, COOLDOWN_MODIFIER -> "gui.rune_editor.params_help_modifier";
        };
        return lang.component(key);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.common.prompt_name"), value -> replace(new Rune(current.id(),
                    value, current.icon(), current.description(), current.type(), current.params())));
            return;
        }

        if (slot == ICON_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.common.prompt_icon"), value -> replace(new Rune(
                    current.id(), current.displayName(), value, current.description(), current.type(),
                    current.params())));
            return;
        }

        if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.common.prompt_description"), value -> replace(new Rune(current.id(),
                    current.displayName(), current.icon(), value, current.type(), current.params())));
            return;
        }

        if (slot == TYPE_SLOT) {
            RuneModifierType[] values = RuneModifierType.values();
            RuneModifierType next = values[(current.type().ordinal() + 1) % values.length];
            replace(new Rune(current.id(), current.displayName(), current.icon(), current.description(), next,
                    current.params()));
            return;
        }

        if (slot == PARAMS_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.rune_editor.prompt_params"), value -> {

                Map<String, String> params = new LinkedHashMap<>();

                for (String pair : value.split(",")) {
                    String[] kv = pair.split("=", 2);
                    if (kv.length == 2) {
                        params.put(kv[0].trim().toLowerCase(Locale.ROOT), kv[1].trim());
                    }
                }

                replace(new Rune(current.id(), current.displayName(), current.icon(), current.description(),
                        current.type(), params));
            });
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
