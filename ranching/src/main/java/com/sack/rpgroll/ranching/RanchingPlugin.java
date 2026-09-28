package com.sack.rpgroll.ranching;

import com.sack.rpgroll.licensing.LicenseGate;
import com.sack.rpgroll.license.identity.LicenseIdentity;

import com.sack.rpgroll.common.assets.ModuleAssetSync;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.common.resource.DirectoryCreator;
import com.sack.rpgroll.common.resource.ResourceCopier;
import com.sack.rpgroll.ranching.api.RanchingAPI;
import com.sack.rpgroll.ranching.command.RanchingAdminCommand;
import com.sack.rpgroll.ranching.command.RanchingCommand;
import com.sack.rpgroll.ranching.core.animal.AnimalManager;
import com.sack.rpgroll.ranching.core.breeding.BreedingEngine;
import com.sack.rpgroll.ranching.core.breeds.BreedManager;
import com.sack.rpgroll.ranching.core.breeding.PregnancyTask;
import com.sack.rpgroll.ranching.core.genetics.GeneManager;
import com.sack.rpgroll.ranching.core.genetics.GeneticsEngine;
import com.sack.rpgroll.ranching.core.genetics.GeneticsMode;
import com.sack.rpgroll.ranching.core.genetics.PedigreeService;
import com.sack.rpgroll.ranching.core.growth.GrowthTask;
import com.sack.rpgroll.ranching.core.health.DiseaseManager;
import com.sack.rpgroll.ranching.core.health.HealthTask;
import com.sack.rpgroll.ranching.core.health.MedicineManager;
import com.sack.rpgroll.ranching.core.health.VaccineManager;
import com.sack.rpgroll.ranching.core.nutrition.FeedManager;
import com.sack.rpgroll.ranching.core.species.SpeciesManager;
import com.sack.rpgroll.ranching.core.welfare.WelfareTask;
import com.sack.rpgroll.ranching.gui.ChatPromptManager;
import com.sack.rpgroll.ranching.item.ItemModels;
import com.sack.rpgroll.ranching.listener.AnimalCareListener;
import com.sack.rpgroll.ranching.listener.BreedingListener;
import com.sack.rpgroll.ranching.listener.ProductionListener;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;

/**
 * Punto de entrada de RPGRoll-Ranching. Cablea los 7 managers de
 * contenido, el Genetics & Bloodline Engine, el registro de animales, las
 * 4 tareas periódicas (crecimiento/bienestar/salud/embarazo) y los
 * listeners que reutilizan los mecanismos vanilla de cría/producción.
 */
public class RanchingPlugin extends JavaPlugin {

    private static final List<String> DIRECTORIES = List.of("species", "breeds", "genes", "feeds", "diseases",
            "vaccines", "medicines", "animals");
    private static final List<String> MODEL_DIRECTORIES = List.of("bedrock", "blockbench");

    private SpeciesManager speciesManager;
    private BreedManager breedManager;
    private GeneManager geneManager;
    private FeedManager feedManager;
    private DiseaseManager diseaseManager;
    private VaccineManager vaccineManager;
    private MedicineManager medicineManager;
    private AnimalManager animalManager;
    private LangManager langManager;

    @Override
    public void onEnable() {
        if (!LicenseGate.verify(this, LicenseIdentity.RESOURCE_ID, LicenseIdentity.PRODUCT_SLUG,
                LicenseIdentity.VERIFY_TOKEN)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }


        saveDefaultConfig();

        langManager = new LangManager(this, List.of("es", "en", "pt_BR"), "es");
        langManager.reload(getConfig().getString("language", "es"));

        new DirectoryCreator(this).create(DIRECTORIES);
        new ResourceCopier(this).copyDirectories(DIRECTORIES);

        new ModuleAssetSync(this, "ranching").syncAll();

        // Pack de Bedrock y proyectos de Blockbench de los modelos de fábrica, para el dueño.
        new DirectoryCreator(this).create(MODEL_DIRECTORIES);
        new ResourceCopier(this).copyDirectories(MODEL_DIRECTORIES);
        registerPack();

        ItemModels.loadProducts(getConfig().getConfigurationSection("product-models"));
        initializeManagers();

        GeneticsMode geneticsMode = parseGeneticsMode(getConfig().getString("genetics-mode", "ADVANCED"));
        double mutationChance = getConfig().getDouble("mutation-chance", 0.01);
        int inbreedingGenerations = getConfig().getInt("inbreeding-warning-generations", 3);

        GeneticsEngine geneticsEngine = new GeneticsEngine(geneticsMode, mutationChance);
        PedigreeService pedigreeService = new PedigreeService();

        animalManager = new AnimalManager(this);
        animalManager.loadAll();

        BreedingEngine breedingEngine = new BreedingEngine(speciesManager, breedManager, geneManager, geneticsEngine,
                pedigreeService, animalManager, inbreedingGenerations, langManager);

        RanchingAPI.init(speciesManager, breedManager, geneManager, feedManager, diseaseManager, vaccineManager,
                medicineManager, animalManager, geneticsEngine, pedigreeService, breedingEngine);

        ChatPromptManager chatPromptManager = new ChatPromptManager(this, langManager);
        getServer().getPluginManager().registerEvents(chatPromptManager, this);

        getServer().getPluginManager().registerEvents(new AnimalCareListener(animalManager, speciesManager,
                feedManager, medicineManager, vaccineManager, diseaseManager, langManager), this);
        getServer().getPluginManager().registerEvents(new BreedingListener(animalManager, breedingEngine), this);
        getServer().getPluginManager().registerEvents(
                new ProductionListener(animalManager, speciesManager, breedManager, geneManager, diseaseManager, langManager),
                this);

        startTasks(animalManager, breedingEngine, inbreedingGenerations);

        // Autoguardado cada 5 minutos: comer, curarse, vacunarse o crecer también tiene que sobrevivir a un cierre inesperado.
        getServer().getScheduler().runTaskTimer(this, animalManager::autosave, 5 * 60 * 20L, 5 * 60 * 20L);

        var ranchingAdminCommand = new RanchingAdminCommand(speciesManager, breedManager, geneManager,
                    feedManager, diseaseManager, vaccineManager, medicineManager, animalManager, geneticsEngine,
                    pedigreeService, breedingEngine, chatPromptManager, inbreedingGenerations, this::reloadContent);

        // Registrado por Brigadier para que `execute as` entregue al jugador real.
        com.sack.rpgroll.common.command.BrigadierCommands.register(this, "ranchingadmin",
                "Comandos administrativos de RPGRoll-Ranching (Ranch Studio)", "rpgrollranching.admin.*", ranchingAdminCommand);

        var ranchingCommand = new RanchingCommand(animalManager, speciesManager, breedManager, chatPromptManager);

        // Registrado por Brigadier para que `execute as` entregue al jugador real.
        com.sack.rpgroll.common.command.BrigadierCommands.register(this, "ranching",
                "Comandos de jugador de RPGRoll-Ranching", "rpgrollranching.use", ranchingCommand);

        getLogger().info("✔ RPGRoll-Ranching habilitado (genética: " + geneticsMode + "). "
                + speciesManager.count() + " especie(s), " + breedManager.count() + " raza(s), "
                + geneManager.count() + " gen(es), " + animalManager.getAll().size() + " animal(es) cargado(s).");
    }

