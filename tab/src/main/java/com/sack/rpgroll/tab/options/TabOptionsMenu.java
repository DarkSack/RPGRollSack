package com.sack.rpgroll.tab.options;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.tab.tablist.TablistBlock;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Menú {@code /tab}: un botón por bloque de la cabecera y el pie del jugador. Clic = mostrar u ocultar ese bloque
 * solo para él; se guarda al momento y su TAB se repinta.
 */
public class TabOptionsMenu extends InventoryGUI {

    private static final int CLOSE_OFFSET = 4;

    private final List<TablistBlock> blocks;
    private final TabOptionsStore store;
    private final LangManager lang;
    private final Consumer<Player> repaint;

    public TabOptionsMenu(Player player, List<TablistBlock> blocks, TabOptionsStore store, LangManager lang,
            Consumer<Player> repaint) {
        super(player, lang.component("options.title"), size(blocks.size()));
        this.blocks = blocks;
        this.store = store;
        this.lang = lang;
        this.repaint = repaint;
    }

    /** Una fila de margen arriba y abajo; 7 botones por fila. */
    static int size(int blocks) {
        int rows = Math.max(1, (blocks + 6) / 7);
        return Math.min(6, rows + 2) * 9;
    }

    static int slot(int index) {
        return 9 * (1 + index / 7) + 1 + index % 7;
    }

    @Override
    public void build() {
        clear();
        int size = getInventory().getSize();
        for (int i = 0; i < size; i++) {
            setItem(i, ItemBuilder.createFiller());
        }
        BlockChoices choices = store.get(player);
        for (int i = 0; i < blocks.size() && slot(i) < size - 9; i++) {
            TablistBlock block = blocks.get(i);
            setItem(slot(i), button(block, choices.isShown(block.id(), block.shownByDefault())));
        }
        setItem(size - 9 + CLOSE_OFFSET, item(Material.BARRIER, lang.component("options.close"), List.of()));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int raw = event.getRawSlot();
        int size = getInventory().getSize();
        if (raw == size - 9 + CLOSE_OFFSET) {
            close();
            return;
        }
        for (int i = 0; i < blocks.size(); i++) {
            if (slot(i) == raw) {
                TablistBlock block = blocks.get(i);
                store.set(player, store.get(player).toggle(block.id(), block.shownByDefault()));
                repaint.accept(player);
                build();
                return;
            }
        }
    }

    private ItemStack button(TablistBlock block, boolean shown) {
        Material material = block.icon() == null ? null : Material.matchMaterial(block.icon());
        if (material == null || !material.isItem() || material.isAir()) {
            material = Material.PAPER;
        }
        ItemStack item = item(material, ComponentUtils.parse(name(block)),
                List.of(lang.component(shown ? "options.shown" : "options.hidden"),
                        lang.component(shown ? "options.click_hide" : "options.click_show")));
        if (shown) {
            item.editMeta(m -> m.setEnchantmentGlintOverride(true));
        }
        return item;
    }

    /** {@code options.blocks.<id>} del idioma, o el {@code name} del tablist, o el id con mayúscula. */
    private String name(TablistBlock block) {
        String key = "options.blocks." + block.id();
        String fromLang = lang.raw(key);
        if (!fromLang.equals(key)) {
            return fromLang;
        }
        if (block.name() != null) {
            return block.name();
        }
        String id = block.id().replace('_', ' ');
        return id.substring(0, 1).toUpperCase(Locale.ROOT) + id.substring(1);
    }

    private static ItemStack item(Material material, Component name, List<Component> lore) {
        return new ItemBuilder(material, 1)
                .setName(plain(name))
                .setLore(lore.stream().map(TabOptionsMenu::plain).toList())
                .build();
    }

    private static Component plain(Component c) {
        return c.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }
}
