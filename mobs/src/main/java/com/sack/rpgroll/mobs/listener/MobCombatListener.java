package com.sack.rpgroll.mobs.listener;

import com.sack.rpgroll.mobs.core.MobDefinition;
import com.sack.rpgroll.mobs.core.MobManager;
import com.sack.rpgroll.mobs.engine.MobEngine;
import com.sack.rpgroll.mobs.instance.MobInstanceService;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

/**
 * Puentea el combate vanilla con {@link MobEngine}: aplica esquive,
 * resistencias/debilidades y defensa sobre el daño entrante de mobs
 * gestionados, registra contribución de daño para loot PER_PLAYER, y
 * dispara el chequeo de fase/trigger DAMAGED una vez que Bukkit ya
 * restó la vida (por eso se agenda para el próximo tick).
 */
public class MobCombatListener implements Listener {

    private final MobEngine engine;
    private final MobInstanceService instanceService;
    private final MobManager mobManager;
    private final JavaPlugin plugin;

    public MobCombatListener(MobEngine engine, MobInstanceService instanceService, MobManager mobManager,
            JavaPlugin plugin) {
        this.engine = engine;
        this.instanceService = instanceService;
        this.mobManager = mobManager;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {

        if (!(event.getEntity() instanceof LivingEntity victim)) {
            return;
        }

        MobDefinition definition = resolveDefinition(victim);
        if (definition == null) {
            return;
        }

        if (engine.rollDodge(definition)) {
            event.setCancelled(true);
            return;
        }

        LivingEntity attacker = resolveAttacker(event);

        double adjusted = engine.applyResistancesAndWeaknesses(definition, event.getDamage(), event.getCause(),
                attacker);
        event.setDamage(adjusted);

        if (attacker instanceof Player player) {
            engine.recordDamageContribution(victim.getUniqueId(), player, adjusted);
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!victim.isDead()) {
                engine.handlePostDamage(victim, definition, attacker);
            }
        });
    }

    @EventHandler(ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {

        LivingEntity entity = event.getEntity();

        MobDefinition definition = resolveDefinition(entity);
        if (definition == null) {
            return;
        }

        engine.onMobDeath(entity, definition, entity.getKiller());

        event.getDrops().clear();
        event.setDroppedExp(0);
    }

    private MobDefinition resolveDefinition(LivingEntity entity) {

        Optional<String> definitionId = instanceService.getDefinitionId(entity);
        return definitionId.isEmpty() ? null : mobManager.get(definitionId.get()).orElse(null);
    }

    private LivingEntity resolveAttacker(EntityDamageEvent event) {

        if (!(event instanceof EntityDamageByEntityEvent byEntity)) {
            return null;
        }

        Entity damager = byEntity.getDamager();

        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof LivingEntity shooter) {
            return shooter;
        }

        return damager instanceof LivingEntity livingDamager ? livingDamager : null;
    }

}
