package com.sack.rpgroll.economy.auction;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Persiste cada subasta como {@code plugins/RPGRoll-Economy/auctions/<uuid>.yml} — el ItemStack se serializa nativo. */
public class AuctionStore {

    private final File folder;

    public AuctionStore(File dataFolder) {
        this.folder = new File(dataFolder, "auctions");
        this.folder.mkdirs();
    }

    public List<AuctionListing> loadAll() {

        List<AuctionListing> listings = new ArrayList<>();
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));

        if (files == null) {
            return listings;
        }

        for (File file : files) {

            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            ItemStack item = config.getItemStack("item");

            if (item == null) {
                continue;
            }

            AuctionListing listing = new AuctionListing(
                    UUID.fromString(config.getString("id")),
                    UUID.fromString(config.getString("seller")),
                    item,
                    config.getDouble("start-price"),
                    config.getDouble("buy-now-price", -1),
                    config.getString("currency"),
                    config.getLong("expires-at"));

            listing.placeBid(
                    config.contains("bidder") && config.getString("bidder") != null
                            ? UUID.fromString(config.getString("bidder")) : null,
                    config.getDouble("current-bid", listing.startPrice()));
            listing.setSettled(config.getBoolean("settled", false));

            listings.add(listing);
        }

        return listings;
    }

    public void save(AuctionListing listing) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", listing.id().toString());
        config.set("seller", listing.sellerId().toString());
        config.set("item", listing.item());
        config.set("start-price", listing.startPrice());
        config.set("buy-now-price", listing.buyNowPrice());
        config.set("currency", listing.currencyId());
        config.set("expires-at", listing.expiresAtMillis());
        config.set("current-bid", listing.currentBid());
        config.set("bidder", listing.currentBidderId() == null ? null : listing.currentBidderId().toString());
        config.set("settled", listing.isSettled());

        try {
            config.save(new File(folder, listing.id() + ".yml"));
        } catch (Exception e) {
            throw new RuntimeException("No se pudo guardar la subasta " + listing.id(), e);
        }
    }

    public void delete(UUID id) {
        new File(folder, id + ".yml").delete();
    }

}
