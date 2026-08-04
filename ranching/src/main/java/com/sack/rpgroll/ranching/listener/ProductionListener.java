package com.sack.rpgroll.ranching.listener;

import com.sack.rpgroll.ranching.core.animal.Animal;
import com.sack.rpgroll.ranching.core.animal.AnimalManager;
import com.sack.rpgroll.ranching.core.breeds.Breed;
import com.sack.rpgroll.ranching.core.breeds.BreedManager;
import com.sack.rpgroll.ranching.core.genetics.Gene;
import com.sack.rpgroll.ranching.core.genetics.GeneManager;
import com.sack.rpgroll.ranching.core.health.Disease;
import com.sack.rpgroll.ranching.core.health.DiseaseManager;
import com.sack.rpgroll.ranching.core.production.ProductKeys;
import com.sack.rpgroll.ranching.core.production.ProductQuality;
import com.sack.rpgroll.ranching.core.production.ProductionEngine;
import com.sack.rpgroll.ranching.core.production.ProductionResult;
import com.sack.rpgroll.ranching.core.species.Species;
import com.sack.rpgroll.ranching.core.species.SpeciesManager;
import com.sack.rpgroll.ranching.integration.SeasonsIntegration;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Sheep;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Engancha la producción a los mecanismos vanilla que ya existen para
 * cada tipo de recurso, en vez de reinventar una mecánica de "recolectar"
 * propia: ordeñar con balde ({@link PlayerInteractEntityEvent}), esquilar
 * ({@link PlayerShearEntityEvent}), poner huevos ({@link EntityDropItemEvent}),
 * y carne/cuero/etc. al morir ({@link EntityDeathEvent}) para cualquier
 * clave de {@code base-production} que no sea leche/lana/huevos.
 */
public class ProductionListener implements Listener {

    private final AnimalManager animalManager;
    private final SpeciesManager speciesManager;
    private final BreedManager breedManager;
    private final GeneManager geneManager;
    private final DiseaseManager diseaseManager;
    private final ProductionEngine productionEngine = new ProductionEngine();

    public ProductionListener(AnimalManager animalManager, SpeciesManager speciesManager, BreedManager breedManager,
            GeneManager geneManager, DiseaseManager diseaseManager) {
        this.animalManager = animalManager;
        this.speciesManager = speciesManager;
        this.breedManager = breedManager;
        this.geneManager = geneManager;
        this.diseaseManager = diseaseManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        ItemStack hand = event.getPlayer().getInventory().getItemInMainHand();

        if (hand.getType() != Material.BUCKET) {
            return;
        }

        Animal animal = animalManager.resolve(event.getRightClicked()).orElse(null);

        if (animal == null) {
            return;
        }

        Optional<ProductionResult> result = resolve(animal, event.getRightClicked().getLocation(), "milk");

        if (result.isEmpty()) {
            return;
        }

        event.setCancelled(true);

        ItemStack milk = tag(new ItemStack(Material.MILK_BUCKET), result.get());
        hand.setAmount(hand.getAmount() - 1);
        event.getPlayer().getInventory().addItem(milk).values()
                .forEach(leftover -> event.getPlayer().getWorld().dropItemNaturally(event.getPlayer().getLocation(), leftover));

        event.getPlayer().sendMessage(Component.text("✔ Leche " + result.get().quality() + " obtenida.", NamedTextColor.GREEN));
    }

    @EventHandler
    public void onShear(PlayerShearEntityEvent event) {

        Animal animal = animalManager.resolve(event.getEntity()).orElse(null);

        if (animal == null) {
            return;
        }

        Optional<ProductionResult> result = resolve(animal, event.getEntity().getLocation(), "wool");

        if (result.isEmpty()) {
            return;
        }

        event.setCancelled(true);

        Material woolMaterial = event.getEntity() instanceof Sheep sheep
                ? woolMaterialFor(sheep)
                : Material.WHITE_WOOL;

        int amount = Math.max(1, (int) Math.round(result.get().amount()));
        ItemStack wool = tag(new ItemStack(woolMaterial, amount), result.get());

        if (event.getEntity() instanceof Sheep sheep) {
            sheep.setSheared(true);
        }

        event.getEntity().getWorld().dropItemNaturally(event.getEntity().getLocation(), wool);
        event.getPlayer().sendMessage(Component.text("✔ Lana " + result.get().quality() + " obtenida.", NamedTextColor.GREEN));
    }

