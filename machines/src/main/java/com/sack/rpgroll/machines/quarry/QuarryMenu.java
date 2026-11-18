package com.sack.rpgroll.machines.quarry;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.MenuBackground;
import com.sack.rpgroll.machines.core.Cost;
import com.sack.rpgroll.machines.core.Payment;
import com.sack.rpgroll.machines.core.Ui;

import net.kyori.adventure.text.Component;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Panel de una cantera: la máquina con su estado en el centro, encendido, salida al cofre y
 * progreso, y los accesos a las mejoras y a los módulos (submenús propios).
 */
public class QuarryMenu extends InventoryGUI {

    private static final int MACHINE = 13;
    private static final int UPGRADES = 19;
    private static final int MODULES = 21;
    private static final int TOGGLE = 22;
    private static final int OUTPUT = 23;
    private static final int GAUGE = 25;

    private final QuarryService service;
    private final LangManager lang;
    private final Quarry quarry;
    private BukkitTask refresh;

    public QuarryMenu(Player player, Quarry quarry, QuarryService service, LangManager lang) {
        super(player, MenuBackground.title(service.settings().menu().background("cantera"),
                lang.component("quarry.menu_title")), 36);
        this.service = service;
        this.lang = lang;
        this.quarry = quarry;
    }

    @Override
    public void build() {

        clear();
        QuarrySettings settings = service.settings();
        QuarrySettings.Menu menu = settings.menu();
        // Con fondo dibujado las casillas libres quedan vacías: los cristales taparían el dibujo.
        if (!menu.drawn()) {
            fill(this::setItem, inventory.getSize());
        }
        World world = Bukkit.getWorld(quarry.world());
        int minHeight = world == null ? -64 : world.getMinHeight();
        int side = service.side(quarry);
        double progress = quarry.progress(side, minHeight);

        List<Component> status = new ArrayList<>();
        status.add(lang.component("quarry.status_owner", "owner", quarry.ownerName()));
        status.add(lang.component("quarry.status_state", "state", lang.raw("quarry.state." + quarry.status().name().toLowerCase())));
        status.add(lang.component("quarry.status_depth", "y", Math.max(minHeight, quarry.cursorY())));
        status.add(lang.component("quarry.status_progress", "progress", Ui.percent(Math.floor(progress * 1000) / 1000)));
        status.add(lang.component("quarry.status_area", "side", side));
        setItem(MACHINE, Ui.button(settings.block(), Ui.model(settings.itemModel()), lang.component("quarry.item_name"), status));

        setItem(UPGRADES, icon(menu, Material.EXPERIENCE_BOTTLE, lang.component("quarry.hub_upgrades"),
                List.of(lang.component("quarry.hub_upgrades_lore"), lang.component("quarry.click_open"))));
        setItem(MODULES, icon(menu, Material.TRIPWIRE_HOOK, lang.component("quarry.hub_modules"),
                List.of(lang.component("quarry.hub_modules_lore"), lang.component("quarry.click_open"))));

        List<Component> output = new ArrayList<>();
        output.add(lang.component("quarry.status_state", "state", lang.raw("quarry.state." + quarry.status().name().toLowerCase())));
        if (!quarry.buffer().isEmpty()) {
            output.add(lang.component("quarry.status_buffer", "count", quarry.buffer().stream().mapToInt(ItemStack::getAmount).sum()));
        }
        setItem(OUTPUT, icon(menu, Material.CHEST, lang.component("quarry.hub_output"), output));

        NamespacedKey gauge = menu.model("cantera_profundidad_" + Quarry.gaugeLevel(progress));
        setItem(GAUGE, Ui.button(gauge == null ? Material.CLOCK : Material.PAPER, gauge,
                lang.component("quarry.status_progress", "progress", Ui.percent(Math.floor(progress * 1000) / 1000)),
                List.of(lang.component("quarry.status_depth", "y", Math.max(minHeight, quarry.cursorY())))));

        setItem(TOGGLE, Ui.button(quarry.enabled() ? Material.LIME_DYE : Material.GRAY_DYE,
                menu.model(quarry.enabled() ? "interruptor_on" : "interruptor_off"),
                lang.component(quarry.enabled() ? "quarry.toggle_on" : "quarry.toggle_off"),
                List.of(lang.component("quarry.click_toggle"))));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (service.store().at(quarry.key()).isEmpty()) {
            close();
            return;
        }
        switch (slot) {
            case TOGGLE -> {
                quarry.enabled(!quarry.enabled());
                service.store().dirty();
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1f);
                build();
                // El estado lo pone el trabajador en su tick: se repinta cuando ya lo ha puesto.
                Bukkit.getScheduler().runTaskLater(JavaPlugin.getProvidingPlugin(QuarryMenu.class), this::build, 2L);
            }
            case UPGRADES -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1f);
                new QuarryUpgradesMenu(player, quarry, service, lang).open();
            }
            case MODULES -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1f);
                new QuarryModulesMenu(player, quarry, service, lang).open();
            }
            default -> {
            }
        }
    }

    /** Abre y repinta cada segundo mientras siga abierto: el estado y el indicador van al día. */
    @Override
    public void open() {
        super.open();
        if (refresh != null) {
            refresh.cancel();
        }
        refresh = Bukkit.getScheduler().runTaskTimer(JavaPlugin.getProvidingPlugin(QuarryMenu.class), () -> {
            if (!player.isOnline() || !player.getOpenInventory().getTopInventory().equals(inventory)) {
                refresh.cancel();
                refresh = null;
                return;
            }
            if (service.store().at(quarry.key()).isEmpty()) {
                close();
                return;
            }
            build();
        }, 20L, 20L);
    }

    // ---------------------------------------------------------------- piezas comunes a los tres menús

    interface Slots {
        void set(int slot, ItemStack item);
    }

    /** Rellena con cristales (menús sin dibujo). */
    static void fill(Slots slots, int size) {
        for (int slot = 0; slot < size; slot++) {
            slots.set(slot, Ui.filler());
        }
    }

    /**
     * Botón cuyo icono ya va dibujado en el fondo: con menús dibujados es un ítem invisible que solo
     * enseña su nombre y su descripción; sin ellos, el ítem vanilla de siempre.
     */
    static ItemStack icon(QuarrySettings.Menu menu, Material plain, Component name, List<Component> lore) {
        NamespacedKey invisible = menu.model("invisible");
        return Ui.button(invisible == null ? plain : Material.PAPER, invisible, name, lore);
    }

    static ItemStack back(QuarrySettings.Menu menu, LangManager lang) {
        return icon(menu, Material.ARROW, lang.component("quarry.back"), List.of());
    }

    static boolean pay(Player player, Cost cost, LangManager lang) {
        if (Payment.take(player, cost)) {
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.8f, 1.2f);
            return true;
        }
        lang.send(player, "menu.cant_afford_message");
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        return false;
    }
}
