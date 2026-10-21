package com.sack.rpgroll.disasters;

import com.sack.rpgroll.common.command.BrigadierCommands;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.common.resource.DirectoryCreator;
import com.sack.rpgroll.common.resource.ResourceCopier;
import com.sack.rpgroll.disasters.command.DisastersAdminCommand;
import com.sack.rpgroll.disasters.core.DisasterManager;
import com.sack.rpgroll.disasters.engine.DisasterEngine;
import com.sack.rpgroll.license.identity.LicenseIdentity;
import com.sack.rpgroll.licensing.LicenseGate;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;

public class DisastersPlugin extends JavaPlugin {

    private static final List<String> DIRECTORIES = List.of("disasters");
    private static final long DAILY_ROLL_TICK_INTERVAL = 1200L;

    private DisasterManager disasterManager;
    private DisasterEngine disasterEngine;
    private LangManager langManager;

    @Override
    public void onEnable() {
        if (!LicenseGate.verify(this, LicenseIdentity.RESOURCE_ID, LicenseIdentity.PRODUCT_SLUG,
                LicenseIdentity.VERIFY_TOKEN)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        boolean firstRun = !new File(getDataFolder(), "config.yml").exists();
        saveDefaultConfig();
        if (firstRun) {
            migrateFromSeasons();
        }

        langManager = new LangManager(this, List.of("es", "en", "pt_BR"), "es");
        langManager.reload(getConfig().getString("language", "es"));

        new DirectoryCreator(this).create(DIRECTORIES);
        new ResourceCopier(this).copyDirectories(DIRECTORIES);

        disasterManager = new DisasterManager(this);
        disasterManager.initialize();
        disasterEngine = new DisasterEngine(this, langManager, disasterManager);
        disasterEngine.configure(getConfig());
        disasterEngine.startRegeneration();
        getServer().getPluginManager().registerEvents(disasterEngine, this);

        DailyRollTask dailyRoll = new DailyRollTask(this, disasterManager, disasterEngine);
        getServer().getScheduler().runTaskTimer(this, dailyRoll, DAILY_ROLL_TICK_INTERVAL, DAILY_ROLL_TICK_INTERVAL);

        // Registrado por Brigadier para que `execute as` entregue al jugador real.
        BrigadierCommands.register(this, "disastersadmin", "Comandos administrativos de RPGRoll-Disasters",
                "rpgrolldisasters.admin", new DisastersAdminCommand(this, langManager, disasterManager, disasterEngine));

        getLogger().info("✔ RPGRoll-Disasters habilitado. " + disasterManager.count() + " desastre(s).");
    }

    @Override
    public void onDisable() {
        if (disasterEngine != null) {
            disasterEngine.stopAll();
        }
    }

    /** Recarga config.yml, el idioma y los desastres. */
    public void reloadAll() {
        reloadConfig();
        langManager.reload(getConfig().getString("language", "es"));
        disasterManager.reload();
        disasterEngine.configure(getConfig());
    }

    /**
     * Hasta la 1.1.2 los desastres venían en RPGRoll-Seasons. La primera vez se traen de allí los
     * mundos desactivados y los desastres propios (los que no vienen con el plugin); los de serie
     * salen nuevos de este jar.
     */
    private void migrateFromSeasons() {

        File seasons = new File(getDataFolder().getParentFile(), "RPGRoll-Seasons");
        File oldDisasters = new File(seasons, "disasters");
        if (!oldDisasters.isDirectory()) {
            return;
        }

        List<String> disabled = YamlConfiguration.loadConfiguration(new File(seasons, "config.yml"))
                .getStringList("disabled-worlds");
        if (!disabled.isEmpty()) {
            getConfig().set("disabled-worlds", disabled);
            saveConfig();
        }

        File[] files = oldDisasters.listFiles((dir, name) -> name.endsWith(".yml") && !name.startsWith("_"));
        int copied = 0;
        for (File file : files == null ? new File[0] : files) {
            if (getResource("disasters/" + file.getName()) != null) {
                continue;
            }
            try {
                File target = new File(getDataFolder(), "disasters/" + file.getName());
                Files.createDirectories(target.getParentFile().toPath());
                Files.copy(file.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                copied++;
            } catch (IOException e) {
                getLogger().warning("✘ No se pudo traer " + file.getName() + " de RPGRoll-Seasons: " + e.getMessage());
            }
        }
        getLogger().info("✔ Traído de RPGRoll-Seasons: disabled-worlds " + disabled + " y " + copied
                + " desastre(s) propio(s). Los de " + oldDisasters.getPath() + " ya no se usan.");
    }

}
