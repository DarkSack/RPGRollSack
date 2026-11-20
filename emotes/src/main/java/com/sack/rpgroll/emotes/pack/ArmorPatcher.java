package com.sack.rpgroll.emotes.pack;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.imageio.ImageIO;

/**
 * Pone las armaduras propias en el modelo del jugador de BetterModel (emotes con {@code player-armor: true}).
 * <p>
 * BetterModel dibuja la armadura con un select por el tipo de equipment ({@code assetId().getPath()}, p. ej.
 * {@code vacuita}) que solo trae casos para las de vanilla: con una armadura de otro pack, el jugador sale sin
 * ella durante el emote. Aquí, sobre la copia del pack en SackResourcePack, cada {@code equipment/*.json} de los
 * demás módulos recibe un caso propio: una copia de los modelos de netherita que apunta a sus texturas de cuerpo
 * y piernas, copiadas dentro del pack de BetterModel.
 * <p>
 * Se puede ejecutar las veces que haga falta: primero quita los casos que puso antes (los que apuntan a
 * {@code bettermodel:rpgroll_armor/}) y los vuelve a poner. Solo lee y escribe archivos.
 */
public final class ArmorPatcher {

    static final String DIR = "rpgroll_armor";
    private static final String MODEL_PREFIX = "bettermodel:" + DIR + "/";
    private static final String TEXTURE_PREFIX = "bettermodel:item/" + DIR + "/";
    private static final Set<String> VANILLA = Set.of("leather", "chainmail", "iron", "gold", "diamond",
            "netherite", "copper", "turtle_scute", "armadillo_scute", "elytra");
    private static final Gson GSON = new Gson();

    private final Path content;
    private final Consumer<String> log;

    /** {@code content}: la carpeta de contenido de SackResourcePack (con {@code bettermodel/} dentro). */
    public ArmorPatcher(Path content, Consumer<String> log) {
        this.content = content;
        this.log = log;
    }

    /** Devuelve cuántas armaduras propias quedaron puestas (0 si no hay pack de BetterModel o no hay ninguna). */
    public int patch() throws IOException {
        Path bm = content.resolve("bettermodel/assets/bettermodel");
        if (!Files.isDirectory(bm.resolve("items"))) {
            return 0;
        }
        Map<String, Layers> armors = customArmors();
        if (armors.isEmpty()) {
            return 0;
        }
        for (var e : armors.entrySet()) {
            copyLayer(e.getValue().body(), bm.resolve("textures/item/" + DIR + "/" + e.getKey() + ".png"));
            copyLayer(e.getValue().legs(), bm.resolve("textures/item/" + DIR + "/" + e.getKey() + "_leggings.png"));
        }

        Set<String> models = new LinkedHashSet<>();
        int selects = 0;
        List<Path> items;
        try (Stream<Path> s = Files.walk(bm.resolve("items"))) {
            items = s.filter(p -> p.toString().endsWith(".json")).sorted().toList();
        }
        for (Path item : items) {
            JsonElement root = JsonParser.parseString(Files.readString(item, StandardCharsets.UTF_8));
            List<JsonObject> found = new ArrayList<>();
            findArmorSelects(root, found);
            if (found.isEmpty()) {
                continue;
            }
            for (JsonObject select : found) {
                patchSelect(select, armors.keySet(), models);
            }
            selects += found.size();
            Files.writeString(item, GSON.toJson(root), StandardCharsets.UTF_8);
        }
        if (selects == 0) {
            log.accept("✘ El pack de BetterModel no trae armaduras de netherita: las propias no se añadieron."
                    + " Comprueba player-armor: true en su config.yml.");
            return 0;
        }

        Map<String, Boolean> legsLayer = new HashMap<>();
        for (String model : models) {
            Path source = asset(model, "models", ".json");
            if (source == null || !Files.isRegularFile(source)) {
                continue;
            }
            JsonObject json = JsonParser.parseString(Files.readString(source, StandardCharsets.UTF_8)).getAsJsonObject();
            for (String type : armors.keySet()) {
                JsonObject copy = json.deepCopy();
                JsonObject textures = copy.getAsJsonObject("textures");
                if (textures != null) {
                    for (var t : textures.entrySet()) {
                        String ref = t.getValue().getAsString();
                        if (ref.startsWith("#")) {
                            continue;
                        }
                        boolean legs = legsLayer.computeIfAbsent(ref, this::isLeggingsLayer);
                        t.setValue(new com.google.gson.JsonPrimitive(TEXTURE_PREFIX + type + (legs ? "_leggings" : "")));
                    }
                }
                Path target = bm.resolve("models/" + DIR + "/" + type + "/" + flat(model) + ".json");
                Files.createDirectories(target.getParent());
                Files.writeString(target, GSON.toJson(copy), StandardCharsets.UTF_8);
            }
        }
        log.accept("✔ Armaduras propias en el modelo del jugador de los emotes: " + armors.size() + " ("
                + String.join(", ", armors.keySet()) + ").");
        return armors.size();
    }

