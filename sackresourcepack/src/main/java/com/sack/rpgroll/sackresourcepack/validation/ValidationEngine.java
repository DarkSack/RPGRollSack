package com.sack.rpgroll.sackresourcepack.validation;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sack.rpgroll.sackresourcepack.manifest.AssetModule;
import com.sack.rpgroll.sackresourcepack.merge.MergeResult;
import com.sack.rpgroll.sackresourcepack.merge.OverrideNotice;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Corre antes de empaquetar el ZIP — nunca bloquea el build por sí sola
 * (eso lo decide quien llame, mirando si hay {@code ERROR}), solo junta
 * todos los problemas encontrados de una pasada.
 */
public class ValidationEngine {

    public List<ValidationIssue> validate(MergeResult mergeResult, List<AssetModule> modules) {

        List<ValidationIssue> issues = new ArrayList<>();

        for (OverrideNotice notice : mergeResult.overrides()) {
            issues.add(ValidationIssue.warning("Override en '" + notice.relativePath() + "': "
                    + notice.previousModuleId() + " → " + notice.newModuleId()));
        }

        checkNamespaces(modules, issues);

        File root = mergeResult.outputDirectory();

        if (!root.isDirectory()) {
            return issues;
        }

        try (Stream<java.nio.file.Path> walk = Files.walk(root.toPath())) {

            for (java.nio.file.Path path : walk.filter(Files::isRegularFile).toList()) {

                File file = path.toFile();
                String relative = root.toPath().relativize(path).toString().replace('\\', '/');

                if (relative.endsWith(".png")) {
                    checkPng(relative, file, issues);
                } else if (relative.contains("/models/") && relative.endsWith(".json")) {
                    checkModel(relative, file, root, issues);
                } else if (relative.endsWith("sounds.json")) {
                    checkSounds(relative, file, root, issues);
                }
            }

        } catch (IOException e) {
            issues.add(ValidationIssue.error("No se pudo recorrer el pack fusionado: " + e.getMessage()));
        }

        return issues;
    }

    private void checkNamespaces(List<AssetModule> modules, List<ValidationIssue> issues) {

        for (AssetModule module : modules) {

            File assetsDir = module.assetsDirectory();

            if (!assetsDir.isDirectory()) {
                continue;
            }

            File[] namespaceFolders = assetsDir.listFiles(File::isDirectory);

            if (namespaceFolders == null) {
                continue;
            }

            boolean declaredNamespacePresent = false;

            for (File namespaceFolder : namespaceFolders) {
                if (namespaceFolder.getName().equals(module.namespace())) {
                    declaredNamespacePresent = true;
                    break;
                }
            }

            if (!declaredNamespacePresent && namespaceFolders.length > 0) {
                issues.add(ValidationIssue.warning("Módulo '" + module.id() + "' declara namespace '"
                        + module.namespace() + "' pero sus assets están bajo otro nombre de carpeta."));
            }
        }
    }

    private void checkPng(String relative, File file, List<ValidationIssue> issues) {

        try {
            if (ImageIO.read(file) == null) {
                issues.add(ValidationIssue.error("PNG inválido o corrupto: " + relative));
            }
        } catch (IOException e) {
            issues.add(ValidationIssue.error("No se pudo leer PNG '" + relative + "': " + e.getMessage()));
        }
    }

