package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.RPGCommand;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gameplay.combat.CombatStats;
import com.sack.rpgroll.api.stats.StatType;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;
import com.sack.rpgroll.player.stats.PlayerStats;
import com.sack.rpgroll.api.playerclass.ClassManager;
import com.sack.rpgroll.api.playerclass.PlayerClass;
import com.sack.rpgroll.api.race.Race;
import com.sack.rpgroll.race.RaceAttributeApplier;
import com.sack.rpgroll.api.race.RaceManager;
import com.sack.rpgroll.util.TabCompleteUtil;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Comando de administrador para cambiar la raza de un jugador.
 * Uso: /rpg admin setrace <jugador> <razaId> [--recalc]
 * <p>
 * Por defecto solo cambia la identidad (raza) y reaplica los modificadores
 * físicos de la nueva raza — no toca PlayerStats, para no borrar puntos
 * que el jugador ya haya invertido con StatPointAllocator.
 * <p>
 * Con el flag --recalc, además recalcula PlayerStats desde cero
 * (default + bonos de raza actual + bonos de clase actual), perdiendo
 * cualquier asignación manual previa. Usar con cuidado.
 */
public class AdminSetRaceCommand implements RPGCommand {

    private final RPGRoll plugin;

    public AdminSetRaceCommand(RPGRoll plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

        LangManager lang = plugin.getBootstrap().getServices().get(LangManager.class);

        if (args.length < 2) {
            lang.send(sender, "admin_setrace.usage", "usage", getUsage());
            return;
        }

        String targetName = args[0];
        String raceId = args[1];
        boolean recalc = args.length >= 3 && args[2].equalsIgnoreCase("--recalc");

        Player target = Bukkit.getPlayerExact(targetName);

        if (target == null) {
            lang.send(sender, "error.player_offline", "player", targetName);
            return;
        }

        try {

            RaceManager raceManager = plugin.getBootstrap().getServices().get(RaceManager.class);
            Optional<Race> raceOpt = raceManager.get(raceId);

            if (raceOpt.isEmpty()) {
                lang.send(sender, "admin_setrace.race_not_found", "race", raceId);
                return;
            }

            PlayerManager playerManager = plugin.getBootstrap().getServices().get(PlayerManager.class);
            Optional<RPGPlayer> rpgPlayerOpt = playerManager.getPlayer(target.getUniqueId());

            if (rpgPlayerOpt.isEmpty()) {
                lang.send(sender, "error.no_rpg_data");
                return;
            }

            Race race = raceOpt.get();
            RPGPlayer rpgPlayer = rpgPlayerOpt.get().setRace(raceId);

            if (recalc) {
                ClassManager classManager = plugin.getBootstrap().getServices().get(ClassManager.class);
                PlayerStats recalculated = recalculateStats(race, classManager, rpgPlayer.getPlayerClass());
                CombatStats recalculatedCombatStats = CombatStats.create(
                        recalculated.getConstitutionModifier(),
                        recalculated.getIntelligenceModifier(),
                        recalculated.getDexterityModifier(),
                        rpgPlayer.getLevel());
                rpgPlayer = rpgPlayer.updateStats(recalculated).updateCombatStats(recalculatedCombatStats);
            }

            playerManager.savePlayer(rpgPlayer);

            RaceAttributeApplier raceAttributeApplier = plugin.getBootstrap().getServices()
                    .get(RaceAttributeApplier.class);
            raceAttributeApplier.apply(target, race);

            lang.send(sender, "admin_setrace.success", "player", target.getName(), "race", race.displayName());

            if (recalc) {
                lang.send(sender, "admin_setclass.stats_recalculated");
            } else {
                lang.send(sender, "admin_setclass.stats_unmodified");
            }

            lang.send(target, "admin_setrace.notify_target", "race", race.displayName());

        } catch (Exception exception) {
            lang.send(sender, "admin_setrace.error");
            plugin.getLogger().severe("✘ Error en /rpg admin setrace: " + exception.getMessage());
        }
    }

    private PlayerStats recalculateStats(Race race, ClassManager classManager, String classId) {

        PlayerStats stats = PlayerStats.createDefault();
        stats = applyBonuses(stats, race.baseAttributes());

        if (classId != null && !classId.isEmpty()) {
            Optional<PlayerClass> classOpt = classManager.get(classId);
            if (classOpt.isPresent()) {
                stats = applyBonuses(stats, classOpt.get().baseAttributes());
            }
        }

        return stats;
    }

    private PlayerStats applyBonuses(PlayerStats stats, Map<StatType, Integer> bonuses) {

        PlayerStats result = stats;

        for (Map.Entry<StatType, Integer> entry : bonuses.entrySet()) {
            StatType stat = entry.getKey();
            int newValue = clamp(result.get(stat) + entry.getValue(), PlayerStats.MIN_STAT, PlayerStats.MAX_STAT);
            result = result.with(stat, newValue);
        }

        return result;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    @Override
    public String getName() {
        return "setrace";
    }

    @Override
    public String getDescription() {
        return "Cambia la raza de un jugador (admin)";
    }

    @Override
    public String getUsage() {
        return "/rpg setrace <jugador> <razaId> [--recalc]";
    }

    @Override
    public String getPermission() {
        return "rpgroll.admin.setrace";
    }

    @Override
    public List<String> getAliases() {
        return List.of();
    }

    @Override
    public boolean isPlayerOnly() {
        return false;
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        if (args.length <= 1) {
            return TabCompleteUtil.allOnlinePlayerNames();
        }
        if (args.length == 2) {
            RaceManager raceManager = plugin.getBootstrap().getServices().get(RaceManager.class);
            return raceManager.getAll().stream().map(Race::id).toList();
        }
        if (args.length == 3) {
            return List.of("--recalc");
        }
        return List.of();
    }

}