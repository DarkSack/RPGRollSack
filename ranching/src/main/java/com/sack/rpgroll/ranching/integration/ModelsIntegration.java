package com.sack.rpgroll.ranching.integration;

import com.magmaguy.freeminecraftmodels.api.ModeledEntityManager;
import com.magmaguy.freeminecraftmodels.customentity.DynamicEntity;

import com.sack.rpgroll.ranching.core.animal.AnimalKeys;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.logging.Logger;

/**
 * El modelo animado de la raza (campo {@code model} del YAML), con FreeMinecraftModels: el animal
 * vanilla sigue siendo el que come, anda y se ordeña, invisible, y el modelo lo sigue y se anima
 * solo (idle/walk). Integración blanda: sin FMM el animal se ve vanilla (o con su reskin). Las
 * clases de FMM solo se tocan dentro de {@link Fmm}.
 */
public final class ModelsIntegration {

    /** Los modelos de fábrica que trae el jar (resources/models/&lt;id&gt;.bbmodel). */
    public static final List<String> BUNDLED = List.of(
            "ranching_holstein", "ranching_jersey", "ranching_angus",
            "ranching_merino", "ranching_suffolk", "ranching_jacob",
            "ranching_leghorn", "ranching_rhode_island", "ranching_silkie",
            "ranching_yorkshire", "ranching_berkshire", "ranching_mangalica",
            "ranching_californian", "ranching_rex", "ranching_angora",
            "ranching_pekin", "ranching_mallard", "ranching_khaki_campbell",
            "ranching_saanen", "ranching_boer", "ranching_nubian",
            "ranching_trex");

    private ModelsIntegration() {
    }

    public static boolean isAvailable() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("FreeMinecraftModels");
        return plugin != null && plugin.isEnabled();
    }

    /**
     * Copia los modelos de fábrica a {@code plugins/FreeMinecraftModels/models/} si FMM está instalado:
     * los que faltan y los que siguen como se instalaron (así llegan las mejoras de versión). Se llama
     * en onLoad, antes de que FMM lea su carpeta; ahí FMM aún puede no estar cargado, así que se mira
     * si su carpeta o su jar están en plugins/.
     */
    public static void installBundled(Plugin plugin) {

        File plugins = plugin.getDataFolder().getParentFile();
        File fmm = new File(plugins, "FreeMinecraftModels");
        File[] jars = plugins.listFiles((dir, name) -> name.toLowerCase(java.util.Locale.ROOT)
                .startsWith("freeminecraftmodels") && name.endsWith(".jar"));

        if (!fmm.isDirectory() && (jars == null || jars.length == 0)) {
            return;
        }

        File folder = new File(fmm, "models");
        Logger log = plugin.getLogger();
        int copied = 0;

        // Lo que se instaló la última vez (id -> hash): un modelo que sigue igual se actualiza con el
        // del jar nuevo; uno que el dueño ha editado (hash distinto) se respeta.
        File record = new File(plugin.getDataFolder(), ".modelos-instalados.yml");
        org.bukkit.configuration.file.YamlConfiguration installed =
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(record);

        for (String id : BUNDLED) {

            File target = new File(folder, id + ".bbmodel");

            try (InputStream in = plugin.getResource("models/" + id + ".bbmodel")) {

                if (in == null) {
                    continue;
                }

                byte[] bundled = in.readAllBytes();
                String bundledHash = sha1(bundled);

                if (target.exists()) {
                    String current = sha1(Files.readAllBytes(target.toPath()));
                    if (current.equals(bundledHash) || !current.equals(installed.getString(id))) {
                        continue;
                    }
                }

                folder.mkdirs();
                Files.write(target.toPath(), bundled);
                installed.set(id, bundledHash);
                copied++;
            } catch (IOException e) {
                log.warning("✘ No se pudo copiar el modelo " + id + " a FreeMinecraftModels: " + e.getMessage());
            }
        }

        if (copied > 0) {
            try {
                plugin.getDataFolder().mkdirs();
                installed.save(record);
            } catch (IOException e) {
                log.warning("✘ No se pudo guardar " + record.getName() + ": " + e.getMessage());
            }
        }

        if (copied > 0) {
            log.info("✔ " + copied + " modelo(s) de raza copiados a FreeMinecraftModels/models.");
        }
    }

    private static String sha1(byte[] data) {
        try {
            return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-1").digest(data));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Pone el modelo si no lo lleva ya. @return true si queda puesto */
    public static boolean ensure(LivingEntity entity, String modelId) {

        if (modelId == null || !isAvailable() || entity.isDead()) {
            return false;
        }

        boolean attached;
        try {
            attached = Fmm.ensure(entity, modelId);
        } catch (RuntimeException | LinkageError e) {
            // FMM tarda en tener listo su adaptador tras arrancar: el ciclo de crecimiento lo reintenta.
            try {
                Fmm.remove(entity);
            } catch (RuntimeException | LinkageError ignored) {
            }
            entity.setInvisible(false);
            return false;
        }

        if (attached) {
            entity.getPersistentDataContainer().set(AnimalKeys.MODEL, PersistentDataType.STRING, modelId);
        }

        return attached;
    }

    /**
     * Quita el modelo (si lo llevaba) y le devuelve la visibilidad: por la marca que se deja al
     * ponerlo, así funciona también tras desinstalar FMM, cuando el animal quedaría invisible.
     */
    public static void remove(LivingEntity entity) {

        if (!entity.getPersistentDataContainer().has(AnimalKeys.MODEL, PersistentDataType.STRING)) {
            return;
        }

        if (isAvailable()) {
            try {
                Fmm.remove(entity);
            } catch (RuntimeException | LinkageError ignored) {
            }
        }

        entity.getPersistentDataContainer().remove(AnimalKeys.MODEL);
        entity.setInvisible(false);
    }

    /** Al morir: animación de muerte si la tiene. */
    public static void removeWithDeath(LivingEntity entity) {
        if (isAvailable()) {
            try {
                Fmm.removeWithDeath(entity);
            } catch (RuntimeException | LinkageError ignored) {
            }
        }
    }

    private static final class Fmm {

        static boolean ensure(LivingEntity entity, String modelId) {
            DynamicEntity current = DynamicEntity.getDynamicEntity(entity);
            if (current != null && !current.isRemoved()) {
                return true;
            }
            if (!ModeledEntityManager.modelExists(modelId)) {
                return false;
            }
            return DynamicEntity.createWithInvisibility(modelId, entity) != null;
        }

        static void remove(LivingEntity entity) {
            DynamicEntity model = DynamicEntity.getDynamicEntity(entity);
            if (model != null && !model.isRemoved()) {
                model.remove();
            }
        }

        static void removeWithDeath(LivingEntity entity) {
            DynamicEntity model = DynamicEntity.getDynamicEntity(entity);
            if (model != null && !model.isRemoved() && !model.isDying()) {
                model.removeWithDeathAnimation();
            }
        }
    }

}
