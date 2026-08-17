package com.sack.rpgroll.magic.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.magic.core.CatalystManager;
import com.sack.rpgroll.magic.core.GrimoireManager;
import com.sack.rpgroll.magic.core.RuneManager;
import com.sack.rpgroll.magic.core.SchoolManager;
import com.sack.rpgroll.magic.core.SpellManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Hub del Magic Studio — enlaza a los 5 navegadores de contenido. */
public class MagicBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int SCHOOLS_SLOT = 10;
    private static final int SPELLS_SLOT = 12;
    private static final int GRIMOIRES_SLOT = 14;
    private static final int RUNES_SLOT = 16;
    private static final int CATALYSTS_SLOT = 22;
    private static final int CLOSE_SLOT = 40;

    private final SchoolManager schoolManager;
    private final SpellManager spellManager;
    private final GrimoireManager grimoireManager;
    private final RuneManager runeManager;
    private final CatalystManager catalystManager;
    private final ChatPromptManager chatPromptManager;

    public MagicBrowserGUI(Player player, SchoolManager schoolManager, SpellManager spellManager,
            GrimoireManager grimoireManager, RuneManager runeManager, CatalystManager catalystManager,
            ChatPromptManager chatPromptManager) {
        super(player, chatPromptManager.lang().component("gui.magic_studio.title"), SIZE);
        this.schoolManager = schoolManager;
        this.spellManager = spellManager;
        this.grimoireManager = grimoireManager;
        this.runeManager = runeManager;
        this.catalystManager = catalystManager;
        this.chatPromptManager = chatPromptManager;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        LangManager lang = chatPromptManager.lang();

        setItem(SCHOOLS_SLOT, new ItemBuilder(Material.ENCHANTED_BOOK)
                .setName(lang.component("gui.magic_studio.schools"))
                .setLore(lang.component("gui.magic_studio.schools_lore", "count", schoolManager.count()))
                .build());

        setItem(SPELLS_SLOT, new ItemBuilder(Material.BLAZE_POWDER)
                .setName(lang.component("gui.magic_studio.spells"))
                .setLore(lang.component("gui.magic_studio.spells_lore", "count", spellManager.count()))
                .build());

        setItem(GRIMOIRES_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(lang.component("gui.magic_studio.grimoires"))
                .setLore(lang.component("gui.magic_studio.grimoires_lore", "count", grimoireManager.count()))
                .build());

        setItem(RUNES_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("gui.magic_studio.runes"))
                .setLore(lang.component("gui.magic_studio.runes_lore", "count", runeManager.count()))
                .build());

        setItem(CATALYSTS_SLOT, new ItemBuilder(Material.BLAZE_ROD)
                .setName(lang.component("gui.magic_studio.catalysts"))
                .setLore(lang.component("gui.magic_studio.catalysts_lore", "count", catalystManager.count()))
                .build());

        setItem(CLOSE_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.close")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == SCHOOLS_SLOT) {
            new SchoolBrowserGUI(player, schoolManager, chatPromptManager).open();
            return;
        }

        if (slot == SPELLS_SLOT) {
            new SpellBrowserGUI(player, spellManager, schoolManager, chatPromptManager).open();
            return;
        }

        if (slot == GRIMOIRES_SLOT) {
            new GrimoireBrowserGUI(player, grimoireManager, spellManager, chatPromptManager).open();
            return;
        }

        if (slot == RUNES_SLOT) {
            new RuneBrowserGUI(player, runeManager, chatPromptManager).open();
            return;
        }

        if (slot == CATALYSTS_SLOT) {
            new CatalystBrowserGUI(player, catalystManager, chatPromptManager).open();
            return;
        }

        if (slot == CLOSE_SLOT) {
            close();
        }
    }

}
