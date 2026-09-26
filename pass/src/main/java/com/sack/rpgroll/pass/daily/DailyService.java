package com.sack.rpgroll.pass.daily;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.pass.PassClock;
import com.sack.rpgroll.pass.mission.MissionService;
import com.sack.rpgroll.pass.mission.MissionType;
import com.sack.rpgroll.pass.player.PassPlayer;
import com.sack.rpgroll.pass.reward.Reward;
import com.sack.rpgroll.pass.reward.RewardService;
import com.sack.rpgroll.pass.season.PassService;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

/** La recompensa de cada día: una al día, con racha y extra por rango. */
public class DailyService {

    private final PassService pass;
    private final MissionService missions;
    private final RewardService rewards;
    private final LangManager lang;
    private final PassClock clock;

    private DailyConfig config = new DailyConfig(true, 0, List.of(), List.of());

    public DailyService(PassService pass, MissionService missions, RewardService rewards, LangManager lang,
            PassClock clock) {
        this.pass = pass;
        this.missions = missions;
        this.rewards = rewards;
        this.lang = lang;
        this.clock = clock;
    }

    public void setConfig(DailyConfig config) {
        this.config = config;
    }

    public DailyConfig config() {
        return config;
    }

    public boolean canClaim(Player player) {
        return !config.days().isEmpty() && pass.player(player).lastDailyClaimDay() != clock.epochDay();
    }

    /** La racha que tendría el jugador si reclamara hoy. */
    public int streakIfClaimedToday(Player player) {
        PassPlayer state = pass.player(player);
        return nextStreak(state.lastDailyClaimDay(), clock.epochDay(), state.dailyStreak(), config.resetIfMissed());
    }

    /** Día del ciclo (desde 0) que toca con esa racha. */
    public int cycleIndex(int streak) {
        return config.days().isEmpty() ? 0 : Math.floorMod(streak - 1, config.days().size());
    }

    public Optional<DailyConfig.RankBonus> bonusFor(Player player) {
        return config.bonuses().stream().filter(b -> player.hasPermission(b.permission())).findFirst();
    }

    public boolean claim(Player player) {

        if (!canClaim(player)) {
            return false;
        }

        PassPlayer state = pass.player(player);
        long today = clock.epochDay();
        int streak = streakIfClaimedToday(player);
        List<Reward> day = config.days().get(cycleIndex(streak));

        state.recordDailyClaim(today, streak);
        pass.save(player);
        rewards.grant(player, day);
        lang.send(player, "daily.claimed", "day", cycleIndex(streak) + 1, "streak", streak);

        bonusFor(player).ifPresent(bonus -> {
            rewards.grant(player, bonus.rewards());
            lang.send(player, "daily.bonus", "rank", bonus.name());
        });

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.0f);
        pass.addXp(player, config.passXp());
        missions.progress(player, MissionType.CLAIM_DAILY, "", 1);
        return true;
    }

    static int nextStreak(long lastClaimDay, long today, int streak, boolean resetIfMissed) {

        if (lastClaimDay == today) {
            return streak;
        }

        boolean consecutive = lastClaimDay == today - 1;
        return consecutive || !resetIfMissed ? streak + 1 : 1;
    }

}
