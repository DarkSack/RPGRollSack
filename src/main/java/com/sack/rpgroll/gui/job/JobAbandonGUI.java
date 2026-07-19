package com.sack.rpgroll.gui.job;

import com.sack.rpgroll.gameplay.job.Job;
import com.sack.rpgroll.gameplay.job.JobManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;
import com.sack.rpgroll.player.jobs.JobProgress;
import com.sack.rpgroll.player.jobs.PlayerJobs;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private final Map<Integer, String> slotToJob;

    public JobAbandonGUI(Player player, JobManager jobManager, PlayerManager playerManager, String pendingJobId) {
        super(player,
                Component.text("Ya tienes 3 trabajos", NamedTextColor.RED).decorate(TextDecoration.BOLD),
                27);
        this.jobManager = jobManager;
        this.playerManager = playerManager;
        this.pendingJobId = pendingJobId;
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
                .setName(Component.text("Elige cuál abandonar", NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
                .setLore(
                        Component.text("Para unirte a un nuevo trabajo", NamedTextColor.GRAY),
                        Component.text("primero debes dejar uno activo.", NamedTextColor.GRAY))
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

        setItem(CANCEL_SLOT, ItemBuilder.createCancelButton("Cancelar"));
    }

    private void addJob(int slot, Job job, JobProgress progress) {

        ItemStack item = ItemBuilder.skull(job.icon())
                .setName(Component.text(ChatColor.translateAlternateColorCodes('&', job.displayName()))
                        .decorate(TextDecoration.BOLD))
                .setLore(
                        Component.text("Nivel: " + progress.level(), NamedTextColor.YELLOW),
                        Component.text(""),
                        Component.text("Click para abandonar y unirte al nuevo trabajo", NamedTextColor.RED))
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
            player.sendMessage(Component.text("No te uniste a ningún trabajo nuevo.", NamedTextColor.YELLOW));
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
            player.sendMessage(Component.text("El trabajo ya no está disponible.", NamedTextColor.RED));
            return;
        }

        RPGPlayer updated = rpgPlayerOpt.get()
                .leaveJob(jobToLeave)
                .joinJob(pendingJobId);

        playerManager.savePlayer(updated);

        close();

        player.sendMessage(Component.text("Te has unido al trabajo: ", NamedTextColor.GREEN)
                .append(Component.text(newJobOpt.get().displayName(), NamedTextColor.GOLD)));
    }

}