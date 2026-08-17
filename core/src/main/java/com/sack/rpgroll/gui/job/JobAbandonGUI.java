package com.sack.rpgroll.gui.job;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gameplay.job.Job;
import com.sack.rpgroll.gameplay.job.JobManager;
import com.sack.rpgroll.gui.InventoryGUI;
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

    private static final int CANCEL_SLOT = 22;

    private final JobManager jobManager;
    private final PlayerManager playerManager;
    private final String pendingJobId;
    private final LangManager lang;
    private final Map<Integer, String> slotToJob;

    public JobAbandonGUI(Player player, JobManager jobManager, PlayerManager playerManager, String pendingJobId,
            LangManager lang) {
        super(player,
                lang.component("job_abandon_gui.title").decorate(TextDecoration.BOLD),
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
            setItem(i, ItemBuilder.createFiller());
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
            setItem(i, ItemBuilder.createFiller());
        }

        setItem(CANCEL_SLOT, ItemBuilder.createCancelButton(lang.raw("job_abandon_gui.cancel_button")));
    }

    private void addJob(int slot, Job job, JobProgress progress) {

        ItemStack item = ItemBuilder.skull(job.icon())
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

        Optional<RPGPlayer> rpgPlayerOpt = playerManager.getPlayer(player.getUniqueId());
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

        RPGPlayer updated = rpgPlayerOpt.get()
                .leaveJob(jobToLeave)
                .joinJob(pendingJobId);

        playerManager.savePlayer(updated);

        close();

        lang.send(player, "job_abandon_gui.joined", "job", newJobOpt.get().displayName());
    }

}
