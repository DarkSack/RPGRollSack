package com.sack.rpgroll.emotes.service;

import com.sack.rpgroll.emotes.catalog.EmoteMusic;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Audio de mentira: apunta las llamadas y deja disparar a mano las tareas programadas. */
public class FakeEmoteAudio implements EmoteAudio {

    /** Una tarea programada con {@link #later}. */
    public static final class Task implements EmoteAudio.Cancellable {
        public final Runnable run;
        public final long ticks;
        public boolean cancelled;

        Task(Runnable run, long ticks) {
            this.run = run;
            this.ticks = ticks;
        }

        @Override
        public void cancel() {
            cancelled = true;
        }
    }

    /** Llamadas en orden: {@code play:<sonido>} o {@code stop:<sonido>}. */
    public final List<String> calls = new ArrayList<>();
    public final List<Task> tasks = new ArrayList<>();

    @Override
    public void playMusic(Player p, EmoteMusic m) {
        calls.add("play:" + m.sound());
    }

    @Override
    public void stopMusic(Player p, EmoteMusic m) {
        calls.add("stop:" + m.sound());
    }

    @Override
    public Cancellable later(Runnable r, long ticks) {
        Task t = new Task(r, ticks);
        tasks.add(t);
        return t;
    }

    /** Dispara la última tarea programada, aunque esté cancelada (como un tick que ya estaba en cola). */
    public void fireLast() {
        tasks.get(tasks.size() - 1).run.run();
    }

    public long pending() {
        return tasks.stream().filter(t -> !t.cancelled).count();
    }
}
