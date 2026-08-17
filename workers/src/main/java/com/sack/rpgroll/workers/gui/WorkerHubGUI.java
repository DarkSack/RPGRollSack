package com.sack.rpgroll.workers.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.workers.core.event.WorkerEventManager;
import com.sack.rpgroll.workers.core.profession.ProfessionManager;
import com.sack.rpgroll.workers.core.schedule.ScheduleManager;
import com.sack.rpgroll.workers.core.skill.SkillManager;
import com.sack.rpgroll.workers.core.worker.WorkerManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Hub del Worker Studio — enlaza a los 4 navegadores de contenido y al Explorador de Workers. */
public class WorkerHubGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int PROFESSIONS_SLOT = 11;
    private static final int SKILLS_SLOT = 13;
    private static final int SCHEDULES_SLOT = 15;
    private static final int EVENTS_SLOT = 17;
    private static final int WORKERS_SLOT = 31;
    private static final int CLOSE_SLOT = 40;

    private final ProfessionManager professionManager;
    private final SkillManager skillManager;
    private final ScheduleManager scheduleManager;
    private final WorkerEventManager eventManager;
    private final WorkerManager workerManager;
    private final ChatPromptManager chatPromptManager;

    public WorkerHubGUI(Player player, ProfessionManager professionManager, SkillManager skillManager,
            ScheduleManager scheduleManager, WorkerEventManager eventManager, WorkerManager workerManager,
            ChatPromptManager chatPromptManager) {
        super(player, Component.text(chatPromptManager.lang().raw("gui.hub.title"), NamedTextColor.GOLD), SIZE);
        this.professionManager = professionManager;
        this.skillManager = skillManager;
        this.scheduleManager = scheduleManager;
        this.eventManager = eventManager;
        this.workerManager = workerManager;
        this.chatPromptManager = chatPromptManager;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(PROFESSIONS_SLOT, button(Material.VILLAGER_SPAWN_EGG,
                chatPromptManager.lang().raw("gui.hub.professions"), professionManager.count()));
        setItem(SKILLS_SLOT, button(Material.BOOK, chatPromptManager.lang().raw("gui.hub.skills"), skillManager.count()));
        setItem(SCHEDULES_SLOT, button(Material.CLOCK, chatPromptManager.lang().raw("gui.hub.schedules"),
                scheduleManager.count()));
        setItem(EVENTS_SLOT, button(Material.PAPER, chatPromptManager.lang().raw("gui.hub.events"), eventManager.count()));

        setItem(WORKERS_SLOT, new ItemBuilder(Material.CHEST)
                .setName(Component.text(
                        chatPromptManager.lang().raw("gui.hub.worker_browser", "count", workerManager.getAll().size()),
                        NamedTextColor.YELLOW))
                .setLore(Component.text(chatPromptManager.lang().raw("gui.common.click_to_manage"), NamedTextColor.GRAY))
                .build());

        setItem(CLOSE_SLOT, ItemBuilder.createCancelButton(chatPromptManager.lang().raw("gui.common.close")));
    }

    private org.bukkit.inventory.ItemStack button(Material material, String label, int count) {
        return new ItemBuilder(material)
                .setName(Component.text(label + " (" + count + ")", NamedTextColor.YELLOW))
                .setLore(Component.text(chatPromptManager.lang().raw("gui.common.click_to_manage"), NamedTextColor.GRAY))
                .build();
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == PROFESSIONS_SLOT) {
            new ProfessionBrowserGUI(player, professionManager, chatPromptManager, this::reopen).open();
        } else if (slot == SKILLS_SLOT) {
            new SkillBrowserGUI(player, skillManager, professionManager, chatPromptManager, this::reopen).open();
        } else if (slot == SCHEDULES_SLOT) {
            new ScheduleBrowserGUI(player, scheduleManager, chatPromptManager, this::reopen).open();
        } else if (slot == EVENTS_SLOT) {
            new WorkerEventBrowserGUI(player, eventManager, chatPromptManager, this::reopen).open();
        } else if (slot == WORKERS_SLOT) {
            new WorkerBrowserGUI(player, workerManager, professionManager, chatPromptManager, this::reopen).open();
        } else if (slot == CLOSE_SLOT) {
            close();
        }
    }

    private void reopen() {
        open();
    }

}
