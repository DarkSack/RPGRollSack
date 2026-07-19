package com.sack.rpgroll.gui.character;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.race.Race;
import com.sack.rpgroll.race.RaceManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * GUI para seleccionar la raza del personaje.
 * Las razas mostradas provienen de RaceManager (races/*.yml), no están
 * hardcodeadas — cualquier raza que el admin agregue aparece automáticamente
 * tras un /rpg reload.
 */
public class RaceSelectionGUI extends InventoryGUI {

        private static final int[] CONTENT_SLOTS = { 10, 11, 12, 13, 14, 15, 16 };

        private final Consumer<String> onRaceSelected;
        private final Map<Integer, String> slotToRace;
        private final RaceManager raceManager;

        public RaceSelectionGUI(Player player, RaceManager raceManager, Consumer<String> onRaceSelected) {
                super(
                                player,
                                Component.text("Selecciona tu Raza", NamedTextColor.GOLD)
                                                .decorate(TextDecoration.BOLD),
                                27);

                this.raceManager = raceManager;
                this.onRaceSelected = onRaceSelected;
                this.slotToRace = new HashMap<>();
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
                        setItem(13, new ItemBuilder(org.bukkit.Material.BARRIER)
                                        .setName(Component.text("Sin razas disponibles", NamedTextColor.RED))
                                        .setLore(Component.text("No hay razas cargadas. Contacta a un admin.",
                                                        NamedTextColor.GRAY))
                                        .build());
                } else {
                        for (int i = 0; i < races.size() && i < CONTENT_SLOTS.length; i++) {
                                addRace(CONTENT_SLOTS[i], races.get(i));
                        }

                        if (races.size() > CONTENT_SLOTS.length) {
                                player.getServer().getLogger().warning(
                                                "RaceSelectionGUI: hay más razas (" + races.size()
                                                                + ") de las que caben en la GUI ("
                                                                + CONTENT_SLOTS.length
                                                                + "). Algunas no se muestran — considerar paginación.");
                        }
                }

                for (int i = 18; i < 27; i++) {
                        setItem(i, ItemBuilder.createFiller());
                }

                setItem(22, ItemBuilder.createCancelButton("Cancelar"));
        }

        private void addRace(int slot, Race race) {

                List<Component> lore = new ArrayList<>();

                if (!race.description().isEmpty()) {
                        lore.add(Component.text(ChatColor.translateAlternateColorCodes('&', race.description())));
                }

                race.baseAttributes().forEach((stat, value) -> {
                        String sign = value >= 0 ? "+" : "";
                        lore.add(Component.text(sign + value + " " + stat.name(), NamedTextColor.DARK_GRAY));
                });

                for (String loreLine : race.lore()) {
                        lore.add(Component.text(ChatColor.translateAlternateColorCodes('&', loreLine)));
                }

                ItemStack item = new ItemBuilder(race.icon())
                                .setName(Component.text(ChatColor.translateAlternateColorCodes('&', race.displayName()))
                                                .decorate(TextDecoration.BOLD))
                                .setLore(lore.toArray(new Component[0]))
                                .build();

                setItem(slot, item);
                slotToRace.put(slot, race.id());
        }

        @Override
        public void handleClick(InventoryClickEvent event) {

                event.setCancelled(true);

                int slot = event.getRawSlot();

                if (slot == 22) {
                        close();
                        player.sendMessage(Component.text("Creación de personaje cancelada.", NamedTextColor.YELLOW));
                        return;
                }

                if (slotToRace.containsKey(slot)) {
                        String selectedRaceId = slotToRace.get(slot);
                        close();
                        onRaceSelected.accept(selectedRaceId);
                }
        }

}