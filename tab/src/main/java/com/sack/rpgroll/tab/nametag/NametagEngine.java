package com.sack.rpgroll.tab.nametag;

import com.sack.rpgroll.tab.placeholder.PlaceholderEngine;
import com.sack.rpgroll.util.ComponentUtils;

import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Nametag multi-línea vía entidades {@link TextDisplay} nativas de Paper,
 * montadas directamente como pasajero del jugador (siguen su posición sin
 * necesidad de reteleportar en cada movimiento) y desplazadas verticalmente
 * con {@link Transformation} — sin ProtocolLib.
 * <p>
 * El override "staff ve algo distinto" (sección 11) usa
 * {@link org.bukkit.entity.Entity#setVisibleByDefault(boolean)} +
 * {@link Player#showEntity}/{@link Player#hideEntity} — también 100% nativo.
 */
public class NametagEngine {

    private static final double LINE_HEIGHT = 0.28;
    private static final double BASE_OFFSET = 0.35;

    private final Plugin plugin;
    private final PlaceholderEngine placeholderEngine;
    private final Map<UUID, List<TextDisplay>> baseDisplays = new ConcurrentHashMap<>();
    private final Map<UUID, List<TextDisplay>> staffDisplays = new ConcurrentHashMap<>();

    public NametagEngine(Plugin plugin, PlaceholderEngine placeholderEngine) {
        this.plugin = plugin;
        this.placeholderEngine = placeholderEngine;
    }

    public void apply(Player subject, NametagDefinition definition) {

        clear(subject);

        List<TextDisplay> base = spawnLines(subject, definition.lines(), 0);
        baseDisplays.put(subject.getUniqueId(), base);

        if (!definition.hasStaffOverride()) {
            return;
        }

        List<TextDisplay> staff = spawnLines(subject, definition.staffLines(), 0);

        for (TextDisplay display : staff) {
            display.setVisibleByDefault(false);
        }

        staffDisplays.put(subject.getUniqueId(), staff);

        for (Player viewer : subject.getWorld().getPlayers()) {
            refreshViewer(subject, definition, viewer);
        }
    }

    /** Actualiza qué variante (base o staff) ve un viewer específico de este subject — llamar en join/permiso cambiado. */
    public void refreshViewer(Player subject, NametagDefinition definition, Player viewer) {

        if (!definition.hasStaffOverride()) {
            return;
        }

        boolean isStaff = viewer.hasPermission(definition.staffOverridePermission());

        List<TextDisplay> base = baseDisplays.get(subject.getUniqueId());
        List<TextDisplay> staff = staffDisplays.get(subject.getUniqueId());

        if (base == null || staff == null) {
            return;
        }

        for (TextDisplay display : base) {
            if (isStaff) {
                viewer.hideEntity(plugin, display);
            } else {
                viewer.showEntity(plugin, display);
            }
        }

        for (TextDisplay display : staff) {
            if (isStaff) {
                viewer.showEntity(plugin, display);
            } else {
                viewer.hideEntity(plugin, display);
            }
        }
    }

    public void clear(Player subject) {

        List<TextDisplay> base = baseDisplays.remove(subject.getUniqueId());
        List<TextDisplay> staff = staffDisplays.remove(subject.getUniqueId());

        removeAll(base);
        removeAll(staff);
    }

    private void removeAll(List<TextDisplay> displays) {

        if (displays == null) {
            return;
        }

        for (TextDisplay display : displays) {
            display.remove();
        }
    }

    private List<TextDisplay> spawnLines(Player subject, List<String> lines, double extraOffset) {

        List<TextDisplay> entities = new ArrayList<>();
        int count = lines.size();

        for (int i = 0; i < count; i++) {

            double yOffset = BASE_OFFSET + extraOffset + (count - i) * LINE_HEIGHT;
            String rendered = placeholderEngine.resolve(lines.get(i), subject);

            TextDisplay display = subject.getWorld().spawn(subject.getLocation(), TextDisplay.class, entity -> {
                entity.setBillboard(Display.Billboard.CENTER);
                entity.setPersistent(false);
                entity.setInvulnerable(true);
                entity.setSeeThrough(false);
                entity.text(ComponentUtils.parse(rendered));
                entity.setTransformation(new Transformation(
                        new Vector3f(0f, (float) yOffset, 0f),
                        new Quaternionf(),
                        new Vector3f(1f, 1f, 1f),
                        new Quaternionf()));
            });

            subject.addPassenger(display);
            entities.add(display);
        }

        return entities;
    }

}
