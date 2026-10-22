package com.sack.rpgroll.worldforge.profile;

import com.sack.rpgroll.worldforge.api.Capability;
import com.sack.rpgroll.worldforge.api.Ids;
import com.sack.rpgroll.worldforge.api.Problem;
import com.sack.rpgroll.worldforge.api.Result;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Checks a profile against the server ({@link Catalog}) and against what its provider
 * really controls. A field the provider cannot honour is an error: a profile never says
 * one thing while the world does another.
 */
public final class ProfileValidator {

    public static final int MAX_ORE_RULES = 64;
    public static final int MAX_BIOMES = 64;
    public static final int MAX_PLATFORM_RADIUS = 16;
    public static final int MAX_VEINS = 64;
    public static final int MAX_VEIN_SIZE = 64;
    public static final int MAX_TEMPLATE_RULES = 16;
    /** x and z. A populator may write one chunk around its own (FEASIBILITY), minus a block for edge updates. */
    public static final int MAX_TEMPLATE_WIDTH = 32;
    public static final int MAX_TEMPLATE_HEIGHT = 64;
    public static final int MAX_RARITY = 10_000;
    public static final int MAX_Y_OFFSET = 32;
    public static final double MIN_GRAVITY = 0.1, MAX_GRAVITY = 4.0;
    /** "Plugin" or "Plugin:id", as in bukkit.yml. */
    private static final java.util.regex.Pattern GENERATOR = java.util.regex.Pattern.compile("[A-Za-z0-9_.-]{1,64}(:\\S{1,128})?");

    private final Map<String, Set<Capability>> providers;
    private final Catalog catalog;

    /** @param providers provider id → what it controls */
    public ProfileValidator(Map<String, Set<Capability>> providers, Catalog catalog) {
        this.providers = Map.copyOf(providers);
        this.catalog = catalog;
    }

    /** Height range of a world created by name with this environment (FEASIBILITY P1, P5). */
    public static int minY(String environment) {
        return "normal".equals(environment) ? -64 : 0;
    }

    /** Exclusive. */
    public static int maxY(String environment) {
        return "normal".equals(environment) ? 320 : 256;
    }

