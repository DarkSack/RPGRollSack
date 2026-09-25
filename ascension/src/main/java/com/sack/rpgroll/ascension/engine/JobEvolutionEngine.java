package com.sack.rpgroll.ascension.engine;

import com.sack.rpgroll.api.RPGRollAPI;
import com.sack.rpgroll.ascension.deferred.JobEvolution;
import com.sack.rpgroll.ascension.deferred.JobEvolutionManager;
import com.sack.rpgroll.ascension.player.AscensionPlayerState;
import com.sack.rpgroll.ascension.player.AscensionPlayerStateManager;
import com.sack.rpgroll.ascension.reward.Rewards;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.player.RPGPlayer;
import com.sack.rpgroll.player.jobs.PlayerJobs;

import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rangos de oficio. Al llegar al nivel de una evolución se obtiene para
 * siempre: se entregan sus herramientas y recompensas una vez, y mientras el
 * jugador siga en ese oficio tiene estos permisos:
 * <ul>
 *   <li>{@code rpgrollascension.jobevolution.<id>}</li>
 *   <li>{@code rpgrollascension.recipe.<id>} por cada {@code unlocked-recipes}</li>
 *   <li>{@code rpgrollascension.quest.<id>} por cada {@code unlocked-quests}</li>
 * </ul>
 * Una receta de RPGRoll-Crafting los exige con una condición
 * {@code PERMISSION}. Si deja el oficio, los permisos se van; si vuelve,
 * regresan sin repetir las recompensas.
 */
public class JobEvolutionEngine {

    public static final String PERMISSION_PREFIX = "rpgrollascension.";

    private final Plugin plugin;
    private final JobEvolutionManager jobEvolutionManager;
    private final AscensionPlayerStateManager stateManager;
    private final RewardService rewardService;
    private final LangManager lang;
    private final Map<UUID, PermissionAttachment> attachments = new ConcurrentHashMap<>();

    public JobEvolutionEngine(Plugin plugin, JobEvolutionManager jobEvolutionManager,
            AscensionPlayerStateManager stateManager, RewardService rewardService, LangManager lang) {
        this.plugin = plugin;
        this.jobEvolutionManager = jobEvolutionManager;
        this.stateManager = stateManager;
        this.rewardService = rewardService;
        this.lang = lang;
    }

    public JobEvolutionManager getJobEvolutionManager() {
        return jobEvolutionManager;
    }

    /** Concede las evoluciones alcanzadas y rehace los permisos. */
    public void check(Player player) {

        PlayerJobs jobs = jobs(player);

        if (jobs == null) {
            return;
        }

        AscensionPlayerState state = stateManager.getOrLoad(player);

        List<JobEvolution> ordered = new ArrayList<>(jobEvolutionManager.getAll());
        ordered.sort(Comparator.comparingInt(JobEvolution::requiredJobLevel));

        for (JobEvolution evolution : ordered) {

            if (state.getJobEvolutions().contains(evolution.id()) || !jobs.hasJob(evolution.baseJob())
                    || jobs.getLevel(evolution.baseJob()) < evolution.requiredJobLevel()) {
                continue;
            }

            state.addJobEvolution(evolution.id());
            lang.send(player, "progress.job_evolution", "name", evolution.displayName());

            List<String> items = new ArrayList<>(evolution.rewards().items());
            items.addAll(evolution.unlockedTools());
            Rewards rewards = evolution.rewards();
            rewardService.grant(player, new Rewards(rewards.money(), rewards.experience(), rewards.talentPoints(),
                    rewards.title(), rewards.reputation(), rewards.stats(), items, rewards.commands(),
                    rewards.message(), rewards.broadcast()), evolution.displayName());
        }

        refreshPermissions(player, jobs, state);
    }

    private void refreshPermissions(Player player, PlayerJobs jobs, AscensionPlayerState state) {

        Set<String> wanted = new HashSet<>();

        for (JobEvolution evolution : jobEvolutionManager.getAll()) {

            if (!state.getJobEvolutions().contains(evolution.id()) || !jobs.hasJob(evolution.baseJob())) {
                continue;
            }

            wanted.add(PERMISSION_PREFIX + "jobevolution." + normalize(evolution.id()));
            evolution.unlockedRecipes().forEach(recipe -> wanted.add(PERMISSION_PREFIX + "recipe." + normalize(recipe)));
            evolution.unlockedQuests().forEach(quest -> wanted.add(PERMISSION_PREFIX + "quest." + normalize(quest)));
        }

        PermissionAttachment attachment = attachments.get(player.getUniqueId());

        // Esto corre en cada revisión (cada 20 s), y cada setPermission hace
        // que Bukkit recalcule todos los permisos del jugador: solo se toca
        // si algo cambió.
        if (attachment != null && attachment.getPermissions().keySet().equals(wanted)) {
            return;
        }

        if (attachment == null) {
            if (wanted.isEmpty()) {
                return;
            }
            attachment = player.addAttachment(plugin);
            attachments.put(player.getUniqueId(), attachment);
        }

        for (String permission : List.copyOf(attachment.getPermissions().keySet())) {
            attachment.unsetPermission(permission);
        }

        for (String permission : wanted) {
            attachment.setPermission(permission, true);
        }
    }

    public void onQuit(Player player) {

        PermissionAttachment attachment = attachments.remove(player.getUniqueId());

        if (attachment != null) {
            player.removeAttachment(attachment);
        }
    }

    /** Evoluciones de un oficio, de menor a mayor nivel. */
    public List<JobEvolution> evolutionsOf(String jobId) {
        return jobEvolutionManager.getAll().stream()
                .filter(evolution -> evolution.baseJob().equalsIgnoreCase(jobId))
                .sorted(Comparator.comparingInt(JobEvolution::requiredJobLevel))
                .toList();
    }

    PlayerJobs jobs(Player player) {

        if (!RPGRollAPI.isReady()) {
            return null;
        }

        return RPGRollAPI.get().getPlayer(player.getUniqueId()).map(RPGPlayer::getJobs).orElse(null);
    }

    private static String normalize(String id) {
        return id.toLowerCase(Locale.ROOT);
    }

}