    @EventHandler
    public void onDropItem(EntityDropItemEvent event) {

        if (!(event.getEntity() instanceof Chicken) || event.getItemDrop().getItemStack().getType() != Material.EGG) {
            return;
        }

        Animal animal = animalManager.resolve(event.getEntity()).orElse(null);

        if (animal == null) {
            return;
        }

        Optional<ProductionResult> result = resolve(animal, event.getEntity().getLocation(), "eggs");

        if (result.isEmpty()) {
            return;
        }

        int amount = Math.max(1, (int) Math.round(result.get().amount()));
        event.getItemDrop().setItemStack(tag(new ItemStack(Material.EGG, amount), result.get()));
    }

    @EventHandler
    public void onDeath(EntityDeathEvent event) {

        Animal animal = animalManager.resolve(event.getEntity()).orElse(null);

        if (animal == null) {
            return;
        }

        Species species = speciesManager.get(animal.speciesId()).orElse(null);

        if (species == null) {
            return;
        }

        for (String productType : species.productTypes()) {

            if (productType.equals("milk") || productType.equals("wool") || productType.equals("eggs")) {
                continue;
            }

            resolve(animal, event.getEntity().getLocation(), productType).ifPresent(result -> {

                int amount = Math.max(1, (int) Math.round(result.amount()));
                Material material = materialForProduct(productType);

                if (material != null) {
                    event.getDrops().add(tag(new ItemStack(material, amount), result));
                }
            });
        }
    }

    private Optional<ProductionResult> resolve(Animal animal, org.bukkit.Location location, String productType) {

        Species species = speciesManager.get(animal.speciesId()).orElse(null);

        if (species == null) {
            return Optional.empty();
        }

        Breed breed = animal.breedId() != null ? breedManager.get(animal.breedId()).orElse(null) : null;
        List<Gene> genes = geneManager.getForSpecies(species.id());
        Disease disease = animal.isSick() ? diseaseManager.get(animal.activeDiseaseId()).orElse(null) : null;
        Double temperature = SeasonsIntegration.temperature(location);

        return productionEngine.produce(animal, species, breed, genes, productType, temperature, disease);
    }

    private ItemStack tag(ItemStack item, ProductionResult result) {

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.getPersistentDataContainer().set(ProductKeys.QUALITY, org.bukkit.persistence.PersistentDataType.STRING,
                result.quality().name());
        meta.getPersistentDataContainer().set(ProductKeys.PRODUCT_TYPE, org.bukkit.persistence.PersistentDataType.STRING,
                result.productType());

        meta.lore(List.of(Component.text("Calidad: " + result.quality(), qualityColor(result.quality()))));
        item.setItemMeta(meta);

        return item;
    }

    private NamedTextColor qualityColor(ProductQuality quality) {
        return switch (quality) {
            case COMMON -> NamedTextColor.GRAY;
            case GOOD -> NamedTextColor.GREEN;
            case PREMIUM -> NamedTextColor.AQUA;
            case ORGANIC -> NamedTextColor.GOLD;
            case LEGENDARY -> NamedTextColor.LIGHT_PURPLE;
        };
    }

    private Material woolMaterialFor(Sheep sheep) {

        try {
            return Material.valueOf(sheep.getColor().name() + "_WOOL");
        } catch (IllegalArgumentException e) {
            return Material.WHITE_WOOL;
        }
    }

    private Material materialForProduct(String productType) {

        return switch (productType.toLowerCase(Locale.ROOT)) {
            case "meat" -> Material.COOKED_BEEF;
            case "leather" -> Material.LEATHER;
            case "horns" -> Material.BONE;
            case "feathers" -> Material.FEATHER;
            default -> null;
        };
    }

}
