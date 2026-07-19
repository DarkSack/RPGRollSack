package com.sack.rpgroll.gui.character;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * GUI para seleccionar la clase del personaje.
 */
public class ClassSelectionGUI extends InventoryGUI {

        private final Consumer<String> onClassSelected;
        private final Map<Integer, String> slotToClass;
        private final String selectedRace;

        public ClassSelectionGUI(Player player, String selectedRace, Consumer<String> onClassSelected) {
                super(player, Component.text("Selecciona tu Clase", NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
                                27);
                this.onClassSelected = onClassSelected;
                this.slotToClass = new HashMap<>();
                this.selectedRace = selectedRace;
        }

        @Override
        public void build() {
                clear();
                // ===== Fila superior =====
                for (int i = 0; i < 9; i++) {
                        setItem(i, ItemBuilder.createFiller());
                }
                setItem(4,
                                new ItemBuilder(Material.PAPER)
                                                .setName(
                                                                Component.text("Raza seleccionada", NamedTextColor.GOLD)
                                                                                .decorate(TextDecoration.BOLD))
                                                .setLore(
                                                                Component.text(selectedRace, NamedTextColor.YELLOW),
                                                                Component.text("Ahora elige tu clase.",
                                                                                NamedTextColor.GRAY))
                                                .build());

                // ===== Clases =====

                addClass(
                                10,
                                "Guerrero",
                                Material.IRON_SWORD,

                                Component.text(
                                                "Maestro del combate cuerpo a cuerpo",
                                                NamedTextColor.GRAY),

                                Component.text(
                                                "Armas: Espadas, Hachas y Escudos",
                                                NamedTextColor.DARK_GRAY),

                                Component.text(
                                                "Rol: Tanque / DPS",
                                                NamedTextColor.DARK_GRAY));

                addClass(
                                11,
                                "Mago",
                                Material.ENCHANTED_BOOK,

                                Component.text(
                                                "Maestro de las artes arcanas",
                                                NamedTextColor.GRAY),

                                Component.text(
                                                "Armas: Bastones y Libros",
                                                NamedTextColor.DARK_GRAY),

                                Component.text(
                                                "Rol: DPS mágico / Control",
                                                NamedTextColor.DARK_GRAY));

                addClass(
                                12,
                                "Pícaro",
                                Material.IRON_SWORD,

                                Component.text(
                                                "Experto en sigilo y trampas",
                                                NamedTextColor.GRAY),

                                Component.text(
                                                "Armas: Dagas y Arcos",
                                                NamedTextColor.DARK_GRAY),

                                Component.text(
                                                "Rol: DPS / Sigilo",
                                                NamedTextColor.DARK_GRAY));

                addClass(
                                13,
                                "Clérigo",
                                Material.GOLDEN_APPLE,

                                Component.text(
                                                "Sanador y protector divino",
                                                NamedTextColor.GRAY),

                                Component.text(
                                                "Armas: Mazas y Escudos",
                                                NamedTextColor.DARK_GRAY),

                                Component.text(
                                                "Rol: Sanador / Soporte",
                                                NamedTextColor.DARK_GRAY));

                addClass(
                                14,
                                "Paladín",
                                Material.DIAMOND_SWORD,

                                Component.text(
                                                "Guerrero sagrado",
                                                NamedTextColor.GRAY),

                                Component.text(
                                                "Armas: Espadas y Escudos",
                                                NamedTextColor.DARK_GRAY),

                                Component.text(
                                                "Rol: Tanque / Sanador",
                                                NamedTextColor.DARK_GRAY));

                addClass(
                                15,
                                "Druida",
                                Material.OAK_SAPLING,

                                Component.text(
                                                "Guardián de la naturaleza",
                                                NamedTextColor.GRAY),

                                Component.text(
                                                "Armas: Bastones y formas bestiales",
                                                NamedTextColor.DARK_GRAY),

                                Component.text(
                                                "Rol: Versátil / Transformación",
                                                NamedTextColor.DARK_GRAY));

                // ===== Fila inferior =====

                for (int i = 18; i < 27; i++) {
                        setItem(i, ItemBuilder.createFiller());
                }

                setItem(22, ItemBuilder.createCancelButton("Volver atrás"));
        }

        private void addClass(
                        int slot,
                        String name,
                        Material icon,
                        Component... lore) {

                ItemStack item = new ItemBuilder(icon)
                                .setName(
                                                Component.text(name, NamedTextColor.GOLD)
                                                                .decorate(TextDecoration.BOLD))
                                .setLore(lore)
                                .build();

                setItem(slot, item);
                slotToClass.put(slot, name);
        }

        @Override
        public void handleClick(InventoryClickEvent event) {

                event.setCancelled(true);

                int slot = event.getRawSlot();

                // Cancelar
                if (slot == 22) {
                        close();

                        player.sendMessage(
                                        Component.text(
                                                        "Creación de personaje cancelada.",
                                                        NamedTextColor.YELLOW));

                        return;
                }

                // Selección de clase
                if (slotToClass.containsKey(slot)) {

                        String selectedClass = slotToClass.get(slot);

                        close();

                        onClassSelected.accept(selectedClass);
                }
        }
}