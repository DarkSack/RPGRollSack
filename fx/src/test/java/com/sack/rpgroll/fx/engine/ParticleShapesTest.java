package com.sack.rpgroll.fx.engine;

import com.sack.rpgroll.fx.core.EffectStep;
import com.sack.rpgroll.fx.core.EffectStepType;

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
                step(Map.of("shape", "CUBE_OUTLINE", "points", "120")), origin, null);

        // pointsPerEdge = max(4, points/12) = max(4, 10) = 10 -> 12 aristas * 10
        assertEquals(120, points.size());
    }

    /**
     * El mínimo por arista es 4, no 2: con 2 el cubo son solo sus esquinas y
     * no se reconoce la figura en el juego.
     */
    @Test
    void cubeOutlineClampsPointsPerEdgeToAtLeastFour() {
        List<Location> points = ParticleShapes.generate(
                step(Map.of("shape", "CUBE_OUTLINE", "points", "1")), origin, null);

        // pointsPerEdge = max(4, 1/12) = max(4, 0) = 4 -> 12 * 4 = 48
        assertEquals(48, points.size());
    }

    @Test
    void polygonWithThreeSidesTracesEachEdge() {
        List<Location> points = ParticleShapes.generate(
                step(Map.of("shape", "POLYGON", "sides", "3", "points", "90", "radius", "2")), origin, null);

        // perEdge = 90/3 = 30 -> 3 lados * 30
        assertEquals(90, points.size());
    }

    @Test
    void densityMultipliesThePointsOfAShape() {
        List<Location> single = ParticleShapes.generate(
                step(Map.of("shape", "CIRCLE", "points", "40")), origin, null);
        List<Location> dense = ParticleShapes.generate(
                step(Map.of("shape", "CIRCLE", "points", "40", "density", "2.5")), origin, null);

        assertEquals(40, single.size());
        assertEquals(100, dense.size());
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
