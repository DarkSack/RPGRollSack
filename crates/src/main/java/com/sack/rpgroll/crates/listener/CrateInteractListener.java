package com.sack.rpgroll.crates.listener;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.crates.core.Crate;
import com.sack.rpgroll.crates.core.CrateActionExecutor;
import com.sack.rpgroll.crates.core.CrateManager;
import com.sack.rpgroll.crates.gui.CrateSpinGUI;
import com.sack.rpgroll.crates.key.CrateKeyItem;
import com.sack.rpgroll.crates.location.PlacedCrate;
import com.sack.rpgroll.crates.location.PlacedCrateManager;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.Optional;

/**
 * Detecta el click derecho sobre el bloque de un crate registrado,
 * valida (y consume) la llave si corresponde, y abre la ruleta.
 */
public class CrateInteractListener implements Listener {

    private final Plugin plugin;
    private final CrateManager crateManager;
    private final PlacedCrateManager placedCrateManager;
    private final CrateKeyItem crateKeyItem;
    private final CrateActionExecutor actionExecutor;
    private final LangManager lang;

    public CrateInteractListener(
            Plugin plugin,
            CrateManager crateManager,
            PlacedCrateManager placedCrateManager,
            CrateKeyItem crateKeyItem,
            CrateActionExecutor actionExecutor,
            LangManager lang) {

        this.plugin = plugin;
        this.crateManager = crateManager;
        this.placedCrateManager = placedCrateManager;
        this.crateKeyItem = crateKeyItem;
        this.actionExecutor = actionExecutor;
        this.lang = lang;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (event.getClickedBlock() == null) {
            return;
        }

        Optional<PlacedCrate> placedOpt = placedCrateManager.findAt(event.getClickedBlock().getLocation());

        if (placedOpt.isEmpty()) {
            return;
        }

        event.setCancelled(true);

        PlacedCrate placed = placedOpt.get();
        Optional<Crate> crateOpt = crateManager.get(placed.crateId());

        if (crateOpt.isEmpty()) {
            plugin.getLogger().warning("✘ Crate colocado '" + placed.placementId()
                    + "' apunta a un tipo inexistente: " + placed.crateId());
            return;
        }

        Player player = event.getPlayer();
        Crate crate = crateOpt.get();

        if (crate.requireKey()) {

            ItemStack inHand = player.getInventory().getItemInMainHand();

            if (!crateKeyItem.isKeyFor(inHand, crate.id())) {
                lang.send(player, "interact.needs_key");
                return;
            }

            consumeOne(player, inHand);
        }

        new CrateSpinGUI(plugin, player, crate, actionExecutor, lang).open();
    }

    /**
     * Descuenta una unidad de la llave, escribiendo el resultado de vuelta
     * en la mano principal explícitamente en vez de mutar el ItemStack
     * obtenido de getItemInMainHand() — no todas las implementaciones
     * garantizan que sea una referencia viva a la ranura real.
     */
    private void consumeOne(Player player, ItemStack keyStack) {

        int newAmount = keyStack.getAmount() - 1;

        if (newAmount <= 0) {
            player.getInventory().setItemInMainHand(null);
            return;
        }

        ItemStack updated = keyStack.clone();
        updated.setAmount(newAmount);
        player.getInventory().setItemInMainHand(updated);
    }

}
