package com.sack.rpgroll.mobs.rarity;

import com.sack.rpgroll.common.integration.VaultEconomy;
import com.sack.rpgroll.common.lang.LangManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Rarezas de los mobs vanilla: al aparecer de forma natural cada mob sale común, raro, legendario
 * o galáctico (los pesos y los multiplicadores van en {@code rarities} de config.yml). Los raros
 * aguantan y pegan más y, si los mata un jugador, sueltan más botín, más experiencia y dinero.
 * <p>
 * No se les pone nombre (un mob vanilla con nombre no desaparece y llenaría el mundo): la rareza se
 * ve en la barra de vida al golpearlo, en el brillo, en el tamaño y en las partículas del galáctico.
 * Los de spawner no tienen rareza, para que una granja no sea una fábrica de botín.
 */
public final class MobRarities implements Listener {

    /** Un nivel de rareza. El primero de la lista es el común (sin cambios). */
    public record Tier(String id, Component name, double weight, double health, double damage, double loot,
            double xp, double money, double scale, boolean glow, boolean particles, boolean announce) {
    }

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final Plugin plugin;
    private final LangManager lang;
    private final NamespacedKey key;
    private final NamespacedKey definitionKey;
    private final Set<UUID> sparkling = new HashSet<>();

    private boolean enabled;
    private boolean hostileOnly;
    private Set<String> reasons = Set.of();
    private Set<String> excluded = Set.of();
    private List<Tier> tiers = List.of();

    public MobRarities(Plugin plugin, LangManager lang) {
        this.plugin = plugin;
        this.lang = lang;
        this.key = new NamespacedKey(plugin, "rarity");
        this.definitionKey = new NamespacedKey(plugin, "mob-definition-id");
        Bukkit.getScheduler().runTaskTimer(plugin, this::sparkle, 20L, 10L);
    }

    /** Sección {@code rarities} de config.yml; sin ella, apagado. */
    public void configure(ConfigurationSection section) {
        enabled = section != null && section.getBoolean("enabled", true);
        if (section == null) {
            tiers = List.of();
            return;
        }
        hostileOnly = !"all".equalsIgnoreCase(section.getString("applies-to", "hostile"));
        reasons = upper(section.getStringList("spawn-reasons"));
        excluded = lower(section.getStringList("exclude"));
        List<Tier> loaded = new ArrayList<>();
        ConfigurationSection list = section.getConfigurationSection("tiers");
        if (list != null) {
            for (String id : list.getKeys(false)) {
                ConfigurationSection t = list.getConfigurationSection(id);
                if (t == null) {
                    continue;
                }
                loaded.add(new Tier(id, LEGACY.deserialize(t.getString("name", id)).decoration(TextDecoration.ITALIC, false),
                        Math.max(0, t.getDouble("weight", 0)), positive(t, "health"), positive(t, "damage"),
                        positive(t, "loot"), positive(t, "xp"), Math.max(0, t.getDouble("money", 0)), positive(t, "scale"),
                        t.getBoolean("glow", false), t.getBoolean("particles", false), t.getBoolean("announce", false)));
            }
        }
        tiers = List.copyOf(loaded);
    }

    private static double positive(ConfigurationSection s, String path) {
        return Math.max(0.1, s.getDouble(path, 1.0));
    }

    private static Set<String> upper(List<String> values) {
        Set<String> out = new HashSet<>();
        values.forEach(v -> out.add(v.toUpperCase(Locale.ROOT)));
        return out;
    }

    private static Set<String> lower(List<String> values) {
        Set<String> out = new HashSet<>();
        values.forEach(v -> out.add(v.toLowerCase(Locale.ROOT).replace("minecraft:", "")));
        return out;
    }

    public List<Tier> tiers() {
        return tiers;
    }

    /** La rareza del mob, o null si no tiene (común, de spawner, de RPGRoll-Mobs...). */
    public Tier tierOf(LivingEntity entity) {
        String id = entity.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        if (id == null) {
            return null;
        }
        for (Tier tier : tiers) {
            if (tier.id().equals(id)) {
                return tier;
            }
        }
        return null;
    }

    /** Posición de la rareza del mob en la lista (0 = común), para el bestiario. */
    public int indexOf(LivingEntity entity) {
        Tier tier = tierOf(entity);
        return tier == null ? 0 : Math.max(0, tiers.indexOf(tier));
    }

