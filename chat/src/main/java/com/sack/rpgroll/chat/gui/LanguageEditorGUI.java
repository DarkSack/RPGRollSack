package com.sack.rpgroll.chat.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.chat.language.Language;
import com.sack.rpgroll.chat.language.LanguageManager;
import com.sack.rpgroll.common.lang.LangManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

/** Editor de un idioma — nombre, carácter de ofuscación, y razas que lo conocen por defecto. */
public class LanguageEditorGUI extends InventoryGUI {

    private static final int SIZE = 27;
    private static final int NAME_SLOT = 10;
    private static final int OBFUSCATION_SLOT = 11;
    private static final int RACES_SLOT = 12;
    private static final int BACK_SLOT = 26;

    private final LanguageManager languageManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private Language current;

    public LanguageEditorGUI(Player player, Language language, LanguageManager languageManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, chatPromptManager.lang().component("language.editor_title", "id", language.id()), SIZE);
        this.current = language;
        this.languageManager = languageManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(Language updated) {
        current = updated;
        languageManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        LangManager lang = chatPromptManager.lang();

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(lang.component("language.label_name", "value", current.displayName())
                        .colorIfAbsent(NamedTextColor.YELLOW))
                .setLore(lang.component("gui.click_new").colorIfAbsent(NamedTextColor.GRAY))
                .build());

        setItem(OBFUSCATION_SLOT, new ItemBuilder(Material.BOOK)
                .setName(lang.component("language.label_obfuscation", "value", current.obfuscationChar())
                        .colorIfAbsent(NamedTextColor.YELLOW))
                .setLore(lang.component("language.lore_obfuscation_hint").colorIfAbsent(NamedTextColor.GRAY),
                        lang.component("gui.click_new").colorIfAbsent(NamedTextColor.GRAY))
                .build());

        setItem(RACES_SLOT, new ItemBuilder(Material.PLAYER_HEAD)
                .setName(lang.component("language.label_races", "value",
                                String.join(", ", current.defaultForRaces()))
                        .colorIfAbsent(NamedTextColor.YELLOW))
                .setLore(lang.component("language.lore_races_hint").colorIfAbsent(NamedTextColor.GRAY),
                        lang.component("language.lore_races_prompt").colorIfAbsent(NamedTextColor.GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("language.prompt_name"),
                    value -> replace(new Language(current.id(), value.trim(), current.obfuscationChar(),
                            current.defaultForRaces())));
            return;
        }

        if (slot == OBFUSCATION_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("language.prompt_obfuscation"), value -> {
                char c = value.isBlank() ? '?' : value.trim().charAt(0);
                replace(new Language(current.id(), current.displayName(), c, current.defaultForRaces()));
            });
            return;
        }

        if (slot == RACES_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("language.prompt_races"), value -> {
                List<String> races = value.isBlank() ? List.of()
                        : new ArrayList<>(List.of(value.split("\\s*,\\s*")));
                replace(new Language(current.id(), current.displayName(), current.obfuscationChar(), races));
            });
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
