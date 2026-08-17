package com.sack.rpgroll.workers.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.workers.core.profession.Profession;
import com.sack.rpgroll.workers.core.profession.ProfessionManager;
import com.sack.rpgroll.workers.core.worker.Worker;
import com.sack.rpgroll.workers.core.worker.WorkerManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;

/** Lista todos los workers rastreados, paginada — click en uno abre su ficha completa. */
public class WorkerBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int PER_PAGE = 36;
    private static final int PREV_SLOT = 38;
    private static final int NEXT_SLOT = 42;
    private static final int BACK_SLOT = 44;

    private final WorkerManager workerManager;
    private final ProfessionManager professionManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private List<Worker> workers;
    private int page;

    public WorkerBrowserGUI(Player player, WorkerManager workerManager, ProfessionManager professionManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, Component.text(chatPromptManager.lang().raw("gui.worker.browser.title"), NamedTextColor.GOLD), SIZE);
        this.workerManager = workerManager;
        this.professionManager = professionManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
        this.workers = List.copyOf(workerManager.getAll());
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        int totalPages = Math.max(1, (workers.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.max(0, Math.min(page, totalPages - 1));
        int start = page * PER_PAGE;

        for (int i = 0; i < PER_PAGE && start + i < workers.size(); i++) {

            Worker worker = workers.get(start + i);
            Profession profession = professionManager.get(worker.professionId()).orElse(null);

            setItem(i, new ItemBuilder(profession != null
                    ? ProfessionBrowserGUI.parseMaterial(profession.icon(), Material.VILLAGER_SPAWN_EGG)
                    : Material.BARRIER)
                    .setName(Component.text(
                            (worker.customName() != null ? worker.customName()
                                    : (profession != null ? profession.displayName() : worker.professionId())) + " #"
                                    + worker.id().toString().substring(0, 8),
                            NamedTextColor.YELLOW))
                    .setLore(
                            Component.text(chatPromptManager.lang().raw("gui.worker.browser.personality", "personality",
                                    worker.personality()), NamedTextColor.GRAY),
                            Component.text(chatPromptManager.lang().raw("gui.worker.browser.morale", "morale",
                                    String.format(Locale.ROOT, "%.0f", worker.morale())),
                                    NamedTextColor.GREEN),
                            worker.isEmployed() ? Component.text(chatPromptManager.lang().raw("gui.worker.browser.employed"),
                                    NamedTextColor.AQUA)
                                    : Component.text(chatPromptManager.lang().raw("gui.worker.browser.unemployed"),
                                            NamedTextColor.GRAY),
                            worker.hasActiveEvent() ? Component.text("⚠ " + worker.activeWorkerEventId(), NamedTextColor.RED)
                                    : Component.text(""))
                    .build());
        }

        if (page > 0) {
            setItem(PREV_SLOT, new ItemBuilder(Material.ARROW)
                    .setName(Component.text(chatPromptManager.lang().raw("gui.worker.browser.prev_page"), NamedTextColor.GRAY))
                    .build());
        }

        if (page < totalPages - 1) {
            setItem(NEXT_SLOT, new ItemBuilder(Material.ARROW)
                    .setName(Component.text(chatPromptManager.lang().raw("gui.worker.browser.next_page"), NamedTextColor.GRAY))
                    .build());
        }

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(chatPromptManager.lang().raw("gui.common.back")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        int index = page * PER_PAGE + slot;

        if (slot < PER_PAGE && index < workers.size()) {
            new WorkerDetailGUI(player, workers.get(index), workerManager, professionManager, chatPromptManager,
                    this::reopen).open();
        } else if (slot == PREV_SLOT) {
            page--;
            build();
        } else if (slot == NEXT_SLOT) {
            page++;
            build();
        } else if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void reopen() {
        this.workers = List.copyOf(workerManager.getAll());
        open();
    }

}
