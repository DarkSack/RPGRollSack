package com.sack.rpgroll.gui.admin;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.gameplay.job.Job;
import com.sack.rpgroll.gameplay.job.JobManager;
import com.sack.rpgroll.gameplay.job.JobReward;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Editor de un trabajo — identidad, curva de experiencia, exploración, y
 * su tabla de recompensas por bloque/entidad (agregar/actualizar por
 * target vía una línea de chat; no hay grilla por cada target dado que
 * pueden ser decenas).
 */
public class JobEditorGUI extends InventoryGUI {

    private static final int SIZE = 27;
    private static final int NAME_SLOT = 9;
    private static final int DESCRIPTION_SLOT = 10;
    private static final int MAX_LEVEL_SLOT = 11;
    private static final int EXP_BASE_SLOT = 12;
    private static final int EXP_MULT_SLOT = 13;
    private static final int REWARDS_SLOT = 14;
    private static final int EXPLORATION_SLOT = 15;
    private static final int BACK_SLOT = 26;

    private final JobManager jobManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private Job current;

    public JobEditorGUI(Player player, Job job, JobManager jobManager, ChatPromptManager chatPromptManager,
            Runnable onBack) {
        super(player, Component.text("Trabajo: " + job.id(), NamedTextColor.GOLD), SIZE);
        this.current = job;
        this.jobManager = jobManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(Job updated) {
        current = updated;
        jobManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(ComponentUtils.parse("Nombre: " + current.displayName()).colorIfAbsent(NamedTextColor.YELLOW))
                .setLore(Component.text("Click para escribir uno nuevo", NamedTextColor.GRAY),
                        Component.text("Nota: un trabajo nuevo necesita su propio listener Java",
                                NamedTextColor.DARK_GRAY),
                        Component.text("para otorgar XP automáticamente (ver AlquimistaJobListener, etc.)",
                                NamedTextColor.DARK_GRAY))
                .build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(Component.text("Descripción", NamedTextColor.YELLOW))
                .setLore(ItemBuilder.toLoreLines(current.description().isBlank() ? "(sin descripción)"
                        : current.description()))
                .build());

        setItem(MAX_LEVEL_SLOT, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(Component.text("Nivel máximo: " + current.maxLevel(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +5 · Click derecho: -5", NamedTextColor.GRAY))
                .build());

        setItem(EXP_BASE_SLOT, new ItemBuilder(Material.GOLD_NUGGET)
                .setName(Component.text("XP base: " + current.expBase(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +10 · Click derecho: -10", NamedTextColor.GRAY))
                .build());

        setItem(EXP_MULT_SLOT, new ItemBuilder(Material.NETHER_STAR)
                .setName(Component.text("Multiplicador de XP: " + current.expMultiplier(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +0.1 · Click derecho: -0.1", NamedTextColor.GRAY))
                .build());

        setItem(REWARDS_SLOT, new ItemBuilder(Material.CHEST)
                .setName(Component.text("Recompensas: " + current.rewards().size() + " target(s)",
                        NamedTextColor.YELLOW))
                .setLore(Component.text("Click para agregar/actualizar (target;oro;xp)", NamedTextColor.GRAY))
                .build());

        setItem(EXPLORATION_SLOT, new ItemBuilder(Material.MAP)
                .setName(Component.text("Exploración: " + current.newBiomeMoney() + " oro/bioma, "
                        + current.distanceBlocks() + " bloques", NamedTextColor.YELLOW))
                .setLore(Component.text("Click para escribir (oro-bioma;xp-bioma;bloques;oro-dist;xp-dist)",
                        NamedTextColor.GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        ClickType click = event.getClick();

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nuevo nombre:", value -> replace(new Job(current.id(),
                    value, current.description(), current.icon(), current.lore(), current.maxLevel(),
                    current.expBase(), current.expMultiplier(), current.rewards(), current.newBiomeMoney(),
                    current.newBiomeExperience(), current.distanceBlocks(), current.distanceMoney(),
                    current.distanceExperience())));
            return;
        }

        if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, "Escribí la nueva descripción:", value -> replace(new Job(current.id(),
                    current.displayName(), value, current.icon(), current.lore(), current.maxLevel(),
                    current.expBase(), current.expMultiplier(), current.rewards(), current.newBiomeMoney(),
                    current.newBiomeExperience(), current.distanceBlocks(), current.distanceMoney(),
                    current.distanceExperience())));
            return;
        }

        if (slot == MAX_LEVEL_SLOT) {
            int delta = click == ClickType.RIGHT ? -5 : 5;
            replace(new Job(current.id(), current.displayName(), current.description(), current.icon(),
                    current.lore(), Math.max(1, current.maxLevel() + delta), current.expBase(),
                    current.expMultiplier(), current.rewards(), current.newBiomeMoney(),
                    current.newBiomeExperience(), current.distanceBlocks(), current.distanceMoney(),
                    current.distanceExperience()));
            return;
        }

        if (slot == EXP_BASE_SLOT) {
            int delta = click == ClickType.RIGHT ? -10 : 10;
            replace(new Job(current.id(), current.displayName(), current.description(), current.icon(),
                    current.lore(), current.maxLevel(), Math.max(1, current.expBase() + delta),
                    current.expMultiplier(), current.rewards(), current.newBiomeMoney(),
                    current.newBiomeExperience(), current.distanceBlocks(), current.distanceMoney(),
                    current.distanceExperience()));
            return;
        }

        if (slot == EXP_MULT_SLOT) {
            double delta = click == ClickType.RIGHT ? -0.1 : 0.1;
            replace(new Job(current.id(), current.displayName(), current.description(), current.icon(),
                    current.lore(), current.maxLevel(), current.expBase(),
                    Math.max(0.1, current.expMultiplier() + delta), current.rewards(), current.newBiomeMoney(),
                    current.newBiomeExperience(), current.distanceBlocks(), current.distanceMoney(),
                    current.distanceExperience()));
            return;
        }

        if (slot == REWARDS_SLOT) {
            chatPromptManager.prompt(player, "Escribí: target;oro;xp (ej. DIAMOND_ORE;5;20):", value -> {

                String[] parts = value.split(";");

                if (parts.length < 3) {
                    player.sendMessage(Component.text("Formato inválido.", NamedTextColor.RED));
                    return;
                }

                try {
                    Map<String, JobReward> rewards = new HashMap<>(current.rewards());
                    rewards.put(parts[0].trim().toUpperCase(),
                            new JobReward(Double.parseDouble(parts[1].trim()), Integer.parseInt(parts[2].trim())));

                    replace(new Job(current.id(), current.displayName(), current.description(), current.icon(),
                            current.lore(), current.maxLevel(), current.expBase(), current.expMultiplier(), rewards,
                            current.newBiomeMoney(), current.newBiomeExperience(), current.distanceBlocks(),
                            current.distanceMoney(), current.distanceExperience()));
                } catch (NumberFormatException e) {
                    player.sendMessage(Component.text("Oro y XP deben ser numéricos.", NamedTextColor.RED));
                }
            });
            return;
        }

        if (slot == EXPLORATION_SLOT) {
            chatPromptManager.prompt(player,
                    "Escribí: oro-bioma;xp-bioma;bloques;oro-distancia;xp-distancia (ej. 50;100;500;10;20):",
                    value -> {

                        String[] parts = value.split(";");

                        if (parts.length < 5) {
                            player.sendMessage(Component.text("Hacen falta 5 valores.", NamedTextColor.RED));
                            return;
                        }

                        try {
                            replace(new Job(current.id(), current.displayName(), current.description(),
                                    current.icon(), current.lore(), current.maxLevel(), current.expBase(),
                                    current.expMultiplier(), current.rewards(),
                                    Double.parseDouble(parts[0].trim()), Integer.parseInt(parts[1].trim()),
                                    Integer.parseInt(parts[2].trim()), Double.parseDouble(parts[3].trim()),
                                    Integer.parseInt(parts[4].trim())));
                        } catch (NumberFormatException e) {
                            player.sendMessage(Component.text("Todos los valores deben ser numéricos.",
                                    NamedTextColor.RED));
                        }
                    });
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
