package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.RPGCommand;
import com.sack.rpgroll.gameplay.combat.CombatStats;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;
import com.sack.rpgroll.player.stats.PlayerStats;
import com.sack.rpgroll.util.MessageUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

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
                                player.sendMessage(Component.text("No se encontraron estadísticas para tu personaje.",
                                                NamedTextColor.RED));
                                player.sendMessage(Component.text("Error al cargar tus datos.", NamedTextColor.RED));
                                return;
                        }

                        displayDetailedStats(player, rpgPlayer.get());

                } catch (Exception exception) {

                        player.sendMessage(Component.text("Error al cargar estadísticas.", NamedTextColor.RED));
                        exception.printStackTrace();

                }

        }

        private void displayDetailedStats(Player player, RPGPlayer rpgPlayer) {

                PlayerStats stats = rpgPlayer.getStats();
                CombatStats combatStats = rpgPlayer.getCombatStats();

                player.sendMessage(MessageUtil.blank());

                player.sendMessage(MessageUtil.top());
                player.sendMessage(MessageUtil.title("MIS ESTADÍSTICAS"));
                player.sendMessage(MessageUtil.separator());

                // Información básica

                player.sendMessage(MessageUtil.line(
                                NamedTextColor.AQUA,
                                "Nivel",
                                rpgPlayer.getLevel()));

                player.sendMessage(MessageUtil.line(
                                NamedTextColor.AQUA,
                                "Raza",
                                rpgPlayer.getRace()));

                player.sendMessage(MessageUtil.line(
                                NamedTextColor.AQUA,
                                "Clase",
                                rpgPlayer.getPlayerClass()));

                player.sendMessage(MessageUtil.separator());

                if (rpgPlayer.getUnspentStatPoints() > 0) {
                        player.sendMessage(MessageUtil.line(
                                        NamedTextColor.GREEN,
                                        "Puntos sin gastar",
                                        rpgPlayer.getUnspentStatPoints() + " (usa /rpg allocate <atributo> <cantidad>)"));
                        player.sendMessage(MessageUtil.separator());
                }

                player.sendMessage(MessageUtil.section("ATRIBUTOS D&D"));

                player.sendMessage(MessageUtil.line(NamedTextColor.RED,
                                "Fuerza",
                                stats.strength()));

                player.sendMessage(MessageUtil.line(NamedTextColor.GREEN,
                                "Destreza",
                                stats.dexterity()));

                player.sendMessage(MessageUtil.line(NamedTextColor.GOLD,
                                "Constitución",
                                stats.constitution()));

                player.sendMessage(MessageUtil.line(NamedTextColor.BLUE,
                                "Inteligencia",
                                stats.intelligence()));

                player.sendMessage(MessageUtil.line(NamedTextColor.AQUA,
                                "Sabiduría",
                                stats.wisdom()));

                player.sendMessage(MessageUtil.line(NamedTextColor.LIGHT_PURPLE,
                                "Carisma",
                                stats.charisma()));

                player.sendMessage(MessageUtil.separator());

                player.sendMessage(MessageUtil.section("ESTADÍSTICAS DE COMBATE"));

                player.sendMessage(MessageUtil.line(
                                NamedTextColor.RED,
                                "Salud",
                                combatStats.currentHealth() + "/" + combatStats.maxHealth()));

                player.sendMessage(MessageUtil.line(
                                NamedTextColor.BLUE,
                                "Maná",
                                combatStats.currentMana() + "/" + combatStats.maxMana()));

                player.sendMessage(MessageUtil.line(
                                NamedTextColor.GRAY,
                                "Armadura",
                                String.format("%.1f", combatStats.armorRating())));

                player.sendMessage(MessageUtil.line(
                                NamedTextColor.GRAY,
                                "Evasión",
                                String.format("%.1f%%", combatStats.evasionChance() * 100)));

                player.sendMessage(MessageUtil.line(
                                NamedTextColor.YELLOW,
                                "Crítico",
                                String.format("%.1f%%", combatStats.criticalChance() * 100)));

                player.sendMessage(MessageUtil.bottom());

                player.sendMessage(MessageUtil.blank());
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
                return List.of("detailed", "detalles");
        }

}
