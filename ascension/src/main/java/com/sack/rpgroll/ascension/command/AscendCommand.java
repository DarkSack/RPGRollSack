package com.sack.rpgroll.ascension.command;

import com.sack.rpgroll.ascension.deferred.Achievement;
import com.sack.rpgroll.ascension.deferred.FactionRank;
import com.sack.rpgroll.ascension.deferred.JobEvolution;
import com.sack.rpgroll.ascension.deferred.SecretTargetType;
import com.sack.rpgroll.ascension.deferred.SecretUnlockRequirement;
import com.sack.rpgroll.ascension.deferred.Title;
import com.sack.rpgroll.ascension.engine.AchievementProgress;
import com.sack.rpgroll.ascension.engine.ProgressService;
import java.util.Locale;
import com.sack.rpgroll.common.command.Senders;

import com.sack.rpgroll.ascension.core.ClassSpecialization;
import com.sack.rpgroll.ascension.core.RaceEvolution;
import com.sack.rpgroll.ascension.deferred.FactionManager;
import com.sack.rpgroll.ascension.deferred.TitleManager;
import com.sack.rpgroll.ascension.engine.AscensionEngine;
import com.sack.rpgroll.ascension.player.AscensionPlayerState;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.util.ComponentUtils;
import com.sack.rpgroll.util.TabCompleteUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

/**
 * /ascend race &lt;id&gt;         — evoluciona tu raza
 * /ascend specialize &lt;id&gt;   — elige una especialización de tu clase
 * /ascend talent &lt;id&gt;       — desbloquea un nodo del árbol de talentos
 * /ascend prestige             — prestigia si cumplís el nivel requerido
 * /ascend affinity             — tus niveles de afinidad
 * /ascend reputation           — tu reputación con cada facción
 * /ascend title &lt;id|clear&gt; — activa un título desbloqueado
 * /ascend legacy                — legado (reset total por un bono permanente)
 * /ascend achievements          — tus logros y lo que te falta
 * /ascend titles                — tus títulos
 * /ascend jobs                  — rangos de oficio
 * /ascend secrets               — contenido secreto descubierto y pistas
 * /ascend secret claim &lt;id&gt;  — adopta una raza o clase secreta desbloqueada
 * /ascend info                  — resumen de tu progresión
 */
