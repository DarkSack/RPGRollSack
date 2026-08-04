package com.sack.rpgroll.sackresourcepack;

import com.sack.rpgroll.sackresourcepack.api.AssetsAPI;
import com.sack.rpgroll.sackresourcepack.build.BuildEngine;
import com.sack.rpgroll.sackresourcepack.build.BuildResult;
import com.sack.rpgroll.sackresourcepack.cmd.CustomModelDataManager;
import com.sack.rpgroll.sackresourcepack.cmd.SrpCommand;
import com.sack.rpgroll.sackresourcepack.dev.DevelopmentModeWatcher;
import com.sack.rpgroll.sackresourcepack.distribution.DistributionEngine;
import com.sack.rpgroll.sackresourcepack.distribution.RemoteUploadService;
import com.sack.rpgroll.sackresourcepack.distribution.ResourcePackHttpServer;
import com.sack.rpgroll.sackresourcepack.gui.listener.GUIListener;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * Punto de entrada de SackResourcePack — cablea todos los motores
 * (manifest/merge/validation/build ya orquestados dentro de {@link
 * BuildEngine}, más distribución, host HTTP, upload remoto y modo
 * desarrollo) y hace un primer build al arrancar. Standalone: no
 * depende de RPGRoll ni de ningún otro plugin.
 */
public class SackResourcePackPlugin extends JavaPlugin {

    private BuildEngine buildEngine;
    private CustomModelDataManager customModelDataManager;
    private DistributionEngine distributionEngine;
    private ResourcePackHttpServer httpServer;
    private RemoteUploadService remoteUploadService;
    private DevelopmentModeWatcher developmentModeWatcher;

    private boolean remoteUploadEnabled;
    private String remoteUploadUrl;
    private String remoteUploadMethod;
    private String remoteAuthHeaderName;
    private String remoteAuthHeaderValue;
    private String httpPublicUrl;
    private boolean httpEnabled;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        File contentDirectory = new File(getDataFolder(), getConfig().getString("content-directory", "content"));
        contentDirectory.mkdirs();

        int packFormat = getConfig().getInt("pack-format", 34);
        int cmdBase = getConfig().getInt("custom-model-data-base", 10000);

        this.buildEngine = new BuildEngine(this, contentDirectory, packFormat);
        this.customModelDataManager = new CustomModelDataManager(this, cmdBase);

        boolean distributionRequired = getConfig().getBoolean("distribution.required", false);
        String promptMessage = getConfig().getString("distribution.prompt-message", "");
        this.distributionEngine = new DistributionEngine(this, distributionRequired, promptMessage);
        getServer().getPluginManager().registerEvents(distributionEngine, this);

        this.httpEnabled = getConfig().getBoolean("http.enabled", true);
        this.httpPublicUrl = getConfig().getString("http.public-url", "");

        this.httpServer = new ResourcePackHttpServer(this,
                getConfig().getString("http.bind-address", "0.0.0.0"),
                getConfig().getInt("http.port", 8080),
                getConfig().getString("http.path", "/resourcepack.zip"),
                buildEngine::getZipFile);

        if (httpEnabled) {
            httpServer.start();
        }

        this.remoteUploadService = new RemoteUploadService(this);
        this.remoteUploadEnabled = getConfig().getBoolean("remote-upload.enabled", false);
        this.remoteUploadUrl = getConfig().getString("remote-upload.url", "");
        this.remoteUploadMethod = getConfig().getString("remote-upload.method", "PUT");
        this.remoteAuthHeaderName = getConfig().getString("remote-upload.auth-header-name", "Authorization");
        this.remoteAuthHeaderValue = getConfig().getString("remote-upload.auth-header-value", "");

        this.developmentModeWatcher = new DevelopmentModeWatcher(this, contentDirectory.toPath(),
                () -> getServer().getScheduler().runTask(this, this::rebuildAndDistribute));

        if (getConfig().getBoolean("development-mode", false)) {
            developmentModeWatcher.start();
        }

        getServer().getPluginManager().registerEvents(new GUIListener(this), this);

        AssetsAPI.init(this, contentDirectory);

        getCommand("srp").setExecutor(new SrpCommand(this));

        rebuildAndDistribute();

        getLogger().info("✔ SackResourcePack habilitado.");
    }

    @Override
    public void onDisable() {

        if (developmentModeWatcher != null) {
            developmentModeWatcher.stop();
        }

        if (httpServer != null) {
            httpServer.stop();
        }
    }

    /** Reconstruye (sin forzar) y, si hay jugadores conectados, les reenvía el pack actualizado. */
    public void rebuildAndDistribute() {

        BuildResult result = buildEngine.build(false);

        if (result.hasErrors()) {
            getLogger().warning("✘ El build de SackResourcePack tiene errores — revisá /srp validate.");
            return;
        }

        String publicUrl = resolvePublicUrl();

        if (publicUrl == null) {
            return;
        }

        distributionEngine.updateCurrentPack(publicUrl, result.sha1());

        if (!result.fromCache()) {
            distributionEngine.sendToAllOnline();
        }
    }

    /** Sube (si está configurado) y reenvía el pack a todos los jugadores conectados. */
    public void publish() {

        BuildResult result = buildEngine.build(false);

        if (remoteUploadEnabled && !remoteUploadUrl.isBlank()) {
            getServer().getScheduler().runTaskAsynchronously(this, () -> remoteUploadService.upload(
                    result.zipFile(), remoteUploadUrl, remoteUploadMethod, remoteAuthHeaderName, remoteAuthHeaderValue));
        }

        String publicUrl = resolvePublicUrl();

        if (publicUrl == null) {
            getLogger().warning("✘ No hay URL pública configurada (ni http.public-url, ni http.enabled) — no se puede distribuir.");
            return;
        }

        distributionEngine.updateCurrentPack(publicUrl, result.sha1());
        distributionEngine.sendToAllOnline();
    }

    private String resolvePublicUrl() {

        if (httpPublicUrl != null && !httpPublicUrl.isBlank()) {
            return httpPublicUrl;
        }

        if (httpEnabled && httpServer.isRunning()) {
            return httpServer.localUrl();
        }

        return null;
    }

    public void toggleDevelopmentMode() {

        if (developmentModeWatcher.isRunning()) {
            developmentModeWatcher.stop();
        } else {
            developmentModeWatcher.start();
        }
    }

    public boolean isDevelopmentModeActive() {
        return developmentModeWatcher.isRunning();
    }

    public BuildEngine getBuildEngine() {
        return buildEngine;
    }

    public CustomModelDataManager getCustomModelDataManager() {
        return customModelDataManager;
    }

    public DistributionEngine getDistributionEngine() {
        return distributionEngine;
    }

    public ResourcePackHttpServer getHttpServer() {
        return httpServer;
    }

    public DevelopmentModeWatcher getDevelopmentModeWatcher() {
        return developmentModeWatcher;
    }

}
