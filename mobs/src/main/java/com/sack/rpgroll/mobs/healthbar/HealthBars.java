package com.sack.rpgroll.mobs.healthbar;

import com.sack.rpgroll.mobs.integration.ModelsIntegration;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Barra de vida sobre el mob que un jugador acaba de golpear: durante unos segundos su nombre
 * pasa a ser «nombre ▮▮▮▮▮▯▯▯▯▯ 45/100» y luego vuelve el de antes. Va en el nombre del mob (o en
 * el del modelo de FMM, si lo lleva), que Bedrock también ve.
 * <p>
 * El nombre original se guarda en el PDC del mob: si se descarga o el servidor se cae con la
 * barra puesta, se devuelve al volver a cargar (un mob vanilla con nombre no desaparece solo).
 */
public final class HealthBars implements Listener {

    private static final GsonComponentSerializer GSON = GsonComponentSerializer.gson();

    private final Plugin plugin;
    private final NamespacedKey nameKey;
    private final NamespacedKey visibleKey;
    private final Map<UUID, BukkitTask> pending = new HashMap<>();

    private boolean enabled = true;
    private int seconds = 5;
    private int segments = 10;
    private java.util.function.Function<LivingEntity, Component> prefix = mob -> null;

    public HealthBars(Plugin plugin) {
        this.plugin = plugin;
        this.nameKey = new NamespacedKey(plugin, "healthbar_name");
        this.visibleKey = new NamespacedKey(plugin, "healthbar_visible");
    }

    /** Lo que va delante del nombre (la rareza del mob), o null. */
    public void prefix(java.util.function.Function<LivingEntity, Component> prefix) {
        this.prefix = prefix;
    }

    /** Sección {@code health-bar} de config.yml. */
    public void configure(ConfigurationSection section) {
        enabled = section == null || section.getBoolean("enabled", true);
        seconds = section == null ? 5 : Math.max(1, section.getInt("seconds", 5));
        segments = section == null ? 10 : Math.max(4, Math.min(20, section.getInt("segments", 10)));
        if (!enabled) {
            restoreAll();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!enabled || !(event.getEntity() instanceof LivingEntity mob) || mob instanceof Player
                || mob instanceof ArmorStand || !byPlayer(event.getDamager())) {
            return;
        }
        // Un tick después, con la vida ya descontada.
        Bukkit.getScheduler().runTask(plugin, () -> show(mob));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeath(EntityDeathEvent event) {
        restore(event.getEntity());
    }

    @EventHandler
    public void onLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity instanceof LivingEntity mob && mob.getPersistentDataContainer().has(nameKey)) {
                restore(mob);
            }
        }
    }

    /** Al apagar: ningún mob se queda con la barra en el nombre. */
    public void restoreAll() {
        for (UUID id : new ArrayList<>(pending.keySet())) {
            if (Bukkit.getEntity(id) instanceof LivingEntity mob) {
                restore(mob);
            }
        }
        pending.values().forEach(BukkitTask::cancel);
        pending.clear();
    }

    private static boolean byPlayer(Entity damager) {
        return damager instanceof Player
                || damager instanceof Projectile projectile && projectile.getShooter() instanceof Player;
    }

    private void show(LivingEntity mob) {
        if (!mob.isValid() || mob.isDead()) {
            return;
        }

        PersistentDataContainer data = mob.getPersistentDataContainer();
        if (!data.has(nameKey)) {
            Component current = mob.customName();
            data.set(nameKey, PersistentDataType.STRING, current == null ? "" : GSON.serialize(current));
            data.set(visibleKey, PersistentDataType.BYTE, (byte) (mob.isCustomNameVisible() ? 1 : 0));
        }

        Component base = original(data);
        if (base == null) {
            base = Component.translatable(mob.getType().translationKey(), NamedTextColor.WHITE);
        }
        Component before = prefix.apply(mob);
        if (before != null) {
            base = before.append(Component.space()).append(base);
        }
        Component label = base.append(Component.space()).append(bar(mob.getHealth(), maxHealth(mob)));

        if (!ModelsIntegration.rename(mob, label)) {
            mob.customName(label);
            mob.setCustomNameVisible(true);
        }

        BukkitTask previous = pending.put(mob.getUniqueId(),
                Bukkit.getScheduler().runTaskLater(plugin, () -> restore(mob), seconds * 20L));
        if (previous != null) {
            previous.cancel();
        }
    }

    private void restore(LivingEntity mob) {
        BukkitTask task = pending.remove(mob.getUniqueId());
        if (task != null) {
            task.cancel();
        }
        PersistentDataContainer data = mob.getPersistentDataContainer();
        if (!data.has(nameKey)) {
            return;
        }
        Component name = original(data);
        boolean visible = data.getOrDefault(visibleKey, PersistentDataType.BYTE, (byte) 0) == 1;
        data.remove(nameKey);
        data.remove(visibleKey);

        mob.customName(name);
        mob.setCustomNameVisible(visible);
        // Con modelo, el nombre visible va en el modelo y el mob lo tiene oculto.
        ModelsIntegration.rename(mob, name);
    }

    private Component original(PersistentDataContainer data) {
        String json = data.get(nameKey, PersistentDataType.STRING);
        return json == null || json.isEmpty() ? null : GSON.deserialize(json);
    }

    private static double maxHealth(LivingEntity mob) {
        AttributeInstance max = mob.getAttribute(Attribute.MAX_HEALTH);
        return max == null ? mob.getHealth() : max.getValue();
    }

    Component bar(double health, double max) {
        int full = filled(health, max, segments);
        double ratio = max <= 0 ? 0 : health / max;
        NamedTextColor color = ratio > 0.5 ? NamedTextColor.GREEN : ratio > 0.25 ? NamedTextColor.YELLOW : NamedTextColor.RED;
        return Component.text()
                .append(Component.text("▮".repeat(full), color))
                .append(Component.text("▮".repeat(segments - full), NamedTextColor.DARK_GRAY))
                .append(Component.text(" " + (int) Math.ceil(Math.max(0, health)) + "/" + (int) Math.ceil(max),
                        NamedTextColor.GRAY))
                .build();
    }

    /** Cuántos segmentos van llenos: al menos uno mientras siga vivo. */
    static int filled(double health, double max, int segments) {
        if (health <= 0 || max <= 0) {
            return 0;
        }
        return Math.max(1, Math.min(segments, (int) Math.round(health / max * segments)));
    }
}
