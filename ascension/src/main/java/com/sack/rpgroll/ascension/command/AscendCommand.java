package com.sack.rpgroll.ascension.command;

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
 * /ascend info                  — resumen de tu progresión
 */
public class AscendCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("race", "specialize", "talent", "prestige", "affinity",
            "reputation", "title", "legacy", "info");

    private final AscensionEngine engine;
    private final FactionManager factionManager;
    private final TitleManager titleManager;
    private final LangManager lang;

    public AscendCommand(AscensionEngine engine, FactionManager factionManager, TitleManager titleManager,
            LangManager lang) {
        this.engine = engine;
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
            player.sendMessage(ComponentUtils.parseWithDefault(
                    lang.raw("command.reputation_entry", "faction", faction.displayName(), "amount",
                            state.getReputation(faction.id())), NamedTextColor.WHITE));
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

        return List.of();
    }

}
