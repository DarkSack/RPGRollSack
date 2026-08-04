package com.sack.rpgroll.guilds.buff;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** Aplica velocidad de movimiento y regeneración bonus periódicamente (spec: +15% velocidad, +20% regeneración). */
public class BuffTickTask implements Runnable {

    private static final float BASE_WALK_SPEED = 0.2f;

    private final BuffCalculator buffCalculator;

    public BuffTickTask(BuffCalculator buffCalculator) {
        this.buffCalculator = buffCalculator;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {

            float targetSpeed = (float) Math.min(1.0,
                    BASE_WALK_SPEED * (1 + buffCalculator.speedBonus(player.getUniqueId())));

            if (Math.abs(player.getWalkSpeed() - targetSpeed) > 0.001f) {
                player.setWalkSpeed(targetSpeed);
            }

            double regenBonus = buffCalculator.regenBonus(player.getUniqueId());

            if (regenBonus > 0 && player.getHealth() > 0 && player.getHealth() < player.getMaxHealth()) {
                double healed = Math.min(player.getMaxHealth(), player.getHealth() + regenBonus);
                player.setHealth(healed);
            }
        }
    }

}
