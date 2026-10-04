package com.sack.rpgroll.machines.spawner;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.machines.core.Payment;
import com.sack.rpgroll.machines.core.Ui;
import com.sack.rpgroll.machines.spawner.SpawnerSettings.Level;
import com.sack.rpgroll.machines.spawner.SpawnerSettings.Track;
import com.sack.rpgroll.machines.spawner.SpawnerSettings.Upgrade;

import net.kyori.adventure.text.Component;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Las tres mejoras de un spawner y su pila. */
public class SpawnerMenu extends InventoryGUI {

    private static final Map<Integer, Upgrade> SLOTS = Map.of(11, Upgrade.SPEED, 13, Upgrade.COUNT, 15, Upgrade.RANGE);
    private static final int STACK = 22;

    private final SpawnerService service;
    private final LangManager lang;
    private final Block block;

    public SpawnerMenu(Player player, Block block, SpawnerService service, LangManager lang, EntityType mob) {
        super(player, SpawnerService.mob(lang.component("spawner.menu_title"), mob), 27);
        this.service = service;
        this.lang = lang;
        this.block = block;
    }

    @Override
    public void build() {

        clear();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            setItem(slot, Ui.filler());
        }
        var spawner = service.spawner(block);
        if (spawner.isEmpty()) {
            return;
        }
        SpawnerData data = service.data(spawner.get());
        SpawnerData.Applied now = data.applied(service.settings());

        for (var entry : SLOTS.entrySet()) {
            Upgrade upgrade = entry.getValue();
            Track track = service.settings().track(upgrade);
            int level = data.level(upgrade);
            List<Component> lore = new ArrayList<>();
            lore.add(lang.component("spawner.level", "level", Ui.roman(level), "max", Ui.roman(track.max())));
            lore.add(value(upgrade, now));
            lore.add(Component.empty());
            if (level >= track.max()) {
                lore.add(lang.component("menu.maxed"));
            } else {
                SpawnerData next = data.with(upgrade, level + 1);
                lore.add(lang.component("spawner.next"));
                lore.add(value(upgrade, next.applied(service.settings())));
                lore.add(Component.empty());
                Level target = track.level(level + 1);
                lore.addAll(Payment.lore(player, target.cost(), lang));
                lore.add(Component.empty());
                lore.add(lang.component(Payment.has(player, target.cost()) ? "menu.click_upgrade" : "menu.cant_afford"));
            }
            setItem(entry.getKey(), Ui.button(track.icon(),
                    lang.component("spawner.upgrade_title", "upgrade", lang.raw("spawner.upgrade." + upgrade.id())), lore));
        }

        if (service.settings().stack()) {
            setItem(STACK, Ui.button(Material.SPAWNER, lang.component("spawner.stack_title", "stack", data.stack(),
                    "max", service.settings().maxStack()), List.of(lang.component("spawner.stack_hint"))));
        }
    }

    private Component value(Upgrade upgrade, SpawnerData.Applied values) {
        return switch (upgrade) {
            case SPEED -> lang.component("spawner.value_speed", "min", Ui.number(values.minDelay() / 20.0),
                    "max", Ui.number(values.maxDelay() / 20.0));
            case COUNT -> lang.component("spawner.value_count", "count", values.spawnCount(), "nearby", values.maxNearby());
            case RANGE -> lang.component("spawner.value_range", "range", values.range());
        };
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        Upgrade upgrade = SLOTS.get(event.getRawSlot());
        if (upgrade == null) {
            return;
        }
        var spawner = service.spawner(block);
        if (spawner.isEmpty()) {
            close();
            return;
        }
        CreatureSpawner state = spawner.get();
        SpawnerData data = service.data(state);
        Track track = service.settings().track(upgrade);
        int level = data.level(upgrade);
        if (level >= track.max()) {
            return;
        }
        if (!Payment.take(player, track.level(level + 1).cost())) {
            lang.send(player, "menu.cant_afford_message");
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            build();
            return;
        }
        service.apply(block, null, data.with(upgrade, level + 1));
        player.playSound(block.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.8f, 1.2f);
        lang.send(player, "spawner.upgraded", "upgrade", lang.raw("spawner.upgrade." + upgrade.id()),
                "level", Ui.roman(level + 1));
        build();
    }
}
