package com.sack.rpgroll.gameplay.enchant.listener;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.gameplay.enchant.EnchantManager;
import com.sack.rpgroll.gameplay.enchant.EnchantTrigger;
import com.sack.rpgroll.gameplay.enchant.ItemEnchantmentStorage;
import com.sack.rpgroll.gameplay.enchant.effect.EnchantEffectContext;
import com.sack.rpgroll.gameplay.enchant.effect.EnchantEffectRegistry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;

/**
 * Tarea periódica que revisa la armadura de todos los jugadores online y
 * ejecuta los encantamientos con trigger PASSIVE (ej. Salto de Liebre).
 * No distingue pieza de armadura específica — aplica igual sin importar si
 * está en casco, pechera, piernas o botas.
 */
public class PassiveEnchantTask extends BukkitRunnable {

    private static final long PERIOD_TICKS = 40L; // 2 segundos

    private final ItemEnchantmentStorage storage;
    private final EnchantManager enchantManager;
    private final EnchantEffectRegistry effectRegistry;

    public PassiveEnchantTask(ItemEnchantmentStorage storage, EnchantManager enchantManager,
            EnchantEffectRegistry effectRegistry) {
        this.storage = storage;
        this.enchantManager = enchantManager;
        this.effectRegistry = effectRegistry;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            checkArmor(player);
        }
    }

    private void checkArmor(Player player) {

        for (ItemStack armorPiece : player.getInventory().getArmorContents()) {

            if (armorPiece == null || armorPiece.getType().isAir()) {
                continue;
            }

            Map<String, Integer> enchants = storage.getEnchantments(armorPiece);

            for (Map.Entry<String, Integer> entry : enchants.entrySet()) {
                enchantManager.get(entry.getKey()).ifPresent(enchant -> {

                    if (enchant.trigger() != EnchantTrigger.PASSIVE) {
                        return;
                    }

                    effectRegistry.get(enchant.effectType()).ifPresent(handler -> {
                        EnchantEffectContext context = new EnchantEffectContext(player, armorPiece, entry.getValue(),
                                enchant);
                        handler.execute(context);
                    });
                });
            }
        }
    }

    public void start(RPGRoll plugin) {
        runTaskTimer(plugin, PERIOD_TICKS, PERIOD_TICKS);
    }

}