package com.sack.rpgroll.tab.teams;

import com.sack.rpgroll.common.content.RPGContent;

/**
 * Prefix/suffix/color aplicados vía Bukkit Scoreboard Team — es lo que
 * controla tanto el color del nombre en el mundo/tablist como collision y
 * friendly-fire. {@code prefix}/{@code suffix} se truncan a 64 caracteres
 * (límite del protocolo tras aplicarse el color).
 */
public record TeamsDefinition(
        String id,
        String prefix,
        String suffix,
        String color,
        boolean allowFriendlyFire,
        boolean canSeeFriendlyInvisibles,
        String collisionRule,
        String nametagVisibility) implements RPGContent {
}
