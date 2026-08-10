package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.RPGCommand;
import com.sack.rpgroll.gameplay.combat.CombatStats;
import com.sack.rpgroll.gameplay.levelup.LevelUpRewardsConfig;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;
import com.sack.rpgroll.player.stats.PlayerStats;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

/**
 * Comando de administrador para reiniciar los atributos D&D de un jugador
 * a sus valores por defecto, devolviéndole como puntos sin gastar todos
 * los puntos que debería haber ganado por nivel (respec completo).
 * <p>
 * No toca la salud/maná máximos acumulados por nivel — esos crecen con
 * el nivel, no con los puntos de atributo, así que un respec de atributos
 * no debería hacer retroceder ese progreso.
 * <p>
 * Uso: /rpg admin resetstats <jugador>
 */
public class AdminResetStatsCommand implements RPGCommand {

    private final RPGRoll plugin;

    public AdminResetStatsCommand(RPGRoll plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

        if (args.length < 1) {
            sender.sendMessage(Component.text("Uso: " + getUsage(), NamedTextColor.RED));
            return;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayerExact(targetName);

        if (target == null) {
            sender.sendMessage(
                    Component.text("Jugador no encontrado o desconectado: " + targetName, NamedTextColor.RED));
            return;
        }

        try {

            PlayerManager playerManager = plugin.getBootstrap().getServices().get(PlayerManager.class);
            LevelUpRewardsConfig rewardsConfig = plugin.getBootstrap().getServices().get(LevelUpRewardsConfig.class);

            Optional<RPGPlayer> rpgPlayerOpt = playerManager.getPlayer(target.getUniqueId());

            if (rpgPlayerOpt.isEmpty()) {
                sender.sendMessage(Component.text("El jugador no tiene datos RPG cargados.", NamedTextColor.RED));
                return;
            }

            RPGPlayer rpgPlayer = rpgPlayerOpt.get();

            int totalEarned = 0;
            for (int level = 2; level <= rpgPlayer.getLevel(); level++) {
                totalEarned += rewardsConfig.getRewards(level)
                        .map(rewards -> rewards.statPoints())
                        .orElse(0);
            }

            PlayerStats defaultStats = PlayerStats.createDefault();

            CombatStats currentCombatStats = rpgPlayer.getCombatStats();
            CombatStats refreshedCombatStats = CombatStats.of(
                    currentCombatStats.maxHealth(),
                    currentCombatStats.currentHealth(),
                    currentCombatStats.maxMana(),
                    currentCombatStats.currentMana(),
                    defaultStats.getDexterityModifier(),
                    rpgPlayer.getLevel());

            RPGPlayer resetPlayer = rpgPlayer
                    .updateStats(defaultStats)
                    .updateCombatStats(refreshedCombatStats)
                    .withUnspentStatPoints(totalEarned);

            playerManager.savePlayer(resetPlayer);

            sender.sendMessage(Component.text(
                    "✔ Atributos de " + target.getName() + " reiniciados. " + totalEarned
                            + " punto(s) de estadística disponibles para reasignar.",
                    NamedTextColor.GREEN));

            target.sendMessage(Component.text(
                    "Un administrador reinició tus atributos. Tienes " + totalEarned
                            + " punto(s) para reasignar con /rpg allocate.",
                    NamedTextColor.YELLOW));

        } catch (Exception exception) {
            sender.sendMessage(Component.text("Error al reiniciar los atributos.", NamedTextColor.RED));
            plugin.getLogger().severe("✘ Error en /rpg admin resetstats: " + exception.getMessage());
        }

    }

    @Override
    public String getName() {
        return "resetstats";
    }

    @Override
    public String getDescription() {
        return "Reinicia los atributos de un jugador y le devuelve todos sus puntos (admin)";
    }

    @Override
    public String getUsage() {
        return "/rpg resetstats <jugador>";
    }

    @Override
    public String getPermission() {
        return "rpgroll.admin.resetstats";
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
            return com.sack.rpgroll.util.TabCompleteUtil.allOnlinePlayerNames();
        }
        return List.of();
    }

}
