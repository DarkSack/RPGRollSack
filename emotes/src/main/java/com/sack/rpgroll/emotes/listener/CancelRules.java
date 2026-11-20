package com.sack.rpgroll.emotes.listener;

import com.sack.rpgroll.emotes.catalog.CancelReason;
import com.sack.rpgroll.emotes.catalog.Emote;
import com.sack.rpgroll.emotes.service.EmoteSession;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

/** Reglas puras de corte, sin Bukkit en marcha para poder probarlas. */
public final class CancelRules {

    private CancelRules() {
    }

    /** Solo cuenta el plano X/Z: girar la cabeza o saltar en el sitio no es moverse. Otro mundo sí. */
    public static boolean movedTooFar(Location start, Location now, double max) {
        if (start.getWorld() != now.getWorld()) {
            return true;
        }
        double dx = now.getX() - start.getX();
        double dz = now.getZ() - start.getZ();
        return dx * dx + dz * dz > max * max;
    }

    /** Las sesiones mágicas no se cortan por nada de esto; el resto, solo por los motivos del emote. */
    public static boolean cancels(Emote emote, EmoteSession s, CancelReason r) {
        return !s.magic() && emote.cancel().contains(r);
    }

    /** Golpear cuenta como combate solo contra un ser vivo distinto de uno mismo y que no sea un soporte de armadura. */
    public static boolean countsAsCombat(Entity attacker, Entity victim) {
        return attacker != victim && victim instanceof LivingEntity && !(victim instanceof ArmorStand);
    }
}
