package com.sack.rpgroll.traps.gui.turret;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.traps.ammo.AmmoDefinition;
import com.sack.rpgroll.traps.ammo.AmmoItem;
import com.sack.rpgroll.traps.ammo.AmmoManager;
import com.sack.rpgroll.traps.turret.TurretDefinition;
import com.sack.rpgroll.traps.turret.TurretItem;
import com.sack.rpgroll.traps.turret.TurretManager;

import net.kyori.adventure.text.Component;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Catálogo para conseguir torretas y munición con un clic.
 * <p>
 * Existe porque la única forma de obtenerlas era escribir
 * {@code /trapadmin turret give <id> <jugador> <cantidad>} de memoria, lo que
 * obliga a saberse los ids antes de empezar. Acá se ven todas con lo que hace
 * falta para decidir — radio, cadencia, a quién le disparan — y la munición
 * disponible al lado, que es lo que hay que meterles para que sirvan.
 * <p>
 * Es solo de consulta y entrega: no edita nada. Para modificar definiciones
 * está {@link TurretBrowserGUI}.
 */
public class TurretCatalogGUI extends InventoryGUI {

    private static final int SIZE = 54;

    /** Las torretas ocupan las tres primeras filas. */
    private static final int TURRET_SLOTS = 27;

    /** Fila separadora con el rótulo de la sección de munición. */
    private static final int DIVIDER_ROW_START = 27;
    private static final int DIVIDER_LABEL_SLOT = 31;

    /** La munición va en la fila siguiente. */
    private static final int AMMO_ROW_START = 36;
    private static final int AMMO_SLOTS = 9;

    private static final int CLOSE_SLOT = 49;

    private final Plugin plugin;
    private final LangManager lang;
    private final List<TurretDefinition> turrets;
    private final List<AmmoDefinition> ammo;

    public TurretCatalogGUI(Player player, Plugin plugin, TurretManager turretManager, AmmoManager ammoManager,
            LangManager lang) {

        super(player, lang.component("gui.turret_catalog.title"), SIZE);

        this.plugin = plugin;
        this.lang = lang;
        this.turrets = List.copyOf(turretManager.getAll());
        this.ammo = List.copyOf(ammoManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        buildTurrets();
        buildDivider();
        buildAmmo();

        setItem(CLOSE_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.close")));
    }

    private void buildTurrets() {

        for (int i = 0; i < turrets.size() && i < TURRET_SLOTS; i++) {

            TurretDefinition turret = turrets.get(i);

            // El bloque base es lo que queda puesto en el mundo, así que es el
            // ícono más honesto: se ve lo mismo que se va a colocar.
            Material icon = turret.baseBlock() != null ? turret.baseBlock()
                    : (turret.model() != null ? turret.model() : Material.DISPENSER);

            List<Component> lore = new ArrayList<>();

            if (turret.description() != null && !turret.description().isBlank()) {
                lore.add(com.sack.rpgroll.util.ComponentUtils.parse(turret.description()));
            }

            lore.add(lang.component("gui.turret_catalog.radius", "value", turret.radius()));
            lore.add(lang.component("gui.turret_catalog.interval", "value", turret.fireIntervalTicks() / 20.0));
            lore.add(lang.component("gui.turret_catalog.targets", "value", targetsOf(turret)));
            lore.add(lang.component("gui.turret_catalog.ammo_hint", "count", ammo.size()));
            lore.add(lang.component("gui.turret_catalog.click_to_get"));

            setItem(i, new ItemBuilder(icon)
                    .setName(com.sack.rpgroll.util.ComponentUtils.parse(turret.displayName()))
                    .setLore(lore.toArray(new Component[0]))
                    .build());
        }
    }

    private void buildDivider() {

        for (int slot = DIVIDER_ROW_START; slot < DIVIDER_ROW_START + 9; slot++) {
            setItem(slot, new ItemBuilder(Material.GRAY_STAINED_GLASS_PANE).setName(Component.empty()).build());
        }

        setItem(DIVIDER_LABEL_SLOT, new ItemBuilder(Material.TIPPED_ARROW)
                .setName(lang.component("gui.turret_catalog.ammo_section"))
                .setLore(lang.component("gui.turret_catalog.ammo_section_lore"))
                .build());
    }

    private void buildAmmo() {

        for (int i = 0; i < ammo.size() && i < AMMO_SLOTS; i++) {

            AmmoDefinition definition = ammo.get(i);

            List<Component> lore = new ArrayList<>();

            if (definition.description() != null && !definition.description().isBlank()) {
                lore.add(com.sack.rpgroll.util.ComponentUtils.parse(definition.description()));
            }

            lore.add(lang.component("gui.turret_catalog.click_to_get_ammo"));

            setItem(AMMO_ROW_START + i, new ItemBuilder(definition.icon())
                    .setName(com.sack.rpgroll.util.ComponentUtils.parse(definition.displayName()))
                    .setLore(lore.toArray(new Component[0]))
                    .build());
        }
    }

    /** Resumen legible de a quién le dispara, para no obligar a abrir el editor. */
    private String targetsOf(TurretDefinition turret) {

        if (turret.targetPlayers() && turret.targetHostileMobs()) {
            return lang.raw("gui.turret_catalog.targets_both");
        }

        if (turret.targetPlayers()) {
            return lang.raw("gui.turret_catalog.targets_players");
        }

        if (turret.targetHostileMobs()) {
            return lang.raw("gui.turret_catalog.targets_mobs");
        }

        return lang.raw("gui.turret_catalog.targets_none");
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < turrets.size() && slot < TURRET_SLOTS) {
            TurretDefinition turret = turrets.get(slot);
            player.getInventory().addItem(TurretItem.create(plugin, turret, lang, 1));
            lang.send(player, "gui.turret_catalog.received", "name", turret.displayName());
            return;
        }

        int ammoIndex = slot - AMMO_ROW_START;

        if (ammoIndex >= 0 && ammoIndex < ammo.size() && ammoIndex < AMMO_SLOTS) {
            AmmoDefinition definition = ammo.get(ammoIndex);
            player.getInventory().addItem(AmmoItem.create(plugin, definition, lang, 1));
            lang.send(player, "gui.turret_catalog.received", "name", definition.displayName());
            return;
        }

        if (slot == CLOSE_SLOT) {
            close();
        }
    }

}
