package com.sack.rpgroll.emotes.install;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class ReloadCoordinatorTest {

    private int reloads;
    private int refreshes;
    private final List<String> warns = new ArrayList<>();
    private final ReloadCoordinator coordinator =
            new ReloadCoordinator(() -> reloads++, () -> refreshes++, warns::add);

    @Test
    void startupWaitsForTheInitialLoadOfBetterModel() {
        coordinator.afterInstall(true, true);

        assertEquals(0, reloads, "al arrancar no se recarga: BetterModel hará su carga inicial");
        assertEquals(0, refreshes, "sin BetterModel cargado no se pueden comprobar las animaciones");

        coordinator.onReloadEnd(true);
        assertEquals(1, refreshes);
    }

    @Test
    void reloadsWhenSomethingWasInstalled() {
        coordinator.afterInstall(true, false);

        assertEquals(1, reloads);
        assertEquals(0, refreshes, "se comprueba al acabar la recarga, no antes");
    }

    @Test
    void refreshesNowWhenNothingChanged() {
        coordinator.afterInstall(false, false);

        assertEquals(0, reloads);
        assertEquals(1, refreshes);
    }

    @Test
    void failedReloadIsRetriedOnTheNextReload() {
        coordinator.afterInstall(true, false);
        coordinator.onReloadEnd(false);
        assertEquals(0, refreshes);
        assertEquals(1, warns.size());

        coordinator.afterInstall(false, false);
        assertEquals(2, reloads, "la recarga fallida queda pendiente");

        coordinator.onReloadEnd(true);
        coordinator.afterInstall(false, false);
        assertEquals(2, reloads, "tras una buena ya no queda nada pendiente");
        assertEquals(2, refreshes);
    }

    @Test
    void reloadAlreadyRunningIsRetriedToo() {
        coordinator.onAlreadyReloading();
        assertEquals(1, warns.size());

        coordinator.afterInstall(false, false);

        assertEquals(1, reloads);
    }

    @Test
    void externalReloadRefreshesTheCatalog() {
        // /bm reload de un admin, o la carga inicial: el catálogo se vuelve a comprobar.
        coordinator.onReloadEnd(true);
        coordinator.onReloadEnd(true);

        assertEquals(2, refreshes);
    }
}
