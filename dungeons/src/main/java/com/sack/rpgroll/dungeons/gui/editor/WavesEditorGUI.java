package com.sack.rpgroll.dungeons.gui.editor;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.dungeons.core.DungeonWave;
import com.sack.rpgroll.dungeons.core.DungeonWaveMob;
import com.sack.rpgroll.dungeons.util.DurationParser;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Editor de la lista de oleadas de una sala de combate (lista compartida
 * por referencia con {@code RoomEditGUI}). Alta por chat:
 * {@code id tiempoLímite tiempoAntes mob1:cantidad,mob2:cantidad} — ej.
 * {@code wave1 30s 5s skeleton_archer:3,zombie_grunt:2} (tiempos en 0
 * para "sin límite"/"sin demora").
 */
public class WavesEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int ADD_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private static final String FORMAT_HINT =
            "id tiempoLimite tiempoAntes mob1:cantidad,mob2:cantidad — ej. wave1 30s 5s skeleton_archer:3,zombie_grunt:2"
                    + " (0 = sin límite/demora)";

    private final DungeonEditorSession session;
    private final List<DungeonWave> waves;
    private final Runnable onBack;

    public WavesEditorGUI(Player player, DungeonEditorSession session, List<DungeonWave> waves, Runnable onBack) {
        super(player, Component.text("Oleadas", NamedTextColor.GOLD), SIZE);
        this.session = session;
        this.waves = waves;
        this.onBack = onBack;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < waves.size() && i < 36; i++) {

            DungeonWave wave = waves.get(i);

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("mobs: " + wave.totalMobCount(), NamedTextColor.GRAY));
            for (DungeonWaveMob mob : wave.mobs()) {
                lore.add(Component.text("  " + mob.mobId() + " x" + mob.amount(), NamedTextColor.DARK_GRAY));
            }
            lore.add(Component.text("tiempo límite: " + (wave.timeLimitMillis() / 1000) + "s", NamedTextColor.GRAY));
            lore.add(Component.text("Shift-click para quitar", NamedTextColor.DARK_GRAY));

            setItem(i, new ItemBuilder(Material.SPAWNER)
                    .setName(Component.text(wave.id(), NamedTextColor.YELLOW))
                    .setLore(lore)
                    .build());
        }

        setItem(ADD_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Agregar oleada", NamedTextColor.GREEN))
                .setLore(Component.text(FORMAT_HINT, NamedTextColor.GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < waves.size() && slot < 36) {
            if (event.isShiftClick()) {
                waves.remove(slot);
                build();
            }
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
        session.chatPromptManager.prompt(player, "Escribí: " + FORMAT_HINT, value -> {

            String[] tokens = value.trim().split("\\s+");

            if (tokens.length < 3) {
                player.sendMessage(Component.text("Formato inválido.", NamedTextColor.RED));
                return;
            }

            String id = tokens[0];
            long timeLimitMillis = DurationParser.parseMillis(tokens[1]);
            long delayBeforeMillis = DurationParser.parseMillis(tokens[2]);

            List<DungeonWaveMob> mobs = new ArrayList<>();

            if (tokens.length >= 4) {
                for (String entry : tokens[3].split(",")) {

                    String[] parts = entry.split(":", 2);
                    if (parts.length != 2) {
                        continue;
                    }

                    try {
                        mobs.add(new DungeonWaveMob(parts[0], Integer.parseInt(parts[1])));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }

            waves.add(new DungeonWave(id, mobs, timeLimitMillis, delayBeforeMillis));
            build();
        });
    }

}
