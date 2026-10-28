package com.sack.rpgroll.items.registry;

import com.sack.rpgroll.items.core.ItemAction;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.Locale;

/**
 * La estela de un disparo de {@code SHOOT}: un núcleo ({@code particle}, {@code color}, {@code size})
 * y, si hay {@code particle2}, una segunda partícula que se enrosca alrededor ({@code spiral} de radio)
 * o acompaña al núcleo. {@code density} son puntos por bloque.
 */
record ShotVisuals(Vector origin, Particle core, Object coreData, Particle accent, Object accentData,
                   double spiral, double density) {

    private static final double NEAR = 3.0;

    static ShotVisuals from(ItemAction action, Vector origin) {
        Particle core = particle(action.param("particle", "CRIT"));
        Particle accent = action.param("particle2", "").isBlank() ? null : particle(action.param("particle2", ""));
        return new ShotVisuals(origin, core,
                data(core, action.param("color", "#FF3030"), action.param("size", "0.8")),
                accent, accent == null ? null : data(accent, action.param("color2", "#FFFFFF"), action.param("size2", "0.6")),
                Double.parseDouble(action.param("spiral", "0")), Double.parseDouble(action.param("density", "3")));
    }

    void trail(World world, Vector from, Vector to) {
        Vector line = to.clone().subtract(from);
        double length = line.length();
        if (length < 1e-6) {
            return;
        }
        Vector dir = line.clone().multiply(1.0 / length);
        // Dos perpendiculares a la dirección para la espiral.
        Vector side = Math.abs(dir.getY()) > 0.9 ? new Vector(1, 0, 0) : new Vector(0, 1, 0);
        Vector u = dir.getCrossProduct(side).normalize();
        Vector v = dir.getCrossProduct(u).normalize();
        int points = Math.max(1, (int) Math.ceil(length * density));
        for (int i = 1; i <= points; i++) {
            Vector p = from.clone().add(dir.clone().multiply(length * i / points));
            if (p.distanceSquared(origin) < NEAR * NEAR) {
                continue; // pegado a la cámara del tirador una partícula tapa media pantalla
            }
            world.spawnParticle(core, p.getX(), p.getY(), p.getZ(), 1, 0, 0, 0, 0, coreData, true);
            if (accent == null) {
                continue;
            }
            if (spiral > 0) {
                double angle = p.distance(origin) * 2.2;
                p.add(u.clone().multiply(Math.cos(angle) * spiral)).add(v.clone().multiply(Math.sin(angle) * spiral));
            }
            world.spawnParticle(accent, p.getX(), p.getY(), p.getZ(), 1, 0, 0, 0, 0, accentData, true);
        }
    }

    private static Particle particle(String name) {
        return Particle.valueOf(name.toUpperCase(Locale.ROOT));
    }

    private static Object data(Particle particle, String color, String size) {
        if (particle.getDataType() == Particle.DustOptions.class) {
            return new Particle.DustOptions(Color.fromRGB(Integer.parseInt(color.replace("#", ""), 16)),
                    Float.parseFloat(size));
        }
        if (particle.getDataType() == Color.class) {
            return Color.fromRGB(Integer.parseInt(color.replace("#", ""), 16));
        }
        if (particle.getDataType() != Void.class) {
            throw new IllegalArgumentException("la partícula necesita datos: " + particle);
        }
        return null;
    }
}
