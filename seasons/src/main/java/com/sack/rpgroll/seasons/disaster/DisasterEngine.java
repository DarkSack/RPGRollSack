package com.sack.rpgroll.seasons.disaster;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.seasons.core.Disaster;
import com.sack.rpgroll.seasons.core.DisasterManager;
import com.sack.rpgroll.seasons.core.DisasterPhenomenon;
import com.sack.rpgroll.util.ComponentUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
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
import java.util.function.Consumer;

/**
 * Pone en marcha los desastres de {@code disasters/}, uno por mundo a la vez.
 * <p>
 * Ningún fenómeno rompe ni pone bloques de verdad: las grietas y los cráteres se envían
 * solo al cliente y se deshacen solos, y los bloques que caen (meteoritos, escombros, rocas)
 * se borran al tocar el suelo. Así no hay que preguntar a WorldGuard ni a los claims, y en
 * Bedrock se ve igual (bloques que caen y partículas, sin modelos del pack).
 */
public class DisasterEngine implements Listener {

    private final Plugin plugin;
    private final LangManager lang;
    private final DisasterManager disasters;
    private final NamespacedKey tag;
    private final Map<String, Running> running = new HashMap<>();
    private final Map<UUID, Consumer<Location>> landings = new HashMap<>();
    private final Set<String> warned = new HashSet<>();

    private double safeRadius = 96;
    private Set<GameMode> gameModes = EnumSet.of(GameMode.SURVIVAL, GameMode.ADVENTURE);
    private int maxStartDelay = 18000;
    private int fakeViewDistance = 96;

    private record Running(Disaster disaster, List<Phenomenon> phenomena) {
    }

    public DisasterEngine(Plugin plugin, LangManager lang, DisasterManager disasters) {
        this.plugin = plugin;
        this.lang = lang;
        this.disasters = disasters;
        this.tag = new NamespacedKey(plugin, "disaster_block");
    }

