package com.sack.rpgroll.ranching.command;

import com.sack.rpgroll.common.command.Senders;
import com.sack.rpgroll.common.lang.LangManager;

import com.sack.rpgroll.ranching.core.animal.Animal;
import com.sack.rpgroll.ranching.core.animal.AnimalManager;
import com.sack.rpgroll.ranching.core.breeds.BreedManager;
import com.sack.rpgroll.ranching.core.ownership.AnimalMarket;
import com.sack.rpgroll.ranching.core.ownership.OwnershipService;
import com.sack.rpgroll.ranching.core.species.SpeciesManager;
import com.sack.rpgroll.ranching.gui.AnimalDetailGUI;
import com.sack.rpgroll.ranching.gui.AnimalMarketGUI;
import com.sack.rpgroll.ranching.gui.ChatPromptManager;
import com.sack.rpgroll.ranching.gui.ConfirmGUI;
import com.sack.rpgroll.ranching.gui.MyAnimalsGUI;
import com.sack.rpgroll.util.TabCompleteUtil;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.sack.rpgroll.gui.util.ItemBuilder;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * /ranching — lo del jugador con sus animales:
 * inspect · animales · mercado · reclamar · vender <precio>|cancelar|servidor · comprar <id> ·
 * llamar [id|todos] · corral [fijar|quitar|enviar [id|todos]]
 */
