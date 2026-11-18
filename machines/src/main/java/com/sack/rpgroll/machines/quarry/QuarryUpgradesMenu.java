package com.sack.rpgroll.machines.quarry;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.MenuBackground;
import com.sack.rpgroll.machines.core.Cost;
import com.sack.rpgroll.machines.core.Payment;
import com.sack.rpgroll.machines.core.Ui;
import com.sack.rpgroll.machines.quarry.QuarrySettings.Numeric;
import com.sack.rpgroll.machines.quarry.QuarrySettings.Track;
import com.sack.rpgroll.machines.quarry.QuarrySettings.Unlock;

import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mejoras con niveles de una cantera: una fila por mejora, con su icono y los niveles en fila (el
 * siguiente se compra con un clic).
 */
public class QuarryUpgradesMenu extends InventoryGUI {

    /** Estado de un nivel en la fila. */
    public enum Tier { OWNED, NEXT, LOCKED, NONE }

    private static final int BACK = 8;
    // shortcut: caben las 4 mejoras con niveles (velocidad, área, fortuna, nivel); una quinta pide ampliar el dibujo.
    private static final int ROWS = 4;
    private static final int TIERS = 5;
    private static final java.util.Set<Numeric> WARNED = java.util.EnumSet.noneOf(Numeric.class);

    private final QuarryService service;
    private final LangManager lang;
    private final Quarry quarry;
    private final Map<Integer, Numeric> numericAt = new HashMap<>();

    public QuarryUpgradesMenu(Player player, Quarry quarry, QuarryService service, LangManager lang) {
        super(player, MenuBackground.title(service.settings().menu().background("cantera_mejoras"),
                lang.component("quarry.upgrades_title")), 9 * (ROWS + 1));
        this.service = service;
        this.lang = lang;
        this.quarry = quarry;
    }

    /** Nivel {@code i} (1..) de una mejora que va por el nivel {@code current} y llega hasta {@code max}. */
    public static Tier tier(int current, int i, int max) {
        if (i > max) {
            return Tier.NONE;
        }
        if (i <= current) {
            return Tier.OWNED;
        }
        return i == current + 1 ? Tier.NEXT : Tier.LOCKED;
    }

    /** El icono de la mejora compra solo los niveles que ya no caben en la fila (del 6 en adelante). */
    public static boolean iconBuys(int current, int max) {
        return current < max && current + 1 > TIERS;
    }

    @Override
    public void build() {

        clear();
        numericAt.clear();
        QuarrySettings settings = service.settings();
        QuarrySettings.Menu menu = settings.menu();
        if (!menu.drawn()) {
            QuarryMenu.fill(this::setItem, inventory.getSize());
        }
        setItem(BACK, QuarryMenu.back(menu, lang));

        int row = 1;
        for (Numeric numeric : Numeric.values()) {
            Track track = settings.track(numeric);
            if (track.max() <= 0) {
                continue;
            }
            if (row > ROWS) {
                if (WARNED.add(numeric)) {
                    Bukkit.getLogger().warning("✘ quarries.yml: el menú de mejoras de la cantera muestra " + ROWS + " mejoras; '"
                            + numeric.id() + "' no cabe y no se puede comprar desde el menú.");
                }
                continue;
            }
            int level = quarry.level(numeric);
            int first = row * 9;
            List<Component> lore = new ArrayList<>();
            lore.add(lang.component("quarry.level", "level", Ui.roman(level), "max", Ui.roman(track.max())));
            lore.add(value(numeric, track.value(level)));
            if (numeric == Numeric.FORTUNE && quarry.active(Unlock.SILK_TOUCH)) {
                lore.add(lang.component("quarry.fortune_vs_silk"));
            }
            if (iconBuys(level, track.max())) {
                numericAt.put(first + 1, numeric);
                Cost cost = track.step(level + 1).cost();
                lore.add(Component.empty());
                lore.add(value(numeric, track.value(level + 1)));
                lore.addAll(Payment.lore(player, cost, lang));
                lore.add(lang.component(Payment.has(player, cost) ? "menu.click_upgrade" : "menu.cant_afford"));
            }
            setItem(first + 1, Ui.button(track.icon(), lang.component("quarry.upgrade_title",
                    "upgrade", lang.raw("quarry.upgrade." + numeric.id())), lore));

            for (int i = 1; i <= TIERS; i++) {
                Tier state = tier(level, i, track.max());
                if (state == Tier.NONE) {
                    continue;
                }
                int slot = first + 2 + i;
                if (state == Tier.NEXT) {
                    numericAt.put(slot, numeric);
                }
                setItem(slot, tierItem(menu, numeric, track, i, state));
            }
            row++;
        }
    }

    private ItemStack tierItem(QuarrySettings.Menu menu, Numeric numeric, Track track, int i, Tier state) {
        List<Component> lore = new ArrayList<>();
        lore.add(value(numeric, track.value(i)));
        lore.add(Component.empty());
        switch (state) {
            case OWNED -> lore.add(lang.component("quarry.tier_owned"));
            case NEXT -> {
                Cost cost = track.step(i).cost();
                lore.addAll(Payment.lore(player, cost, lang));
                lore.add(Component.empty());
                lore.add(lang.component(Payment.has(player, cost) ? "menu.click_upgrade" : "menu.cant_afford"));
            }
            default -> lore.add(lang.component("quarry.tier_locked"));
        }
        NamespacedKey model = menu.model(switch (state) {
            case OWNED -> "nivel_ok_" + i;
            case NEXT -> "nivel_" + i;
            default -> "nivel_bloqueado";
        });
        Material plain = switch (state) {
            case OWNED -> Material.LIME_STAINED_GLASS_PANE;
            case NEXT -> Material.YELLOW_STAINED_GLASS_PANE;
            default -> Material.RED_STAINED_GLASS_PANE;
        };
        return Ui.button(model == null ? plain : Material.PAPER, model, lang.component("quarry.tier_name",
                "upgrade", lang.raw("quarry.upgrade." + numeric.id()), "level", Ui.roman(i)), lore);
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
        if (slot == BACK) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1f);
            new QuarryMenu(player, quarry, service, lang).open();
            return;
        }
        Numeric numeric = numericAt.get(slot);
        if (numeric != null) {
            upgrade(numeric);
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
        if (!QuarryMenu.pay(player, track.step(level + 1).cost(), lang)) {
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
}
