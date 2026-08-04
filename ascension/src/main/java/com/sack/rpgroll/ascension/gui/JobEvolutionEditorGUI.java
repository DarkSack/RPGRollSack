package com.sack.rpgroll.ascension.gui;

import com.sack.rpgroll.ascension.deferred.JobEvolution;
import com.sack.rpgroll.ascension.deferred.JobEvolutionManager;
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
    private JobEvolution current;

    public JobEvolutionEditorGUI(Player player, JobEvolution evolution, JobEvolutionManager manager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text("Evolución de job: " + evolution.id(), NamedTextColor.GOLD), SIZE);
        this.current = evolution;
        this.manager = manager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
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
                .setName(Component.text("Job base: " + current.baseJob(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click para cambiarlo", NamedTextColor.GRAY))
                .build());

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(Component.text("Nombre: " + current.displayName(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click para escribir uno nuevo", NamedTextColor.GRAY))
                .build());

        setItem(LEVEL_SLOT, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(Component.text("Nivel de job requerido: " + current.requiredJobLevel(),
                        NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +5 · Click derecho: -5", NamedTextColor.GRAY))
                .build());

        setItem(RECIPES_SLOT, new ItemBuilder(Material.CRAFTING_TABLE)
                .setName(Component.text("Recetas desbloqueadas: " + current.unlockedRecipes().size(),
                        NamedTextColor.YELLOW))
                .setLore(Component.text(String.join(", ", current.unlockedRecipes()), NamedTextColor.GRAY),
                        Component.text("Click para escribir lista separada por comas", NamedTextColor.GRAY))
                .build());

        setItem(TOOLS_SLOT, new ItemBuilder(Material.IRON_HOE)
                .setName(Component.text("Herramientas desbloqueadas: " + current.unlockedTools().size(),
                        NamedTextColor.YELLOW))
                .setLore(Component.text(String.join(", ", current.unlockedTools()), NamedTextColor.GRAY),
                        Component.text("Click para escribir lista separada por comas", NamedTextColor.GRAY))
                .build());

        setItem(QUESTS_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(Component.text("Quests desbloqueadas: " + current.unlockedQuests().size(),
                        NamedTextColor.YELLOW))
                .setLore(Component.text(String.join(", ", current.unlockedQuests()), NamedTextColor.GRAY),
                        Component.text("Click para escribir lista separada por comas", NamedTextColor.GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        ClickType click = event.getClick();

        if (slot == BASE_JOB_SLOT) {
            chatPromptManager.prompt(player, "Escribí el id del job base:",
                    value -> replace(new JobEvolution(current.id(), value.trim().toLowerCase(Locale.ROOT),
                            current.displayName(), current.requiredJobLevel(), current.unlockedRecipes(),
                            current.unlockedTools(), current.unlockedQuests())));
            return;
        }

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nuevo nombre:",
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
            chatPromptManager.prompt(player, "Escribí las recetas desbloqueadas, separadas por comas:",
                    value -> replace(new JobEvolution(current.id(), current.baseJob(), current.displayName(),
                            current.requiredJobLevel(), List.of(value.trim().split(",")), current.unlockedTools(),
                            current.unlockedQuests())));
            return;
        }

        if (slot == TOOLS_SLOT) {
            chatPromptManager.prompt(player, "Escribí las herramientas desbloqueadas, separadas por comas:",
                    value -> replace(new JobEvolution(current.id(), current.baseJob(), current.displayName(),
                            current.requiredJobLevel(), current.unlockedRecipes(), List.of(value.trim().split(",")),
                            current.unlockedQuests())));
            return;
        }

        if (slot == QUESTS_SLOT) {
            chatPromptManager.prompt(player, "Escribí las quests desbloqueadas, separadas por comas:",
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
