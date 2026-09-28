package com.sack.rpgroll.ranching.gui;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/** "¿Seguro?" antes de gastar o cobrar dinero: el ítem del centro dice qué se confirma. */
public class ConfirmGUI extends InventoryGUI {

    private static final int CONFIRM = 11;
    private static final int SUBJECT = 13;
    private static final int CANCEL = 15;

    private final LangManager lang;
    private final ItemStack subject;
    private final Runnable onConfirm;
    private final Runnable onCancel;

    public ConfirmGUI(Player player, LangManager lang, ItemStack subject, Runnable onConfirm, Runnable onCancel) {
        super(player, lang.component("gui.confirm.title"), 27);
        this.lang = lang;
        this.subject = subject;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < 27; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(CONFIRM, ItemBuilder.createConfirmButton(lang.raw("gui.confirm.accept")));
        setItem(SUBJECT, subject);
        setItem(CANCEL, ItemBuilder.createCancelButton(lang.raw("gui.confirm.decline")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);

        if (event.getRawSlot() == CONFIRM) {
            close();
            onConfirm.run();
        } else if (event.getRawSlot() == CANCEL) {
            if (onCancel != null) {
                onCancel.run();
            } else {
                close();
            }
        }
    }

}
