package com.sack.rpgroll.emotes;

import kr.toxicity.model.api.BetterModelPlatform.ReloadResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** /emotesadmin reload: la recarga de BetterModel va fuera del hilo principal y su resultado vuelve a él. */
class ReloadOffMainThreadTest {

    private final List<Runnable> async = new ArrayList<>();
    private final List<Runnable> main = new ArrayList<>();
    private final List<String> log = new ArrayList<>();
    private final AtomicInteger reloads = new AtomicInteger();

    private void reload(ReloadResult result) {
        EmotesPlugin.reloadOffMainThread(async::add, main::add, () -> {
            reloads.incrementAndGet();
            return result;
        }, () -> log.add("ya recargando"), log::add);
    }

    @Test
    void reloadRunsOnlyInTheAsyncTask() {
        reload(ReloadResult.OnReload.INSTANCE);
        assertEquals(0, reloads.get());
        assertEquals(1, async.size());

        async.getFirst().run();
        assertEquals(1, reloads.get());
        // El coordinador solo se toca en el hilo principal.
        assertTrue(log.isEmpty());
        main.getFirst().run();
        assertEquals(List.of("ya recargando"), log);
    }

    @Test
    void failureIsLoggedOnMainThread() {
        reload(new ReloadResult.Failure(new IllegalStateException("roto")));
        async.getFirst().run();
        assertTrue(log.isEmpty());
        main.getFirst().run();
        assertEquals(List.of("Detalle del fallo de BetterModel: roto"), log);
    }
}
