package com.sack.rpgroll.fx.engine;

import com.sack.rpgroll.fx.core.EffectStep;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * El dato extra que algunas partículas necesitan para poder salir: el color y
 * tamaño del polvo ({@code DUST}), los dos colores de {@code DUST_COLOR_TRANSITION},
 * el bloque de {@code BLOCK}/{@code FALLING_DUST}, el ítem de {@code ITEM}...
 * <p>
 * Sin esto esas partículas fallaban, y son justo las que dan color a un
 * efecto: con solo FLAME/END_ROD/SNOWFLAKE todo se ve de los mismos tres tonos.
 * <p>
 * Params del step que lee:
 * <ul>
 *   <li>{@code color}: {@code "#FF8800"}, {@code "255,136,0"}, un nombre ({@code GOLD}),
 *       una lista {@code "#FF0000,#FFFF00"} (degradado a lo largo de la figura) o
 *       {@code RAINBOW} (arcoíris que además va girando en cada repetición)</li>
 *   <li>{@code color-to}: color final; en DUST hace un degradado entre {@code color} y
 *       este, en DUST_COLOR_TRANSITION es el color al que se apaga cada partícula</li>
 *   <li>{@code size}: tamaño del polvo (0.01 a 4, por defecto 1)</li>
 *   <li>{@code block} / {@code item}: material para las partículas de bloque o ítem</li>
 *   <li>{@code power}: para las partículas que piden un número (DRAGON_BREATH...)</li>
 * </ul>
 */
public final class ParticleData {

    private interface Source {
        Object at(int index, int total, int iteration);
    }

    private final Source source;

    private ParticleData(Source source) {
        this.source = source;
    }

    /** El dato para el punto {@code index} de {@code total}, en la repetición {@code iteration}. */
    public Object forPoint(int index, int total, int iteration) {
        return source.at(index, total, iteration);
    }

    private static ParticleData constant(Object value) {
        return new ParticleData((index, total, iteration) -> value);
    }

    /** @return el dato a usar, o null si la partícula pide algo que FX no sabe construir. */
    public static ParticleData of(Particle particle, EffectStep step) {

        Class<?> type = particle.getDataType();

        if (type == Void.class) {
            return constant(null);
        }

        float size = (float) Math.max(0.01, Math.min(4.0, step.paramDouble("size", 1.0)));

        if (type == Particle.DustOptions.class) {
            ColorSource colors = ColorSource.parse(step.param("color", "#FFFFFF"), step.param("color-to", ""));
            return new ParticleData((index, total, iteration) ->
                    new Particle.DustOptions(colors.at(index, total, iteration), size));
        }

        if (type == Particle.DustTransition.class) {
            ColorSource colors = ColorSource.parse(step.param("color", "#FFFFFF"), "");
            Color to = firstOr(parseColors(step.param("color-to", "#FFFFFF")), Color.WHITE);
            return new ParticleData((index, total, iteration) ->
                    new Particle.DustTransition(colors.at(index, total, iteration), to, size));
        }

        if (type == Color.class) {
            ColorSource colors = ColorSource.parse(step.param("color", "#FFFFFF"), step.param("color-to", ""));
            return new ParticleData(colors::at);
        }

        if (type == Float.class) {
            return constant((float) step.paramDouble("power", 1.0));
        }

        if (type == Integer.class) {
            return constant(step.paramInt("data-delay", 0));
        }

        if (BlockData.class.isAssignableFrom(type)) {
            Material material = Material.matchMaterial(step.param("block", "STONE"));
            return material != null && material.isBlock() ? constant(material.createBlockData()) : null;
        }

        if (type == ItemStack.class) {
            Material material = Material.matchMaterial(step.param("item", "DIAMOND"));
            return material != null && material.isItem() ? constant(new ItemStack(material)) : null;
        }

        return null;
    }

    /** Lista de colores separados por comas; "r,g,b" con tres números cuenta como uno solo. */
    public static List<Color> parseColors(String raw) {

        List<Color> result = new ArrayList<>();

        if (raw == null || raw.isBlank()) {
            return result;
        }

        String[] parts = raw.split(",");

        if (parts.length == 3 && isNumber(parts[0]) && isNumber(parts[1]) && isNumber(parts[2])) {
            result.add(Color.fromRGB(channel(parts[0]), channel(parts[1]), channel(parts[2])));
            return result;
        }

        for (String part : parts) {
            Color color = parseColor(part.trim());
            if (color != null) {
                result.add(color);
            }
        }

        return result;
    }

    private static Color parseColor(String raw) {

        if (raw.isEmpty()) {
            return null;
        }

        String hex = raw.startsWith("#") ? raw.substring(1) : raw;

        if (hex.length() == 6 && hex.chars().allMatch(c -> Character.digit(c, 16) >= 0)) {
            return Color.fromRGB(Integer.parseInt(hex, 16));
        }

        try {
            return (Color) Color.class.getField(raw.toUpperCase(Locale.ROOT)).get(null);
        } catch (ReflectiveOperationException | ClassCastException e) {
            return null;
        }
    }

    private static boolean isNumber(String raw) {
        return raw.trim().matches("\\d{1,3}");
    }

    private static int channel(String raw) {
        return Math.max(0, Math.min(255, Integer.parseInt(raw.trim())));
    }

    private static Color firstOr(List<Color> colors, Color fallback) {
        return colors.isEmpty() ? fallback : colors.get(0);
    }

    /** De dónde sale el color de cada punto: fijo, degradado entre varias paradas o arcoíris. */
    private static final class ColorSource {

        private final List<Color> stops;
        private final boolean rainbow;

        private ColorSource(List<Color> stops, boolean rainbow) {
            this.stops = stops;
            this.rainbow = rainbow;
        }

        static ColorSource parse(String raw, String to) {

            if (raw.trim().equalsIgnoreCase("RAINBOW")) {
                return new ColorSource(List.of(), true);
            }

            List<Color> stops = parseColors(raw);
            stops.addAll(parseColors(to));

            if (stops.isEmpty()) {
                stops.add(Color.WHITE);
            }

            return new ColorSource(stops, false);
        }

        Color at(int index, int total, int iteration) {

            double t = total <= 1 ? 0 : (double) index / (total - 1);

            if (rainbow) {
                double hue = (t + iteration * 0.07) % 1.0;
                return hsv(hue);
            }

            if (stops.size() == 1) {
                return stops.get(0);
            }

            double scaled = t * (stops.size() - 1);
            int from = Math.min(stops.size() - 2, (int) Math.floor(scaled));
            double local = scaled - from;

            return lerp(stops.get(from), stops.get(from + 1), local);
        }

        private static Color lerp(Color a, Color b, double t) {
            return Color.fromRGB(
                    (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                    (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                    (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
        }

        /** Tono puro (saturación y brillo al máximo) para el arcoíris. */
        private static Color hsv(double hue) {

            double h = hue * 6;
            int sector = (int) Math.floor(h) % 6;
            double f = h - Math.floor(h);
            int up = (int) Math.round(255 * f);
            int down = 255 - up;

            return switch (sector) {
                case 0 -> Color.fromRGB(255, up, 0);
                case 1 -> Color.fromRGB(down, 255, 0);
                case 2 -> Color.fromRGB(0, 255, up);
                case 3 -> Color.fromRGB(0, down, 255);
                case 4 -> Color.fromRGB(up, 0, 255);
                default -> Color.fromRGB(255, 0, down);
            };
        }
    }

}
