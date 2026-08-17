package com.sack.rpgroll.gui.admin;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.common.lang.LangManager;
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
    private final LangManager lang;
    private final Runnable onBack;
    private Job current;

    public JobEditorGUI(Player player, Job job, JobManager jobManager, ChatPromptManager chatPromptManager,
            LangManager lang, Runnable onBack) {
        super(player, lang.component("job_editor_gui.title", "id", job.id()), SIZE);
        this.current = job;
        this.jobManager = jobManager;
        this.chatPromptManager = chatPromptManager;
        this.lang = lang;
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
                .setName(ComponentUtils.parse(lang.raw("job_editor_gui.name_slot_name", "name",
                        current.displayName())).colorIfAbsent(NamedTextColor.YELLOW))
                .setLore(lang.component("job_editor_gui.click_new_value"),
                        lang.component("job_editor_gui.name_slot_note_line1"),
                        lang.component("job_editor_gui.name_slot_note_line2"))
                .build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(lang.component("job_editor_gui.description_slot_name"))
                .setLore(ItemBuilder.toLoreLines(current.description().isBlank()
                        ? lang.raw("job_editor_gui.no_description")
                        : current.description()))
                .build());

        setItem(MAX_LEVEL_SLOT, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(lang.component("job_editor_gui.max_level_slot_name", "level", current.maxLevel()))
                .setLore(lang.component("job_editor_gui.max_level_slot_lore"))
                .build());

        setItem(EXP_BASE_SLOT, new ItemBuilder(Material.GOLD_NUGGET)
                .setName(lang.component("job_editor_gui.exp_base_slot_name", "exp", current.expBase()))
                .setLore(lang.component("job_editor_gui.exp_base_slot_lore"))
                .build());

        setItem(EXP_MULT_SLOT, new ItemBuilder(Material.NETHER_STAR)
                .setName(lang.component("job_editor_gui.exp_mult_slot_name", "multiplier",
                        current.expMultiplier()))
                .setLore(lang.component("job_editor_gui.exp_mult_slot_lore"))
                .build());

        setItem(REWARDS_SLOT, new ItemBuilder(Material.CHEST)
                .setName(lang.component("job_editor_gui.rewards_slot_name", "count", current.rewards().size()))
                .setLore(lang.component("job_editor_gui.rewards_slot_lore"))
                .build());

        setItem(EXPLORATION_SLOT, new ItemBuilder(Material.MAP)
                .setName(lang.component("job_editor_gui.exploration_slot_name", "money", current.newBiomeMoney(),
                        "blocks", current.distanceBlocks()))
                .setLore(lang.component("job_editor_gui.exploration_slot_lore"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("job_editor_gui.back_button")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        ClickType click = event.getClick();

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, lang.raw("job_editor_gui.prompt_name"), value -> replace(new Job(
                    current.id(), value, current.description(), current.icon(), current.lore(), current.maxLevel(),
                    current.expBase(), current.expMultiplier(), current.rewards(), current.newBiomeMoney(),
                    current.newBiomeExperience(), current.distanceBlocks(), current.distanceMoney(),
                    current.distanceExperience())));
            return;
        }

        if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, lang.raw("job_editor_gui.prompt_description"), value -> replace(
                    new Job(current.id(), current.displayName(), value, current.icon(), current.lore(),
                            current.maxLevel(), current.expBase(), current.expMultiplier(), current.rewards(),
                            current.newBiomeMoney(), current.newBiomeExperience(), current.distanceBlocks(),
                            current.distanceMoney(), current.distanceExperience())));
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
            chatPromptManager.prompt(player, lang.raw("job_editor_gui.prompt_rewards"), value -> {

                String[] parts = value.split(";");

                if (parts.length < 3) {
                    player.sendMessage(lang.component("job_editor_gui.invalid_format"));
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
                    player.sendMessage(lang.component("job_editor_gui.rewards_not_numeric"));
                }
            });
            return;
        }

        if (slot == EXPLORATION_SLOT) {
            chatPromptManager.prompt(player, lang.raw("job_editor_gui.prompt_exploration"),
                    value -> {

                        String[] parts = value.split(";");

                        if (parts.length < 5) {
                            player.sendMessage(lang.component("job_editor_gui.exploration_missing_values"));
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
                            player.sendMessage(lang.component("job_editor_gui.exploration_not_numeric"));
                        }
                    });
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