    @Override
    public void onDisable() {
        if (animalManager != null) {
            animalManager.saveAll();
        }
    }

    private void initializeManagers() {

        speciesManager = new SpeciesManager(this);
        speciesManager.initialize();

        breedManager = new BreedManager(this);
        breedManager.initialize();

        geneManager = new GeneManager(this);
        geneManager.initialize();

        feedManager = new FeedManager(this);
        feedManager.initialize();

        diseaseManager = new DiseaseManager(this);
        diseaseManager.initialize();

        vaccineManager = new VaccineManager(this);
        vaccineManager.initialize();

        medicineManager = new MedicineManager(this);
        medicineManager.initialize();
    }

    private void reloadContent() {
        reloadConfig();
        langManager.reload(getConfig().getString("language", "es"));
        ItemModels.loadProducts(getConfig().getConfigurationSection("product-models"));
        speciesManager.reload();
        breedManager.reload();
        geneManager.reload();
        feedManager.reload();
        diseaseManager.reload();
        vaccineManager.reload();
        medicineManager.reload();
    }

    private void startTasks(AnimalManager animalManager, BreedingEngine breedingEngine, int inbreedingGenerations) {

        long growthInterval = getConfig().getLong("growth-check-interval-ticks", 200);
        long welfareInterval = getConfig().getLong("welfare-check-interval-ticks", 200);
        long healthInterval = getConfig().getLong("health-check-interval-ticks", 200);
        long pregnancyInterval = getConfig().getLong("pregnancy-check-interval-ticks", 100);
        double contagionRadius = getConfig().getDouble("disease-contagion-radius", 6.0);

        new GrowthTask(animalManager, speciesManager, breedManager, growthInterval)
                .runTaskTimer(this, growthInterval, growthInterval);
        new WelfareTask(animalManager, welfareInterval).runTaskTimer(this, welfareInterval, welfareInterval);
        new HealthTask(animalManager, diseaseManager, healthInterval, contagionRadius)
                .runTaskTimer(this, healthInterval, healthInterval);
        new PregnancyTask(animalManager, breedingEngine, pregnancyInterval)
                .runTaskTimer(this, pregnancyInterval, pregnancyInterval);
    }

    private GeneticsMode parseGeneticsMode(String raw) {

        try {
            return GeneticsMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return GeneticsMode.ADVANCED;
        }
    }

    public SpeciesManager getSpeciesManager() {
        return speciesManager;
    }

    public BreedManager getBreedManager() {
        return breedManager;
    }

    public GeneManager getGeneManager() {
        return geneManager;
    }

    public FeedManager getFeedManager() {
        return feedManager;
    }

    public DiseaseManager getDiseaseManager() {
        return diseaseManager;
    }

    public VaccineManager getVaccineManager() {
        return vaccineManager;
    }

    public MedicineManager getMedicineManager() {
        return medicineManager;
    }

    public AnimalManager getAnimalManager() {
        return animalManager;
    }

    /**
     * El pack de Java de los modelos de fábrica viaja dentro del jar ({@code resourcepack/}) y se
     * registra en SackResourcePack si está. Sin él, el dueño lo sirve por su cuenta.
     */
    private void registerPack() {

        if (!getServer().getPluginManager().isPluginEnabled("SackResourcePack")
                || !getConfig().getBoolean("resource-pack.register-in-sackresourcepack", true)) {
            return;
        }
        try {
            if (com.sack.rpgroll.sackresourcepack.api.AssetsAPI.isReady()
                    && com.sack.rpgroll.sackresourcepack.api.AssetsAPI.assets().registerPlugin(this)) {
                getLogger().info("✔ Modelos registrados en SackResourcePack (aplica con /srp rebuild).");
            }
        } catch (LinkageError e) {
            getLogger().warning("✘ No se pudo registrar el pack en SackResourcePack: " + e.getMessage());
        }
    }
}
