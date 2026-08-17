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
import com.sack.rpgroll.api.race.RaceManager;
import com.sack.rpgroll.util.TabCompleteUtil;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Comando de administrador para cambiar la clase de un jugador.
 * Uso: /rpg admin setclass <jugador> <claseId> [--recalc]
 * Misma lógica que AdminSetRaceCommand respecto a --recalc.
 */
public class AdminSetClassCommand implements RPGCommand {

    private final RPGRoll plugin;

    public AdminSetClassCommand(RPGRoll plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

        LangManager lang = plugin.getBootstrap().getServices().get(LangManager.class);

        if (args.length < 2) {
            lang.send(sender, "admin_setclass.usage", "usage", getUsage());
            return;
        }

        String targetName = args[0];
        String classId = args[1];
        boolean recalc = args.length >= 3 && args[2].equalsIgnoreCase("--recalc");

        Player target = Bukkit.getPlayerExact(targetName);

        if (target == null) {
            lang.send(sender, "error.player_offline", "player", targetName);
            return;
        }

        try {

            ClassManager classManager = plugin.getBootstrap().getServices().get(ClassManager.class);
            Optional<PlayerClass> classOpt = classManager.get(classId);

            if (classOpt.isEmpty()) {
                lang.send(sender, "admin_setclass.class_not_found", "class", classId);
                return;
            }

            PlayerManager playerManager = plugin.getBootstrap().getServices().get(PlayerManager.class);
            Optional<RPGPlayer> rpgPlayerOpt = playerManager.getPlayer(target.getUniqueId());

            if (rpgPlayerOpt.isEmpty()) {
                lang.send(sender, "error.no_rpg_data");
                return;
            }

            PlayerClass playerClass = classOpt.get();
            RPGPlayer rpgPlayer = rpgPlayerOpt.get().setClass(classId);

            if (recalc) {
                RaceManager raceManager = plugin.getBootstrap().getServices().get(RaceManager.class);
                PlayerStats recalculated = recalculateStats(playerClass, raceManager, rpgPlayer.getRace());
                CombatStats recalculatedCombatStats = CombatStats.create(
                        recalculated.getConstitutionModifier(),
                        recalculated.getIntelligenceModifier(),
                        recalculated.getDexterityModifier(),
                        rpgPlayer.getLevel());
                rpgPlayer = rpgPlayer.updateStats(recalculated).updateCombatStats(recalculatedCombatStats);
            }

            playerManager.savePlayer(rpgPlayer);

            lang.send(sender, "admin_setclass.success", "player", target.getName(), "class", playerClass.displayName());

            if (recalc) {
                lang.send(sender, "admin_setclass.stats_recalculated");
            } else {
                lang.send(sender, "admin_setclass.stats_unmodified");
            }

            lang.send(target, "admin_setclass.notify_target", "class", playerClass.displayName());

        } catch (Exception exception) {
            lang.send(sender, "admin_setclass.error");
            plugin.getLogger().severe("✘ Error en /rpg admin setclass: " + exception.getMessage());
        }
    }

    private PlayerStats recalculateStats(PlayerClass playerClass, RaceManager raceManager, String raceId) {

        PlayerStats stats = PlayerStats.createDefault();

        if (raceId != null && !raceId.isEmpty()) {
            Optional<Race> raceOpt = raceManager.get(raceId);
            if (raceOpt.isPresent()) {
                stats = applyBonuses(stats, raceOpt.get().baseAttributes());
            }
        }

        stats = applyBonuses(stats, playerClass.baseAttributes());
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
        return "setclass";
    }

    @Override
    public String getDescription() {
        return "Cambia la clase de un jugador (admin)";
    }

    @Override
    public String getUsage() {
        return "/rpg setclass <jugador> <claseId> [--recalc]";
    }

    @Override
    public String getPermission() {
        return "rpgroll.admin.setclass";
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
            ClassManager classManager = plugin.getBootstrap().getServices().get(ClassManager.class);
            return classManager.getAll().stream().map(PlayerClass::id).toList();
        }
        if (args.length == 3) {
            return List.of("--recalc");
        }
        return List.of();
    }

}