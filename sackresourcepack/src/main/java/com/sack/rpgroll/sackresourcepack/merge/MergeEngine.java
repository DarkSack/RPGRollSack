package com.sack.rpgroll.sackresourcepack.merge;

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
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Copia {@code assets/} de cada módulo (en el orden ya resuelto por {@link
 * com.sack.rpgroll.sackresourcepack.manifest.DependencyResolver}) hacia un
 * único árbol de trabajo — archivos sueltos (texturas, modelos, .ogg) se
 * copian tal cual (el último módulo pisa), salvo tres formatos que se
 * fusionan de verdad en vez de pisarse: {@code sounds.json} (unión de
 * eventos de sonido), {@code lang/*.json} (unión de claves de traducción,
 * nunca se pierde lo que ya había) y cualquier {@code font/*.json}
 * (concatena el array {@code providers}).
 */
public class MergeEngine {

    /**
     * Un resource pack real lleva {@code pack.mcmeta} en la raíz y los
     * namespaces bajo {@code assets/<namespace>/...} — NO directo en la
     * raíz. Todo lo que viene de {@code module.assetsDirectory()} (que
     * termina en "assets/" del lado del módulo) se reubica bajo este
     * mismo nombre del lado del pack fusionado.
     */
    private static final String ASSETS_PREFIX = "assets/";

    private final Plugin plugin;

    public MergeEngine(Plugin plugin) {
        this.plugin = plugin;
    }

    public MergeResult merge(List<AssetModule> orderedModules, File outputDirectory) {

        deleteRecursively(outputDirectory);
        outputDirectory.mkdirs();

        Map<String, String> fileOwners = new LinkedHashMap<>();
        List<OverrideNotice> overrides = new ArrayList<>();

        for (AssetModule module : orderedModules) {

            File assetsDir = module.assetsDirectory();

            if (!assetsDir.isDirectory()) {
                continue;
            }

            mergeModule(module, assetsDir, outputDirectory, fileOwners, overrides);
        }

        return new MergeResult(outputDirectory, fileOwners, overrides);
    }

    private void mergeModule(AssetModule module, File assetsDir, File outputDirectory,
            Map<String, String> fileOwners, List<OverrideNotice> overrides) {

        try (Stream<Path> walk = Files.walk(assetsDir.toPath())) {

            for (Path source : walk.filter(Files::isRegularFile).toList()) {

                String relativePath = assetsDir.toPath().relativize(source).toString().replace('\\', '/');
                File destination = new File(outputDirectory, ASSETS_PREFIX + relativePath);

                if (isJsonMergeTarget(relativePath) && destination.isFile()) {
                    mergeJsonFile(module, relativePath, source.toFile(), destination, fileOwners, overrides);
                } else {
                    copyPlainFile(module, relativePath, source.toFile(), destination, fileOwners, overrides);
                }
            }

        } catch (IOException e) {
            plugin.getLogger().warning(
                    "✘ Error fusionando el módulo '" + module.id() + "': " + e.getMessage());
        }
    }

    private boolean isJsonMergeTarget(String relativePath) {
        return relativePath.endsWith("sounds.json")
                || (relativePath.contains("/lang/") && relativePath.endsWith(".json"))
                || (relativePath.contains("/font/") && relativePath.endsWith(".json"));
    }

    private void copyPlainFile(AssetModule module, String relativePath, File source, File destination,
            Map<String, String> fileOwners, List<OverrideNotice> overrides) {

        try {

            if (destination.isFile() && !sameContent(source, destination)) {
                String previousOwner = fileOwners.getOrDefault(relativePath, "?");
                overrides.add(new OverrideNotice(relativePath, previousOwner, module.id()));
            }

            destination.getParentFile().mkdirs();
            Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
            fileOwners.put(relativePath, module.id());

        } catch (IOException e) {
            plugin.getLogger().warning("✘ Error copiando " + relativePath + " (módulo '" + module.id() + "'): "
                    + e.getMessage());
        }
    }

    private void mergeJsonFile(AssetModule module, String relativePath, File source, File destination,
            Map<String, String> fileOwners, List<OverrideNotice> overrides) {

        try {
            JsonObject existing = readJsonObject(destination);
            JsonObject incoming = readJsonObject(source);

            if (relativePath.contains("/font/")) {
                mergeFontProviders(existing, incoming);
            } else {
                mergeFlatKeys(relativePath, module, existing, incoming, fileOwners, overrides);
            }

            Files.writeString(destination.toPath(), prettyPrint(existing), StandardCharsets.UTF_8);
            fileOwners.put(relativePath, fileOwners.getOrDefault(relativePath, "") + "+" + module.id());

        } catch (Exception e) {
            plugin.getLogger().warning(
                    "✘ Error fusionando JSON " + relativePath + " (módulo '" + module.id() + "'): " + e.getMessage());
        }
    }

    private void mergeFlatKeys(String relativePath, AssetModule module, JsonObject existing, JsonObject incoming,
            Map<String, String> fileOwners, List<OverrideNotice> overrides) {

        for (var entry : incoming.entrySet()) {

            if (existing.has(entry.getKey()) && !existing.get(entry.getKey()).equals(entry.getValue())) {
                String previousOwner = fileOwners.getOrDefault(relativePath, "?");
                overrides.add(new OverrideNotice(relativePath + "#" + entry.getKey(), previousOwner, module.id()));
            }

            existing.add(entry.getKey(), entry.getValue());
        }
    }

    private void mergeFontProviders(JsonObject existing, JsonObject incoming) {

        JsonArray providers = existing.has("providers") && existing.get("providers").isJsonArray()
                ? existing.getAsJsonArray("providers")
                : new JsonArray();

        if (incoming.has("providers") && incoming.get("providers").isJsonArray()) {
            for (JsonElement provider : incoming.getAsJsonArray("providers")) {
                providers.add(provider);
            }
        }

        existing.add("providers", providers);
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

    private boolean sameContent(File a, File b) {
        try {
            return hash(a).equals(hash(b));
        } catch (Exception e) {
            return false;
        }
    }

    private String hash(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return Base64.getEncoder().encodeToString(digest.digest(Files.readAllBytes(file.toPath())));
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
