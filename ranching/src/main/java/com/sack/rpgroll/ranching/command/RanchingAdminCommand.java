package com.sack.rpgroll.ranching.command;

import com.sack.rpgroll.ranching.core.animal.AnimalManager;
import com.sack.rpgroll.ranching.core.breeds.Breed;
import com.sack.rpgroll.ranching.core.breeds.BreedManager;
import com.sack.rpgroll.ranching.core.genetics.GeneManager;
import com.sack.rpgroll.ranching.core.genetics.GeneticsEngine;
import com.sack.rpgroll.ranching.core.genetics.PedigreeService;
import com.sack.rpgroll.ranching.core.breeding.BreedingEngine;
import com.sack.rpgroll.ranching.core.health.DiseaseManager;
import com.sack.rpgroll.ranching.core.health.MedicineManager;
import com.sack.rpgroll.ranching.core.health.VaccineManager;
import com.sack.rpgroll.ranching.core.nutrition.FeedManager;
import com.sack.rpgroll.ranching.core.species.Sex;
import com.sack.rpgroll.ranching.core.species.Species;
import com.sack.rpgroll.ranching.core.species.SpeciesManager;
import com.sack.rpgroll.ranching.gui.ChatPromptManager;
import com.sack.rpgroll.ranching.gui.RanchHubGUI;
import com.sack.rpgroll.util.TabCompleteUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Random;

/** /ranchingadmin browser|reload|spawn <especie> [<raza>] */
public class RanchingAdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("browser", "reload", "spawn");

    private final SpeciesManager speciesManager;
    private final BreedManager breedManager;
    private final GeneManager geneManager;
    private final FeedManager feedManager;
    private final DiseaseManager diseaseManager;
    private final VaccineManager vaccineManager;
    private final MedicineManager medicineManager;
    private final AnimalManager animalManager;
    private final GeneticsEngine geneticsEngine;
    private final PedigreeService pedigreeService;
    private final BreedingEngine breedingEngine;
    private final ChatPromptManager chatPromptManager;
    private final int inbreedingGenerations;
    private final Runnable onReload;
    private final Random random = new Random();

    public RanchingAdminCommand(SpeciesManager speciesManager, BreedManager breedManager, GeneManager geneManager,
            FeedManager feedManager, DiseaseManager diseaseManager, VaccineManager vaccineManager,
            MedicineManager medicineManager, AnimalManager animalManager, GeneticsEngine geneticsEngine,
            PedigreeService pedigreeService, BreedingEngine breedingEngine, ChatPromptManager chatPromptManager,
            int inbreedingGenerations, Runnable onReload) {
        this.speciesManager = speciesManager;
        this.breedManager = breedManager;
        this.geneManager = geneManager;
        this.feedManager = feedManager;
        this.diseaseManager = diseaseManager;
        this.vaccineManager = vaccineManager;
        this.medicineManager = medicineManager;
        this.animalManager = animalManager;
        this.geneticsEngine = geneticsEngine;
        this.pedigreeService = pedigreeService;
        this.breedingEngine = breedingEngine;
        this.chatPromptManager = chatPromptManager;
        this.inbreedingGenerations = inbreedingGenerations;
        this.onReload = onReload;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("rpgrollranching.admin.*")) {
            sender.sendMessage(Component.text("No tenés permiso para usar este comando.", NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "browser" -> handleBrowser(sender);
            case "reload" -> handleReload(sender);
            case "spawn" -> handleSpawn(sender, args);
            default -> sendUsage(sender);
        }

        return true;
    }

    private void handleBrowser(CommandSender sender) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo un jugador puede abrir el Ranch Studio.", NamedTextColor.RED));
            return;
        }

        new RanchHubGUI(player, speciesManager, breedManager, geneManager, feedManager, diseaseManager, vaccineManager,
                medicineManager, animalManager, geneticsEngine, pedigreeService, breedingEngine, chatPromptManager,
                inbreedingGenerations).open();
    }

    private void handleReload(CommandSender sender) {
        onReload.run();
        sender.sendMessage(Component.text("✔ RPGRoll-Ranching recargado.", NamedTextColor.GREEN));
    }

    private void handleSpawn(CommandSender sender, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Solo un jugador puede spawnear un animal.", NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(Component.text("Uso: /ranchingadmin spawn <especie> [raza]", NamedTextColor.RED));
            return;
        }

        Species species = speciesManager.get(args[1].toLowerCase()).orElse(null);

        if (species == null) {
            player.sendMessage(Component.text("No existe la especie '" + args[1] + "'.", NamedTextColor.RED));
            return;
        }

        Breed breed = args.length >= 3 ? breedManager.get(args[2].toLowerCase()).orElse(null) : null;

        Location location = player.getLocation();
        var entityType = animalManager.resolveEntityType(species);
        LivingEntity entity = (LivingEntity) location.getWorld().spawnEntity(location, entityType);

        Sex sex = random.nextBoolean() ? Sex.MALE : Sex.FEMALE;
        animalManager.registerFounder(entity, species, breed, sex, geneticsEngine,
                geneManager.getForSpecies(species.id()));

        player.sendMessage(Component.text("✔ Spawneado un/a " + species.displayName() + " (" + sex + ").",
                NamedTextColor.GREEN));
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("Uso: /ranchingadmin <browser|reload|spawn <especie> [raza]>",
                NamedTextColor.RED));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
        }

        if (args.length == 2 && "spawn".equalsIgnoreCase(args[0])) {
            return TabCompleteUtil.filter(args[1], speciesManager.getAll().stream()
                    .map(Species::id).toList());
        }

        if (args.length == 3 && "spawn".equalsIgnoreCase(args[0])) {
            return TabCompleteUtil.filter(args[2], breedManager.getForSpecies(args[1].toLowerCase()).stream()
                    .map(Breed::id).toList());
        }

        return List.of();
    }

}
