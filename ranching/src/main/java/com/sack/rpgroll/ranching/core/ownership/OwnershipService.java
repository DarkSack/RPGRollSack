package com.sack.rpgroll.ranching.core.ownership;

import com.sack.rpgroll.common.lang.LangManager;

import com.sack.rpgroll.ranching.core.animal.Animal;
import com.sack.rpgroll.ranching.core.animal.AnimalManager;
import com.sack.rpgroll.ranching.core.breeds.Breed;
import com.sack.rpgroll.ranching.core.breeds.BreedManager;
import com.sack.rpgroll.ranching.core.species.Species;
import com.sack.rpgroll.ranching.core.species.SpeciesManager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Lo que un jugador hace con sus animales —reclamar, llamar, mandar al corral, vender, comprar—, con
 * sus comprobaciones y mensajes. Lo usan el comando, los menús y el listener, así se comportan igual.
 */
public class OwnershipService {

    public static final String BYPASS_PERMISSION = "rpgrollranching.bypass-owner";

    private final AnimalManager animalManager;
    private final SpeciesManager speciesManager;
    private final BreedManager breedManager;
    private final PenManager pens;
    private final AnimalRecall recall;
    private final AnimalMarket market;
    private final LangManager lang;
    private final Map<UUID, Long> lastRecall = new HashMap<>();

    public OwnershipService(AnimalManager animalManager, SpeciesManager speciesManager, BreedManager breedManager,
            PenManager pens, AnimalRecall recall, AnimalMarket market, LangManager lang) {
        this.animalManager = animalManager;
        this.speciesManager = speciesManager;
        this.breedManager = breedManager;
        this.pens = pens;
        this.recall = recall;
        this.market = market;
        this.lang = lang;
    }

    public AnimalManager animals() {
        return animalManager;
    }

    public SpeciesManager species() {
        return speciesManager;
    }

    public BreedManager breeds() {
        return breedManager;
    }

    public PenManager pens() {
        return pens;
    }

    public AnimalMarket market() {
        return market;
    }

    public LangManager lang() {
        return lang;
    }

    public OwnershipSettings settings() {
        return market.settings();
    }

    /** Puede tocar el animal: es suyo, no es de nadie, o tiene el permiso de saltárselo. */
    public boolean mayHandle(Player player, Animal animal) {
        return animal.ownerId() == null || animal.isOwnedBy(player.getUniqueId())
                || player.hasPermission(BYPASS_PERMISSION);
    }

    /** "Vaca Holstein #1a2b3c4d" */
    public String describe(Animal animal) {

        String species = speciesManager.get(animal.speciesId()).map(Species::displayName).orElse(animal.speciesId());
        String breed = animal.breedId() == null ? null
                : breedManager.get(animal.breedId()).map(Breed::displayName).orElse(null);

        return species + (breed != null ? " " + breed : "") + " &7#" + shortId(animal);
    }

    public static String shortId(Animal animal) {
        return animal.id().toString().substring(0, 8);
    }

    public String ownerName(Animal animal) {

        if (animal.ownerId() == null) {
            return lang.raw("owner.nobody");
        }

        OfflinePlayer owner = Bukkit.getOfflinePlayer(animal.ownerId());
        return owner.getName() != null ? owner.getName() : shortUuid(animal.ownerId());
    }

    private static String shortUuid(UUID id) {
        return id.toString().substring(0, 8);
    }

    /** Dónde está (o dónde se le vio por última vez), con la distancia si es el mismo mundo. */
    public String whereIs(Animal animal, Player viewer) {

        Location location = animalManager.entityOf(animal).map(entity -> entity.getLocation()).orElse(null);

        if (location == null && animal.lastSeen() != null) {
            var seen = animal.lastSeen();
            var world = Bukkit.getWorld(seen.world());
            location = world == null ? null : new Location(world, seen.x(), seen.y(), seen.z());
        }

        if (location == null) {
            return lang.raw("owner.where_unknown");
        }

        String coords = location.getWorld().getName() + " " + location.getBlockX() + ", " + location.getBlockY() + ", "
                + location.getBlockZ();

        if (viewer.getWorld().equals(location.getWorld())) {
            return lang.raw("owner.where_distance", "coords", coords, "distance",
                    (int) viewer.getLocation().distance(location));
        }

        return coords;
    }

