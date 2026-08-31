package com.sack.rpgroll.workers.gui;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.workers.core.economy.WageType;
import com.sack.rpgroll.workers.core.profession.Profession;
import com.sack.rpgroll.workers.core.profession.ProfessionManager;
import com.sack.rpgroll.workers.core.worker.Worker;
import com.sack.rpgroll.workers.core.worker.WorkerManager;
import com.sack.rpgroll.workers.integration.GuildsIntegration;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Locale;

/** Ficha completa de UN worker — identidad, necesidades, habilidades, contrato, y acciones de gestión. */
public class WorkerDetailGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int IDENTITY_SLOT = 10;
    private static final int NEEDS_SLOT = 12;
    private static final int SKILLS_SLOT = 14;
    private static final int CONTRACT_SLOT = 16;

    private static final int RENAME_SLOT = 28;
    private static final int HIRE_SLOT = 30;
    private static final int FIRE_SLOT = 31;
    private static final int SET_HOME_SLOT = 32;

    private static final int BACK_SLOT = 40;

    private final Worker worker;
    private final WorkerManager workerManager;
    private final ProfessionManager professionManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;

    public WorkerDetailGUI(Player player, Worker worker, WorkerManager workerManager,
            ProfessionManager professionManager, ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.worker.detail.title", "id",
                worker.id().toString().substring(0, 8)), NamedTextColor.GOLD), SIZE);
        this.worker = worker;
        this.workerManager = workerManager;
        this.professionManager = professionManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        Profession profession = professionManager.get(worker.professionId()).orElse(null);

        setItem(IDENTITY_SLOT, new ItemBuilder(profession != null
                ? ProfessionBrowserGUI.parseMaterial(profession.icon(), Material.VILLAGER_SPAWN_EGG)
                : Material.BARRIER)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.worker.detail.identity"), NamedTextColor.GOLD))
                .setLore(ItemBuilder.toLoreLines(chatPromptManager.lang().raw("gui.worker.detail.identity_lore", "name",
                        worker.customName() != null ? worker.customName()
                                : chatPromptManager.lang().raw("gui.worker.detail.no_name"),
                        "profession", profession != null ? profession.displayName() : worker.professionId(),
                        "personality", worker.personality(), "action", worker.currentAction())))
                .build());

        setItem(NEEDS_SLOT, new ItemBuilder(Material.GOLDEN_APPLE)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.worker.detail.needs"), NamedTextColor.GREEN))
                .setLore(ItemBuilder.toLoreLines(chatPromptManager.lang().raw("gui.worker.detail.needs_lore", "hunger",
                        String.format(Locale.ROOT, "%.0f", worker.hunger()), "energy",
                        String.format(Locale.ROOT, "%.0f", worker.energy()), "sleep",
                        String.format(Locale.ROOT, "%.0f", worker.sleep()), "stress",
                        String.format(Locale.ROOT, "%.0f", worker.stress()), "motivation",
                        String.format(Locale.ROOT, "%.0f", worker.motivation()), "health",
                        String.format(Locale.ROOT, "%.0f", worker.health()), "happiness",
                        String.format(Locale.ROOT, "%.0f", worker.happiness()), "morale",
                        String.format(Locale.ROOT, "%.0f", worker.morale()))))
                .build());

        setItem(SKILLS_SLOT, new ItemBuilder(Material.BOOK)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.worker.detail.skills"), NamedTextColor.LIGHT_PURPLE))
                .setLore(ItemBuilder.toLoreLines(formatSkills()))
                .build());

        setItem(CONTRACT_SLOT, new ItemBuilder(Material.PAPER)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.worker.detail.contract"), NamedTextColor.AQUA))
                .setLore(ItemBuilder.toLoreLines(worker.isEmployed()
                        ? chatPromptManager.lang().raw("gui.worker.detail.contract_employed", "employer",
                                shortId(worker.employerId()), "wage", worker.wageAmount(), "type", worker.wageType())
                        : chatPromptManager.lang().raw("gui.worker.detail.contract_unemployed")))
                .build());

        setItem(RENAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.worker.detail.rename"), NamedTextColor.YELLOW))
                .build());

        setItem(HIRE_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.worker.detail.hire"), NamedTextColor.GREEN))
                .build());

        setItem(FIRE_SLOT, new ItemBuilder(Material.BARRIER)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.worker.detail.fire"), NamedTextColor.RED)).build());

        setItem(SET_HOME_SLOT, new ItemBuilder(Material.RED_BED)
                .setName(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.worker.detail.set_home"), NamedTextColor.LIGHT_PURPLE))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(chatPromptManager.lang().raw("gui.common.back")));
    }

    private String formatSkills() {

        if (worker.skillLevels().isEmpty()) {
            return chatPromptManager.lang().raw("gui.worker.detail.no_skills");
        }

        StringBuilder builder = new StringBuilder();

        for (var entry : worker.skillLevels().entrySet()) {
            builder.append(chatPromptManager.lang().raw("gui.worker.detail.skill_level_line", "skill", entry.getKey(),
                    "level", entry.getValue())).append("\n");
        }

        return builder.toString().strip();
    }

    private String shortId(java.util.UUID id) {
        return id == null ? "?" : id.toString().substring(0, 8);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == RENAME_SLOT) {
            chatPromptManager.prompt(player, chatPromptManager.lang().raw("gui.worker.detail.prompt_rename"), value -> {
                worker.setCustomName(value);
                workerManager.save(worker);
                build();
            });
        } else if (slot == HIRE_SLOT) {

            if (!canManage(player)) {
                player.sendMessage(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.worker.detail.already_employed_other"), NamedTextColor.RED));
                return;
            }

            Profession profession = professionManager.get(worker.professionId()).orElse(null);
            double wage = profession != null ? profession.wageAmount() : 0;
            WageType wageType = profession != null ? profession.wageType() : WageType.PER_TASK;

            worker.hire(player.getUniqueId(), wage, wageType);
            workerManager.save(worker);
            player.sendMessage(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.worker.detail.hired"), NamedTextColor.GREEN));
            build();

        } else if (slot == FIRE_SLOT) {

            if (!canManage(player)) {
                player.sendMessage(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.worker.detail.cannot_fire_other"), NamedTextColor.RED));
                return;
            }

            worker.fire();
            workerManager.save(worker);
            player.sendMessage(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.worker.detail.fired"), NamedTextColor.YELLOW));
            build();

        } else if (slot == SET_HOME_SLOT) {

            worker.setHomeLocation(player.getLocation());
            workerManager.save(worker);
            player.sendMessage(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("gui.worker.detail.home_set"), NamedTextColor.GREEN));
            build();

        } else if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private boolean canManage(Player viewer) {

        if (!worker.isEmployed()) {
            return true;
        }

        return worker.employerId().equals(viewer.getUniqueId())
                || GuildsIntegration.sameGuild(worker.employerId(), viewer.getUniqueId());
    }

}
