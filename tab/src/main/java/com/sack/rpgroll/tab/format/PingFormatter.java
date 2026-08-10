package com.sack.rpgroll.tab.format;

import java.util.List;

/** Resuelve el formato de texto correspondiente a un ping en ms, según una lista ordenada de {@link PingTier}. */
public final class PingFormatter {

    private static final List<PingTier> DEFAULT_TIERS = List.of(
            new PingTier("&a{ping}ms", 50),
            new PingTier("&e{ping}ms", 100),
            new PingTier("&c{ping}ms", null));

    private PingFormatter() {
    }

    public static String format(int ping, List<PingTier> tiers) {

        List<PingTier> effective = (tiers == null || tiers.isEmpty()) ? DEFAULT_TIERS : tiers;

        for (PingTier tier : effective) {
            if (tier.matches(ping)) {
                return tier.format().replace("{ping}", String.valueOf(ping));
            }
        }

        return String.valueOf(ping);
    }

    public static List<PingTier> defaults() {
        return DEFAULT_TIERS;
    }

}
