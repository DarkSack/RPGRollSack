package com.sack.rpgroll.tab.format;

/**
 * Un tramo de formato de ping. {@code maxMillis} es el límite superior
 * (exclusivo) de este tramo — {@code null} significa "sin límite" (el
 * tramo "catch-all", normalmente el peor/último).
 */
public record PingTier(String format, Integer maxMillis) {

    public boolean matches(int ping) {
        return maxMillis == null || ping < maxMillis;
    }

}
