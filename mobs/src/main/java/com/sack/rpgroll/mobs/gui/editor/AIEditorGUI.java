package com.sack.rpgroll.mobs.gui.editor;

import com.sack.rpgroll.common.lang.LangManager;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.mobs.core.AIBehaviorDef;

import net.kyori.adventure.text.Component;

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
    private final LangManager lang;

    public AIEditorGUI(Player player, MobEditorSession session, Runnable onBack) {
        super(player, session.chatPromptManager.lang().component("gui.ai.title", "id", session.original.id()),
                SIZE);
        this.session = session;
        this.onBack = onBack;
        this.lang = session.chatPromptManager.lang();
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

            Component name = priority >= 0
                    ? lang.component("gui.ai.goal_active", "goal", goal, "priority", priority + 1)
                    : lang.component("gui.ai.goal_inactive", "goal", goal);

            setItem(GOALS_START_SLOT + i, new ItemBuilder(priority >= 0 ? Material.COMPASS : Material.GRAY_DYE)
                    .setName(name)
                    .setLore(lang.component("gui.ai.goal_hint1"),
                            lang.component("gui.common.shift_remove_hint"))
                    .build());
        }

        setItem(AGGRO_RANGE_SLOT, new ItemBuilder(Material.SPYGLASS)
                .setName(lang.component("gui.ai.aggro_range_label", "value", ai.aggroRange()))
                .setLore(lang.component("gui.ai.aggro_hint1"),
                        lang.component("gui.ai.aggro_hint2"))
                .build());

        setItem(FLEE_HEALTH_SLOT, new ItemBuilder(Material.FEATHER)
                .setName(lang.component("gui.ai.flee_label", "value", ai.fleeHealthPercent()))
                .setLore(lang.component("gui.ai.flee_note"),
                        lang.component("gui.ai.flee_hint"))
                .build());

        setItem(MOVE_SPEED_SLOT, new ItemBuilder(Material.SUGAR)
                .setName(lang.component("gui.ai.speed_label", "value", ai.moveSpeed()))
                .setLore(lang.component("gui.ai.speed_hint1"),
                        lang.component("gui.ai.speed_hint2"))
                .build());

        setItem(GUARD_REGION_SLOT, new ItemBuilder(Material.OAK_FENCE_GATE)
                .setName(lang.component("gui.ai.guard_region_label", "value",
                        ai.guardRegionId() != null ? ai.guardRegionId() : lang.raw("gui.phase_edit.none_feminine")))
                .setLore(lang.component("gui.ai.guard_region_hint"),
                        lang.component("gui.common.shift_remove_hint"))
                .build());

        for (int i = 0; i < ai.patrolPoints().size() && i < 8; i++) {
            setItem(PATROL_START_SLOT + i, new ItemBuilder(Material.MAP)
                    .setName(lang.component("gui.ai.patrol_point_label", "index", i + 1, "point",
                            ai.patrolPoints().get(i)))
                    .setLore(lang.component("gui.common.shift_remove_dark"))
                    .build());
        }

        setItem(ADD_PATROL_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("gui.ai.add_patrol"))
                .setLore(lang.component("gui.ai.add_patrol_hint"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back_button")));
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
            session.chatPromptManager.prompt(player, "gui.common.prompt_region_id", value -> {
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
        session.chatPromptManager.prompt(player, "gui.ai.prompt_coords", value -> {

            String[] parts = value.trim().split(",");

            if (parts.length != 3) {
                lang.send(player, "gui.common.invalid_format");
                return;
            }

            try {
                Double.parseDouble(parts[0].trim());
                Double.parseDouble(parts[1].trim());
                Double.parseDouble(parts[2].trim());
            } catch (NumberFormatException e) {
                lang.send(player, "gui.ai.invalid_coords");
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
