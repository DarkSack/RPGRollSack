package com.sack.rpgroll.economy.shop;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ShopStore {

    private final File folder;

    public ShopStore(File dataFolder) {
        this.folder = new File(dataFolder, "shops");
        this.folder.mkdirs();
    }

    public List<PlayerShop> loadAll() {

        List<PlayerShop> shops = new ArrayList<>();
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yml"));

        if (files == null) {
            return shops;
        }

        for (File file : files) {

            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

            PlayerShop shop = new PlayerShop(
                    UUID.fromString(config.getString("id")),
                    UUID.fromString(config.getString("owner")),
                    config.getString("name", "Tienda"),
                    config.getString("currency"));

            shop.setOpen(config.getBoolean("open", true));

            for (Map<?, ?> raw : config.getMapList("listings")) {

                Object materialRaw = raw.get("material");
                if (materialRaw == null) {
                    continue;
                }

                Material material;
                try {
                    material = Material.valueOf(materialRaw.toString());
                } catch (IllegalArgumentException e) {
                    continue;
                }

                String displayName = raw.get("display-name") != null ? raw.get("display-name").toString() : material.name();
                double price = raw.get("price") instanceof Number number ? number.doubleValue() : 1.0;
                int stock = raw.get("stock") instanceof Number number ? number.intValue() : -1;

                shop.listings().add(new ShopListing(material, displayName, price, stock));
            }

            shops.add(shop);
        }

        return shops;
    }

    public void save(PlayerShop shop) {

        YamlConfiguration config = new YamlConfiguration();
        config.set("id", shop.id().toString());
        config.set("owner", shop.ownerId().toString());
        config.set("name", shop.name());
        config.set("currency", shop.currencyId());
        config.set("open", shop.isOpen());

        List<Map<String, Object>> listings = new ArrayList<>();
        for (ShopListing listing : shop.listings()) {
            listings.add(Map.of(
                    "material", listing.material().name(),
                    "display-name", listing.displayName(),
                    "price", listing.unitPrice(),
                    "stock", listing.stock()));
        }
        config.set("listings", listings);

        try {
            config.save(new File(folder, shop.id() + ".yml"));
        } catch (Exception e) {
            throw new RuntimeException("No se pudo guardar la tienda " + shop.id(), e);
        }
    }

    public void delete(UUID id) {
        new File(folder, id + ".yml").delete();
    }

}
