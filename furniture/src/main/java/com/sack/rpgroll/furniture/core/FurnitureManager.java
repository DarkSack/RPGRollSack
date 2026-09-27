package com.sack.rpgroll.furniture.core;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Los muebles y sus categorías.
 * <p>
 * Los muebles salen de todos los {@code .yml} de {@code furniture/} (y sus subcarpetas); cada
 * clave de primer nivel de un fichero es un mueble. Las categorías, en el orden en que salen en
 * el catálogo, vienen de {@code categories:} del config.yml.
 */
public class FurnitureManager {

    /** Una categoría del catálogo. */
    public record Category(String id, String name, Material icon) {
    }

    private volatile Map<String, FurnitureDefinition> furniture = Map.of();
    private volatile Map<String, Category> categories = Map.of();

    public void load(File folder, ConfigurationSection categoriesSection, Consumer<String> warn) {

        Map<String, Category> newCategories = new LinkedHashMap<>();
        if (categoriesSection != null) {
            for (String key : categoriesSection.getKeys(false)) {
                String id = key.toLowerCase(Locale.ROOT);
                Material icon = Material.matchMaterial(categoriesSection.getString(key + ".icon", "OAK_PLANKS"));
                newCategories.put(id, new Category(id, categoriesSection.getString(key + ".name", key),
                        icon == null ? Material.OAK_PLANKS : icon));
            }
        }

        Map<String, FurnitureDefinition> loaded = new LinkedHashMap<>();

        for (File file : yamlFiles(folder, warn)) {

            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);

            for (String id : yaml.getKeys(false)) {

                ConfigurationSection section = yaml.getConfigurationSection(id);
                if (section == null) {
                    continue;
                }

                String where = file.getName() + ": ";
                FurnitureParser.parse(id, section, message -> warn.accept(where + message)).ifPresent(def -> {
                    if (loaded.putIfAbsent(def.id(), def) != null) {
                        warn.accept("✘ " + where + "el mueble '" + def.id() + "' ya existe en otro fichero, se ignora este");
                    }
                });
            }
        }

        for (FurnitureDefinition def : loaded.values()) {
            if (!newCategories.containsKey(def.category())) {
                newCategories.put(def.category(), new Category(def.category(), def.category(), Material.OAK_PLANKS));
            }
        }

        this.categories = Collections.unmodifiableMap(newCategories);
        this.furniture = Collections.unmodifiableMap(loaded);
    }

    /**
     * Quita los muebles cuyo material no es un ítem y avisa de los materiales de receta que no lo
     * son. Necesita el servidor arrancado (el registro de ítems).
     */
    public void checkItems(Consumer<String> warn) {

        Map<String, FurnitureDefinition> kept = new LinkedHashMap<>();
        for (FurnitureDefinition def : furniture.values()) {
            if (!def.material().isItem()) {
                warn.accept("✘ Mueble '" + def.id() + "': el material " + def.material() + " no es un ítem, se ignora");
                continue;
            }
            java.util.stream.Stream.concat(java.util.stream.Stream.ofNullable(def.recipe()),
                            def.variants().values().stream().map(FurnitureVariant::recipe).filter(java.util.Objects::nonNull))
                    .flatMap(r -> r.materials().keySet().stream()).distinct()
                    .filter(m -> !m.isItem())
                    .forEach(m -> warn.accept("⚠ Mueble '" + def.id() + "': " + m + " no es un ítem, su receta no se podrá pagar"));
            kept.put(def.id(), def);
        }
        this.furniture = Collections.unmodifiableMap(kept);
    }

    public Optional<FurnitureDefinition> get(String id) {
        return id == null ? Optional.empty() : Optional.ofNullable(furniture.get(id.toLowerCase(Locale.ROOT)));
    }

    public Collection<FurnitureDefinition> all() {
        return furniture.values();
    }

    public int count() {
        return furniture.size();
    }

    public Collection<Category> categories() {
        return categories.values();
    }

    /** Las categorías que tienen algún mueble que cumpla {@code filter}, en orden. */
    public List<Category> categoriesWith(java.util.function.Predicate<FurnitureDefinition> filter) {

        List<Category> out = new ArrayList<>();
        for (Category category : categories.values()) {
            if (furniture.values().stream().anyMatch(def -> def.category().equals(category.id()) && filter.test(def))) {
                out.add(category);
            }
        }
        return out;
    }

    public List<FurnitureDefinition> inCategory(String category, java.util.function.Predicate<FurnitureDefinition> filter) {
        return furniture.values().stream().filter(def -> def.category().equals(category) && filter.test(def)).toList();
    }

    private static List<File> yamlFiles(File folder, Consumer<String> warn) {

        if (!folder.isDirectory()) {
            return List.of();
        }

        try (Stream<Path> paths = Files.walk(folder.toPath())) {
            return paths.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".yml"))
                    .filter(p -> !p.getFileName().toString().startsWith("_"))
                    .sorted()
                    .map(Path::toFile)
                    .toList();
        } catch (IOException e) {
            warn.accept("✘ No se pudo leer la carpeta furniture/: " + e.getMessage());
            return List.of();
        }
    }
}
