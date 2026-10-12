package com.sack.rpgroll.traps.turret;

import com.sack.rpgroll.common.lang.LangManager;

import com.destroystokyo.paper.event.player.PlayerStopSpectatingEntityEvent;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mirar por una cámara: el jugador pasa a espectador pegado a la cabeza de la cámara y, al salir
 * (agacharse, desconectarse, que rompan la cámara, cualquier teletransporte), vuelve a donde
 * estaba y a su modo de juego.
 * <p>
 * A dónde volver se guarda en el propio jugador y no en memoria: si el servidor se cae con alguien
 * mirando, al entrar de nuevo se le devuelve en vez de dejarlo en espectador.
 */
public final class CameraView implements Listener {

    /** {@code impact.type} de la torreta que avisa en vez de disparar, y del bloque desde el que se miran. */
    public static final String CAMERA = "CAMERA";
    public static final String MONITOR = "MONITOR";

    private final Plugin plugin;
    private final TurretEngine engine;
    private final LangManager lang;
    private final NamespacedKey originKey;
    private final Map<UUID, Long> lastCombat = new HashMap<>();

    /** Tras dar o recibir un golpe no se mira por una cámara: desaparecer del sitio sería huir de la pelea. */
    private static final long COMBAT_MILLIS = 15_000;

    public CameraView(Plugin plugin, TurretEngine engine, LangManager lang) {
        this.plugin = plugin;
        this.engine = engine;
        this.lang = lang;
        this.originKey = new NamespacedKey(plugin, "camera-origin");
    }

    /** De dónde salió quien mira, tal como se guarda en su PersistentDataContainer. */
    record Origin(String world, double x, double y, double z, float yaw, float pitch, GameMode mode) {

        String encode() {
            return world + ";" + x + ";" + y + ";" + z + ";" + yaw + ";" + pitch + ";" + mode.name();
        }

        /** null si el texto no es de los nuestros: quien lo lea decide a dónde mandar al jugador. */
        static Origin decode(String raw) {

            String[] parts = raw == null ? new String[0] : raw.split(";");

            if (parts.length != 7) {
                return null;
            }

            try {
                return new Origin(parts[0], Double.parseDouble(parts[1]), Double.parseDouble(parts[2]),
                        Double.parseDouble(parts[3]), Float.parseFloat(parts[4]), Float.parseFloat(parts[5]),
                        GameMode.valueOf(parts[6]));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    /** Lo que escribe el dueño, sin códigos de formato y a 24 letras como mucho; null si no queda nada. */
    public static String cleanName(String raw) {

        String name = raw == null ? "" : raw.replaceAll("[&§][0-9a-fk-orA-FK-OR]?|[<>{}]", "").trim();

        return name.isEmpty() ? null : name.substring(0, Math.min(name.length(), 24)).trim();
    }

    /** true, y se lo dice, si acaba de pelear. */
    public boolean inCombat(Player player) {

        long left = COMBAT_MILLIS - (System.currentTimeMillis() - lastCombat.getOrDefault(player.getUniqueId(), 0L));

        if (left <= 0) {
            return false;
        }

        lang.send(player, "camera.in_combat", "seconds", (left + 999) / 1000);
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {

        long now = System.currentTimeMillis();
        Object attacker = event.getDamager() instanceof Projectile projectile ? projectile.getShooter() : event.getDamager();

        if (event.getEntity() instanceof Player victim) {
            lastCombat.put(victim.getUniqueId(), now);
        }

        if (attacker instanceof Player player) {
            lastCombat.put(player.getUniqueId(), now);
        }
    }

    public void watch(Player player, PlacedTurret camera) {

        if (inCombat(player)) {
            return;
        }

        Location post = camera.location();

        // shortcut: solo cámaras del mundo en el que estás; entre mundos Multiverse repone el modo de
        // juego al llegar y te saca de la cámara. Si hace falta, mirar con un cliente falso en vez de espectador.
        if (post == null || player.getGameMode() == GameMode.SPECTATOR || !post.getWorld().equals(player.getWorld())) {
            lang.send(player, "camera.unavailable");
            return;
        }

        Location from = player.getLocation();
        player.closeInventory();
        player.getPersistentDataContainer().set(originKey, PersistentDataType.STRING, new Origin(
                from.getWorld().getName(), from.getX(), from.getY(), from.getZ(), from.getYaw(), from.getPitch(),
                player.getGameMode()).encode());

        player.setGameMode(GameMode.SPECTATOR);
        // Medio segundo después de llegar: una cámara lejana no tiene cabeza hasta que su zona se carga
        // con quien mira, y el motor la crea en su siguiente pasada.
        player.teleportAsync(post).thenAccept(arrived -> Bukkit.getScheduler().runTaskLater(plugin, () -> {

            // Si en ese rato salió (se desconectó, lo movieron), ya está devuelto: nada que hacer.
            if (!player.getPersistentDataContainer().has(originKey, PersistentDataType.STRING)) {
                return;
            }

            ItemDisplay head = engine.visual(camera.placementId());

            if (!arrived || head == null) {
                restore(player);
                lang.send(player, "camera.unavailable");
                return;
            }

            player.setSpectatorTarget(head);
            lang.send(player, "camera.watching");
        }, 10L));
    }

    /** Devuelve a quien estaba mirando a su sitio y a su modo de juego. No hace nada si no miraba. */
    public void restore(Player player) {

        String raw = player.getPersistentDataContainer().get(originKey, PersistentDataType.STRING);

        if (raw == null) {
            return;
        }

        // Antes de moverlo: el teletransporte suelta la cámara y vuelve a llamar aquí.
        player.getPersistentDataContainer().remove(originKey);

        Origin origin = Origin.decode(raw);
        World world = origin == null ? null : Bukkit.getWorld(origin.world());

        player.teleport(world == null ? Bukkit.getWorlds().get(0).getSpawnLocation()
                : new Location(world, origin.x(), origin.y(), origin.z(), origin.yaw(), origin.pitch()));
        player.setGameMode(origin == null ? GameMode.SURVIVAL : origin.mode());
    }

    /** Al apagar el plugin nadie se queda en espectador. */
    public void restoreAll() {
        Bukkit.getOnlinePlayers().forEach(this::restore);
    }

    @EventHandler
    public void onStopSpectating(PlayerStopSpectatingEntityEvent event) {

        Player player = event.getPlayer();

        // Un tick después: el evento llega en mitad del cambio de cámara (o del teletransporte que la soltó).
        if (player.getPersistentDataContainer().has(originKey, PersistentDataType.STRING)) {
            Bukkit.getScheduler().runTask(plugin, () -> restore(player));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        restore(event.getPlayer());
        lastCombat.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        restore(event.getPlayer());
    }

}
