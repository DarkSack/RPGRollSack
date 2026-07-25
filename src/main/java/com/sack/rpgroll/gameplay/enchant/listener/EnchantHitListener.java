package com.sack.rpgroll.gameplay.enchant.listener;

import com.sack.rpgroll.gameplay.enchant.EnchantManager;
import com.sack.rpgroll.gameplay.enchant.EnchantTrigger;
import com.sack.rpgroll.gameplay.enchant.ItemEnchantmentStorage;
import com.sack.rpgroll.gameplay.enchant.effect.EnchantEffectContext;
import com.sack.rpgroll.gameplay.enchant.effect.EnchantEffectRegistry;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Dispara encantamientos con trigger ON_HIT cuando un jugador golpea a una
 * entidad viva con el arma en su mano principal.
 * <p>
 * Se ejecuta en MONITOR + ignoreCancelled=true para leer el daño final ya
 * resuelto (necesario para Vampirismo) — no modifica el evento en sí, solo
 * dispara efectos secundarios (curar, prender fuego, aplicar poción).
 */
public class EnchantHitListener implements Listener {

    private final ItemEnchantmentStorage storage;
    private final EnchantManager enchantManager;
    private final EnchantEffectRegistry effectRegistry;

    public EnchantHitListener(ItemEnchantmentStorage storage, EnchantManager enchantManager,
            EnchantEffectRegistry effectRegistry) {
        this.storage = storage;
        this.enchantManager = enchantManager;
        this.effectRegistry = effectRegistry;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {

        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        ItemStack weapon = player.getInventory().getItemInMainHand();
        Map<String, Integer> enchants = storage.getEnchantments(weapon);

        if (enchants.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Integer> entry : enchants.entrySet()) {
            enchantManager.get(entry.getKey()).ifPresent(enchant -> {

                if (enchant.trigger() != EnchantTrigger.ON_HIT) {
                    return;
                }

                effectRegistry.get(enchant.effectType()).ifPresent(handler -> {
                    EnchantEffectContext context = new EnchantEffectContext(player, weapon, entry.getValue(), enchant);
                    context.setTarget(target);
                    context.setDamageEvent(event);
                    handler.execute(context);
                });
            });
        }
    }

}