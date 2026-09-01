package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.RPGCommand;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gameplay.combat.CombatStats;
import com.sack.rpgroll.gameplay.levelup.LevelUpRewardsConfig;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;
import com.sack.rpgroll.player.stats.PlayerStats;

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

        LangManager lang = plugin.getBootstrap().getServices().get(LangManager.class);

        if (args.length < 1) {
            lang.send(sender, "admin_resetstats.usage", "usage", getUsage());
            return;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayerExact(targetName);

        if (target == null) {
            lang.send(sender, "error.player_offline", "player", targetName);
            return;
        }

        try {

            PlayerManager playerManager = plugin.getBootstrap().getServices().get(PlayerManager.class);
            LevelUpRewardsConfig rewardsConfig = plugin.getBootstrap().getServices().get(LevelUpRewardsConfig.class);

            Optional<RPGPlayer> rpgPlayerOpt = playerManager.getPlayer(target.getUniqueId());

            if (rpgPlayerOpt.isEmpty()) {
                lang.send(sender, "error.no_rpg_data");
                return;
            }

            RPGPlayer rpgPlayer = rpgPlayerOpt.get();

            int totalEarned = 0;
            for (int level = 2; level <= rpgPlayer.getLevel(); level++) {
                totalEarned += rewardsConfig.getRewards(level)
                        .map(rewards -> rewards.statPoints())
                        .orElse(0);
            }

            // Antes esto era PlayerStats.createDefault() a secas, que dejaba al
            // jugador en 10 planos y le borraba los bonos de su raza y su clase
            // para siempre. Reiniciar los puntos gastados no debería quitarle lo
            // que es innato a su personaje.
            var services = plugin.getBootstrap().getServices();
            PlayerStats defaultStats = com.sack.rpgroll.player.stats.BaseStats.forRaceAndClass(
                    rpgPlayer.getRace(), rpgPlayer.getPlayerClass(),
                    services.get(com.sack.rpgroll.api.race.RaceManager.class),
                    services.get(com.sack.rpgroll.api.playerclass.ClassManager.class));

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

            lang.send(sender, "admin_resetstats.success", "player", target.getName(), "points", totalEarned);

            lang.send(target, "admin_resetstats.notify_target", "points", totalEarned);

        } catch (Exception exception) {
            lang.send(sender, "admin_resetstats.error");
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
