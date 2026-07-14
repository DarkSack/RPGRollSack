package com.sack.rpgroll.gui.character;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import org.bukkit.ChatColor;
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
        super(player, ChatColor.GOLD + "Selecciona tu Raza", 27); // 3 filas
        this.onRaceSelected = onRaceSelected;
        this.slotToRace = new HashMap<>();
    }

    @Override
    public void build() {
        clear();

        // Fila superior - título
        for (int i = 0; i < 9; i++) {
            setItem(i, ItemBuilder.createFiller());
        }

        // Razas disponibles
        addRace(10, "Humano", Material.IRON_SWORD,
                "&7Versátiles y adaptables",
                "&8+1 a todas las estadísticas",
                "&8Bonificación: Talento extra");

        addRace(11, "Elfo", Material.BOW,
                "&7Ágiles y sabios",
                "&8+2 Destreza, +1 Sabiduría",
                "&8Bonificación: Visión en la oscuridad");

        addRace(12, "Enano", Material.IRON_PICKAXE,
                "&7Resistentes y fuertes",
                "&8+2 Constitución, +1 Fuerza",
                "&8Bonificación: Resistencia al veneno");

        addRace(13, "Orco", Material.IRON_AXE,
                "&7Poderosos y salvajes",
                "&8+2 Fuerza, +1 Constitución",
                "&8Bonificación: Ataque feroz");

        addRace(14, "Halfling", Material.BREAD,
                "&7Astutos y afortunados",
                "&8+2 Destreza, +1 Carisma",
                "&8Bonificación: Suerte");

        addRace(15, "Tiefling", Material.BLAZE_POWDER,
                "&7Carismáticos y místicos",
                "&8+2 Carisma, +1 Inteligencia",
                "&8Bonificación: Resistencia al fuego");

        addRace(16, "Dracónido", Material.DRAGON_HEAD,
                "&7Honorables y fuertes",
                "&8+2 Fuerza, +1 Carisma",
                "&8Bonificación: Aliento de dragón");

        // Fila inferior - cancelar
        for (int i = 18; i < 27; i++) {
            setItem(i, ItemBuilder.createFiller());
        }
        setItem(22, ItemBuilder.createCancelButton("Cancelar"));
    }

    private void addRace(int slot, String name, Material icon, String... lore) {
        ItemStack item = new ItemBuilder(icon)
                .setName("&e&l" + name)
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
            player.sendMessage(ChatColor.YELLOW + "Creación de personaje cancelada.");
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
