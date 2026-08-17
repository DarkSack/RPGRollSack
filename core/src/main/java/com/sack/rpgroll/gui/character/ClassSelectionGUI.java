package com.sack.rpgroll.gui.character;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.api.playerclass.ClassManager;
import com.sack.rpgroll.api.playerclass.PlayerClass;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

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
 * GUI para seleccionar la clase del personaje. Las clases provienen de
 * ClassManager (classes/*.yml), mismo patrón que RaceSelectionGUI.
 */
public class ClassSelectionGUI extends InventoryGUI {

        private static final int[] CONTENT_SLOTS = { 10, 11, 12, 13, 14, 15, 16 };
        private static final int CANCEL_SLOT = 22;

        private final ClassManager classManager;
        private final Consumer<String> onClassSelected;
        private final Map<Integer, String> slotToClass;
        private final String selectedRace;
        private final boolean mandatory;
        private final LangManager lang;

        public ClassSelectionGUI(Player player, ClassManager classManager, String selectedRace,
                        Consumer<String> onClassSelected, boolean mandatory, LangManager lang) {
                super(player, lang.component("class_selection_gui.title").decorate(TextDecoration.BOLD),
                                27);
                this.classManager = classManager;
                this.onClassSelected = onClassSelected;
                this.slotToClass = new HashMap<>();
                this.selectedRace = selectedRace;
                this.mandatory = mandatory;
                this.lang = lang;
        }

        @Override
        public boolean isSelectionRequired() {
                return mandatory;
        }

        @Override
        public void build() {

                clear();
                slotToClass.clear();

                for (int i = 0; i < 9; i++) {
                        setItem(i, ItemBuilder.createFiller());
                }

                setItem(4, new ItemBuilder(Material.PAPER)
                                .setName(lang.component("class_selection_gui.selected_race_name")
                                                .decorate(TextDecoration.BOLD))
                                .setLore(
                                                Component.text(selectedRace, NamedTextColor.YELLOW),
                                                lang.component("class_selection_gui.selected_race_lore"))
                                .build());

                List<PlayerClass> classes = new ArrayList<>(classManager.getAll());

                if (classes.isEmpty()) {
                        setItem(13, new ItemBuilder(Material.BARRIER)
                                        .setName(lang.component("class_selection_gui.none_available"))
                                        .setLore(lang.component("class_selection_gui.none_available_lore"))
                                        .build());
                } else {
                        for (int i = 0; i < classes.size() && i < CONTENT_SLOTS.length; i++) {
                                addClass(CONTENT_SLOTS[i], classes.get(i));
                        }
                }

                for (int i = 18; i < 27; i++) {
                        setItem(i, ItemBuilder.createFiller());
                }

                if (!mandatory) {
                        setItem(CANCEL_SLOT, ItemBuilder.createCancelButton(lang.raw("class_selection_gui.back_button")));
                }
        }

        private void addClass(int slot, PlayerClass playerClass) {

                List<Component> lore = new ArrayList<>();

                lore.addAll(ItemBuilder.toLoreLines(playerClass.description()));

                playerClass.baseAttributes().forEach((stat, value) -> {
                        String sign = value >= 0 ? "+" : "";
                        lore.add(Component.text(sign + value + " " + stat.name(), NamedTextColor.DARK_GRAY));
                });

                for (String loreLine : playerClass.lore()) {
                        lore.addAll(ItemBuilder.toLoreLines(loreLine));
                }

                ItemStack item = ItemBuilder.skull(playerClass.icon())
                                .setName(ComponentUtils.parse(playerClass.displayName())
                                                .decorate(TextDecoration.BOLD))
                                .setLore(lore.toArray(new Component[0]))
                                .build();

                setItem(slot, item);
                slotToClass.put(slot, playerClass.id());
        }

        @Override
        public void handleClick(InventoryClickEvent event) {

                event.setCancelled(true);

                int slot = event.getRawSlot();

                if (!mandatory && slot == CANCEL_SLOT) {
                        close();
                        lang.send(player, "class_selection_gui.cancelled");
                        return;
                }

                if (slotToClass.containsKey(slot)) {
                        String selectedClassId = slotToClass.get(slot);
                        markSelectionMade();
                        close();
                        onClassSelected.accept(selectedClassId);
                }
        }

}
