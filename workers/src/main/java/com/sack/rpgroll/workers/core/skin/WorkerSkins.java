package com.sack.rpgroll.workers.core.skin;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.workers.core.worker.Worker;
import com.sack.rpgroll.workers.core.worker.WorkerManager;

import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;

/**
 * Poner o quitar la skin de un worker a partir de lo que escribe el staff, ya sea por
 * {@code /workersadmin skin} o desde su ficha: resuelve, guarda, cambia el cuerpo y avisa.
 */
public class WorkerSkins {

    /** Lo que se puede escribir para quitarle la skin (vuelve al aspecto de su profesión). */
    public static final List<String> CLEAR_WORDS = List.of("quitar", "ninguna", "remove", "none", "remover",
            "nenhuma");

    private final SkinResolver resolver;
    private final WorkerBodyService bodies;
    private final WorkerManager workerManager;
    private final LangManager lang;

    public WorkerSkins(SkinResolver resolver, WorkerBodyService bodies, WorkerManager workerManager, LangManager lang) {
        this.resolver = resolver;
        this.bodies = bodies;
        this.workerManager = workerManager;
        this.lang = lang;
    }

    public WorkerBodyService bodies() {
        return bodies;
    }

    /**
     * @param input enlace o id de MineSkin, nombre de un jugador, o una de {@link #CLEAR_WORDS}
     * @param after se ejecuta cuando la skin ya cambió (para refrescar una GUI, por ejemplo)
     */
    public void apply(Player player, Worker worker, String input, Runnable after) {

        String text = input == null ? "" : input.trim();

        if (text.isEmpty()) {
            lang.send(player, "skin.usage");
            return;
        }

        if (CLEAR_WORDS.contains(text.toLowerCase(Locale.ROOT))) {
            worker.setSkin(null);
            workerManager.save(worker);
            bodies.refresh(worker);
            lang.send(player, "skin.cleared");
            after.run();
            return;
        }

        lang.send(player, "skin.resolving");

        resolver.resolve(text, skin -> {

            // Murió o se recargó el plugin mientras se buscaba: ese worker ya no es este.
            if (workerManager.get(worker.id()).orElse(null) != worker) {
                return;
            }

            worker.setSkin(skin);
            workerManager.save(worker);
            bodies.refresh(worker);

            if (player.isOnline()) {
                lang.send(player, "skin.applied");
            }

            after.run();
        }, reason -> {
            if (player.isOnline()) {
                lang.send(player, reason);
            }
        });
    }

}
