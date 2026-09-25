package com.sack.rpgroll.tab.nametag;

import com.sack.rpgroll.tab.placeholder.PlaceholderEngine;
import com.sack.rpgroll.util.ComponentUtils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
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
 * desplazadas verticalmente con {@link Transformation} — sin ProtocolLib.
 * <p>
 * Los displays NO van montados como pasajeros del jugador: se mueven con él
 * desde {@link #follow()}, un tick sí y otro también. Montarlos era más
 * cómodo, pero Paper rechaza cualquier teletransporte de plugin a otro mundo
 * de un jugador que lleve pasajeros ({@code CraftPlayer#teleport0} devuelve
 * {@code false} antes incluso de lanzar el evento), así que un nametag
 * montado rompía {@code /spawn}, {@code /home}, el RTP y los portales de
 * Multiverse para todo el servidor, sin error y sin plugin al que culpar.
 * El teletransporte vanilla sí funcionaba, porque desmonta él solo.
 * <p>
 * El cambio de mundo lo resuelve {@code PlayerChangedWorldEvent}, que ya
 * vuelve a aplicar el nametag: los displays viejos se borran y salen nuevos
 * en el mundo de destino.
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

    /** Ticks que el cliente tarda en interpolar cada salto: sin esto el texto va a tirones. */
    private static final int FOLLOW_INTERPOLATION_TICKS = 2;

    private BukkitTask followTask;

    public NametagEngine(Plugin plugin, PlaceholderEngine placeholderEngine) {
        this.plugin = plugin;
        this.placeholderEngine = placeholderEngine;
    }

    public void start() {

        if (followTask == null) {
            followTask = Bukkit.getScheduler().runTaskTimer(plugin, this::follow, 1L, 1L);
        }
    }

    public void stop() {

        if (followTask != null) {
            followTask.cancel();
            followTask = null;
        }

        for (UUID id : List.copyOf(baseDisplays.keySet())) {
            removeAll(baseDisplays.remove(id));
        }

        for (UUID id : List.copyOf(staffDisplays.keySet())) {
            removeAll(staffDisplays.remove(id));
        }
    }

    /** Lleva cada display a la cabeza de su jugador. Solo teletransporta si el jugador se ha movido. */
    private void follow() {

        followAll(baseDisplays);
        followAll(staffDisplays);
    }

    private void followAll(Map<UUID, List<TextDisplay>> displays) {

        for (Map.Entry<UUID, List<TextDisplay>> entry : displays.entrySet()) {

            Player subject = Bukkit.getPlayer(entry.getKey());

            if (subject == null) {
                continue;
            }

            Location anchor = anchor(subject);

            for (TextDisplay display : entry.getValue()) {

                if (!display.isValid()) {
                    continue;
                }

                // Entre el teletransporte y el PlayerChangedWorldEvent hay un
                // instante en que jugador y display están en mundos distintos;
                // ahí no se mueve nada, el evento los reemplaza enseguida.
                if (display.getWorld() != anchor.getWorld()) {
                    continue;
                }

                if (display.getLocation().distanceSquared(anchor) > 1.0E-4) {
                    display.teleport(anchor);
                }
            }
        }
    }

    /**
     * Donde se apoya el texto: sobre la cabeza, a la altura en que el jugador
     * llevaría un pasajero. Usa la altura real de la caja, así que al
     * agacharse o nadar el nombre baja con él.
     */
    private static Location anchor(Player subject) {

        Location location = subject.getLocation();
        location.add(0, subject.getHeight(), 0);
        location.setPitch(0f);
        return location;
    }

    public void apply(Player subject, NametagDefinition definition) {

        clear(subject);

        List<TextDisplay> base = spawnLines(subject, definition.lines(), 0);
        baseDisplays.put(subject.getUniqueId(), base);

        if (!definition.hasStaffOverride()) {
            hideFromSelf(subject, base);
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

        hideFromSelf(subject, base);
        hideFromSelf(subject, staff);
    }

    /**
     * El jugador no ve su propio nametag, igual que en vanilla. Montado como
     * pasajero quedaba siempre sobre la cámara; siguiéndolo por tick va un
     * pelo por detrás y se le cruzaría por delante al correr.
     */
    private void hideFromSelf(Player subject, List<TextDisplay> displays) {

        for (TextDisplay display : displays) {
            subject.hideEntity(plugin, display);
        }
    }

    /** Actualiza qué variante (base o staff) ve un viewer específico de este subject — llamar en join/permiso cambiado. */
    public void refreshViewer(Player subject, NametagDefinition definition, Player viewer) {

        if (!definition.hasStaffOverride()) {
            return;
        }

        if (viewer.equals(subject)) {
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

            TextDisplay display = subject.getWorld().spawn(anchor(subject), TextDisplay.class, entity -> {
                entity.setBillboard(Display.Billboard.CENTER);
                entity.setPersistent(false);
                entity.setInvulnerable(true);
                entity.setSeeThrough(false);
                entity.setTeleportDuration(FOLLOW_INTERPOLATION_TICKS);
                entity.text(ComponentUtils.parse(rendered));
                entity.setTransformation(new Transformation(
                        new Vector3f(0f, (float) yOffset, 0f),
                        new Quaternionf(),
                        new Vector3f(1f, 1f, 1f),
                        new Quaternionf()));
            });

            entities.add(display);
        }

        return entities;
    }

}
