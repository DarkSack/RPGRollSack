package com.sack.rpgroll.ascension.listener;

import com.sack.rpgroll.ascension.core.Affinity;
import com.sack.rpgroll.ascension.core.AffinityManager;
import com.sack.rpgroll.ascension.player.AscensionPlayerState;
import com.sack.rpgroll.ascension.player.AscensionPlayerStateManager;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Único gancho mecánico real del sistema de afinidades: el nivel de
 * afinidad del jugador (derivado de su experiencia de afinidad) reduce el
 * daño de las causas de daño que esa afinidad resiste (fuego, ahogo,
 * congelamiento, veneno, etc. según {@link Affinity#resistCauses()}).
 * Tope de 20% de reducción en nivel 100, para no volver trivial ningún
 * contenido.
 */
public class AffinityCombatListener implements Listener {

    private static final int MAX_AFFINITY_LEVEL = 100;
    private static final int XP_PER_LEVEL = 100;
    private static final double MAX_REDUCTION_PERCENT = 20.0;

    private final AffinityManager affinityManager;
    private final AscensionPlayerStateManager stateManager;

    public AffinityCombatListener(AffinityManager affinityManager, AscensionPlayerStateManager stateManager) {
        this.affinityManager = affinityManager;
        this.stateManager = stateManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {

        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        String cause = event.getCause().name();

        for (Affinity affinity : affinityManager.getAll()) {

            if (affinity.resistCauses().stream().noneMatch(cause::equalsIgnoreCase)) {
                continue;
            }

            int level = getAffinityLevel(player, affinity.id());
            if (level <= 0) {
                continue;
            }

            double reductionPercent = Math.min(MAX_REDUCTION_PERCENT, level * (MAX_REDUCTION_PERCENT / MAX_AFFINITY_LEVEL));
            event.setDamage(event.getDamage() * (1 - (reductionPercent / 100.0)));
        }
    }

    private int getAffinityLevel(Player player, String affinityId) {

        AscensionPlayerState state = stateManager.getOrLoad(player);
        int xp = state.getAffinityExperience(affinityId);

        return Math.min(MAX_AFFINITY_LEVEL, xp / XP_PER_LEVEL);
    }

}
