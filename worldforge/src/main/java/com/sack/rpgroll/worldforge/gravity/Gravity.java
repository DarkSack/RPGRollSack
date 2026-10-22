package com.sack.rpgroll.worldforge.gravity;

import com.sack.rpgroll.worldforge.platform.ServerPlatform;
import com.sack.rpgroll.worldforge.profile.Profile;
import com.sack.rpgroll.worldforge.worlds.WorldService;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * A preset's {@code player-gravity}: Minecraft's own gravity attribute, scaled for the players in
 * that world (FEASIBILITY: "gravedad real" covers living entities only). Mobs, items, projectiles and
 * falling blocks keep normal gravity. The safe fall distance is scaled the other way, so a jump in
 * low gravity does not hurt more than a normal one. Modifiers are transient: nothing is saved in the
 * player, and leaving the world removes them.
 */
public final class Gravity implements Listener {

    static final NamespacedKey KEY = new NamespacedKey("worldforge", "gravity");

    private final ServerPlatform platform;
    private final WorldService worlds;

    public Gravity(ServerPlatform platform, WorldService worlds) {
        this.platform = platform;
        this.worlds = worlds;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        apply(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorld(PlayerChangedWorldEvent e) {
        apply(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent e) {
        // The respawned player is a new entity; set it once it exists, on its own thread.
        platform.entity(e.getPlayer(), () -> apply(e.getPlayer()));
    }

    /** Call on the player's thread. */
    public void apply(Player p) {
        Profile profile = worlds.profileOf(p.getWorld());
        Double g = profile == null ? null : profile.playerGravity();
        set(p.getAttribute(Attribute.GRAVITY), g == null ? 0 : g - 1);
        set(p.getAttribute(Attribute.SAFE_FALL_DISTANCE), g == null ? 0 : 1 / g - 1);
    }

    private static void set(AttributeInstance a, double amount) {
        if (a == null) {
            return;
        }
        a.removeModifier(KEY);
        if (amount != 0) {
            a.addTransientModifier(new AttributeModifier(KEY, amount, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
        }
    }
}
