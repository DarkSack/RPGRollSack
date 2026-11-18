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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * GUI principal de trabajos: muestra el catálogo completo (JobManager) y
 * el estado de cada uno para el jugador (activo con nivel/XP, o disponible
 * para unirse). Click en un trabajo activo pide confirmación antes de
 * abandonarlo (pierde nivel y XP); click en uno inactivo lo une, o abre
 * JobAbandonGUI si ya está en el máximo (3).
 */
public class JobsGUI extends InventoryGUI {

    /** Fondo dibujado (sackito:menus/trabajos). */
    public static final String BACKGROUND = "trabajos";

    private static final int[] CONTENT_SLOTS = {
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25
    };

    private final JobManager jobManager;
    private final PlayerManager playerManager;
    private final LangManager lang;
    private final Map<Integer, String> slotToJob;

    public JobsGUI(Player player, JobManager jobManager, PlayerManager playerManager, LangManager lang) {
        super(player, CoreMenus.skin().title(BACKGROUND, lang.component("jobs_gui.title").decorate(TextDecoration.BOLD)), 45);
        this.jobManager = jobManager;
        this.playerManager = playerManager;
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
        for (int i = 36; i < 45; i++) {
            setItem(i, CoreMenus.filler());
        }

        Optional<RPGPlayer> rpgPlayerOpt = playerManager.getPlayer(player.getUniqueId());

        if (rpgPlayerOpt.isEmpty()) {
            setItem(22, new ItemBuilder(Material.BARRIER)
                    .setName(lang.component("error.load_data"))
                    .build());
            return;
        }

        PlayerJobs playerJobs = rpgPlayerOpt.get().getJobs();

        // Info general en la parte superior
        setItem(4, new ItemBuilder(Material.BOOK)
                .setName(lang.component("jobs_gui.info_name").decorate(TextDecoration.BOLD))
                .setLore(lang.component("jobs_gui.info_lore", "count", playerJobs.count(), "max",
                        PlayerJobs.MAX_ACTIVE_JOBS))
                .build());

        List<Job> jobs = new ArrayList<>(jobManager.getAll());

        if (jobs.isEmpty()) {
            setItem(22, new ItemBuilder(Material.BARRIER)
                    .setName(lang.component("jobs_gui.none_available"))
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
                lore.add(ComponentUtils.parse(line).colorIfAbsent(net.kyori.adventure.text.format.NamedTextColor.GRAY));
            }
        }

        lore.add(Component.text(""));

        if (active) {

            JobProgress progress = playerJobs.getProgress(job.id()).orElseThrow();
            int expRequired = job.getExpRequiredForLevel(progress.level() + 1);

            lore.add(lang.component("jobs_gui.active_badge").decorate(TextDecoration.BOLD));
            lore.add(lang.component("jobs_gui.level_line", "level", progress.level(), "max", job.maxLevel()));

            if (progress.level() < job.maxLevel()) {
                lore.add(lang.component("jobs_gui.xp_line", "current", progress.experience(), "required",
                        expRequired));
            } else {
                lore.add(lang.component("jobs_gui.max_level"));
            }

            lore.add(Component.text(""));
            lore.add(lang.component("jobs_gui.click_abandon"));

        } else {

            lore.add(lang.component("jobs_gui.available_badge"));
            lore.add(Component.text(""));
            lore.add(lang.component("jobs_gui.click_join"));

        }

        ItemStack item = ItemBuilder.icon(job.icon())
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
            confirmLeave(jobId, playerJobs.getProgress(jobId).map(JobProgress::level).orElse(1));
            return;
        }

        if (playerJobs.isFull()) {
            close();
            JobAbandonGUI abandonGUI = new JobAbandonGUI(player, jobManager, playerManager, jobId, lang);
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

        // Cancelable: un addon puede exigir requisitos que el core no conoce.
        var joinEvent = new com.sack.rpgroll.api.event.PlayerJoinJobEvent(player, rpgPlayer, jobId);
        org.bukkit.Bukkit.getPluginManager().callEvent(joinEvent);
        if (joinEvent.isCancelled()) {
            lang.send(player, "jobs_gui.join_denied", "job", jobOpt.get().displayName());
            return;
        }

        RPGPlayer updated = rpgPlayer.joinJob(jobId);
        playerManager.savePlayer(updated);

        lang.send(player, "jobs_gui.joined", "job", jobOpt.get().displayName());

        build();
    }

    /**
     * Abandonar borra el nivel y la experiencia del oficio: antes se pregunta, para que tocar un
     * oficio activo para leerlo no lo pierda. Las dos salidas vuelven a esta misma lista.
     */
    private void confirmLeave(String jobId, int level) {

        Optional<Job> jobOpt = jobManager.get(jobId);
        String name = jobOpt.map(Job::displayName).orElse(jobId);
        ItemBuilder icon = jobOpt.map(job -> ItemBuilder.icon(job.icon()))
                .orElseGet(() -> new ItemBuilder(Material.BARRIER));

        ItemStack subject = icon
                .setName(lang.component("jobs_gui.confirm_leave_name", "job", name).decorate(TextDecoration.BOLD))
                .setLore(lang.component("jobs_gui.confirm_leave_lore1", "level", level),
                        lang.component("jobs_gui.confirm_leave_lore2"))
                .build();

        close();
        new ConfirmGUI(player, lang.component("jobs_gui.confirm_title").decorate(TextDecoration.BOLD), subject,
                lang.raw("jobs_gui.confirm_leave_button", "level", level), lang.raw("jobs_gui.confirm_keep_button"),
                true,
                () -> {
                    playerManager.getPlayer(player.getUniqueId())
                            .filter(current -> current.getJobs().hasJob(jobId))
                            .ifPresent(current -> leaveJob(current, jobId));
                    reopen();
                },
                () -> {
                    lang.send(player, "jobs_gui.kept", "job", name);
                    reopen();
                },
                false).open();
    }

    private void reopen() {
        new JobsGUI(player, jobManager, playerManager, lang).open();
    }

    private void leaveJob(RPGPlayer rpgPlayer, String jobId) {

        Optional<Job> jobOpt = jobManager.get(jobId);

        RPGPlayer updated = rpgPlayer.leaveJob(jobId);
        playerManager.savePlayer(updated);
        org.bukkit.Bukkit.getPluginManager().callEvent(
                new com.sack.rpgroll.api.event.PlayerLeaveJobEvent(player, updated, jobId));

        String name = jobOpt.map(Job::displayName).orElse(jobId);
        lang.send(player, "jobs_gui.left", "job", name);

        build();
    }

}
