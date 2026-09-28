package com.sack.rpgroll.ranching.command;

import com.sack.rpgroll.common.command.Senders;

import com.sack.rpgroll.ranching.core.animal.AnimalManager;
import com.sack.rpgroll.ranching.core.breeds.Breed;
import com.sack.rpgroll.ranching.core.breeds.BreedManager;
import com.sack.rpgroll.ranching.core.genetics.GeneManager;
import com.sack.rpgroll.ranching.core.genetics.GeneticsEngine;
import com.sack.rpgroll.ranching.core.genetics.PedigreeService;
import com.sack.rpgroll.ranching.core.breeding.BreedingEngine;
import com.sack.rpgroll.ranching.core.production.ProductQuality;
import com.sack.rpgroll.ranching.core.health.DiseaseManager;
import com.sack.rpgroll.ranching.core.health.MedicineManager;
import com.sack.rpgroll.ranching.core.health.VaccineManager;
import com.sack.rpgroll.ranching.core.nutrition.FeedManager;
import com.sack.rpgroll.ranching.core.species.Sex;
import com.sack.rpgroll.ranching.core.species.Species;
import com.sack.rpgroll.ranching.core.species.SpeciesManager;
import com.sack.rpgroll.ranching.gui.ChatPromptManager;
import com.sack.rpgroll.ranching.gui.RanchHubGUI;
import com.sack.rpgroll.ranching.item.RanchingItemFactory;
import com.sack.rpgroll.util.ComponentUtils;
import com.sack.rpgroll.util.TabCompleteUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Stream;

/**
 * /ranchingadmin browser|reload|spawn <especie> [<raza>]
 * /ranchingadmin givefeed|givemedicine|givevaccine <id> [cantidad] [jugador]
 * /ranchingadmin giveproduct <tipo> [calidad] [cantidad] [jugador]
 */