    /** Elige rareza con {@code roll} en [0, 1): por peso, en el orden de la lista. */
    static Tier pick(List<Tier> tiers, double roll) {
        double total = tiers.stream().mapToDouble(Tier::weight).sum();
        if (total <= 0) {
            return null;
        }
        double at = roll * total;
        for (Tier tier : tiers) {
            at -= tier.weight();
            if (at < 0) {
                return tier;
            }
        }
        return tiers.get(tiers.size() - 1);
    }

    // ----- Aparecer -----

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        LivingEntity entity = event.getEntity();
        if (!enabled || tiers.size() < 2 || !(entity instanceof Mob)
                || (hostileOnly && !(entity instanceof Enemy))
                || (!reasons.isEmpty() && !reasons.contains(event.getSpawnReason().name()))
                || excluded.contains(entity.getType().getKey().getKey())
                || entity.customName() != null
                || (entity instanceof Tameable tameable && tameable.isTamed())
                || entity.getPersistentDataContainer().has(definitionKey)
                || entity.getPersistentDataContainer().has(key)) {
            return;
        }
        Tier tier = pick(tiers, ThreadLocalRandom.current().nextDouble());
        if (tier == null || tier == tiers.get(0)) {
            return;
        }
        apply(entity, tier);
    }

    /** Pone la rareza a un mob (también desde /mobadmin, para probar). */
    public void apply(LivingEntity entity, Tier tier) {
        entity.getPersistentDataContainer().set(key, PersistentDataType.STRING, tier.id());
        multiply(entity, Attribute.MAX_HEALTH, tier.health());
        multiply(entity, Attribute.ATTACK_DAMAGE, tier.damage());
        multiply(entity, Attribute.SCALE, tier.scale());
        AttributeInstance max = entity.getAttribute(Attribute.MAX_HEALTH);
        if (max != null) {
            entity.setHealth(max.getValue());
        }
        entity.setGlowing(tier.glow());
        if (tier.particles()) {
            sparkling.add(entity.getUniqueId());
        }
        if (tier.announce()) {
            Location at = entity.getLocation();
            Component mob = Component.translatable(entity.getType().translationKey());
            for (Player player : entity.getWorld().getPlayers()) {
                if (player.getLocation().distanceSquared(at) < 64 * 64) {
                    player.sendMessage(lang.component("rarity.spawned_near", "rarity", tier.name(), "mob", mob));
                }
            }
        }
    }

    private static void multiply(LivingEntity entity, Attribute attribute, double factor) {
        AttributeInstance instance = entity.getAttribute(attribute);
        if (instance != null && Math.abs(factor - 1) > 0.001) {
            instance.setBaseValue(instance.getBaseValue() * factor);
        }
    }

    @EventHandler
    public void onLoad(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity instanceof LivingEntity living) {
                Tier tier = tierOf(living);
                if (tier != null && tier.particles()) {
                    sparkling.add(entity.getUniqueId());
                }
            }
        }
    }

    private void sparkle() {
        sparkling.removeIf(id -> {
            Entity entity = Bukkit.getEntity(id);
            if (entity == null || !entity.isValid()) {
                return true;
            }
            Location at = entity.getLocation().add(0, entity.getHeight() * 0.6, 0);
            entity.getWorld().spawnParticle(Particle.END_ROD, at, 2, 0.4, 0.5, 0.4, 0.01);
            entity.getWorld().spawnParticle(Particle.PORTAL, at, 6, 0.4, 0.5, 0.4, 0.2);
            return false;
        });
    }

    // ----- Morir: botín, experiencia y dinero -----

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        sparkling.remove(entity.getUniqueId());
        Tier tier = tierOf(entity);
        Player killer = entity.getKiller();
        if (tier == null || killer == null) {
            return;
        }
        for (ItemStack drop : event.getDrops()) {
            drop.setAmount(Math.min(drop.getMaxStackSize(), (int) Math.round(drop.getAmount() * tier.loot())));
        }
        event.setDroppedExp((int) Math.round(event.getDroppedExp() * tier.xp()));
        if (tier.money() > 0) {
            VaultEconomy.get().ifPresent(economy -> {
                if (economy.depositPlayer(killer, tier.money()).transactionSuccess()) {
                    killer.sendActionBar(lang.component("loot.money", "amount", economy.format(tier.money())));
                }
            });
        }
        if (tier.announce()) {
            Component mob = Component.translatable(entity.getType().translationKey());
            Bukkit.broadcast(lang.component("rarity.killed_broadcast", "player", killer.getName(), "rarity", tier.name(),
                    "mob", mob));
        }
    }
}
