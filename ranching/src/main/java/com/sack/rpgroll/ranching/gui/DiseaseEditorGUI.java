package com.sack.rpgroll.ranching.gui;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.ranching.core.health.Disease;
import com.sack.rpgroll.ranching.core.health.DiseaseManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class DiseaseEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int NAME_SLOT = 9;
    private static final int DESCRIPTION_SLOT = 10;
    private static final int SYMPTOMS_SLOT = 11;
    private static final int DURATION_SLOT = 12;
    private static final int CONTAGION_SLOT = 13;
    private static final int HEALTH_PENALTY_SLOT = 14;
    private static final int HAPPINESS_PENALTY_SLOT = 15;
    private static final int PRODUCTION_PENALTY_SLOT = 16;
    private static final int BACK_SLOT = 40;

    private final DiseaseManager diseaseManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private Disease current;

    public DiseaseEditorGUI(Player player, Disease disease, DiseaseManager diseaseManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.disease.editor.title", "id", disease.id()), NamedTextColor.GOLD), SIZE);
        this.current = disease;
        this.diseaseManager = diseaseManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(Disease updated) {
        current = updated;
        diseaseManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        var lang = chatPromptManager.lang();

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(lang.component("gui.editor.name_line", "name", current.displayName())).build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.editor.description_title"), NamedTextColor.YELLOW))
                .setLore(ItemBuilder.toLoreLines(current.description().isBlank() ? lang.raw("gui.editor.no_description") : current.description()))
                .build());

        setItem(SYMPTOMS_SLOT, new ItemBuilder(Material.SPIDER_EYE)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.disease.editor.symptoms_line", "symptoms", String.join(", ", current.symptoms())), NamedTextColor.GOLD))
                .setLore(ComponentUtils.parseWithDefault(lang.raw("gui.disease.editor.prompt_symptoms"), NamedTextColor.GRAY)).build());

        setItem(DURATION_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.disease.editor.duration_line", "ticks", current.durationTicks()), NamedTextColor.WHITE))
                .setLore(ComponentUtils.parseWithDefault(lang.raw("gui.editor.step1200_hint"), NamedTextColor.GRAY)).build());

        setItem(CONTAGION_SLOT, new ItemBuilder(Material.SLIME_BALL)
                .setName(ComponentUtils.parseWithDefault(
                        lang.raw("gui.disease.editor.contagion_line", "value", String.format(Locale.ROOT, "%.0f", current.contagionChance() * 100)), NamedTextColor.GREEN))
                .setLore(ComponentUtils.parseWithDefault(lang.raw("gui.editor.step5pct_hint"), NamedTextColor.GRAY)).build());

        setItem(HEALTH_PENALTY_SLOT, new ItemBuilder(Material.REDSTONE)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.disease.editor.health_penalty_line", "value", current.healthPenaltyPerCheck()), NamedTextColor.RED))
                .setLore(ComponentUtils.parseWithDefault(lang.raw("gui.editor.step1_hint"), NamedTextColor.GRAY)).build());

        setItem(HAPPINESS_PENALTY_SLOT, new ItemBuilder(Material.GRAY_DYE)
                .setName(ComponentUtils.parseWithDefault(lang.raw("gui.disease.editor.happiness_penalty_line", "value", current.happinessPenaltyPerCheck()), NamedTextColor.LIGHT_PURPLE))
                .setLore(ComponentUtils.parseWithDefault(lang.raw("gui.editor.step1_hint"), NamedTextColor.GRAY)).build());

        setItem(PRODUCTION_PENALTY_SLOT, new ItemBuilder(Material.BARRIER)
                .setName(ComponentUtils.parseWithDefault(
                        lang.raw("gui.disease.editor.production_penalty_line", "value", String.format(Locale.ROOT, "%.0f", current.productionPenalty() * 100)), NamedTextColor.AQUA))
                .setLore(ComponentUtils.parseWithDefault(lang.raw("gui.editor.step10pct_hint"), NamedTextColor.GRAY)).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        int sign = event.getClick() == ClickType.RIGHT ? -1 : 1;

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.editor.prompt_name"), value -> replace(new Disease(current.id(),
                    value, current.description(), current.symptoms(), current.durationTicks(),
                    current.contagionChance(), current.healthPenaltyPerCheck(), current.happinessPenaltyPerCheck(),
                    current.productionPenalty())));
        } else if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.editor.prompt_description"), value -> replace(new Disease(
                    current.id(), current.displayName(), value, current.symptoms(), current.durationTicks(),
                    current.contagionChance(), current.healthPenaltyPerCheck(), current.happinessPenaltyPerCheck(),
                    current.productionPenalty())));
        } else if (slot == SYMPTOMS_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.disease.editor.prompt_symptoms"), value -> {

                Set<String> symptoms = new HashSet<>();
                for (String entry : value.split(",")) {
                    if (!entry.isBlank()) {
                        symptoms.add(entry.trim().toLowerCase(Locale.ROOT));
                    }
                }

                replace(new Disease(current.id(), current.displayName(), current.description(), symptoms,
                        current.durationTicks(), current.contagionChance(), current.healthPenaltyPerCheck(),
                        current.happinessPenaltyPerCheck(), current.productionPenalty()));
            });
        } else if (slot == DURATION_SLOT) {
            replace(new Disease(current.id(), current.displayName(), current.description(), current.symptoms(),
                    Math.max(20, current.durationTicks() + sign * 1200L), current.contagionChance(),
                    current.healthPenaltyPerCheck(), current.happinessPenaltyPerCheck(), current.productionPenalty()));
        } else if (slot == CONTAGION_SLOT) {
            replace(new Disease(current.id(), current.displayName(), current.description(), current.symptoms(),
                    current.durationTicks(), Math.max(0, Math.min(1, current.contagionChance() + sign * 0.05)),
                    current.healthPenaltyPerCheck(), current.happinessPenaltyPerCheck(), current.productionPenalty()));
        } else if (slot == HEALTH_PENALTY_SLOT) {
            replace(new Disease(current.id(), current.displayName(), current.description(), current.symptoms(),
                    current.durationTicks(), current.contagionChance(),
                    Math.max(0, current.healthPenaltyPerCheck() + sign), current.happinessPenaltyPerCheck(),
                    current.productionPenalty()));
        } else if (slot == HAPPINESS_PENALTY_SLOT) {
            replace(new Disease(current.id(), current.displayName(), current.description(), current.symptoms(),
                    current.durationTicks(), current.contagionChance(), current.healthPenaltyPerCheck(),
                    Math.max(0, current.happinessPenaltyPerCheck() + sign), current.productionPenalty()));
        } else if (slot == PRODUCTION_PENALTY_SLOT) {
            replace(new Disease(current.id(), current.displayName(), current.description(), current.symptoms(),
                    current.durationTicks(), current.contagionChance(), current.healthPenaltyPerCheck(),
                    current.happinessPenaltyPerCheck(), Math.max(0, Math.min(1, current.productionPenalty() + sign * 0.1))));
        } else if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
