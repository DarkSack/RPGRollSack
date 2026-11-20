package com.sack.rpgroll.traps.turret;

import com.sack.rpgroll.common.lang.LangManager;

import com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent;

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
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mirar por una cámara: el jugador pasa a espectador con los ojos en la cabeza de la cámara, quieto
 * pero mirando a donde él quiera (la cabeza gira con su vista), y al salir (agacharse,
 * desconectarse, que rompan la cámara, cualquier teletransporte) vuelve a donde estaba y a su modo
 * de juego.
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
    /** Marca que la visión nocturna que tiene la puso la cámara: al salir se le quita aunque el servidor se cayera. */
    private final NamespacedKey nightVisionKey;
    /** La visión nocturna que ya traía (una poción) antes de encender la de la cámara, para devolvérsela. */
    private final Map<UUID, PotionEffect> ownNightVision = new HashMap<>();
    /** Si tenía pulsado el salto en el último aviso de teclas: la visión cambia al pulsarlo, no mientras se mantiene. */
    private final Map<UUID, Boolean> jumping = new HashMap<>();
    private final Map<UUID, Long> lastCombat = new HashMap<>();
    /** Quién mira ahora por qué cámara. Solo desde que llega a ella. */
    private final Map<UUID, PlacedTurret> watching = new HashMap<>();

    /** Tras dar o recibir un golpe no se mira por una cámara: desaparecer del sitio sería huir de la pelea. */
    private static final long COMBAT_MILLIS = 15_000;

    public CameraView(Plugin plugin, TurretEngine engine, LangManager lang) {
        this.plugin = plugin;
        this.engine = engine;
        this.lang = lang;
        this.originKey = new NamespacedKey(plugin, "camera-origin");
        this.nightVisionKey = new NamespacedKey(plugin, "camera-night-vision");
        engine.attach(this);
    }

    /** Velocidad de vuelo de Minecraft por defecto. */
    private static final float DEFAULT_FLY_SPEED = 0.1f;

    /**
     * De dónde salió quien mira, tal como se guarda en su PersistentDataContainer. {@code flySpeed} es
     * la suya: mirando se le pone a 0 para que no se mueva de la cámara.
     */
    record Origin(String world, double x, double y, double z, float yaw, float pitch, GameMode mode, float flySpeed) {

        String encode() {
            return world + ";" + x + ";" + y + ";" + z + ";" + yaw + ";" + pitch + ";" + mode.name() + ";" + flySpeed;
        }

        /** null si el texto no es de los nuestros: quien lo lea decide a dónde mandar al jugador. */
        static Origin decode(String raw) {

            String[] parts = raw == null ? new String[0] : raw.split(";");

            // 7 partes: guardado antes de que se apuntara la velocidad de vuelo.
            if (parts.length != 7 && parts.length != 8) {
                return null;
            }

            try {
                return new Origin(parts[0], Double.parseDouble(parts[1]), Double.parseDouble(parts[2]),
                        Double.parseDouble(parts[3]), Float.parseFloat(parts[4]), Float.parseFloat(parts[5]),
                        GameMode.valueOf(parts[6]),
                        parts.length == 8 ? Float.parseFloat(parts[7]) : DEFAULT_FLY_SPEED);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }

    /** La posición de {@code post} con la vista de {@code wanted}: mirando se gira, no se anda. */
    static Location lookOnly(Location post, Location wanted) {
        Location kept = post.clone();
        kept.setYaw(wanted.getYaw());
        kept.setPitch(wanted.getPitch());
        return kept;
    }

    /**
     * La clave de idioma de un aviso de colocar/retirar según qué es: una cámara o un monitor tienen
     * su propio texto ("cámara colocada", no "torreta colocada"); lo demás usa el de las torretas.
     */
    public static String textKey(String impactType, String key) {
        if (CAMERA.equals(impactType)) {
            return "camera." + key;
        }
        return MONITOR.equals(impactType) ? "camera.monitor_" + key : "admin.turret." + key;
    }

    /**
     * true si el teletransporte deja a quien mira en el mismo sitio. Paper hace un teletransporte
     * (PLUGIN) cada vez que se le cambia el destino a un movimiento, y así es como se le retiene en
     * la cámara: eso no es salir de ella.
     */
    static boolean staysPut(Location from, Location to) {
        return to != null && Math.abs(from.getX() - to.getX()) < 1e-3 && Math.abs(from.getY() - to.getY()) < 1e-3
                && Math.abs(from.getZ() - to.getZ()) < 1e-3;
    }

    /**
     * Hacia dónde mira el frente de una cámara o un monitor recién puesto: hacia quien lo puso, en
     * pasos de 90° para que quede recto con los bloques.
     */
    public static float facing(float placerYaw) {
        float towardPlacer = Math.round((placerYaw + 180f) / 90f) * 90f;
        return ((towardPlacer % 360f) + 360f) % 360f;
    }

    /** true si alguien mira ahora por esa cámara: entonces la cabeza va con su vista y no sola. */
    public boolean isWatched(String placementId) {
        return watching.values().stream().anyMatch(camera -> camera.placementId().equals(placementId));
    }

    /** Saca a todos los que miran por una cámara que se va (la rompieron o la retiró un admin). */
    public void release(String placementId) {
        Bukkit.getOnlinePlayers().stream()
                .filter(player -> watching.containsKey(player.getUniqueId())
                        && placementId.equals(watching.get(player.getUniqueId()).placementId()))
                .map(Player.class::cast)
                .toList()
                .forEach(this::restore);
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

    /** true desde que empieza a ir a una cámara hasta que vuelve a su sitio. */
    public boolean isWatching(Player player) {
        return player.getPersistentDataContainer().has(originKey, PersistentDataType.STRING);
    }

    public void watch(Player player, PlacedTurret camera) {

        // Ya está mirando (o llegando): un clic derecho más no es pedir otra cámara.
        if (isWatching(player)) {
            return;
        }

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
                player.getGameMode(), player.getFlySpeed()).encode());

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

            // Los ojos en la cabeza de la cámara, mirando a donde ella. Sin velocidad de vuelo no se
            // mueve de ahí, y la cabeza se le oculta para que no le tape la vista.
            player.teleport(head.getLocation().subtract(0, player.getEyeHeight(), 0));
            player.setFlySpeed(0f);
            player.hideEntity(plugin, head);
            watching.put(player.getUniqueId(), camera);
            lang.send(player, "camera.watching");
        }, 10L));
    }

    /** Devuelve a quien estaba mirando a su sitio y a su modo de juego. No hace nada si no miraba. */
    public void restore(Player player) {

        String raw = player.getPersistentDataContainer().get(originKey, PersistentDataType.STRING);

        if (raw == null) {
            return;
        }

        // Antes de moverlo: el teletransporte de vuelta no debe contar como salir de la cámara.
        player.getPersistentDataContainer().remove(originKey);
        PlacedTurret camera = watching.remove(player.getUniqueId());
        ItemDisplay head = camera == null ? null : engine.visual(camera.placementId());

        if (head != null) {
            player.showEntity(plugin, head);
        }

        Origin origin = Origin.decode(raw);
        World world = origin == null ? null : Bukkit.getWorld(origin.world());

        player.teleport(world == null ? Bukkit.getWorlds().get(0).getSpawnLocation()
                : new Location(world, origin.x(), origin.y(), origin.z(), origin.yaw(), origin.pitch()));
        player.setGameMode(origin == null ? GameMode.SURVIVAL : origin.mode());
        player.setFlySpeed(origin == null ? DEFAULT_FLY_SPEED : origin.flySpeed());
        nightVisionOff(player);
        jumping.remove(player.getUniqueId());
    }

    /** Saltar (espacio) mirando por una cámara enciende o apaga la visión nocturna. */
    @EventHandler
    public void onInput(PlayerInputEvent event) {

        Player player = event.getPlayer();
        boolean jump = event.getInput().isJump();
        boolean was = Boolean.TRUE.equals(jumping.put(player.getUniqueId(), jump));

        if (!jump || was || !watching.containsKey(player.getUniqueId())) {
            return;
        }

        if (player.getPersistentDataContainer().has(nightVisionKey, PersistentDataType.BYTE)) {
            nightVisionOff(player);
            lang.send(player, "camera.night_vision_off");
            return;
        }

        PotionEffect own = player.getPotionEffect(PotionEffectType.NIGHT_VISION);

        if (own != null) {
            ownNightVision.put(player.getUniqueId(), own);
        }

        player.getPersistentDataContainer().set(nightVisionKey, PersistentDataType.BYTE, (byte) 1);
        player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, PotionEffect.INFINITE_DURATION, 0,
                false, false, false));
        lang.send(player, "camera.night_vision_on");
    }

    /** Quita la visión nocturna de la cámara (si la tenía) y le devuelve la suya. */
    private void nightVisionOff(Player player) {

        if (!player.getPersistentDataContainer().has(nightVisionKey, PersistentDataType.BYTE)) {
            return;
        }

        player.getPersistentDataContainer().remove(nightVisionKey);
        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        PotionEffect own = ownNightVision.remove(player.getUniqueId());

        if (own != null) {
            player.addPotionEffect(own);
        }
    }

    /** Al apagar el plugin nadie se queda en espectador. */
    public void restoreAll() {
        Bukkit.getOnlinePlayers().forEach(this::restore);
    }

    /** Mirando se gira la vista (y con ella la cabeza de la cámara), pero no se anda. */
    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {

        PlacedTurret camera = watching.get(event.getPlayer().getUniqueId());

        if (camera == null) {
            return;
        }

        // Colgada, la vista no pasa de lo que la cámara puede girar.
        CameraMount mount = CameraMount.of(camera.mount());
        Location wanted = event.getTo();

        if (mount != null && !mount.covers(camera.yaw(), wanted.getYaw(), wanted.getPitch())) {
            float[] view = mount.clamp(camera.yaw(), wanted.getYaw(), wanted.getPitch());
            wanted = wanted.clone();
            wanted.setYaw(view[0]);
            wanted.setPitch(view[1]);
        }

        if (event.hasChangedPosition() || wanted != event.getTo()) {
            event.setTo(lookOnly(event.getFrom(), wanted));
        }

        ItemDisplay head = engine.visual(camera.placementId());

        if (head != null) {
            head.setRotation(event.getTo().getYaw(), event.getTo().getPitch());
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (event.isSneaking() && watching.containsKey(event.getPlayer().getUniqueId())) {
            restore(event.getPlayer());
        }
    }

    /** Clic a una entidad en espectador: se metería en ella y dejaría la cámara. */
    @EventHandler
    public void onSpectateEntity(PlayerStartSpectatingEntityEvent event) {
        if (watching.containsKey(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /**
     * El menú de espectador (ir a otro jugador) no se usa mirando; cualquier otro teletransporte
     * saca de la cámara, un tick después para no pisarlo.
     */
    @EventHandler(ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {

        Player player = event.getPlayer();

        if (!watching.containsKey(player.getUniqueId())) {
            return;
        }

        if (event.getCause() == PlayerTeleportEvent.TeleportCause.SPECTATE) {
            event.setCancelled(true);
            return;
        }

        if (staysPut(event.getFrom(), event.getTo())) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> restore(player));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        restore(event.getPlayer());
        lastCombat.remove(event.getPlayer().getUniqueId());
        jumping.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        restore(event.getPlayer());
        nightVisionOff(event.getPlayer());
    }

}
