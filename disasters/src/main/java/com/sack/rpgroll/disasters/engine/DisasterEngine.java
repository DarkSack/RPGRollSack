package com.sack.rpgroll.disasters.engine;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.disasters.core.Disaster;
import com.sack.rpgroll.disasters.core.DisasterLevel;
import com.sack.rpgroll.disasters.core.DisasterManager;
import com.sack.rpgroll.disasters.core.DisasterPhenomenon;
import com.sack.rpgroll.disasters.engine.Phenomenon.Power;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

/**
 * Pone en marcha los desastres de {@code disasters/}, uno por mundo a la vez: aviso con cuenta
 * atrás, título y barra de jefe mientras dura, sus fenómenos y el mensaje del final.
 * <p>
 * Lo que rompen los fenómenos pasa por {@link WorldDamage}, que respeta las protecciones y lo
 * repone con el tiempo. Los bloques que vuelan (meteoritos, escombros, rocas) nunca se colocan:
 * se borran al tocar el suelo. En Bedrock se ve igual: bloques que caen y partículas, sin pack.
 */
public class DisasterEngine implements Listener {

    private final Plugin plugin;
    private final LangManager lang;
    private final DisasterManager disasters;
    private final NamespacedKey tag;
    private final WorldDamage damage;
    private final Map<String, Running> running = new HashMap<>();
    private final Map<UUID, Consumer<Location>> landings = new HashMap<>();
    private final Set<String> warned = new HashSet<>();

    private double safeRadius = 96;
    private Set<GameMode> gameModes = EnumSet.of(GameMode.SURVIVAL, GameMode.ADVENTURE);
    private int maxStartDelay = 18000;
    private int warningSeconds = 10;
    private String warningSound = "BLOCK_NOTE_BLOCK_BASS 1 0.6";
    private String bossBar = "RED PROGRESS";
    private Map<Integer, DisasterLevel> levels = Map.of(1, new DisasterLevel(1, 1, 1, 1));

    /** Un desastre en un mundo: primero avisa ({@code warningLeft} > 0) y luego corren sus fenómenos. */
    private final class Running {
        final Disaster disaster;
        final int level;
        final World world;
        final List<Phenomenon> phenomena;
        final int durationTicks;
        final BossBar bar;
        int warningLeft;
        int elapsed;
        BukkitTask ticker;

        Running(Disaster disaster, int level, World world, List<Phenomenon> phenomena, int durationTicks) {
            this.disaster = disaster;
            this.level = level;
            this.world = world;
            this.phenomena = phenomena;
            this.durationTicks = durationTicks;
            this.bar = bossBar(disaster, level);
        }
    }

    public DisasterEngine(Plugin plugin, LangManager lang, DisasterManager disasters) {
        this.plugin = plugin;
        this.lang = lang;
        this.disasters = disasters;
        this.tag = new NamespacedKey(plugin, "disaster_block");
        this.damage = new WorldDamage(plugin, this::warn, this::inSafeZone);
    }

