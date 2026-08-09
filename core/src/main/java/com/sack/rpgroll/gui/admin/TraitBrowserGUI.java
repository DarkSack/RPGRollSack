package com.sack.rpgroll.gui.admin;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.gameplay.trait.Trait;
import com.sack.rpgroll.gameplay.trait.TraitEffect;
import com.sack.rpgroll.gameplay.trait.TraitManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

public class TraitBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final TraitManager traitManager;
    private final ChatPromptManager chatPromptManager;
    private List<Trait> traits;

    public TraitBrowserGUI(Player player, TraitManager traitManager, ChatPromptManager chatPromptManager) {
        super(player, Component.text("Traits RPGRoll", NamedTextColor.GOLD), SIZE);
        this.traitManager = traitManager;
        this.chatPromptManager = chatPromptManager;
        this.traits = List.copyOf(traitManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < traits.size() && i < 36; i++) {

            Trait trait = traits.get(i);

            setItem(i, new ItemBuilder(Material.NETHER_STAR)
                    .setName(Component.text(trait.id(), NamedTextColor.YELLOW))
                    .setLore(Component.text("Nivel " + trait.requiredLevel(), NamedTextColor.GRAY),
                            Component.text("Click para editar", NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Crear trait nuevo", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Cerrar"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < traits.size() && slot < 36) {
            new TraitEditorGUI(player, traits.get(slot), traitManager, chatPromptManager, this::reopen).open();
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
        chatPromptManager.prompt(player, "Escribí el id del nuevo trait:", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (traitManager.exists(id)) {
                player.sendMessage(Component.text("Ya existe un trait con ese id.", NamedTextColor.RED));
                reopen();
                return;
            }

            Trait trait = new Trait(id, id, "", 1, TraitEffect.empty());
            traitManager.save(trait);
            reopen();
        });
    }

    private void reopen() {
        this.traits = List.copyOf(traitManager.getAll());
        open();
    }

}
