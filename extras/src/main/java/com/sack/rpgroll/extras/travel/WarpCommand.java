package com.sack.rpgroll.extras.travel;

import com.sack.rpgroll.common.block.Protection;
import com.sack.rpgroll.common.command.Senders;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.extras.travel.Warps.Warp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Pattern;

/**
 * Warps de jugador: {@code /warp <nombre>} viaja, {@code /warp set <nombre>} fija uno donde está,
 * {@code /warp delete <nombre>} lo quita y {@code /warp list} los enseña. {@code /setwarp},
 * {@code /delwarp} y {@code /warps} son los mismos tres con su nombre de siempre.
 * <p>
 * Solo se fija donde el jugador puede construir (ver {@link Protection}): ni en el claim de otro
 * ni en una región protegida. Y solo se viaja si el sitio sigue siendo pisable, para que un warp
 * no se convierta en una trampa cavando debajo.
 */
public class WarpCommand implements CommandExecutor, TabCompleter {

    public static final String PERMISSION = "rpgrollextras.warp";
    private static final String ADMIN_PERMISSION = PERMISSION + ".admin";
    private static final List<String> ACTIONS = List.of("set", "delete", "list");
    private static final Pattern NAME = Pattern.compile("[a-z0-9_]{2,16}");

    private final Warps warps;
    private final LangManager lang;
    private int maxPerPlayer;
    private Set<String> disabledWorlds = Set.of();

    public WarpCommand(Warps warps, LangManager lang) {
        this.warps = warps;
        this.lang = lang;
    }

    public void configure(ConfigurationSection config) {
        maxPerPlayer = Math.max(0, config.getInt("max-per-player", 2));
        disabledWorlds = Set.copyOf(config.getStringList("disabled-worlds"));
    }

    /** {@code /setwarp x} es {@code /warp set x}. */
    public CommandExecutor as(String action) {
        return (sender, command, label, args) -> {
            String[] full = new String[args.length + 1];
            full[0] = action;
            System.arraycopy(args, 0, full, 1, args.length);
            return onCommand(sender, command, label, full);
        };
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(Senders.asPlayer(sender) instanceof Player player)) {
            lang.send(sender, "server_menu.players_only");
            return true;
        }

        String action = args.length == 0 ? "list" : args[0].toLowerCase(Locale.ROOT);

        try {
            switch (action) {
                case "list" -> list(player);
                case "set" -> set(player, args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "");
                case "delete" -> delete(player, args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "");
                default -> travel(player, action);
            }
        } catch (IOException exception) {
            lang.send(player, "warp.save_failed");
            Bukkit.getLogger().warning("[RPGRoll-Extras] warps.yml: " + exception.getMessage());
        }

        return true;
    }

    private void list(Player player) {

        StringJoiner names = new StringJoiner("&7, ");
        for (Warp warp : warps.all()) {
            names.add("&e" + warp.name() + " &8(" + warp.ownerName() + ")");
        }

        if (names.length() == 0) {
            lang.send(player, "warp.list_empty");
        } else {
            lang.send(player, "warp.list", "warps", names.toString());
        }
    }

    private void set(Player player, String name) throws IOException {

        if (!NAME.matcher(name).matches() || ACTIONS.contains(name)) {
            lang.send(player, "warp.bad_name");
            return;
        }

        if (warps.get(name).isPresent()) {
            lang.send(player, "warp.exists", "warp", name);
            return;
        }

        boolean admin = player.hasPermission(ADMIN_PERMISSION);

        if (!admin && warps.ownedBy(player.getUniqueId()) >= maxPerPlayer) {
            lang.send(player, "warp.limit", "max", maxPerPlayer);
            return;
        }

        Location here = player.getLocation();

        if (disabledWorlds.contains(here.getWorld().getName())) {
            lang.send(player, "warp.world_disabled");
            return;
        }

        if (!admin && !Protection.canPlace(player, List.of(here.getBlock()), null)) {
            lang.send(player, "warp.protected");
            return;
        }

        if (!safe(here)) {
            lang.send(player, "warp.unsafe_here");
            return;
        }

        warps.put(new Warp(name, player.getUniqueId(), player.getName(), here.getWorld().getName(), here.getX(),
                here.getY(), here.getZ(), here.getYaw(), here.getPitch()));
        lang.send(player, "warp.set", "warp", name);
    }

    private void delete(Player player, String name) throws IOException {

        Warp warp = warps.get(name).orElse(null);

        if (warp == null) {
            lang.send(player, "warp.unknown", "warp", name);
            return;
        }

        if (!warp.owner().equals(player.getUniqueId()) && !player.hasPermission(ADMIN_PERMISSION)) {
            lang.send(player, "warp.not_yours", "owner", warp.ownerName());
            return;
        }

        warps.remove(name);
        lang.send(player, "warp.deleted", "warp", name);
    }

    private void travel(Player player, String name) {

        Warp warp = warps.get(name).orElse(null);
        World world = warp == null ? null : Bukkit.getWorld(warp.world());

        if (world == null) {
            lang.send(player, "warp.unknown", "warp", name);
            return;
        }

        Location target = new Location(world, warp.x(), warp.y(), warp.z(), warp.yaw(), warp.pitch());

        // El futuro se completa en el hilo principal: desde aquí ya se puede leer el mundo.
        world.getChunkAtAsync(target).thenAccept(chunk -> {

            if (!safe(target)) {
                lang.send(player, "warp.unsafe", "warp", name);
                return;
            }

            player.teleportAsync(target).thenAccept(arrived -> {
                if (arrived) {
                    lang.send(player, "warp.done", "warp", name, "owner", warp.ownerName());
                }
            });
        });
    }

    /** Sin lava, agua ni fuego en el cuerpo, sin asfixia y con algo firme a dos bloques como mucho. */
    private static boolean safe(Location spot) {

        // Medio bloque arriba: de pie en una losa, los pies son el aire de encima y no la losa.
        Block feet = spot.clone().add(0, 0.5, 0).getBlock();
        Block head = feet.getRelative(BlockFace.UP);

        if (feet.isLiquid() || head.isLiquid() || head.getType().isOccluding()
                || RandomTeleportCommand.UNSAFE.contains(feet.getType())) {
            return false;
        }

        for (int down = 0; down <= 2; down++) {
            Block ground = feet.getRelative(0, -down, 0);
            if (ground.isLiquid()) {
                return false;
            }
            if (!ground.isPassable()) {
                return !RandomTeleportCommand.UNSAFE.contains(ground.getType());
            }
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length != 1 && !(args.length == 2 && args[0].equalsIgnoreCase("delete"))) {
            return List.of();
        }

        List<String> options = new ArrayList<>(args.length == 1 ? ACTIONS : List.of());
        warps.all().forEach(warp -> options.add(warp.name()));
        String typed = args[args.length - 1].toLowerCase(Locale.ROOT);
        return options.stream().filter(option -> option.startsWith(typed)).toList();
    }

}