    /** Lee config.yml; lo que falte se queda como está. */
    public void configure(ConfigurationSection config) {

        if (config == null) {
            return;
        }

        safeRadius = Math.max(0, config.getDouble("safe-radius", safeRadius));
        maxStartDelay = Math.max(1, config.getInt("max-start-delay", maxStartDelay));
        warningSeconds = Math.max(0, config.getInt("warning-seconds", warningSeconds));
        warningSound = config.getString("warning-sound", warningSound);
        bossBar = config.getString("bossbar", bossBar);

        if (config.isList("gamemodes")) {
            Set<GameMode> modes = EnumSet.noneOf(GameMode.class);
            for (String name : config.getStringList("gamemodes")) {
                try {
                    modes.add(GameMode.valueOf(name.trim().toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException e) {
                    warn("config.yml gamemodes: " + name + " no es un modo de juego; se omite");
                }
            }
            gameModes = modes;
        }

        try {
            Map<Integer, DisasterLevel> read = DisasterLevel.parse(config.getConfigurationSection("levels"));
            if (!read.isEmpty()) {
                levels = read;
            }
        } catch (IllegalArgumentException e) {
            warn("config.yml levels: " + e.getMessage() + "; se usan los de antes");
        }

        damage.configure(config.getConfigurationSection("world-damage"));
    }

    /** Lee {@code regen.dat} y empieza a reponer lo que dejaron los desastres de antes. */
    public void startRegeneration() {
        damage.start();
    }

    public int maxStartDelay() {
        return maxStartDelay;
    }

    public Optional<Disaster> active(World world) {
        return Optional.ofNullable(running.get(world.getName())).map(current -> current.disaster);
    }

    /** Los niveles del desastre: los suyos o, si no trae, los de config.yml. */
    public Map<Integer, DisasterLevel> levelsOf(Disaster disaster) {
        return disaster.levels().isEmpty() ? levels : disaster.levels();
    }

    /** Un nivel al azar según los pesos; el más bajo si ninguno tiene peso. */
    public int randomLevel(Disaster disaster) {
        Map<Integer, DisasterLevel> table = levelsOf(disaster);
        Integer picked = DisasterLevel.pick(table, ThreadLocalRandom.current().nextDouble());
        return picked != null ? picked : table.keySet().stream().min(Integer::compare).orElse(1);
    }

    /**
     * Avisa y luego arranca el desastre al nivel dado. False si el nivel no existe, si en ese mundo
     * ya hay uno o si ninguno de sus fenómenos puede ocurrir ahí (los de cielo abierto no salen en el
     * Nether ni en el End).
     */
    public boolean start(Disaster disaster, World world, int level) {

        DisasterLevel stats = levelsOf(disaster).get(level);
        if (stats == null || running.containsKey(world.getName())) {
            return false;
        }

        Power power = new Power(disaster.intensity() * stats.intensity(), stats.size());
        int duration = (int) Math.max(20, Math.round(disaster.durationTicks() * stats.duration()));
        List<Phenomenon> phenomena = new ArrayList<>();

        for (DisasterPhenomenon own : disaster.allPhenomena(disasters::get)) {
            DisasterPhenomenon phenomenon = own.withDefaults(disaster.regeneration());
            if (phenomenon.type().needsSky() && world.getEnvironment() != World.Environment.NORMAL) {
                continue;
            }
            phenomena.add(switch (phenomenon.type()) {
                case TORNADO -> new Tornado(this, world, phenomenon, power, duration);
                case METEOR_STORM -> new MeteorStorm(this, world, phenomenon, power, duration);
                case ACID_RAIN -> new AcidRain(this, world, phenomenon, power, duration);
                case EARTHQUAKE -> new Earthquake(this, world, phenomenon, power, duration);
                case SINKHOLE -> new Sinkhole(this, world, phenomenon, power, duration);
                case MOBS -> new Mobs(this, world, phenomenon, power, duration);
            });
        }

        if (phenomena.isEmpty()) {
            return false;
        }

        Running current = new Running(disaster, level, world, phenomena, duration);
        current.warningLeft = disaster.warningSeconds() >= 0 ? disaster.warningSeconds() : warningSeconds;
        running.put(world.getName(), current);
        current.ticker = Bukkit.getScheduler().runTaskTimer(plugin, () -> tick(current), 0L, 20L);
        return true;
    }

    /** Para que el sorteo diario no los saque todos al amanecer: empieza dentro de {@code delayTicks}. */
    public void startLater(Disaster disaster, World world, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!world.getPlayers().isEmpty()) {
                start(disaster, world, randomLevel(disaster));
            }
        }, delayTicks);
    }

    /** Cada segundo: la cuenta atrás del aviso y, después, la barra con lo que queda. */
    private void tick(Running current) {

        if (current.warningLeft > 0) {
            Component actionBar = lang.component("disaster.warning", placeholders(current, current.warningLeft));
            current.bar.name(lang.component("disaster.warning_bossbar", placeholders(current, current.warningLeft)));
            current.bar.progress(1f);
            Looks.SoundLook sound = sound(warningSound, "warning-sound");
            for (Player player : current.world.getPlayers()) {
                player.sendActionBar(actionBar);
                sound.play(player);
            }
            showBar(current);
            current.warningLeft--;
            return;
        }

        if (current.elapsed == 0) {
            current.bar.name(lang.component("disaster.bossbar", placeholders(current, 0)));
            announceStart(current);
            runCommands(current.disaster.startCommands(), current);
            current.phenomena.forEach(phenomenon -> phenomenon.runTaskTimer(plugin, 1L, phenomenon.period));
        }

        current.elapsed += 20;
        current.bar.progress((float) Math.clamp(1.0 - (double) current.elapsed / current.durationTicks, 0, 1));
        showBar(current);
    }

    private void showBar(Running current) {
        if (!bossBarSpec(current.disaster).isBlank()) {
            current.world.getPlayers().forEach(player -> player.showBossBar(current.bar));
        }
    }

    /** Corta el desastre del mundo (también si aún estaba avisando). False si no había ninguno. */
    public boolean stop(World world) {
        Running current = running.get(world.getName());
        if (current == null) {
            return false;
        }
        if (current.elapsed == 0) {
            end(current, false);
        } else {
            List.copyOf(current.phenomena).forEach(this::finished);
        }
        return true;
    }

    /** Al apagar: corta todo y guarda lo que falta por reponer, que se repondrá al volver a arrancar. */
    public void stopAll() {
        List.copyOf(running.keySet()).forEach(name -> {
            World world = Bukkit.getWorld(name);
            if (world != null) {
                stop(world);
            }
        });
        damage.shutdown();
    }

    /** Repone ya todo lo que rompieron los desastres. Devuelve cuántos bloques. */
    public int regenerateNow() {
        return damage.restoreAll();
    }

    public int pendingRegeneration() {
        return damage.pending();
    }

    void finished(Phenomenon phenomenon) {

        Running current = running.get(phenomenon.world.getName());
        if (current == null || !current.phenomena.remove(phenomenon)) {
            return;
        }

        try {
            phenomenon.cancel();
        } catch (IllegalStateException notScheduled) {
            // Se paró durante el aviso, antes de programarse.
        }
        phenomenon.cleanup();

        if (current.phenomena.isEmpty()) {
            end(current, true);
        }
    }

    private void end(Running current, boolean announce) {

        running.remove(current.world.getName());
        current.ticker.cancel();
        current.world.getPlayers().forEach(player -> player.hideBossBar(current.bar));
        // Bloques que caducaron o se rompieron sin llegar a tocar el suelo.
        landings.keySet().removeIf(id -> Bukkit.getEntity(id) == null);

        if (!announce) {
            return;
        }
        Component end = message(current, "end", "disaster.ended");
        if (end != null) {
            current.world.getPlayers().forEach(player -> player.sendMessage(end));
        }
        runCommands(current.disaster.endCommands(), current);
    }

    private void announceStart(Running current) {

        Component chat = message(current, "start", "disaster.started");
        Component title = message(current, "title", "disaster.title");
        Component subtitle = message(current, "subtitle", "disaster.subtitle");
        Looks.SoundLook sound = sound(current.disaster.startSound(), "desastre '" + current.disaster.id() + "' start-sound");

        for (Player player : current.world.getPlayers()) {
            if (chat != null) {
                player.sendMessage(chat);
            }
            if (title != null || subtitle != null) {
                player.showTitle(Title.title(title == null ? Component.empty() : title,
                        subtitle == null ? Component.empty() : subtitle));
            }
            sound.play(player);
        }
    }

    private Object[] placeholders(Running current, int seconds) {
        return new Object[] {"disaster", current.disaster.displayName(), "description", current.disaster.description(),
                "level", current.level, "seconds", seconds};
    }

    /** El texto propio del desastre, o el del idioma si no trae; null si lo dejó en blanco a propósito. */
    private Component message(Running current, String key, String langKey) {

        String own = current.disaster.messages().get(key);
        if (own == null) {
            return lang.component(langKey, placeholders(current, 0));
        }
        if (own.isBlank()) {
            return null;
        }
        return ComponentUtils.parse(own.replace("{disaster}", current.disaster.displayName())
                .replace("{description}", current.disaster.description())
                .replace("{level}", String.valueOf(current.level)));
    }

    private String bossBarSpec(Disaster disaster) {
        return disaster.bossBar() != null ? disaster.bossBar() : bossBar;
    }

    /** {@code "COLOR ESTILO"}: RED, BLUE, GREEN... y PROGRESS, NOTCHED_6, NOTCHED_10, NOTCHED_12, NOTCHED_20. */
    private BossBar bossBar(Disaster disaster, int level) {

        String[] parts = bossBarSpec(disaster).trim().toUpperCase(Locale.ROOT).split("\\s+");
        BossBar.Color color = BossBar.Color.RED;
        BossBar.Overlay overlay = BossBar.Overlay.PROGRESS;
        try {
            if (!parts[0].isEmpty()) {
                color = BossBar.Color.valueOf(parts[0]);
            }
            if (parts.length > 1) {
                overlay = BossBar.Overlay.valueOf(parts[1]);
            }
        } catch (IllegalArgumentException e) {
            warn("bossbar '" + bossBarSpec(disaster) + "' no vale (color y estilo); se usa RED PROGRESS");
        }
        return BossBar.bossBar(Component.text(disaster.displayName() + " " + level), 1f, color, overlay);
    }

    private Looks.SoundLook sound(String spec, String where) {
        try {
            return Looks.sound(spec);
        } catch (RuntimeException e) {
            warn(where + ": " + spec + " no vale (" + e.getMessage() + ")");
            return Looks.SoundLook.NONE;
        }
    }

    private void runCommands(List<String> commands, Running current) {
        for (String command : commands) {
            String line = command.replace("%world%", current.world.getName())
                    .replace("%disaster%", current.disaster.id())
                    .replace("%level%", String.valueOf(current.level));
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), line);
            } catch (RuntimeException e) {
                warn("desastre '" + current.disaster.id() + "': el comando '" + line + "' falló (" + e.getMessage() + ")");
            }
        }
    }

    /** Avisa una sola vez de cada parámetro mal escrito, para no llenar la consola cada tick. */
    void warn(String message) {
        if (warned.add(message)) {
            plugin.getLogger().warning("✘ Desastres: " + message);
        }
    }

    // ----- Lo que comparten los fenómenos -----

    Plugin plugin() {
        return plugin;
    }

    WorldDamage damage() {
        return damage;
    }

    NamespacedKey tag() {
        return tag;
    }

    /** Jugadores a los que el desastre puede tocar: en los modos de juego de config.yml y fuera de la zona segura. */
    List<Player> targets(World world) {
        return world.getPlayers().stream().filter(this::affects).toList();
    }

    /**
     * Si un fenómeno puede mover o dañar esta entidad. Fuera quedan los jugadores en otros modos de
     * juego, los NPC (sin IA, invulnerables o de Citizens), los soportes de armadura y la zona segura.
     */
    boolean affects(Entity entity) {
        if (inSafeZone(entity.getLocation()) || entity.isInvulnerable() || entity.hasMetadata("NPC")) {
            return false;
        }
        if (entity instanceof Player player) {
            return gameModes.contains(player.getGameMode());
        }
        if (entity instanceof LivingEntity living) {
            return living.hasAI() && !(entity instanceof ArmorStand);
        }
        return entity instanceof Item;
    }

    boolean inSafeZone(Location location) {
        Location spawn = location.getWorld().getSpawnLocation();
        double dx = location.getX() - spawn.getX();
        double dz = location.getZ() - spawn.getZ();
        return dx * dx + dz * dz < safeRadius * safeRadius;
    }

    /** Si no hay nada que pare la lluvia encima (techos, hojas y cristal cuentan como techo). */
    static boolean underOpenSky(Location location) {
        return location.getBlockY() > location.getWorld().getHighestBlockYAt(location.getBlockX(),
                location.getBlockZ(), HeightMap.MOTION_BLOCKING);
    }

    /** Un bloque macizo corriente, sin inventario: el que se puede usar como escombro o roca. */
    static boolean plainSolid(org.bukkit.block.Block block) {
        return block.getType().isSolid() && !block.getType().isInteractable();
    }

    /**
     * Un bloque que cae, del plugin: no deja objeto, no daña por sí mismo, no se guarda con el
     * chunk y nunca se coloca. Al tocar el suelo desaparece y llama a {@code onLand} si lo hay.
     */
    FallingBlock fallingBlock(Location location, BlockData data, Consumer<Location> onLand) {

        FallingBlock block = location.getWorld().spawnFallingBlock(location, data);
        block.setDropItem(false);
        // Sin setCancelDrop: con él Paper lo borra al aterrizar sin avisar con EntityChangeBlockEvent,
        // y es ese evento (cancelado en onLand) el que dispara el impacto.
        block.setHurtEntities(false);
        block.setPersistent(false);
        block.getPersistentDataContainer().set(tag, PersistentDataType.BYTE, (byte) 1);
        onLanding(block, onLand);
        return block;
    }

    /** Cambia lo que pasa cuando este bloque del plugin toca el suelo (null = solo desaparece). */
    void onLanding(FallingBlock block, Consumer<Location> onLand) {
        if (onLand == null) {
            landings.remove(block.getUniqueId());
        } else {
            landings.put(block.getUniqueId(), onLand);
        }
    }

    void discard(FallingBlock block) {
        landings.remove(block.getUniqueId());
        block.remove();
    }

    @EventHandler(ignoreCancelled = true)
    public void onLand(EntityChangeBlockEvent event) {

        if (!(event.getEntity() instanceof FallingBlock block)
                || !block.getPersistentDataContainer().has(tag, PersistentDataType.BYTE)) {
            return;
        }

        event.setCancelled(true);
        block.remove();

        Consumer<Location> onLand = landings.remove(block.getUniqueId());
        if (onLand != null) {
            onLand.accept(event.getBlock().getLocation().add(0.5, 0, 0.5));
        }
    }

}
