package com.sack.rpgroll.gui.character;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.api.race.Race;
import com.sack.rpgroll.api.race.RaceManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * GUI para seleccionar la raza del personaje. Las razas provienen de
 * RaceManager (races/*.yml) y se muestran como cabezas de jugador con
 * textura custom (base64).
 */
public class RaceSelectionGUI extends InventoryGUI {
        private static final MiniMessage MINI = MiniMessage.miniMessage();
        private static final int[] CONTENT_SLOTS = { 10, 11, 12, 13, 14, 15, 16 };
        private static final int CANCEL_SLOT = 22;

        private final Consumer<String> onRaceSelected;
        private final Map<Integer, String> slotToRace;
        private final RaceManager raceManager;
        private final boolean mandatory;
        private final LangManager lang;

        /**
         * @param mandatory si es true, no se muestra botón de cancelar y la GUI
         *                  se reabre automáticamente si se cierra sin seleccionar.
         */
        public RaceSelectionGUI(Player player, RaceManager raceManager, Consumer<String> onRaceSelected,
                        boolean mandatory, LangManager lang) {
                super(
                                player,
                                lang.component("race_selection_gui.title").decorate(TextDecoration.BOLD),
                                27);

                this.raceManager = raceManager;
                this.onRaceSelected = onRaceSelected;
                this.mandatory = mandatory;
                this.lang = lang;
                this.slotToRace = new HashMap<>();
        }

        @Override
        public boolean isSelectionRequired() {
                return mandatory;
        }

        @Override
        public void build() {

                clear();
                slotToRace.clear();

                for (int i = 0; i < 9; i++) {
                        setItem(i, ItemBuilder.createFiller());
                }

                List<Race> races = new ArrayList<>(raceManager.getAll());

                if (races.isEmpty()) {
                        setItem(13, new ItemBuilder(Material.BARRIER)
                                        .setName(lang.component("race_selection_gui.none_available"))
                                        .setLore(lang.component("race_selection_gui.none_available_lore"))
                                        .build());
                } else {
                        for (int i = 0; i < races.size() && i < CONTENT_SLOTS.length; i++) {
                                addRace(CONTENT_SLOTS[i], races.get(i));
                        }
                }

                for (int i = 18; i < 27; i++) {
                        setItem(i, ItemBuilder.createFiller());
                }

                if (!mandatory) {
                        setItem(CANCEL_SLOT, ItemBuilder.createCancelButton(lang.raw("race_selection_gui.cancel_button")));
                }
        }

        private void addRace(int slot, Race race) {

                List<Component> lore = new ArrayList<>();

                lore.addAll(ItemBuilder.toLoreLines(race.description()));

                race.baseAttributes().forEach((stat, value) -> {
                        String sign = value >= 0 ? "+" : "";
                        lore.add(Component.text(sign + value + " " + stat.name(), NamedTextColor.DARK_GRAY));
                });

                for (String loreLine : race.lore()) {
                        lore.addAll(ItemBuilder.toLoreLines(loreLine));
                }

                ItemStack item = ItemBuilder.skull(race.icon())
                                .setName(MINI.deserialize(race.displayName()))
                                .setLore(lore.toArray(new Component[0]))
                                .build();

                setItem(slot, item);
                slotToRace.put(slot, race.id());
        }

        @Override
        public void handleClick(InventoryClickEvent event) {

                event.setCancelled(true);

                int slot = event.getRawSlot();

                if (!mandatory && slot == CANCEL_SLOT) {
                        close();
                        lang.send(player, "race_selection_gui.cancelled");
                        return;
                }

                if (slotToRace.containsKey(slot)) {
                        String selectedRaceId = slotToRace.get(slot);
                        markSelectionMade();
                        close();
                        onRaceSelected.accept(selectedRaceId);
                }
        }

}
