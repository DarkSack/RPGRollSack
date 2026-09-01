package com.sack.rpgroll.common.integration;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

/**
 * Puente opcional hacia RPGRoll-FX, compartido por todo el ecosistema.
 * <p>
 * Se resuelve por reflexión, igual que el resto de integraciones blandas de
 * los addons: ninguno compila contra RPGRoll-FX, así que un servidor
 * que no lo tenga instalado sigue funcionando sin tocar nada. Si no está, las
 * acciones {@code PARTICLES} simplemente no hacen nada.
 * <p>
 * El {@code caster} puede ser null: hay trampas que se disparan sin jugador
 * (una cadena, un temporizador), y el efecto igual tiene que verse.
 */
public final class ParticlesIntegration {

    private static final String API_CLASS = "com.sack.rpgroll.fx.api.RPGRollFXAPI";

    private static Boolean available;
    private static Method getMethod;
    private static Method playMethod;

    private ParticlesIntegration() {
    }

    /** @return true si el efecto se lanzó; false si el plugin no está o el id no existe. */
    public static boolean play(String effectId, Player caster, Location location) {

        if (!isAvailable()) {
            return false;
        }

        try {
            Object api = getMethod.invoke(null);
            return api != null && (boolean) playMethod.invoke(api, effectId, caster, location);

        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    private static boolean isAvailable() {

        if (available != null) {
            return available;
        }

        if (Bukkit.getPluginManager().getPlugin("RPGRoll-FX") == null) {
            available = false;
            return false;
        }

        try {
            Class<?> apiClass = Class.forName(API_CLASS);
            getMethod = apiClass.getMethod("get");
            playMethod = apiClass.getMethod("play", String.class, Player.class, Location.class);
            available = true;

        } catch (ReflectiveOperationException e) {
            available = false;
        }

        return available;
    }

}
