package com.sack.rpgroll.recipes.index;

import com.sack.rpgroll.common.recipe.RecipeEntry;
import com.sack.rpgroll.common.recipe.RecipeSlot;
import com.sack.rpgroll.common.recipe.RecipeStation;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Foto fija de todas las recetas: qué produce cada ítem, dónde se usa y el catálogo que se
 * recorre en el menú. Inmutable: el menú abierto sigue con la suya aunque se rehaga el índice.
 */
public final class RecipeIndex {

    /**
     * Un ítem del catálogo, con todo lo que hace falta para filtrarlo sin volver a mirar recetas.
     *
     * @param stations dónde se produce (vacío si nada lo produce: materias primas)
     * @param sources  qué orígenes lo producen (ídem)
     * @param origin   para mostrar: quién lo produce o, si nadie, quién lo usa
     */
    public record CatalogItem(ItemStack stack, String key, Set<String> stations, Set<String> sources,
            String origin, String haystack, Set<String> normalizedSources, boolean restricted) {

        public ItemStack icon() {
            return stack.clone();
        }

        /** Vanilla sin datos propios: su única clave es la del material. */
        public boolean plain() {
            return key.startsWith("m:");
        }
    }

    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    private final List<RecipeEntry> recipes;
    private final Map<String, List<RecipeEntry>> byOutput;
    private final Map<String, List<RecipeEntry>> byInput;
    private final List<CatalogItem> catalog;
    private final List<RecipeStation> stations;
    private final List<String> sources;
    private final long builtAt;

    private RecipeIndex(List<RecipeEntry> recipes, Map<String, List<RecipeEntry>> byOutput,
            Map<String, List<RecipeEntry>> byInput, List<CatalogItem> catalog, List<RecipeStation> stations,
            List<String> sources, long builtAt) {
        this.recipes = recipes;
        this.byOutput = byOutput;
        this.byInput = byInput;
        this.catalog = catalog;
        this.stations = stations;
        this.sources = sources;
        this.builtAt = builtAt;
    }

    public static RecipeIndex empty() {
        return new RecipeIndex(List.of(), Map.of(), Map.of(), List.of(), List.of(), List.of(), 0);
    }

    /**
     * @param stationName nombre para mostrar de cada estación (entra en la búsqueda: "horno"
     *                    encuentra todo lo que se hace en un horno)
     */
    public static RecipeIndex build(List<RecipeEntry> entries, Function<RecipeStation, String> stationName) {

        // Estaciones por orden de aparición: primero las vanilla (el iterador de Bukkit va primero).
        Map<String, RecipeStation> stationsById = new LinkedHashMap<>();
        for (RecipeEntry entry : entries) {
            stationsById.putIfAbsent(entry.station().id(), entry.station());
        }
        List<String> stationOrder = List.copyOf(stationsById.keySet());

        List<RecipeEntry> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparingInt((RecipeEntry e) -> stationOrder.indexOf(e.station().id())));

        Map<String, List<RecipeEntry>> byOutput = new HashMap<>();
        Map<String, List<RecipeEntry>> byInput = new HashMap<>();
        Map<String, Builder> items = new LinkedHashMap<>();
        Set<String> sources = new LinkedHashSet<>();

        for (RecipeEntry entry : sorted) {

            sources.add(entry.source());
            String station = stationName.apply(entry.station());

            for (ItemStack output : entry.outputs()) {
                index(byOutput, output, entry);
                catalogItem(items, output).add(entry, station, true);
            }

            for (RecipeSlot slot : entry.inputs()) {
                for (ItemStack option : slot.options()) {
                    index(byInput, option, entry);
                    catalogItem(items, option).add(entry, station, false);
                }
            }
        }

        List<CatalogItem> catalog = new ArrayList<>(items.size());
        for (Builder builder : items.values()) {
            catalog.add(builder.build());
        }
        catalog.sort(CATALOG_ORDER);