public class RanchingCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("inspect", "animales", "mercado", "reclamar", "vender",
            "comprar", "llamar", "corral");

    private final AnimalManager animalManager;
    private final SpeciesManager speciesManager;
    private final BreedManager breedManager;
    private final ChatPromptManager chatPromptManager;
    private final OwnershipService ownership;
    private final LangManager lang;

    public RanchingCommand(AnimalManager animalManager, SpeciesManager speciesManager, BreedManager breedManager,
            ChatPromptManager chatPromptManager, OwnershipService ownership) {
        this.animalManager = animalManager;
        this.speciesManager = speciesManager;
        this.breedManager = breedManager;
        this.chatPromptManager = chatPromptManager;
        this.ownership = ownership;
        this.lang = chatPromptManager.lang();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(Senders.asPlayer(sender) instanceof Player player)) {
            sender.sendMessage(lang.raw("command.player_only"));
            return true;
        }

        if (args.length < 1) {
            lang.send(player, "command.ranching.usage");
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "inspect", "inspeccionar" -> targeted(player).ifPresent(animal -> new AnimalDetailGUI(player, animal,
                    speciesManager, breedManager, chatPromptManager, player::closeInventory).open());
            case "animales", "animals" -> new MyAnimalsGUI(player, ownership, chatPromptManager).open();
            case "mercado", "market" -> {
                if (ownership.settings().marketEnabled()) {
                    new AnimalMarketGUI(player, ownership, chatPromptManager).open();
                } else {
                    lang.send(player, "market.result.disabled");
                }
            }
            case "reclamar", "claim" -> targeted(player).ifPresent(animal -> ownership.claim(player, animal));
            case "vender", "sell" -> handleSell(player, args);
            case "comprar", "buy" -> handleBuy(player, args);
            case "llamar", "call" -> handleCall(player, args);
            case "corral", "pen" -> handlePen(player, args);
            default -> lang.send(player, "command.ranching.usage");
        }

        return true;
    }

    /** El animal rastreado al que mira el jugador (con el aviso si no mira a ninguno). */
    private Optional<Animal> targeted(Player player) {

        Entity target = player.getTargetEntity(6);
        Optional<Animal> animal = target != null ? animalManager.resolve(target) : Optional.empty();

        if (animal.isEmpty()) {
            lang.send(player, "command.ranching.look_at_animal");
        }

        return animal;
    }

    /** Un animal propio por su id corto, o todos con "todos"; sin argumento, el que se mira. */
    private Optional<Animal> ownAnimal(Player player, String shortId) {

        Optional<Animal> animal = shortId == null ? targeted(player) : animalManager.byShortId(shortId);

        if (animal.isEmpty()) {
            if (shortId != null) {
                lang.send(player, "owner.unknown_id", "id", shortId);
            }
            return Optional.empty();
        }

        if (!animal.get().isOwnedBy(player.getUniqueId()) && !player.hasPermission(OwnershipService.BYPASS_PERMISSION)) {
            lang.send(player, "owner.not_yours", "owner", ownership.ownerName(animal.get()));
            return Optional.empty();
        }

        return animal;
    }

    private void handleSell(Player player, String[] args) {

        if (args.length < 2) {
            lang.send(player, "command.ranching.usage_sell");
            return;
        }

        String what = args[1].toLowerCase(Locale.ROOT);

        if (what.equals("servidor") || what.equals("server")) {
            ownAnimal(player, args.length >= 3 ? args[2] : null).ifPresent(animal -> {
                double price = ownership.market().serverPrice(animal);
                if (price <= 0) {
                    lang.send(player, "market.result.not_sellable");
                    return;
                }
                new ConfirmGUI(player, lang, new ItemBuilder(Material.GOLD_INGOT)
                        .setName(lang.component("gui.my_animals.name", "animal", ownership.describe(animal)))
                        .setLore(lang.component("gui.market.server_pays", "price", AnimalMarket.format(price))).build(),
                        () -> ownership.sellToServer(player, animal), null).open();
            });
            return;
        }

        if (what.equals("cancelar") || what.equals("cancel")) {
            ownAnimal(player, args.length >= 3 ? args[2] : null).ifPresent(animal -> ownership.list(player, animal, 0));
            return;
        }

        double price;
        try {
            price = Double.parseDouble(args[1].replace(",", "."));
        } catch (NumberFormatException e) {
            lang.send(player, "command.ranching.usage_sell");
            return;
        }

        ownAnimal(player, args.length >= 3 ? args[2] : null).ifPresent(animal -> ownership.list(player, animal, price));
    }

    private void handleBuy(Player player, String[] args) {

        if (args.length < 2) {
            lang.send(player, "command.ranching.usage_buy");
            return;
        }

        Animal animal = animalManager.byShortId(args[1]).orElse(null);

        if (animal == null || !animal.isForSale()) {
            lang.send(player, "market.result.not_for_sale");
            return;
        }

        new ConfirmGUI(player, lang, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("gui.my_animals.name", "animal", ownership.describe(animal)))
                .setLore(lang.component("gui.market.seller", "owner", ownership.ownerName(animal)),
                        lang.component("gui.market.price", "price", AnimalMarket.format(animal.salePrice())))
                .build(), () -> ownership.buy(player, animal), null).open();
    }

    private void handleCall(Player player, String[] args) {

        if (args.length < 2) {
            new MyAnimalsGUI(player, ownership, chatPromptManager).open();
            return;
        }

        if (isAll(args[1])) {
            ownership.callAll(player);
        } else {
            ownAnimal(player, args[1]).ifPresent(animal -> ownership.call(player, animal));
        }
    }

    private void handlePen(Player player, String[] args) {

        String sub = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "";

        switch (sub) {
            case "fijar", "set" -> {
                ownership.pens().set(player.getUniqueId(), player.getLocation());
                lang.send(player, "owner.pen_set");
            }
            case "quitar", "remove" -> lang.send(player, ownership.pens().remove(player.getUniqueId())
                    ? "owner.pen_removed" : "owner.no_pen");
            case "enviar", "send" -> {
                if (args.length < 3 || isAll(args[2])) {
                    ownership.sendAllToPen(player);
                } else {
                    ownAnimal(player, args[2]).ifPresent(animal -> ownership.sendToPen(player, animal));
                }
            }
            default -> {
                var pen = ownership.pens().get(player.getUniqueId());
                if (pen.isPresent()) {
                    var l = pen.get();
                    lang.send(player, "owner.pen_info", "pen", l.getWorld().getName() + " " + l.getBlockX() + ", "
                            + l.getBlockY() + ", " + l.getBlockZ());
                } else {
                    lang.send(player, "owner.no_pen");
                }
                lang.send(player, "command.ranching.usage_pen");
            }
        }
    }

    private static boolean isAll(String raw) {
        return raw.equalsIgnoreCase("todos") || raw.equalsIgnoreCase("all");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        Player player = Senders.asPlayer(sender) instanceof Player p ? p : null;
        List<String> ownIds = player == null ? List.of()
                : animalManager.ownedBy(player.getUniqueId()).stream().map(OwnershipService::shortId).toList();

        if (args.length == 2) {
            return switch (sub) {
                case "vender", "sell" -> TabCompleteUtil.filter(args[1], List.of("cancelar", "servidor", "100"));
                case "comprar", "buy" -> TabCompleteUtil.filter(args[1],
                        animalManager.forSale().stream().map(OwnershipService::shortId).toList());
                case "llamar", "call" -> TabCompleteUtil.filter(args[1],
                        java.util.stream.Stream.concat(java.util.stream.Stream.of("todos"), ownIds.stream()).toList());
                case "corral", "pen" -> TabCompleteUtil.filter(args[1], List.of("fijar", "quitar", "enviar"));
                default -> List.of();
            };
        }

        if (args.length == 3 && (sub.equals("vender") || sub.equals("sell"))) {
            return TabCompleteUtil.filter(args[2], ownIds);
        }

        if (args.length == 3 && (sub.equals("corral") || sub.equals("pen"))
                && (args[1].equalsIgnoreCase("enviar") || args[1].equalsIgnoreCase("send"))) {
            return TabCompleteUtil.filter(args[2],
                    java.util.stream.Stream.concat(java.util.stream.Stream.of("todos"), ownIds.stream()).toList());
        }

        return List.of();
    }

}
