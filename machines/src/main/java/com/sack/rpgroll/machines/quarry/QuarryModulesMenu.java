package com.sack.rpgroll.machines.quarry;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.MenuBackground;
import com.sack.rpgroll.machines.core.Cost;
import com.sack.rpgroll.machines.core.Payment;
import com.sack.rpgroll.machines.core.Ui;
import com.sack.rpgroll.machines.quarry.QuarrySettings.Unlock;

import net.kyori.adventure.text.Component;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Mejoras de una vez de una cantera (toque de seda, autofundido, filtro): comprar y encender/apagar. */
public class QuarryModulesMenu extends InventoryGUI {

    private static final int BACK = 8;
    private static final int[] SLOTS = {20, 22, 24};

    private final QuarryService service;
    private final LangManager lang;
    private final Quarry quarry;
    private final Map<Integer, Unlock> unlockAt = new HashMap<>();

    public QuarryModulesMenu(Player player, Quarry quarry, QuarryService service, LangManager lang) {
        super(player, MenuBackground.title(service.settings().menu().background("cantera_modulos"),
                lang.component("quarry.modules_title")), 36);
        this.service = service;
        this.lang = lang;
        this.quarry = quarry;
    }

    @Override
    public void build() {

        clear();
        unlockAt.clear();
        QuarrySettings settings = service.settings();
        QuarrySettings.Menu menu = settings.menu();
        if (!menu.drawn()) {
            QuarryMenu.fill(this::setItem, inventory.getSize());
        }
        setItem(BACK, QuarryMenu.back(menu, lang));

        int u = 0;
        for (Unlock unlock : Unlock.values()) {
            Cost cost = settings.unlocks().get(unlock);
            if (cost == null || u >= SLOTS.length) {
                continue;
            }
            int slot = SLOTS[u++];
            unlockAt.put(slot, unlock);
            List<Component> lore = new ArrayList<>();
            lore.add(lang.component("quarry.unlock_desc." + unlock.id()));
            lore.add(Component.empty());
            if (quarry.unlocked(unlock)) {
                lore.add(lang.component(quarry.active(unlock) ? "quarry.on" : "quarry.off"));
                lore.add(lang.component("quarry.click_toggle"));
            } else {
                lore.addAll(Payment.lore(player, cost, lang));
                lore.add(Component.empty());
                lore.add(lang.component(Payment.has(player, cost) ? "quarry.click_buy" : "menu.cant_afford"));
            }
            setItem(slot, Ui.button(unlock.icon(), lang.component("quarry.upgrade_title",
                    "upgrade", lang.raw("quarry.unlock." + unlock.id())), lore));
        }
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (service.store().at(quarry.key()).isEmpty()) {
            close();
            return;
        }
        if (slot == BACK) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1f);
            new QuarryMenu(player, quarry, service, lang).open();
            return;
        }
        Unlock unlock = unlockAt.get(slot);
        if (unlock == null) {
            return;
        }
        if (quarry.unlocked(unlock)) {
            quarry.unlock(unlock, !quarry.active(unlock));
            service.store().dirty();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1f);
        } else if (QuarryMenu.pay(player, service.settings().unlocks().get(unlock), lang)) {
            quarry.unlock(unlock, true);
            service.store().dirty();
            lang.send(player, "quarry.unlocked", "upgrade", lang.raw("quarry.unlock." + unlock.id()));
        }
        build();
    }
}
