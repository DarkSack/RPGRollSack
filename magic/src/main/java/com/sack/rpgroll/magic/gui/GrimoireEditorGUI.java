package com.sack.rpgroll.magic.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.magic.core.Grimoire;
import com.sack.rpgroll.magic.core.GrimoireManager;
import com.sack.rpgroll.magic.core.SpellManager;
import com.sack.rpgroll.magic.item.MagicItemFactory;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

public class GrimoireEditorGUI extends InventoryGUI {

    private static final int SIZE = 54;

    private static final int NAME_SLOT = 10;
    private static final int ICON_SLOT = 11;
    private static final int DESCRIPTION_SLOT = 12;
    private static final int SCHOOL_SLOT = 13;
    private static final int LEVEL_SLOT = 14;

    private static final int SPELLS_START = 18;
    private static final int SPELLS_MAX = 27;
    private static final int GIVE_SLOT = 48;
    private static final int ADD_SPELL_SLOT = 49;
    private static final int BACK_SLOT = 53;

    private final GrimoireManager grimoireManager;
    private final SpellManager spellManager;
    private final ChatPromptManager chatPromptManager;
    private final LangManager lang;
    private final Runnable onBack;
    private Grimoire current;

    public GrimoireEditorGUI(Player player, Grimoire grimoire, GrimoireManager grimoireManager,
            SpellManager spellManager, ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, chatPromptManager.lang().component("gui.grimoire_editor.title", "id", grimoire.id()), SIZE);
        this.current = grimoire;
        this.grimoireManager = grimoireManager;
        this.spellManager = spellManager;
        this.chatPromptManager = chatPromptManager;
        this.lang = chatPromptManager.lang();
        this.onBack = onBack;
    }

    private void replace(Grimoire updated) {
        current = updated;
        grimoireManager.save(current);
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

        setItem(SCHOOL_SLOT, new ItemBuilder(Material.ENCHANTED_BOOK)
                .setName(lang.component("gui.grimoire_editor.school_label", "value",
                        current.schoolId() == null ? lang.raw("gui.grimoire_editor.school_any") : current.schoolId()))
                .setLore(lang.component("gui.grimoire_editor.school_lore"))
                .build());

        setItem(LEVEL_SLOT, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(lang.component("gui.grimoire_editor.level_label", "level", current.requiredLevel()))
                .setLore(lang.component("gui.common.step_1")).build());

        List<String> spellIds = current.spellIds();

        for (int i = 0; i < spellIds.size() && i < SPELLS_MAX; i++) {

            String spellId = spellIds.get(i);
            String displayName = spellManager.get(spellId).map(spell -> spell.displayName()).orElse(spellId);

            setItem(SPELLS_START + i, new ItemBuilder(Material.BLAZE_POWDER)
                    .setName(Component.text(displayName, NamedTextColor.LIGHT_PURPLE))
                    .setLore(lang.component("gui.common.id_label", "id", spellId),
                            lang.component("gui.common.shift_remove"))
                    .build());
        }

        setItem(GIVE_SLOT, new ItemBuilder(Material.CHEST)
                .setName(lang.component("gui.grimoire_editor.give"))
                .build());

        setItem(ADD_SPELL_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("gui.grimoire_editor.add_spell"))
                .setLore(lang.component("gui.grimoire_editor.add_spell_lore")).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        ClickType click = event.getClick();
        int sign = click == ClickType.RIGHT ? -1 : 1;

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.common.prompt_name"), value -> replace(new Grimoire(current.id(),
                    value, current.icon(), current.description(), current.schoolId(), current.requiredLevel(),
                    current.spellIds())));
            return;
        }

        if (slot == ICON_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.common.prompt_icon"), value -> replace(new Grimoire(
                    current.id(), current.displayName(), value, current.description(), current.schoolId(),
                    current.requiredLevel(), current.spellIds())));
            return;
        }

        if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.common.prompt_description"), value -> replace(new Grimoire(
                    current.id(), current.displayName(), current.icon(), value, current.schoolId(),
                    current.requiredLevel(), current.spellIds())));
            return;
        }

        if (slot == SCHOOL_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.grimoire_editor.prompt_school"),
                    value -> replace(new Grimoire(current.id(), current.displayName(), current.icon(),
                            current.description(), value.equalsIgnoreCase("ninguna") ? null : value.trim(),
                            current.requiredLevel(), current.spellIds())));
            return;
        }

        if (slot == LEVEL_SLOT) {
            replace(new Grimoire(current.id(), current.displayName(), current.icon(), current.description(),
                    current.schoolId(), Math.max(0, current.requiredLevel() + sign), current.spellIds()));
            return;
        }

        if (slot >= SPELLS_START && slot < SPELLS_START + Math.min(current.spellIds().size(), SPELLS_MAX)) {
            if (event.isShiftClick()) {
                List<String> spellIds = new ArrayList<>(current.spellIds());
                spellIds.remove(slot - SPELLS_START);
                replace(new Grimoire(current.id(), current.displayName(), current.icon(), current.description(),
                        current.schoolId(), current.requiredLevel(), spellIds));
            }
            return;
        }

        if (slot == GIVE_SLOT) {
            player.getInventory().addItem(MagicItemFactory.createGrimoire(current, lang));
            lang.send(player, "gui.grimoire_editor.given", "name", current.displayName());
            return;
        }

        if (slot == ADD_SPELL_SLOT) {
            chatPromptManager.prompt(player, lang.raw("gui.grimoire_editor.prompt_add_spell"), value -> {

                String spellId = value.trim().toLowerCase(java.util.Locale.ROOT);

                if (!spellManager.exists(spellId)) {
                    lang.send(player, "gui.grimoire_editor.unknown_spell");
                    return;
                }

                if (current.spellIds().contains(spellId)) {
                    lang.send(player, "gui.grimoire_editor.spell_already_added");
                    return;
                }

                List<String> spellIds = new ArrayList<>(current.spellIds());
                spellIds.add(spellId);
                replace(new Grimoire(current.id(), current.displayName(), current.icon(), current.description(),
                        current.schoolId(), current.requiredLevel(), spellIds));
            });
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
