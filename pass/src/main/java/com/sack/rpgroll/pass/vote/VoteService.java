package com.sack.rpgroll.pass.vote;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.pass.PassClock;
import com.sack.rpgroll.pass.mission.MissionService;
import com.sack.rpgroll.pass.mission.MissionType;
import com.sack.rpgroll.pass.player.PassPlayer;
import com.sack.rpgroll.pass.reward.Reward;
import com.sack.rpgroll.pass.reward.RewardService;
import com.sack.rpgroll.pass.season.PassService;
import com.sack.rpgroll.util.ComponentUtils;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entrega los votos que avisa Votifier. Si quien votó no está conectado, el
 * voto queda en {@code data/pending-votes.yml} y se le entrega al entrar.
 */
public class VoteService {

    private final PassService pass;
    private final MissionService missions;
    private final RewardService rewards;
    private final LangManager lang;
    private final PassClock clock;
    private final File pendingFile;
    private final Logger logger;
    private final Map<String, Integer> pending = new ConcurrentHashMap<>();

    private VoteConfig config = new VoteConfig(List.of(), List.of(), 0, Map.of(), "");

    public VoteService(PassService pass, MissionService missions, RewardService rewards, LangManager lang,
            PassClock clock, File dataFolder, Logger logger) {
        this.pass = pass;
        this.missions = missions;
        this.rewards = rewards;
        this.lang = lang;
        this.clock = clock;
        this.pendingFile = new File(dataFolder, "data/pending-votes.yml");
        this.logger = logger;
        loadPending();
    }

    public void setConfig(VoteConfig config) {
        this.config = config;
    }

    public VoteConfig config() {
        return config;
    }

    /** Un voto recibido por Votifier (o simulado por admin). Siempre en el hilo principal. */
    public void onVote(String username, String service) {

        Player player = Bukkit.getPlayerExact(username);

        if (player == null) {
            pending.merge(username.toLowerCase(Locale.ROOT), 1, Integer::sum);
            savePending();
            logger.info("Voto de " + username + " (" + service + ") guardado hasta que entre.");
            return;
        }

        deliver(player, service);
    }

    /** Al entrar: los votos que llegaron mientras no estaba. */
    public void deliverPending(Player player) {

        Integer count = pending.remove(player.getName().toLowerCase(Locale.ROOT));

        if (count == null) {
            return;
        }

        savePending();
        for (int i = 0; i < count; i++) {
            deliver(player, "offline");
        }
    }

    private void deliver(Player player, String service) {

        PassPlayer state = pass.player(player);
        long today = clock.epochDay();
        boolean firstToday = state.lastVoteDay() != today;
        int streak = !firstToday ? state.voteStreak()
                : state.lastVoteDay() == today - 1 ? state.voteStreak() + 1 : 1;

        state.recordVote(today, streak);
        rewards.grant(player, config.rewards());
        lang.send(player, "votes.thanks", "service", service, "streak", streak);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.6f);

        // El premio de racha se da una vez: el primer voto del día en que se alcanza.
        List<Reward> streakRewards = config.streaks().get(streak);
        if (firstToday && streakRewards != null) {
            rewards.grant(player, streakRewards);
            lang.send(player, "votes.streak_reward", "days", streak);
        }

        if (!config.broadcast().isBlank()) {
            Bukkit.broadcast(ComponentUtils.parse(config.broadcast().replace("{player}", player.getName())));
        }

        pass.addXp(player, config.passXp());
        missions.progress(player, MissionType.VOTE, "", 1);
    }

    private void loadPending() {

        if (!pendingFile.isFile()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(pendingFile);
        for (String name : yaml.getKeys(false)) {
            pending.put(name, yaml.getInt(name));
        }
    }

    private void savePending() {

        YamlConfiguration yaml = new YamlConfiguration();
        pending.forEach(yaml::set);

        try {
            pendingFile.getParentFile().mkdirs();
            yaml.save(pendingFile);
        } catch (IOException e) {
            logger.log(Level.WARNING, "No se pudieron guardar los votos pendientes", e);
        }
    }

}
