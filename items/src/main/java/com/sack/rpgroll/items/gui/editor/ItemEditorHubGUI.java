package com.sack.rpgroll.items.gui.editor;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.io.IOException;

/**
 * Punto de entrada del editor gráfico completo: una vista previa en vivo
 * del ítem en edición, y un botón por cada componente — agrupados en 4
 * secciones con su propio color de vidrio (identidad, contenido mágico,
 * comportamiento, extra) en vez de un solo bloque de 12 botones sueltos.
 * Cada sub-editor comparte la misma {@link EditorSession} — los cambios se
 * acumulan hasta que se presiona Guardar, sin importar cuántas veces se
 * navegue entre pantallas.
 */
public class ItemEditorHubGUI extends InventoryGUI {

    private static final int SIZE = 54;
    private static final int PREVIEW_SLOT = 4;

    // Identidad (amarillo): qué ES el ítem.
    private static final Material IDENTITY_GLASS = Material.YELLOW_STAINED_GLASS_PANE;
    private static final int DISPLAY_SLOT = 10;
    private static final int STATS_SLOT = 11;
    private static final int RULES_SLOT = 12;

    // Contenido mágico (púrpura): encantamientos, efectos, sockets, skins.
    private static final Material CONTENT_GLASS = Material.PURPLE_STAINED_GLASS_PANE;
    private static final int ENCHANTMENTS_SLOT = 19;
    private static final int EFFECTS_SLOT = 20;
    private static final int SOCKETS_SLOT = 21;
    private static final int SKINS_SLOT = 22;

    // Comportamiento (rojo): mejoras, triggers, habilidades.
    private static final Material BEHAVIOR_GLASS = Material.RED_STAINED_GLASS_PANE;
    private static final int UPGRADES_SLOT = 28;
    private static final int TRIGGERS_SLOT = 29;
    private static final int ABILITIES_SLOT = 30;

    // Extra (celeste): recetas, datos custom.
    private static final Material EXTRA_GLASS = Material.LIGHT_BLUE_STAINED_GLASS_PANE;
    private static final int RECIPES_SLOT = 37;
    private static final int CUSTOM_DATA_SLOT = 38;

    private static final int SAVE_SLOT = 48;
    private static final int CANCEL_SLOT = 50;

    private final EditorSession session;

    public ItemEditorHubGUI(Player player, EditorSession session) {
        super(player, Component.text("Editando: " + session.original.id(), NamedTextColor.GOLD), SIZE);
        this.session = session;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        fillSectionRow(9, IDENTITY_GLASS, "Identidad");
        fillSectionRow(18, CONTENT_GLASS, "Contenido mágico");
        fillSectionRow(27, BEHAVIOR_GLASS, "Comportamiento");
        fillSectionRow(36, EXTRA_GLASS, "Extra");

        for (int slot = 45; slot < SIZE; slot++) {
            setItem(slot, glass(Material.BLACK_STAINED_GLASS_PANE, " "));
        }

        setItem(PREVIEW_SLOT, session.preview());

        setItem(DISPLAY_SLOT, categoryButton(Material.NAME_TAG, "Apariencia",
                "Material, nombre, lore, rareza, brillo, flags"));
        setItem(STATS_SLOT, categoryButton(Material.REDSTONE, "Stats y Atributos",
                "Estadísticas RPGRoll + atributos vanilla"));
        setItem(RULES_SLOT, categoryButton(Material.BOOK, "Reglas",
                "Requisitos, durabilidad y economía"));

        setItem(ENCHANTMENTS_SLOT, categoryButton(Material.ENCHANTED_BOOK, "Encantamientos",
                "Vanilla + RPGRoll-Enchantments"));
        setItem(EFFECTS_SLOT, categoryButton(Material.POTION, "Efectos",
                "Efectos de poción mientras está equipado"));
        setItem(SOCKETS_SLOT, categoryButton(Material.AMETHYST_SHARD, "Sockets",
                "Ranuras para gemas"));
        setItem(SKINS_SLOT, categoryButton(Material.ARMOR_STAND, "Skins",
                "Apariencias alternativas"));

        setItem(UPGRADES_SLOT, categoryButton(Material.ANVIL, "Mejoras",
                "Niveles de mejora (+1, +2, ...)"));
        setItem(TRIGGERS_SLOT, categoryButton(Material.COMPARATOR, "Comportamiento",
                "Acciones por trigger (click, ataque, romper bloque, etc.)"));
        setItem(ABILITIES_SLOT, categoryButton(Material.BLAZE_POWDER, "Habilidades",
                "Habilidades activas y pasivas"));

        setItem(RECIPES_SLOT, categoryButton(Material.CRAFTING_TABLE, "Recetas",
                "Cómo se craftea este ítem"));
        setItem(CUSTOM_DATA_SLOT, categoryButton(Material.PAPER, "Datos custom",
                "Pares clave-valor libres para otros addons"));

        setItem(SAVE_SLOT, ItemBuilder.createConfirmButton("Guardar"));
        setItem(CANCEL_SLOT, ItemBuilder.createCancelButton("Cancelar"));
    }

    /** Una fila completa (9 slots) pintada con el vidrio de la sección, para que los botones se vean "insertados". */
    private void fillSectionRow(int rowStart, Material sectionGlass, String label) {
        for (int i = 0; i < 9; i++) {
            setItem(rowStart + i, glass(sectionGlass, label));
        }
    }

    private ItemStack glass(Material material, String label) {
        return new ItemBuilder(material)
                .setName(Component.text(label, NamedTextColor.GRAY))
                .build();
    }

    private ItemStack categoryButton(Material material, String name, String description) {
        return new ItemBuilder(material)
                .setName(Component.text(name, NamedTextColor.YELLOW))
                .setLore(Component.text(description, NamedTextColor.GRAY))
                .build();
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);

        switch (event.getSlot()) {
            case DISPLAY_SLOT -> new DisplayEditorGUI(player, session, this::reopen).open();
            case STATS_SLOT -> new StatsEditorGUI(player, session, this::reopen).open();
            case RULES_SLOT -> new RulesEditorGUI(player, session, this::reopen).open();
            case ENCHANTMENTS_SLOT -> new EnchantmentsEditorGUI(player, session, this::reopen).open();
            case EFFECTS_SLOT -> new EffectsEditorGUI(player, session, this::reopen).open();
            case SOCKETS_SLOT -> new SocketsEditorGUI(player, session, this::reopen).open();
            case SKINS_SLOT -> new SkinsEditorGUI(player, session, this::reopen).open();
            case UPGRADES_SLOT -> new UpgradesEditorGUI(player, session, this::reopen).open();
            case TRIGGERS_SLOT -> new TriggersEditorGUI(player, session, this::reopen).open();
            case ABILITIES_SLOT -> new AbilitiesEditorGUI(player, session, this::reopen).open();
            case RECIPES_SLOT -> new RecipesEditorGUI(player, session, this::reopen).open();
            case CUSTOM_DATA_SLOT -> new CustomDataEditorGUI(player, session, this::reopen).open();
            case SAVE_SLOT -> save();
            case CANCEL_SLOT -> close();
            default -> {
            }
        }
    }

    private void reopen() {
        new ItemEditorHubGUI(player, session).open();
    }

    private void save() {

        try {
            session.save();
            player.sendMessage(Component.text("✔ Ítem guardado.", NamedTextColor.GREEN));
        } catch (IOException e) {
            player.sendMessage(Component.text("✘ Error guardando el ítem: " + e.getMessage(), NamedTextColor.RED));
        }

        markSelectionMade();
        close();
    }

}
