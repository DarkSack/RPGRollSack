package com.sack.rpgroll.gui.admin;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.api.race.Race;
import com.sack.rpgroll.api.race.RacePhysicalModifiers;
import com.sack.rpgroll.race.RaceManagerImpl;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RaceBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final RaceManagerImpl raceManager;
    private final ChatPromptManager chatPromptManager;
    private List<Race> races;

    public RaceBrowserGUI(Player player, RaceManagerImpl raceManager, ChatPromptManager chatPromptManager) {
        super(player, Component.text("Razas RPGRoll", NamedTextColor.GOLD), SIZE);
        this.raceManager = raceManager;
        this.chatPromptManager = chatPromptManager;
        this.races = List.copyOf(raceManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < races.size() && i < 36; i++) {

            Race race = races.get(i);

            setItem(i, new ItemBuilder(Material.PLAYER_HEAD)
                    .setName(Component.text(race.id(), NamedTextColor.YELLOW))
                    .setLore(Component.text(race.baseAttributes().size() + " atributo(s) base", NamedTextColor.GRAY),
                            Component.text("Click para editar", NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Crear raza nueva", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Cerrar"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < races.size() && slot < 36) {
            new RaceEditorGUI(player, races.get(slot), raceManager, chatPromptManager, this::reopen).open();
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
        chatPromptManager.prompt(player, "Escribí el id de la nueva raza:", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (raceManager.exists(id)) {
                player.sendMessage(Component.text("Ya existe una raza con ese id.", NamedTextColor.RED));
                reopen();
                return;
            }

            Race race = new Race(id, id, "", Map.of(), List.of(), "", List.of(), RacePhysicalModifiers.none());
            raceManager.save(race);
            reopen();
        });
    }

    private void reopen() {
        this.races = List.copyOf(raceManager.getAll());
        open();
    }

}
