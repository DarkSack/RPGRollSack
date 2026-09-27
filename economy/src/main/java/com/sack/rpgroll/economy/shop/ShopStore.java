package com.sack.rpgroll.economy.shop;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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

                ItemStack item = raw.get("item") instanceof ItemStack stack ? stack : null;

                if (item == null) {
                    Object materialRaw = raw.get("material");
                    if (materialRaw == null) {
                        continue;
                    }
                    try {
                        item = new ItemStack(Material.valueOf(materialRaw.toString()));
                    } catch (IllegalArgumentException e) {
                        continue;
                    }
                }

                String displayName = raw.get("display-name") != null ? raw.get("display-name").toString()
                        : item.getType().name();
                double price = raw.get("price") instanceof Number number ? number.doubleValue() : 1.0;
                int stock = raw.get("stock") instanceof Number number ? number.intValue() : -1;

                if (!raw.containsKey("item") && !shop.isServerShop()) {
                    // Línea de una tienda de jugador de antes de guardar el ítem: su stock lo escribió el
                    // dueño sin entregar nada, así que no existe. Puede reponerlo metiendo el ítem de verdad.
                    stock = 0;
                } else if (stock < 0 && !shop.isServerShop()) {
                    stock = 0;
                }

                shop.listings().add(new ShopListing(item, displayName, price, stock));
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
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("material", listing.material().name());
            entry.put("item", listing.item());
            entry.put("display-name", listing.displayName());
            entry.put("price", listing.unitPrice());
            entry.put("stock", listing.stock());
            listings.add(entry);
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