    public List<Problem> validate(Profile p) {
        List<Problem> out = new ArrayList<>();
        Ids.checkId("id", p.id()).ifPresent(out::add);
        if (p.name() == null || p.name().isBlank() || p.name().length() > 64) {
            out.add(Problem.error("name", "debe tener entre 1 y 64 caracteres."));
        }
        if (p.description() != null && p.description().length() > 256) {
            out.add(Problem.error("description", "como mucho 256 caracteres."));
        }

        Set<Capability> caps = providers.get(p.provider());
        if (caps == null) {
            out.add(Problem.error("provider", "«" + p.provider() + "» no existe. Hay: " + String.join(", ", providers.keySet().stream().sorted().toList()) + "."));
            return out;
        }

        String env = p.environment();
        if (!Set.of("normal", "nether", "the_end").contains(env)) {
            out.add(Problem.error("environment", "«" + env + "» no existe: normal, nether o the_end."));
            env = "normal";
        }
        int minY = minY(env), maxY = maxY(env);
        if (!"normal".equals(env)) {
            need(out, caps, Capability.ENVIRONMENT, "environment", p.provider());
        }

        if (!"normal".equals(p.worldType())) {
            need(out, caps, Capability.WORLD_TYPE, "world-type", p.provider());
            if (!Set.of("amplified", "large_biomes").contains(p.worldType())) {
                out.add(Problem.error("world-type", "«" + p.worldType() + "» no existe: normal, amplified o large_biomes."));
            }
        }
        if (Boolean.TRUE.equals(p.structures())) {
            need(out, caps, Capability.STRUCTURES, "structures", p.provider());
        }

        if (p.spawn() != null) {
            need(out, caps, Capability.SPAWN, "spawn", p.provider());
            if (p.spawn().y() < minY || p.spawn().y() >= maxY) {
                out.add(Problem.error("spawn.y", "fuera del mundo (" + minY + " a " + (maxY - 1) + ")."));
            }
        }

        if (p.platform() != null) {
            need(out, caps, Capability.PLATFORM, "platform", p.provider());
            block(out, "platform.block", p.platform().block());
            if (p.platform().radius() < 0 || p.platform().radius() > MAX_PLATFORM_RADIUS) {
                out.add(Problem.error("platform.radius", "de 0 a " + MAX_PLATFORM_RADIUS + "."));
            }
            if (p.platform().y() < minY || p.platform().y() >= maxY) {
                out.add(Problem.error("platform.y", "fuera del mundo (" + minY + " a " + (maxY - 1) + ")."));
            }
        }

        if (!p.layers().isEmpty()) {
            need(out, caps, Capability.LAYERS, "layers", p.provider());
            int total = 0;
            for (int i = 0; i < p.layers().size(); i++) {
                Profile.Layer l = p.layers().get(i);
                block(out, "layers[" + i + "].block", l.block());
                if (l.height() < 1) {
                    out.add(Problem.error("layers[" + i + "].height", "al menos 1."));
                }
                total += Math.max(0, l.height());
            }
            if (total > maxY - minY) {
                out.add(Problem.error("layers", "suman " + total + " bloques y el mundo solo tiene " + (maxY - minY) + "."));
            }
        } else if (caps.contains(Capability.LAYERS) && "flat".equals(p.provider())) {
            out.add(Problem.error("layers", "un mundo plano necesita al menos una capa."));
        }

        if (!p.biomes().isEmpty()) {
            need(out, caps, Capability.BIOMES, "biomes", p.provider());
            if (p.biomes().size() > MAX_BIOMES) {
                out.add(Problem.error("biomes", "como mucho " + MAX_BIOMES + "."));
            }
            for (int i = 0; i < p.biomes().size(); i++) {
                biome(out, "biomes[" + i + "]", p.biomes().get(i));
            }
        }

        if (!p.ores().isEmpty()) {
            need(out, caps, Capability.ORES, "ores", p.provider());
            if (p.ores().size() > MAX_ORE_RULES) {
                out.add(Problem.error("ores", "como mucho " + MAX_ORE_RULES + " reglas."));
            }
            Set<String> ids = new HashSet<>();
            for (int i = 0; i < p.ores().size(); i++) {
                ore(out, "ores[" + i + "]", p.ores().get(i), ids, minY, maxY);
            }
        }
        if ("plugin".equals(p.provider())) {
            if (p.generator() == null) {
                out.add(Problem.error("generator", "falta: el generador del otro plugin, como en bukkit.yml (Plugin o Plugin:id)."));
            } else if (!GENERATOR.matcher(p.generator()).matches()) {
                out.add(Problem.error("generator", "«" + p.generator() + "» no tiene la forma Plugin o Plugin:id."));
            } else {
                String why = catalog.generatorProblem(p.generator());
                if (why != null) {
                    out.add(Problem.error("generator", why));
                }
            }
        } else if (p.generator() != null) {
            out.add(Problem.error("generator", "solo con provider: plugin."));
        }
        if ("terrain".equals(p.provider())) {
            if (p.terrain() == null) {
                out.add(Problem.error("terrain", "falta: el id de un terreno de terrains/ (/wf terrain list)."));
            } else if (!catalog.isTerrain(p.terrain())) {
                out.add(Problem.error("terrain", "no hay un terreno «" + p.terrain() + "» en terrains/."));
            }
        } else if (p.terrain() != null) {
            out.add(Problem.error("terrain", "solo con provider: terrain."));
        }

        if (p.playerGravity() != null && !(p.playerGravity() >= MIN_GRAVITY && p.playerGravity() <= MAX_GRAVITY)) {
            out.add(Problem.error("player-gravity", "de " + MIN_GRAVITY + " a " + MAX_GRAVITY + " (1 = normal)."));
        }

        if (!p.templates().isEmpty()) {
            need(out, caps, Capability.TEMPLATES, "templates", p.provider());
            if (p.templates().size() > MAX_TEMPLATE_RULES) {
                out.add(Problem.error("templates", "como mucho " + MAX_TEMPLATE_RULES + " reglas."));
            }
            Set<String> ids = new HashSet<>();
            for (int i = 0; i < p.templates().size(); i++) {
                template(out, "templates[" + i + "]", p.templates().get(i), ids, minY, maxY);
            }
        }
        return out;
    }