    /** Lee la sección {@code disasters} de config.yml; lo que falte se queda como está. */
    public void configure(ConfigurationSection section) {

        if (section == null) {
            return;
        }

        safeRadius = Math.max(0, section.getDouble("safe-radius", safeRadius));
        maxStartDelay = Math.max(1, section.getInt("max-start-delay", maxStartDelay));
        fakeViewDistance = Math.max(16, section.getInt("fake-view-distance", fakeViewDistance));

        if (section.isList("gamemodes")) {
            Set<GameMode> modes = EnumSet.noneOf(GameMode.class);
            for (String name : section.getStringList("gamemodes")) {
                try {
                    modes.add(GameMode.valueOf(name.trim().toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException e) {
                    warn("config.yml disasters.gamemodes: " + name + " no es un modo de juego; se omite");
                }
            }
            gameModes = modes;
        }
    }

    public int maxStartDelay() {
        return maxStartDelay;
    }

    public Optional<Disaster> active(World world) {
        return Optional.ofNullable(running.get(world.getName())).map(Running::disaster);
    }

    /**
     * Arranca el desastre ya. False si en ese mundo ya hay uno, o si ninguno de sus fenómenos
     * puede ocurrir ahí (los de cielo abierto no salen en el Nether ni en el End).
     */
    public boolean start(Disaster disaster, World world, double intensity) {

        if (running.containsKey(world.getName())) {
            return false;
        }

        List<Phenomenon> phenomena = new ArrayList<>();

        for (DisasterPhenomenon phenomenon : disaster.allPhenomena(disasters::get)) {
            if (phenomenon.type().needsSky() && world.getEnvironment() != World.Environment.NORMAL) {
                continue;
            }
            phenomena.add(switch (phenomenon.type()) {
                case TORNADO -> new Tornado(this, world, phenomenon, intensity, disaster.durationTicks());
                case METEOR_STORM -> new MeteorStorm(this, world, phenomenon, intensity, disaster.durationTicks());
                case ACID_RAIN -> new AcidRain(this, world, phenomenon, intensity, disaster.durationTicks());
                case EARTHQUAKE -> new Earthquake(this, world, phenomenon, intensity, disaster.durationTicks());
            });
        }

        if (phenomena.isEmpty()) {
            return false;
        }

        running.put(world.getName(), new Running(disaster, phenomena));
        phenomena.forEach(phenomenon -> phenomenon.runTaskTimer(plugin, 1L, phenomenon.period));
        announceStart(disaster, world);
        runCommands(disaster.startCommands(), disaster, world);
        return true;
    }

    /** Para que el sorteo diario no los saque todos al amanecer: empieza dentro de {@code delayTicks}. */
    public void startLater(Disaster disaster, World world, long delayTicks) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!world.getPlayers().isEmpty()) {
                start(disaster, world, disaster.intensity());
            }
        }, delayTicks);
    }

    /** Corta el desastre del mundo. False si no había ninguno. */
    public boolean stop(World world) {
        Running current = running.get(world.getName());
        if (current == null) {
            return false;
        }
        List.copyOf(current.phenomena()).forEach(this::finished);
        return true;
    }

    public void stopAll() {
        List.copyOf(running.values()).forEach(current -> List.copyOf(current.phenomena()).forEach(this::finished));
    }

    void finished(Phenomenon phenomenon) {

        Running current = running.get(phenomenon.world.getName());
        if (current == null || !current.phenomena().remove(phenomenon)) {
            return;
        }

        phenomenon.cancel();
        phenomenon.cleanup();

        if (current.phenomena().isEmpty()) {
            running.remove(phenomenon.world.getName());
            // Bloques que caducaron o se rompieron sin llegar a tocar el suelo.
            landings.keySet().removeIf(id -> Bukkit.getEntity(id) == null);

            Disaster disaster = current.disaster();
            Component end = message(disaster, "end", "disaster.ended");
            if (end != null) {
                phenomenon.world.getPlayers().forEach(player -> player.sendMessage(end));
            }
            runCommands(disaster.endCommands(), disaster, phenomenon.world);
        }
    }

    private void announceStart(Disaster disaster, World world) {

        Component chat = message(disaster, "start", "disaster.started");
        Component title = message(disaster, "title", "disaster.title");
        Component subtitle = message(disaster, "subtitle", "disaster.subtitle");
        Looks.SoundLook sound = soundOf(disaster);

        for (Player player : world.getPlayers()) {
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

    /** El texto propio del desastre, o el del idioma si no trae; null si lo dejó en blanco a propósito. */
    private Component message(Disaster disaster, String key, String langKey) {

        String own = disaster.messages().get(key);
        if (own == null) {
            return lang.component(langKey, "disaster", disaster.displayName(), "description", disaster.description());
        }
        if (own.isBlank()) {
            return null;
        }
        return ComponentUtils.parse(own.replace("{disaster}", disaster.displayName())
                .replace("{description}", disaster.description()));
    }

    private Looks.SoundLook soundOf(Disaster disaster) {
        try {
            return Looks.sound(disaster.startSound());
        } catch (RuntimeException e) {
            warn("desastre '" + disaster.id() + "' start-sound: " + disaster.startSound() + " no vale ("
                    + e.getMessage() + ")");
            return Looks.SoundLook.NONE;
        }
    }

    private void runCommands(List<String> commands, Disaster disaster, World world) {
        for (String command : commands) {
            String line = command.replace("%world%", world.getName()).replace("%disaster%", disaster.id());
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), line);
            } catch (RuntimeException e) {
                warn("desastre '" + disaster.id() + "': el comando '" + line + "' falló (" + e.getMessage() + ")");
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

    /** Lo que se puede tapar con un bloque falso sin confundir al jugador: macizo y sin inventario. */
    static boolean canFake(Block block) {
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

        if (onLand != null) {
            landings.put(block.getUniqueId(), onLand);
        }
        return block;
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

    /** Enseña esos bloques cambiados a los jugadores cercanos durante {@code ticks}; el mundo no cambia. */
    void fake(Map<Block, BlockData> blocks, int ticks) {

        if (blocks.isEmpty() || ticks <= 0) {
            return;
        }

        Location center = blocks.keySet().iterator().next().getLocation();
        Map<Location, BlockData> shown = new HashMap<>();
        blocks.forEach((block, data) -> shown.put(block.getLocation(), data));

        double reach = (double) fakeViewDistance * fakeViewDistance;
        List<Player> viewers = center.getWorld().getPlayers().stream()
                .filter(player -> player.getLocation().distanceSquared(center) < reach)
                .toList();
        viewers.forEach(player -> player.sendMultiBlockChange(shown));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Map<Location, BlockData> real = new HashMap<>();
            blocks.keySet().forEach(block -> real.put(block.getLocation(), block.getBlockData()));
            viewers.stream().filter(Player::isOnline).forEach(player -> player.sendMultiBlockChange(real));
        }, ticks);
    }

}