    private void checkModel(String relative, File file, File root, List<ValidationIssue> issues) {

        JsonObject model;

        try {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            JsonElement parsed = JsonParser.parseString(content);

            if (!parsed.isJsonObject()) {
                issues.add(ValidationIssue.error("Modelo '" + relative + "' no es un objeto JSON válido."));
                return;
            }

            model = parsed.getAsJsonObject();

        } catch (Exception e) {
            issues.add(ValidationIssue.error("Modelo '" + relative + "' no parsea como JSON: " + e.getMessage()));
            return;
        }

        if (model.has("parent") && model.get("parent").isJsonPrimitive()) {

            String parent = model.get("parent").getAsString();

            if (!isVanillaReference(parent) && !referenceExists(root, parent, "models", ".json")) {
                issues.add(ValidationIssue.warning(
                        "Modelo '" + relative + "' referencia un parent inexistente: " + parent));
            }
        }

        if (model.has("textures") && model.get("textures").isJsonObject()) {

            for (var entry : model.getAsJsonObject("textures").entrySet()) {

                if (!entry.getValue().isJsonPrimitive()) {
                    continue;
                }

                String texture = entry.getValue().getAsString();

                if (!isVanillaReference(texture) && !referenceExists(root, texture, "textures", ".png")) {
                    issues.add(ValidationIssue.warning(
                            "Modelo '" + relative + "' referencia una textura inexistente: " + texture));
                }
            }
        }
    }

    private void checkSounds(String relative, File file, File root, List<ValidationIssue> issues) {

        JsonObject sounds;

        try {
            String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            JsonElement parsed = JsonParser.parseString(content);

            if (!parsed.isJsonObject()) {
                issues.add(ValidationIssue.error("'" + relative + "' no es un objeto JSON válido."));
                return;
            }

            sounds = parsed.getAsJsonObject();

        } catch (Exception e) {
            issues.add(ValidationIssue.error("'" + relative + "' no parsea como JSON: " + e.getMessage()));
            return;
        }

        // El namespace por defecto de las referencias sin ":" es la carpeta
        // donde vive este sounds.json (ej. assets/rpgroll_fishing/sounds.json).
        String withoutAssetsPrefix = relative.startsWith("assets/") ? relative.substring("assets/".length()) : relative;
        String defaultNamespace = withoutAssetsPrefix.contains("/")
                ? withoutAssetsPrefix.substring(0, withoutAssetsPrefix.indexOf('/'))
                : "minecraft";

        for (var entry : sounds.entrySet()) {

            if (!entry.getValue().isJsonObject()) {
                continue;
            }

            JsonObject soundEvent = entry.getValue().getAsJsonObject();

            if (!soundEvent.has("sounds") || !soundEvent.get("sounds").isJsonArray()) {
                continue;
            }

            for (JsonElement soundElement : soundEvent.getAsJsonArray("sounds")) {
                checkSoundEntry(relative, entry.getKey(), soundElement, defaultNamespace, root, issues);
            }
        }
    }

    private void checkSoundEntry(String relative, String eventName, JsonElement soundElement,
            String defaultNamespace, File root, List<ValidationIssue> issues) {

        String soundName;

        if (soundElement.isJsonPrimitive()) {
            soundName = soundElement.getAsString();
        } else if (soundElement.isJsonObject() && soundElement.getAsJsonObject().has("name")) {
            soundName = soundElement.getAsJsonObject().get("name").getAsString();
        } else {
            return;
        }

        String namespace = defaultNamespace;
        String path = soundName;

        if (soundName.contains(":")) {
            String[] parts = soundName.split(":", 2);
            namespace = parts[0];
            path = parts[1];
        }

        if (namespace.equals("minecraft")) {
            return;
        }

        File oggFile = new File(root, "assets/" + namespace + "/sounds/" + path + ".ogg");

        if (!oggFile.isFile()) {
            issues.add(ValidationIssue.warning(
                    "'" + relative + "' (evento '" + eventName + "') referencia un .ogg inexistente: " + soundName));
        }
    }

    private boolean isVanillaReference(String reference) {
        return !reference.contains(":") || reference.startsWith("minecraft:");
    }

    private boolean referenceExists(File root, String reference, String kind, String extension) {

        String namespace;
        String path;

        if (reference.contains(":")) {
            String[] parts = reference.split(":", 2);
            namespace = parts[0];
            path = parts[1];
        } else {
            return true;
        }

        return new File(root, "assets/" + namespace + "/" + kind + "/" + path + extension).isFile();
    }

}
