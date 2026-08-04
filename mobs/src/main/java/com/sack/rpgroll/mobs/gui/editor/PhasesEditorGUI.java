package com.sack.rpgroll.mobs.gui.editor;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.mobs.core.MobPhase;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Lista de fases de jefe — click abre el editor de esa fase, shift-click la elimina. */
public class PhasesEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int ADD_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final MobEditorSession session;
    private final Runnable onBack;

    public PhasesEditorGUI(Player player, MobEditorSession session, Runnable onBack) {
        super(player, Component.text("Fases: " + session.original.id(), NamedTextColor.GOLD), SIZE);
        this.session = session;
        this.onBack = onBack;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        List<MobPhase> phases = session.phases;

        for (int i = 0; i < phases.size() && i < 36; i++) {

            MobPhase phase = phases.get(i);

            setItem(i, new ItemBuilder(Material.NETHER_STAR)
                    .setName(Component.text(phase.id() + " (≤" + phase.healthThresholdPercent() + "% vida)",
                            NamedTextColor.YELLOW))
                    .setLore(
                            Component.text("skills: " + phase.skills().size() + " · multiplicadores: "
                                    + phase.statMultipliers().size(), NamedTextColor.GRAY),
                            Component.text("Click para editar · Shift-click para quitar", NamedTextColor.DARK_GRAY))
                    .build());
        }

        setItem(ADD_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Agregar fase", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < session.phases.size() && slot < 36) {

            if (event.isShiftClick()) {
                List<MobPhase> updated = new ArrayList<>(session.phases);
                updated.remove(slot);
                session.phases = updated;
                build();
                return;
            }

            new PhaseEditGUI(player, session, slot, this::reopen).open();
            return;
        }

        if (slot == ADD_SLOT) {
            promptAdd();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void promptAdd() {
        session.chatPromptManager.prompt(player, "Escribí el id de la nueva fase:", value -> {

            String id = value.trim().toLowerCase().replace(' ', '_');

            List<MobPhase> updated = new ArrayList<>(session.phases);
            updated.add(new MobPhase(id, 50.0, Map.of(), List.of(), null, null, null, null));
            session.phases = updated;

            build();
        });
    }

    private void reopen() {
        new PhasesEditorGUI(player, session, onBack).open();
    }

}
