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
 * NOTA: las clases siguen hardcodeadas — pendiente crear ClassManager
 * análogo a RaceManager para leerlas desde classes/*.yml.
 */
public class ClassSelectionGUI extends InventoryGUI {

        private static final int CANCEL_SLOT = 22;

        // Estos son placeholders y no van a mostrar textura real hasta cambiarlos.
        private static final String SKULL_GUERRERO = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTJlOGE0ZjYxMjU1N2Y2MzU3MTQ0ODEyNTlkODA2M2MxM2VlNmM2ZWEzN2VlYThiMWNiZTlkNzE3ZDA1ZDRhYiJ9fX0=";
        private static final String SKULL_MAGO = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNzBmYmI5MTc4ZDlkNDY4ZTNjOTczNWVlNTcxYjdiYWE1NDMzOGY3ZjgzMWY3ZjRlNjBjYTljOGQxNDg3MGM3In19fQ==";
        private static final String SKULL_PICARO = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZTA3MDMxOTU2ZmFlYWM0NjA0NWM1Zjg1YTJjMjQ2YmRlM2NhMzg0MWJjNzU4NGMwNTRiODAxNzFiNGZmMWZlOCJ9fX0=";
        private static final String SKULL_CLERIGO = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYjViMDAzZWNlYzEwNmY2NmI0ZTYyZTBmZWFlMDFjZWVhYTBjYzM1YzQzOTlhY2U0YTJlNGZhMDMyMmQ1NzQ2MCJ9fX0=";
        private static final String SKULL_PALADIN = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNjBmZDJjYzkxMTZjMGY3MjRiZGNkYmYyZTk2MzNjYjBhN2Q0NTNmOGIzYTFhZDlkMTQ5M2U1ZTZmMTI4MTU1NSJ9fX0=";
        private static final String SKULL_DRUIDA = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2RiZGQwYTIxMjA2NGYwMzkyNjdlYzNiYTZlMzQ1YmRkMDZlYjM2ZDkyNGQ4ZmExNjU4NjE2MWYxZThjMTlmZiJ9fX0=";

        private final Consumer<String> onClassSelected;
        private final Map<Integer, String> slotToClass;
        private final String selectedRace;
        private final boolean mandatory;

        public ClassSelectionGUI(Player player, String selectedRace, Consumer<String> onClassSelected,
                        boolean mandatory) {
                super(player, Component.text("Selecciona tu Clase", NamedTextColor.GOLD).decorate(TextDecoration.BOLD),
                                27);
                this.onClassSelected = onClassSelected;
                this.slotToClass = new HashMap<>();
                this.selectedRace = selectedRace;
                this.mandatory = mandatory;
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
                                .setName(Component.text("Raza seleccionada", NamedTextColor.GOLD)
                                                .decorate(TextDecoration.BOLD))
                                .setLore(
                                                Component.text(selectedRace, NamedTextColor.YELLOW),
                                                Component.text("Ahora elige tu clase.", NamedTextColor.GRAY))
                                .build());

                addClass(10, "Guerrero", SKULL_GUERRERO,
                                Component.text("Maestro del combate cuerpo a cuerpo", NamedTextColor.GRAY),
                                Component.text("Armas: Espadas, Hachas y Escudos", NamedTextColor.DARK_GRAY),
                                Component.text("Rol: Tanque / DPS", NamedTextColor.DARK_GRAY));

                addClass(11, "Mago", SKULL_MAGO,
                                Component.text("Maestro de las artes arcanas", NamedTextColor.GRAY),
                                Component.text("Armas: Bastones y Libros", NamedTextColor.DARK_GRAY),
                                Component.text("Rol: DPS mágico / Control", NamedTextColor.DARK_GRAY));

                addClass(12, "Pícaro", SKULL_PICARO,
                                Component.text("Experto en sigilo y trampas", NamedTextColor.GRAY),
                                Component.text("Armas: Dagas y Arcos", NamedTextColor.DARK_GRAY),
                                Component.text("Rol: DPS / Sigilo", NamedTextColor.DARK_GRAY));

                addClass(13, "Clérigo", SKULL_CLERIGO,
                                Component.text("Sanador y protector divino", NamedTextColor.GRAY),
                                Component.text("Armas: Mazas y Escudos", NamedTextColor.DARK_GRAY),
                                Component.text("Rol: Sanador / Soporte", NamedTextColor.DARK_GRAY));

                addClass(14, "Paladín", SKULL_PALADIN,
                                Component.text("Guerrero sagrado", NamedTextColor.GRAY),
                                Component.text("Armas: Espadas y Escudos", NamedTextColor.DARK_GRAY),
                                Component.text("Rol: Tanque / Sanador", NamedTextColor.DARK_GRAY));

                addClass(15, "Druida", SKULL_DRUIDA,
                                Component.text("Guardián de la naturaleza", NamedTextColor.GRAY),
                                Component.text("Armas: Bastones y formas bestiales", NamedTextColor.DARK_GRAY),
                                Component.text("Rol: Versátil / Transformación", NamedTextColor.DARK_GRAY));

                for (int i = 18; i < 27; i++) {
                        setItem(i, ItemBuilder.createFiller());
                }

                if (!mandatory) {
                        setItem(CANCEL_SLOT, ItemBuilder.createCancelButton("Volver atrás"));
                }
        }

        private void addClass(int slot, String name, String skullTexture, Component... lore) {

                ItemStack item = ItemBuilder.skull(skullTexture)
                                .setName(Component.text(name, NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
                                .setLore(lore)
                                .build();

                setItem(slot, item);
                slotToClass.put(slot, name);
        }

        @Override
        public void handleClick(InventoryClickEvent event) {

                event.setCancelled(true);

                int slot = event.getRawSlot();

                if (!mandatory && slot == CANCEL_SLOT) {
                        close();
                        player.sendMessage(Component.text("Creación de personaje cancelada.", NamedTextColor.YELLOW));
                        return;
                }

                if (slotToClass.containsKey(slot)) {
                        String selectedClass = slotToClass.get(slot);
                        markSelectionMade();
                        close();
                        onClassSelected.accept(selectedClass);
                }
        }

}