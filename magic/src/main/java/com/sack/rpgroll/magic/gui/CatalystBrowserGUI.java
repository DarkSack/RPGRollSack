package com.sack.rpgroll.magic.gui;

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
        super(player, Component.text("Catalizadores", NamedTextColor.GOLD), SIZE);
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

        for (int i = 0; i < catalysts.size() && i < 36; i++) {

            SpellCatalyst catalyst = catalysts.get(i);

            setItem(i, new ItemBuilder(SchoolBrowserGUI.parseMaterial(catalyst.material()))
                    .setName(Component.text(catalyst.displayName(), NamedTextColor.GOLD))
                    .setLore(Component.text("id: " + catalyst.id(), NamedTextColor.GRAY),
                            Component.text("Click para editar", NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Crear catalizador nuevo", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
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
        chatPromptManager.prompt(player, "Escribí el id del nuevo catalizador:", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (catalystManager.exists(id)) {
                player.sendMessage(Component.text("Ya existe un catalizador con ese id.", NamedTextColor.RED));
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
