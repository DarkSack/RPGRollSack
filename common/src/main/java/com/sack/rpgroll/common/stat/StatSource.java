package com.sack.rpgroll.common.stat;

import org.bukkit.entity.Player;

/**
 * Algo del jugador que le suma a un stat que no es un atributo de Minecraft:
 * {@code mana}, {@code magic_power}, {@code magic_resistance}… (la evolución,
 * la especialización y los talentos de Ascension). Se registra con
 * {@link Stats#register}.
 */
public interface StatSource {

    /** @return lo que suma a {@code stat}, 0 si no lo conoce */
    double bonus(Player player, String stat);

}
