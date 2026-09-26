package com.sack.rpgroll.economy.command;

import com.sack.rpgroll.common.command.Senders;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.economy.gui.ServerShopCategoryGUI;
import com.sack.rpgroll.economy.gui.ServerShopGUI;
import com.sack.rpgroll.economy.servershop.ServerShopCategory;
import com.sack.rpgroll.economy.servershop.ServerShopManager;
import com.sack.rpgroll.economy.servershop.ServerShopService;
import com.sack.rpgroll.economy.wallet.WalletService;
import com.sack.rpgroll.util.TabCompleteUtil;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

/** {@code /tienda [sección]}: abre la tienda del servidor, o directamente una sección. */
public class ServerShopCommand implements CommandExecutor, TabCompleter {

    private final ServerShopManager shops;
    private final ServerShopService service;
    private final WalletService wallet;
    private final LangManager lang;

    public ServerShopCommand(ServerShopManager shops, ServerShopService service, WalletService wallet,
            LangManager lang) {
        this.shops = shops;
        this.service = service;
        this.wallet = wallet;
        this.lang = lang;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(Senders.asPlayer(sender) instanceof Player player)) {
            lang.send(sender, "server_shop.players_only");
            return true;
        }

        Runnable home = () -> new ServerShopGUI(player, shops, service, wallet, lang).open();

        if (args.length == 0) {
            home.run();
            return true;
        }

        Optional<ServerShopCategory> category = shops.get(args[0]);

        if (category.isEmpty()) {
            lang.send(player, "server_shop.unknown_category", "id", args[0]);
            return true;
        }

        if (!service.canEnter(player, category.get())) {
            lang.send(player, "server_shop.no_permission");
            return true;
        }

        new ServerShopCategoryGUI(player, category.get(), service, wallet, lang, home).open();
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length != 1) {
            return List.of();
        }

        return TabCompleteUtil.filter(args[0], shops.getAll().stream()
                .filter(category -> !(sender instanceof Player player) || service.canEnter(player, category))
                .map(ServerShopCategory::id)
                .toList());
    }

}
