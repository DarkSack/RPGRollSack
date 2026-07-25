package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.RPGCommand;
import com.sack.rpgroll.gameplay.stats.StatType;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;
import com.sack.rpgroll.player.stats.PlayerStats;
import com.sack.rpgroll.playerclass.ClassManager;
import com.sack.rpgroll.playerclass.PlayerClass;
import com.sack.rpgroll.race.Race;
import com.sack.rpgroll.race.RaceManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

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

        if (args.length < 2) {
            sender.sendMessage(Component.text("Uso: " + getUsage(), NamedTextColor.RED));
            return;
        }

        String targetName = args[0];
        String classId = args[1];
        boolean recalc = args.length >= 3 && args[2].equalsIgnoreCase("--recalc");

        Player target = Bukkit.getPlayerExact(targetName);

        if (target == null) {
            sender.sendMessage(
                    Component.text("Jugador no encontrado o desconectado: " + targetName, NamedTextColor.RED));
            return;
        }

        try {

            ClassManager classManager = plugin.getBootstrap().getServices().get(ClassManager.class);
            Optional<PlayerClass> classOpt = classManager.get(classId);

            if (classOpt.isEmpty()) {
                sender.sendMessage(Component.text("No existe la clase: " + classId, NamedTextColor.RED));
                return;
            }

            PlayerManager playerManager = plugin.getBootstrap().getServices().get(PlayerManager.class);
            Optional<RPGPlayer> rpgPlayerOpt = playerManager.getPlayer(target.getUniqueId());

            if (rpgPlayerOpt.isEmpty()) {
                sender.sendMessage(Component.text("El jugador no tiene datos RPG cargados.", NamedTextColor.RED));
                return;
            }

            PlayerClass playerClass = classOpt.get();
            RPGPlayer rpgPlayer = rpgPlayerOpt.get().setClass(classId);

            if (recalc) {
                RaceManager raceManager = plugin.getBootstrap().getServices().get(RaceManager.class);
                PlayerStats recalculated = recalculateStats(playerClass, raceManager, rpgPlayer.getRace());
                rpgPlayer = rpgPlayer.updateStats(recalculated);
            }

            playerManager.savePlayer(rpgPlayer);

            sender.sendMessage(Component.text("✔ Clase de " + target.getName() + " cambiada a: ", NamedTextColor.GREEN)
                    .append(Component.text(playerClass.displayName(), NamedTextColor.GOLD)));

            if (recalc) {
                sender.sendMessage(Component.text("  (stats recalculados desde cero)", NamedTextColor.YELLOW));
            } else {
                sender.sendMessage(Component.text("  (stats sin modificar — usa --recalc para recalcularlos)",
                        NamedTextColor.GRAY));
            }

            target.sendMessage(Component.text("Un administrador cambió tu clase a: ", NamedTextColor.YELLOW)
                    .append(Component.text(playerClass.displayName(), NamedTextColor.GOLD)));

        } catch (Exception exception) {
            sender.sendMessage(Component.text("Error al cambiar la clase.", NamedTextColor.RED));
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

}