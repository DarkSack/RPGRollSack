package com.sack.rpgroll.sackeffects.engine;

import com.sack.rpgroll.sackeffects.core.EffectStep;
import com.sack.rpgroll.sackeffects.core.EffectStepType;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParticleShapesTest {

    private final Location origin = new Location(null, 0, 64, 0);

    private EffectStep step(Map<String, String> params) {
        return new EffectStep(EffectStepType.PARTICLE, 0, params);
    }

    @Test
    void unknownShapeFallsBackToASinglePointAtOrigin() {
        List<Location> points = ParticleShapes.generate(step(Map.of("shape", "NOT_A_SHAPE")), origin, null);

        assertEquals(1, points.size());
        assertEquals(origin, points.get(0));
    }

    @Test
    void missingShapeDefaultsToPoint() {
        List<Location> points = ParticleShapes.generate(step(Map.of()), origin, null);
        assertEquals(1, points.size());
    }

    @Test
    void pointResultIsAClonedLocationNotTheOriginalInstance() {
        List<Location> points = ParticleShapes.generate(step(Map.of("shape", "POINT")), origin, null);

        assertEquals(origin, points.get(0));
        assertTrue(points.get(0) != origin, "debe ser un clon, no la misma instancia");
    }

    @Test
    void circleGeneratesExactlyTheRequestedPointCount() {
        List<Location> points = ParticleShapes.generate(step(Map.of("shape", "CIRCLE", "points", "12")), origin, null);
        assertEquals(12, points.size());
    }

    @Test
    void circleEveryPointIsExactlyRadiusAwayFromOriginOnTheHorizontalPlane() {
        double radius = 3.0;
        List<Location> points = ParticleShapes.generate(
                step(Map.of("shape", "CIRCLE", "points", "16", "radius", String.valueOf(radius))), origin, null);

        for (Location point : points) {
            double dx = point.getX() - origin.getX();
            double dz = point.getZ() - origin.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);

            assertEquals(radius, distance, 1e-9);
            assertEquals(origin.getY(), point.getY(), 1e-9);
        }
    }

    @Test
    void circleClampsPointsBelowOneToOne() {
        List<Location> points = ParticleShapes.generate(step(Map.of("shape", "CIRCLE", "points", "0")), origin, null);
        assertEquals(1, points.size());
    }

    @Test
    void sphereEveryPointLiesExactlyOnTheSphereSurface() {
        double radius = 2.0;
        List<Location> points = ParticleShapes.generate(
                step(Map.of("shape", "SPHERE", "points", "50", "radius", String.valueOf(radius))), origin, null);

        assertEquals(50, points.size());

        for (Location point : points) {
            double dx = point.getX() - origin.getX();
            double dy = point.getY() - origin.getY();
            double dz = point.getZ() - origin.getZ();
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

            assertEquals(radius, distance, 1e-6);
        }
    }

    @Test
    void lineStartsAtFromAndEndsAtTo() {
        Location to = origin.clone().add(10, 0, 0);
        List<Location> points = ParticleShapes.generate(step(Map.of("shape", "LINE", "points", "5")), origin, to);

        assertEquals(origin, points.get(0));
        assertEquals(to, points.get(points.size() - 1));
    }

    @Test
    void lineWithNullSecondaryDefaultsToOriginAsBothEnds() {
        List<Location> points = ParticleShapes.generate(step(Map.of("shape", "LINE", "points", "4")), origin, null);

        for (Location point : points) {
            assertEquals(origin, point);
        }
    }

    @Test
    void lineClampsSampleCountToAtLeastTwo() {
        Location to = origin.clone().add(2, 0, 0);
        List<Location> points = ParticleShapes.generate(step(Map.of("shape", "LINE", "points", "0")), origin, to);

        assertEquals(2, points.size());
        assertEquals(origin, points.get(0));
        assertEquals(to, points.get(1));
    }

    @Test
    void helixGainsHeightLinearlyAndGeneratesRequestedPointCount() {
        List<Location> points = ParticleShapes.generate(
                step(Map.of("shape", "HELIX", "points", "10", "height", "10", "turns", "2")), origin, null);

        assertEquals(10, points.size());
        assertEquals(origin.getY(), points.get(0).getY(), 1e-9);
        // El último punto está en t = 9/10, no llega a la altura completa.
        assertEquals(origin.getY() + 9.0, points.get(points.size() - 1).getY(), 1e-9);
    }

    @Test
    void cubeOutlineHasTwelveEdgesTimesPointsPerEdge() {
        List<Location> points = ParticleShapes.generate(
                step(Map.of("shape", "CUBE_OUTLINE", "points", "24")), origin, null);

        // pointsPerEdge = max(2, points/12) = max(2, 2) = 2 -> 12 edges * 2 = 24
        assertEquals(24, points.size());
    }

    @Test
    void cubeOutlineClampsPointsPerEdgeToAtLeastTwo() {
        List<Location> points = ParticleShapes.generate(
                step(Map.of("shape", "CUBE_OUTLINE", "points", "1")), origin, null);

        // pointsPerEdge = max(2, 1/12) = max(2, 0) = 2 -> 12 * 2 = 24
        assertEquals(24, points.size());
    }

    @Test
    void burstGeneratesRequestedPointCountWithinRadiusBounds() {
        double radius = 4.0;
        List<Location> points = ParticleShapes.generate(
                step(Map.of("shape", "BURST", "points", "30", "radius", String.valueOf(radius))), origin, null);

        assertEquals(30, points.size());

        for (Location point : points) {
            assertTrue(Math.abs(point.getX() - origin.getX()) <= radius);
            assertTrue(Math.abs(point.getY() - origin.getY()) <= radius);
            assertTrue(Math.abs(point.getZ() - origin.getZ()) <= radius);
        }
    }

    @Test
    void coneGeneratesRequestedPointCount() {
        Location facingOrigin = new Location(null, 0, 64, 0, 0f, 0f);
        List<Location> points = ParticleShapes.generate(
                step(Map.of("shape", "CONE", "points", "15", "length", "5", "radius", "2")), facingOrigin, null);

        assertEquals(15, points.size());
    }

    @Test
    void coneFirstPointStartsAtOriginSinceRingRadiusIsZeroAtTZero() {
        Location facingOrigin = new Location(null, 0, 64, 0, 0f, 0f);
        List<Location> points = ParticleShapes.generate(
                step(Map.of("shape", "CONE", "points", "10", "length", "5", "radius", "2")), facingOrigin, null);

        assertEquals(facingOrigin.getX(), points.get(0).getX(), 1e-9);
        assertEquals(facingOrigin.getY(), points.get(0).getY(), 1e-9);
        assertEquals(facingOrigin.getZ(), points.get(0).getZ(), 1e-9);
    }
}
