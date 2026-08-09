package com.sack.rpgroll.sackresourcepack.datapack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sack.rpgroll.sackresourcepack.manifest.AssetModule;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Copia {@code data/} de cada módulo hacia un único árbol de datapack —
 * análogo a {@link com.sack.rpgroll.sackresourcepack.merge.MergeEngine} pero
 * para contenido server-side (tags, loot tables, recipes, functions...) en
 * vez de assets de cliente. Se mantiene como clase separada porque la
 * estrategia de fusión es distinta: acá solo los archivos bajo
 * {@code tags/} se fusionan de verdad (unión del array {@code values}, sin
 * duplicados) — el resto (loot_tables, recipes, advancements, functions)
 * se copia tal cual y el último módulo pisa, porque no tiene sentido
 * "fusionar" dos recetas distintas con el mismo id.
 */
public class DatapackMergeEngine {

    /**
     * Un datapack real lleva {@code pack.mcmeta} en la raíz y los
     * namespaces bajo {@code data/<namespace>/...} — igual que
     * {@code assets/} del lado del resource pack.
     */
    private static final String DATA_PREFIX = "data/";

    private final Plugin plugin;

    public DatapackMergeEngine(Plugin plugin) {
        this.plugin = plugin;
    }

    public DatapackMergeResult merge(java.util.List<AssetModule> orderedModules, File outputDirectory) {

        deleteRecursively(outputDirectory);
        outputDirectory.mkdirs();

        Map<String, String> fileOwners = new LinkedHashMap<>();

        for (AssetModule module : orderedModules) {

            File dataDir = module.dataDirectory();

            if (!dataDir.isDirectory()) {
                continue;
            }

            mergeModule(module, dataDir, outputDirectory, fileOwners);
        }

        return new DatapackMergeResult(outputDirectory, fileOwners);
    }

    private void mergeModule(AssetModule module, File dataDir, File outputDirectory, Map<String, String> fileOwners) {

        try (Stream<Path> walk = Files.walk(dataDir.toPath())) {

            for (Path source : walk.filter(Files::isRegularFile).toList()) {

                String relativePath = dataDir.toPath().relativize(source).toString().replace('\\', '/');
                File destination = new File(outputDirectory, DATA_PREFIX + relativePath);

                if (isTagFile(relativePath) && destination.isFile()) {
                    mergeTagFile(relativePath, source.toFile(), destination, module, fileOwners);
                } else {
                    copyPlainFile(module, relativePath, source.toFile(), destination, fileOwners);
                }
            }

        } catch (IOException e) {
            plugin.getLogger().warning(
                    "✘ Error fusionando el datapack del módulo '" + module.id() + "': " + e.getMessage());
        }
    }

    private boolean isTagFile(String relativePath) {
        return relativePath.contains("/tags/") && relativePath.endsWith(".json");
    }

    private void copyPlainFile(AssetModule module, String relativePath, File source, File destination,
            Map<String, String> fileOwners) {

        try {
            destination.getParentFile().mkdirs();
            Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
            fileOwners.put(relativePath, module.id());
        } catch (IOException e) {
            plugin.getLogger().warning("✘ Error copiando " + relativePath + " (módulo '" + module.id() + "'): "
                    + e.getMessage());
        }
    }

    private void mergeTagFile(String relativePath, File source, File destination, AssetModule module,
            Map<String, String> fileOwners) {

        try {
            JsonObject existing = readJsonObject(destination);
            JsonObject incoming = readJsonObject(source);

            Set<String> values = new LinkedHashSet<>();

            if (existing.has("values") && existing.get("values").isJsonArray()) {
                for (JsonElement value : existing.getAsJsonArray("values")) {
                    values.add(value.toString());
                }
            }

            if (incoming.has("values") && incoming.get("values").isJsonArray()) {
                for (JsonElement value : incoming.getAsJsonArray("values")) {
                    values.add(value.toString());
                }
            }

            JsonArray merged = new JsonArray();
            values.forEach(value -> merged.add(JsonParser.parseString(value)));

            existing.add("values", merged);

            if (incoming.has("replace")) {
                existing.add("replace", incoming.get("replace"));
            }

            Files.writeString(destination.toPath(), prettyPrint(existing), StandardCharsets.UTF_8);
            fileOwners.put(relativePath, fileOwners.getOrDefault(relativePath, "") + "+" + module.id());

        } catch (Exception e) {
            plugin.getLogger().warning(
                    "✘ Error fusionando tag " + relativePath + " (módulo '" + module.id() + "'): " + e.getMessage());
        }
    }

    private JsonObject readJsonObject(File file) throws IOException {

        if (!file.isFile()) {
            return new JsonObject();
        }

        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        JsonElement parsed = JsonParser.parseString(content);

        return parsed.isJsonObject() ? parsed.getAsJsonObject() : new JsonObject();
    }

    private String prettyPrint(JsonObject object) {
        return new com.google.gson.GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(object);
    }

    private void deleteRecursively(File file) {

        if (!file.exists()) {
            return;
        }

        File[] children = file.listFiles();

        if (children != null) {
            for (File child : children) {
                deleteRecursively(child);
            }
        }

        file.delete();
    }

}
