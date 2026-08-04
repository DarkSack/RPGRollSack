package com.sack.rpgroll.sackeffects.engine;

import com.sack.rpgroll.sackeffects.core.EffectStep;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Geometría pura: convierte los params de un step {@code PARTICLE} en la
 * lista de puntos donde hay que spawnear la partícula. No sabe nada de
 * {@code World#spawnParticle} ni de {@code Particle} — solo matemática, así
 * es fácil de leer y de testear por separado del motor de ejecución.
 * <p>
 * Formas soportadas ({@code shape} en el step): POINT, CIRCLE, SPHERE,
 * LINE, HELIX, CONE, CUBE_OUTLINE, BURST. Cualquier otro valor (o ausente)
 * cae a POINT.
 */
public final class ParticleShapes {

    private static final Random RANDOM = new Random();

    private ParticleShapes() {
    }

    /**
     * @param origin    punto de anclaje de la forma (resuelto del target del step)
     * @param secondary segundo punto, solo usado por LINE (el "hasta"); puede ser null
     */
    public static List<Location> generate(EffectStep step, Location origin, Location secondary) {

        String shape = step.param("shape", "POINT").trim().toUpperCase(Locale.ROOT);
        int points = Math.max(1, step.paramInt("points", 20));
        double radius = step.paramDouble("radius", 1.0);

        return switch (shape) {
            case "CIRCLE" -> circle(origin, radius, points);
            case "SPHERE" -> sphere(origin, radius, points);
            case "LINE" -> line(origin, secondary != null ? secondary : origin, points);
            case "HELIX" -> helix(origin, radius, step.paramDouble("height", 2.0), step.paramDouble("turns", 3.0),
                    points);
            case "CONE" -> cone(origin, radius, step.paramDouble("length", 3.0), points);
            case "CUBE_OUTLINE" -> cubeOutline(origin, radius, Math.max(2, points / 12));
            case "BURST" -> burst(origin, radius, points);
            default -> List.of(origin.clone());
        };
    }

    private static List<Location> circle(Location origin, double radius, int points) {

        List<Location> result = new ArrayList<>();

        for (int i = 0; i < points; i++) {
            double angle = 2 * Math.PI * i / points;
            result.add(origin.clone().add(radius * Math.cos(angle), 0, radius * Math.sin(angle)));
        }

        return result;
    }

    /** Distribución pareja sobre la superficie de una esfera (espiral áurea). */
    private static List<Location> sphere(Location origin, double radius, int points) {

        List<Location> result = new ArrayList<>();
        double goldenAngle = Math.PI * (3 - Math.sqrt(5));

        for (int i = 0; i < points; i++) {

            double t = (i + 0.5) / points;
            double yFraction = 1 - 2 * t;
            double ringRadius = Math.sqrt(Math.max(0, 1 - yFraction * yFraction));
            double angle = goldenAngle * i;

            double x = Math.cos(angle) * ringRadius;
            double z = Math.sin(angle) * ringRadius;

            result.add(origin.clone().add(x * radius, yFraction * radius, z * radius));
        }

        return result;
    }

    private static List<Location> line(Location from, Location to, int points) {

        List<Location> result = new ArrayList<>();
        int samples = Math.max(2, points);

        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double dz = to.getZ() - from.getZ();

        for (int i = 0; i < samples; i++) {
            double t = (double) i / (samples - 1);
            result.add(from.clone().add(dx * t, dy * t, dz * t));
        }

        return result;
    }

    private static List<Location> helix(Location origin, double radius, double height, double turns, int points) {

        List<Location> result = new ArrayList<>();

        for (int i = 0; i < points; i++) {

            double t = (double) i / points;
            double angle = 2 * Math.PI * turns * t;

            result.add(origin.clone().add(radius * Math.cos(angle), height * t, radius * Math.sin(angle)));
        }

        return result;
    }

    /** Espiral saliendo de {@code origin} en la dirección que mire (yaw/pitch), ensanchándose con la distancia. */
    private static List<Location> cone(Location origin, double radius, double length, int points) {

        List<Location> result = new ArrayList<>();

        Vector axis = origin.getDirection().normalize();
        Vector[] basis = orthonormalBasis(axis);
        Vector right = basis[0];
        Vector up = basis[1];

        double goldenAngle = Math.PI * (3 - Math.sqrt(5));

        for (int i = 0; i < points; i++) {

            double t = (double) i / points;
            double angle = goldenAngle * i;
            double ringRadius = radius * t;

            Vector offset = right.clone().multiply(Math.cos(angle) * ringRadius)
                    .add(up.clone().multiply(Math.sin(angle) * ringRadius))
                    .add(axis.clone().multiply(length * t));

            result.add(origin.clone().add(offset));
        }

        return result;
    }

    private static final int[][] CUBE_EDGES = {
            { 0, 1 }, { 0, 2 }, { 0, 4 }, { 1, 3 }, { 1, 5 }, { 2, 3 },
            { 2, 6 }, { 3, 7 }, { 4, 5 }, { 4, 6 }, { 5, 7 }, { 6, 7 }
    };

    private static List<Location> cubeOutline(Location origin, double halfSize, int pointsPerEdge) {

        List<Vector> corners = new ArrayList<>();

        for (double sx : new double[] { -1, 1 }) {
            for (double sy : new double[] { -1, 1 }) {
                for (double sz : new double[] { -1, 1 }) {
                    corners.add(new Vector(sx * halfSize, sy * halfSize, sz * halfSize));
                }
            }
        }

        List<Location> result = new ArrayList<>();
        int perEdge = Math.max(2, pointsPerEdge);

        for (int[] edge : CUBE_EDGES) {

            Vector a = corners.get(edge[0]);
            Vector b = corners.get(edge[1]);

            for (int i = 0; i < perEdge; i++) {
                double t = (double) i / (perEdge - 1);
                Vector p = a.clone().multiply(1 - t).add(b.clone().multiply(t));
                result.add(origin.clone().add(p));
            }
        }

        return result;
    }

    private static List<Location> burst(Location origin, double radius, int points) {

        List<Location> result = new ArrayList<>();

        for (int i = 0; i < points; i++) {
            double x = (RANDOM.nextDouble() * 2 - 1) * radius;
            double y = (RANDOM.nextDouble() * 2 - 1) * radius;
            double z = (RANDOM.nextDouble() * 2 - 1) * radius;
            result.add(origin.clone().add(x, y, z));
        }

        return result;
    }

    /** Dos vectores perpendiculares entre sí y a {@code axis}, para poder "pararnos" en el plano de un cono. */
    private static Vector[] orthonormalBasis(Vector axis) {

        Vector arbitrary = Math.abs(axis.getY()) < 0.99 ? new Vector(0, 1, 0) : new Vector(1, 0, 0);

        Vector right = axis.clone().crossProduct(arbitrary).normalize();
        Vector up = axis.clone().crossProduct(right).normalize();

        return new Vector[] { right, up };
    }

}
