package com.sack.rpgroll.tab.teams;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

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
    private final Supplier<Scoreboard> factory;

    public PlayerScoreboardService() {
        this(() -> Bukkit.getScoreboardManager().getNewScoreboard());
    }

    PlayerScoreboardService(Supplier<Scoreboard> factory) {
        this.factory = factory;
    }

    public Scoreboard getOrCreate(Player player) {

        Scoreboard board = boards.computeIfAbsent(player.getUniqueId(), id -> factory.get());
        // Al reconectar es otro Player que arranca con el scoreboard principal, y otro plugin
        // puede habérselo cambiado: sin esto se pinta en un tablero que ya no ve (sin sidebar).
        if (player.getScoreboard() != board) {
            player.setScoreboard(board);
        }
        return board;
    }

    public void remove(Player player) {
        boards.remove(player.getUniqueId());
    }

}
