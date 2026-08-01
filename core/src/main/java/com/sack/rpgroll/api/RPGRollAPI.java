package com.sack.rpgroll.api;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.gameplay.job.JobManager;
import com.sack.rpgroll.gameplay.skill.SkillManager;
import com.sack.rpgroll.gameplay.trait.TraitManager;
import com.sack.rpgroll.integration.VaultEconomyProvider;
import com.sack.rpgroll.player.PlayerManager;
import com.sack.rpgroll.player.RPGPlayer;
import com.sack.rpgroll.api.playerclass.ClassManager;
import com.sack.rpgroll.api.race.RaceManager;

import java.util.Optional;
import java.util.UUID;

/**
 * Punto de entrada público del framework RPGRoll para addons externos.
 * <p>
 * <b>Requisitos para un addon que consuma esta API:</b>
 * <ol>
 * <li>Declarar {@code depend: [RPGRoll]} en su {@code plugin.yml},
 * para garantizar que RPGRoll esté cargado antes que el addon.</li>
 * <li>Agregar RPGRoll como dependencia {@code compileOnly} en su build,
 * vía Maven local ({@code ./gradlew publishToMavenLocal} desde
 * el proyecto de RPGRoll) o referencia directa al jar compilado.</li>
 * <li>Llamar {@link #get()} únicamente desde {@code onEnable()} en
 * adelante — nunca desde el constructor del plugin ni desde
 * inicializadores estáticos, ya que RPGRoll podría no estar
 * listo todavía en esos momentos.</li>
 * </ol>
 * <p>
 * Ejemplo de uso típico:
 * 
 * <pre>{@code
 * public class MiAddon extends JavaPlugin {
 *     @Override
 *     public void onEnable() {
 *         RPGRollAPI api = RPGRollAPI.get();
 *         getServer().getPluginManager().registerEvents(new MiListener(api), this);
 *     }
 * }
 * }</pre>
 * <p>
 * Contrato de estabilidad: solo las clases en {@code com.sack.rpgroll.api}
 * y {@code com.sack.rpgroll.api.event} son superficie pública soportada.
 * Los managers devueltos por esta clase (RaceManager, JobManager, etc.)
 * también son parte del contrato público — sus métodos no cambian de
 * firma sin un bump de versión mayor. Cualquier otra clase interna del
 * framework puede cambiar libremente entre versiones.
 */
public final class RPGRollAPI {

    private static RPGRollAPI instance;

    private final RPGRoll plugin;

    private RPGRollAPI(RPGRoll plugin) {
        this.plugin = plugin;
    }

    /**
     * Inicializa la API. Llamado únicamente por Bootstrap durante el arranque
     * del plugin — los addons nunca deben llamar esto.
     */
    public static void init(RPGRoll plugin) {
        instance = new RPGRollAPI(plugin);
    }

    /**
     * Punto de entrada para addons.
     *
     * @throws IllegalStateException si RPGRoll todavía no terminó de arrancar.
     *                               No debería ocurrir nunca si el addon declaró
     *                               {@code depend: [RPGRoll]} correctamente y solo
     *                               llama a este
     *                               método desde {@code onEnable()} en adelante.
     */
    public static RPGRollAPI get() {
        if (instance == null) {
            throw new IllegalStateException(
                    "RPGRollAPI no está inicializada todavía. Verifica que tu plugin.yml "
                            + "tenga 'depend: [RPGRoll]' y que no estés llamando get() antes de onEnable().");
        }
        return instance;
    }

    /**
     * @return true si RPGRollAPI ya está lista para usarse. Útil para
     *         integraciones opcionales que no quieren declarar depend
     *         obligatorio, y prefieren chequear en runtime.
     */
    public static boolean isReady() {
        return instance != null;
    }

    // ============ Jugadores ============

    public Optional<RPGPlayer> getPlayer(UUID uuid) {
        return getPlayerManager().getPlayer(uuid);
    }

    /**
     * @return true si el jugador tiene un personaje creado (raza y clase
     *         asignadas). Atajo sobre getPlayer(uuid) + isCharacterComplete().
     */
    public boolean hasCompletedCharacter(UUID uuid) {
        return getPlayer(uuid).map(RPGPlayer::isCharacterComplete).orElse(false);
    }

    public PlayerManager getPlayerManager() {
        return plugin.getBootstrap().getServices().get(PlayerManager.class);
    }

    // ============ Contenido ============

    public RaceManager getRaceManager() {
        return plugin.getBootstrap().getServices().get(RaceManager.class);
    }

    public ClassManager getClassManager() {
        return plugin.getBootstrap().getServices().get(ClassManager.class);
    }

    public SkillManager getSkillManager() {
        return plugin.getBootstrap().getServices().get(SkillManager.class);
    }

    public TraitManager getTraitManager() {
        return plugin.getBootstrap().getServices().get(TraitManager.class);
    }

    public JobManager getJobManager() {
        return plugin.getBootstrap().getServices().get(JobManager.class);
    }

    // ============ Integraciones ============

    public VaultEconomyProvider getEconomyProvider() {
        return plugin.getBootstrap().getServices().get(VaultEconomyProvider.class);
    }

    /**
     * @return la versión del framework, útil para que addons verifiquen
     *         compatibilidad antes de usar features nuevas de la API.
     */
    public String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

}