package com.sack.rpgroll.tab.teams;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bukkit solo permite UN {@link Scoreboard} activo por jugador — ese único
 * objeto determina a la vez los colores de nametag que ve (vía Teams) Y su
 * sidebar/belowname. Para que cada jugador pueda tener un sidebar distinto
 * sin que los demás pierdan sus colores de equipo, cada jugador online
 * necesita su PROPIO Scoreboard, con los Teams de todos los demás
 * espejados en él (ver {@link TeamsEngine}).
 * <p>
 * Sin esto, un jugador con Scoreboard individual "perdería" el nametag
 * coloreado de todo el resto del servidor, porque los Teams del scoreboard
 * principal no existen en su tablero personal.
 */
public class PlayerScoreboardService {

    private final Map<UUID, Scoreboard> boards = new ConcurrentHashMap<>();

    public Scoreboard getOrCreate(Player player) {

        return boards.computeIfAbsent(player.getUniqueId(), id -> {

            Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
            player.setScoreboard(board);
            return board;
        });
    }

    public void remove(Player player) {
        boards.remove(player.getUniqueId());
    }

}
