package com.sack.rpgroll.gui.admin;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.api.playerclass.PlayerClass;
import com.sack.rpgroll.api.stats.StatType;
import com.sack.rpgroll.playerclass.ClassManagerImpl;

import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassEditorGUI extends InventoryGUI {

    private static final int SIZE = 27;
    private static final int NAME_SLOT = 9;
    private static final int DESCRIPTION_SLOT = 10;
    private static final int ATTRIBUTES_SLOT = 11;
    private static final int PASSIVE_TRAITS_SLOT = 12;
    private static final int LORE_SLOT = 13;
    private static final int BACK_SLOT = 26;

    private final ClassManagerImpl classManager;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;
    private final Runnable onBack;
    private PlayerClass current;

    public ClassEditorGUI(Player player, PlayerClass playerClass, ClassManagerImpl classManager,
            ChatPromptManager chatPromptManager, LangManager lang, Runnable onBack) {
        super(player, lang.component("class_editor_gui.title", "id", playerClass.id()), SIZE);
        this.current = playerClass;
        this.classManager = classManager;
        this.chatPromptManager = chatPromptManager;
        this.lang = lang;
        this.onBack = onBack;
    }

    private void replace(PlayerClass updated) {
        current = updated;
        classManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(ComponentUtils.parse(lang.raw("class_editor_gui.name_slot_name", "name",
                        current.displayName())).colorIfAbsent(NamedTextColor.YELLOW))
                .setLore(lang.component("class_editor_gui.click_new_value"))
                .build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(lang.component("class_editor_gui.description_slot_name"))
                .setLore(ItemBuilder.toLoreLines(current.description().isBlank()
                        ? lang.raw("class_editor_gui.no_description")
                        : current.description()))
                .build());

        setItem(ATTRIBUTES_SLOT, new ItemBuilder(Material.BEACON)
                .setName(lang.component("class_editor_gui.attributes_slot_name", "attributes",
                        current.baseAttributes()))
                .setLore(lang.component("class_editor_gui.attributes_slot_lore"))
                .build());

        setItem(PASSIVE_TRAITS_SLOT, new ItemBuilder(Material.BOOK)
                .setName(lang.component("class_editor_gui.passive_traits_slot_name", "traits",
                        String.join(", ", current.passiveTraits())))
                .setLore(lang.component("class_editor_gui.passive_traits_slot_lore"))
                .build());

        setItem(LORE_SLOT, new ItemBuilder(Material.PAPER)
                .setName(lang.component("class_editor_gui.lore_slot_name", "count", current.lore().size()))
                .setLore(lang.component("class_editor_gui.lore_slot_lore"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("class_editor_gui.back_button")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, lang.raw("class_editor_gui.prompt_name"), value -> replace(new PlayerClass(
                    current.id(), value, current.description(), current.baseAttributes(), current.passiveTraits(),
                    current.icon(), current.lore())));
            return;
        }

        if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, lang.raw("class_editor_gui.prompt_description"), value -> replace(new PlayerClass(
                    current.id(), current.displayName(), value, current.baseAttributes(), current.passiveTraits(),
                    current.icon(), current.lore())));
            return;
        }

        if (slot == ATTRIBUTES_SLOT) {
            chatPromptManager.prompt(player, lang.raw("class_editor_gui.prompt_attributes"),
                    value -> {
                        Map<StatType, Integer> attributes = parseAttributes(value);
                        replace(new PlayerClass(current.id(), current.displayName(), current.description(),
                                attributes, current.passiveTraits(), current.icon(), current.lore()));
                    });
            return;
        }

        if (slot == PASSIVE_TRAITS_SLOT) {
            chatPromptManager.prompt(player, lang.raw("class_editor_gui.prompt_passive_traits"), value -> {
                List<String> traits = value.isBlank() ? List.of() : List.of(value.split("\\s*,\\s*"));
                replace(new PlayerClass(current.id(), current.displayName(), current.description(),
                        current.baseAttributes(), traits, current.icon(), current.lore()));
            });
            return;
        }

        if (slot == LORE_SLOT) {
            chatPromptManager.prompt(player, lang.raw("class_editor_gui.prompt_lore"), value -> {
                List<String> lore = value.isBlank() ? List.of() : List.of(value.split("\\s*;\\s*"));
                replace(new PlayerClass(current.id(), current.displayName(), current.description(),
                        current.baseAttributes(), current.passiveTraits(), current.icon(), lore));
            });
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
