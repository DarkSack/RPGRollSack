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
import org.bukkit.Input;
import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Otorga recompensas de trabajo "explorador": dinero/XP por descubrir un
 * bioma nuevo (una sola vez por bioma, para siempre), y por cada tramo de
 * distancia recorrida configurado (distance-blocks en el YAML).
 * <p>
 * Se dispara en PlayerMoveEvent, filtrando a solo cuando cambia de bloque
 * entero (no en cada micro-movimiento de cámara) para no sobrecargar. El
 * progreso vive en memoria mientras el jugador está conectado: antes cada
 * bloque recorrido hacía dos SELECT y un INSERT en SQLite en el hilo
 * principal. Se escribe al cobrar un tramo, al salir y al apagar.
 * <p>
 * La distancia solo cuenta en horizontal y cuando el jugador se mueve por
 * su cuenta (teclas de movimiento o planeando con élitros): en una corriente
 * de agua en bucle se podía cobrar sin tocar el teclado.
 */
public class ExplorerJobListener implements Listener {

    private static final String JOB_ID = "explorador";

    /** Más que esto en un solo evento no es caminar (un tirón de lag, un empujón): no cuenta. */
    private static final double MAX_STEP = 10.0;

    private final JobManager jobManager;
    private final PlayerManager playerManager;
    private final ExplorerProgressStorage explorerStorage;
    private final JobRewardService rewardService;
    private final Map<UUID, ExplorerProgress> progressCache = new HashMap<>();

    public ExplorerJobListener(JobManager jobManager, PlayerManager playerManager,
            ExplorerProgressStorage explorerStorage, JobRewardService rewardService) {
        this.jobManager = jobManager;
        this.playerManager = playerManager;
        this.explorerStorage = explorerStorage;
        this.rewardService = rewardService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
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
        UUID uuid = player.getUniqueId();
        ExplorerProgress progress = progressCache.computeIfAbsent(uuid, explorerStorage::load);

        progress = checkNewBiome(player, job, progress, to);
        progress = checkDistance(player, job, progress, from, to);
        progressCache.put(uuid, progress);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {

        ExplorerProgress progress = progressCache.remove(event.getPlayer().getUniqueId());

        if (progress != null) {
            explorerStorage.saveDistance(event.getPlayer().getUniqueId(), progress.distanceSinceLastPayout());
        }
    }

    /** Al apagar, antes de cerrar la base de datos: guarda lo recorrido desde el último cobro. */
    public void flushAll() {
        progressCache.forEach((uuid, progress) -> explorerStorage.saveDistance(uuid, progress.distanceSinceLastPayout()));
        progressCache.clear();
    }

    private ExplorerProgress checkNewBiome(Player player, Job job, ExplorerProgress progress, Location to) {

        if (job.newBiomeMoney() <= 0 && job.newBiomeExperience() <= 0) {
            return progress;
        }

        Biome biome = to.getBlock().getBiome();
        String biomeName = biome.getKey().getKey().toUpperCase();

        if (progress.hasVisited(biomeName)) {
            return progress;
        }

        explorerStorage.markBiomeVisited(player.getUniqueId(), biomeName);

        // Recompensa directa vía JobRewardService, usando el propio nombre
        // de bioma como "target" sintético — no requiere que esté en
        // rewards:, se paga el monto fijo new-biome-money/experience.
        rewardService.rewardDirect(player, JOB_ID, new JobReward(job.newBiomeMoney(), job.newBiomeExperience()));
        return progress.withNewBiome(biomeName);
    }

    private ExplorerProgress checkDistance(Player player, Job job, ExplorerProgress progress, Location from,
            Location to) {

        if (job.distanceBlocks() <= 0 || !movesOnItsOwn(player)) {
            return progress;
        }

        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double moved = Math.sqrt(dx * dx + dz * dz);

        if (moved > MAX_STEP) {
            return progress;
        }

        double accumulated = progress.distanceSinceLastPayout() + moved;

        if (accumulated < job.distanceBlocks()) {
            return progress.withDistance(accumulated);
        }

        double remainder = accumulated % job.distanceBlocks();
        explorerStorage.saveDistance(player.getUniqueId(), remainder);

        rewardService.rewardDirect(player, JOB_ID, new JobReward(job.distanceMoney(), job.distanceExperience()));
        return progress.withDistance(remainder);
    }

    private boolean movesOnItsOwn(Player player) {

        if (player.isInsideVehicle()) {
            return false;
        }

        if (player.isGliding()) {
            return true;
        }

        Input input = player.getCurrentInput();
        return input.isForward() || input.isBackward() || input.isLeft() || input.isRight();
    }

}