    /** Cada {@code assets/<ns>/equipment/<tipo>.json} de los módulos (salvo bettermodel) con sus dos capas. */
    private Map<String, Layers> customArmors() throws IOException {
        Map<String, Layers> out = new LinkedHashMap<>();
        for (Path module : modules()) {
            Path assets = module.resolve("assets");
            if (!Files.isDirectory(assets)) {
                continue;
            }
            List<Path> files;
            try (Stream<Path> s = Files.list(assets)) {
                files = s.map(ns -> ns.resolve("equipment")).filter(Files::isDirectory)
                        .flatMap(dir -> {
                            try {
                                return Files.list(dir).toList().stream();
                            } catch (IOException e) {
                                return Stream.empty();
                            }
                        })
                        .filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList();
            }
            for (Path file : files) {
                String type = file.getFileName().toString().replaceFirst("\\.json$", "");
                if (VANILLA.contains(type) || out.containsKey(type)) {
                    continue;
                }
                JsonObject layers;
                try {
                    layers = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8))
                            .getAsJsonObject().getAsJsonObject("layers");
                } catch (RuntimeException e) {
                    log.accept("✘ " + file + " no se pudo leer: " + e.getMessage());
                    continue;
                }
                if (layers == null) {
                    continue;
                }
                Path body = layerTexture(layers, "humanoid");
                Path legs = layerTexture(layers, "humanoid_leggings");
                if (body != null || legs != null) {
                    out.put(type, new Layers(body, legs));
                }
            }
        }
        return out;
    }

    /** La primera textura de la capa, buscada en todos los módulos; null si no hay. */
    private Path layerTexture(JsonObject layers, String layer) throws IOException {
        JsonArray list = layers.getAsJsonArray(layer);
        if (list == null || list.isEmpty() || !list.get(0).isJsonObject()
                || !list.get(0).getAsJsonObject().has("texture")) {
            return null;
        }
        String id = list.get(0).getAsJsonObject().get("texture").getAsString();
        int colon = id.indexOf(':');
        String ns = colon < 0 ? "minecraft" : id.substring(0, colon);
        String path = colon < 0 ? id : id.substring(colon + 1);
        for (Path module : modules()) {
            Path png = module.resolve("assets/" + ns + "/textures/entity/equipment/" + layer + "/" + path + ".png");
            if (Files.isRegularFile(png)) {
                return png;
            }
        }
        return null;
    }

    private List<Path> modules() throws IOException {
        try (Stream<Path> s = Files.list(content)) {
            return s.filter(Files::isDirectory)
                    .filter(p -> !p.getFileName().toString().startsWith(".")
                            && !p.getFileName().toString().equals("bettermodel"))
                    .sorted().toList();
        }
    }

    /** Los select por tipo de armadura: custom_model_data índice 0 con un caso «netherite». */
    private static void findArmorSelects(JsonElement node, List<JsonObject> out) {
        if (node.isJsonArray()) {
            for (JsonElement e : node.getAsJsonArray()) {
                findArmorSelects(e, out);
            }
            return;
        }
        if (!node.isJsonObject()) {
            return;
        }
        JsonObject o = node.getAsJsonObject();
        if (isArmorSelect(o)) {
            out.add(o);
        }
        for (var e : o.entrySet()) {
            findArmorSelects(e.getValue(), out);
        }
    }

    private static boolean isArmorSelect(JsonObject o) {
        if (!"select".equals(strip(string(o, "type"))) || !"custom_model_data".equals(strip(string(o, "property")))) {
            return false;
        }
        int index = o.has("index") ? o.get("index").getAsInt() : 0;
        return index == 0 && netheriteCase(o) != null;
    }

    private static JsonObject netheriteCase(JsonObject select) {
        JsonArray cases = select.getAsJsonArray("cases");
        if (cases == null) {
            return null;
        }
        for (JsonElement c : cases) {
            if (c.isJsonObject() && whenValues(c.getAsJsonObject()).contains("netherite")) {
                return c.getAsJsonObject();
            }
        }
        return null;
    }

    private static void patchSelect(JsonObject select, Set<String> types, Set<String> models) {
        JsonArray cases = select.getAsJsonArray("cases");
        // Fuera los casos de una pasada anterior (también los de armaduras que ya no existen).
        JsonArray kept = new JsonArray();
        Set<String> taken = new LinkedHashSet<>();
        for (JsonElement c : cases) {
            if (c.isJsonObject() && referencesOurs(c.getAsJsonObject().get("model"))) {
                continue;
            }
            kept.add(c);
            if (c.isJsonObject()) {
                taken.addAll(whenValues(c.getAsJsonObject()));
            }
        }
        JsonObject netherite = netheriteCase(select);
        JsonElement base = netherite.get("model");
        collectModels(base, models);
        for (String type : types) {
            if (taken.contains(type)) {
                continue;
            }
            JsonElement model = base.deepCopy();
            rewriteModels(model, type);
            JsonObject added = new JsonObject();
            added.addProperty("when", type);
            added.add("model", model);
            kept.add(added);
        }
        select.add("cases", kept);
    }

    private static boolean referencesOurs(JsonElement node) {
        if (node == null) {
            return false;
        }
        if (node.isJsonPrimitive()) {
            return node.getAsString().startsWith(MODEL_PREFIX);
        }
        if (node.isJsonArray()) {
            for (JsonElement e : node.getAsJsonArray()) {
                if (referencesOurs(e)) {
                    return true;
                }
            }
        } else if (node.isJsonObject()) {
            for (var e : node.getAsJsonObject().entrySet()) {
                if (referencesOurs(e.getValue())) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Las referencias {@code "model": "<id>"} de un nodo de item model, recursivo. */
    private static void collectModels(JsonElement node, Set<String> out) {
        if (node == null) {
            return;
        }
        if (node.isJsonArray()) {
            node.getAsJsonArray().forEach(e -> collectModels(e, out));
        } else if (node.isJsonObject()) {
            for (var e : node.getAsJsonObject().entrySet()) {
                if (e.getKey().equals("model") && e.getValue().isJsonPrimitive()) {
                    out.add(e.getValue().getAsString());
                } else {
                    collectModels(e.getValue(), out);
                }
            }
        }
    }

    private static void rewriteModels(JsonElement node, String type) {
        if (node.isJsonArray()) {
            node.getAsJsonArray().forEach(e -> rewriteModels(e, type));
        } else if (node.isJsonObject()) {
            JsonObject o = node.getAsJsonObject();
            for (var e : o.entrySet()) {
                if (e.getKey().equals("model") && e.getValue().isJsonPrimitive()) {
                    e.setValue(new com.google.gson.JsonPrimitive(MODEL_PREFIX + type + "/" + flat(e.getValue().getAsString())));
                } else {
                    rewriteModels(e.getValue(), type);
                }
            }
        }
    }

    private static Set<String> whenValues(JsonObject c) {
        Set<String> out = new LinkedHashSet<>();
        JsonElement when = c.get("when");
        if (when == null) {
            return out;
        }
        if (when.isJsonArray()) {
            when.getAsJsonArray().forEach(e -> out.add(e.getAsString()));
        } else {
            out.add(when.getAsString());
        }
        return out;
    }

    /**
     * La capa de piernas no pinta la cabeza: en la plantilla 64×32 de la armadura, la zona del casco
     * (x 0-32, y 0-16) queda vacía. La de cuerpo sí la pinta.
     */
    private boolean isLeggingsLayer(String textureRef) {
        Path png = asset(textureRef, "textures", ".png");
        if (png == null || !Files.isRegularFile(png)) {
            return false;
        }
        try {
            BufferedImage im = ImageIO.read(png.toFile());
            if (im == null) {
                return false;
            }
            int w = im.getWidth() / 2;
            int h = im.getHeight() / (im.getHeight() >= im.getWidth() ? 4 : 2);
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    if ((im.getRGB(x, y) >>> 24) != 0) {
                        return false;
                    }
                }
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /** {@code ns:path} a {@code content/bettermodel/assets/<ns>/<kind>/<path><ext>}. */
    private Path asset(String id, String kind, String ext) {
        int colon = id.indexOf(':');
        String ns = colon < 0 ? "minecraft" : id.substring(0, colon);
        String path = colon < 0 ? id : id.substring(colon + 1);
        if (path.contains("..")) {
            return null;
        }
        return content.resolve("bettermodel/assets/" + ns + "/" + kind + "/" + path + ext);
    }

    /** Copia la capa o, si no hay, deja una textura 64×32 transparente. */
    private static void copyLayer(Path source, Path target) throws IOException {
        Files.createDirectories(target.getParent());
        if (source != null) {
            Files.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            return;
        }
        ImageIO.write(new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB), "png", target.toFile());
    }

    private static String flat(String id) {
        int colon = id.indexOf(':');
        return (colon < 0 ? id : id.substring(colon + 1)).replace('/', '_');
    }

    private static String string(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
    }

    private static String strip(String s) {
        return s == null ? null : s.startsWith("minecraft:") ? s.substring(10) : s;
    }

    private record Layers(Path body, Path legs) {
    }
}
