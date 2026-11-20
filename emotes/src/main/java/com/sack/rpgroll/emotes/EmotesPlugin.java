package com.sack.rpgroll.emotes;

import com.sack.rpgroll.common.command.BrigadierCommands;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.common.resource.ResourceCopier;
import com.sack.rpgroll.emotes.install.AnimationInstaller;
import com.sack.rpgroll.emotes.install.ReloadCoordinator;
import com.sack.rpgroll.emotes.command.EmoteCommand;
import com.sack.rpgroll.emotes.command.EmotesAdminCommand;
import com.sack.rpgroll.emotes.catalog.EmoteCatalog;
import com.sack.rpgroll.emotes.engine.BetterModelEngine;
import com.sack.rpgroll.emotes.gui.EmotesMenu;
import com.sack.rpgroll.emotes.gui.QuickDialog;
import com.sack.rpgroll.emotes.listener.CancelListener;
import com.sack.rpgroll.emotes.listener.HotkeyListener;
import com.sack.rpgroll.emotes.magic.MagicBridge;
import com.sack.rpgroll.emotes.pack.SrpBridge;
import com.sack.rpgroll.emotes.player.EmotePlayerStore;
import com.sack.rpgroll.emotes.service.BukkitEmoteAudio;
import com.sack.rpgroll.emotes.service.EmoteService;
import com.sack.rpgroll.gui.MenuSkin;
import com.sack.rpgroll.license.identity.LicenseIdentity;
import com.sack.rpgroll.licensing.LicenseGate;

import kr.toxicity.model.api.BetterModelPlatform.ReloadResult;
import kr.toxicity.model.api.bukkit.BetterModelBukkit;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * RPGRoll-Emotes: emotes y animaciones del jugador sobre BetterModel.
 * <p>
 * Conecta licencia, config, idiomas, menús, catálogo, datos de jugador, servicio y cortes. Con los comandos
 * {@code /emote}, {@code /emotes} (menú; {@code /emotes rapido}, diálogo de favoritos) y {@code /emotesadmin}.
 * Shift + F abre el menú.
 */
public class EmotesPlugin extends JavaPlugin {

    /** Módulo con el que se registra el fondo de los menús en {@link MenuSkin}. */
    public static final String MENU_MODULE = "emotes";

    private LangManager lang;
    private EmotesSettings settings;
    private EmoteCatalog catalog;
    private BetterModelEngine engine;
    private ReloadCoordinator reloads;
    private EmotePlayerStore store;
    private EmoteService service;

