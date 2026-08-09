package com.sack.rpgroll.gui.admin;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.gameplay.job.Job;
import com.sack.rpgroll.gameplay.job.JobManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class JobBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final JobManager jobManager;
    private final ChatPromptManager chatPromptManager;
    private List<Job> jobs;

    public JobBrowserGUI(Player player, JobManager jobManager, ChatPromptManager chatPromptManager) {
        super(player, Component.text("Trabajos RPGRoll", NamedTextColor.GOLD), SIZE);
        this.jobManager = jobManager;
        this.chatPromptManager = chatPromptManager;
        this.jobs = List.copyOf(jobManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < jobs.size() && i < 36; i++) {

            Job job = jobs.get(i);

            setItem(i, new ItemBuilder(Material.IRON_PICKAXE)
                    .setName(Component.text(job.id(), NamedTextColor.YELLOW))
                    .setLore(Component.text("Nivel máx. " + job.maxLevel() + " · " + job.rewards().size()
                            + " recompensa(s)", NamedTextColor.GRAY),
                            Component.text("Click para editar", NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Crear trabajo nuevo", NamedTextColor.GREEN))
                .setLore(Component.text("Recordá: sin listener Java propio, no otorgará XP solo.",
                        NamedTextColor.DARK_GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Cerrar"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < jobs.size() && slot < 36) {
            new JobEditorGUI(player, jobs.get(slot), jobManager, chatPromptManager, this::reopen).open();
            return;
        }

        if (slot == NEW_SLOT) {
            promptNew();
            return;
        }

        if (slot == BACK_SLOT) {
            close();
        }
    }

    private void promptNew() {
        chatPromptManager.prompt(player, "Escribí el id del nuevo trabajo:", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (jobManager.exists(id)) {
                player.sendMessage(Component.text("Ya existe un trabajo con ese id.", NamedTextColor.RED));
                reopen();
                return;
            }

            Job job = new Job(id, id, "", "", List.of(), 50, 100, 1.4, Map.of(), 0, 0, 0, 0, 0);
            jobManager.save(job);
            reopen();
        });
    }

    private void reopen() {
        this.jobs = List.copyOf(jobManager.getAll());
        open();
    }

}
