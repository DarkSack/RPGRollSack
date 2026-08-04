package com.sack.rpgroll.ascension.command;

import com.sack.rpgroll.ascension.core.ClassSpecialization;
import com.sack.rpgroll.ascension.core.RaceEvolution;
import com.sack.rpgroll.ascension.deferred.FactionManager;
import com.sack.rpgroll.ascension.deferred.TitleManager;
import com.sack.rpgroll.ascension.engine.AscensionEngine;
import com.sack.rpgroll.ascension.player.AscensionPlayerState;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
public class AscendCommand implements CommandExecutor {

    private final AscensionEngine engine;
    private final FactionManager factionManager;
    private final TitleManager titleManager;

    public AscendCommand(AscensionEngine engine, FactionManager factionManager, TitleManager titleManager) {
        this.engine = engine;
        this.factionManager = factionManager;
        this.titleManager = titleManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Solo un jugador puede usar este comando.");
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
        player.sendMessage(Component.text(
                "Uso: /ascend <race|specialize|talent|prestige|affinity|reputation|title|legacy|info> [args]",
                NamedTextColor.RED));
    }

    private void handleRace(Player player, String[] args) {

        if (args.length < 2) {
            player.sendMessage(Component.text("Uso: /ascend race <id>", NamedTextColor.RED));
            return;
        }

        Optional<RaceEvolution> evolutionOpt = engine.getEvolutionManager().get(args[1]);
        if (evolutionOpt.isEmpty()) {
            player.sendMessage(Component.text("No existe una evolución con id: " + args[1], NamedTextColor.RED));
            return;
        }

        reportResult(player, engine.evolveRace(player, evolutionOpt.get()), "✔ Evolucionaste a: " + args[1]);
    }

    private void handleSpecialize(Player player, String[] args) {

        if (args.length < 2) {
            player.sendMessage(Component.text("Uso: /ascend specialize <id>", NamedTextColor.RED));
            return;
        }

        Optional<ClassSpecialization> specOpt = engine.getSpecializationManager().get(args[1]);
        if (specOpt.isEmpty()) {
            player.sendMessage(Component.text("No existe una especialización con id: " + args[1], NamedTextColor.RED));
            return;
        }

        reportResult(player, engine.specialize(player, specOpt.get()), "✔ Especialización elegida: " + args[1]);
    }

    private void handleTalent(Player player, String[] args) {

        if (args.length < 2) {
            player.sendMessage(Component.text("Uso: /ascend talent <id>", NamedTextColor.RED));
            return;
        }

        var result = engine.unlockTalent(player, args[1]);

        switch (result) {
            case OK -> player.sendMessage(Component.text("✔ Talento desbloqueado: " + args[1], NamedTextColor.GREEN));
            case NO_SPECIALIZATION -> player.sendMessage(Component.text(
                    "Necesitás elegir una especialización primero.", NamedTextColor.RED));
            case NODE_NOT_FOUND -> player.sendMessage(Component.text(
                    "Ese nodo de talento no existe en tu árbol.", NamedTextColor.RED));
            case ALREADY_UNLOCKED -> player.sendMessage(Component.text(
                    "Ya desbloqueaste ese talento.", NamedTextColor.RED));
            case MISSING_PREREQUISITES -> player.sendMessage(Component.text(
                    "Te faltan los talentos previos requeridos.", NamedTextColor.RED));
            case NOT_ENOUGH_POINTS -> player.sendMessage(Component.text(
                    "No tenés suficientes puntos de talento.", NamedTextColor.RED));
        }
    }

    private void handlePrestige(Player player) {
        reportResult(player, engine.prestige(player), "✔ ¡Prestigiaste! Bono acumulado: "
                + engine.getExperienceBonusPercent(player) + "% de experiencia.");
    }

