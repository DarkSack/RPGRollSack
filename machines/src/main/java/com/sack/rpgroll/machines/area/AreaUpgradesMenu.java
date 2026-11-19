package com.sack.rpgroll.machines.area;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.MenuBackground;
import com.sack.rpgroll.machines.core.Cost;
import com.sack.rpgroll.machines.core.Payment;
import com.sack.rpgroll.machines.core.Ui;
import com.sack.rpgroll.machines.quarry.QuarrySettings.Menu;
import com.sack.rpgroll.machines.quarry.QuarrySettings.Track;
import com.sack.rpgroll.machines.quarry.QuarryUpgradesMenu;
import com.sack.rpgroll.machines.quarry.QuarryUpgradesMenu.Tier;

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
 * Mejoras de una máquina de área: una fila por mejora, con su icono y los niveles en fila (el
 * siguiente se compra con un clic). El mismo dibujo que las mejoras de la Cantera.
 */
public class AreaUpgradesMenu extends InventoryGUI {

    private static final int BACK = 8;
    private static final int TIERS = 5;

    private final AreaService service;
    private final AreaKind kind;
    private final LangManager lang;
    private final AreaMachine machine;
    private final Map<Integer, String> upgradeAt = new HashMap<>();

    public AreaUpgradesMenu(Player player, AreaMachine machine, AreaKind kind, AreaService service, LangManager lang) {
        super(player, MenuBackground.title(kind.settings().menu().background(kind.art() + "_mejoras"),
                lang.component("area." + kind.id() + ".upgrades_title")), 9 * (Math.min(4, kind.upgrades().size()) + 1));
        this.service = service;
        this.kind = kind;
        this.lang = lang;
        this.machine = machine;
    }

    @Override
    public void build() {
        clear();
        upgradeAt.clear();
        Menu menu = kind.settings().menu();
        if (!menu.drawn()) {
            AreaMenu.fill(this, inventory.getSize());
        }
        setItem(BACK, AreaMenu.icon(menu, Material.ARROW, lang.component("area.back"), List.of()));
        int row = 1;
        for (String upgrade : kind.upgrades()) {
            if (row * 9 >= inventory.getSize()) {
                break;
            }
            Track track = kind.settings().track(upgrade);
            int level = machine.level(upgrade);
            int first = row * 9;
            row++;
            List<Component> lore = new ArrayList<>();
            lore.add(lang.component("area.level", "level", Ui.roman(level), "max", Ui.roman(track.max())));
            lore.add(value(upgrade, track.value(level)));
            if (QuarryUpgradesMenu.iconBuys(level, track.max())) {
                upgradeAt.put(first + 1, upgrade);
                Cost cost = track.step(level + 1).cost();
                lore.add(Component.empty());
                lore.add(value(upgrade, track.value(level + 1)));
                lore.addAll(Payment.lore(player, cost, lang));
                lore.add(lang.component(Payment.has(player, cost) ? "menu.click_upgrade" : "menu.cant_afford"));
            }
            setItem(first + 1, Ui.button(track.icon(), lang.component("area.upgrade_title", "upgrade", name(upgrade)), lore));
            for (int i = 1; i <= TIERS; i++) {
                Tier state = QuarryUpgradesMenu.tier(level, i, track.max());
                if (state == Tier.NONE) {
                    continue;
                }
                int slot = first + 2 + i;
                if (state == Tier.NEXT) {
                    upgradeAt.put(slot, upgrade);
                }
                setItem(slot, tierItem(menu, upgrade, track, i, state));
            }
        }
    }

    private String name(String upgrade) {
        return lang.raw("area." + kind.id() + ".upgrade." + upgrade);
    }

    private Component value(String upgrade, double value) {
        return lang.component("area." + kind.id() + ".value." + upgrade, "value", kind.format(upgrade, value, lang));
    }

    private ItemStack tierItem(Menu menu, String upgrade, Track track, int i, Tier state) {
        List<Component> lore = new ArrayList<>();
        lore.add(value(upgrade, track.value(i)));
        lore.add(Component.empty());
        switch (state) {
            case OWNED -> lore.add(lang.component("area.tier_owned"));
            case NEXT -> {
                Cost cost = track.step(i).cost();
                lore.addAll(Payment.lore(player, cost, lang));
                lore.add(Component.empty());
                lore.add(lang.component(Payment.has(player, cost) ? "menu.click_upgrade" : "menu.cant_afford"));
            }
            default -> lore.add(lang.component("area.tier_locked"));
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
        return Ui.button(model == null ? plain : Material.PAPER, model,
                lang.component("area.tier_name", "upgrade", name(upgrade), "level", Ui.roman(i)), lore);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (service.store().at(machine.key()).isEmpty()) {
            close();
            return;
        }
        if (slot == BACK) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1f);
            new AreaMenu(player, machine, kind, service, lang).open();
            return;
        }
        String upgrade = upgradeAt.get(slot);
        if (upgrade != null) {
            upgrade(upgrade);
        }
    }

    private void upgrade(String upgrade) {
        Track track = kind.settings().track(upgrade);
        int level = machine.level(upgrade);
        if (level >= track.max()) {
            return;
        }
        if (upgrade.equals("area")) {
            // El área nueva también tiene que caber en el claim, antes de cobrar.
            World world = Bukkit.getWorld(machine.world());
            machine.level(upgrade, level + 1);
            int side = kind.side(machine);
            machine.level(upgrade, level);
            if (world == null || !service.areaAllowed(machine, world, side)) {
                lang.send(player, "area.need_claim", "side", side);
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                return;
            }
        }
        if (!AreaMenu.pay(player, track.step(level + 1).cost(), lang)) {
            build();
            return;
        }
        machine.level(upgrade, level + 1);
        machine.cursor(0);
        service.store().dirty();
        lang.send(player, "area.upgraded", "upgrade", name(upgrade), "level", Ui.roman(level + 1));
        build();
    }
}
