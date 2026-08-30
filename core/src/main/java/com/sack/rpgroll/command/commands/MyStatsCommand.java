package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.api.playerclass.ClassManager;
import com.sack.rpgroll.api.playerclass.PlayerClass;
import com.sack.rpgroll.api.race.Race;
import com.sack.rpgroll.api.race.RaceManager;
import com.sack.rpgroll.command.RPGCommand;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gameplay.combat.CombatStats;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;
import com.sack.rpgroll.player.stats.PlayerStats;
import com.sack.rpgroll.util.MessageUtil;

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
                LangManager lang = plugin.getBootstrap().getServices().get(LangManager.class);

                try {

                        PlayerManager playerManager = plugin.getBootstrap()
                                        .getServices()
                                        .get(PlayerManager.class);

                        Optional<RPGPlayer> rpgPlayer = playerManager.getPlayer(player.getUniqueId());

                        if (rpgPlayer.isEmpty()) {
                                lang.send(player, "my_stats_command.profile_load_error");
                                lang.send(player, "my_stats_command.data_load_error");
                                return;
                        }

                        displayDetailedStats(player, rpgPlayer.get(), lang);

                } catch (Exception exception) {

                        lang.send(player, "my_stats_command.load_error");
                        exception.printStackTrace();

                }

        }

        private void displayDetailedStats(Player player, RPGPlayer rpgPlayer, LangManager lang) {

                PlayerStats stats = rpgPlayer.getStats();
                CombatStats combatStats = rpgPlayer.getCombatStats();

                RaceManager raceManager = plugin.getBootstrap().getServices().get(RaceManager.class);
                ClassManager classManager = plugin.getBootstrap().getServices().get(ClassManager.class);

                String raceDisplay = raceManager.get(rpgPlayer.getRace())
                                .map(Race::displayName)
                                .orElse(rpgPlayer.getRace());
                String classDisplay = classManager.get(rpgPlayer.getPlayerClass())
                                .map(PlayerClass::displayName)
                                .orElse(rpgPlayer.getPlayerClass());

                player.sendMessage(MessageUtil.blank());

                player.sendMessage(MessageUtil.top());
                player.sendMessage(MessageUtil.title(lang.raw("my_stats_command.title")));
                player.sendMessage(MessageUtil.separator());

                // Información básica

                player.sendMessage(MessageUtil.line(
                                NamedTextColor.AQUA,
                                lang.raw("my_stats_command.label_level"),
                                rpgPlayer.getLevel()));

                player.sendMessage(MessageUtil.line(
                                NamedTextColor.AQUA,
                                lang.raw("my_stats_command.label_race"),
                                raceDisplay));

                player.sendMessage(MessageUtil.line(
                                NamedTextColor.AQUA,
                                lang.raw("my_stats_command.label_class"),
                                classDisplay));

                player.sendMessage(MessageUtil.separator());

                if (rpgPlayer.getUnspentStatPoints() > 0) {
                        player.sendMessage(MessageUtil.line(
                                        NamedTextColor.GREEN,
                                        lang.raw("my_stats_command.label_unspent_points"),
                                        rpgPlayer.getUnspentStatPoints() + lang.raw("my_stats_command.unspent_points_hint")));
                        player.sendMessage(MessageUtil.separator());
                }

                player.sendMessage(MessageUtil.section(lang.raw("my_stats_command.section_attributes")));

                player.sendMessage(MessageUtil.line(NamedTextColor.RED,
                                lang.raw("my_stats_command.label_strength"),
                                stats.strength()));

                player.sendMessage(MessageUtil.line(NamedTextColor.GREEN,
                                lang.raw("my_stats_command.label_dexterity"),
                                stats.dexterity()));

                player.sendMessage(MessageUtil.line(NamedTextColor.GOLD,
                                lang.raw("my_stats_command.label_constitution"),
                                stats.constitution()));

                player.sendMessage(MessageUtil.line(NamedTextColor.BLUE,
                                lang.raw("my_stats_command.label_intelligence"),
                                stats.intelligence()));

                player.sendMessage(MessageUtil.line(NamedTextColor.AQUA,
                                lang.raw("my_stats_command.label_wisdom"),
                                stats.wisdom()));

                player.sendMessage(MessageUtil.line(NamedTextColor.LIGHT_PURPLE,
                                lang.raw("my_stats_command.label_charisma"),
                                stats.charisma()));

                player.sendMessage(MessageUtil.separator());

                player.sendMessage(MessageUtil.section(lang.raw("my_stats_command.section_combat")));

                player.sendMessage(MessageUtil.line(
                                NamedTextColor.RED,
                                lang.raw("my_stats_command.label_health"),
                                combatStats.currentHealth() + "/" + combatStats.maxHealth()));

                player.sendMessage(MessageUtil.line(
                                NamedTextColor.BLUE,
                                lang.raw("my_stats_command.label_mana"),
                                combatStats.currentMana() + "/" + combatStats.maxMana()));

                player.sendMessage(MessageUtil.line(
                                NamedTextColor.GRAY,
                                lang.raw("my_stats_command.label_armor"),
                                String.format("%.1f", combatStats.armorRating())));

                player.sendMessage(MessageUtil.line(
                                NamedTextColor.GRAY,
                                lang.raw("my_stats_command.label_evasion"),
                                String.format("%.1f%%", combatStats.evasionChance() * 100)));

                player.sendMessage(MessageUtil.line(
                                NamedTextColor.YELLOW,
                                lang.raw("my_stats_command.label_critical"),
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

        @Override
        public String getPermission() {
                return "rpgroll.player.mystats";
        }

}
