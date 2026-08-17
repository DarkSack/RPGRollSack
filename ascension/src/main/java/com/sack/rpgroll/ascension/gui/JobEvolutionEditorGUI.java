package com.sack.rpgroll.ascension.gui;

import com.sack.rpgroll.ascension.deferred.JobEvolution;
import com.sack.rpgroll.ascension.deferred.JobEvolutionManager;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

public class JobEvolutionEditorGUI extends InventoryGUI {

    private static final int SIZE = 27;
    private static final int BASE_JOB_SLOT = 9;
    private static final int NAME_SLOT = 10;
    private static final int LEVEL_SLOT = 11;
    private static final int RECIPES_SLOT = 12;
    private static final int TOOLS_SLOT = 13;
    private static final int QUESTS_SLOT = 14;
    private static final int BACK_SLOT = 26;

    private final JobEvolutionManager manager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private final LangManager lang;
    private JobEvolution current;

    public JobEvolutionEditorGUI(Player player, JobEvolution evolution, JobEvolutionManager manager,
            ChatPromptManager chatPromptManager, Runnable onBack, LangManager lang) {
        super(player, lang.component("gui.job_evolution.editor_title", "id", evolution.id()), SIZE);
        this.current = evolution;
        this.manager = manager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
        this.lang = lang;
    }

    private void replace(JobEvolution updated) {
        current = updated;
        manager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(BASE_JOB_SLOT, new ItemBuilder(Material.IRON_PICKAXE)
                .setName(lang.component("gui.job_evolution.base_job_label", "job", current.baseJob()))
                .setLore(lang.component("gui.job_evolution.click_to_change"))
                .build());

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(lang.component("gui.common.name_label", "name", current.displayName()))
                .setLore(lang.component("gui.common.click_new_value"))
                .build());

        setItem(LEVEL_SLOT, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(lang.component("gui.job_evolution.level_label", "level", current.requiredJobLevel()))
                .setLore(lang.component("gui.job_evolution.click_plusminus5"))
                .build());

        setItem(RECIPES_SLOT, new ItemBuilder(Material.CRAFTING_TABLE)
                .setName(lang.component("gui.job_evolution.recipes_label", "count",
                        current.unlockedRecipes().size()))
                .setLore(Component.text(String.join(", ", current.unlockedRecipes()), NamedTextColor.GRAY),
                        lang.component("gui.common.comma_hint"))
                .build());

        setItem(TOOLS_SLOT, new ItemBuilder(Material.IRON_HOE)
                .setName(lang.component("gui.job_evolution.tools_label", "count", current.unlockedTools().size()))
                .setLore(Component.text(String.join(", ", current.unlockedTools()), NamedTextColor.GRAY),
                        lang.component("gui.common.comma_hint"))
                .build());

        setItem(QUESTS_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(lang.component("gui.job_evolution.quests_label", "count",
                        current.unlockedQuests().size()))
                .setLore(Component.text(String.join(", ", current.unlockedQuests()), NamedTextColor.GRAY),
                        lang.component("gui.common.comma_hint"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back_button")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        ClickType click = event.getClick();

        if (slot == BASE_JOB_SLOT) {
            chatPromptManager.prompt(player, "gui.job_evolution.prompt_base_job",
                    value -> replace(new JobEvolution(current.id(), value.trim().toLowerCase(Locale.ROOT),
                            current.displayName(), current.requiredJobLevel(), current.unlockedRecipes(),
                            current.unlockedTools(), current.unlockedQuests())));
            return;
        }

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "gui.job_evolution.prompt_new_name",
                    value -> replace(new JobEvolution(current.id(), current.baseJob(), value,
                            current.requiredJobLevel(), current.unlockedRecipes(), current.unlockedTools(),
                            current.unlockedQuests())));
            return;
        }

        if (slot == LEVEL_SLOT) {
            int delta = click == ClickType.RIGHT ? -5 : 5;
            replace(new JobEvolution(current.id(), current.baseJob(), current.displayName(),
                    Math.max(0, current.requiredJobLevel() + delta), current.unlockedRecipes(),
                    current.unlockedTools(), current.unlockedQuests()));
            return;
        }

        if (slot == RECIPES_SLOT) {
            chatPromptManager.prompt(player, "gui.job_evolution.prompt_recipes",
                    value -> replace(new JobEvolution(current.id(), current.baseJob(), current.displayName(),
                            current.requiredJobLevel(), List.of(value.trim().split(",")), current.unlockedTools(),
                            current.unlockedQuests())));
            return;
        }

        if (slot == TOOLS_SLOT) {
            chatPromptManager.prompt(player, "gui.job_evolution.prompt_tools",
                    value -> replace(new JobEvolution(current.id(), current.baseJob(), current.displayName(),
                            current.requiredJobLevel(), current.unlockedRecipes(), List.of(value.trim().split(",")),
                            current.unlockedQuests())));
            return;
        }

        if (slot == QUESTS_SLOT) {
            chatPromptManager.prompt(player, "gui.job_evolution.prompt_quests",
                    value -> replace(new JobEvolution(current.id(), current.baseJob(), current.displayName(),
                            current.requiredJobLevel(), current.unlockedRecipes(), current.unlockedTools(),
                            List.of(value.trim().split(",")))));
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