    private void handleAffinity(Player player) {

        AscensionPlayerState state = engine.getStateManager().getOrLoad(player);

        if (state.getAffinityExperience().isEmpty()) {
            player.sendMessage(Component.text("Todavía no desarrollaste ninguna afinidad.", NamedTextColor.GRAY));
            return;
        }

        player.sendMessage(Component.text("Tus afinidades:", NamedTextColor.GOLD));

        state.getAffinityExperience().forEach((id, xp) -> player.sendMessage(Component.text(
                "• " + id + ": " + Math.min(100, xp / 100) + " (xp: " + xp + ")", NamedTextColor.WHITE)));
    }

    private void handleReputation(Player player) {

        AscensionPlayerState state = engine.getStateManager().getOrLoad(player);

        if (factionManager.count() == 0) {
            player.sendMessage(Component.text("No hay facciones definidas.", NamedTextColor.GRAY));
            return;
        }

        player.sendMessage(Component.text("Tu reputación:", NamedTextColor.GOLD));

        for (var faction : factionManager.getAll()) {
            player.sendMessage(Component.text(
                    "• " + faction.displayName() + ": " + state.getReputation(faction.id()), NamedTextColor.WHITE));
        }
    }

    private void handleTitle(Player player, String[] args) {

        AscensionPlayerState state = engine.getStateManager().getOrLoad(player);

        if (args.length < 2) {
            player.sendMessage(Component.text("Uso: /ascend title <id|clear>", NamedTextColor.RED));
            return;
        }

        if (args[1].equalsIgnoreCase("clear")) {
            state.setActiveTitle(null);
            player.sendMessage(Component.text("✔ Título desactivado.", NamedTextColor.GREEN));
            return;
        }

        if (!state.getUnlockedTitles().contains(args[1])) {
            player.sendMessage(Component.text("No desbloqueaste ese título todavía.", NamedTextColor.RED));
            return;
        }

        state.setActiveTitle(args[1]);
        String displayName = titleManager.get(args[1]).map(t -> t.displayName()).orElse(args[1]);
        player.sendMessage(Component.text("✔ Título activo: " + displayName, NamedTextColor.GREEN));
    }

    private void handleLegacy(Player player) {
        reportResult(player, engine.performLegacy(player), "✔ Legado completado — tu progreso se reinició "
                + "a cambio de un bono permanente.");
    }

    private void handleInfo(Player player) {

        AscensionPlayerState state = engine.getStateManager().getOrLoad(player);

        player.sendMessage(Component.text("Progresión avanzada:", NamedTextColor.GOLD));
        player.sendMessage(Component.text(
                "Evolución: " + (state.getCurrentEvolutionId() == null ? "ninguna" : state.getCurrentEvolutionId()),
                NamedTextColor.WHITE));
        player.sendMessage(Component.text(
                "Especialización: " + (state.getCurrentSpecializationId() == null ? "ninguna"
                        : state.getCurrentSpecializationId()),
                NamedTextColor.WHITE));
        player.sendMessage(Component.text(
                "Talentos desbloqueados: " + state.getUnlockedTalents().size() + " (puntos disponibles: "
                        + state.getAvailableTalentPoints() + ")",
                NamedTextColor.WHITE));
        player.sendMessage(Component.text("Prestigio: " + state.getPrestigeCount(), NamedTextColor.WHITE));
        player.sendMessage(Component.text("Legado: " + state.getLegacyCount(), NamedTextColor.WHITE));
        player.sendMessage(Component.text(
                "Bono de experiencia total: " + engine.getExperienceBonusPercent(player) + "%", NamedTextColor.WHITE));
    }

    private void reportResult(Player player, java.util.List<String> reasons, String successMessage) {

        if (reasons.isEmpty()) {
            player.sendMessage(Component.text(successMessage, NamedTextColor.GREEN));
            return;
        }

        player.sendMessage(Component.text("No se pudo:", NamedTextColor.RED));
        reasons.forEach(reason -> player.sendMessage(Component.text("- " + reason, NamedTextColor.RED)));
    }

}
