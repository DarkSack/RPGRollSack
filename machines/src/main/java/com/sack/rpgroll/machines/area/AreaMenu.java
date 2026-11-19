package com.sack.rpgroll.machines.area;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.MenuBackground;
import com.sack.rpgroll.machines.core.Cost;
import com.sack.rpgroll.machines.core.Payment;
import com.sack.rpgroll.machines.core.Ui;
import com.sack.rpgroll.machines.quarry.QuarrySettings.Menu;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/**
 * Panel de una máquina de área: la máquina con su estado en el centro, mejoras, encendido, salida
 * al cofre e indicador (lo que cada tipo pone: agua, cultivos maduros, tronco...).
 */
public class AreaMenu extends InventoryGUI {

    private static final int MACHINE = 13;
    private static final int UPGRADES = 19;
    private static final int TOGGLE = 22;
    private static final int OUTPUT = 23;
    private static final int GAUGE = 25;
    /** Niveles de llenado del indicador dibujado: {@code <art>_indicador_0..8}. */
    public static final int GAUGE_STEPS = 8;

    private final AreaService service;
    private final AreaKind kind;
    private final LangManager lang;
    private final AreaMachine machine;
    private BukkitTask refresh;

    public AreaMenu(Player player, AreaMachine machine, AreaKind kind, AreaService service, LangManager lang) {
        super(player, MenuBackground.title(kind.settings().menu().background(kind.art()),
                lang.component("area." + kind.id() + ".menu_title")), 36);
        this.service = service;
        this.kind = kind;
        this.lang = lang;
        this.machine = machine;
    }

    /** Del 0 al {@link #GAUGE_STEPS}: lo lleno que va el indicador para una fracción de 0 a 1. */
    public static int gauge(double fraction) {
        return (int) Math.round(Math.clamp(fraction, 0.0, 1.0) * GAUGE_STEPS);
    }

    @Override
    public void build() {
        clear();
        Menu menu = kind.settings().menu();
        // Con fondo dibujado las casillas libres quedan vacías: los cristales taparían el dibujo.
        if (!menu.drawn()) {
            fill(this, inventory.getSize());
        }
        String prefix = "area." + kind.id();
        Component state = lang.component("area.status_state", "state", lang.raw("area.state." + machine.status().name().toLowerCase()));
        List<Component> status = new ArrayList<>();
        status.add(lang.component("area.status_owner", "owner", machine.ownerName()));
        status.add(state);
        status.add(lang.component("area.status_area", "side", kind.side(machine)));
        status.addAll(kind.lore(machine, lang));
        setItem(MACHINE, Ui.button(kind.settings().block(), Ui.model(kind.settings().itemModel()),
                lang.component(prefix + ".item_name"), status));
        setItem(UPGRADES, icon(menu, Material.EXPERIENCE_BOTTLE, lang.component("area.hub_upgrades"),
                List.of(lang.component(prefix + ".hub_upgrades_lore"), lang.component("area.click_open"))));
        List<Component> output = new ArrayList<>(List.of(state));
        if (!machine.buffer().isEmpty()) {
            output.add(lang.component("area.status_buffer", "count", machine.buffer().stream().mapToInt(ItemStack::getAmount).sum()));
        }
        setItem(OUTPUT, icon(menu, Material.CHEST, lang.component("area.hub_output"), output));
        NamespacedKey gauge = menu.model(kind.art() + "_indicador_" + gauge(machine.gauge()));
        setItem(GAUGE, Ui.button(gauge == null ? Material.CLOCK : Material.PAPER, gauge,
                lang.component(prefix + ".gauge"), kind.lore(machine, lang)));
        setItem(TOGGLE, Ui.button(machine.enabled() ? Material.LIME_DYE : Material.GRAY_DYE,
                menu.model(machine.enabled() ? "interruptor_on" : "interruptor_off"),
                lang.component(machine.enabled() ? "area.toggle_on" : "area.toggle_off"),
                List.of(lang.component("area.click_toggle"))));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {
        event.setCancelled(true);
        if (service.store().at(machine.key()).isEmpty()) {
            close();
            return;
        }
        switch (event.getRawSlot()) {
            case TOGGLE -> {
                machine.enabled(!machine.enabled());
                service.store().dirty();
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1f);
                build();
                // El estado lo pone el trabajador en su tick: se repinta cuando ya lo ha puesto.
                Bukkit.getScheduler().runTaskLater(JavaPlugin.getProvidingPlugin(AreaMenu.class), this::build, 2L);
            }
            case UPGRADES -> {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1f);
                new AreaUpgradesMenu(player, machine, kind, service, lang).open();
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
        refresh = Bukkit.getScheduler().runTaskTimer(JavaPlugin.getProvidingPlugin(AreaMenu.class), () -> {
            if (!player.isOnline() || !player.getOpenInventory().getTopInventory().equals(inventory)) {
                refresh.cancel();
                refresh = null;
                return;
            }
            if (service.store().at(machine.key()).isEmpty()) {
                close();
                return;
            }
            build();
        }, 20L, 20L);
    }

    // ---------------------------------------------------------------- piezas comunes a los dos menús

    /** Rellena con cristales (menús sin dibujo). */
    static void fill(InventoryGUI gui, int size) {
        for (int slot = 0; slot < size; slot++) {
            gui.getInventory().setItem(slot, Ui.filler());
        }
    }

    /**
     * Botón cuyo icono ya va dibujado en el fondo: con menús dibujados es un ítem invisible que solo
     * enseña su nombre y su descripción; sin ellos, el ítem vanilla de siempre.
     */
    static ItemStack icon(Menu menu, Material plain, Component name, List<Component> lore) {
        NamespacedKey invisible = menu.model("invisible");
        return Ui.button(invisible == null ? plain : Material.PAPER, invisible, name, lore);
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
