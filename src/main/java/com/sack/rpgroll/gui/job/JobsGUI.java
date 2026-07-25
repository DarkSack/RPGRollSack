package com.sack.rpgroll.gui.job;

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
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

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
 * GUI principal de trabajos: muestra el catálogo completo (JobManager) y
 * el estado de cada uno para el jugador (activo con nivel/XP, o disponible
 * para unirse). Click en un trabajo activo lo abandona; click en uno
 * inactivo lo une, o abre JobAbandonGUI si ya está en el máximo (3).
 */
public class JobsGUI extends InventoryGUI {

    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25
    };

    private final JobManager jobManager;
    private final PlayerManager playerManager;
    private final Map<Integer, String> slotToJob;

    public JobsGUI(Player player, JobManager jobManager, PlayerManager playerManager) {
        super(player, Component.text("Trabajos", NamedTextColor.GOLD).decorate(TextDecoration.BOLD), 45);
        this.jobManager = jobManager;
        this.playerManager = playerManager;
        this.slotToJob = new HashMap<>();
    }

    @Override
    public void build() {

        clear();
        slotToJob.clear();

        for (int i = 0; i < 9; i++) {
            setItem(i, ItemBuilder.createFiller());
        }
        for (int i = 36; i < 45; i++) {
            setItem(i, ItemBuilder.createFiller());
        }

        Optional<RPGPlayer> rpgPlayerOpt = playerManager.getPlayer(player.getUniqueId());

        if (rpgPlayerOpt.isEmpty()) {
            setItem(22, new ItemBuilder(Material.BARRIER)
                    .setName(Component.text("Error al cargar tus datos", NamedTextColor.RED))
                    .build());
            return;
        }

        PlayerJobs playerJobs = rpgPlayerOpt.get().getJobs();

        // Info general en la parte superior
        setItem(4, new ItemBuilder(Material.BOOK)
                .setName(Component.text("Tus trabajos", NamedTextColor.GOLD).decorate(TextDecoration.BOLD))
                .setLore(
                        Component.text("Activos: " + playerJobs.count() + "/" + PlayerJobs.MAX_ACTIVE_JOBS,
                                NamedTextColor.GRAY))
                .build());

        List<Job> jobs = new ArrayList<>(jobManager.getAll());

        if (jobs.isEmpty()) {
            setItem(22, new ItemBuilder(Material.BARRIER)
                    .setName(Component.text("Sin trabajos disponibles", NamedTextColor.RED))
                    .build());
            return;
        }

        for (int i = 0; i < jobs.size() && i < CONTENT_SLOTS.length; i++) {
            addJob(CONTENT_SLOTS[i], jobs.get(i), playerJobs);
        }
    }

    private void addJob(int slot, Job job, PlayerJobs playerJobs) {

        boolean active = playerJobs.hasJob(job.id());
        List<Component> lore = new ArrayList<>();

        if (!job.description().isEmpty()) {
            for (String line : job.description().split("\n")) {
                lore.add(ComponentUtils.parse(line).color(NamedTextColor.GRAY));
            }
        }

        lore.add(Component.text(""));

        if (active) {

            JobProgress progress = playerJobs.getProgress(job.id()).orElseThrow();
            int expRequired = job.getExpRequiredForLevel(progress.level() + 1);

            lore.add(Component.text("● Activo", NamedTextColor.GREEN).decorate(TextDecoration.BOLD));
            lore.add(Component.text("Nivel: " + progress.level() + "/" + job.maxLevel(), NamedTextColor.YELLOW));

            if (progress.level() < job.maxLevel()) {
                lore.add(Component.text("XP: " + progress.experience() + "/" + expRequired, NamedTextColor.YELLOW));
            } else {
                lore.add(Component.text("¡Nivel máximo!", NamedTextColor.GOLD));
            }

            lore.add(Component.text(""));
            lore.add(Component.text("Click para abandonar", NamedTextColor.RED));

        } else {

            lore.add(Component.text("○ Disponible", NamedTextColor.GRAY));
            lore.add(Component.text(""));
            lore.add(Component.text("Click para unirte", NamedTextColor.GREEN));

        }

        ItemStack item = ItemBuilder.skull(job.icon())
                .setName(ComponentUtils.parse(job.displayName()).decorate(TextDecoration.BOLD))
                .setLore(lore)
                .build();

        setItem(slot, item);
        slotToJob.put(slot, job.id());
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);

        int slot = event.getRawSlot();
        String jobId = slotToJob.get(slot);

        if (jobId == null) {
            return;
        }

        Optional<RPGPlayer> rpgPlayerOpt = playerManager.getPlayer(player.getUniqueId());
        if (rpgPlayerOpt.isEmpty()) {
            return;
        }

        RPGPlayer rpgPlayer = rpgPlayerOpt.get();
        PlayerJobs playerJobs = rpgPlayer.getJobs();

        if (playerJobs.hasJob(jobId)) {
            leaveJob(rpgPlayer, jobId);
            return;
        }

        if (playerJobs.isFull()) {
            close();
            JobAbandonGUI abandonGUI = new JobAbandonGUI(player, jobManager, playerManager, jobId);
            abandonGUI.open();
            return;
        }

        joinJob(rpgPlayer, jobId);
    }

    private void joinJob(RPGPlayer rpgPlayer, String jobId) {

        Optional<Job> jobOpt = jobManager.get(jobId);
        if (jobOpt.isEmpty()) {
            return;
        }

        RPGPlayer updated = rpgPlayer.joinJob(jobId);
        playerManager.savePlayer(updated);

        player.sendMessage(Component.text("Te has unido al trabajo: ", NamedTextColor.GREEN)
                .append(Component.text(jobOpt.get().displayName(), NamedTextColor.GOLD)));

        build();
    }

    private void leaveJob(RPGPlayer rpgPlayer, String jobId) {

        Optional<Job> jobOpt = jobManager.get(jobId);

        RPGPlayer updated = rpgPlayer.leaveJob(jobId);
        playerManager.savePlayer(updated);

        String name = jobOpt.map(Job::displayName).orElse(jobId);
        player.sendMessage(Component.text("Has abandonado el trabajo: ", NamedTextColor.YELLOW)
                .append(Component.text(name, NamedTextColor.WHITE)));

        build();
    }

}