    public void claim(Player player, Animal animal) {

        if (animal.isOwnedBy(player.getUniqueId())) {
            lang.send(player, "owner.already_yours");
            return;
        }
        if (animal.ownerId() != null) {
            lang.send(player, "owner.not_yours", "owner", ownerName(animal));
            return;
        }
        if (!settings().claimUnowned()) {
            lang.send(player, "owner.claim_disabled");
            return;
        }
        if (market.atLimit(player.getUniqueId())) {
            lang.send(player, "owner.limit", "max", settings().maxAnimalsPerPlayer());
            return;
        }

        animal.setOwnerId(player.getUniqueId());
        animalManager.save(animal);
        lang.send(player, "owner.claimed", "animal", describe(animal));
    }

    /** Llama a un animal propio hasta el jugador. */
    public void call(Player player, Animal animal) {
        bringOne(player, animal, player.getLocation(), "owner.called");
    }

    public void sendToPen(Player player, Animal animal) {

        Location pen = pens.get(player.getUniqueId()).orElse(null);

        if (pen == null) {
            lang.send(player, "owner.no_pen");
            return;
        }

        bringOne(player, animal, pen, "owner.sent_to_pen");
    }

    public void callAll(Player player) {
        bringAll(player, player.getLocation(), "owner.called_all");
    }

    public void sendAllToPen(Player player) {

        Location pen = pens.get(player.getUniqueId()).orElse(null);

        if (pen == null) {
            lang.send(player, "owner.no_pen");
            return;
        }

        bringAll(player, pen, "owner.sent_all_to_pen");
    }

    private void bringOne(Player player, Animal animal, Location destination, String doneKey) {

        if (!animal.isOwnedBy(player.getUniqueId()) && !player.hasPermission(BYPASS_PERMISSION)) {
            lang.send(player, "owner.not_yours", "owner", ownerName(animal));
            return;
        }
        if (!checkCooldown(player) || !checkWorld(player, animal, destination)) {
            return;
        }

        recall.bring(animal, destination, outcome -> report(player, animal, outcome, doneKey));
    }

    private void bringAll(Player player, Location destination, String doneKey) {

        List<Animal> owned = animalManager.ownedBy(player.getUniqueId());

        if (owned.isEmpty()) {
            lang.send(player, "owner.none");
            return;
        }
        if (!checkCooldown(player)) {
            return;
        }

        int[] pending = {owned.size()};
        int[] ok = {0};

        for (Animal animal : owned) {

            if (!settings().recallCrossWorld() && !sameWorld(animal, destination)) {
                if (--pending[0] == 0) {
                    lang.send(player, doneKey, "count", ok[0]);
                }
                continue;
            }

            recall.bring(animal, destination, outcome -> {
                if (outcome == AnimalRecall.Outcome.MOVED || outcome == AnimalRecall.Outcome.RECOVERED) {
                    ok[0]++;
                }
                if (--pending[0] == 0 && player.isOnline()) {
                    lang.send(player, doneKey, "count", ok[0]);
                }
            });
        }
    }

    private void report(Player player, Animal animal, AnimalRecall.Outcome outcome, String doneKey) {

        if (!player.isOnline()) {
            return;
        }

        switch (outcome) {
            case MOVED -> lang.send(player, doneKey, "animal", describe(animal), "count", 1);
            case RECOVERED -> lang.send(player, "owner.recovered", "animal", describe(animal));
            case BUSY -> lang.send(player, "owner.busy");
            case NOT_FOUND -> lang.send(player, "owner.not_found", "animal", describe(animal));
        }
    }

