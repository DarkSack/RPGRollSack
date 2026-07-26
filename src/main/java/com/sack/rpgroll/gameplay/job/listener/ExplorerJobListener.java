package com.sack.rpgroll.gameplay.job.listener;

import com.sack.rpgroll.gameplay.job.ExplorerProgressStorage;
import com.sack.rpgroll.gameplay.job.Job;
import com.sack.rpgroll.gameplay.job.JobManager;
import com.sack.rpgroll.gameplay.job.JobReward;
import com.sack.rpgroll.gameplay.job.JobRewardService;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;
import com.sack.rpgroll.player.jobs.ExplorerProgress;
import com.sack.rpgroll.player.jobs.PlayerJobs;

import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.Optional;

/**
 * Otorga recompensas de trabajo "explorador": dinero/XP por descubrir un
 * bioma nuevo (una sola vez por bioma, para siempre), y por cada tramo de
 * distancia recorrida configurado (distance-blocks en el YAML).
 * <p>
 * Se dispara en PlayerMoveEvent, filtrando a solo cuando cambia de bloque
 * entero (no en cada micro-movimiento de cámara) para no sobrecargar.
 */
public class ExplorerJobListener implements Listener {

    private static final String JOB_ID = "explorador";

    private final JobManager jobManager;
    private final PlayerManager playerManager;
    private final ExplorerProgressStorage explorerStorage;
    private final JobRewardService rewardService;

    public ExplorerJobListener(JobManager jobManager, PlayerManager playerManager,
            ExplorerProgressStorage explorerStorage, JobRewardService rewardService) {
        this.jobManager = jobManager;
        this.playerManager = playerManager;
        this.explorerStorage = explorerStorage;
        this.rewardService = rewardService;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {

        Location from = event.getFrom();
        Location to = event.getTo();

        if (to == null) {
            return;
        }

        // Filtrar micro-movimientos (solo procesar si cambió de bloque)
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY()
                && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();

        Optional<RPGPlayer> rpgPlayerOpt = playerManager.getPlayer(player.getUniqueId());
        if (rpgPlayerOpt.isEmpty()) {
            return;
        }

        PlayerJobs playerJobs = rpgPlayerOpt.get().getJobs();

        if (!playerJobs.hasJob(JOB_ID)) {
            return;
        }

        Optional<Job> jobOpt = jobManager.get(JOB_ID);
        if (jobOpt.isEmpty() || !jobOpt.get().hasExplorationRewards()) {
            return;
        }

        Job job = jobOpt.get();
        ExplorerProgress progress = explorerStorage.load(player.getUniqueId());

        checkNewBiome(player, job, progress, to);
        checkDistance(player, job, progress, from, to);
    }

    private void checkNewBiome(Player player, Job job, ExplorerProgress progress, Location to) {

        if (job.newBiomeMoney() <= 0 && job.newBiomeExperience() <= 0) {
            return;
        }

        Biome biome = to.getBlock().getBiome();
        String biomeName = biome.getKey().getKey().toUpperCase();

        if (progress.hasVisited(biomeName)) {
            return;
        }

        explorerStorage.markBiomeVisited(player.getUniqueId(), biomeName);

        // Recompensa directa vía JobRewardService, usando el propio nombre
        // de bioma como "target" sintético — no requiere que esté en
        // rewards:, se paga el monto fijo new-biome-money/experience.
        rewardService.rewardDirect(player, JOB_ID, new JobReward(job.newBiomeMoney(), job.newBiomeExperience()));
    }

    private void checkDistance(Player player, Job job, ExplorerProgress progress, Location from, Location to) {

        if (job.distanceBlocks() <= 0) {
            return;
        }

        double moved = from.distance(to);
        double accumulated = progress.distanceSinceLastPayout() + moved;

        if (accumulated < job.distanceBlocks()) {
            explorerStorage.saveDistance(player.getUniqueId(), accumulated);
            return;
        }

        double remainder = accumulated % job.distanceBlocks();
        explorerStorage.saveDistance(player.getUniqueId(), remainder);

        rewardService.rewardDirect(player, JOB_ID, new JobReward(job.distanceMoney(), job.distanceExperience()));
    }

}