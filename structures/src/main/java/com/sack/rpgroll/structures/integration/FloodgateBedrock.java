package com.sack.rpgroll.structures.integration;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Bedrock por Floodgate, por reflexión (sin depender de Floodgate al compilar).
 * <p>
 * Doc oficial leída: https://geysermc.org/wiki/floodgate/api/ (Floodgate 2.0):
 * {@code org.geysermc.floodgate.api.FloodgateApi.getInstance().isFloodgatePlayer(UUID)}.
 */
public final class FloodgateBedrock implements BedrockCheck {

    private final java.util.function.Supplier<Integrations> integrations;
    private Object api;
    private Method isFloodgatePlayer;

    public FloodgateBedrock(java.util.function.Supplier<Integrations> integrations) {
        this.integrations = integrations;
    }

    @Override
    public boolean isBedrock(UUID player) {
        if (!integrations.get().active(Hook.FLOODGATE)) {
            return false;
        }
        try {
            if (api == null) {
                Plugin plugin = Bukkit.getPluginManager().getPlugin(Hook.FLOODGATE.plugin());
                if (plugin == null) {
                    return false;
                }
                Class<?> cls = Class.forName("org.geysermc.floodgate.api.FloodgateApi", true,
                        plugin.getClass().getClassLoader());
                isFloodgatePlayer = cls.getMethod("isFloodgatePlayer", UUID.class);
                api = cls.getMethod("getInstance").invoke(null);
            }
            return (boolean) isFloodgatePlayer.invoke(api, player);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            integrations.get().markBroken(Hook.FLOODGATE, e.toString());
            return false;
        }
    }
}
