package com.sack.rpgroll.emotes.pack;

import com.sack.rpgroll.sackresourcepack.api.AssetsAPI;

import kr.toxicity.model.api.BetterModel;
import kr.toxicity.model.api.BetterModelPlatform.ReloadResult;
import kr.toxicity.model.api.bukkit.BetterModelBukkit;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;

/**
 * Mete el pack de recursos que genera BetterModel ({@code plugins/BetterModel/build.zip}) en SackResourcePack,
 * como el módulo {@code content/bettermodel/}. Sin esto, en un servidor que sirve el pack por SRP nadie ve los
 * emotes: el cliente solo recibe el pack de SRP.
 * <p>
 * Se copia tras cada recarga correcta de BetterModel, también la carga inicial del arranque; al crearlo no
 * copia nada, porque en ese momento el zip puede ser el de la sesión anterior. No publica: el admin aplica con {@code /srp publish} ({@code /srp rebuild} deja el hash viejo y
 * el cliente no descarga el pack nuevo).
 * <p>
 * Solo se crea si SackResourcePack está activo: esta clase es la única que toca sus clases.
 * <p>
 * BetterModel no deja quitar un handler de fin de recarga. Desactivar y activar Emotes en caliente (PlugManX)
 * registra otro, y el viejo sigue vivo con la instancia anterior; como esa instancia ya está desactivada, sale
 * sin copiar y solo copia el de la activación actual. Aun así, el cargador de clases viejo ya está cerrado y
 * puede dejar errores en el log. Solo un reinicio del servidor deja un único handler.
 * <p>
 * El handler puede llegar fuera del hilo principal (la recarga de /emotesadmin reload va en otro hilo): solo
 * copia archivos y escribe en el log, sin otra API de Bukkit; registerArchive no lanza eventos por eso.
 */
public final class SrpBridge {

    static final String MODULE = "bettermodel";
    static final String DEFAULT_BUILD = "BetterModel/build";

    private final Plugin emotes;

    public SrpBridge(Plugin emotes) {
        this.emotes = emotes;
        // Una recarga fallida puede dejar el zip a medias: solo se copia tras una que salió bien.
        BetterModelBukkit.platform().addReloadEndHandler(r -> {
            // El de una activación anterior (PlugManX) sigue registrado: no copia nada.
            if (!emotes.isEnabled()) {
                return;
            }
            if (r instanceof ReloadResult.Success) {
                sync();
            }
        });
    }

    /** Copia el zip actual de BetterModel a SackResourcePack y avisa en el log. */
    public synchronized void sync() {
        if (!AssetsAPI.isReady()) {
            emotes.getLogger().warning("SackResourcePack aún no está listo: el pack de BetterModel no se copió.");
            return;
        }
        // build-folder-location es relativo a plugins/ (por defecto BetterModel/build).
        File zip = new File(emotes.getDataFolder().getParentFile(), buildFolderLocation() + ".zip");
        boolean copied;
        try {
            copied = AssetsAPI.assets().registerArchive(MODULE, zip, MODULE, 20, "Modelos y emotes de BetterModel");
        } catch (LinkageError e) {
            // Un SackResourcePack anterior a registerArchive (1.1.9 o antes).
            emotes.getLogger().warning("✘ Este SackResourcePack no sabe copiar el pack de BetterModel; actualízalo. ("
                    + e.getMessage() + ")");
            return;
        }
        if (copied) {
            patchArmors();
            emotes.getLogger().info("✔ Pack de BetterModel copiado a SackResourcePack (content/" + MODULE
                    + "). Aplícalo con /srp publish.");
        } else {
            emotes.getLogger().warning("✘ No se pudo copiar " + zip + " a SackResourcePack. Comprueba que"
                    + " BetterModel tenga pack-type: zip en su config.yml.");
        }
    }

    /** Las armaduras de otros packs (sackito-items…) en el modelo del jugador; ver {@link ArmorPatcher}. */
    private void patchArmors() {
        Plugin srp = emotes.getServer().getPluginManager().getPlugin("SackResourcePack");
        if (srp == null) {
            return;
        }
        File content = new File(srp.getDataFolder(), srp.getConfig().getString("content-directory", "content"));
        try {
            new ArmorPatcher(content.toPath(), emotes.getLogger()::info).patch();
        } catch (IOException | RuntimeException e) {
            emotes.getLogger().warning("✘ No se pudieron añadir las armaduras propias al pack de BetterModel: " + e);
        }
    }

    private static String buildFolderLocation() {
        try {
            String location = BetterModel.config().buildFolderLocation();
            return location == null || location.isBlank() ? DEFAULT_BUILD : location;
        } catch (RuntimeException e) {
            return DEFAULT_BUILD;
        }
    }
}
