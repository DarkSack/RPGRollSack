package com.sack.rpgroll.mobs.gui.editor;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.mobs.core.AIBehaviorDef;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Editor de comportamiento: objetivos de IA (en orden de prioridad),
 * rango de aggro, % de huida, velocidad, puntos de patrulla y región a
 * proteger.
 */
public class AIEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final String[] AVAILABLE_GOALS = {
            "ATTACK_PLAYERS", "FLEE", "PATROL", "GUARD_REGION", "DEFEND_ALLY", "FOLLOW_LEADER", "STAY_STILL"
    };

    private static final int GOALS_START_SLOT = 0;
    private static final int AGGRO_RANGE_SLOT = 9;
    private static final int FLEE_HEALTH_SLOT = 10;
    private static final int MOVE_SPEED_SLOT = 11;
    private static final int GUARD_REGION_SLOT = 12;

    private static final int PATROL_START_SLOT = 18;
    private static final int ADD_PATROL_SLOT = 26;

    private static final int BACK_SLOT = 44;

    private final MobEditorSession session;
    private final Runnable onBack;

    public AIEditorGUI(Player player, MobEditorSession session, Runnable onBack) {
        super(player, Component.text("IA: " + session.original.id(), NamedTextColor.GOLD), SIZE);
        this.session = session;
        this.onBack = onBack;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        AIBehaviorDef ai = session.ai;

        for (int i = 0; i < AVAILABLE_GOALS.length; i++) {

            String goal = AVAILABLE_GOALS[i];
            int priority = ai.goals().indexOf(goal);

            setItem(GOALS_START_SLOT + i, new ItemBuilder(priority >= 0 ? Material.COMPASS : Material.GRAY_DYE)
                    .setName(Component.text(goal + (priority >= 0 ? " (#" + (priority + 1) + ")" : ""),
                            priority >= 0 ? NamedTextColor.GREEN : NamedTextColor.GRAY))
                    .setLore(Component.text("Click: agregar al final de la prioridad", NamedTextColor.GRAY),
                            Component.text("Shift-click: quitar", NamedTextColor.GRAY))
                    .build());
        }

        setItem(AGGRO_RANGE_SLOT, new ItemBuilder(Material.SPYGLASS)
                .setName(Component.text("Rango de aggro: " + ai.aggroRange(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +1 · Shift-click: +5", NamedTextColor.GRAY),
                        Component.text("Click derecho: -1 · Shift-click derecho: -5", NamedTextColor.GRAY))
                .build());

        setItem(FLEE_HEALTH_SLOT, new ItemBuilder(Material.FEATHER)
                .setName(Component.text("% de vida para huir: " + ai.fleeHealthPercent(), NamedTextColor.YELLOW))
                .setLore(Component.text("0 = nunca huye", NamedTextColor.DARK_GRAY),
                        Component.text("Click: +5 · Shift-click: +25 · Derecho: -5/-25", NamedTextColor.GRAY))
                .build());

        setItem(MOVE_SPEED_SLOT, new ItemBuilder(Material.SUGAR)
                .setName(Component.text("Velocidad: " + ai.moveSpeed(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +0.1 · Shift-click: +0.5", NamedTextColor.GRAY),
                        Component.text("Click derecho: -0.1 · Shift-click derecho: -0.5", NamedTextColor.GRAY))
                .build());

        setItem(GUARD_REGION_SLOT, new ItemBuilder(Material.OAK_FENCE_GATE)
                .setName(Component.text("Región a proteger: "
                        + (ai.guardRegionId() != null ? ai.guardRegionId() : "(ninguna)"), NamedTextColor.YELLOW))
                .setLore(Component.text("Click: escribir el id de región", NamedTextColor.GRAY),
                        Component.text("Shift-click: quitar", NamedTextColor.GRAY))
                .build());

        for (int i = 0; i < ai.patrolPoints().size() && i < 8; i++) {
            setItem(PATROL_START_SLOT + i, new ItemBuilder(Material.MAP)
                    .setName(Component.text("Punto " + (i + 1) + ": " + ai.patrolPoints().get(i),
                            NamedTextColor.AQUA))
                    .setLore(Component.text("Shift-click para quitar", NamedTextColor.DARK_GRAY))
                    .build());
        }

        setItem(ADD_PATROL_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Agregar punto de patrulla", NamedTextColor.GREEN))
                .setLore(Component.text("Click para escribir: x,y,z", NamedTextColor.GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < AVAILABLE_GOALS.length) {
            toggleGoal(AVAILABLE_GOALS[slot], event.isShiftClick());
            return;
        }

        if (slot == AGGRO_RANGE_SLOT) {
            double updated = Math.max(1, session.ai.aggroRange() + delta(event.getClick()));
            session.ai = withAggroRange(session.ai, updated);
            build();
            return;
        }

        if (slot == FLEE_HEALTH_SLOT) {
            double updated = Math.max(0, Math.min(100, session.ai.fleeHealthPercent() + delta(event.getClick()) * 5));
            session.ai = withFleeHealthPercent(session.ai, updated);
            build();
            return;
        }

        if (slot == MOVE_SPEED_SLOT) {
            double updated = Math.max(0.1, session.ai.moveSpeed() + delta(event.getClick()) / 10.0);
            session.ai = withMoveSpeed(session.ai, updated);
            build();
            return;
        }

        if (slot == GUARD_REGION_SLOT) {
            if (event.isShiftClick()) {
                session.ai = withGuardRegionId(session.ai, null);
                build();
                return;
            }
            session.chatPromptManager.prompt(player, "Escribí el id de región:", value -> {
                session.ai = withGuardRegionId(session.ai, value.trim());
                build();
            });
            return;
        }

        if (slot >= PATROL_START_SLOT && slot < PATROL_START_SLOT + Math.min(session.ai.patrolPoints().size(), 8)) {
            if (event.isShiftClick()) {
                List<String> points = new ArrayList<>(session.ai.patrolPoints());
                points.remove(slot - PATROL_START_SLOT);
                session.ai = withPatrolPoints(session.ai, points);
                build();
            }
            return;
        }

        if (slot == ADD_PATROL_SLOT) {
            promptAddPatrolPoint();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void toggleGoal(String goal, boolean shiftClick) {

        List<String> goals = new ArrayList<>(session.ai.goals());

        if (shiftClick) {
            goals.remove(goal);
        } else if (!goals.contains(goal)) {
            goals.add(goal);
        }

        session.ai = withGoals(session.ai, goals);
        build();
    }

    private void promptAddPatrolPoint() {
        session.chatPromptManager.prompt(player, "Escribí las coordenadas: x,y,z", value -> {

            String[] parts = value.trim().split(",");

            if (parts.length != 3) {
                player.sendMessage(Component.text("Formato inválido.", NamedTextColor.RED));
                return;
            }

            try {
                Double.parseDouble(parts[0].trim());
                Double.parseDouble(parts[1].trim());
                Double.parseDouble(parts[2].trim());
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("Coordenadas numéricas inválidas.", NamedTextColor.RED));
                return;
            }

            List<String> points = new ArrayList<>(session.ai.patrolPoints());
            points.add(value.trim());
            session.ai = withPatrolPoints(session.ai, points);

            build();
        });
    }

    private double delta(ClickType click) {
        return switch (click) {
            case LEFT -> 1;
            case SHIFT_LEFT -> 5;
            case RIGHT -> -1;
            case SHIFT_RIGHT -> -5;
            default -> 0;
        };
    }

    private AIBehaviorDef withGoals(AIBehaviorDef ai, List<String> goals) {
        return new AIBehaviorDef(goals, ai.aggroRange(), ai.fleeHealthPercent(), ai.moveSpeed(), ai.patrolPoints(),
                ai.guardRegionId());
    }

    private AIBehaviorDef withAggroRange(AIBehaviorDef ai, double aggroRange) {
        return new AIBehaviorDef(ai.goals(), aggroRange, ai.fleeHealthPercent(), ai.moveSpeed(), ai.patrolPoints(),
                ai.guardRegionId());
    }

    private AIBehaviorDef withFleeHealthPercent(AIBehaviorDef ai, double fleeHealthPercent) {
        return new AIBehaviorDef(ai.goals(), ai.aggroRange(), fleeHealthPercent, ai.moveSpeed(), ai.patrolPoints(),
                ai.guardRegionId());
    }

    private AIBehaviorDef withMoveSpeed(AIBehaviorDef ai, double moveSpeed) {
        return new AIBehaviorDef(ai.goals(), ai.aggroRange(), ai.fleeHealthPercent(), moveSpeed, ai.patrolPoints(),
                ai.guardRegionId());
    }

    private AIBehaviorDef withPatrolPoints(AIBehaviorDef ai, List<String> patrolPoints) {
        return new AIBehaviorDef(ai.goals(), ai.aggroRange(), ai.fleeHealthPercent(), ai.moveSpeed(), patrolPoints,
                ai.guardRegionId());
    }

    private AIBehaviorDef withGuardRegionId(AIBehaviorDef ai, String guardRegionId) {
        return new AIBehaviorDef(ai.goals(), ai.aggroRange(), ai.fleeHealthPercent(), ai.moveSpeed(),
                ai.patrolPoints(), guardRegionId);
    }

}
