package com.sack.rpgroll.workers.gui;

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
        super(player, Component.text("Worker #" + worker.id().toString().substring(0, 8), NamedTextColor.GOLD), SIZE);
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
                .setName(Component.text("Identidad", NamedTextColor.GOLD))
                .setLore(ItemBuilder.toLoreLines(
                        "Nombre: " + (worker.customName() != null ? worker.customName() : "(sin nombre)") + "\n"
                                + "Profesión: " + (profession != null ? profession.displayName() : worker.professionId())
                                + "\n" + "Personalidad: " + worker.personality() + "\n" + "Acción actual: "
                                + worker.currentAction()))
                .build());

        setItem(NEEDS_SLOT, new ItemBuilder(Material.GOLDEN_APPLE)
                .setName(Component.text("Necesidades", NamedTextColor.GREEN))
                .setLore(ItemBuilder.toLoreLines(String.format(Locale.ROOT,
                        "Hambre: %.0f\nEnergía: %.0f\nSueño: %.0f\nEstrés: %.0f\nMotivación: %.0f\nSalud: %.0f\n"
                                + "Felicidad: %.0f\nMoral general: %.0f",
                        worker.hunger(), worker.energy(), worker.sleep(), worker.stress(), worker.motivation(),
                        worker.health(), worker.happiness(), worker.morale())))
                .build());

        setItem(SKILLS_SLOT, new ItemBuilder(Material.BOOK)
                .setName(Component.text("Habilidades", NamedTextColor.LIGHT_PURPLE))
                .setLore(ItemBuilder.toLoreLines(formatSkills()))
                .build());

        setItem(CONTRACT_SLOT, new ItemBuilder(Material.PAPER)
                .setName(Component.text("Contrato", NamedTextColor.AQUA))
                .setLore(ItemBuilder.toLoreLines(worker.isEmployed()
                        ? "Empleador: " + shortId(worker.employerId()) + "\nSalario: " + worker.wageAmount() + " ("
                                + worker.wageType() + ")"
                        : "Sin empleador"))
                .build());

        setItem(RENAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(Component.text("Renombrar", NamedTextColor.YELLOW)).build());

        setItem(HIRE_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("▶ Contratar (salario por defecto de la profesión)", NamedTextColor.GREEN))
                .build());

        setItem(FIRE_SLOT, new ItemBuilder(Material.BARRIER)
                .setName(Component.text("Despedir", NamedTextColor.RED)).build());

        setItem(SET_HOME_SLOT, new ItemBuilder(Material.RED_BED)
                .setName(Component.text("Fijar hogar en mi ubicación", NamedTextColor.LIGHT_PURPLE)).build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    private String formatSkills() {

        if (worker.skillLevels().isEmpty()) {
            return "(sin experiencia todavía)";
        }

        StringBuilder builder = new StringBuilder();

        for (var entry : worker.skillLevels().entrySet()) {
            builder.append(entry.getKey()).append(": nivel ").append(entry.getValue()).append("\n");
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
            chatPromptManager.prompt(player, "Escribí el nuevo nombre del worker:", value -> {
                worker.setCustomName(value);
                workerManager.save(worker);
                build();
            });
        } else if (slot == HIRE_SLOT) {

            if (!canManage(player)) {
                player.sendMessage(Component.text("Ya tiene otro empleador.", NamedTextColor.RED));
                return;
            }

            Profession profession = professionManager.get(worker.professionId()).orElse(null);
            double wage = profession != null ? profession.wageAmount() : 0;
            WageType wageType = profession != null ? profession.wageType() : WageType.PER_TASK;

            worker.hire(player.getUniqueId(), wage, wageType);
            workerManager.save(worker);
            player.sendMessage(Component.text("✔ Contratado.", NamedTextColor.GREEN));
            build();

        } else if (slot == FIRE_SLOT) {

            if (!canManage(player)) {
                player.sendMessage(Component.text("No podés despedir el worker de otro jugador.", NamedTextColor.RED));
                return;
            }

            worker.fire();
            workerManager.save(worker);
            player.sendMessage(Component.text("Despedido.", NamedTextColor.YELLOW));
            build();

        } else if (slot == SET_HOME_SLOT) {

            worker.setHomeLocation(player.getLocation());
            workerManager.save(worker);
            player.sendMessage(Component.text("✔ Hogar fijado en tu ubicación.", NamedTextColor.GREEN));
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
