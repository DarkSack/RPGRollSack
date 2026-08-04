package com.sack.rpgroll.chat.context;

import com.sack.rpgroll.guilds.GuildsAPI;

import org.bukkit.WeatherType;
import org.bukkit.entity.Player;

/**
 * Chat contextual — spec: "el formato puede cambiar según mundo/región/
 * pvp/clima/hora" (dungeon/evento/boss/guerra quedan fuera de esta pasada,
 * ya que requerirían integración directa con RPGRoll-Dungeons/Mobs, no
 * incluida en el alcance "núcleo" elegido). Se expone como una etiqueta
 * corta ({@code {context_prefix}}) que los canales pueden insertar en su
 * formato en vez de un motor de formatos condicionales completo.
 */
public class ChatContextResolver {

    public String contextPrefix(Player player) {

        StringBuilder prefix = new StringBuilder();

        if (player.getWorld().getPVP()) {
            prefix.append("&c[PvP]&r ");
        }

        if (player.getWorld().hasStorm()) {
            prefix.append("&9[Lluvia]&r ");
        } else if (player.getPlayerWeather() == WeatherType.DOWNFALL) {
            prefix.append("&9[Lluvia]&r ");
        }

        long time = player.getWorld().getTime();
        boolean night = time >= 13000 && time <= 23000;
        if (night) {
            prefix.append("&8[Noche]&r ");
        }

        if (GuildsAPI.isReady()) {
            GuildsAPI.getGuildManager().getAll().stream()
                    .filter(guild -> guild.territories().stream().anyMatch(t -> t.contains(player.getLocation())))
                    .findFirst()
                    .ifPresent(guild -> prefix.append("&2[").append(guild.name()).append("]&r "));
        }

        return prefix.toString();
    }

}
