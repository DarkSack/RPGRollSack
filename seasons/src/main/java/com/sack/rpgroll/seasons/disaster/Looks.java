package com.sack.rpgroll.seasons.disaster;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Partículas, sonidos y efectos escritos como texto en el YAML de un desastre, separados por espacios:
 * <ul>
 *   <li>partícula: {@code NOMBRE[*cantidad] [dato] [tamaño]}; el dato es un bloque ({@code BLOCK},
 *       {@code FALLING_DUST}), un color {@code #RRGGBB} ({@code DUST}) o un objeto ({@code ITEM}).
 *       Ej. {@code "FALLING_DUST*30 LIME_CONCRETE_POWDER"}, {@code "DUST*8 #7CFC00 1.2"}.</li>
 *   <li>sonido: {@code NOMBRE [volumen] [tono]}; el nombre puede ser una clave del pack de recursos
 *       ({@code minecraft:entity.generic.explode}, {@code sackito:viento}). {@code ""} = ninguno.</li>
 *   <li>efecto: {@code NOMBRE [nivel] [segundos]}, ej. {@code "SLOWNESS 2 5"}.</li>
 * </ul>
 * Lo que no se entiende lanza {@link IllegalArgumentException} con el motivo.
 */
final class Looks {

    private Looks() {
    }

    /** @param count cuántas por llamada; 0 = las que diga quien la lanza */
    record ParticleLook(Particle particle, Object data, int count) {

        void spawn(World world, Location at, int defaultCount, double spreadX, double spreadY, double spreadZ,
                double speed) {
            world.spawnParticle(particle, at, count > 0 ? count : defaultCount, spreadX, spreadY, spreadZ, speed, data,
                    true);
        }
    }

    /** Sin {@code sound} ni {@code key} no suena nada. */
    record SoundLook(Sound sound, String key, float volume, float pitch) {

        static final SoundLook NONE = new SoundLook(null, null, 0, 0);

        void play(World world, Location at) {
            if (sound != null) {
                world.playSound(at, sound, volume, pitch);
            } else if (key != null) {
                world.playSound(at, key, volume, pitch);
            }
        }

        void play(Player player) {
            if (sound != null) {
                player.playSound(player.getLocation(), sound, volume, pitch);
            } else if (key != null) {
                player.playSound(player.getLocation(), key, volume, pitch);
            }
        }
    }

    static ParticleLook particle(String spec) {

        String[] parts = spec.trim().split("\\s+");
        String[] head = parts[0].split("\\*", 2);
        Particle particle = Particle.valueOf(head[0].toUpperCase(Locale.ROOT));
        int count = head.length == 2 ? Integer.parseInt(head[1]) : 0;
        String arg = parts.length > 1 ? parts[1] : null;
        Class<?> type = particle.getDataType();

        Object data;
        if (type == Void.class) {
            data = null;
        } else if (type == BlockData.class) {
            data = material(arg == null ? "STONE" : arg).createBlockData();
        } else if (type == Particle.DustOptions.class) {
            data = new Particle.DustOptions(color(arg == null ? "#FFFFFF" : arg),
                    parts.length > 2 ? Float.parseFloat(parts[2]) : 1f);
        } else if (type == ItemStack.class) {
            data = new ItemStack(item(arg == null ? "STONE" : arg));
        } else if (type == Color.class) {
            data = color(arg == null ? "#FFFFFF" : arg);
        } else if (type == Float.class) {
            data = arg == null ? 0f : Float.parseFloat(arg);
        } else if (type == Integer.class) {
            data = arg == null ? 0 : Integer.parseInt(arg);
        } else {
            throw new IllegalArgumentException("la partícula " + particle + " pide un dato que no se puede escribir aquí");
        }
        return new ParticleLook(particle, data, count);
    }

    static SoundLook sound(String spec) {

        String[] parts = spec.trim().split("\\s+");
        if (parts[0].isEmpty()) {
            return SoundLook.NONE;
        }
        float volume = parts.length > 1 ? Float.parseFloat(parts[1]) : 1f;
        float pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1f;

        // Con punto o dos puntos es una clave (también las del pack); si no, el nombre de la lista de Bukkit.
        if (parts[0].contains(".") || parts[0].contains(":")) {
            NamespacedKey key = NamespacedKey.fromString(parts[0].toLowerCase(Locale.ROOT));
            if (key == null) {
                throw new IllegalArgumentException("clave de sonido inválida: " + parts[0]);
            }
            return new SoundLook(null, key.asString(), volume, pitch);
        }
        return new SoundLook(namedSound(parts[0].toUpperCase(Locale.ROOT)), null, volume, pitch);
    }

    /** {@code Sound.valueOf} se va a quitar de la API: se lee la constante con ese nombre. */
    private static Sound namedSound(String name) {
        try {
            if (Sound.class.getField(name).get(null) instanceof Sound sound) {
                return sound;
            }
        } catch (ReflectiveOperationException ignored) {
            // cae al error de abajo
        }
        throw new IllegalArgumentException("sonido desconocido: " + name);
    }

    @SuppressWarnings("deprecation")
    static PotionEffect potion(String spec) {

        String[] parts = spec.trim().split("\\s+");
        PotionEffectType type = PotionEffectType.getByName(parts[0].toUpperCase(Locale.ROOT));
        if (type == null) {
            throw new IllegalArgumentException("efecto desconocido: " + parts[0]);
        }
        int level = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
        double seconds = parts.length > 2 ? Double.parseDouble(parts[2]) : 5;
        return new PotionEffect(type, (int) Math.round(seconds * 20), Math.max(0, level - 1));
    }

    static Material material(String name) {
        Material material = Material.matchMaterial(name.trim());
        if (material == null || !material.isBlock()) {
            throw new IllegalArgumentException("bloque desconocido: " + name);
        }
        return material;
    }

    /** Como {@link #material}, pero {@code *} vale por cualquier texto: {@code *_LEAVES} son todas las hojas. */
    static List<Material> materials(String name) {
        String pattern = name.trim().toUpperCase(Locale.ROOT);
        if (!pattern.contains("*")) {
            return List.of(material(pattern));
        }
        String regex = Pattern.quote(pattern).replace("*", "\\E.*\\Q");
        List<Material> found = Arrays.stream(Material.values())
                .filter(material -> material.name().matches(regex) && !material.isLegacy() && material.isBlock())
                .toList();
        if (found.isEmpty()) {
            throw new IllegalArgumentException("ningún bloque encaja con " + name);
        }
        return found;
    }

    static Material item(String name) {
        Material material = Material.matchMaterial(name.trim());
        if (material == null || !material.isItem()) {
            throw new IllegalArgumentException("objeto desconocido: " + name);
        }
        return material;
    }

    static Color color(String hex) {
        try {
            return Color.fromRGB(Integer.parseInt(hex.trim().replace("#", ""), 16));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("color inválido: " + hex + " (usa #RRGGBB)");
        }
    }

}