public class RanchingAdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("browser", "reload", "spawn", "givefeed", "givemedicine", "givevaccine",
            "giveproduct", "setowner", "givedinoegg");
    private static final List<String> GIVE_CONTENT = List.of("givefeed", "givemedicine", "givevaccine");
    private static final List<String> QUALITIES = Arrays.stream(ProductQuality.values()).map(Enum::name).toList();

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
            sender.sendMessage(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("command.admin.no_permission"), NamedTextColor.RED));
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
            case "givefeed", "givemedicine", "givevaccine" -> handleGiveContent(sender, args);
            case "giveproduct" -> handleGiveProduct(sender, args);
            case "setowner" -> handleSetOwner(sender, args);
            case "givedinoegg" -> give(sender, com.sack.rpgroll.ranching.listener.DinoEggListener.createEgg(lang()),
                    args.length >= 2 ? new String[] {"givedinoegg", "dino_egg", "1", args[1]} : new String[] {"givedinoegg", "dino_egg", "1"}, 2);
            default -> sendUsage(sender);
        }

        return true;
    }

    private void handleBrowser(CommandSender sender) {

        if (!(Senders.asPlayer(sender) instanceof Player player)) {
            sender.sendMessage(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("command.admin.player_only_browser"), NamedTextColor.RED));
            return;
        }

        new RanchHubGUI(player, speciesManager, breedManager, geneManager, feedManager, diseaseManager, vaccineManager,
                medicineManager, animalManager, geneticsEngine, pedigreeService, breedingEngine, chatPromptManager,
                inbreedingGenerations).open();
    }

    private void handleReload(CommandSender sender) {
        onReload.run();
        sender.sendMessage(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("command.admin.reloaded"), NamedTextColor.GREEN));
    }

    private void handleSpawn(CommandSender sender, String[] args) {

        if (!(Senders.asPlayer(sender) instanceof Player player)) {
            sender.sendMessage(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("command.admin.player_only_spawn"), NamedTextColor.RED));
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("command.admin.usage_spawn"), NamedTextColor.RED));
            return;
        }

        Species species = speciesManager.get(args[1].toLowerCase()).orElse(null);

        if (species == null) {
            player.sendMessage(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("command.admin.unknown_species", "id", args[1]), NamedTextColor.RED));
            return;
        }

        Breed breed = args.length >= 3 ? breedManager.get(args[2].toLowerCase()).orElse(null) : null;

        // Dueño: el 4.º argumento ("-" o "ninguno" = sin dueño); si no se da, quien lo spawnea.
        java.util.UUID owner = player.getUniqueId();
        if (args.length >= 4) {
            owner = parseOwner(args[3]);
            if (owner == null && !isNobody(args[3])) {
                send(sender, "command.admin.player_not_found", NamedTextColor.RED, "name", args[3]);
                return;
            }
        }

        Location location = player.getLocation();
        var entityType = animalManager.resolveEntityType(species);
        LivingEntity entity = (LivingEntity) location.getWorld().spawnEntity(location, entityType);

        // Sexo: el 5.º argumento (macho/hembra); si no se da o es "-", al azar.
        Sex sex = args.length >= 5 ? parseSex(args[4]) : null;
        if (sex == null) {
            sex = random.nextBoolean() ? Sex.MALE : Sex.FEMALE;
        }
        animalManager.registerFounder(entity, species, breed, sex, geneticsEngine,
                geneManager.getForSpecies(species.id()), owner);

        player.sendMessage(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("command.admin.spawned_prefix"), NamedTextColor.GREEN)
                .append(ComponentUtils.parse(species.displayName()))
                .append(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("command.admin.spawned_suffix", "sex", sex), NamedTextColor.GREEN)));
    }

    private static Sex parseSex(String raw) {
        return switch (raw.toLowerCase(Locale.ROOT)) {
            case "macho", "male", "m" -> Sex.MALE;
            case "hembra", "female", "f", "femea", "fêmea" -> Sex.FEMALE;
            default -> null;
        };
    }

    private static boolean isNobody(String raw) {
        return raw.equals("-") || raw.equalsIgnoreCase("ninguno") || raw.equalsIgnoreCase("none");
    }

    /** Un jugador (conectado o que haya entrado alguna vez) por nombre; null si no existe o es "nadie". */
    private static java.util.UUID parseOwner(String raw) {

        if (isNobody(raw)) {
            return null;
        }

        Player online = Bukkit.getPlayerExact(raw);
        if (online != null) {
            return online.getUniqueId();
        }

        var offline = Bukkit.getOfflinePlayerIfCached(raw);
        return offline != null ? offline.getUniqueId() : null;
    }

    /** /ranchingadmin setowner <jugador|ninguno> — cambia el dueño del animal al que se mira. */
    private void handleSetOwner(CommandSender sender, String[] args) {

        if (!(Senders.asPlayer(sender) instanceof Player player)) {
            send(sender, "command.admin.player_only_spawn", NamedTextColor.RED);
            return;
        }

        if (args.length < 2) {
            send(sender, "command.admin.usage_setowner", NamedTextColor.RED);
            return;
        }

        var target = player.getTargetEntity(6);
        var animal = target == null ? null : animalManager.resolve(target).orElse(null);

        if (animal == null) {
            send(sender, "command.ranching.look_at_animal", NamedTextColor.RED);
            return;
        }

        java.util.UUID owner = parseOwner(args[1]);

        if (owner == null && !isNobody(args[1])) {
            send(sender, "command.admin.player_not_found", NamedTextColor.RED, "name", args[1]);
            return;
        }

        animal.setOwnerId(owner);
        animal.setSalePrice(0);
        animalManager.save(animal);
        send(sender, "command.admin.owner_set", NamedTextColor.GREEN, "owner", owner == null ? "-" : args[1]);
    }

    private void handleGiveContent(CommandSender sender, String[] args) {

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (args.length < 2) {
            send(sender, "command.admin.usage_give", NamedTextColor.RED, "sub", sub);
            return;
        }

        String id = args[1].toLowerCase(Locale.ROOT);
        ItemStack item = switch (sub) {
            case "givefeed" -> feedManager.get(id).map(f -> RanchingItemFactory.createFeed(lang(), f)).orElse(null);
            case "givemedicine" -> medicineManager.get(id).map(m -> RanchingItemFactory.createMedicine(lang(), m)).orElse(null);
            default -> vaccineManager.get(id).map(v -> RanchingItemFactory.createVaccine(lang(), v)).orElse(null);
        };

        if (item == null) {
            send(sender, "command.admin.unknown_item", NamedTextColor.RED, "id", args[1]);
            return;
        }

        give(sender, item, args, 2);
    }

    private void handleGiveProduct(CommandSender sender, String[] args) {

        if (args.length < 2) {
            send(sender, "command.admin.usage_giveproduct", NamedTextColor.RED);
            return;
        }

        String type = args[1].toLowerCase(Locale.ROOT);

        if (RanchingItemFactory.productMaterial(type) == null) {
            send(sender, "command.admin.unknown_product", NamedTextColor.RED, "id", args[1],
                    "types", String.join(", ", productTypes()));
            return;
        }

        ProductQuality quality = ProductQuality.COMMON;

        if (args.length >= 3) {
            try {
                quality = ProductQuality.valueOf(args[2].toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                send(sender, "command.admin.unknown_quality", NamedTextColor.RED, "value", args[2],
                        "values", String.join(", ", QUALITIES));
                return;
            }
        }

        give(sender, RanchingItemFactory.createProduct(lang(), type, quality, 1), args, 3);
    }

    /** Reparte el ítem: {@code args[from]} es la cantidad y {@code args[from + 1]} el jugador (por defecto, quien lo pide). */
    private void give(CommandSender sender, ItemStack item, String[] args, int from) {

        int amount = 1;

        if (args.length > from) {
            try {
                amount = Integer.parseInt(args[from]);
            } catch (NumberFormatException e) {
                amount = 0;
            }
            if (amount < 1 || amount > 64 * 36) {
                send(sender, "command.admin.bad_amount", NamedTextColor.RED, "value", args[from]);
                return;
            }
        }

        Player target;

        if (args.length > from + 1) {
            target = Bukkit.getPlayerExact(args[from + 1]);
            if (target == null) {
                send(sender, "command.admin.player_not_found", NamedTextColor.RED, "name", args[from + 1]);
                return;
            }
        } else if (Senders.asPlayer(sender) instanceof Player player) {
            target = player;
        } else {
            send(sender, "command.admin.give_needs_player", NamedTextColor.RED);
            return;
        }

        int left = amount;
        while (left > 0) {
            ItemStack stack = item.clone();
            stack.setAmount(Math.min(left, item.getMaxStackSize()));
            left -= stack.getAmount();
            // Lo que no cabe cae al suelo, a sus pies.
            target.getInventory().addItem(stack).values()
                    .forEach(rest -> target.getWorld().dropItemNaturally(target.getLocation(), rest));
        }

        send(sender, "command.admin.given", NamedTextColor.GREEN, "amount", amount,
                "item", args[1].toLowerCase(Locale.ROOT), "player", target.getName());
    }

    /** Tipos de producto de las especies cargadas que tienen ítem propio. */
    private List<String> productTypes() {
        return speciesManager.getAll().stream()
                .flatMap(s -> s.productTypes().stream())
                .map(t -> t.toLowerCase(Locale.ROOT))
                .filter(t -> RanchingItemFactory.productMaterial(t) != null)
                .distinct().sorted().toList();
    }

    private com.sack.rpgroll.common.lang.LangManager lang() {
        return chatPromptManager.lang();
    }

    private void send(CommandSender sender, String key, NamedTextColor color, Object... placeholders) {
        sender.sendMessage(ComponentUtils.parseWithDefault(lang().raw(key, placeholders), color));
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(ComponentUtils.parseWithDefault(chatPromptManager.lang().raw("command.admin.usage"), NamedTextColor.RED));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        if (args.length == 2 && GIVE_CONTENT.contains(sub)) {
            Stream<String> ids = switch (sub) {
                case "givefeed" -> feedManager.getAll().stream().map(f -> f.id());
                case "givemedicine" -> medicineManager.getAll().stream().map(m -> m.id());
                default -> vaccineManager.getAll().stream().map(v -> v.id());
            };
            return TabCompleteUtil.filter(args[1], ids.toList());
        }

        if (sub.equals("giveproduct")) {
            if (args.length == 2) {
                return TabCompleteUtil.filter(args[1], productTypes());
            }
            if (args.length == 3) {
                return TabCompleteUtil.filter(args[2], QUALITIES);
            }
        }

        boolean playerArg = (GIVE_CONTENT.contains(sub) && args.length == 4) || (sub.equals("giveproduct") && args.length == 5);
        if (playerArg) {
            return TabCompleteUtil.filter(args[args.length - 1],
                    Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        }

        if (args.length == 2 && "spawn".equalsIgnoreCase(args[0])) {
            return TabCompleteUtil.filter(args[1], speciesManager.getAll().stream()
                    .map(Species::id).toList());
        }

        if (args.length == 5 && "spawn".equalsIgnoreCase(args[0])) {
            return TabCompleteUtil.filter(args[4], List.of("macho", "hembra", "-"));
        }

        if (args.length == 3 && "spawn".equalsIgnoreCase(args[0])) {
            return TabCompleteUtil.filter(args[2], breedManager.getForSpecies(args[1].toLowerCase()).stream()
                    .map(Breed::id).toList());
        }

        return List.of();
    }

}
