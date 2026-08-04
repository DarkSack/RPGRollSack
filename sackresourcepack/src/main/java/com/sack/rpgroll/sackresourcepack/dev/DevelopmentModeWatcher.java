package com.sack.rpgroll.sackresourcepack.dev;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;

/**
 * Modo de desarrollo en caliente: un {@link WatchService} (JDK puro, sin
 * dependencias) observa {@code content/} recursivamente y, tras 1.5s sin
 * más cambios (para no reconstruir en medio de que alguien todavía está
 * copiando varios archivos), dispara {@code onChangeRebuild} en el hilo
 * principal de Bukkit.
 */
public class DevelopmentModeWatcher {

    private static final long DEBOUNCE_MILLIS = 1500L;

    private final Plugin plugin;
    private final Path root;
    private final Runnable onChangeRebuild;

    private WatchService watchService;
    private Thread watchThread;
    private BukkitTask debounceTask;

    private volatile boolean running;
    private volatile long lastEventAtMillis;
    private volatile boolean pending;

    public DevelopmentModeWatcher(Plugin plugin, Path root, Runnable onChangeRebuild) {
        this.plugin = plugin;
        this.root = root;
        this.onChangeRebuild = onChangeRebuild;
    }

    public boolean isRunning() {
        return running;
    }

    public void start() {

        if (running) {
            return;
        }

        try {
            Files.createDirectories(root);
            watchService = FileSystems.getDefault().newWatchService();
            registerAll(root);
        } catch (IOException e) {
            plugin.getLogger().severe("✘ No se pudo iniciar el modo de desarrollo: " + e.getMessage());
            return;
        }

        running = true;

        watchThread = new Thread(this::watchLoop, "SackResourcePack-DevWatcher");
        watchThread.setDaemon(true);
        watchThread.start();

        debounceTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkDebounce, 20L, 20L);

        plugin.getLogger().info("✔ Modo de desarrollo activo — observando " + root);
    }

    public void stop() {

        running = false;

        if (watchThread != null) {
            watchThread.interrupt();
        }

        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException ignored) {
            }
        }

        if (debounceTask != null) {
            debounceTask.cancel();
        }

        plugin.getLogger().info("✔ Modo de desarrollo desactivado.");
    }

    private void registerAll(Path start) throws IOException {

        try (var walk = Files.walk(start)) {
            for (Path dir : walk.filter(Files::isDirectory).toList()) {
                try {
                    dir.register(watchService, StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void watchLoop() {

        while (running) {

            WatchKey key;

            try {
                key = watchService.take();
            } catch (InterruptedException e) {
                break;
            } catch (java.nio.file.ClosedWatchServiceException e) {
                break;
            }

            for (WatchEvent<?> event : key.pollEvents()) {

                lastEventAtMillis = System.currentTimeMillis();
                pending = true;

                if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE && key.watchable() instanceof Path parent) {

                    Path changed = parent.resolve((Path) event.context());

                    if (Files.isDirectory(changed)) {
                        try {
                            registerAll(changed);
                        } catch (IOException ignored) {
                        }
                    }
                }
            }

            key.reset();
        }
    }

    private void checkDebounce() {

        if (pending && System.currentTimeMillis() - lastEventAtMillis >= DEBOUNCE_MILLIS) {
            pending = false;
            onChangeRebuild.run();
        }
    }

}
