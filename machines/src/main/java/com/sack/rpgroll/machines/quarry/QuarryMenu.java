package com.sack.rpgroll.machines.quarry;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.machines.core.Cost;
import com.sack.rpgroll.machines.core.Payment;
import com.sack.rpgroll.machines.core.Ui;
import com.sack.rpgroll.machines.quarry.QuarrySettings.Numeric;
import com.sack.rpgroll.machines.quarry.QuarrySettings.Track;
import com.sack.rpgroll.machines.quarry.QuarrySettings.Unlock;

import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Estado, mejoras e interruptor de una cantera. */
public class QuarryMenu extends InventoryGUI {

    private static final int STATUS = 4;
    private static final int TOGGLE = 31;
    private static final int[] NUMERIC_SLOTS = {19, 20, 21, 22};
    private static final int[] UNLOCK_SLOTS = {23, 24, 25};

    private final QuarryService service;
    private final LangManager lang;
    private final Quarry quarry;
    private final Map<Integer, Numeric> numericAt = new HashMap<>();
    private final Map<Integer, Unlock> unlockAt = new HashMap<>();

    public QuarryMenu(Player player, Quarry quarry, QuarryService service, LangManager lang) {
        super(player, lang.component("quarry.menu_title"), 36);
        this.service = service;
        this.lang = lang;
        this.quarry = quarry;
    }

    @Override
    public void build() {

        clear();
        numericAt.clear();
        unlockAt.clear();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            setItem(slot, Ui.filler());
        }
        QuarrySettings settings = service.settings();
        World world = Bukkit.getWorld(quarry.world());
        int minHeight = world == null ? -64 : world.getMinHeight();
        int side = service.side(quarry);

        List<Component> status = new ArrayList<>();
        status.add(lang.component("quarry.status_owner", "owner", quarry.ownerName()));
        status.add(lang.component("quarry.status_state", "state", lang.raw("quarry.state." + quarry.status().name().toLowerCase())));
        status.add(lang.component("quarry.status_depth", "y", Math.max(minHeight, quarry.cursorY())));
        status.add(lang.component("quarry.status_progress", "progress", Ui.percent(Math.floor(quarry.progress(side, minHeight) * 1000) / 1000)));
        status.add(lang.component("quarry.status_area", "side", side));
        if (!quarry.buffer().isEmpty()) {
            status.add(lang.component("quarry.status_buffer", "count", quarry.buffer().stream().mapToInt(i -> i.getAmount()).sum()));
        }
        setItem(STATUS, Ui.button(settings.block(), Ui.model(settings.itemModel()), lang.component("quarry.item_name"), status));

        int n = 0;
        for (Numeric numeric : Numeric.values()) {
            Track track = settings.track(numeric);
            if (track.max() <= 0 || n >= NUMERIC_SLOTS.length) {
                continue;
            }
            int slot = NUMERIC_SLOTS[n++];
            numericAt.put(slot, numeric);
            int level = quarry.level(numeric);
            List<Component> lore = new ArrayList<>();
            lore.add(lang.component("quarry.level", "level", Ui.roman(level), "max", Ui.roman(track.max())));
            lore.add(value(numeric, track.value(level)));
            if (numeric == Numeric.FORTUNE && quarry.active(Unlock.SILK_TOUCH)) {
                lore.add(lang.component("quarry.fortune_vs_silk"));
            }
            lore.add(Component.empty());
            if (level >= track.max()) {
                lore.add(lang.component("menu.maxed"));
            } else {
                lore.add(lang.component("quarry.next"));
                lore.add(value(numeric, track.value(level + 1)));
                lore.add(Component.empty());
                Cost cost = track.step(level + 1).cost();
                lore.addAll(Payment.lore(player, cost, lang));
                lore.add(Component.empty());
                lore.add(lang.component(Payment.has(player, cost) ? "menu.click_upgrade" : "menu.cant_afford"));
            }
            setItem(slot, Ui.button(track.icon(), lang.component("quarry.upgrade_title",
                    "upgrade", lang.raw("quarry.upgrade." + numeric.id())), lore));
        }

        int u = 0;
        for (Unlock unlock : Unlock.values()) {
            Cost cost = settings.unlocks().get(unlock);
            if (cost == null || u >= UNLOCK_SLOTS.length) {
                continue;
            }
            int slot = UNLOCK_SLOTS[u++];
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

        setItem(TOGGLE, Ui.button(quarry.enabled() ? Material.LIME_DYE : Material.GRAY_DYE,
                lang.component(quarry.enabled() ? "quarry.toggle_on" : "quarry.toggle_off"),
                List.of(lang.component("quarry.click_toggle"))));
    }

    private Component value(Numeric numeric, double value) {
        return switch (numeric) {
            case SPEED -> lang.component("quarry.value_speed", "value", Ui.number(value));
            case AREA -> lang.component("quarry.value_area", "value", (int) value);
            case FORTUNE -> lang.component("quarry.value_fortune", "value", Ui.roman((int) value));
            case TIER -> value < 0 ? lang.component("quarry.value_tier_default")
                    : lang.component("quarry.value_tier", "value", (int) value);
        };
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (service.store().at(quarry.key()).isEmpty()) {
            close();
            return;
        }
        if (slot == TOGGLE) {
            quarry.enabled(!quarry.enabled());
            service.store().dirty();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1f);
            build();
            return;
        }
        Numeric numeric = numericAt.get(slot);
        if (numeric != null) {
            upgrade(numeric);
            return;
        }
        Unlock unlock = unlockAt.get(slot);
        if (unlock != null) {
            if (quarry.unlocked(unlock)) {
                quarry.unlock(unlock, !quarry.active(unlock));
                service.store().dirty();
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1f);
            } else if (pay(service.settings().unlocks().get(unlock))) {
                quarry.unlock(unlock, true);
                service.store().dirty();
                lang.send(player, "quarry.unlocked", "upgrade", lang.raw("quarry.unlock." + unlock.id()));
            }
            build();
        }
    }

    private void upgrade(Numeric numeric) {

        Track track = service.settings().track(numeric);
        int level = quarry.level(numeric);
        if (level >= track.max()) {
            return;
        }
        if (numeric == Numeric.AREA) {
            World world = Bukkit.getWorld(quarry.world());
            int side = (int) track.value(level + 1);
            if (world == null || !service.areaAllowed(quarry, world, side)) {
                lang.send(player, "quarry.need_claim", "side", side);
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
        }
        if (!pay(track.step(level + 1).cost())) {
            build();
            return;
        }
        quarry.level(numeric, level + 1);
        if (numeric == Numeric.AREA || numeric == Numeric.TIER) {
            // Las columnas nuevas están enteras, y las menas que no podía picar siguen ahí: se
            // vuelve a empezar desde arriba (lo ya excavado es aire y se recorre rápido).
            quarry.restart();
        }
        service.store().dirty();
        lang.send(player, "quarry.upgraded", "upgrade", lang.raw("quarry.upgrade." + numeric.id()),
                "level", Ui.roman(level + 1));
        build();
    }

    private boolean pay(Cost cost) {
        if (Payment.take(player, cost)) {
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 1.2f);
            return true;
        }
        lang.send(player, "menu.cant_afford_message");
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        return false;
    }
}
