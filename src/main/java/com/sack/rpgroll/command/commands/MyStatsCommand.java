package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.RPGCommand;
import com.sack.rpgroll.gameplay.combat.CombatStats;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;
import com.sack.rpgroll.player.stats.PlayerStats;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

/**
 * Comando para ver estadísticas detalladas del jugador.
 * Uso: /rpg mystats
 */
public class MyStatsCommand implements RPGCommand {

    private final RPGRoll plugin;

    public MyStatsCommand(RPGRoll plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

        Player player = (Player) sender;

        try {

            PlayerManager playerManager = plugin.getBootstrap()
                    .getServices()
                    .get(PlayerManager.class);

            Optional<RPGPlayer> rpgPlayer = playerManager.getPlayer(player.getUniqueId());

            if (rpgPlayer.isEmpty()) {
                player.sendMessage(ChatColor.RED + "Error al cargar tus datos.");
                return;
            }

            displayDetailedStats(player, rpgPlayer.get());

        } catch (Exception exception) {

            player.sendMessage(ChatColor.RED + "Error al cargar estadísticas.");
            exception.printStackTrace();

        }

    }

    private void displayDetailedStats(Player player, RPGPlayer rpgPlayer) {

        PlayerStats stats = rpgPlayer.getStats();
        CombatStats combatStats = rpgPlayer.getCombatStats();

        player.sendMessage("");
        player.sendMessage(ChatColor.GOLD + "╔════════════════════════════════╗");
        player.sendMessage(ChatColor.YELLOW + "╠        " + ChatColor.BOLD + "MIS ESTADÍSTICAS" + ChatColor.RESET
                + ChatColor.YELLOW + "        ╣");
        player.sendMessage(ChatColor.GOLD + "╠════════════════════════════════╣");

        // Información básica
        player.sendMessage(ChatColor.AQUA + "╠ Nivel: " + ChatColor.WHITE + rpgPlayer.getLevel());
        player.sendMessage(ChatColor.AQUA + "╠ Raza: " + ChatColor.WHITE + rpgPlayer.getRace());
        player.sendMessage(ChatColor.AQUA + "╠ Clase: " + ChatColor.WHITE + rpgPlayer.getPlayerClass());

        player.sendMessage(ChatColor.GOLD + "╠════════════════════════════════╣");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "╠ ATRIBUTOS D&D");

        // Stats D&D
        player.sendMessage(ChatColor.RED + "╠ Fuerza: " + ChatColor.WHITE + stats.strength());
        player.sendMessage(ChatColor.GREEN + "╠ Destreza: " + ChatColor.WHITE + stats.dexterity());
        player.sendMessage(ChatColor.GOLD + "╠ Constitución: " + ChatColor.WHITE + stats.constitution());
        player.sendMessage(ChatColor.BLUE + "╠ Inteligencia: " + ChatColor.WHITE + stats.intelligence());
        player.sendMessage(ChatColor.AQUA + "╠ Sabiduría: " + ChatColor.WHITE + stats.wisdom());
        player.sendMessage(ChatColor.LIGHT_PURPLE + "╠ Carisma: " + ChatColor.WHITE + stats.charisma());

        player.sendMessage(ChatColor.GOLD + "╠════════════════════════════════╣");
        player.sendMessage(ChatColor.LIGHT_PURPLE + "╠ ESTADÍSTICAS DE COMBATE");

        // Combat Stats
        player.sendMessage(ChatColor.RED + "╠ Salud: " + ChatColor.WHITE +
                combatStats.currentHealth() + "/" + combatStats.maxHealth());
        player.sendMessage(ChatColor.BLUE + "╠ Maná: " + ChatColor.WHITE +
                combatStats.currentMana() + "/" + combatStats.maxMana());
        player.sendMessage(ChatColor.GRAY + "╠ Armadura: " + ChatColor.WHITE +
                String.format("%.1f", combatStats.armorRating()));
        player.sendMessage(ChatColor.GRAY + "╠ Evasión: " + ChatColor.WHITE +
                String.format("%.1f%%", combatStats.evasionChance() * 100));
        player.sendMessage(ChatColor.YELLOW + "╠ Crítico: " + ChatColor.WHITE +
                String.format("%.1f%%", combatStats.criticalChance() * 100));

        player.sendMessage(ChatColor.GOLD + "╚════════════════════════════════╝");
        player.sendMessage("");

    }

    @Override
    public String getName() {
        return "mystats";
    }

    @Override
    public String getDescription() {
        return "Muestra tus estadísticas detalladas";
    }

    @Override
    public String getUsage() {
        return "/rpg mystats";
    }

    @Override
    public List<String> getAliases() {
        return List.of("stats", "detailed", "detalles");
    }

}
