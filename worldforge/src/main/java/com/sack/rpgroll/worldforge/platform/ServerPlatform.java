package com.sack.rpgroll.worldforge.platform;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Where each kind of work runs. Paper also has the region, global, async and entity
 * schedulers (on Paper they all end up on the main thread except async), so one code path
 * serves both platforms (docs/ARCHITECTURE.md D7). Nothing else in WorldForge touches a
 * scheduler.
 */
public final class ServerPlatform {

    private final Plugin plugin;
    private final boolean folia;

    public ServerPlatform(Plugin plugin) {
        this.plugin = plugin;
        this.folia = detectFolia();
    }

    public static boolean detectFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public boolean folia() {
        return folia;
    }

    public String name() {
        return folia ? "Folia" : "Paper";
    }

    /** World-wide work (create, load, unload worlds; global state). */
    public <T> CompletableFuture<T> global(Supplier<T> work) {
        CompletableFuture<T> f = new CompletableFuture<>();
        Bukkit.getGlobalRegionScheduler().execute(plugin, () -> complete(f, work));
        return f;
    }

    /** Work on the blocks of one chunk, on the thread that owns it. */
    public <T> CompletableFuture<T> at(World world, int chunkX, int chunkZ, Supplier<T> work) {
        CompletableFuture<T> f = new CompletableFuture<>();
        Bukkit.getRegionScheduler().execute(plugin, world, chunkX, chunkZ, () -> complete(f, work));
        return f;
    }

    /** Disk, hashing, anything that must not touch the world. */
    public <T> CompletableFuture<T> async(Supplier<T> work) {
        CompletableFuture<T> f = new CompletableFuture<>();
        Bukkit.getAsyncScheduler().runNow(plugin, task -> complete(f, work));
        return f;
    }

    /** Work for one entity (open a menu, teleport), on the thread that owns it. */
    public void entity(Entity entity, Runnable work) {
        entity.getScheduler().run(plugin, task -> work.run(), null);
    }

    public void cancelAll() {
        Bukkit.getAsyncScheduler().cancelTasks(plugin);
        Bukkit.getGlobalRegionScheduler().cancelTasks(plugin);
    }

    private static <T> void complete(CompletableFuture<T> f, Supplier<T> work) {
        try {
            f.complete(work.get());
        } catch (Throwable t) {
            f.completeExceptionally(t);
        }
    }
}
