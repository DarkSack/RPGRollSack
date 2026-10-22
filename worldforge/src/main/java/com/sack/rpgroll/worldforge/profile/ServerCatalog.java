package com.sack.rpgroll.worldforge.profile;

import com.sack.rpgroll.worldforge.api.Result;
import com.sack.rpgroll.worldforge.template.Templates;
import com.sack.rpgroll.worldforge.terrain.Terrains;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;

/**
 * Blocks and biomes of the running server. Biomes come from the registry, so datapack
 * biomes count. If the registry is not ready yet (very early in startup) the check reports
 * "unknown" and {@link #deferred()} says so; profiles are checked again once the server is up.
 */
public final class ServerCatalog implements Catalog {

    private volatile boolean deferred;
    private final Templates templates;
    private final Terrains terrains;

    public ServerCatalog(Templates templates, Terrains terrains) {
        this.templates = templates;
        this.terrains = terrains;
    }

    @Override
    public boolean isTerrain(String id) {
        return terrains.get(id).isPresent();
    }

    @Override
    public boolean isBlock(String key) {
        Material m = Material.matchMaterial(key);
        return m != null && m.isBlock();
    }

    @Override
    public boolean isBiome(String key) {
        NamespacedKey k = NamespacedKey.fromString(key);
        if (k == null) {
            return false;
        }
        try {
            return RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME).get(k) != null;
        } catch (RuntimeException notReady) {
            deferred = true;
            return true;
        }
    }

    @Override
    public String generatorProblem(String spec) {
        String name = spec.split(":", 2)[0];
        org.bukkit.plugin.Plugin p = org.bukkit.Bukkit.getPluginManager().getPlugin(name);
        if (p == null) {
            return "no hay ningún plugin «" + name + "» instalado.";
        }
        if (!p.isEnabled()) {
            deferred = true; // still starting (WorldForge loads at STARTUP): checked again once the server is up
        }
        return null;
    }

    @Override
    public Result<int[]> template(String name) {
        return templates.size(name);
    }

    /** True if some biome could not be checked yet; re-validate after the server starts. */
    public boolean deferred() {
        return deferred;
    }

    public void clearDeferred() {
        deferred = false;
    }
}
