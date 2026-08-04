package com.sack.rpgroll.crates.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.crates.core.Crate;
import com.sack.rpgroll.crates.core.CrateAction;
import com.sack.rpgroll.crates.core.CrateManager;
import com.sack.rpgroll.crates.core.CrateReward;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

public class CrateBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final CrateManager crateManager;
    private final ChatPromptManager chatPromptManager;
    private List<Crate> crates;

    public CrateBrowserGUI(Player player, CrateManager crateManager, ChatPromptManager chatPromptManager) {
        super(player, Component.text("Crates RPGRoll", NamedTextColor.GOLD), SIZE);
        this.crateManager = crateManager;
        this.chatPromptManager = chatPromptManager;
        this.crates = List.copyOf(crateManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < crates.size() && i < 36; i++) {

            Crate crate = crates.get(i);

            setItem(i, new ItemBuilder(Material.ENDER_CHEST)
                    .setName(Component.text(crate.id(), NamedTextColor.YELLOW))
                    .setLore(Component.text(crate.rewards().size() + " recompensa(s)", NamedTextColor.GRAY),
                            Component.text("Click para editar", NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Crear crate nuevo", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Cerrar"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < crates.size() && slot < 36) {
            new CrateEditorGUI(player, crates.get(slot), crateManager, chatPromptManager, this::reopen).open();
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
        chatPromptManager.prompt(player, "Escribí el id del nuevo crate:", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (crateManager.exists(id)) {
                player.sendMessage(Component.text("Ya existe un crate con ese id.", NamedTextColor.RED));
                reopen();
                return;
            }

            List<CrateAction> actions = List.of(
                    new CrateAction(CrateAction.CrateActionType.GIVE_ITEM, "GOLD_INGOT,1"),
                    new CrateAction(CrateAction.CrateActionType.MESSAGE, "&aGanaste un Lingote de Oro."));

            CrateReward defaultReward = new CrateReward("oro", "&f1 Lingote de Oro", Material.GOLD_INGOT, List.of(),
                    100, false, actions);

            Crate crate = new Crate(id, id, id, true, Material.TRIPWIRE_HOOK, "Llave: " + id, List.of(), List.of(),
                    List.of(defaultReward));

            crateManager.save(crate);
            reopen();
        });
    }

    private void reopen() {
        this.crates = List.copyOf(crateManager.getAll());
        build();
    }

}
