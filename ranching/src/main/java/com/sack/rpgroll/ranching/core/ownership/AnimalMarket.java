package com.sack.rpgroll.ranching.core.ownership;

import com.sack.rpgroll.common.integration.VaultEconomy;

import com.sack.rpgroll.ranching.core.animal.Animal;
import com.sack.rpgroll.ranching.core.animal.AnimalManager;
import com.sack.rpgroll.ranching.core.breeds.Breed;
import com.sack.rpgroll.ranching.core.breeds.BreedManager;
import com.sack.rpgroll.ranching.core.genetics.GeneManager;
import com.sack.rpgroll.ranching.core.genetics.GeneticsEngine;
import com.sack.rpgroll.ranching.core.species.Sex;
import com.sack.rpgroll.ranching.core.species.Species;
import com.sack.rpgroll.ranching.core.species.SpeciesManager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Random;
import java.util.UUID;

/**
 * Comprar y vender animales, con el dinero de Vault: entre jugadores (el dueño pone precio y
 * cualquiera lo compra) y con el servidor (fundadores nuevos de {@code market.server-shop}, y la venta
 * de un animal propio por {@code market.sell-to-server} × su calidad).
 */
public class AnimalMarket {

    public enum Result {
        OK, NO_ECONOMY, DISABLED, NOT_FOR_SALE, OWN_ANIMAL, NOT_OWNER, NO_MONEY, LIMIT, BAD_PRICE, NOT_SELLABLE, NO_SPECIES
    }

    private final AnimalManager animalManager;
    private final SpeciesManager speciesManager;
    private final BreedManager breedManager;
    private final GeneManager geneManager;
    private final GeneticsEngine geneticsEngine;
    private final Random random = new Random();
    private OwnershipSettings settings;

    public AnimalMarket(AnimalManager animalManager, SpeciesManager speciesManager, BreedManager breedManager,
            GeneManager geneManager, GeneticsEngine geneticsEngine, OwnershipSettings settings) {
        this.animalManager = animalManager;
        this.speciesManager = speciesManager;
        this.breedManager = breedManager;
        this.geneManager = geneManager;
        this.geneticsEngine = geneticsEngine;
        this.settings = settings;
    }

    public void configure(OwnershipSettings settings) {
        this.settings = settings;
    }

    public OwnershipSettings settings() {
        return settings;
    }

    /** Si el jugador ya tiene tantos animales como permite {@code ownership.max-animals-per-player}. */
    public boolean atLimit(UUID playerId) {
        return settings.maxAnimalsPerPlayer() > 0 && animalManager.ownedBy(playerId).size() >= settings.maxAnimalsPerPlayer();
    }

    /** Pone en venta (o quita de la venta con precio 0) un animal propio. */
    public Result list(Player owner, Animal animal, double price) {

        if (!settings.marketEnabled()) {
            return Result.DISABLED;
        }
        if (!animal.isOwnedBy(owner.getUniqueId())) {
            return Result.NOT_OWNER;
        }
        if (price != 0 && (price < settings.minPrice() || price > settings.maxPrice() || Double.isNaN(price))) {
            return Result.BAD_PRICE;
        }

        animal.setSalePrice(price);
        animalManager.save(animal);
        return Result.OK;
    }

    public Result buy(Player buyer, Animal animal) {

        if (!settings.marketEnabled()) {
            return Result.DISABLED;
        }
        if (!animal.isForSale()) {
            return Result.NOT_FOR_SALE;
        }
        if (animal.isOwnedBy(buyer.getUniqueId())) {
            return Result.OWN_ANIMAL;
        }
        if (atLimit(buyer.getUniqueId())) {
            return Result.LIMIT;
        }

        var economy = VaultEconomy.get().orElse(null);

        if (economy == null) {
            return Result.NO_ECONOMY;
        }

        double price = animal.salePrice();

        if (!economy.has(buyer, price) || !economy.withdrawPlayer(buyer, price).transactionSuccess()) {
            return Result.NO_MONEY;
        }

        UUID sellerId = animal.ownerId();

        if (sellerId != null) {
            OfflinePlayer seller = Bukkit.getOfflinePlayer(sellerId);
            economy.depositPlayer(seller, price * (1 - settings.taxPercent() / 100.0));
        }

        animal.setOwnerId(buyer.getUniqueId());
        animal.setSalePrice(0);
        animalManager.save(animal);
        return Result.OK;
    }

    /** Lo que pagaría el servidor por este animal; 0 si no lo compra. */
    public double serverPrice(Animal animal) {
        double base = settings.sellToServer().getOrDefault(animal.speciesId().toLowerCase(Locale.ROOT), 0.0);
        return Math.round(base * animal.quality().priceMultiplier() * 100) / 100.0;
    }

    /** Vende un animal propio al servidor: se cobra y el animal desaparece. */
    public Result sellToServer(Player owner, Animal animal) {

        if (!settings.marketEnabled()) {
            return Result.DISABLED;
        }
        if (!animal.isOwnedBy(owner.getUniqueId())) {
            return Result.NOT_OWNER;
        }

        double price = serverPrice(animal);

        if (price <= 0) {
            return Result.NOT_SELLABLE;
        }

        var economy = VaultEconomy.get().orElse(null);

        if (economy == null) {
            return Result.NO_ECONOMY;
        }

        if (!economy.depositPlayer(owner, price).transactionSuccess()) {
            return Result.NO_ECONOMY;
        }

        Entity entity = animalManager.entityOf(animal).orElse(null);

        if (entity instanceof LivingEntity living) {
            com.sack.rpgroll.common.reskin.EntityReskinService.remove(living);
            com.sack.rpgroll.ranching.integration.ModelsIntegration.remove(living);
        }
        if (entity != null) {
            entity.remove();
        }

        animalManager.remove(animal.id());
        return Result.OK;
    }

    /** Compra al servidor uno de {@code market.server-shop}: un fundador nuevo, suyo, a sus pies. */
    public Result buyFromServer(Player buyer, OwnershipSettings.ServerOffer offer) {

        if (!settings.marketEnabled()) {
            return Result.DISABLED;
        }
        if (atLimit(buyer.getUniqueId())) {
            return Result.LIMIT;
        }

        Species species = speciesManager.get(offer.speciesId()).orElse(null);

        if (species == null) {
            return Result.NO_SPECIES;
        }

        var economy = VaultEconomy.get().orElse(null);

        if (economy == null) {
            return Result.NO_ECONOMY;
        }

        if (!economy.has(buyer, offer.price()) || !economy.withdrawPlayer(buyer, offer.price()).transactionSuccess()) {
            return Result.NO_MONEY;
        }

        Breed breed = offer.breedId() == null ? null : breedManager.get(offer.breedId()).orElse(null);
        Sex sex = offer.sex() == null ? (random.nextBoolean() ? Sex.MALE : Sex.FEMALE) : parseSex(offer.sex());
        Location location = buyer.getLocation();
        LivingEntity entity = (LivingEntity) location.getWorld().spawnEntity(location,
                animalManager.resolveEntityType(species));

        animalManager.registerFounder(entity, species, breed, sex, geneticsEngine,
                geneManager.getForSpecies(species.id()), buyer.getUniqueId());
        return Result.OK;
    }

    private Sex parseSex(String raw) {
        try {
            return Sex.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return random.nextBoolean() ? Sex.MALE : Sex.FEMALE;
        }
    }

    /** El dinero con el formato de la economía de Vault (o el número a secas si no hay). */
    public static String format(double amount) {
        return VaultEconomy.get().map(economy -> economy.format(amount))
                .orElse(String.format(Locale.ROOT, "%.2f", amount));
    }

}
