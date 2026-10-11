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
 * copian tal cual (el último módulo pisa), salvo cuatro formatos que se
 * fusionan de verdad en vez de pisarse: {@code sounds.json} (unión de
 * eventos de sonido), {@code lang/*.json} (unión de claves de traducción,
 * nunca se pierde lo que ya había), cualquier {@code font/*.json}
 * (concatena el array {@code providers}) y los {@code blockstates/*.json}
 * (unión de {@code variants}, donde un modelo propio gana al vanilla).
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
                if (module.excludes(relativePath)) {
                    continue;
                }
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
                || (relativePath.contains("/font/") && relativePath.endsWith(".json"))
                || (relativePath.contains("/blockstates/") && relativePath.endsWith(".json"));
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
            } else if (relativePath.contains("/blockstates/")) {
                if (!mergeBlockstateVariants(existing, incoming)) {
                    copyPlainFile(module, relativePath, source, destination, fileOwners, overrides);
                    return;
                }
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

    /**
     * Dos módulos que dan modelo propio a estados distintos del mismo bloque (menas y lucky
     * blocks sobre el bloque musical) traen cada uno el archivo entero: sus estados y, en el
     * resto, el modelo vanilla. Se unen los {@code variants}; el modelo vanilla de un módulo
     * nunca pisa al propio de otro.
     *
     * @return false si alguno no usa {@code variants} (multipart): ahí el último pisa, como antes
     */
    static boolean mergeBlockstateVariants(JsonObject existing, JsonObject incoming) {

        if (!existing.has("variants") || !existing.get("variants").isJsonObject()
                || !incoming.has("variants") || !incoming.get("variants").isJsonObject()) {
            return false;
        }

        JsonObject variants = existing.getAsJsonObject("variants");

        for (var entry : incoming.getAsJsonObject("variants").entrySet()) {
            if (!variants.has(entry.getKey()) || !isVanillaModel(entry.getValue())) {
                variants.add(entry.getKey(), entry.getValue());
            }
        }

        return true;
    }

    private static boolean isVanillaModel(JsonElement variant) {

        if (!variant.isJsonObject() || !variant.getAsJsonObject().has("model")) {
            return false;
        }

        String model = variant.getAsJsonObject().get("model").getAsString();
        return !model.contains(":") || model.startsWith("minecraft:");
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
