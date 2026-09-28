package com.sack.rpgroll.ranching.listener;

import com.sack.rpgroll.ranching.core.animal.AnimalManager;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Con un modelo de FreeMinecraftModels el cliente no ve al animal vanilla: el clic sobre el modelo
 * llega como un evento de FMM (hijo de {@link PlayerInteractEntityEvent}) y el juego no hace lo que
 * haría al tocar al animal. Aquí se hace: esquilar con tijeras (vía {@link PlayerShearEntityEvent},
 * que ya trata ProductionListener) y poner en celo con su comida (el resto de la cría sigue igual).
 * Ordeñar, alimentar y curar ya los tratan los listeners del rancho con el evento de FMM.
 */
public class ModelInteractionBridge implements Listener {

    private static final String FMM_EVENT = "com.magmaguy.freeminecraftmodels.api.ModeledEntityInteractEvent";

    private final AnimalManager animalManager;

    public ModelInteractionBridge(AnimalManager animalManager) {
        this.animalManager = animalManager;
    }

    // HIGHEST: después de la protección de dueños, el cuidado y la producción (si alguno lo usó, está cancelado).
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onModelInteract(PlayerInteractEntityEvent event) {

        // Solo los clics de FMM: en los del juego (también PlayerInteractAtEntityEvent) vanilla ya lo hace.
        // Por nombre, para no cargar clases de FMM si no está instalado.
        if (!FMM_EVENT.equals(event.getClass().getName()) || event.getHand() != EquipmentSlot.HAND
                || animalManager.resolve(event.getRightClicked()).isEmpty()) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (hand.getType() == Material.SHEARS && event.getRightClicked() instanceof Sheep sheep && sheep.readyToBeSheared()) {
            Bukkit.getPluginManager().callEvent(new PlayerShearEntityEvent(player, sheep, hand, EquipmentSlot.HAND,
                    List.of()));
            return;
        }

        if (event.getRightClicked() instanceof Animals animal && animal.isBreedItem(hand) && animal.canBreed()
                && !animal.isLoveMode()) {
            animal.setLoveModeTicks(600);
            animal.setBreedCause(player.getUniqueId());
            animal.getWorld().spawnParticle(Particle.HEART, animal.getLocation().add(0, animal.getHeight(), 0), 5,
                    0.4, 0.3, 0.4);
            if (player.getGameMode() != GameMode.CREATIVE) {
                hand.setAmount(hand.getAmount() - 1);
            }
            event.setCancelled(true);
        }
    }

}
