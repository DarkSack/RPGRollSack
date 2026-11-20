package com.sack.rpgroll.emotes.command;

import com.sack.rpgroll.common.command.Senders;
import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.emotes.catalog.Emote;
import com.sack.rpgroll.emotes.catalog.EmoteCatalog;
import com.sack.rpgroll.emotes.player.EmotePlayer;
import com.sack.rpgroll.emotes.player.EmotePlayerStore;
import com.sack.rpgroll.emotes.service.EmoteService;
import com.sack.rpgroll.emotes.service.LaunchResult;
import com.sack.rpgroll.emotes.service.PlayerState;
import com.sack.rpgroll.util.TabCompleteUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** {@code /emote <id|1-7|stop>} (sin argumento abre el menú) y {@code /emotes} (abre el menú). */
public class EmoteCommand implements CommandExecutor, TabCompleter {

    private final EmoteService service;
    private final EmoteCatalog catalog;
    private final EmotePlayerStore store;
    private final LangManager lang;
    private final Consumer<Player> openMenu;

    public EmoteCommand(EmoteService service, EmoteCatalog catalog, EmotePlayerStore store, LangManager lang,
                        Consumer<Player> openMenu) {
        this.service = service;
        this.catalog = catalog;
        this.store = store;
        this.lang = lang;
        this.openMenu = openMenu;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player p = Senders.asPlayer(sender);
        if (p == null) {
            lang.send(sender, "command.players_only");
            return true;
        }
        if (args.length == 0) {
            openMenu.accept(p);
        } else {
            run(p, args[0]);
        }
        return true;
    }

    /**
     * Ejecutor de {@code /emotes}: abre el menú; {@code /emotes rapido} abre el diálogo de favoritos
     * (pensado para atarlo a una tecla con un macro).
     */
    public CommandExecutor menuExecutor(Consumer<Player> openQuick) {
        return (sender, command, label, args) -> {
            Player p = Senders.asPlayer(sender);
            if (p == null) {
                lang.send(sender, "command.players_only");
            } else if (args.length > 0 && args[0].equalsIgnoreCase("rapido")) {
                openQuick.accept(p);
            } else {
                openMenu.accept(p);
            }
            return true;
        };
    }

    /** Autocompletado de {@code /emotes}: solo {@code rapido}. */
    public TabCompleter menuCompleter() {
        return (sender, command, alias, args) ->
                args.length == 1 ? TabCompleteUtil.filter(args[0], List.of("rapido")) : List.of();
    }

    /** {@code stop} para el actual; si no, lanza y muestra por la barra de acción lo que salió mal. */
    public void run(Player p, String arg) {
        if (arg.equalsIgnoreCase("stop")) {
            service.stop(p);
            return;
        }
        boolean bypass = p.hasPermission("rpgrollemotes.bypass.cooldown");
        LaunchResult result = service.launch(p, arg, PlayerState.of(p), bypass);
        if (result != LaunchResult.OK) {
            p.sendActionBar(lang.component("launch." + result.name().toLowerCase(Locale.ROOT)));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        Player p = Senders.asPlayer(sender);
        return p == null || args.length != 1 ? List.of() : suggest(p, args[0]);
    }

    /** Los emotes visibles que tiene, más {@code stop} y los huecos de favoritos ocupados. */
    List<String> suggest(Player p, String token) {
        EmotePlayer data = store.get(p.getUniqueId());
        List<String> options = new ArrayList<>(List.of("stop"));
        for (int slot = 1; slot <= data.favorites().size(); slot++) {
            if (data.favorite(slot) != null) {
                options.add(String.valueOf(slot));
            }
        }
        // Los visibles que tiene: un oculto libre (los de magia) no se sugiere aunque se tenga.
        for (Emote e : catalog.visibleTo(e -> service.owns(p, data, e))) {
            if (service.owns(p, data, e)) {
                options.add(e.id());
            }
        }
        return TabCompleteUtil.filter(token, options);
    }
}
