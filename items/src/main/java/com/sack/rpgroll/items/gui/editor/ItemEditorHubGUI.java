package com.sack.rpgroll.items.gui.editor;

import com.sack.rpgroll.common.lang.LangManager;
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
    private final LangManager lang;

    public ItemEditorHubGUI(Player player, EditorSession session) {
        super(player, session.chatPromptManager.lang().component("editor.hub.title", "id", session.original.id()), SIZE);
        this.session = session;
        this.lang = session.chatPromptManager.lang();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        fillSectionRow(9, IDENTITY_GLASS, lang.raw("editor.hub.identity"));
        fillSectionRow(18, CONTENT_GLASS, lang.raw("editor.hub.content_magic"));
        fillSectionRow(27, BEHAVIOR_GLASS, lang.raw("editor.hub.behavior"));
        fillSectionRow(36, EXTRA_GLASS, lang.raw("editor.hub.extra"));

        for (int slot = 45; slot < SIZE; slot++) {
            setItem(slot, glass(Material.BLACK_STAINED_GLASS_PANE, " "));
        }

        setItem(PREVIEW_SLOT, session.preview());

        setItem(DISPLAY_SLOT, categoryButton(Material.NAME_TAG, lang.raw("editor.hub.display_name"),
                lang.raw("editor.hub.display_desc")));
        setItem(STATS_SLOT, categoryButton(Material.REDSTONE, lang.raw("editor.hub.stats_name"),
                lang.raw("editor.hub.stats_desc")));
        setItem(RULES_SLOT, categoryButton(Material.BOOK, lang.raw("editor.hub.rules_name"),
                lang.raw("editor.hub.rules_desc")));

        setItem(ENCHANTMENTS_SLOT, categoryButton(Material.ENCHANTED_BOOK, lang.raw("editor.hub.enchantments_name"),
                lang.raw("editor.hub.enchantments_desc")));
        setItem(EFFECTS_SLOT, categoryButton(Material.POTION, lang.raw("editor.hub.effects_name"),
                lang.raw("editor.hub.effects_desc")));
        setItem(SOCKETS_SLOT, categoryButton(Material.AMETHYST_SHARD, lang.raw("editor.hub.sockets_name"),
                lang.raw("editor.hub.sockets_desc")));
        setItem(SKINS_SLOT, categoryButton(Material.ARMOR_STAND, lang.raw("editor.hub.skins_name"),
                lang.raw("editor.hub.skins_desc")));

        setItem(UPGRADES_SLOT, categoryButton(Material.ANVIL, lang.raw("editor.hub.upgrades_name"),
                lang.raw("editor.hub.upgrades_desc")));
        setItem(TRIGGERS_SLOT, categoryButton(Material.COMPARATOR, lang.raw("editor.hub.triggers_name"),
                lang.raw("editor.hub.triggers_desc")));
        setItem(ABILITIES_SLOT, categoryButton(Material.BLAZE_POWDER, lang.raw("editor.hub.abilities_name"),
                lang.raw("editor.hub.abilities_desc")));

        setItem(RECIPES_SLOT, categoryButton(Material.CRAFTING_TABLE, lang.raw("editor.hub.recipes_name"),
                lang.raw("editor.hub.recipes_desc")));
        setItem(CUSTOM_DATA_SLOT, categoryButton(Material.PAPER, lang.raw("editor.hub.custom_data_name"),
                lang.raw("editor.hub.custom_data_desc")));

        setItem(SAVE_SLOT, ItemBuilder.createConfirmButton(lang.raw("editor.hub.save")));
        setItem(CANCEL_SLOT, ItemBuilder.createCancelButton(lang.raw("editor.hub.cancel")));
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
            lang.send(player, "editor.hub.saved");
        } catch (IOException e) {
            lang.send(player, "editor.hub.save_error", "error", e.getMessage());
        }

        markSelectionMade();
        close();
    }

}