    private void template(List<Problem> out, String path, TemplateRule t, Set<String> ids, int minY, int maxY) {
        Ids.checkId(path + ".id", t.id()).ifPresent(out::add);
        if (!ids.add(t.id())) {
            out.add(Problem.error(path + ".id", "«" + t.id() + "» está repetido."));
        }
        int height = 0;
        var badName = Ids.checkId(path + ".structure", t.structure());
        if (badName.isPresent()) {
            out.add(badName.get());
        } else {
            Result<int[]> size = catalog.template(t.structure());
            if (!size.isOk()) {
                out.add(Problem.error(path + ".structure", size.errors().getFirst().message()));
            } else {
                int[] s = size.value();
                height = s[1];
                if (s[0] > MAX_TEMPLATE_WIDTH || s[2] > MAX_TEMPLATE_WIDTH || s[1] > MAX_TEMPLATE_HEIGHT) {
                    out.add(Problem.error(path + ".structure", "«" + t.structure() + "» mide " + s[0] + "×" + s[1] + "×" + s[2]
                            + "; como mucho " + MAX_TEMPLATE_WIDTH + " de ancho y de fondo y " + MAX_TEMPLATE_HEIGHT + " de alto."));
                }
            }
        }
        if (t.rarity() < 1 || t.rarity() > MAX_RARITY) {
            out.add(Problem.error(path + ".rarity", "de 1 (cada chunk) a " + MAX_RARITY + "."));
        }
        if (!Set.of("surface", "underground").contains(t.placement())) {
            out.add(Problem.error(path + ".placement", "«" + t.placement() + "» no existe: surface o underground."));
        }
        if (!Set.of("random", "none").contains(t.rotation())) {
            out.add(Problem.error(path + ".rotation", "«" + t.rotation() + "» no existe: random o none."));
        }
        if (Math.abs(t.yOffset()) > MAX_Y_OFFSET) {
            out.add(Problem.error(path + ".y-offset", "de -" + MAX_Y_OFFSET + " a " + MAX_Y_OFFSET + "."));
        }
        int lo = t.minY() == null ? minY : t.minY(), hi = t.maxY() == null ? maxY - 1 : t.maxY();
        if (lo > hi) {
            out.add(Problem.error(path + ".min-y", "es mayor que max-y."));
        } else if (lo < minY || hi >= maxY) {
            out.add(Problem.error(path, "alturas fuera del mundo (" + minY + " a " + (maxY - 1) + ")."));
        } else if ("underground".equals(t.placement()) && lo + height > maxY) {
            out.add(Problem.error(path + ".min-y", "con " + height + " de alto la plantilla no cabe por debajo del techo del mundo (" + (maxY - 1) + ")."));
        }
        for (int i = 0; i < t.biomes().size(); i++) {
            biome(out, path + ".biomes[" + i + "]", t.biomes().get(i));
        }
        for (int i = 0; i < t.excludeBiomes().size(); i++) {
            biome(out, path + ".exclude-biomes[" + i + "]", t.excludeBiomes().get(i));
            if (t.biomes().contains(t.excludeBiomes().get(i))) {
                out.add(Problem.error(path + ".exclude-biomes[" + i + "]", "está a la vez permitido y excluido."));
            }
        }
    }

    private void ore(List<Problem> out, String path, OreRule o, Set<String> ids, int minY, int maxY) {
        Ids.checkId(path + ".id", o.id()).ifPresent(out::add);
        if (!ids.add(o.id())) {
            out.add(Problem.error(path + ".id", "«" + o.id() + "» está repetido."));
        }
        block(out, path + ".block", o.block());
        for (int i = 0; i < o.replaces().size(); i++) {
            block(out, path + ".replaces[" + i + "]", o.replaces().get(i));
        }
        if (o.veinsPerChunk() < 0 || o.veinsPerChunk() > MAX_VEINS) {
            out.add(Problem.error(path + ".veins-per-chunk", "de 0 a " + MAX_VEINS + "."));
        }
        if (o.size() < 1 || o.size() > MAX_VEIN_SIZE) {
            out.add(Problem.error(path + ".size", "de 1 a " + MAX_VEIN_SIZE + "."));
        }
        if (o.minY() > o.maxY()) {
            out.add(Problem.error(path + ".min-y", "es mayor que max-y."));
        }
        if (o.minY() < minY || o.maxY() >= maxY) {
            out.add(Problem.error(path, "alturas fuera del mundo (" + minY + " a " + (maxY - 1) + ")."));
        }
        if (!Set.of("uniform", "triangle").contains(o.distribution())) {
            out.add(Problem.error(path + ".distribution", "«" + o.distribution() + "» no existe: uniform o triangle."));
        }
        for (int i = 0; i < o.biomes().size(); i++) {
            biome(out, path + ".biomes[" + i + "]", o.biomes().get(i));
        }
        for (int i = 0; i < o.excludeBiomes().size(); i++) {
            biome(out, path + ".exclude-biomes[" + i + "]", o.excludeBiomes().get(i));
            if (o.biomes().contains(o.excludeBiomes().get(i))) {
                out.add(Problem.error(path + ".exclude-biomes[" + i + "]", "está a la vez permitido y excluido."));
            }
        }
    }

    private static void need(List<Problem> out, Set<Capability> caps, Capability c, String field, String provider) {
        if (!caps.contains(c)) {
            out.add(Problem.error(field, "el proveedor «" + provider + "» no controla " + c.label() + "."));
        }
    }

    private void block(List<Problem> out, String field, String key) {
        if (!catalog.isBlock(key)) {
            out.add(Problem.error(field, "«" + key + "» no es un bloque de este servidor."));
        }
    }

    private void biome(List<Problem> out, String field, String key) {
        if (!catalog.isBiome(key)) {
            out.add(Problem.error(field, "«" + key + "» no es un bioma de este servidor."));
        }
    }
}
