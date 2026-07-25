package com.sack.rpgroll.gameplay.enchant.listener;

import com.sack.rpgroll.gameplay.enchant.CustomEnchantment;
import com.sack.rpgroll.gameplay.enchant.EnchantManager;
import com.sack.rpgroll.gameplay.enchant.EnchantedBookFactory;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Otorga libros encantados custom al matar mobs, según drop-chance y
 * drop-mobs definidos en cada CustomEnchantment. Reutiliza la misma marca
 * de "spawner" que CazadorJobListener — mobs de granjas automáticas
 * tampoco sueltan libros encantados.
 */
public class EnchantDropListener implements Listener {

    private final EnchantManager enchantManager;
    private final EnchantedBookFactory bookFactory;
    private final NamespacedKey fromSpawnerKey;

    public EnchantDropListener(EnchantManager enchantManager, EnchantedBookFactory bookFactory,
            NamespacedKey fromSpawnerKey) {
        this.enchantManager = enchantManager;
        this.bookFactory = bookFactory;
        this.fromSpawnerKey = fromSpawnerKey;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {

        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();

        if (killer == null) {
            return;
        }

        if (entity.getPersistentDataContainer().has(fromSpawnerKey, PersistentDataType.BYTE)) {
            return;
        }

        String mobName = entity.getType().name();

        for (CustomEnchantment enchant : enchantManager.getAll()) {

            if (!enchant.isDroppable() || !enchant.dropMobs().contains(mobName)) {
                continue;
            }

            if (ThreadLocalRandom.current().nextDouble() > enchant.dropChance()) {
                continue;
            }

            int level = ThreadLocalRandom.current().nextInt(enchant.maxLevel()) + 1;
            ItemStack book = bookFactory.create(enchant, level);

            entity.getWorld().dropItemNaturally(entity.getLocation(), book);
        }
    }

}