package com.sack.rpgroll.gameplay.job;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.integration.VaultEconomyProvider;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;
import com.sack.rpgroll.player.jobs.JobProgress;
import com.sack.rpgroll.player.jobs.PlayerJobs;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * Aplica la recompensa de una acción de trabajo: paga con Vault (si está
 * disponible), suma experiencia del trabajo, y procesa level up si corresponde.
 * Reutilizable desde cualquier listener de trabajo (Minero, Pescador, etc.).
 */
public class JobRewardService {

    private final RPGRoll plugin;
    private final PlayerManager playerManager;
    private final JobManager jobManager;
    private final VaultEconomyProvider economyProvider;

    public JobRewardService(RPGRoll plugin, PlayerManager playerManager, JobManager jobManager,
            VaultEconomyProvider economyProvider) {
        this.plugin = plugin;
        this.playerManager = playerManager;
        this.jobManager = jobManager;
        this.economyProvider = economyProvider;
    }

    /**
     * Procesa la recompensa de un trabajo para un target específico.
     * No hace nada si el jugador no tiene ese trabajo activo, o si el
     * trabajo no define recompensa para ese target.
     */
    public void reward(Player bukkitPlayer, String jobId, String target) {

        Optional<RPGPlayer> rpgPlayerOpt = playerManager.getPlayer(bukkitPlayer.getUniqueId());
        if (rpgPlayerOpt.isEmpty()) {
            return;
        }

        RPGPlayer rpgPlayer = rpgPlayerOpt.get();
        PlayerJobs playerJobs = rpgPlayer.getJobs();

        if (!playerJobs.hasJob(jobId)) {
            return;
        }

        Optional<Job> jobOpt = jobManager.get(jobId);
        if (jobOpt.isEmpty()) {
            return;
        }

        Job job = jobOpt.get();
        JobReward reward = job.getReward(target);

        if (reward == null) {
            return;
        }

        boolean paid = payReward(bukkitPlayer, reward);

        // Sumar experiencia y procesar level up
        JobProgress current = playerJobs.getProgress(jobId).orElse(JobProgress.start(jobId));
        JobProgress updated = applyExperience(job, current, reward.experience());

        boolean leveledUp = updated.level() > current.level();

        RPGPlayer updatedPlayer = rpgPlayer.updateJobs(playerJobs.withProgress(updated));
        playerManager.savePlayer(updatedPlayer);

        sendFeedback(bukkitPlayer, job, reward, paid);

        if (leveledUp) {
            bukkitPlayer.sendMessage(Component.text(
                    "🎉 " + job.displayName() + " subió a nivel " + updated.level() + "!", NamedTextColor.GOLD));
        }
    }

    /**
     * Igual que reward(), pero recibe la JobReward ya resuelta en vez de
     * buscarla por target en el catálogo. Usado por Explorador, cuyo pago
     * no depende de un target sino de eventos calculados (bioma nuevo, tramo
     * de distancia).
     */
    public void rewardDirect(Player bukkitPlayer, String jobId, JobReward reward) {

        Optional<RPGPlayer> rpgPlayerOpt = playerManager.getPlayer(bukkitPlayer.getUniqueId());
        if (rpgPlayerOpt.isEmpty()) {
            return;
        }

        RPGPlayer rpgPlayer = rpgPlayerOpt.get();
        PlayerJobs playerJobs = rpgPlayer.getJobs();

        if (!playerJobs.hasJob(jobId)) {
            return;
        }

        Optional<Job> jobOpt = jobManager.get(jobId);
        if (jobOpt.isEmpty()) {
            return;
        }

        Job job = jobOpt.get();

        boolean paid = payReward(bukkitPlayer, reward);

        JobProgress current = playerJobs.getProgress(jobId).orElse(JobProgress.start(jobId));
        JobProgress updated = applyExperience(job, current, reward.experience());

        boolean leveledUp = updated.level() > current.level();

        RPGPlayer updatedPlayer = rpgPlayer.updateJobs(playerJobs.withProgress(updated));
        playerManager.savePlayer(updatedPlayer);

        sendFeedback(bukkitPlayer, job, reward, paid);

        if (leveledUp) {
            bukkitPlayer.sendMessage(Component.text(
                    "🎉 " + job.displayName() + " subió a nivel " + updated.level() + "!", NamedTextColor.GOLD));
        }
    }

    /**
     * Paga la recompensa en dinero, si Vault/economía está disponible.
     *
     * @return true si se pagó efectivamente, false si no había economía activa
     *         o la recompensa no incluía dinero.
     */
    private boolean payReward(Player bukkitPlayer, JobReward reward) {

        if (reward.money() <= 0) {
            return false;
        }

        return economyProvider.getEconomy()
                .map(economy -> {
                    economy.depositPlayer(bukkitPlayer, reward.money());
                    return true;
                })
                .orElse(false);
    }

    private void sendFeedback(Player bukkitPlayer, Job job, JobReward reward, boolean paid) {

        StringBuilder message = new StringBuilder("+" + reward.experience() + " XP (" + job.displayName() + ")");

        if (paid) {
            message.append(" | +$").append(reward.money());
        }

        bukkitPlayer.sendActionBar(Component.text(message.toString(), NamedTextColor.GREEN));
    }

    private JobProgress applyExperience(Job job, JobProgress current, int expGained) {

        int newExp = current.experience() + expGained;
        int newLevel = current.level();

        if (newLevel >= job.maxLevel()) {
            return new JobProgress(current.jobId(), newLevel, newExp);
        }

        int expRequired = job.getExpRequiredForLevel(newLevel + 1);

        while (newExp >= expRequired && newLevel < job.maxLevel()) {
            newLevel++;
            expRequired = job.getExpRequiredForLevel(newLevel + 1);
        }

        return new JobProgress(current.jobId(), newLevel, newExp);
    }

}