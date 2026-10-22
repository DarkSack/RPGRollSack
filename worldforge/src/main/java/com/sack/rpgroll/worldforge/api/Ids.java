package com.sack.rpgroll.worldforge.api;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The shapes identifiers may take. Validated at every edge (command, GUI, YAML), so the
 * rest of the code can use them in file names and keys without escaping.
 */
public final class Ids {

    /** Profiles, protected regions, ore rules, templates. */
    private static final Pattern ID = Pattern.compile("[a-z0-9][a-z0-9_-]{0,31}");

    /**
     * World names. Paper 26.1 stores a world created by name under
     * {@code <main world>/dimensions/minecraft/<name>} with key {@code minecraft:<name>}
     * (FEASIBILITY P2), so the name must also be a valid key path.
     */
    private static final Pattern WORLD = Pattern.compile("[a-z0-9][a-z0-9_]{0,39}");

    /** Jobs and backups: what {@link #newJobId()} produces. */
    private static final Pattern JOB = Pattern.compile("[a-z]{1,4}-[0-9a-f]{8}");

    private static final SecureRandom RANDOM = new SecureRandom();

    private Ids() {
    }

    public static Optional<Problem> checkId(String field, String value) {
        if (value == null || !ID.matcher(value).matches()) {
            return Optional.of(Problem.error(field, "«" + value + "» no vale como identificador: minúsculas, números, '_' o '-', hasta 32, empezando por letra o número."));
        }
        return Optional.empty();
    }

    public static Optional<Problem> checkWorldName(String field, String value) {
        if (value == null || !WORLD.matcher(value).matches()) {
            return Optional.of(Problem.error(field, "«" + value + "» no vale como nombre de mundo: minúsculas, números o '_', hasta 40."));
        }
        return Optional.empty();
    }

    public static boolean isJobId(String value) {
        return value != null && JOB.matcher(value).matches();
    }

    public static String newJobId() {
        return "job-" + String.format("%08x", RANDOM.nextInt());
    }

    public static String newBackupId() {
        return "bak-" + String.format("%08x", RANDOM.nextInt());
    }

    /** Short code the admin must type back to confirm one specific plan. */
    public static String newToken() {
        return String.format("%06x", RANDOM.nextInt(0x1000000));
    }
}
