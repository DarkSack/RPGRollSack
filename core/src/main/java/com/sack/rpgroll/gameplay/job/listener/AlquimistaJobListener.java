package com.sack.rpgroll.gameplay.job.listener;

import com.sack.rpgroll.gameplay.job.JobRewardService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import java.util.Set;

/**
 * Otorga recompensas de trabajo "alquimista" cuando el jugador retira una
 * poción ya fermentada de los slots de resultado del soporte de pociones.
 * <p>
 * Se usa InventoryClickEvent en vez de BrewEvent porque BrewEvent se
 * dispara ANTES de aplicar la transformación (getContents() en ese momento
 * todavía trae ingredientes de entrada, no el resultado final). Detectar
 * el retiro real captura el tipo de poción correcto.
 * <p>
 * No requiere anti-farm — el tiempo de fermentación (~20s por tanda) y el
 * costo de ingredientes reales ya limitan naturalmente la tasa de creación.
 */
public class AlquimistaJobListener implements Listener {

    private static final String JOB_ID = "alquimista";

    // Slots de resultado en un BrewerInventory: 0, 1, 2 (los 3 frascos de salida)
    private static final Set<Integer> RESULT_SLOTS = Set.of(0, 1, 2);

    private final JobRewardService rewardService;

    public AlquimistaJobListener(JobRewardService rewardService) {
        this.rewardService = rewardService;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        Inventory topInventory = event.getView().getTopInventory();

        if (!(topInventory instanceof BrewerInventory)) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        int slot = event.getRawSlot();

        if (!RESULT_SLOTS.contains(slot)) {
            return;
        }

        ItemStack clickedItem = event.getCurrentItem();
        rewardIfPotion(player, clickedItem);

        // Shift-click puede mover más de un item si los 3 slots tienen la
        // misma poción — currentItem solo refleja el slot bajo el cursor,
        // así que en shift-click revisamos los otros dos slots también.
        if (event.isShiftClick()) {
            for (int otherSlot : RESULT_SLOTS) {
                if (otherSlot != slot) {
                    rewardIfPotion(player, topInventory.getItem(otherSlot));
                }
            }
        }
    }

    private void rewardIfPotion(Player player, ItemStack item) {

        if (item == null || item.getAmount() == 0) {
            return;
        }

        if (!(item.getItemMeta() instanceof PotionMeta potionMeta)) {
            return;
        }

        PotionType type = potionMeta.getBasePotionType();

        if (type == null) {
            return;
        }

        rewardService.reward(player, JOB_ID, type.name());
    }

}