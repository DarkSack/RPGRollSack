package com.sack.rpgroll.magic.gui;

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
        super(player, Component.text("Magic Studio", NamedTextColor.GOLD), SIZE);
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

        setItem(SCHOOLS_SLOT, new ItemBuilder(Material.ENCHANTED_BOOK)
                .setName(Component.text("Escuelas", NamedTextColor.AQUA))
                .setLore(Component.text(schoolManager.count() + " definida(s)", NamedTextColor.GRAY))
                .build());

        setItem(SPELLS_SLOT, new ItemBuilder(Material.BLAZE_POWDER)
                .setName(Component.text("Hechizos", NamedTextColor.LIGHT_PURPLE))
                .setLore(Component.text(spellManager.count() + " definido(s)", NamedTextColor.GRAY))
                .build());

        setItem(GRIMOIRES_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(Component.text("Grimorios", NamedTextColor.YELLOW))
                .setLore(Component.text(grimoireManager.count() + " definido(s)", NamedTextColor.GRAY))
                .build());

        setItem(RUNES_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Runas", NamedTextColor.GREEN))
                .setLore(Component.text(runeManager.count() + " definida(s)", NamedTextColor.GRAY))
                .build());

        setItem(CATALYSTS_SLOT, new ItemBuilder(Material.BLAZE_ROD)
                .setName(Component.text("Catalizadores", NamedTextColor.GOLD))
                .setLore(Component.text(catalystManager.count() + " definido(s)", NamedTextColor.GRAY))
                .build());

        setItem(CLOSE_SLOT, ItemBuilder.createCancelButton("Cerrar"));
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
