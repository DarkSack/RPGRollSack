package com.sack.rpgroll.pass.requirement;

import com.sack.rpgroll.common.character.Characters;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.pass.PassClock;
import com.sack.rpgroll.pass.player.PassPlayer;
import com.sack.rpgroll.pass.season.Season;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Evalúa los requisitos de un nivel. Los que dependen de un plugin ausente
 * (nivel de personaje sin el core, misiones sin RPGRoll-Quests) no se pueden
 * comprobar y no bloquean: se omiten.
 */
public class RequirementService {

    private final LangManager lang;
    private final PassClock clock;

    public RequirementService(LangManager lang, PassClock clock) {
        this.lang = lang;
        this.clock = clock;
    }

    public List<RequirementStatus> evaluate(Player player, PassPlayer state, Season season,
            LevelRequirements requirements) {

        List<RequirementStatus> result = new ArrayList<>();

        if (requirements.isEmpty()) {
            return result;
        }

        if (requirements.characterLevel() > 0) {
            Characters.get().ifPresent(characters -> {
                int level = characters.level(player.getUniqueId());
                result.add(line(level >= requirements.characterLevel(), "requirement.character_level",
                        "current", level, "required", requirements.characterLevel()));
            });
        }

        if (requirements.playtimeMinutes() > 0) {
            int played = Math.min(state.seasonPlaytime(), requirements.playtimeMinutes());
            result.add(line(state.seasonPlaytime() >= requirements.playtimeMinutes(), "requirement.playtime",
                    "current", LevelRequirements.format(played),
                    "required", LevelRequirements.format(requirements.playtimeMinutes())));
        }

        LocalDate today = clock.today();

        if (requirements.seasonDay() > 1) {
            LocalDate from = season.start().plusDays(requirements.seasonDay() - 1L);
            result.add(line(!today.isBefore(from), "requirement.season_day",
                    "day", requirements.seasonDay(), "date", from));
        }

        if (requirements.availableFrom() != null) {
            result.add(line(!today.isBefore(requirements.availableFrom()), "requirement.available_from",
                    "date", requirements.availableFrom()));
        }

        if (requirements.votes() > 0) {
            result.add(line(state.votesTotal() >= requirements.votes(), "requirement.votes",
                    "current", Math.min(state.votesTotal(), requirements.votes()), "required", requirements.votes()));
        }

        if (requirements.dailyStreak() > 0) {
            result.add(line(state.dailyStreak() >= requirements.dailyStreak(), "requirement.daily_streak",
                    "current", Math.min(state.dailyStreak(), requirements.dailyStreak()),
                    "required", requirements.dailyStreak()));
        }

        if (requirements.missions() > 0) {
            result.add(line(state.seasonMissions() >= requirements.missions(), "requirement.missions",
                    "current", Math.min(state.seasonMissions(), requirements.missions()),
                    "required", requirements.missions()));
        }

        if (!requirements.quests().isEmpty() && Bukkit.getPluginManager().isPluginEnabled("RPGRoll-Quests")) {
            for (String quest : requirements.quests()) {
                result.add(line(QuestProgress.hasCompleted(player, quest), "requirement.quest", "quest", quest));
            }
        }

        if (requirements.permission() != null) {
            result.add(line(player.hasPermission(requirements.permission()), "requirement.permission",
                    "name", requirements.permissionName()));
        }

        return result;
    }

    public boolean meets(Player player, PassPlayer state, Season season, LevelRequirements requirements) {
        return requirements.isEmpty()
                || evaluate(player, state, season, requirements).stream().allMatch(RequirementStatus::met);
    }

    private RequirementStatus line(boolean met, String key, Object... pairs) {

        Object[] withMark = new Object[pairs.length + 2];
        withMark[0] = "mark";
        withMark[1] = lang.raw(met ? "requirement.met" : "requirement.unmet");
        System.arraycopy(pairs, 0, withMark, 2, pairs.length);

        return new RequirementStatus(met, lang.component(key, withMark));
    }

}
