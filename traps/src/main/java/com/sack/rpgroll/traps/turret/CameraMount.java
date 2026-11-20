package com.sack.rpgroll.traps.turret;

import org.bukkit.block.BlockFace;

/**
 * Dónde está colgada una cámara, y con eso hasta dónde gira: de una pared barre de lado a lado
 * mirando hacia fuera del muro, y del techo además mira hacia abajo, pero ninguna da la vuelta
 * entera. Los ángulos son los de Minecraft: yaw 0 = sur, pitch positivo = hacia abajo.
 *
 * @param halfArc   cuánto gira a cada lado de hacia donde mira en reposo.
 * @param restPitch el cabeceo mientras barre.
 */
public enum CameraMount {

    WALL(80f, -30f, 75f, 15f),
    CEILING(120f, 0f, 90f, 40f);

    private final float halfArc;
    private final float minPitch;
    private final float maxPitch;
    private final float restPitch;

    CameraMount(float halfArc, float minPitch, float maxPitch, float restPitch) {
        this.halfArc = halfArc;
        this.minPitch = minPitch;
        this.maxPitch = maxPitch;
        this.restPitch = restPitch;
    }

    /** Por dónde va el barrido: grados desde el reposo y hacia qué lado (1 o -1). */
    public record Sweep(float offset, int direction) {
    }

    /** null si no es de los nuestros (una cámara de antes, o puesta por comando, gira entera). */
    public static CameraMount of(String raw) {
        for (CameraMount mount : values()) {
            if (mount.name().equals(raw)) {
                return mount;
            }
        }
        return null;
    }

    /**
     * Con la cara del muro sobre la que se puso el bloque (hacia dónde quedó respecto a él): un lado
     * es pared, abajo es techo; encima del suelo no se cuelga (null).
     */
    public static CameraMount fromFace(BlockFace face) {
        if (face == BlockFace.DOWN) {
            return CEILING;
        }
        return face != null && face.isCartesian() && face != BlockFace.UP ? WALL : null;
    }

    /** El yaw que mira hacia {@code face}, es decir, hacia fuera del muro. */
    public static float yawAwayFrom(BlockFace face) {
        return switch (face) {
            case WEST -> 90f;
            case NORTH -> 180f;
            case EAST -> 270f;
            default -> 0f;
        };
    }

    public float restPitch() {
        return restPitch;
    }

    /** La vista más parecida a {yaw, pitch} que la cámara puede dar, con yaw entre 0 y 360. */
    public float[] clamp(float baseYaw, float yaw, float pitch) {
        float offset = Math.max(-halfArc, Math.min(halfArc, wrap(yaw - baseYaw)));
        return new float[] {normalize(baseYaw + offset), Math.max(minPitch, Math.min(maxPitch, pitch))};
    }

    /** true si la cámara puede mirar ahí (lo de detrás del muro no lo ve). */
    public boolean covers(float baseYaw, float yaw, float pitch) {
        return Math.abs(wrap(yaw - baseYaw)) <= halfArc && pitch >= minPitch && pitch <= maxPitch;
    }

    /** El siguiente paso del barrido: da la vuelta al llegar a un borde. */
    public Sweep sweep(Sweep current, float step) {
        float next = current.offset() + current.direction() * step;
        if (Math.abs(next) >= halfArc) {
            return new Sweep(Math.copySign(halfArc, next), -current.direction());
        }
        return new Sweep(next, current.direction());
    }

    /** Diferencia de ángulos entre -180 y 180. */
    private static float wrap(float degrees) {
        return ((degrees % 360f) + 540f) % 360f - 180f;
    }

    private static float normalize(float degrees) {
        return ((degrees % 360f) + 360f) % 360f;
    }
}
