package com.sack.rpgroll.pass;

import com.sack.rpgroll.common.command.BrigadierCommands;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.common.resource.DirectoryCreator;
import com.sack.rpgroll.common.resource.ResourceCopier;
import com.sack.rpgroll.common.resource.ResourceFile;
import com.sack.rpgroll.license.identity.LicenseIdentity;
import com.sack.rpgroll.licensing.LicenseGate;
import com.sack.rpgroll.pass.command.PassAdminCommand;
import com.sack.rpgroll.pass.command.PlayerCommands;
import com.sack.rpgroll.pass.daily.DailyConfig;
import com.sack.rpgroll.pass.daily.DailyService;
import com.sack.rpgroll.pass.listener.ConnectionListener;
import com.sack.rpgroll.pass.listener.MobsHook;
import com.sack.rpgroll.pass.listener.PlaytimeTask;
import com.sack.rpgroll.pass.listener.ProgressListener;
import com.sack.rpgroll.pass.listener.QuestsHook;
import com.sack.rpgroll.pass.mission.MissionLoader;
import com.sack.rpgroll.pass.mission.MissionService;
import com.sack.rpgroll.pass.player.PassPlayerStore;
import com.sack.rpgroll.pass.reward.RewardService;
import com.sack.rpgroll.pass.season.PassService;
import com.sack.rpgroll.pass.season.SeasonLoader;
import com.sack.rpgroll.pass.vote.VoteConfig;
import com.sack.rpgroll.pass.vote.VoteService;
import com.sack.rpgroll.pass.vote.VotifierBridge;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.ZoneId;
import java.util.List;

/**
 * RPGRoll-Pass: pase de temporada con pista gratis y premium, misiones
 * diarias/semanales/de temporada, recompensa diaria con racha y extra por
 * rango, y recompensas por votar (Votifier).
 */
public class PassPlugin extends JavaPlugin {

    private static final long AUTOSAVE_TICKS = 20L * 60 * 5;

    private LangManager lang;
    private PassPlayerStore store;
    private PassModule module;
    private PassClock clock;

    @Override
    public void onEnable() {

        if (!LicenseGate.verify(this, LicenseIdentity.RESOURCE_ID, LicenseIdentity.PRODUCT_SLUG,
                LicenseIdentity.VERIFY_TOKEN)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        new DirectoryCreator(this).create(List.of("seasons"));
        new ResourceCopier(this).copyDirectories(List.of("seasons"));
        new ResourceCopier(this).copyFiles(List.of(
                new ResourceFile("missions.yml", "missions.yml", false),
                new ResourceFile("daily.yml", "daily.yml", false),
                new ResourceFile("votes.yml", "votes.yml", false)));

        lang = new LangManager(this, List.of("es", "en", "pt_BR"), "es");
        lang.reload(getConfig().getString("language", "es"));

        clock = new PassClock(zone());
        store = new PassPlayerStore(getDataFolder(), getLogger());
        RewardService rewards = new RewardService(this, lang);
        PassService pass = new PassService(store, rewards, lang, clock);
        MissionService missions = new MissionService(pass, lang, clock);
        pass.onSeasonStart(missions::resetSeasonMissions);
        DailyService daily = new DailyService(pass, missions, rewards, lang, clock);
        VoteService votes = new VoteService(pass, missions, rewards, lang, clock, getDataFolder(), getLogger());
        module = new PassModule(lang, pass, missions, daily, votes, rewards);

        loadContent();

        var plugins = getServer().getPluginManager();
        plugins.registerEvents(new ProgressListener(missions), this);
        plugins.registerEvents(new ConnectionListener(this, store, pass, daily, votes, lang), this);

        if (plugins.isPluginEnabled("RPGRoll-Quests")) {
            plugins.registerEvents(new QuestsHook(missions), this);
        }
        if (plugins.isPluginEnabled("RPGRoll-Mobs")) {
            plugins.registerEvents(new MobsHook(missions), this);
        }

        boolean votifier = VotifierBridge.register(this, votes::onVote);

        Duration afk = Duration.ofSeconds(Math.max(1, getConfig().getInt("playtime-afk-seconds", 300)));
        Bukkit.getScheduler().runTaskTimer(this, new PlaytimeTask(missions, afk),
                PlaytimeTask.PERIOD_TICKS, PlaytimeTask.PERIOD_TICKS);
        Bukkit.getScheduler().runTaskTimer(this, store::saveDirty, AUTOSAVE_TICKS, AUTOSAVE_TICKS);

        registerCommands();

        getLogger().info("✔ RPGRoll-Pass habilitado. Temporada: "
                + pass.season().map(s -> s.id() + (pass.openSeason().isPresent() ? " (abierta)" : " (cerrada)"))
                        .orElse("ninguna")
                + ", " + missions.count() + " misión(es), votos " + (votifier ? "por Votifier" : "sin Votifier") + ".");
    }

    @Override
    public void onDisable() {
        if (store != null) {
            store.saveAll();
        }
    }

    private void loadContent() {

        var warn = (java.util.function.Consumer<String>) message -> getLogger().warning(message);
        String seasonId = getConfig().getString("active-season", "");

        module.pass().setSeason(seasonId.isBlank() ? null
                : SeasonLoader.load(new File(getDataFolder(), "seasons"), seasonId, warn).orElse(null));

        YamlConfiguration missionsYaml = yaml("missions.yml");
        module.missions().load(MissionLoader.parse(missionsYaml.getConfigurationSection("missions"), warn),
                missionsYaml.getInt("daily-count", 3), missionsYaml.getInt("weekly-count", 3));

        module.daily().setConfig(DailyConfig.parse(yaml("daily.yml"), warn));
        module.votes().setConfig(VoteConfig.parse(yaml("votes.yml"), warn));
    }

    private void reload() {
        reloadConfig();
        lang.reload(getConfig().getString("language", "es"));
        loadContent();
    }

    private void registerCommands() {

        BrigadierCommands.register(this, "pase", "Pase de temporada", List.of("pass", "battlepass"),
                new PlayerCommands.Pass(module), new PlayerCommands.Pass(module), null);
        BrigadierCommands.register(this, "diario", "Recompensa diaria", List.of("daily", "recompensas"),
                new PlayerCommands.Daily(module), null, null);
        BrigadierCommands.register(this, "votar", "Votar por el servidor", List.of("vote", "votes"),
                new PlayerCommands.Vote(module), null, null);

        PassAdminCommand admin = new PassAdminCommand(module, this::reload);
        BrigadierCommands.register(this, "passadmin", "Administra RPGRoll-Pass", List.of(), admin, admin,
                "rpgroll.pass.admin");
    }

    private ZoneId zone() {
        try {
            return ZoneId.of(getConfig().getString("timezone", "America/Mexico_City"));
        } catch (DateTimeException e) {
            getLogger().warning("timezone inválida en config.yml; se usa la del servidor.");
            return ZoneId.systemDefault();
        }
    }

    private YamlConfiguration yaml(String name) {
        return YamlConfiguration.loadConfiguration(new File(getDataFolder(), name));
    }

}
