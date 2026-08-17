package com.sack.rpgroll.gui.admin;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.gameplay.trait.Trait;
import com.sack.rpgroll.gameplay.trait.TraitEffect;
import com.sack.rpgroll.gameplay.trait.TraitManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Editor de un trait — identidad + sus 10 bonos de {@link TraitEffect}. Dado
 * el número de campos, se editan todos juntos vía una sola línea de chat
 * en vez de un botón por bono.
 */
public class TraitEditorGUI extends InventoryGUI {

    private static final int SIZE = 27;
    private static final int NAME_SLOT = 9;
    private static final int DESCRIPTION_SLOT = 10;
    private static final int LEVEL_SLOT = 11;
    private static final int EFFECT_SLOT = 12;
    private static final int BACK_SLOT = 26;

    private final TraitManager traitManager;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;
    private final Runnable onBack;
    private Trait current;

    public TraitEditorGUI(Player player, Trait trait, TraitManager traitManager, ChatPromptManager chatPromptManager,
            LangManager lang, Runnable onBack) {
        super(player, lang.component("trait_editor_gui.title", "id", trait.id()), SIZE);
        this.current = trait;
        this.traitManager = traitManager;
        this.chatPromptManager = chatPromptManager;
        this.lang = lang;
        this.onBack = onBack;
    }

    private void replace(Trait updated) {
        current = updated;
        traitManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(ComponentUtils.parse(lang.raw("trait_editor_gui.name_slot_name", "name", current.name()))
                        .colorIfAbsent(NamedTextColor.YELLOW))
                .setLore(lang.component("trait_editor_gui.click_new_value"))
                .build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(lang.component("trait_editor_gui.description_slot_name"))
                .setLore(ItemBuilder.toLoreLines(current.description().isBlank()
                        ? lang.raw("trait_editor_gui.no_description")
                        : current.description()))
                .build());

        setItem(LEVEL_SLOT, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(lang.component("trait_editor_gui.level_slot_name", "level", current.requiredLevel()))
                .setLore(lang.component("trait_editor_gui.level_slot_lore"))
                .build());

        TraitEffect effect = current.effect();
        setItem(EFFECT_SLOT, new ItemBuilder(Material.POTION)
                .setName(lang.component("trait_editor_gui.effect_slot_name"))
                .setLore(lang.component("trait_editor_gui.effect_lore_line1",
                        "str", effect.strengthBonus(), "dex", effect.dexterityBonus(), "con",
                        effect.constitutionBonus()),
                        lang.component("trait_editor_gui.effect_lore_line2",
                                "int", effect.intelligenceBonus(), "wis", effect.wisdomBonus(), "cha",
                                effect.charismaBonus()),
                        lang.component("trait_editor_gui.effect_lore_line3",
                                "hp", effect.healthBonus(), "mp", effect.manaBonus(), "dmg", effect.damageBonus(),
                                "def", effect.defenseBonus()),
                        lang.component("trait_editor_gui.effect_lore_hint"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("trait_editor_gui.back_button")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        ClickType click = event.getClick();

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, lang.raw("trait_editor_gui.prompt_name"), value -> replace(new Trait(
                    current.id(), value, current.description(), current.requiredLevel(), current.effect())));
            return;
        }

        if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, lang.raw("trait_editor_gui.prompt_description"), value -> replace(
                    new Trait(current.id(), current.name(), value, current.requiredLevel(), current.effect())));
            return;
        }

        if (slot == LEVEL_SLOT) {
            int delta = click == ClickType.RIGHT ? -1 : 1;
            replace(new Trait(current.id(), current.name(), current.description(),
                    Math.max(1, current.requiredLevel() + delta), current.effect()));
            return;
        }

        if (slot == EFFECT_SLOT) {
            chatPromptManager.prompt(player, lang.raw("trait_editor_gui.prompt_effect"), value -> {

                        String[] parts = value.split("\\s*,\\s*");

                        if (parts.length < 10) {
                            player.sendMessage(lang.component("trait_editor_gui.effect_missing_values"));
                            return;
                        }

                        try {
                            TraitEffect effect = new TraitEffect(
                                    Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()),
                                    Integer.parseInt(parts[2].trim()), Integer.parseInt(parts[3].trim()),
                                    Integer.parseInt(parts[4].trim()), Integer.parseInt(parts[5].trim()),
                                    Integer.parseInt(parts[6].trim()), Integer.parseInt(parts[7].trim()),
                                    Double.parseDouble(parts[8].trim()), Double.parseDouble(parts[9].trim()));

                            replace(new Trait(current.id(), current.name(), current.description(),
                                    current.requiredLevel(), effect));
                        } catch (NumberFormatException e) {
                            player.sendMessage(lang.component("trait_editor_gui.effect_values_not_numeric"));
                        }
                    });
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
