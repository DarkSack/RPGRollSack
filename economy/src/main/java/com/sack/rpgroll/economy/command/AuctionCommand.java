package com.sack.rpgroll.economy.command;

import com.sack.rpgroll.common.command.Senders;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.economy.auction.AuctionPrices;
import com.sack.rpgroll.economy.auction.AuctionService;
import com.sack.rpgroll.economy.gui.AuctionCollectGUI;
import com.sack.rpgroll.economy.gui.AuctionHouseGUI;
import com.sack.rpgroll.economy.gui.AuctionMineGUI;
import com.sack.rpgroll.economy.gui.ChatPromptManager;
import com.sack.rpgroll.util.TabCompleteUtil;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * {@code /subasta} (alias de config): abre la Casa de Subastas.
 * <ul>
 *   <li>{@code vender <precio>} — publica lo de la mano a precio fijo</li>
 *   <li>{@code subastar <inicial> [compra ya]} — lo publica con pujas</li>
 *   <li>{@code buscar <texto>} — abre el buscador filtrado</li>
 *   <li>{@code mis} — mis publicaciones · {@code recoger} — la caja de recogida</li>
 * </ul>
 * Cada subcomando acepta también su nombre en inglés.
 */
public class AuctionCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("vender", "subastar", "buscar", "mis", "recoger");

    private final AuctionService service;
    private final ChatPromptManager prompts;
    private final LangManager lang;

    public AuctionCommand(AuctionService service, ChatPromptManager prompts) {
        this.service = service;
        this.prompts = prompts;
        this.lang = service.lang();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(Senders.asPlayer(sender) instanceof Player player)) {
            lang.send(sender, "common.players_only");
            return true;
        }

        if (args.length == 0) {
            new AuctionHouseGUI(player, service, prompts).open();
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "vender", "sell" -> {
                if (args.length < 2) {
                    lang.send(player, "auction_house.usage");
                    return true;
                }
                service.sellHand(player, false, parse(args[1]), 0);
            }
            case "subastar", "auction", "bid" -> {
                if (args.length < 2) {
                    lang.send(player, "auction_house.usage");
                    return true;
                }
                double buyNow = args.length > 2 ? parse(args[2]) : 0;
                service.sellHand(player, true, parse(args[1]), Double.isNaN(buyNow) ? 0 : buyNow);
            }
            case "buscar", "search" -> new AuctionHouseGUI(player, service, prompts,
                    AuctionHouseGUI.View.search(String.join(" ", Arrays.copyOfRange(args, 1, args.length)))).open();
            case "mis", "mine", "listings" -> new AuctionMineGUI(player, service, prompts, new AuctionHouseGUI.View()).open();
            case "recoger", "collect" -> new AuctionCollectGUI(player, service, prompts, new AuctionHouseGUI.View()).open();
            default -> lang.send(player, "auction_house.usage");
        }

        return true;
    }

    private static double parse(String text) {
        return AuctionPrices.parse(text);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return TabCompleteUtil.filter(args[0], SUBCOMMANDS);
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && List.of("vender", "sell", "subastar", "auction").contains(sub)) {
            return List.of("<precio>");
        }
        if (args.length == 3 && List.of("subastar", "auction").contains(sub)) {
            return List.of("[compra-ya]");
        }
        return List.of();
    }

}
