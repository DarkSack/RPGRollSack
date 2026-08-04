package com.sack.rpgroll.dungeons.gui.editor;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.dungeons.core.CheckpointMode;
import com.sack.rpgroll.dungeons.core.DungeonCheckpointPolicy;
import com.sack.rpgroll.dungeons.core.DungeonReviveConfig;
import com.sack.rpgroll.dungeons.core.ReviveMode;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

/** Política de checkpoints (qué pasa al perder el grupo entero) y configuración de revivir individual. */
public class CheckpointsReviveEditorGUI extends InventoryGUI {

    private static final int SIZE = 27;

    private static final int CHECKPOINT_MODE_SLOT = 9;
    private static final int SHARED_LIVES_SLOT = 10;
    private static final int MAX_RETRIES_SLOT = 11;

    private static final int REVIVE_MODE_SLOT = 14;
    private static final int REVIVE_TIMER_SLOT = 15;
    private static final int REVIVE_ITEM_SLOT = 16;

    private static final int BACK_SLOT = 22;

    private final DungeonEditorSession session;
    private final Runnable onBack;

    public CheckpointsReviveEditorGUI(Player player, DungeonEditorSession session, Runnable onBack) {
        super(player, Component.text("Checkpoints/Revivir: " + session.original.id(), NamedTextColor.GOLD), SIZE);
        this.session = session;
        this.onBack = onBack;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        DungeonCheckpointPolicy policy = session.checkpointPolicy;
        DungeonReviveConfig revive = session.reviveConfig;

        setItem(CHECKPOINT_MODE_SLOT, new ItemBuilder(Material.RESPAWN_ANCHOR)
                .setName(Component.text("Al perder el grupo: " + policy.mode(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click para pasar al siguiente modo", NamedTextColor.GRAY))
                .build());

        setItem(SHARED_LIVES_SLOT, new ItemBuilder(policy.sharedLives() ? Material.TOTEM_OF_UNDYING : Material.BONE)
                .setName(Component.text("Vida compartida: " + policy.sharedLives(), NamedTextColor.YELLOW))
                .setLore(Component.text("Si está activa, cada muerte individual descuenta del pool de reintentos",
                        NamedTextColor.GRAY),
                        Component.text("Click para alternar", NamedTextColor.GRAY))
                .build());

        setItem(MAX_RETRIES_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(Component.text("Reintentos máximos: "
                        + (policy.unlimitedRetries() ? "ilimitados" : policy.maxRetries()), NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +1 · Derecho: -1 (por debajo de 0 = ilimitado)", NamedTextColor.GRAY))
                .build());

        setItem(REVIVE_MODE_SLOT, new ItemBuilder(Material.GOLDEN_APPLE)
                .setName(Component.text("Revivir: " + revive.mode(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click para pasar al siguiente modo", NamedTextColor.GRAY))
                .build());

        setItem(REVIVE_TIMER_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(Component.text("Temporizador: " + (revive.reviveTimerMillis() / 1000) + "s",
                        NamedTextColor.YELLOW))
                .setLore(Component.text("Usado por el modo AFTER_TIMER", NamedTextColor.DARK_GRAY),
                        Component.text("Click: +5s · Shift-click: +30s · Derecho: -5s/-30s", NamedTextColor.GRAY))
                .build());

        setItem(REVIVE_ITEM_SLOT, new ItemBuilder(Material.TOTEM_OF_UNDYING)
                .setName(Component.text("Ítem requerido: "
                        + (revive.itemMaterial() != null ? revive.itemMaterial() : "(ninguno)"), NamedTextColor.YELLOW))
                .setLore(Component.text("Usado por el modo WITH_ITEM", NamedTextColor.DARK_GRAY),
                        Component.text("Click: escribir · Shift-click: quitar", NamedTextColor.GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        DungeonCheckpointPolicy policy = session.checkpointPolicy;
        DungeonReviveConfig revive = session.reviveConfig;

        if (slot == CHECKPOINT_MODE_SLOT) {
            session.checkpointPolicy = new DungeonCheckpointPolicy(nextEnum(CheckpointMode.values(), policy.mode()),
                    policy.sharedLives(), policy.maxRetries());
            build();
            return;
        }

        if (slot == SHARED_LIVES_SLOT) {
            session.checkpointPolicy = new DungeonCheckpointPolicy(policy.mode(), !policy.sharedLives(),
                    policy.maxRetries());
            build();
            return;
        }

        if (slot == MAX_RETRIES_SLOT) {
            int updated = policy.maxRetries() + (int) delta(event.getClick(), 1, 1);
            session.checkpointPolicy = new DungeonCheckpointPolicy(policy.mode(), policy.sharedLives(), updated);
            build();
            return;
        }

        if (slot == REVIVE_MODE_SLOT) {
            session.reviveConfig = new DungeonReviveConfig(nextEnum(ReviveMode.values(), revive.mode()),
                    revive.reviveTimerMillis(), revive.itemMaterial());
            build();
            return;
        }

        if (slot == REVIVE_TIMER_SLOT) {
            long deltaMillis = (long) delta(event.getClick(), 5, 30) * 1000L;
            long updated = Math.max(0, revive.reviveTimerMillis() + deltaMillis);
            session.reviveConfig = new DungeonReviveConfig(revive.mode(), updated, revive.itemMaterial());
            build();
            return;
        }

        if (slot == REVIVE_ITEM_SLOT) {
            if (event.isShiftClick()) {
                session.reviveConfig = new DungeonReviveConfig(revive.mode(), revive.reviveTimerMillis(), null);
                build();
                return;
            }
            session.chatPromptManager.prompt(player, "Escribí el Material requerido:", value -> {
                DungeonReviveConfig current = session.reviveConfig;
                session.reviveConfig = new DungeonReviveConfig(current.mode(), current.reviveTimerMillis(),
                        value.trim().toUpperCase());
                build();
            });
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private double delta(ClickType click, double small, double large) {
        return switch (click) {
            case LEFT -> small;
            case SHIFT_LEFT -> large;
            case RIGHT -> -small;
            case SHIFT_RIGHT -> -large;
            default -> 0;
        };
    }

    private <T extends Enum<T>> T nextEnum(T[] values, T current) {
        return values[(current.ordinal() + 1) % values.length];
    }

}
