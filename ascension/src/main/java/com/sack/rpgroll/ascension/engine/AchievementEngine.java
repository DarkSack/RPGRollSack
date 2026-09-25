package com.sack.rpgroll.ascension.engine;

import com.sack.rpgroll.api.RPGRollAPI;
import com.sack.rpgroll.ascension.deferred.Achievement;
import com.sack.rpgroll.ascension.deferred.AchievementManager;
import com.sack.rpgroll.ascension.player.AscensionPlayerState;
import com.sack.rpgroll.ascension.player.AscensionPlayerStateManager;
import com.sack.rpgroll.ascension.progress.Criterion;
import com.sack.rpgroll.ascension.progress.ProgressEvent;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.player.RPGPlayer;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Logros: los criterios de contador avanzan con cada evento, los de estado
 * se comparan al revisar, y al cumplirlos todos se desbloquea el logro y se
 * entregan sus recompensas. Las cuentas están en {@link AchievementProgress}.
 */
public class AchievementEngine {

    private final AchievementManager achievementManager;
    private final AscensionPlayerStateManager stateManager;
    private final RewardService rewardService;
    private final LangManager lang;

    public AchievementEngine(AchievementManager achievementManager, AscensionPlayerStateManager stateManager,
            RewardService rewardService, LangManager lang) {
        this.achievementManager = achievementManager;
        this.stateManager = stateManager;
        this.rewardService = rewardService;
        this.lang = lang;
    }

    public AchievementManager getAchievementManager() {
        return achievementManager;
    }

    public void onEvent(Player player, ProgressEvent event) {

        AscensionPlayerState state = stateManager.getOrLoad(player);
        AchievementProgress.StateValues values = stateValues(player, state);

        for (Achievement achievement : achievementManager.getAll()) {

            if (state.getUnlockedAchievements().contains(achievement.id())) {
                continue;
            }

            if (AchievementProgress.apply(state, achievement, event)
                    && AchievementProgress.isComplete(state, achievement, values)) {
                unlock(player, achievement);
            }
        }
    }

    /**
     * Revisa los logros que dependen del estado (nivel, prestigio,
     * reputación…). Repite mientras se desbloquee alguno, porque desbloquear
     * uno puede completar otro (un logro de "consigue 10 logros", o uno de
     * reputación que se cumple con la recompensa del anterior).
     */
    public void checkState(Player player) {

        AscensionPlayerState state = stateManager.getOrLoad(player);
        boolean unlockedAny;
        int guard = 0;

        do {
            unlockedAny = false;
            AchievementProgress.StateValues values = stateValues(player, state);

            for (Achievement achievement : achievementManager.getAll()) {
                if (!state.getUnlockedAchievements().contains(achievement.id())
                        && AchievementProgress.isComplete(state, achievement, values)) {
                    unlock(player, achievement);
                    unlockedAny = true;
                }
            }
        } while (unlockedAny && ++guard < 10);
    }

    /**
     * @return false si ya lo tenía
     */
    public boolean unlock(Player player, Achievement achievement) {

        AscensionPlayerState state = stateManager.getOrLoad(player);

        if (!state.unlockAchievement(achievement.id())) {
            return false;
        }

        state.clearAchievementProgress(achievement.id());

        lang.send(player, "progress.achievement_unlocked", "name", achievement.displayName());
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.0f);

        rewardService.grant(player, achievement.rewards(), achievement.displayName());
        return true;
    }

    /** Valor actual y requerido de cada criterio, para mostrarlo. */
    public int current(Player player, Achievement achievement, int index) {
        AscensionPlayerState state = stateManager.getOrLoad(player);
        return AchievementProgress.current(state, achievement, index, stateValues(player, state));
    }

    AchievementProgress.StateValues stateValues(Player player, AscensionPlayerState state) {

        RPGPlayer rpgPlayer = RPGRollAPI.isReady()
                ? RPGRollAPI.get().getPlayer(player.getUniqueId()).orElse(null)
                : null;

        return (Criterion criterion) -> switch (criterion.type()) {
            case REACH_LEVEL -> rpgPlayer == null ? 0 : rpgPlayer.getLevel();
            case JOB_LEVEL -> rpgPlayer == null || criterion.key() == null || !rpgPlayer.getJobs().hasJob(criterion.key())
                    ? 0
                    : rpgPlayer.getJobs().getLevel(criterion.key());
            case PRESTIGE -> state.getPrestigeCount();
            case LEGACY -> state.getLegacyCount();
            case REPUTATION -> criterion.key() == null ? 0 : state.getReputation(criterion.key());
            case ACHIEVEMENTS -> state.getUnlockedAchievements().size();
            default -> 0;
        };
    }

}
