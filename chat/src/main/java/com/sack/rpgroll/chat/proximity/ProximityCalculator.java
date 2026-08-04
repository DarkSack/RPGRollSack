package com.sack.rpgroll.chat.proximity;

import com.sack.rpgroll.chat.channel.ChatChannel;
import com.sack.rpgroll.chat.channel.ChannelScope;

import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Resuelve alcance de canales PROXIMITY — spec: radio/mundo/atenuación por
 * distancia/obstáculos/dimensiones. "Dimensiones" se resuelve exigiendo
 * mismo mundo (Bukkit ya trata cada mundo como una dimensión separada).
 */
public final class ProximityCalculator {

    private ProximityCalculator() {
    }

    public static boolean canHear(Player sender, Player receiver, ChatChannel channel) {

        if (channel.scope() != ChannelScope.PROXIMITY) {
            return true;
        }

        if (sender.equals(receiver)) {
            return true;
        }

        if (!sender.getWorld().equals(receiver.getWorld())) {
            return false;
        }

        double distance = sender.getLocation().distance(receiver.getLocation());

        if (channel.distance() > 0 && distance > channel.distance()) {
            return false;
        }

        return !isObstructed(sender.getEyeLocation(), receiver.getEyeLocation());
    }

    /**
     * @return true si un mensaje entre estas dos ubicaciones se atenúa por
     *         estar en la mitad exterior del radio del canal — se usa para
     *         mostrar el mensaje en un gris más tenue ("atenuación por distancia").
     */
    public static boolean isAttenuated(Player sender, Player receiver, ChatChannel channel) {

        if (channel.scope() != ChannelScope.PROXIMITY || channel.distance() <= 0) {
            return false;
        }

        double distance = sender.getLocation().distance(receiver.getLocation());
        return distance > channel.distance() / 2.0;
    }

    private static boolean isObstructed(Location from, Location to) {

        if (!from.getWorld().equals(to.getWorld())) {
            return true;
        }

        double distance = from.distance(to);

        if (distance <= 0) {
            return false;
        }

        var direction = to.toVector().subtract(from.toVector()).normalize();
        var result = from.getWorld().rayTraceBlocks(from, direction, distance, FluidCollisionMode.NEVER, true);

        return result != null && result.getHitBlock() != null;
    }

}