    @Override
    public void onEnable() {

        if (!LicenseGate.verify(this, LicenseIdentity.RESOURCE_ID, LicenseIdentity.PRODUCT_SLUG,
                LicenseIdentity.VERIFY_TOKEN)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        lang = new LangManager(this, List.of("es", "en", "pt_BR"), "es");
        settings = EmotesSettings.from(getConfig(), getLogger()::warning);
        lang.reload(getConfig().getString("language", "es"));
        MenuSkin.register(MENU_MODULE, MenuSkin.from(getConfig().getConfigurationSection("menus")));

        // Los de fábrica quedan en plugins/RPGRoll-Emotes/, junto a los que añaden los packs.
        new ResourceCopier(this).copyDirectories(List.of("emotes", "animations"));
        store = new EmotePlayerStore(getDataFolder(), settings.maxFavorites());
        catalog = new EmoteCatalog(settings.limbModel());
        engine = new BetterModelEngine(this);
        reloads = new ReloadCoordinator(this::reloadBetterModel, this::validateCatalog, getLogger()::warning);
        // Cualquier fin de recarga (la inicial, la nuestra o un /bm reload) vuelve a comprobar el catálogo.
        // No se puede quitar: tras desactivar en caliente (PlugManX) el viejo sigue registrado y no hace nada.
        BetterModelBukkit.platform().addReloadEndHandler(r -> {
            if (!isEnabled()) {
                return;
            }
            onMainThread(() -> reloads.onReloadEnd(r instanceof ReloadResult.Success));
        });
        // Tick 0 = arranque del servidor: BetterModel aún hará su carga inicial. Activado en caliente
        // (PlugManX) ya cargó, así que se trata como un /emotesadmin reload.
        boolean serverStarting = Bukkit.getCurrentTick() == 0;
        loadContent(serverStarting);
        registerPack(serverStarting);
        service = new EmoteService(catalog, store, engine, () -> settings,
                System::currentTimeMillis, new BukkitEmoteAudio(this));
        getServer().getPluginManager().registerEvents(
                new CancelListener(service, catalog, store, () -> settings, System::currentTimeMillis), this);
        getServer().getPluginManager().registerEvents(new MagicBridge(service, catalog, () -> settings,
                (task, ticks) -> Bukkit.getScheduler().runTaskLater(this, task, ticks)), this);
        // Tras una recarga ya hay gente conectada: sin esto no tendrían sus datos cargados.
        for (Player p : Bukkit.getOnlinePlayers()) {
            store.load(p.getUniqueId());
        }

        // /emote sin argumento y /emotes abren el menú; el comando se declara antes y el menú lo usa para lanzar.
        EmoteCommand[] holder = new EmoteCommand[1];
        Consumer<Player> openMenu = p -> new EmotesMenu(p, service, catalog, store, holder[0], lang).open();
        EmoteCommand emoteCommand = new EmoteCommand(service, catalog, store, lang, openMenu);
        holder[0] = emoteCommand;
        BrigadierCommands.register(this, "emote", "Lanza un emote, un favorito o para el actual", List.of("e"),
                emoteCommand, emoteCommand, "rpgrollemotes.use");
        // La etiqueta quick_actions solo se toca desde un bootstrapper (paper-plugin.yml): de ahí /emotes rapido.
        Consumer<Player> openQuick = p -> {
            if (settings.quickActionsHotkey()) {
                QuickDialog.show(p, store.get(p.getUniqueId()), catalog, lang);
            } else {
                lang.send(p, "quick.disabled");
            }
        };
        BrigadierCommands.register(this, "emotes", "Abre el menú de emotes", List.of(),
                emoteCommand.menuExecutor(openQuick), emoteCommand.menuCompleter(), "rpgrollemotes.use");
        getServer().getPluginManager().registerEvents(new HotkeyListener(() -> settings, openMenu), this);
        EmotesAdminCommand admin = new EmotesAdminCommand(service, catalog, store, lang, this::reloadAll,
                Bukkit::getPlayerExact, Bukkit::getOfflinePlayerIfCached);
        BrigadierCommands.register(this, "emotesadmin", "Administra los emotes", List.of(), admin, admin,
                "rpgrollemotes.admin");

        getLogger().info("✔ RPGRoll-Emotes habilitado.");
    }

    /**
     * Instala las animaciones en BetterModel, carga el catálogo desde {@code plugins/RPGRoll-Emotes/emotes/}
     * con sus subcarpetas y quita los emotes sin animación.
     * <p>
     * En el arranque no recarga BetterModel ni valida: su carga inicial (después de este onEnable) ya lee
     * los archivos instalados, y recargar aquí bloquearía el hilo principal otro tanto. La validación la
     * hace el handler de fin de recarga. Con /emotesadmin reload sí recarga si cambió algo.
     */
    private void loadContent(boolean startup) {
        File players = new File(getDataFolder().getParentFile(), "BetterModel/players");
        boolean installed = new AnimationInstaller(getDataFolder(), players, factoryModel(), getLogger()::warning)
                .install();
        // Hasta que BetterModel confirme, los emotes nuevos se ofrecen sin comprobar; no se quita ninguno.
        catalog.load(List.of(new File(getDataFolder(), "emotes")), getLogger()::warning);
        reloads.afterInstall(installed, startup);
    }

    /** Vuelve a leer el catálogo y quita los emotes cuya animación no tiene BetterModel. */
    private void validateCatalog() {
        catalog.load(List.of(new File(getDataFolder(), "emotes")), getLogger()::warning);
        catalog.checkAnimations(engine::exists, getLogger()::warning);
    }

    /** El handler de BetterModel puede llegar fuera del hilo principal; el catálogo se toca solo en él. */
    private void onMainThread(Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else if (isEnabled()) {
            Bukkit.getScheduler().runTask(this, task);
        }
    }

    /**
     * Copia el pack de BetterModel a SackResourcePack tras cada recarga de BetterModel (también la inicial). Sin SRP, el
     * puente no se crea y sus clases no se cargan. {@code resource-pack.register-in-sackresourcepack} se lee
     * solo al arrancar. Activado en caliente, copia ya: puede que BetterModel no vuelva a recargar.
     */
    private void registerPack(boolean serverStarting) {
        if (!settings.registerInSrp() || !getServer().getPluginManager().isPluginEnabled("SackResourcePack")) {
            return;
        }
        try {
            // Al arrancar no copia: el handler de SrpBridge lo hace al terminar la carga inicial de BetterModel.
            SrpBridge bridge = new SrpBridge(this);
            if (!serverStarting) {
                // En otro hilo: si el zip cambió son miles de archivos (sync solo copia y escribe en el log).
                Bukkit.getScheduler().runTaskAsynchronously(this, bridge::sync);
            }
        } catch (LinkageError e) {
            getLogger().warning("✘ No se pudo conectar con SackResourcePack: " + e.getMessage());
        }
    }

    /** El modelo de fábrica tal como viene en el jar, o null si falta. */
    private byte[] factoryModel() {
        try (InputStream in = getResource("animations/" + AnimationInstaller.FACTORY_FILE)) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException e) {
            getLogger().warning("No se pudo leer el modelo de fábrica del jar: " + e.getMessage());
            return null;
        }
    }