        return new RecipeIndex(List.copyOf(sorted), freeze(byOutput), freeze(byInput), List.copyOf(catalog),
                List.copyOf(stationsById.values()), List.copyOf(sources), System.currentTimeMillis());
    }

    /** Vanilla sin datos primero, por orden de material; luego lo demás, por origen. */
    private static final Comparator<CatalogItem> CATALOG_ORDER = Comparator
            .comparing((CatalogItem item) -> !item.plain())
            .thenComparing(item -> item.plain() ? "" : item.origin())
            .thenComparingInt(item -> item.stack().getType().ordinal());

    private static void index(Map<String, List<RecipeEntry>> map, ItemStack stack, RecipeEntry entry) {
        for (String key : ItemKeys.keysOf(stack)) {
            List<RecipeEntry> list = map.computeIfAbsent(key, k -> new ArrayList<>());
            if (list.isEmpty() || list.getLast() != entry) {
                list.add(entry);
            }
        }
    }

    private static Builder catalogItem(Map<String, Builder> items, ItemStack stack) {
        return items.computeIfAbsent(ItemKeys.primary(stack), key -> new Builder(stack, key));
    }

    private static Map<String, List<RecipeEntry>> freeze(Map<String, List<RecipeEntry>> map) {
        Map<String, List<RecipeEntry>> frozen = new HashMap<>(map.size() * 2);
        map.forEach((key, list) -> frozen.put(key, List.copyOf(list)));
        return Map.copyOf(frozen);
    }

    // --- Consultas ---

    /** Recetas que producen este ítem y que {@code viewer} puede ver. */
    public List<RecipeEntry> howToMake(ItemStack stack, Player viewer) {
        return lookup(byOutput, stack, viewer);
    }

    /** Recetas en las que este ítem es ingrediente. */
    public List<RecipeEntry> usesOf(ItemStack stack, Player viewer) {
        return lookup(byInput, stack, viewer);
    }

    private static List<RecipeEntry> lookup(Map<String, List<RecipeEntry>> map, ItemStack stack, Player viewer) {
        for (String key : ItemKeys.keysOf(stack)) {
            List<RecipeEntry> found = map.get(key);
            if (found != null && !found.isEmpty()) {
                return viewer == null ? found : found.stream().filter(entry -> entry.isVisibleTo(viewer)).toList();
            }
        }
        return List.of();
    }

    /** Catálogo filtrado; {@code stationId} y {@code source} null = todos. */
    public List<CatalogItem> catalog(String stationId, String source, String query, Player viewer) {

        List<String> terms = SearchText.terms(query);
        List<CatalogItem> result = new ArrayList<>();

        for (CatalogItem item : catalog) {
            if (stationId != null && !item.stations().contains(stationId)) {
                continue;
            }
            if (source != null && !item.sources().contains(source)) {
                continue;
            }
            if (!terms.isEmpty() && !SearchText.matches(terms, item.haystack(), item.normalizedSources())) {
                continue;
            }
            if (item.restricted() && viewer != null && howToMake(item.stack(), viewer).isEmpty()
                    && usesOf(item.stack(), viewer).isEmpty()) {
                continue;
            }
            result.add(item);
        }

        return result;
    }

    public List<RecipeEntry> recipes() {
        return recipes;
    }

    public List<CatalogItem> catalog() {
        return catalog;
    }

    public List<RecipeStation> stations() {
        return stations;
    }

    public List<String> sources() {
        return sources;
    }

    public long builtAt() {
        return builtAt;
    }

    public int recipeCount(String source) {
        return (int) recipes.stream().filter(entry -> entry.source().equals(source)).count();
    }

    /**
     * Un ítem se clasifica por las recetas que lo PRODUCEN: filtrar por "RPGRoll-Furniture" o
     * por "Horno" enseña lo que sale de ahí, como el @mod de JEI, y no la arcilla que un mueble
     * usa. Lo que nada produce (materias primas) solo sale sin filtros o buscándolo.
     */
    private static final class Builder {

        private final ItemStack stack;
        private final String key;
        private final Side made = new Side();
        private final Side used = new Side();
        private boolean restricted = true;

        private static final class Side {
            final Set<String> stations = new LinkedHashSet<>();
            final Set<String> stationNames = new LinkedHashSet<>();
            final Set<String> sources = new LinkedHashSet<>();
        }

        Builder(ItemStack stack, String key) {
            this.stack = stack.asOne();
            this.key = key;
        }

        void add(RecipeEntry entry, String stationName, boolean output) {
            Side side = output ? made : used;
            side.stations.add(entry.station().id());
            side.stationNames.add(stationName);
            side.sources.add(entry.source());
            if (entry.visibleTo() == null) {
                restricted = false;
            }
        }

        CatalogItem build() {

            Set<String> stations = made.stations;
            Set<String> stationNames = made.stationNames;
            Set<String> sources = made.sources;
            String origin = String.join(", ", made.sources.isEmpty() ? used.sources : made.sources);

            StringBuilder text = new StringBuilder(stack.getType().getKey().getKey());
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                if (meta.hasDisplayName()) {
                    text.append(' ').append(PLAIN.serialize(meta.displayName()));
                }
                if (meta.hasItemName()) {
                    text.append(' ').append(PLAIN.serialize(meta.itemName()));
                }
                if (meta instanceof org.bukkit.inventory.meta.PotionMeta potion && potion.getBasePotionType() != null) {
                    text.append(' ').append(potion.getBasePotionType().getKey().getKey());
                }
            }
            stationNames.forEach(name -> text.append(' ').append(name));

            Set<String> normalizedSources = new LinkedHashSet<>();
            sources.forEach(source -> normalizedSources.add(SearchText.normalize(source)));

            return new CatalogItem(stack, key, Collections.unmodifiableSet(stations),
                    Collections.unmodifiableSet(sources), origin, SearchText.normalize(text.toString()),
                    Collections.unmodifiableSet(normalizedSources), restricted);
        }
    }
}
