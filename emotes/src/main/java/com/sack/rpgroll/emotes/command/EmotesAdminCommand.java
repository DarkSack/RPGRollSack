package com.sack.rpgroll.emotes.command;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.emotes.catalog.Emote;
import com.sack.rpgroll.emotes.catalog.EmoteCatalog;
import com.sack.rpgroll.emotes.player.EmotePlayer;
import com.sack.rpgroll.emotes.player.EmotePlayerStore;
import com.sack.rpgroll.emotes.service.EmoteService;
import com.sack.rpgroll.emotes.service.LaunchResult;
import com.sack.rpgroll.util.TabCompleteUtil;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * {@code /emotesadmin give|take|list|play|reload}. Pase, cajas, misiones y NPC lo corren desde la consola,
 * a veces con el jugador desconectado: give, take y list trabajan por UUID con el almacén.
 */
public class EmotesAdminCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBS = List.of("give", "take", "list", "play", "reload");

    private final EmoteService service;
    private final EmoteCatalog catalog;
    private final EmotePlayerStore store;
    private final LangManager lang;
    private final Runnable reload;
    private final Function<String, Player> online;
    private final Function<String, OfflinePlayer> offline;

    public EmotesAdminCommand(EmoteService service, EmoteCatalog catalog, EmotePlayerStore store, LangManager lang,
                              Runnable reload, Function<String, Player> online,
                              Function<String, OfflinePlayer> offline) {
        this.service = service;
        this.catalog = catalog;
        this.store = store;
        this.lang = lang;
        this.reload = reload;
        this.online = online;
        this.offline = offline;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        int needed = switch (sub) {
            case "give", "take", "play" -> 3;
            case "list" -> 2;
            case "reload" -> 1;
            default -> Integer.MAX_VALUE;
        };
        if (args.length < needed) {
            lang.send(sender, "admin.usage");
            return true;
        }
        switch (sub) {
            case "give" -> give(sender, args[1], args[2]);
            case "take" -> take(sender, args[1], args[2]);
            case "list" -> list(sender, args[1]);
            case "play" -> play(sender, args[1], args[2]);
            default -> reload(sender);
        }
        return true;
    }

    public void give(CommandSender sender, String name, String id) {
        change(sender, name, id, true);
    }

    public void take(CommandSender sender, String name, String id) {
        change(sender, name, id, false);
    }

    private void change(CommandSender sender, String name, String id, boolean give) {
        Emote emote = catalog.get(id).orElse(null);
        if (emote == null) {
            lang.send(sender, "admin.unknown_emote", "id", id);
            return;
        }
        UUID uuid = resolve(name);
        if (uuid == null) {
            lang.send(sender, "admin.player_not_found", "player", name);
            return;
        }
        boolean isOnline = online.apply(name) != null;
        EmotePlayer data = store.get(uuid);
        // YAML ilegible: no se guarda, así que no se dice que se dio o se quitó.
        if (store.isProtected(uuid)) {
            lang.send(sender, "admin.save_failed", "player", name);
            if (!isOnline) {
                store.unload(uuid);
            }
            return;
        }
        boolean changed = give ? data.unlock(emote.id()) : data.lock(emote.id());
        if (isOnline) {
            store.save(uuid);
        } else {
            store.unload(uuid); // desconectado: guarda y lo saca de memoria
        }
        String key = give ? (changed ? "admin.given" : "admin.already") : (changed ? "admin.taken" : "admin.not_had");
        lang.send(sender, key, "player", name, "id", emote.id());
    }

    public void list(CommandSender sender, String name) {
        UUID uuid = resolve(name);
        if (uuid == null) {
            lang.send(sender, "admin.player_not_found", "player", name);
            return;
        }
        Set<String> unlocked = store.get(uuid).unlocked();
        if (unlocked.isEmpty()) {
            lang.send(sender, "admin.list_empty", "player", name);
        } else {
            lang.send(sender, "admin.list", "player", name, "emotes", String.join(", ", unlocked));
        }
        if (online.apply(name) == null) {
            store.unload(uuid);
        }
    }

    public void play(CommandSender sender, String name, String id) {
        Player target = online.apply(name);
        if (target == null) {
            lang.send(sender, "admin.not_online", "player", name);
            return;
        }
        LaunchResult result = service.force(target, id);
        String shown = catalog.get(id).map(Emote::id).orElse(id);
        lang.send(sender, result == LaunchResult.OK ? "admin.played" : "admin.play_failed",
                "player", name, "id", shown);
    }

    public void reload(CommandSender sender) {
        reload.run();
        lang.send(sender, "admin.reloaded");
    }

    /** Conectado, o desconectado que esté en la caché de nombres (null = desconocido, sin consultar a Mojang). */
    private UUID resolve(String name) {
        Player p = online.apply(name);
        if (p != null) {
            return p.getUniqueId();
        }
        OfflinePlayer off = offline.apply(name);
        return off == null ? null : off.getUniqueId();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return suggest(args);
    }

    List<String> suggest(String[] args) {
        if (args.length <= 1) {
            return TabCompleteUtil.filter(args.length == 0 ? "" : args[0], SUBS);
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && List.of("give", "take", "list", "play").contains(sub)) {
            return TabCompleteUtil.onlinePlayerNames(args[1]);
        }
        if (args.length == 3 && List.of("give", "take", "play").contains(sub)) {
            return TabCompleteUtil.filter(args[2], catalog.all().stream().map(Emote::id).toList());
        }
        return List.of();
    }
}
