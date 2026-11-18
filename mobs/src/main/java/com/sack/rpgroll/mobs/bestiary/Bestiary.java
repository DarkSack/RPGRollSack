package com.sack.rpgroll.mobs.bestiary;

import com.sack.rpgroll.mobs.core.MobDefinition;
import com.sack.rpgroll.mobs.core.MobManager;
import com.sack.rpgroll.mobs.rarity.MobRarities;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Enemy;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * El bestiario («pokédex» de mobs): cuántos de cada especie ha matado cada jugador y de qué rareza.
 * Se guarda en el PDC del jugador ({@code rpgroll-mobs:bestiary}, una lista de enteros por especie
 * con una casilla por rareza, en el orden de {@code rarities.tiers}), así viaja con su playerdata.
 */
public final class Bestiary implements Listener {

    /** Una entrada del bestiario: un mob vanilla o uno de RPGRoll-Mobs. */
    public record Species(String key, Component name, Material icon, boolean hostile) {
    }

    private final Plugin plugin;
    private final MobManager mobs;
    private final MobRarities rarities;
    private final NamespacedKey root;
    private final NamespacedKey definitionKey;

    public Bestiary(Plugin plugin, MobManager mobs, MobRarities rarities) {
        this.plugin = plugin;
        this.mobs = mobs;
        this.rarities = rarities;
        this.root = new NamespacedKey(plugin, "bestiary");
        this.definitionKey = new NamespacedKey(plugin, "mob-definition-id");
    }

    public MobRarities rarities() {
        return rarities;
    }

    /** Clave de especie: {@code zombie}, o {@code rpgroll.<id>} para los mobs de RPGRoll-Mobs. */
    public String speciesOf(LivingEntity entity) {
        String definition = entity.getPersistentDataContainer().get(definitionKey, PersistentDataType.STRING);
        return definition != null ? rpgrollKey(definition) : entity.getType().getKey().getKey();
    }

    static String rpgrollKey(String definitionId) {
        return "rpgroll." + definitionId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        LivingEntity entity = event.getEntity();
        Player killer = entity.getKiller();
        if (killer == null || entity instanceof Player) {
            return;
        }
        record(killer, speciesOf(entity), rarities.indexOf(entity));
    }

    void record(Player player, String species, int tier) {
        PersistentDataContainer data = player.getPersistentDataContainer();
        PersistentDataContainer book = data.getOrDefault(root, PersistentDataType.TAG_CONTAINER,
                data.getAdapterContext().newPersistentDataContainer());
        NamespacedKey key = new NamespacedKey(plugin, species);
        book.set(key, PersistentDataType.INTEGER_ARRAY, add(book.get(key, PersistentDataType.INTEGER_ARRAY), tier));
        data.set(root, PersistentDataType.TAG_CONTAINER, book);
    }

    /** Suma una baja en la casilla {@code tier}, agrandando la lista si hace falta. */
    static int[] add(int[] counts, int tier) {
        int[] out = new int[Math.max(tier + 1, counts == null ? 0 : counts.length)];
        if (counts != null) {
            System.arraycopy(counts, 0, out, 0, counts.length);
        }
        out[tier]++;
        return out;
    }

    /** Bajas del jugador para la especie, una casilla por rareza (vacío si nunca mató ninguno). */
    public int[] counts(Player player, String species) {
        PersistentDataContainer book = player.getPersistentDataContainer().get(root, PersistentDataType.TAG_CONTAINER);
        if (book == null) {
            return new int[0];
        }
        int[] counts = book.get(new NamespacedKey(plugin, species), PersistentDataType.INTEGER_ARRAY);
        return counts == null ? new int[0] : counts;
    }

    static int total(int[] counts) {
        int sum = 0;
        for (int c : counts) {
            sum += c;
        }
        return sum;
    }

    /** Todas las especies: primero las de RPGRoll-Mobs, luego las hostiles vanilla y luego el resto. */
    public List<Species> species() {
        List<Species> out = new ArrayList<>();
        for (MobDefinition definition : mobs.getAll()) {
            out.add(new Species(rpgrollKey(definition.id()), ComponentUtils.parse(definition.displayName()),
                    egg(definition.model().baseEntityType()), true));
        }
        List<Species> vanilla = new ArrayList<>();
        for (EntityType type : EntityType.values()) {
            Material egg = Material.matchMaterial(type.name() + "_SPAWN_EGG");
            if (type == EntityType.PLAYER || type == EntityType.ARMOR_STAND || !type.isAlive() || egg == null
                    || type.getEntityClass() == null) {
                continue;
            }
            vanilla.add(new Species(type.getKey().getKey(), Component.translatable(type.translationKey()), egg,
                    Enemy.class.isAssignableFrom(type.getEntityClass())));
        }
        vanilla.sort(Comparator.comparing((Species s) -> !s.hostile()).thenComparing(Species::key));
        out.addAll(vanilla);
        return out;
    }

    private static Material egg(String baseType) {
        Material egg = baseType == null ? null : Material.matchMaterial(baseType.toUpperCase(Locale.ROOT) + "_SPAWN_EGG");
        return egg == null ? Material.ZOMBIE_SPAWN_EGG : egg;
    }
}
