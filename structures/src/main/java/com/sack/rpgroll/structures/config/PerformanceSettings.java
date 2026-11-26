package com.sack.rpgroll.structures.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.function.Consumer;

/**
 * Ajustes de {@code performance.yml}: presupuesto de construcción, límites de obras, vista previa y cada cuántos
 * segundos se vuelve a mirar la protección de una obra en marcha.
 */
public record PerformanceSettings(int blocksPerTick, int maxProjectsServer, int maxProjectsWorld,
                                  int maxProjectsPlayer, int previewMaxBlocks, int previewMaxDistance,
                                  int previewSeconds, int cancelRefundPercent, int protectionRecheckSeconds) {

    public static PerformanceSettings from(ConfigurationSection root, Consumer<String> warn) {
        return new PerformanceSettings(
                positive(root, "blocks-per-tick", 256, warn),
                positive(root, "max-projects.server", 10, warn),
                positive(root, "max-projects.world", 5, warn),
                positive(root, "max-projects.player", 2, warn),
                positive(root, "preview.max-blocks", 4096, warn),
                positive(root, "preview.max-distance", 64, warn),
                positive(root, "preview.seconds", 300, warn),
                refund(root, warn),
                positive(root, "protection-recheck-seconds", 60, warn));
    }

    private static int positive(ConfigurationSection s, String key, int def, Consumer<String> warn) {
        if (!s.isSet(key)) {
            return def;
        }
        int v = s.getInt(key, -1);
        if (v <= 0) {
            warn.accept(key + ": valor inválido (" + s.get(key) + "); uso " + def + ".");
            return def;
        }
        return v;
    }

    private static int refund(ConfigurationSection s, Consumer<String> warn) {
        if (!s.isSet("cancel-refund")) {
            return 100;
        }
        int v = s.getInt("cancel-refund", -1);
        if (v < 0 || v > 100) {
            warn.accept("cancel-refund: debe estar entre 0 y 100 (" + s.get("cancel-refund") + "); uso 100.");
            return 100;
        }
        return v;
    }
}
