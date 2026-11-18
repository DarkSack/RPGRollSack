package com.sack.rpgroll.gui.job;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gameplay.job.Job;
import com.sack.rpgroll.gameplay.job.JobManager;
import com.sack.rpgroll.gui.CoreMenus;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.confirm.ConfirmGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;
import com.sack.rpgroll.player.jobs.JobProgress;
import com.sack.rpgroll.player.jobs.PlayerJobs;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * GUI para elegir qué trabajo abandonar cuando el jugador ya tiene el
 * máximo de trabajos activos (3) e intenta unirse a uno nuevo.
 * Al elegir un trabajo, lo abandona y automáticamente une al pendingJobId.
 */
public class JobAbandonGUI extends InventoryGUI {

    /** Fondo dibujado (sackito:menus/trabajo_abandonar); el botón de cancelar (22) ya va dibujado. */
    public static final String BACKGROUND = "trabajo_abandonar";

    private static final int CANCEL_SLOT = 22;

    private final JobManager jobManager;
    private final PlayerManager playerManager;
    private final String pendingJobId;
    private final LangManager lang;
    private final Map<Integer, String> slotToJob;

    public JobAbandonGUI(Player player, JobManager jobManager, PlayerManager playerManager, String pendingJobId,
            LangManager lang) {
        super(player,
                CoreMenus.skin().title(BACKGROUND, lang.component("job_abandon_gui.title").decorate(TextDecoration.BOLD)),
                27);
        this.jobManager = jobManager;
        this.playerManager = playerManager;
        this.pendingJobId = pendingJobId;
        this.lang = lang;
        this.slotToJob = new HashMap<>();
    }

    @Override
    public void build() {

        clear();
        slotToJob.clear();

        for (int i = 0; i < 9; i++) {
            setItem(i, CoreMenus.filler());
        }

        setItem(4, new ItemBuilder(Material.PAPER)
                .setName(lang.component("job_abandon_gui.header_name").decorate(TextDecoration.BOLD))
                .setLore(lang.component("job_abandon_gui.header_lore_line1"),
                        lang.component("job_abandon_gui.header_lore_line2"))
                .build());

        Optional<RPGPlayer> rpgPlayerOpt = playerManager.getPlayer(player.getUniqueId());

        if (rpgPlayerOpt.isEmpty()) {
            return;
        }

        PlayerJobs playerJobs = rpgPlayerOpt.get().getJobs();

        int slot = 10;
        for (String jobId : playerJobs.getActiveJobIds()) {

            Optional<Job> jobOpt = jobManager.get(jobId);
            if (jobOpt.isEmpty()) {
                continue;
            }

            addJob(slot, jobOpt.get(), playerJobs.getProgress(jobId).orElseThrow());
            slot++;
        }

        for (int i = 18; i < 27; i++) {
            setItem(i, CoreMenus.filler());
        }

        setItem(CANCEL_SLOT, CoreMenus.skin().button(ItemBuilder.createCancelButton(lang.raw("job_abandon_gui.cancel_button"))));
    }

    private void addJob(int slot, Job job, JobProgress progress) {

        ItemStack item = ItemBuilder.icon(job.icon())
                .setName(ComponentUtils.parse(job.displayName())
                        .decorate(TextDecoration.BOLD))
                .setLore(
                        lang.component("job_abandon_gui.level_line", "level", progress.level()),
                        Component.text(""),
                        lang.component("job_abandon_gui.click_swap"))
                .build();

        setItem(slot, item);
        slotToJob.put(slot, job.id());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);

        int slot = event.getRawSlot();

        if (slot == CANCEL_SLOT) {
            close();
            lang.send(player, "job_abandon_gui.cancelled");
            return;
        }

        String jobToLeave = slotToJob.get(slot);
        if (jobToLeave == null) {
            return;
        }

        confirmSwap(jobToLeave);
    }

    /** Cambiar de oficio borra el nivel del que se deja: se confirma antes, como en JobsGUI. */
    private void confirmSwap(String jobToLeave) {

        Optional<Job> oldJob = jobManager.get(jobToLeave);
        String oldName = oldJob.map(Job::displayName).orElse(jobToLeave);
        String newName = jobManager.get(pendingJobId).map(Job::displayName).orElse(pendingJobId);
        int level = playerManager.getPlayer(player.getUniqueId())
                .flatMap(current -> current.getJobs().getProgress(jobToLeave))
                .map(JobProgress::level).orElse(1);

        ItemStack subject = oldJob.map(job -> ItemBuilder.icon(job.icon()))
                .orElseGet(() -> new ItemBuilder(Material.BARRIER))
                .setName(lang.component("job_abandon_gui.confirm_name", "old", oldName, "new", newName)
                        .decorate(TextDecoration.BOLD))
                .setLore(lang.component("jobs_gui.confirm_leave_lore1", "level", level),
                        lang.component("jobs_gui.confirm_leave_lore2"))
                .build();

        close();
        new ConfirmGUI(player, lang.component("jobs_gui.confirm_title").decorate(TextDecoration.BOLD), subject,
                lang.raw("job_abandon_gui.confirm_button", "old", oldName), lang.raw("jobs_gui.confirm_keep_button"),
                true,
                () -> swap(jobToLeave),
                () -> new JobAbandonGUI(player, jobManager, playerManager, pendingJobId, lang).open(),
                false).open();
    }

    private void swap(String jobToLeave) {

        Optional<RPGPlayer> rpgPlayerOpt = playerManager.getPlayer(player.getUniqueId())
                .filter(current -> current.getJobs().hasJob(jobToLeave));
        if (rpgPlayerOpt.isEmpty()) {
            close();
            return;
        }

        Optional<Job> newJobOpt = jobManager.get(pendingJobId);
        if (newJobOpt.isEmpty()) {
            close();
            lang.send(player, "job_abandon_gui.job_unavailable");
            return;
        }

        var joinEvent = new com.sack.rpgroll.api.event.PlayerJoinJobEvent(player, rpgPlayerOpt.get(), pendingJobId);
        org.bukkit.Bukkit.getPluginManager().callEvent(joinEvent);
        if (joinEvent.isCancelled()) {
            close();
            lang.send(player, "jobs_gui.join_denied", "job", newJobOpt.get().displayName());
            return;
        }

        RPGPlayer updated = rpgPlayerOpt.get()
                .leaveJob(jobToLeave)
                .joinJob(pendingJobId);

        playerManager.savePlayer(updated);
        org.bukkit.Bukkit.getPluginManager().callEvent(
                new com.sack.rpgroll.api.event.PlayerLeaveJobEvent(player, updated, jobToLeave));

        close();

        lang.send(player, "job_abandon_gui.joined", "job", newJobOpt.get().displayName());
    }

}
