package com.sack.rpgroll.extras.lock;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Teclado de 4 dígitos, como el de un teléfono. Al cuarto dígito se cierra y entrega el código. */
final class KeypadGUI extends InventoryGUI {

    private static final Map<Integer, Character> DIGITS = Map.of(12, '1', 13, '2', 14, '3', 21, '4', 22, '5',
            23, '6', 30, '7', 31, '8', 32, '9', 40, '0');
    private static final int DISPLAY = 4;
    private static final int CLEAR = 39;
    private static final int CLOSE = 41;

    private final LangManager lang;
    private final boolean masked;
    private final Consumer<String> onCode;
    private final StringBuilder typed = new StringBuilder();

    /** @param masked true al abrir (el código no se ve); false al elegir uno nuevo. */
    KeypadGUI(Player player, LangManager lang, boolean masked, Consumer<String> onCode) {
        super(player, lang.component(masked ? "lock.keypad_open" : "lock.keypad_set"), 45);
        this.lang = lang;
        this.masked = masked;
        this.onCode = onCode;
    }

    private static ItemStack named(Material material, Component name, List<Component> lore) {
        ItemStack item = ItemStack.of(material);
        var meta = item.getItemMeta();
        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        meta.lore(lore.stream().map(c -> c.decoration(TextDecoration.ITALIC, false)).toList());
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void build() {
        clear();
        String shown = masked ? "●".repeat(typed.length()) : typed.toString();
        shown += "_".repeat(4 - typed.length());
        setItem(DISPLAY, named(Material.NAME_TAG, lang.component("lock.display", "code", shown), List.of()));
        DIGITS.forEach((slot, digit) -> setItem(slot,
                named(Material.PAPER, lang.component("lock.digit", "digit", digit), List.of())));
        setItem(CLEAR, named(Material.RED_DYE, lang.component("lock.clear"), List.of()));
        setItem(CLOSE, named(Material.BARRIER, lang.component("lock.cancel"), List.of()));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot == CLOSE) {
            close();
            return;
        }
        if (slot == CLEAR) {
            typed.setLength(0);
            build();
            return;
        }
        Character digit = DIGITS.get(slot);
        if (digit == null) {
            return;
        }
        typed.append(digit);
        player.playSound(player, Sound.BLOCK_STONE_BUTTON_CLICK_ON, 0.5f, 1.6f);
        if (typed.length() < 4) {
            build();
            return;
        }
        String code = typed.toString();
        close();
        onCode.accept(code);
    }

    @Override
    public void handlePlayerInventoryClick(InventoryClickEvent event) {
        event.setCancelled(true);
    }
}
