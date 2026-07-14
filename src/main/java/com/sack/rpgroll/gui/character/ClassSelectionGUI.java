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
 * GUI para seleccionar la clase del personaje.
 */
public class ClassSelectionGUI extends InventoryGUI {

    private final Consumer<String> onClassSelected;
    private final Map<Integer, String> slotToClass;
    private final String selectedRace;

    public ClassSelectionGUI(Player player, String selectedRace, Consumer<String> onClassSelected) {
        super(player, ChatColor.GOLD + "Selecciona tu Clase", 27); // 3 filas
        this.onClassSelected = onClassSelected;
        this.slotToClass = new HashMap<>();
        this.selectedRace = selectedRace;
    }

    @Override
    public void build() {
        clear();

        // Fila superior - información
        for (int i = 0; i < 9; i++) {
            setItem(i, ItemBuilder.createFiller());
        }
        setItem(4, new ItemBuilder(Material.PAPER)
                .setName("&e&lRaza seleccionada: &f" + selectedRace)
                .setLore("&7Ahora elige tu clase")
                .build());

        // Clases disponibles
        addClass(10, "Guerrero", Material.IRON_SWORD,
                "&7Maestro del combate cuerpo a cuerpo",
                "&8Armas: Espadas, Hachas, Escudos",
                "&8Rol: Tanque / DPS");

        addClass(11, "Mago", Material.ENCHANTED_BOOK,
                "&7Maestro de las artes arcanas",
                "&8Armas: Bastones, Libros",
                "&8Rol: DPS mágico / Control");

        addClass(12, "Pícaro", Material.IRON_SWORD,
                "&7Experto en sigilo y trampas",
                "&8Armas: Dagas, Arcos",
                "&8Rol: DPS / Sigilo");

        addClass(13, "Clérigo", Material.GOLDEN_APPLE,
                "&7Sanador y protector divino",
                "&8Armas: Mazas, Escudos",
                "&8Rol: Sanador / Soporte");

        addClass(14, "Paladín", Material.DIAMOND_SWORD,
                "&7Guerrero sagrado",
                "&8Armas: Espadas, Escudos",
                "&8Rol: Tanque / Sanador");

        addClass(15, "Druida", Material.OAK_SAPLING,
                "&7Guardián de la naturaleza",
                "&8Armas: Bastones, Formas bestiales",
                "&8Rol: Versátil / Transformación");

        // Fila inferior - botones
        for (int i = 18; i < 27; i++) {
            setItem(i, ItemBuilder.createFiller());
        }
        setItem(22, ItemBuilder.createCancelButton("Volver atrás"));
    }

    private void addClass(int slot, String name, Material icon, String... lore) {
        ItemStack item = new ItemBuilder(icon)
                .setName("&e&l" + name)
                .setLore(lore)
                .build();
        setItem(slot, item);
        slotToClass.put(slot, name);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);

        int slot = event.getRawSlot();

        // Volver atrás / Cancelar
        if (slot == 22) {
            close();
            player.sendMessage(ChatColor.YELLOW + "Creación de personaje cancelada.");
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
