package com.sack.rpgroll.magic.gui;

import com.sack.rpgroll.util.ComponentUtils;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.magic.core.Spell;
import com.sack.rpgroll.magic.core.SpellCastTrigger;
import com.sack.rpgroll.magic.core.SpellCost;
import com.sack.rpgroll.magic.core.SpellManager;
import com.sack.rpgroll.magic.core.SchoolManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public class SpellBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final SpellManager spellManager;
    private final SchoolManager schoolManager;
    private final ChatPromptManager chatPromptManager;
    private List<Spell> spells;

    public SpellBrowserGUI(Player player, SpellManager spellManager, SchoolManager schoolManager,
            ChatPromptManager chatPromptManager) {
        super(player, chatPromptManager.lang().component("gui.spell_browser.title"), SIZE);
        this.spellManager = spellManager;
        this.schoolManager = schoolManager;
        this.chatPromptManager = chatPromptManager;
        this.spells = List.copyOf(spellManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        LangManager lang = chatPromptManager.lang();

        for (int i = 0; i < spells.size() && i < 36; i++) {

            Spell spell = spells.get(i);
            NamedTextColor color = SchoolBrowserGUI.parseColor(spell.color());

            setItem(i, new ItemBuilder(SchoolBrowserGUI.parseMaterial(spell.icon()))
                    .setName(ComponentUtils.parseWithDefault(spell.displayName(), color))
                    .setLore(lang.component("gui.common.id_label", "id", spell.id()),
                            lang.component("gui.spell_browser.school_label", "schoolId", spell.schoolId()),
                            lang.component("gui.spell_browser.rarity_level_label", "rarity", spell.rarity(),
                                    "level", spell.level()),
                            lang.component("gui.spell_browser.component_count_label", "count",
                                    spell.components().size()),
                            lang.component("gui.common.click_edit"))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("gui.spell_browser.new"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < spells.size() && slot < 36) {
            new SpellEditorHubGUI(player, spells.get(slot), spellManager, schoolManager, chatPromptManager,
                    this::reopen).open();
            return;
        }

        if (slot == NEW_SLOT) {
            promptNew();
            return;
        }

        if (slot == BACK_SLOT) {
            close();
        }
    }

    private void promptNew() {

        LangManager lang = chatPromptManager.lang();

        if (schoolManager.count() == 0) {
            lang.send(player, "gui.spell_browser.need_school_first");
            return;
        }

        chatPromptManager.prompt(player, lang.raw("gui.spell_browser.prompt_new"), value -> {

            String[] parts = value.trim().split("\\s+", 2);

            if (parts.length < 2) {
                lang.send(player, "gui.spell_browser.invalid_format");
                return;
            }

            String id = parts[0].toLowerCase(Locale.ROOT).replace(' ', '_');
            String schoolId = parts[1].toLowerCase(Locale.ROOT);

            if (spellManager.exists(id)) {
                lang.send(player, "gui.spell_browser.already_exists");
                reopen();
                return;
            }

            if (!schoolManager.exists(schoolId)) {
                lang.send(player, "gui.spell_browser.unknown_school", "id", schoolId);
                reopen();
                return;
            }

            Spell spell = new Spell(id, id, "BLAZE_POWDER", "WHITE", schoolId, null, 1, SpellCost.none(), 0, 20,
                    SpellCastTrigger.RIGHT_CLICK, null, 0, Set.of(), "", List.of());

            spellManager.save(spell);
            reopen();
        });
    }

    private void reopen() {
        this.spells = List.copyOf(spellManager.getAll());
        open();
    }

}
