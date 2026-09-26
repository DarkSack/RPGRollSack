package com.sack.rpgroll.economy.servershop;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Lee una categoría de la tienda. Una línea mal escrita se avisa y se salta:
 * no tumba el resto de la categoría.
 */
public class ServerShopCategoryParser implements ContentParser<ServerShopCategory> {

    private static final List<String> FORMS = List.of("POTION", "SPLASH_POTION", "LINGERING_POTION");

    private final Consumer<String> warn;

    public ServerShopCategoryParser(Consumer<String> warn) {
        this.warn = warn;
    }

    @Override
    public ServerShopCategory parse(YamlConfiguration config) {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        List<ServerShopEntry> entries = new ArrayList<>();
        List<Map<?, ?>> raw = config.getMapList("items");

        for (int i = 0; i < raw.size(); i++) {
            try {
                entries.add(entry(raw.get(i)));
            } catch (IllegalArgumentException e) {
                warn.accept("Tienda '" + id + "', línea " + (i + 1) + ": " + e.getMessage() + " (se omite).");
            }
        }

        return new ServerShopCategory(id,
                config.getString("display-name", id),
                config.getString("icon", "CHEST"),
                config.getInt("slot", -1),
                config.getStringList("description"),
                config.getString("permission"),
                config.getBoolean("premium", false),
                config.getString("currency"),
                entries);
    }

    ServerShopEntry entry(Map<?, ?> map) {

        ServerShopEntry.Kind kind;
        String key;

        if (map.containsKey("item")) {
            kind = ServerShopEntry.Kind.ITEM;
            key = text(map, "item");
        } else if (map.containsKey("enchant")) {
            kind = ServerShopEntry.Kind.ENCHANT;
            key = text(map, "enchant");
        } else if (map.containsKey("book")) {
            kind = ServerShopEntry.Kind.BOOK;
            key = text(map, "book").toLowerCase(Locale.ROOT);
        } else if (map.containsKey("potion")) {
            kind = ServerShopEntry.Kind.POTION;
            key = text(map, "potion").toUpperCase(Locale.ROOT);
        } else if (map.containsKey("material")) {
            kind = ServerShopEntry.Kind.MATERIAL;
            key = material(text(map, "material")).name();
        } else {
            throw new IllegalArgumentException("falta material, item, enchant, book o potion");
        }

        if (key.isBlank()) {
            throw new IllegalArgumentException("el id está vacío");
        }

        String form = "POTION";
        if (kind == ServerShopEntry.Kind.POTION && map.containsKey("form")) {
            form = text(map, "form").toUpperCase(Locale.ROOT);
            if (!FORMS.contains(form)) {
                throw new IllegalArgumentException("form tiene que ser POTION, SPLASH_POTION o LINGERING_POTION");
            }
        }

        double buy = number(map, "buy");
        double sell = number(map, "sell");
        String market = map.containsKey("market") ? text(map, "market") : null;

        if (kind != ServerShopEntry.Kind.MATERIAL && (sell > 0 || market != null)) {
            throw new IllegalArgumentException(key + ": solo los ítems vanilla (material) se pueden vender o "
                    + "enlazar al mercado");
        }

        if (market == null && buy <= 0 && sell <= 0) {
            throw new IllegalArgumentException(key + ": sin buy, sell ni market no hay nada que hacer con él");
        }

        // Pagar más de lo que cuesta sería dinero infinito comprando y revendiendo.
        if (buy > 0 && sell > buy) {
            warn.accept("Tienda: " + key + " se vendería por más (" + sell + ") de lo que cuesta (" + buy
                    + "); se paga como máximo el precio de compra.");
            sell = buy;
        }

        Object lore = map.get("lore");

        return new ServerShopEntry(kind, key,
                (int) number(map, "level"),
                form,
                map.containsKey("amount") ? (int) number(map, "amount") : 1,
                buy,
                sell,
                market,
                map.containsKey("name") ? text(map, "name") : null,
                lore instanceof List<?> list ? list.stream().map(String::valueOf).toList() : List.of());
    }

    /** Por nombre, con o sin "minecraft:". No usa el registro del servidor, así que vale en los tests. */
    private static Material material(String raw) {

        String name = raw.toUpperCase(Locale.ROOT).replace("MINECRAFT:", "").trim();

        try {
            Material material = Material.valueOf(name);
            if (material.name().endsWith("AIR") || material.name().startsWith("LEGACY_")) {
                throw new IllegalArgumentException("material no válido para la tienda: " + raw);
            }
            return material;
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("material desconocido: " + raw);
        }
    }

    private static String text(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value == null ? "" : value.toString().trim();
    }

    private static double number(Map<?, ?> map, String key) {

        Object value = map.get(key);

        if (value == null) {
            return 0;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }

        try {
            return Double.parseDouble(value.toString().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(key + " no es un número: " + value);
        }
    }

}
