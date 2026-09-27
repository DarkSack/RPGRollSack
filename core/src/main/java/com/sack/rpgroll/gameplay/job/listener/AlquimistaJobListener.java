package com.sack.rpgroll.gameplay.job.listener;

import com.sack.rpgroll.gameplay.job.JobRewardService;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionType;

import java.util.EnumSet;
import java.util.Set;

/**
 * Otorga recompensas de trabajo "alquimista" cuando el jugador retira una
 * poción recién fermentada de los slots de resultado del soporte de pociones.
 * <p>
 * Al terminar la tanda ({@link BrewEvent}) cada poción resultante se marca;
 * al sacarla del soporte se paga y se le quita la marca. Antes pagaba
 * cualquier clic sobre una poción en esos slots, aunque la hubiera metido el
 * propio jugador: con una sola poción, meterla y sacarla daba dinero y
 * experiencia sin fin.
 * <p>
 * Se paga al retirarla y no en el BrewEvent porque la tanda no tiene dueño:
 * puede terminar con nadie cerca, y la cobra quien la saca.
 */
public class AlquimistaJobListener implements Listener {

    private static final String JOB_ID = "alquimista";

    // Slots de resultado en un BrewerInventory: 0, 1, 2 (los 3 frascos de salida)
    private static final Set<Integer> RESULT_SLOTS = Set.of(0, 1, 2);

    /** Los clics con los que la poción sale del slot (no los que solo la miran o dejan otra). */
    private static final Set<InventoryAction> TAKES = EnumSet.of(
            InventoryAction.PICKUP_ALL, InventoryAction.PICKUP_HALF, InventoryAction.PICKUP_ONE,
            InventoryAction.PICKUP_SOME, InventoryAction.MOVE_TO_OTHER_INVENTORY, InventoryAction.SWAP_WITH_CURSOR,
            InventoryAction.HOTBAR_SWAP, InventoryAction.DROP_ALL_SLOT, InventoryAction.DROP_ONE_SLOT);

    private final JobRewardService rewardService;
    private final NamespacedKey brewedKey;

    public AlquimistaJobListener(JobRewardService rewardService, NamespacedKey brewedKey) {
        this.rewardService = rewardService;
        this.brewedKey = brewedKey;
    }

    /** En HIGHEST para marcar el resultado final, después de las recetas de RPGRoll-Crafting. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBrew(BrewEvent event) {

        for (ItemStack result : event.getResults()) {

            if (result == null || !(result.getItemMeta() instanceof PotionMeta meta)) {
                continue;
            }

            meta.getPersistentDataContainer().set(brewedKey, PersistentDataType.BYTE, (byte) 1);
            result.setItemMeta(meta);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {

        if (!(event.getView().getTopInventory() instanceof BrewerInventory)) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!RESULT_SLOTS.contains(event.getRawSlot()) || !TAKES.contains(event.getAction())) {
            return;
        }

        ItemStack item = event.getCurrentItem();

        if (item == null || item.getAmount() == 0 || !(item.getItemMeta() instanceof PotionMeta meta)
                || !meta.getPersistentDataContainer().has(brewedKey, PersistentDataType.BYTE)) {
            return;
        }

        // Una poción, un pago: la marca se va con ella (el ítem del evento es el del slot).
        PotionType type = meta.getBasePotionType();
        meta.getPersistentDataContainer().remove(brewedKey);
        item.setItemMeta(meta);

        if (type != null) {
            rewardService.reward(player, JOB_ID, type.name());
        }
    }

}
