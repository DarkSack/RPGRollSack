package com.sack.rpgroll.sackresourcepack;

import com.sack.rpgroll.licensing.LicenseGate;
import com.sack.rpgroll.license.identity.LicenseIdentity;

import com.sack.rpgroll.sackresourcepack.api.AssetsAPI;
import com.sack.rpgroll.sackresourcepack.build.BuildEngine;
import com.sack.rpgroll.sackresourcepack.build.BuildResult;
import com.sack.rpgroll.sackresourcepack.cmd.CustomModelDataManager;
import com.sack.rpgroll.sackresourcepack.cmd.SrpCommand;
import com.sack.rpgroll.sackresourcepack.datapack.DatapackBuildEngine;
import com.sack.rpgroll.sackresourcepack.dev.DevelopmentModeWatcher;
import com.sack.rpgroll.sackresourcepack.distribution.DistributionEngine;
import com.sack.rpgroll.sackresourcepack.distribution.RemoteUploadService;
import com.sack.rpgroll.sackresourcepack.distribution.ResourcePackHttpServer;
import com.sack.rpgroll.sackresourcepack.distribution.s3.S3UploadService;
import com.sack.rpgroll.sackresourcepack.distribution.s3.S3UploadSettings;
import com.sack.rpgroll.sackresourcepack.gui.listener.GUIListener;
import com.sack.rpgroll.sackresourcepack.lang.LangManager;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;

/**
 * Punto de entrada de SackResourcePack — cablea todos los motores
 * (manifest/merge/validation/build ya orquestados dentro de {@link
 * BuildEngine}, más distribución, host HTTP, upload remoto y modo
 * desarrollo) y hace un primer build al arrancar. Standalone: no
 * depende de RPGRoll ni de ningún otro plugin.
 */
public class SackResourcePackPlugin extends JavaPlugin {

    private LangManager lang;
    private BuildEngine buildEngine;
    private CustomModelDataManager customModelDataManager;
    private DistributionEngine distributionEngine;
    private ResourcePackHttpServer httpServer;
    private RemoteUploadService remoteUploadService;
    private S3UploadService s3UploadService;
    private DatapackBuildEngine datapackBuildEngine;
    private DevelopmentModeWatcher developmentModeWatcher;

    private boolean remoteUploadEnabled;
    private String remoteUploadMode;
    private String remoteUploadUrl;
    private String remoteUploadMethod;
    private String remoteAuthHeaderName;
    private String remoteAuthHeaderValue;
    private S3UploadSettings s3UploadSettings;
    private String httpPublicUrl;
    private boolean httpEnabled;
    private boolean datapackEnabled;

    @Override
    public void onEnable() {
        if (!LicenseGate.verify(this, LicenseIdentity.RESOURCE_ID)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }


        saveDefaultConfig();

        this.lang = new LangManager(this, List.of("es", "en", "pt_BR"), "es");
        lang.reload(getConfig().getString("language", "es"));

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
                buildEngine::getZipFile,
                buildEngine::getMergedDirectory);

        if (httpEnabled) {
            httpServer.start();
        }

        this.remoteUploadService = new RemoteUploadService(this);
        this.s3UploadService = new S3UploadService(this);
        this.remoteUploadEnabled = getConfig().getBoolean("remote-upload.enabled", false);
        this.remoteUploadMode = getConfig().getString("remote-upload.mode", "generic");
        this.remoteUploadUrl = getConfig().getString("remote-upload.url", "");
        this.remoteUploadMethod = getConfig().getString("remote-upload.method", "PUT");
        this.remoteAuthHeaderName = getConfig().getString("remote-upload.auth-header-name", "Authorization");
        this.remoteAuthHeaderValue = getConfig().getString("remote-upload.auth-header-value", "");
        this.s3UploadSettings = new S3UploadSettings(
                getConfig().getString("s3.endpoint", ""),
                getConfig().getString("s3.bucket", ""),
                getConfig().getString("s3.region", "us-east-1"),
                getConfig().getString("s3.object-key", "resourcepack.zip"),
                getConfig().getString("s3.access-key", ""),
                getConfig().getString("s3.secret-key", ""),
                getConfig().getBoolean("s3.path-style", false));

        this.datapackEnabled = getConfig().getBoolean("datapack.enabled", false);
        this.datapackBuildEngine = new DatapackBuildEngine(this, contentDirectory,
                getConfig().getString("datapack.name", "sackresourcepack"),
                getConfig().getInt("datapack.pack-format", 48));

        this.developmentModeWatcher = new DevelopmentModeWatcher(this, contentDirectory.toPath(),
                () -> getServer().getScheduler().runTask(this, this::rebuildAndDistribute));

        if (getConfig().getBoolean("development-mode", false)) {
            developmentModeWatcher.start();
        }

        getServer().getPluginManager().registerEvents(new GUIListener(this), this);

        AssetsAPI.init(this, contentDirectory);

        var srpCommand = new SrpCommand(this);

        // Registrado por Brigadier para que `execute as` entregue al jugador real.
        com.sack.rpgroll.sackresourcepack.command.BrigadierCommands.register(this, "srp",
                "Comandos de SackResourcePack", "sackresourcepack.admin.*", srpCommand);

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

        if (remoteUploadEnabled && "s3".equalsIgnoreCase(remoteUploadMode) && s3UploadSettings.isConfigured()) {
            getServer().getScheduler().runTaskAsynchronously(this,
                    () -> s3UploadService.upload(result.zipFile(), s3UploadSettings));
        } else if (remoteUploadEnabled && !remoteUploadUrl.isBlank()) {
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

    public DatapackBuildEngine getDatapackBuildEngine() {
        return datapackBuildEngine;
    }

    public boolean isDatapackEnabled() {
        return datapackEnabled;
    }

    public LangManager lang() {
        return lang;
    }

}
