package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.RPGCommand;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gameplay.combat.CombatStats;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Rellena la vida y el maná de un jugador a su máximo.
 * <p>
 * Existe porque no había ninguna forma de recargar maná: ni comando, ni ítem,
 * ni consola. Probar cualquier cosa que gaste maná —un hechizo caro, una
 * cadena de hechizos— obligaba a esperar la regeneración natural o a reiniciar,
 * y para un admin montando contenido eso es un tapón.
 * <p>
 * Uso: {@code /rpg restore <jugador> [vida|mana]} — sin el tercer argumento
 * rellena las dos.
 */
public class AdminRestoreCommand implements RPGCommand {

    private final RPGRoll plugin;

    public AdminRestoreCommand(RPGRoll plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

        LangManager lang = plugin.getBootstrap().getServices().get(LangManager.class);

        if (args.length < 1) {
            lang.send(sender, "admin_restore.usage", "usage", getUsage());
            return;
        }

        Player target = Bukkit.getPlayerExact(args[0]);

        if (target == null) {
            lang.send(sender, "error.player_offline", "player", args[0]);
            return;
        }

        String what = args.length > 1 ? args[1].toLowerCase(java.util.Locale.ROOT) : "todo";

        if (!what.equals("todo") && !what.equals("vida") && !what.equals("mana")) {
            lang.send(sender, "admin_restore.unknown_kind", "value", args[1]);
            return;
        }

        try {

            PlayerManager playerManager = plugin.getBootstrap().getServices().get(PlayerManager.class);
            var playerOpt = playerManager.getPlayer(target.getUniqueId());

            if (playerOpt.isEmpty()) {
                lang.send(sender, "error.player_offline", "player", args[0]);
                return;
            }

            RPGPlayer rpgPlayer = playerOpt.get();

            CombatStats current = rpgPlayer.getCombatStats();

            int health = what.equals("mana") ? current.currentHealth() : current.maxHealth();
            int mana = what.equals("vida") ? current.currentMana() : current.maxMana();

            CombatStats restored = CombatStats.of(
                    current.maxHealth(), health,
                    current.maxMana(), mana,
                    rpgPlayer.getStats().getDexterityModifier(), rpgPlayer.getLevel());

            playerManager.savePlayer(rpgPlayer.updateCombatStats(restored));

            lang.send(sender, "admin_restore.success",
                    "player", target.getName(), "health", health, "mana", mana);

            lang.send(target, "admin_restore.notify_target", "health", health, "mana", mana);

        } catch (Exception exception) {
            lang.send(sender, "admin_restore.error");
            plugin.getLogger().severe("✘ Error en /rpg restore: " + exception.getMessage());
        }
    }

    @Override
    public String getName() {
        return "restore";
    }

    @Override
    public List<String> getAliases() {
        return List.of("rellenar", "refill");
    }

    @Override
    public String getDescription() {
        return "Rellena la vida y el maná de un jugador a su máximo";
    }

    @Override
    public String getUsage() {
        return "/rpg restore <jugador> [vida|mana]";
    }

    @Override
    public String getPermission() {
        return "rpgroll.admin.restore";
    }

    @Override
    public boolean isPlayerOnly() {
        return false;
    }

}
