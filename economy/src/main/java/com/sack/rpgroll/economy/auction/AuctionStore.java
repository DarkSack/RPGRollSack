package com.sack.rpgroll.economy.auction;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Persiste cada subasta como {@code plugins/RPGRoll-Economy/auctions/<uuid>.yml} — el ItemStack se serializa nativo. */
public class AuctionStore {

    private final File folder;
    private final Logger logger;

    public AuctionStore(File dataFolder, Logger logger) {
        this.folder = new File(dataFolder, "auctions");
        this.folder.mkdirs();
        this.logger = logger;
    }

    public List<AuctionListing> loadAll() {

        List<AuctionListing> listings = new ArrayList<>();
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));

        if (files == null) {
            return listings;
        }

        for (File file : files) {

            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                ItemStack item = config.getItemStack("item");

                if (item == null) {
                    logger.warning("Subasta " + file.getName() + ": el ítem no se pudo leer; se deja el archivo sin cargar.");
                    continue;
                }

                long expires = config.getLong("expires-at");
                double buyNow = config.getDouble("buy-now-price", -1);

                // Las subastas de antes de 2026-09 no guardaban tipo: todas eran con pujas.
                AuctionListing listing = new AuctionListing(
                        UUID.fromString(config.getString("id")),
                        UUID.fromString(config.getString("seller")),
                        config.getString("seller-name", "?"),
                        item,
                        config.getString("item-name", item.getType().name()),
                        config.getDouble("start-price"),
                        buyNow,
                        config.getBoolean("biddable", true),
                        config.getString("currency"),
                        config.getLong("created-at", expires),
                        expires);

                String bidder = config.getString("bidder");
                listing.placeBid(bidder == null || bidder.isEmpty() ? null : UUID.fromString(bidder),
                        config.getString("bidder-name", "?"), config.getDouble("current-bid", listing.startPrice()));
                listing.setSettled(config.getBoolean("settled", false));

                listings.add(listing);
            } catch (RuntimeException e) {
                logger.log(Level.WARNING, "Subasta " + file.getName() + " ilegible; se deja el archivo sin cargar.", e);
            }
        }

        return listings;
    }

    public void save(AuctionListing listing) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", listing.id().toString());
        config.set("seller", listing.sellerId().toString());
        config.set("seller-name", listing.sellerName());
        config.set("item", listing.item());
        config.set("item-name", listing.itemName());
        config.set("start-price", listing.startPrice());
        config.set("buy-now-price", listing.buyNowPrice());
        config.set("biddable", listing.biddable());
        config.set("currency", listing.currencyId());
        config.set("created-at", listing.createdAtMillis());
        config.set("expires-at", listing.expiresAtMillis());
        config.set("current-bid", listing.currentBid());
        config.set("bidder", listing.currentBidderId() == null ? null : listing.currentBidderId().toString());
        config.set("bidder-name", listing.currentBidderName());
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