    private boolean checkCooldown(Player player) {

        long cooldownMillis = settings().recallCooldownSeconds() * 1000L;
        long now = System.currentTimeMillis();
        Long last = lastRecall.get(player.getUniqueId());

        if (cooldownMillis > 0 && last != null && now - last < cooldownMillis && !player.hasPermission(BYPASS_PERMISSION)) {
            lang.send(player, "owner.cooldown", "seconds", (int) Math.ceil((cooldownMillis - (now - last)) / 1000.0));
            return false;
        }

        lastRecall.put(player.getUniqueId(), now);
        return true;
    }

    private boolean checkWorld(Player player, Animal animal, Location destination) {

        if (settings().recallCrossWorld() || sameWorld(animal, destination)) {
            return true;
        }

        lang.send(player, "owner.other_world");
        return false;
    }

    private boolean sameWorld(Animal animal, Location destination) {
        String world = animalManager.entityOf(animal).map(e -> e.getWorld().getName())
                .orElse(animal.lastSeen() == null ? null : animal.lastSeen().world());
        return world == null || world.equals(destination.getWorld().getName());
    }

    /** Pone precio (o con 0 lo quita de la venta). */
    public void list(Player player, Animal animal, double price) {

        AnimalMarket.Result result = market.list(player, animal, price);

        if (result == AnimalMarket.Result.OK) {
            if (price > 0) {
                lang.send(player, "market.listed", "animal", describe(animal), "price", AnimalMarket.format(price));
            } else {
                lang.send(player, "market.unlisted", "animal", describe(animal));
            }
            return;
        }

        sendResult(player, result, animal);
    }

    public void buy(Player player, Animal animal) {

        String seller = ownerName(animal);
        UUID sellerId = animal.ownerId();
        double price = animal.salePrice();
        AnimalMarket.Result result = market.buy(player, animal);

        if (result != AnimalMarket.Result.OK) {
            sendResult(player, result, animal);
            return;
        }

        lang.send(player, "market.bought", "animal", describe(animal), "price", AnimalMarket.format(price), "seller", seller);

        Player online = sellerId == null ? null : Bukkit.getPlayer(sellerId);
        if (online != null) {
            lang.send(online, "market.sold_to_player", "animal", describe(animal), "buyer", player.getName(), "price",
                    AnimalMarket.format(price * (1 - settings().taxPercent() / 100.0)));
        }
    }

    public void sellToServer(Player player, Animal animal) {

        String name = describe(animal);
        double price = market.serverPrice(animal);
        AnimalMarket.Result result = market.sellToServer(player, animal);

        if (result == AnimalMarket.Result.OK) {
            lang.send(player, "market.sold_to_server", "animal", name, "price", AnimalMarket.format(price));
        } else {
            sendResult(player, result, animal);
        }
    }

    public void buyFromServer(Player player, OwnershipSettings.ServerOffer offer) {

        AnimalMarket.Result result = market.buyFromServer(player, offer);

        if (result == AnimalMarket.Result.OK) {
            lang.send(player, "market.bought_from_server", "animal", offerName(offer), "price",
                    AnimalMarket.format(offer.price()));
        } else {
            sendResult(player, result, null);
        }
    }

    public String offerName(OwnershipSettings.ServerOffer offer) {
        String species = speciesManager.get(offer.speciesId()).map(Species::displayName).orElse(offer.speciesId());
        String breed = offer.breedId() == null ? null
                : breedManager.get(offer.breedId()).map(Breed::displayName).orElse(null);
        return species + (breed != null ? " " + breed : "");
    }

    private void sendResult(Player player, AnimalMarket.Result result, Animal animal) {
        lang.send(player, "market.result." + result.name().toLowerCase(Locale.ROOT),
                "owner", animal == null ? "" : ownerName(animal),
                "max", settings().maxAnimalsPerPlayer(),
                "min", AnimalMarket.format(settings().minPrice()),
                "maxprice", AnimalMarket.format(settings().maxPrice()));
    }

}
