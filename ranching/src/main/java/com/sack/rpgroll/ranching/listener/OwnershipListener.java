package com.sack.rpgroll.ranching.listener;

import com.sack.rpgroll.common.reskin.EntityReskinService;

import com.sack.rpgroll.ranching.core.animal.Animal;
import com.sack.rpgroll.ranching.core.animal.AnimalManager;
import com.sack.rpgroll.ranching.core.ownership.AnimalMarket;
import com.sack.rpgroll.ranching.core.ownership.OwnershipService;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Los animales con dueño solo los toca su dueño: ordeñar, esquilar, alimentar, curar, criar, atar
 * con correa y hacerles daño. Si están a la venta, el clic de otro jugador enseña el precio y un
 * botón para comprarlo. También lleva la cuenta de dónde está cada animal cuando su chunk se
 * descarga, y retira las entidades viejas de un animal que ya se recuperó en otra.
 */
public class OwnershipListener implements Listener {

    private static final long NOTICE_COOLDOWN_MS = 1500;

    private final org.bukkit.plugin.Plugin plugin;
    private final OwnershipService ownership;
    private final AnimalManager animalManager;
    private final Map<UUID, Long> lastNotice = new HashMap<>();

    public OwnershipListener(org.bukkit.plugin.Plugin plugin, OwnershipService ownership) {
        this.plugin = plugin;
        this.ownership = ownership;
        this.animalManager = ownership.animals();
    }

    // LOWEST: antes que el cuidado, la producción y la cría (todos ignoran lo cancelado).
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEntityEvent event) {

        Animal animal = animalManager.resolve(event.getRightClicked()).orElse(null);
        Player player = event.getPlayer();

        if (animal == null || !ownership.settings().protect() || ownership.mayHandle(player, animal)) {
            return;
        }

        event.setCancelled(true);

        if (event.getHand() == EquipmentSlot.HAND && noticeAllowed(player)) {
            notice(player, animal);
        }
    }

    /** "Es de X" o, si está a la venta, su precio con el botón de comprar. */
    private void notice(Player player, Animal animal) {

        if (animal.isForSale() && ownership.settings().marketEnabled()) {
            String command = "/ranching comprar " + OwnershipService.shortId(animal);
            player.sendMessage(ownership.lang().component("market.for_sale_notice", "animal", ownership.describe(animal),
                    "owner", ownership.ownerName(animal), "price", AnimalMarket.format(animal.salePrice())));
            player.sendMessage(Component.empty().append(ownership.lang().component("market.buy_button"))
                    .clickEvent(ClickEvent.runCommand(command)));
        } else {
            ownership.lang().send(player, "owner.not_yours", "owner", ownership.ownerName(animal));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onShear(PlayerShearEntityEvent event) {
        if (blocked(event.getPlayer(), event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onLeash(PlayerLeashEntityEvent event) {
        if (blocked(event.getPlayer(), event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {

        Player attacker = event.getDamager() instanceof Player player ? player
                : event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter
                        ? shooter : null;

        if (attacker != null && blocked(attacker, event.getEntity())) {
            event.setCancelled(true);
            // Con un modelo de FMM el clic derecho con la mano vacía no llega: golpear también enseña el precio.
            Animal animal = animalManager.resolve(event.getEntity()).orElse(null);
            if (animal != null && noticeAllowed(attacker)) {
                notice(attacker, animal);
            }
        }
    }

    private boolean blocked(Player player, Entity entity) {
        Animal animal = animalManager.resolve(entity).orElse(null);
        return animal != null && ownership.settings().protect() && !ownership.mayHandle(player, animal);
    }

    private boolean noticeAllowed(Player player) {
        long now = System.currentTimeMillis();
        Long last = lastNotice.put(player.getUniqueId(), now);
        return last == null || now - last > NOTICE_COOLDOWN_MS;
    }

    /** Un teletransporte (comando, portal, plugin) puede dejarlo en un chunk que no está cargado: se apunta adónde fue. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(org.bukkit.event.entity.EntityTeleportEvent event) {
        if (event.getTo() != null) {
            animalManager.resolve(event.getEntity()).ifPresent(animal -> animalManager.updateLastSeen(animal, event.getTo()));
        }
    }

    /** Al descargarse un chunk, cada animal queda apuntado donde estaba: así se le encuentra luego. */
    @EventHandler
    public void onEntitiesUnload(EntitiesUnloadEvent event) {
        for (Entity entity : event.getEntities()) {
            animalManager.resolve(entity).ifPresent(animal -> animalManager.updateLastSeen(animal, entity.getLocation()));
        }
    }

    /** Una entidad de un animal que ya vive en otra (se recuperó mientras esta estaba descargada) sobra. */
    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent event) {
        // Al tick siguiente: quitar entidades o poner modelos en plena carga del chunk no es seguro.
        var entities = java.util.List.copyOf(event.getEntities());
        org.bukkit.Bukkit.getScheduler().runTask(plugin, () -> checkLoaded(entities));
    }

    private void checkLoaded(java.util.List<Entity> entities) {

        for (Entity entity : entities) {

            if (!entity.isValid()) {
                continue;
            }


            if (!animalManager.isTracked(entity)) {
                continue;
            }

            Animal animal = animalManager.get(animalManager.animalIdOf(entity)).orElse(null);

            if (animal != null && !animal.entityId().equals(entity.getUniqueId())) {
                if (entity instanceof LivingEntity living) {
                    EntityReskinService.remove(living);
                    com.sack.rpgroll.ranching.integration.ModelsIntegration.remove(living);
                }
                entity.remove();
                continue;
            }

            // FMM no guarda el modelo con la entidad: se le vuelve a poner en cuanto se carga.
            if (animal != null && entity instanceof LivingEntity living) {
                var breed = animal.breedId() == null ? null : ownership.breeds().get(animal.breedId()).orElse(null);
                animalManager.ensureAppearanceAttached(living, breed, animal.stage());
            }
        }
    }

}
