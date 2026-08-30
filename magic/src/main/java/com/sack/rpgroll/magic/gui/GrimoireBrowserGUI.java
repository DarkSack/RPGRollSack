package com.sack.rpgroll.magic.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.magic.core.Grimoire;
import com.sack.rpgroll.magic.core.GrimoireManager;
import com.sack.rpgroll.magic.core.SpellManager;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

public class GrimoireBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final GrimoireManager grimoireManager;
    private final SpellManager spellManager;
    private final ChatPromptManager chatPromptManager;
    private List<Grimoire> grimoires;

    public GrimoireBrowserGUI(Player player, GrimoireManager grimoireManager, SpellManager spellManager,
            ChatPromptManager chatPromptManager) {
        super(player, chatPromptManager.lang().component("gui.grimoire_browser.title"), SIZE);
        this.grimoireManager = grimoireManager;
        this.spellManager = spellManager;
        this.chatPromptManager = chatPromptManager;
        this.grimoires = List.copyOf(grimoireManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        LangManager lang = chatPromptManager.lang();

        for (int i = 0; i < grimoires.size() && i < 36; i++) {

            Grimoire grimoire = grimoires.get(i);

            setItem(i, new ItemBuilder(SchoolBrowserGUI.parseMaterial(grimoire.icon()))
                    .setName(ComponentUtils.parse(grimoire.displayName()))
                    .setLore(lang.component("gui.common.id_label", "id", grimoire.id()),
                            lang.component("gui.grimoire_browser.spell_count_lore", "count", grimoire.spellIds().size()),
                            lang.component("gui.common.click_edit"))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("gui.grimoire_browser.new"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < grimoires.size() && slot < 36) {
            new GrimoireEditorGUI(player, grimoires.get(slot), grimoireManager, spellManager, chatPromptManager,
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
        chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.grimoire_browser.prompt_new_id"), value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (grimoireManager.exists(id)) {
                chatPromptManager.lang().send(player, "gui.grimoire_browser.already_exists");
                reopen();
                return;
            }

            Grimoire grimoire = new Grimoire(id, id, "WRITTEN_BOOK", "", null, 0, List.of());
            grimoireManager.save(grimoire);
            reopen();
        });
    }

    private void reopen() {
        this.grimoires = List.copyOf(grimoireManager.getAll());
        open();
    }

}