    /**
     * Lanza la recarga de BetterModel; su resultado llega al coordinador por el handler de fin de recarga.
     * Va fuera del hilo principal, como el propio /bm reload: es síncrona y sus handlers (la copia de
     * SrpBridge, miles de archivos) corren en el mismo hilo, así que congelaría el servidor.
     */
    private void reloadBetterModel() {
        reloadOffMainThread(task -> Bukkit.getScheduler().runTaskAsynchronously(this, task), this::onMainThread,
                BetterModelBukkit.platform()::reload, reloads::onAlreadyReloading, getLogger()::warning);
    }

    /** Recarga en {@code async}; lo que toca al coordinador o avisa vuelve por {@code main}. */
    static void reloadOffMainThread(Consumer<Runnable> async, Consumer<Runnable> main,
                                    Supplier<ReloadResult> reload, Runnable alreadyReloading,
                                    Consumer<String> warn) {
        async.accept(() -> {
            ReloadResult result = reload.get();
            if (result instanceof ReloadResult.OnReload) {
                main.accept(alreadyReloading);
            } else if (result instanceof ReloadResult.Failure f) {
                main.accept(() -> warn.accept("Detalle del fallo de BetterModel: " + f.throwable().getMessage()));
            }
        });
    }

    /**
     * Recarga config, ajustes, idiomas, fondo de menús, animaciones y catálogo. Los datos de jugador no se tocan.
     * No recarga {@code limb-model} ni {@code favorites.max}: se fijan al crear el catálogo y el almacén,
     * así que esos cambios necesitan reiniciar. Tampoco {@code resource-pack.register-in-sackresourcepack}.
     */
    public void reloadAll() {
        reloadConfig();
        settings = EmotesSettings.from(getConfig(), getLogger()::warning);
        lang.reload(getConfig().getString("language", "es"));
        MenuSkin.register(MENU_MODULE, MenuSkin.from(getConfig().getConfigurationSection("menus")));
        loadContent(false);
    }

    @Override
    public void onDisable() {
        // Nulos si la licencia desactivó el plugin antes de crearlos.
        if (service != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                service.clear(p.getUniqueId());
            }
        }
        if (store != null) {
            store.saveAll();
        }
    }

    public EmoteService service() {
        return service;
    }

    public EmoteCatalog catalog() {
        return catalog;
    }

    public EmotePlayerStore store() {
        return store;
    }

    public EmotesSettings settings() {
        return settings;
    }

    public LangManager lang() {
        return lang;
    }
}
