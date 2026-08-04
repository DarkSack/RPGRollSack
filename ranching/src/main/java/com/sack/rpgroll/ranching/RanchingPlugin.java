package com.sack.rpgroll.ranching;

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

    private SpeciesManager speciesManager;
    private BreedManager breedManager;
    private GeneManager geneManager;
    private FeedManager feedManager;
    private DiseaseManager diseaseManager;
    private VaccineManager vaccineManager;
    private MedicineManager medicineManager;
    private AnimalManager animalManager;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        new DirectoryCreator(this).create(DIRECTORIES);
        new ResourceCopier(this).copyDirectories(DIRECTORIES);

        initializeManagers();

        GeneticsMode geneticsMode = parseGeneticsMode(getConfig().getString("genetics-mode", "ADVANCED"));
        double mutationChance = getConfig().getDouble("mutation-chance", 0.01);
        int inbreedingGenerations = getConfig().getInt("inbreeding-warning-generations", 3);

        GeneticsEngine geneticsEngine = new GeneticsEngine(geneticsMode, mutationChance);
        PedigreeService pedigreeService = new PedigreeService();

        animalManager = new AnimalManager(this);
        animalManager.loadAll();

        BreedingEngine breedingEngine = new BreedingEngine(speciesManager, breedManager, geneManager, geneticsEngine,
                pedigreeService, animalManager, inbreedingGenerations);

        RanchingAPI.init(speciesManager, breedManager, geneManager, feedManager, diseaseManager, vaccineManager,
                medicineManager, animalManager, geneticsEngine, pedigreeService, breedingEngine);

        ChatPromptManager chatPromptManager = new ChatPromptManager(this);
        getServer().getPluginManager().registerEvents(chatPromptManager, this);

        getServer().getPluginManager().registerEvents(new AnimalCareListener(animalManager, speciesManager,
                feedManager, medicineManager, vaccineManager, diseaseManager), this);
        getServer().getPluginManager().registerEvents(new BreedingListener(animalManager, breedingEngine), this);
        getServer().getPluginManager().registerEvents(
                new ProductionListener(animalManager, speciesManager, breedManager, geneManager, diseaseManager),
                this);

        startTasks(animalManager, breedingEngine, inbreedingGenerations);

        var adminCommand = getCommand("ranchingadmin");
        if (adminCommand == null) {
            getLogger().severe("✘ El comando 'ranchingadmin' no está declarado en plugin.yml");
        } else {
            adminCommand.setExecutor(new RanchingAdminCommand(speciesManager, breedManager, geneManager, feedManager,
                    diseaseManager, vaccineManager, medicineManager, animalManager, geneticsEngine, pedigreeService,
                    breedingEngine, chatPromptManager, inbreedingGenerations, this::reloadContent));
        }

        var playerCommand = getCommand("ranching");
        if (playerCommand == null) {
            getLogger().severe("✘ El comando 'ranching' no está declarado en plugin.yml");
        } else {
            playerCommand.setExecutor(new RanchingCommand(animalManager, speciesManager, breedManager));
        }

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

}
