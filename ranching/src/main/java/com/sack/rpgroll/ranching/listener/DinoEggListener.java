package com.sack.rpgroll.ranching.listener;

import com.sack.rpgroll.common.lang.LangManager;

import com.sack.rpgroll.ranching.core.animal.AnimalManager;
import com.sack.rpgroll.ranching.core.breeds.Breed;
import com.sack.rpgroll.ranching.core.breeds.BreedManager;
import com.sack.rpgroll.ranching.core.genetics.GeneManager;
import com.sack.rpgroll.ranching.core.genetics.GeneticsEngine;
import com.sack.rpgroll.ranching.core.species.Sex;
import com.sack.rpgroll.ranching.core.species.Species;
import com.sack.rpgroll.ranching.core.species.SpeciesManager;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sniffer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

/**
 * El huevo fosilizado. Rarísimo: a veces lo pone una gallina (o pato) del rancho en lugar de un
 * huevo, y algo menos rara vez lo desentierra un sniffer. Usado sobre un bloque, tiembla, se
 * agrieta y sale un mini T-Rex que es de quien lo incubó. Sección {@code secret} del config.yml.
 */
public class DinoEggListener implements Listener {

    public static final NamespacedKey KEY = new NamespacedKey("rpgrollranching", "dino-egg");

    private final Plugin plugin;
    private final AnimalManager animalManager;
    private final SpeciesManager speciesManager;
    private final BreedManager breedManager;
    private final GeneManager geneManager;
    private final GeneticsEngine geneticsEngine;
    private final LangManager lang;
    private final Random random = new Random();
    private double layChance;
    private double snifferChance;
    private String speciesId;
    private String breedId;

    public DinoEggListener(Plugin plugin, AnimalManager animalManager, SpeciesManager speciesManager,
            BreedManager breedManager, GeneManager geneManager, GeneticsEngine geneticsEngine, LangManager lang) {
        this.plugin = plugin;
        this.animalManager = animalManager;
        this.speciesManager = speciesManager;
        this.breedManager = breedManager;
        this.geneManager = geneManager;
        this.geneticsEngine = geneticsEngine;
        this.lang = lang;
    }

    public void configure(ConfigurationSection secret) {
        layChance = secret == null ? 0.0005 : secret.getDouble("dino-egg-chance", 0.0005);
        snifferChance = secret == null ? 0.02 : secret.getDouble("sniffer-dino-egg-chance", 0.02);
        speciesId = secret == null ? "trex" : secret.getString("species", "trex");
        breedId = secret == null ? "tiranosaurio" : secret.getString("breed", "tiranosaurio");
    }

    public static ItemStack createEgg(LangManager lang) {

        ItemStack egg = new ItemStack(Material.SNIFFER_EGG);
        ItemMeta meta = egg.getItemMeta();
        meta.displayName(lang.component("secret.egg_name"));
        meta.lore(com.sack.rpgroll.gui.util.ItemBuilder.toLoreLines(lang.raw("secret.egg_lore")));
        meta.getPersistentDataContainer().set(KEY, PersistentDataType.BOOLEAN, true);
        meta.setMaxStackSize(1);
        egg.setItemMeta(meta);
        return egg;
    }

    public static boolean isEgg(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(KEY, PersistentDataType.BOOLEAN);
    }

    // HIGHEST: después de que ProductionListener marque el huevo con su calidad.
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(EntityDropItemEvent event) {

        double chance;

        if (event.getEntity() instanceof Sniffer) {
            chance = snifferChance;
        } else if (event.getEntity() instanceof Chicken && event.getItemDrop().getItemStack().getType() == Material.EGG
                && animalManager.resolve(event.getEntity()).isPresent()) {
            chance = layChance;
        } else {
            return;
        }

        if (chance <= 0 || random.nextDouble() >= chance) {
            return;
        }

        event.getItemDrop().setItemStack(createEgg(lang));
        Location at = event.getItemDrop().getLocation();
        at.getWorld().playSound(at, Sound.BLOCK_SNIFFER_EGG_CRACK, 1f, 0.6f);
        at.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, at, 12, 0.4, 0.3, 0.4);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onUse(PlayerInteractEvent event) {

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getHand() != EquipmentSlot.HAND
                || event.getClickedBlock() == null || !isEgg(event.getItem())) {
            return;
        }

        // No se coloca como un huevo de sniffer: se incuba.
        event.setCancelled(true);

        Player player = event.getPlayer();
        Species species = speciesManager.get(speciesId).orElse(null);

        if (species == null) {
            lang.send(player, "secret.no_species");
            return;
        }

        event.getItem().setAmount(event.getItem().getAmount() - 1);
        Location spot = event.getClickedBlock().getRelative(event.getBlockFace()).getLocation().add(0.5, 0, 0.5);
        lang.send(player, "secret.hatching");
        hatch(player, spot, species);
    }

    private void hatch(Player player, Location spot, Species species) {

        new BukkitRunnable() {

            private int step;

            @Override
            public void run() {

                if (step < 4) {
                    spot.getWorld().playSound(spot, Sound.BLOCK_SNIFFER_EGG_CRACK, 1f, 0.8f + step * 0.1f);
                    spot.getWorld().spawnParticle(Particle.BLOCK, spot.clone().add(0, 0.3, 0), 15, 0.2, 0.2, 0.2,
                            Material.SNIFFER_EGG.createBlockData());
                    step++;
                    return;
                }

                cancel();
                spot.getWorld().playSound(spot, Sound.BLOCK_SNIFFER_EGG_HATCH, 1f, 0.7f);
                spot.getWorld().playSound(spot, Sound.ENTITY_RAVAGER_ROAR, 0.6f, 1.8f);
                spot.getWorld().spawnParticle(Particle.EXPLOSION, spot.clone().add(0, 0.5, 0), 2);

                Breed breed = breedManager.get(breedId).orElse(null);
                LivingEntity entity = (LivingEntity) spot.getWorld().spawnEntity(spot,
                        animalManager.resolveEntityType(species));
                Sex sex = random.nextBoolean() ? Sex.MALE : Sex.FEMALE;
                animalManager.registerFounder(entity, species, breed, sex, geneticsEngine,
                        geneManager.getForSpecies(species.id()), player.getUniqueId());

                if (player.isOnline()) {
                    lang.send(player, "secret.hatched", "sex", sex);
                }
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

}
