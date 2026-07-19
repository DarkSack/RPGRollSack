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
 * GUI para seleccionar la raza del personaje.
 */
public class RaceSelectionGUI extends InventoryGUI {

    private final Consumer<String> onRaceSelected;
    private final Map<Integer, String> slotToRace;

    public RaceSelectionGUI(Player player, Consumer<String> onRaceSelected) {
        super(
                player,
                Component.text("Selecciona tu Raza", NamedTextColor.GOLD)
                        .decorate(TextDecoration.BOLD),
                27);

        this.onRaceSelected = onRaceSelected;
        this.slotToRace = new HashMap<>();
    }

    @Override
    public void build() {

        clear();

        // ===== Fila superior =====

        for (int i = 0; i < 9; i++) {
            setItem(i, ItemBuilder.createFiller());
        }

        // ===== Razas =====

        addRace(
                10,
                "Humano",
                Material.IRON_SWORD,

                Component.text("Versátiles y adaptables", NamedTextColor.GRAY),
                Component.text("+1 a todas las estadísticas", NamedTextColor.DARK_GRAY),
                Component.text("Bonificación: Talento extra", NamedTextColor.DARK_GRAY));

        addRace(
                11,
                "Elfo",
                Material.BOW,

                Component.text("Ágiles y sabios", NamedTextColor.GRAY),
                Component.text("+2 Destreza, +1 Sabiduría", NamedTextColor.DARK_GRAY),
                Component.text("Bonificación: Visión en la oscuridad", NamedTextColor.DARK_GRAY));

        addRace(
                12,
                "Enano",
                Material.IRON_PICKAXE,

                Component.text("Resistentes y fuertes", NamedTextColor.GRAY),
                Component.text("+2 Constitución, +1 Fuerza", NamedTextColor.DARK_GRAY),
                Component.text("Bonificación: Resistencia al veneno", NamedTextColor.DARK_GRAY));

        addRace(
                13,
                "Orco",
                Material.IRON_AXE,

                Component.text("Poderosos y salvajes", NamedTextColor.GRAY),
                Component.text("+2 Fuerza, +1 Constitución", NamedTextColor.DARK_GRAY),
                Component.text("Bonificación: Ataque feroz", NamedTextColor.DARK_GRAY));

        addRace(
                14,
                "Halfling",
                Material.BREAD,

                Component.text("Astutos y afortunados", NamedTextColor.GRAY),
                Component.text("+2 Destreza, +1 Carisma", NamedTextColor.DARK_GRAY),
                Component.text("Bonificación: Suerte", NamedTextColor.DARK_GRAY));

        addRace(
                15,
                "Tiefling",
                Material.BLAZE_POWDER,

                Component.text("Carismáticos y místicos", NamedTextColor.GRAY),
                Component.text("+2 Carisma, +1 Inteligencia", NamedTextColor.DARK_GRAY),
                Component.text("Bonificación: Resistencia al fuego", NamedTextColor.DARK_GRAY));

        addRace(
                16,
                "Dracónido",
                Material.DRAGON_HEAD,

                Component.text("Honorables y fuertes", NamedTextColor.GRAY),
                Component.text("+2 Fuerza, +1 Carisma", NamedTextColor.DARK_GRAY),
                Component.text("Bonificación: Aliento de dragón", NamedTextColor.DARK_GRAY));

        // ===== Botón cancelar =====

        for (int i = 18; i < 27; i++) {
            setItem(i, ItemBuilder.createFiller());
        }

        setItem(22, ItemBuilder.createCancelButton("Cancelar"));
    }

    private void addRace(
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
        slotToRace.put(slot, name);
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

        // Selección de raza
        if (slotToRace.containsKey(slot)) {

            String selectedRace = slotToRace.get(slot);

            close();

            onRaceSelected.accept(selectedRace);
        }
    }

}