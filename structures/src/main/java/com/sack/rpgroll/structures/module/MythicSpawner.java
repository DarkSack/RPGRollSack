package com.sack.rpgroll.structures.module;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Genera mobs de MythicMobs. Adaptador por reflexión: sin MythicMobs en el classpath de compilación.
 *
 * <p>API leída en https://wiki.mythiccraft.io/mythicmobs/API (antes git.mythiccraft.io/mythiccraft/MythicMobs/-/wikis/API)
 * y https://www.mythiccraft.io/javadocs/mythic/, MythicMobs 5.12.1 (Mythic-Dist). Cadena usada:
 * {@code MythicBukkit.inst().getMobManager().getMythicMob(String)} (Optional de MythicMob),
 * {@code MythicMob.spawn(AbstractLocation, double level)} con {@code BukkitAdapter.adapt(Location)},
 * {@code ActiveMob.getEntity().getBukkitEntity()}. La wiki solo muestra {@code spawn(adapt(loc), 1)}; la firma
 * exacta no está en la javadoc, por eso se busca por nombre y tipos y, si no aparece, la integración se marca rota.
 * Confirmado por la documentación: MythicBukkit.inst(), getMobManager(), getMythicMob(String) (ejemplo de la wiki),
 * spawn(adapt(loc), 1), ActiveMob.getEntity() y AbstractEntity.getBukkitEntity() (javadoc). No confirmado: el tipo
 * MythicMob (la javadoc dio 404), que se deduce del tipo genérico de retorno de getMythicMob y, si no se puede,
 * se prueba {@code io.lumine.mythic.api.mobs.MythicMob}. Los métodos se resuelven una vez, sobre los tipos públicos documentados (no sobre {@code getClass()} de las
 * instancias, que pueden ser clases no públicas).
 */
public interface MythicSpawner {

    /** @return el mob generado, o null si ese nombre Mythic no existe (o la reflexión falló) */
    Entity spawn(String mythicName, Location at);

    /**
     * @param loader cargador de clases del plugin MythicMobs
     * @param broken recibe el motivo cuando la reflexión falla (el llamador apaga la integración y avisa una vez)
     */
    static MythicSpawner reflective(ClassLoader loader, Consumer<String> broken) {
        return new Reflective(loader, broken);
    }

    /** Métodos cacheados en la primera llamada; si resolverlos falla, queda rota y no lo reintenta. */
    final class Reflective implements MythicSpawner {
        private final ClassLoader loader;
        private final Consumer<String> broken;
        private Method inst, getMobManager, getMythicMob, adapt, spawn, getEntity, getBukkitEntity;
        private boolean resolved, failed;

        Reflective(ClassLoader loader, Consumer<String> broken) {
            this.loader = loader;
            this.broken = broken;
        }

        private Class<?> cls(String n) throws ClassNotFoundException {
            return Class.forName(n, true, loader);
        }

        /** Tipo de Optional&lt;MythicMob&gt; de getMythicMob; si no se deduce, io.lumine.mythic.api.mobs.MythicMob. */
        private Class<?> mythicMobType() throws ClassNotFoundException {
            if (getMythicMob.getGenericReturnType() instanceof java.lang.reflect.ParameterizedType pt
                    && pt.getActualTypeArguments().length == 1 && pt.getActualTypeArguments()[0] instanceof Class<?> c) {
                return c;
            }
            return cls("io.lumine.mythic.api.mobs.MythicMob");
        }

        private void resolve() throws ReflectiveOperationException {
            Class<?> bukkit = cls("io.lumine.mythic.bukkit.MythicBukkit");
            inst = bukkit.getMethod("inst");
            getMobManager = bukkit.getMethod("getMobManager");
            getMythicMob = getMobManager.getReturnType().getMethod("getMythicMob", String.class);
            adapt = cls("io.lumine.mythic.bukkit.BukkitAdapter").getMethod("adapt", Location.class);
            Class<?> mythicMob = mythicMobType();
            for (Method m : mythicMob.getMethods()) {
                if (m.getName().equals("spawn") && m.getParameterCount() == 2
                        && m.getParameterTypes()[0].isAssignableFrom(adapt.getReturnType())
                        && m.getParameterTypes()[1] == double.class) {
                    spawn = m;
                    break;
                }
            }
            if (spawn == null) {
                throw new NoSuchMethodException("MythicMob.spawn(AbstractLocation, double)");
            }
            getEntity = cls("io.lumine.mythic.core.mobs.ActiveMob").getMethod("getEntity");
            getBukkitEntity = cls("io.lumine.mythic.api.adapters.AbstractEntity").getMethod("getBukkitEntity");
        }

        @Override
        public Entity spawn(String name, Location at) {
            if (failed) {
                return null;
            }
            try {
                if (!resolved) {
                    resolve();
                    resolved = true;
                }
                Object mobs = getMobManager.invoke(inst.invoke(null));
                Optional<?> mob = (Optional<?>) getMythicMob.invoke(mobs, name);
                if (mob.isEmpty()) {
                    return null;
                }
                Object active = spawn.invoke(mob.get(), adapt.invoke(null, at), 1.0);
                return (Entity) getBukkitEntity.invoke(getEntity.invoke(active));
            } catch (InvocationTargetException e) {
                throw new IllegalStateException("MythicMobs falló al generar " + name, e.getCause());
            } catch (ReflectiveOperationException | LinkageError | ClassCastException e) {
                failed = true;
                broken.accept(e.toString());
                return null;
            }
        }
    }
}
