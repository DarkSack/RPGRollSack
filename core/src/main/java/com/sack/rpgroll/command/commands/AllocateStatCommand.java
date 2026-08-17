package com.sack.rpgroll.command.commands;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.command.RPGCommand;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gameplay.combat.CombatStats;
import com.sack.rpgroll.gameplay.stats.StatPointAllocator;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;
import com.sack.rpgroll.player.stats.PlayerStats;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

/**
 * Comando para gastar los puntos de estadística ganados al subir de nivel.
 * Uso: /rpg allocate <atributo> <cantidad>
 * <p>
 * Constitución e Inteligencia también aumentan la salud/maná máximos si el
 * punto invertido sube el modificador correspondiente (ver
 * {@link com.sack.rpgroll.gameplay.combat.CombatStats}); Destreza refresca
 * evasión/crítico de la misma forma.
 */
public class AllocateStatCommand implements RPGCommand {

    private final RPGRoll plugin;

    public AllocateStatCommand(RPGRoll plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {

        Player player = (Player) sender;
        LangManager lang = plugin.getBootstrap().getServices().get(LangManager.class);

        if (args.length < 2) {
            lang.send(player, "allocate_stat_command.usage", "usage", getUsage());
            return;
        }

        String statInput = args[0];
        int amount;

        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException exception) {
            lang.send(player, "allocate_stat_command.invalid_amount");
            return;
        }

        if (amount <= 0) {
            lang.send(player, "allocate_stat_command.must_be_positive");
            return;
        }

        try {

            PlayerManager playerManager = plugin.getBootstrap().getServices().get(PlayerManager.class);
            Optional<RPGPlayer> rpgPlayerOpt = playerManager.getPlayer(player.getUniqueId());

            if (rpgPlayerOpt.isEmpty()) {
                lang.send(player, "error.load_data");
                return;
            }

            RPGPlayer rpgPlayer = rpgPlayerOpt.get();
            int available = rpgPlayer.getUnspentStatPoints();

            if (available <= 0) {
                lang.send(player, "stats.no_points");
                return;
            }

            PlayerStats statsBefore = rpgPlayer.getStats();

            StatPointAllocator allocator = new StatPointAllocator(rpgPlayer, available);

            if (!allocator.allocate(statInput, amount)) {
                lang.send(player, "allocate_stat_command.invalid_allocation",
                        "available", available, "max", PlayerStats.MAX_STAT);
                return;
            }

            RPGPlayer afterAllocation = allocator.getPlayer();
            PlayerStats statsAfter = afterAllocation.getStats();

            CombatStats combatStats = afterAllocation.getCombatStats();

            int healthDelta = (statsAfter.getConstitutionModifier() - statsBefore.getConstitutionModifier()) * 5;
            int manaDelta = (statsAfter.getIntelligenceModifier() - statsBefore.getIntelligenceModifier()) * 5;

            if (healthDelta > 0) {
                combatStats = combatStats.growHealth(healthDelta);
            }
            if (manaDelta > 0) {
                combatStats = combatStats.growMana(manaDelta);
            }

            // Refresca armorRating/evasionChance/criticalChance con la destreza
            // actual, preservando la salud/maná ya calculados arriba.
            combatStats = CombatStats.of(
                    combatStats.maxHealth(),
                    combatStats.currentHealth(),
                    combatStats.maxMana(),
                    combatStats.currentMana(),
                    statsAfter.getDexterityModifier(),
                    afterAllocation.getLevel());

            RPGPlayer updatedPlayer = afterAllocation
                    .updateCombatStats(combatStats)
                    .spendStatPoints(amount);

            playerManager.savePlayer(updatedPlayer);

            lang.send(player, "allocate_stat_command.success", "amount", amount, "stat", statInput.toUpperCase());
            lang.send(player, "allocate_stat_command.remaining", "points", updatedPlayer.getUnspentStatPoints());

        } catch (Exception exception) {

            lang.send(player, "allocate_stat_command.error");
            exception.printStackTrace();

        }

    }

    @Override
    public String getName() {
        return "allocate";
    }

    @Override
    public String getDescription() {
        return "Gasta tus puntos de estadística disponibles";
    }

    @Override
    public String getUsage() {
        return "/rpg allocate <fuerza|destreza|constitucion|inteligencia|sabiduria|carisma> <cantidad>";
    }

    @Override
    public String getPermission() {
        return "rpgroll.player.allocate";
    }

    @Override
    public List<String> getAliases() {
        return List.of("asignar", "gastar");
    }

    @Override
    public List<String> getTabCompletions(CommandSender sender, String[] args) {
        if (args.length <= 1) {
            return List.of("fuerza", "destreza", "constitucion", "inteligencia", "sabiduria", "carisma");
        }
        if (args.length == 2) {
            return List.of("1", "2", "3", "5");
        }
        return List.of();
    }

}
