package com.sack.rpgroll.magic.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.magic.core.CatalystManager;
import com.sack.rpgroll.magic.core.SpellCatalyst;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

public class CatalystBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final CatalystManager catalystManager;
    private final ChatPromptManager chatPromptManager;
    private List<SpellCatalyst> catalysts;

    public CatalystBrowserGUI(Player player, CatalystManager catalystManager, ChatPromptManager chatPromptManager) {
        super(player, chatPromptManager.lang().component("gui.catalyst_browser.title"), SIZE);
        this.catalystManager = catalystManager;
        this.chatPromptManager = chatPromptManager;
        this.catalysts = List.copyOf(catalystManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        LangManager lang = chatPromptManager.lang();

        for (int i = 0; i < catalysts.size() && i < 36; i++) {

            SpellCatalyst catalyst = catalysts.get(i);

            setItem(i, new ItemBuilder(SchoolBrowserGUI.parseMaterial(catalyst.material()))
                    .setName(Component.text(catalyst.displayName(), NamedTextColor.GOLD))
                    .setLore(lang.component("gui.common.id_label", "id", catalyst.id()),
                            lang.component("gui.common.click_edit"))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("gui.catalyst_browser.new"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < catalysts.size() && slot < 36) {
            new CatalystEditorGUI(player, catalysts.get(slot), catalystManager, chatPromptManager, this::reopen)
                    .open();
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
        chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.catalyst_browser.prompt_new_id"), value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (catalystManager.exists(id)) {
                chatPromptManager.lang().send(player, "gui.catalyst_browser.already_exists");
                reopen();
                return;
            }

            SpellCatalyst catalyst = new SpellCatalyst(id, id, "BLAZE_ROD", "", 1.0, 1.0, 1.0);
            catalystManager.save(catalyst);
            reopen();
        });
    }

    private void reopen() {
        this.catalysts = List.copyOf(catalystManager.getAll());
        open();
    }

}
