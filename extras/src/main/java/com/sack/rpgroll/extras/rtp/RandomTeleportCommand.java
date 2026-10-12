package com.sack.rpgroll.extras.rtp;

import com.sack.rpgroll.common.command.Senders;
import com.sack.rpgroll.common.lang.LangManager;

import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * {@code /rtp [mundo]}: lleva al jugador a un punto al azar de la superficie. El chunk de destino se
 * pide con {@code getChunkAtAsync}: si no está generado se genera fuera del hilo principal, que es
 * lo que congelaba el servidor con un RTP que precarga destinos.
 * <p>
 * Sin argumento va al mundo en el que ya está si es uno de los configurados, y si no al de
 * {@code default-world}. Acepta también {@code world=<mundo>}, la forma de LeafRTP.
 */
public class RandomTeleportCommand implements CommandExecutor, TabCompleter {

    public static final String PERMISSION = "rpgrollextras.rtp";

    private static final int ATTEMPTS = 12;
    /** Sólidos sobre los que no se deja a nadie. Agua y lava no son sólidos: se descartan solos. */
    private static final Set<Material> UNSAFE = EnumSet.of(Material.MAGMA_BLOCK, Material.CACTUS,
            Material.CAMPFIRE, Material.SOUL_CAMPFIRE);

    /** El anillo de un mundo en el que se puede caer, en bloques desde el centro de su borde. */
    record Zone(int minRadius, int maxRadius) {

        /** El mismo anillo sin salirse de {@code limit}: el borde del mundo manda. */
        Zone within(int limit) {
            int max = Math.max(1, Math.min(maxRadius, limit));
            return new Zone(Math.min(minRadius, max - 1), max);
        }

        /** Un punto {x, z} al azar, repartido por igual en toda la superficie del anillo. */
        int[] pick(Random random, double centerX, double centerZ) {
            double min = (double) minRadius * minRadius;
            double radius = Math.sqrt(min + random.nextDouble() * ((double) maxRadius * maxRadius - min));
            double angle = random.nextDouble() * 2 * Math.PI;
            return new int[] {(int) Math.floor(centerX + radius * Math.cos(angle)),
                    (int) Math.floor(centerZ + radius * Math.sin(angle))};
        }
    }

    private final Plugin plugin;
    private final LangManager lang;
    private final Map<UUID, Long> lastUse = new HashMap<>();
    /** Quien ya tiene uno en marcha (esperando o buscando sitio). */
    private final Set<UUID> busy = new HashSet<>();

    private Map<String, Zone> zones = Map.of();
    private String defaultWorld = "world";
    private int delaySeconds;
    private int cooldownSeconds;

    public RandomTeleportCommand(Plugin plugin, LangManager lang) {
        this.plugin = plugin;
        this.lang = lang;
    }

    public void configure(ConfigurationSection config) {

        Map<String, Zone> loaded = new HashMap<>();
        ConfigurationSection worlds = config.getConfigurationSection("worlds");

        if (worlds != null) {
            for (String world : worlds.getKeys(false)) {
                loaded.put(world, new Zone(Math.max(0, worlds.getInt(world + ".min-radius", 0)),
                        Math.max(1, worlds.getInt(world + ".max-radius", 1000))));
            }
        }

        zones = loaded;
        defaultWorld = config.getString("default-world", "world");
        delaySeconds = Math.max(0, config.getInt("delay-seconds", 3));
        cooldownSeconds = Math.max(0, config.getInt("cooldown-seconds", 300));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(Senders.asPlayer(sender) instanceof Player player)) {
            lang.send(sender, "server_menu.players_only");
            return true;
        }

        String name = args.length > 0 ? args[0].replaceFirst("^world=", "")
                : zones.containsKey(player.getWorld().getName()) ? player.getWorld().getName() : defaultWorld;
        World world = Bukkit.getWorld(name);
        Zone zone = zones.get(name);

        // shortcut: solo mundos con cielo, porque en el Nether el bloque más alto es el techo de bedrock;
        // buscar un hueco de arriba abajo cuando haga falta un RTP allí.
        if (world == null || zone == null || world.hasCeiling()) {
            lang.send(player, "rtp.unknown_world", "world", name);
            return true;
        }

        UUID id = player.getUniqueId();

        if (busy.contains(id)) {
            return true;
        }

        long wait = cooldownSeconds - (System.currentTimeMillis() - lastUse.getOrDefault(id, 0L)) / 1000;
        if (wait > 0 && !player.hasPermission(PERMISSION + ".nocooldown")) {
            lang.send(player, "rtp.cooldown", "seconds", wait);
            return true;
        }

        int delay = player.hasPermission(PERMISSION + ".nodelay") ? 0 : delaySeconds;
        Location start = player.getLocation();
        busy.add(id);

        if (delay > 0) {
            lang.send(player, "rtp.wait", "seconds", delay);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {

            if (!player.isOnline()) {
                busy.remove(id);
                return;
            }

            if (delay > 0 && (!start.getWorld().equals(player.getWorld())
                    || start.distanceSquared(player.getLocation()) > 1)) {
                busy.remove(id);
                lang.send(player, "rtp.moved");
                return;
            }

            lang.send(player, "rtp.searching");
            search(player, world, zone, ATTEMPTS);

        }, delay * 20L);

        return true;
    }

    private void search(Player player, World world, Zone zone, int attempts) {

        UUID id = player.getUniqueId();

        if (!player.isOnline()) {
            busy.remove(id);
            return;
        }

        if (attempts == 0) {
            busy.remove(id);
            lang.send(player, "rtp.no_spot");
            return;
        }

        WorldBorder border = world.getWorldBorder();
        int[] spot = zone.within((int) (border.getSize() / 2) - 2).pick(ThreadLocalRandom.current(),
                border.getCenter().getX(), border.getCenter().getZ());

        // El futuro se completa en el hilo principal: desde aquí ya se puede leer el mundo.
        world.getChunkAtAsync(spot[0] >> 4, spot[1] >> 4).thenAccept(chunk -> {

            Block ground = world.getHighestBlockAt(spot[0], spot[1], HeightMap.MOTION_BLOCKING);

            if (!ground.getType().isSolid() || UNSAFE.contains(ground.getType())) {
                search(player, world, zone, attempts - 1);
                return;
            }

            Location target = ground.getLocation().add(0.5, 1, 0.5);
            target.setYaw(player.getLocation().getYaw());

            player.teleportAsync(target).thenAccept(arrived -> {

                busy.remove(id);

                if (!arrived) {
                    lang.send(player, "rtp.no_spot");
                    return;
                }

                lastUse.put(id, System.currentTimeMillis());
                lang.send(player, "rtp.done", "x", target.getBlockX(), "y", target.getBlockY(),
                        "z", target.getBlockZ());
            });
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return args.length == 1 ? zones.keySet().stream().filter(world -> world.startsWith(args[0])).toList()
                : List.of();
    }

}
