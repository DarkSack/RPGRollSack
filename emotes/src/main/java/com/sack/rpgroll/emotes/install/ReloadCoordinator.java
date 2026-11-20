package com.sack.rpgroll.emotes.install;

import java.util.function.Consumer;

/**
 * Decide cuándo recargar BetterModel y cuándo volver a comprobar el catálogo.
 * <p>
 * Las animaciones solo se pueden comprobar con BetterModel ya cargado. En el arranque su carga inicial llega
 * después del onEnable de RPGRoll-Emotes: recargar ahí bloquearía el hilo principal otro tanto, y comprobar
 * antes quitaría todos los emotes. Por eso, al arrancar, solo se espera al fin de esa carga. Una recarga
 * fallida (o pisada por otra en curso) queda pendiente y se reintenta en el siguiente /emotesadmin reload.
 */
public final class ReloadCoordinator {

    private final Runnable reloadBetterModel;
    private final Runnable refreshCatalog;
    private final Consumer<String> warn;
    private boolean pending;

    /**
     * @param reloadBetterModel lanza la recarga; su fin llega por {@link #onReloadEnd(boolean)}
     * @param refreshCatalog    vuelve a leer el catálogo y quita los emotes sin animación
     */
    public ReloadCoordinator(Runnable reloadBetterModel, Runnable refreshCatalog, Consumer<String> warn) {
        this.reloadBetterModel = reloadBetterModel;
        this.refreshCatalog = refreshCatalog;
        this.warn = warn;
    }

    /**
     * Tras instalar las animaciones.
     *
     * @param installed si se copió algún archivo nuevo o cambiado
     * @param startup   true en el arranque: BetterModel aún hará su carga inicial
     */
    public void afterInstall(boolean installed, boolean startup) {
        if (startup) {
            return;
        }
        if (installed || pending) {
            pending = true;
            reloadBetterModel.run();
        } else {
            refreshCatalog.run();
        }
    }

    /** Fin de cualquier recarga de BetterModel: la inicial, la nuestra o un /bm reload. */
    public void onReloadEnd(boolean success) {
        if (success) {
            pending = false;
            refreshCatalog.run();
            return;
        }
        pending = true;
        warn.accept("La recarga de BetterModel falló. Se reintenta con /emotesadmin reload (o usa /bm reload) "
                + "para ver las animaciones nuevas.");
    }

    /** BetterModel ya estaba recargando: la nuestra no se hizo y queda pendiente. */
    public void onAlreadyReloading() {
        pending = true;
        warn.accept("BetterModel ya estaba recargando. Se reintenta con /emotesadmin reload (o usa /bm reload).");
    }
}