public class AscendCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("race", "specialize", "talent", "prestige", "affinity",
            "reputation", "title", "titles", "achievements", "jobs", "secrets", "secret", "legacy", "info");

    private final AscensionEngine engine;
    private final ProgressService progress;
    private final FactionManager factionManager;
    private final TitleManager titleManager;
    private final LangManager lang;

    public AscendCommand(AscensionEngine engine, ProgressService progress, FactionManager factionManager,
            TitleManager titleManager, LangManager lang) {
        this.engine = engine;
        this.progress = progress;
        this.factionManager = factionManager;
        this.titleManager = titleManager;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(Senders.asPlayer(sender) instanceof Player player)) {
            lang.send(sender, "general.player_only");
            return true;
        }

        if (args.length < 1) {
            sendUsage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "race" -> handleRace(player, args);
            case "specialize" -> handleSpecialize(player, args);
            case "talent" -> handleTalent(player, args);
            case "prestige" -> handlePrestige(player);
            case "affinity" -> handleAffinity(player);
            case "reputation" -> handleReputation(player);
            case "title" -> handleTitle(player, args);
            case "titles" -> handleTitles(player);
            case "achievements" -> handleAchievements(player);
            case "jobs" -> handleJobs(player);
            case "secrets" -> handleSecrets(player);
            case "secret" -> handleSecret(player, args);
            case "legacy" -> handleLegacy(player);
            case "info" -> handleInfo(player);
            default -> sendUsage(player);
        }

        return true;
    }

    private void sendUsage(Player player) {
        lang.send(player, "command.usage");
    }

    private void handleRace(Player player, String[] args) {

        if (args.length < 2) {
            lang.send(player, "command.race_usage");
            return;
        }

        Optional<RaceEvolution> evolutionOpt = engine.getEvolutionManager().get(args[1]);
        if (evolutionOpt.isEmpty()) {
            lang.send(player, "command.race_not_found", "id", args[1]);
            return;
        }

        reportResult(player, engine.evolveRace(player, evolutionOpt.get()),
                lang.raw("command.race_success", "id", args[1]));
    }

    private void handleSpecialize(Player player, String[] args) {

        if (args.length < 2) {
            lang.send(player, "command.specialize_usage");
            return;
        }

        Optional<ClassSpecialization> specOpt = engine.getSpecializationManager().get(args[1]);
        if (specOpt.isEmpty()) {
            lang.send(player, "command.specialize_not_found", "id", args[1]);
            return;
        }

        reportResult(player, engine.specialize(player, specOpt.get()),
                lang.raw("command.specialize_success", "id", args[1]));
    }

    private void handleTalent(Player player, String[] args) {

        if (args.length < 2) {
            lang.send(player, "command.talent_usage");
            return;
        }

        var result = engine.unlockTalent(player, args[1]);

        switch (result) {
            case OK -> lang.send(player, "command.talent_success", "id", args[1]);
            case NO_SPECIALIZATION -> lang.send(player, "command.talent_no_specialization");
            case NODE_NOT_FOUND -> lang.send(player, "command.talent_node_not_found");
            case ALREADY_UNLOCKED -> lang.send(player, "command.talent_already_unlocked");
            case MISSING_PREREQUISITES -> lang.send(player, "command.talent_missing_prerequisites");
            case NOT_ENOUGH_POINTS -> lang.send(player, "command.talent_not_enough_points");
        }
    }

    private void handlePrestige(Player player) {
        reportResult(player, engine.prestige(player), lang.raw("command.prestige_success", "percent",
                engine.getExperienceBonusPercent(player)));
    }

    private void handleAffinity(Player player) {

        AscensionPlayerState state = engine.getStateManager().getOrLoad(player);

        if (state.getAffinityExperience().isEmpty()) {
            lang.send(player, "command.affinity_empty");
            return;
        }

        lang.send(player, "command.affinity_header");

        state.getAffinityExperience().forEach((id, xp) -> player.sendMessage(ComponentUtils.parseWithDefault(
                lang.raw("command.affinity_entry", "id", id, "level", Math.min(100, xp / 100), "xp", xp), NamedTextColor.WHITE)));
    }

    private void handleReputation(Player player) {

        AscensionPlayerState state = engine.getStateManager().getOrLoad(player);

        if (factionManager.count() == 0) {
            lang.send(player, "command.reputation_empty");
            return;
        }

        lang.send(player, "command.reputation_header");

        for (var faction : factionManager.getAll()) {

            int amount = state.getReputation(faction.id());
            String rank = faction.rankFor(amount).map(FactionRank::displayName).orElse(lang.raw("command.none_label"));

            player.sendMessage(ComponentUtils.parseWithDefault(
                    lang.raw("command.reputation_entry", "faction", faction.displayName(), "amount", amount,
                            "rank", rank), NamedTextColor.WHITE));

            faction.nextRank(amount).ifPresent(next -> lang.send(player, "command.reputation_next",
                    "rank", next.displayName(), "missing", next.threshold() - amount));
        }
    }

    private void handleTitle(Player player, String[] args) {

        AscensionPlayerState state = engine.getStateManager().getOrLoad(player);

        if (args.length < 2) {
            lang.send(player, "command.title_usage");
            return;
        }

        if (args[1].equalsIgnoreCase("clear")) {
            state.setActiveTitle(null);
            lang.send(player, "command.title_cleared");
            return;
        }

        if (!state.getUnlockedTitles().contains(args[1])) {
            lang.send(player, "command.title_not_unlocked");
            return;
        }

        state.setActiveTitle(args[1]);
        String displayName = titleManager.get(args[1]).map(t -> t.displayName()).orElse(args[1]);
        lang.send(player, "command.title_active", "name", displayName);
    }

    private void handleLegacy(Player player) {
        reportResult(player, engine.performLegacy(player), lang.raw("command.legacy_success"));
    }

    private void handleInfo(Player player) {

        AscensionPlayerState state = engine.getStateManager().getOrLoad(player);
        String noneLabel = lang.raw("command.none_label");

        lang.send(player, "command.info_header");
        lang.send(player, "command.info_evolution", "value",
                state.getCurrentEvolutionId() == null ? noneLabel : state.getCurrentEvolutionId());
        lang.send(player, "command.info_specialization", "value",
                state.getCurrentSpecializationId() == null ? noneLabel : state.getCurrentSpecializationId());
        lang.send(player, "command.info_talents", "count", state.getUnlockedTalents().size(), "points",
                state.getAvailableTalentPoints());
        lang.send(player, "command.info_prestige", "count", state.getPrestigeCount());
        lang.send(player, "command.info_legacy", "count", state.getLegacyCount());
        lang.send(player, "command.info_exp_bonus", "percent", engine.getExperienceBonusPercent(player));
        lang.send(player, "command.info_achievements", "count", state.getUnlockedAchievements().size(),
                "total", progress.achievements().getAchievementManager().count());
    }

    private void handleAchievements(Player player) {

        AscensionPlayerState state = engine.getStateManager().getOrLoad(player);
        var achievementEngine = progress.achievements();
        var all = achievementEngine.getAchievementManager().getAll();

        if (all.isEmpty()) {
            lang.send(player, "command.achievements_empty");
            return;
        }

        lang.send(player, "command.achievements_header", "unlocked", state.getUnlockedAchievements().size(),
                "total", all.size());

        for (Achievement achievement : all) {

            boolean unlocked = state.getUnlockedAchievements().contains(achievement.id());

            if (!unlocked && achievement.hidden()) {
                lang.send(player, "command.achievements_hidden");
                continue;
            }

            lang.send(player, unlocked ? "command.achievements_unlocked" : "command.achievements_locked",
                    "name", achievement.displayName(), "description", achievement.description());

            if (unlocked) {
                continue;
            }

            for (int i = 0; i < achievement.criteria().size(); i++) {
                var criterion = achievement.criteria().get(i);
                lang.send(player, "command.achievements_criterion",
                        "type", lang.raw("trigger." + criterion.type().name().toLowerCase(Locale.ROOT)),
                        "target", describeTarget(criterion),
                        "current", Math.min(achievementEngine.current(player, achievement, i),
                                AchievementProgress.required(criterion)),
                        "required", AchievementProgress.required(criterion));
            }
        }
    }

    private String describeTarget(com.sack.rpgroll.ascension.progress.Criterion criterion) {

        if (criterion.key() != null) {
            return criterion.key();
        }

        return com.sack.rpgroll.ascension.progress.Glob.isWildcard(criterion.target())
                ? lang.raw("command.any_target")
                : criterion.target();
    }

    private void handleTitles(Player player) {

        AscensionPlayerState state = engine.getStateManager().getOrLoad(player);

        if (state.getUnlockedTitles().isEmpty()) {
            lang.send(player, "command.titles_empty");
            return;
        }

        lang.send(player, "command.titles_header");

        for (String titleId : state.getUnlockedTitles().stream().sorted().toList()) {
            String name = titleManager.get(titleId).map(Title::displayName).orElse(titleId);
            lang.send(player, titleId.equals(state.getActiveTitle()) ? "command.titles_active_entry"
                    : "command.titles_entry", "name", name, "id", titleId);
        }
    }

    private void handleSecrets(Player player) {

        var secretEngine = progress.secrets();
        var secrets = secretEngine.getSecretManager().getAll();

        if (secrets.isEmpty()) {
            lang.send(player, "command.secrets_empty");
            return;
        }

        lang.send(player, "command.secrets_header");

        for (SecretUnlockRequirement secret : secrets) {

            String type = secretEngine.typeName(secret.targetType());

            if (secretEngine.isUnlocked(player, secret)) {
                boolean claimable = secret.targetType() == SecretTargetType.CLASS
                        || secret.targetType() == SecretTargetType.RACE;
                lang.send(player, claimable ? "command.secrets_claimable" : "command.secrets_unlocked",
                        "type", type, "name", secret.targetId(), "id", secret.id());
            } else if (secret.hint() != null && !secret.hint().isBlank()) {
                lang.send(player, "command.secrets_hint", "type", type, "hint", secret.hint());
            } else {
                lang.send(player, "command.secrets_locked", "type", type);
            }
        }
    }

    private void handleSecret(Player player, String[] args) {

        if (args.length < 3 || !args[1].equalsIgnoreCase("claim")) {
            lang.send(player, "command.secret_usage");
            return;
        }

        switch (progress.secrets().claim(player, args[2])) {
            case OK -> lang.send(player, "command.secret_claimed");
            case NOT_FOUND -> lang.send(player, "command.secret_not_found", "id", args[2]);
            case NOT_CLAIMABLE -> lang.send(player, "command.secret_not_claimable");
            case LOCKED -> lang.send(player, "command.secret_locked");
            case FAILED -> lang.send(player, "command.secret_failed");
        }
    }

    private void handleJobs(Player player) {

        AscensionPlayerState state = engine.getStateManager().getOrLoad(player);
        var jobEngine = progress.jobEvolutions();

        if (jobEngine.getJobEvolutionManager().count() == 0) {
            lang.send(player, "command.jobs_empty");
            return;
        }

        lang.send(player, "command.jobs_header");

        jobEngine.getJobEvolutionManager().getAll().stream()
                .map(JobEvolution::baseJob)
                .distinct()
                .sorted()
                .forEach(job -> {
                    lang.send(player, "command.jobs_job", "job", job);
                    for (JobEvolution evolution : jobEngine.evolutionsOf(job)) {
                        lang.send(player, state.getJobEvolutions().contains(evolution.id())
                                ? "command.jobs_evolution_owned" : "command.jobs_evolution_pending",
                                "name", evolution.displayName(), "level", evolution.requiredJobLevel());
                    }
                });
    }

    private void reportResult(Player player, java.util.List<String> reasons, String successMessage) {

        if (reasons.isEmpty()) {
            player.sendMessage(ComponentUtils.parse(successMessage));
            return;
        }

        lang.send(player, "command.failed_header");
        reasons.forEach(reason -> lang.send(player, "command.failed_reason", "reason", reason));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
        }

        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "race" -> TabCompleteUtil.filter(args[1],
                        engine.getEvolutionManager().getAll().stream().map(RaceEvolution::id).toList());
                case "specialize" -> TabCompleteUtil.filter(args[1],
                        engine.getSpecializationManager().getAll().stream().map(ClassSpecialization::id).toList());
                case "secret" -> TabCompleteUtil.filter(args[1], List.of("claim"));
                case "title" -> {
                    if (!(Senders.asPlayer(sender) instanceof Player player)) {
                        yield List.of();
                    }
                    List<String> options = new java.util.ArrayList<>(
                            engine.getStateManager().getOrLoad(player).getUnlockedTitles());
                    options.add("clear");
                    yield TabCompleteUtil.filter(args[1], options);
                }
                default -> List.of();
            };
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("secret") && args[1].equalsIgnoreCase("claim")
                && Senders.asPlayer(sender) instanceof Player player) {
            List<String> claimable = progress.secrets().getSecretManager().getAll().stream()
                    .filter(secret -> secret.targetType() == SecretTargetType.CLASS
                            || secret.targetType() == SecretTargetType.RACE)
                    .filter(secret -> progress.secrets().isUnlocked(player, secret))
                    .map(SecretUnlockRequirement::id)
                    .toList();
            return TabCompleteUtil.filter(args[2], claimable);
        }

        return List.of();
    }

}
