package com.sack.rpgroll.common.quick;

import com.sack.rpgroll.common.command.BrigadierCommands;
import com.sack.rpgroll.common.command.CommandWords;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * {@code /dar}, {@code /invocar} y {@code /abrir}: la forma corta de darse algo, invocar algo o
 * abrir un menú, valga de qué módulo sea. Cada módulo registra lo suyo en {@link Things}; los menús
 * salen de la tabla {@code abrir:} del config de la Lib y solo se ofrecen los que tienen su comando.
 */
public final class QuickCommands {

    static final int MAX_GIVE = 36 * 64;
    static final int MAX_SUMMON = 50;
    private static final int LIMIT = 80;

    private QuickCommands() {
    }

    public static void register(JavaPlugin plugin) {

        Things.register(plugin, new VanillaItems());
        Things.register(plugin, new VanillaMobs());

        Give give = new Give();
        BrigadierCommands.register(plugin, "dar", "Da cualquier cosa de RPGRoll o de Minecraft", List.of("rgive"),
                give, give, "rpgrolllib.dar");
        Summon summon = new Summon();
        BrigadierCommands.register(plugin, "invocar", "Invoca criaturas de RPGRoll o de Minecraft",
                List.of("rsummon"), summon, summon, "rpgrolllib.invocar");
        Open open = new Open(plugin);
        BrigadierCommands.register(plugin, "abrir", "Abre un menú", List.of(), open, open, null);
    }

    /** {@code [jugador] <cosa> [cantidad]}: el jugador solo cuenta si hay algo detrás y está conectado. */
    record GiveArgs(String target, String thing, int amount) {

        static GiveArgs parse(String[] args, Predicate<String> online) {
            int i = args.length >= 2 && online.test(args[0]) ? 1 : 0;
            String target = i == 1 ? args[0] : null;
            int amount = args.length > i + 1 ? QuickCommands.amount(args[i + 1], MAX_GIVE) : 1;
            return new GiveArgs(target, args[i], amount);
        }
    }

    /** -1 si no es un número positivo; si se pasa, el tope. */
    static int amount(String text, int max) {
        try {
            int n = Integer.parseInt(text);
            return n < 1 ? -1 : Math.min(n, max);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // ---------------------------------------------------------------- /dar

    private static final class Give implements CommandExecutor, TabCompleter {

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (args.length == 0) {
                Msg.send(sender, "give.usage");
                return true;
            }
            GiveArgs a = GiveArgs.parse(args, name -> Bukkit.getPlayerExact(name) != null);
            Player target = a.target() != null ? Bukkit.getPlayerExact(a.target())
                    : sender instanceof Player p ? p : null;
            if (target == null) {
                Msg.send(sender, "give.usage");
                return true;
            }
            if (a.amount() < 0) {
                Msg.send(sender, "bad_amount");
                return true;
            }
            List<Things.Give> sources = Things.sources(Things.Give.class);
            Optional<Things.Match> match = Things.resolve(sources, a.thing());
            Optional<ItemStack> item = match.flatMap(m -> ((Things.Give) m.source()).create(m.id()));
            if (item.isEmpty()) {
                unknown(sender, sources, a.thing());
                return true;
            }
            Things.Give source = (Things.Give) match.get().source();
            int left = a.amount();
            int max = Math.max(1, item.get().getMaxStackSize());
            ItemStack next = item.get();
            while (left > 0) {
                // Una pieza nueva por pila y no un clon: hay ítems con datos propios de cada ejemplar.
                ItemStack stack = next != null ? next : source.create(match.get().id()).orElse(item.get().clone());
                next = null;
                stack.setAmount(Math.min(left, max));
                left -= stack.getAmount();
                // Lo que no cabe cae a sus pies, como el /give de Minecraft.
                target.getInventory().addItem(stack).values()
                        .forEach(rest -> target.getWorld().dropItemNaturally(target.getLocation(), rest));
            }
            Msg.send(sender, "give.done", a.amount(), match.get().source().type() + ":" + match.get().id(),
                    target.getName());
            return true;
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            List<Things.Give> sources = Things.sources(Things.Give.class);
            String last = args[args.length - 1];
            boolean playerFirst = args.length >= 2 && Bukkit.getPlayerExact(args[0]) != null;
            if (args.length == 1) {
                List<String> out = new ArrayList<>(players(last));
                out.addAll(Things.suggest(sources, last, LIMIT));
                return out;
            }
            if (args.length == 2 && playerFirst) {
                return Things.suggest(sources, last, LIMIT);
            }
            if (args.length == (playerFirst ? 3 : 2)) {
                return prefixed(List.of("1", "16", "64"), last);
            }
            return List.of();
        }
    }

    // ---------------------------------------------------------------- /invocar

    private static final class Summon implements CommandExecutor, TabCompleter {

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (args.length == 0) {
                Msg.send(sender, "summon.usage");
                return true;
            }
            int amount = args.length > 1 ? amount(args[1], MAX_SUMMON) : 1;
            if (amount < 0) {
                Msg.send(sender, "bad_amount");
                return true;
            }
            Location at;
            if (args.length > 2) {
                Player p = Bukkit.getPlayerExact(args[2]);
                if (p == null) {
                    Msg.send(sender, "no_player", args[2]);
                    return true;
                }
                at = p.getLocation();
            } else if (sender instanceof Entity e) {
                at = e.getLocation();
            } else {
                Msg.send(sender, "summon.usage");
                return true;
            }
            List<Things.Summon> sources = Things.sources(Things.Summon.class);
            Optional<Things.Match> match = Things.resolve(sources, args[0]);
            if (match.isEmpty()) {
                unknown(sender, sources, args[0]);
                return true;
            }
            Things.Summon source = (Things.Summon) match.get().source();
            int done = 0;
            for (int i = 0; i < amount; i++) {
                if (source.spawn(match.get().id(), at)) {
                    done++;
                }
            }
            Msg.send(sender, "summon.done", done, match.get().source().type() + ":" + match.get().id());
            return true;
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            String last = args[args.length - 1];
            return switch (args.length) {
                case 1 -> Things.suggest(Things.sources(Things.Summon.class), last, LIMIT);
                case 2 -> prefixed(List.of("1", "5", "10"), last);
                case 3 -> players(last);
                default -> List.of();
            };
        }
    }

    // ---------------------------------------------------------------- /abrir

    private static final class Open implements CommandExecutor, TabCompleter {

        private final JavaPlugin plugin;

        private Open(JavaPlugin plugin) {
            this.plugin = plugin;
        }

        /** Los menús de la tabla cuyo comando existe en este servidor (sin ese módulo, no se ofrecen). */
        private Map<String, String> menus() {
            Map<String, String> out = new LinkedHashMap<>();
            ConfigurationSection table = plugin.getConfig().getConfigurationSection("abrir");
            if (table == null) {
                return out;
            }
            for (String id : table.getKeys(false)) {
                String run = table.getString(id, "").trim();
                String base = run.split(" ")[0];
                if (!base.isEmpty() && Bukkit.getCommandMap().getCommand(base) != null) {
                    out.put(id.toLowerCase(Locale.ROOT), run);
                }
            }
            return out;
        }

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            Map<String, String> menus = menus();
            if (args.length == 0) {
                Msg.send(sender, "open.usage", String.join(", ", menus.keySet()));
                return true;
            }
            String run = menus.get(args[0].toLowerCase(Locale.ROOT));
            if (run == null) {
                Msg.send(sender, "open.unknown", args[0], String.join(", ", menus.keySet()));
                return true;
            }
            Player target;
            if (args.length > 1) {
                if (!sender.hasPermission("rpgrolllib.abrir.otros")) {
                    Msg.send(sender, "open.others");
                    return true;
                }
                target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    Msg.send(sender, "no_player", args[1]);
                    return true;
                }
            } else if (sender instanceof Player p) {
                target = p;
            } else {
                Msg.send(sender, "open.usage", String.join(", ", menus.keySet()));
                return true;
            }
            // Como si lo escribiera el jugador: pasa por los permisos de siempre.
            target.performCommand(run);
            return true;
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            String last = args[args.length - 1];
            if (args.length == 1) {
                return prefixed(new ArrayList<>(menus().keySet()), last);
            }
            return args.length == 2 && sender.hasPermission("rpgrolllib.abrir.otros") ? players(last) : List.of();
        }
    }

    // ---------------------------------------------------------------- lo de Minecraft

    private static final class VanillaItems implements Things.Give {

        private List<String> ids;

        @Override
        public String type() {
            return Things.VANILLA;
        }

        @Override
        public Collection<String> ids() {
            if (ids == null) {
                ids = Arrays.stream(Material.values()).filter(m -> m.isItem() && !m.isAir())
                        .map(m -> m.getKey().getKey()).toList();
            }
            return ids;
        }

        @Override
        public Optional<ItemStack> create(String id) {
            Material m = Material.matchMaterial(id);
            return m == null || !m.isItem() || m.isAir() ? Optional.empty() : Optional.of(new ItemStack(m));
        }
    }

    private static final class VanillaMobs implements Things.Summon {

        private List<String> ids;

        @Override
        public String type() {
            return Things.VANILLA;
        }

        @Override
        public Collection<String> ids() {
            if (ids == null) {
                ids = Arrays.stream(EntityType.values())
                        .filter(t -> t.isSpawnable() && t.isAlive() && t != EntityType.PLAYER)
                        .map(t -> t.getKey().getKey()).toList();
            }
            return ids;
        }

        @Override
        public boolean spawn(String id, Location at) {
            org.bukkit.NamespacedKey key = org.bukkit.NamespacedKey.fromString(id);
            EntityType type = key == null ? null : org.bukkit.Registry.ENTITY_TYPE.get(key);
            if (type == null || !type.isSpawnable() || !type.isAlive() || type == EntityType.PLAYER) {
                return false;
            }
            at.getWorld().spawnEntity(at, type);
            return true;
        }
    }

    // ---------------------------------------------------------------- comunes

    private static void unknown(CommandSender sender, List<? extends Things.Source> sources, String thing) {
        List<String> options = Things.candidates(sources, thing);
        if (options.isEmpty()) {
            Msg.send(sender, "unknown", thing);
        } else {
            Msg.send(sender, "ambiguous", thing, String.join(", ", options));
        }
    }

    private static List<String> players(String prefix) {
        List<String> out = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getName().toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))) {
                out.add(p.getName());
            }
        }
        return out;
    }

    private static List<String> prefixed(List<String> options, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(o -> o.toLowerCase(Locale.ROOT).startsWith(p)).toList();
    }

    /** Los pocos mensajes de estos comandos, en el idioma de quien escribe. */
    private static final class Msg {

        private static final Map<String, Map<String, String>> TEXT = Map.ofEntries(
                Map.entry("give.usage", Map.of(
                        "es", "§eUso: §f/dar [jugador] <cosa> [cantidad] §7(p. ej. /dar item:medallon_del_dragon)",
                        "en", "§eUsage: §f/dar [player] <thing> [amount] §7(e.g. /dar item:medallon_del_dragon)",
                        "pt", "§eUso: §f/dar [jogador] <coisa> [quantidade] §7(ex. /dar item:medallon_del_dragon)")),
                Map.entry("give.done", Map.of(
                        "es", "§aDiste §f{0} × {1} §aa §f{2}§a.",
                        "en", "§aGave §f{0} × {1} §ato §f{2}§a.",
                        "pt", "§aVocê deu §f{0} × {1} §apara §f{2}§a.")),
                Map.entry("summon.usage", Map.of(
                        "es", "§eUso: §f/invocar <criatura> [cantidad] [jugador]",
                        "en", "§eUsage: §f/invocar <creature> [amount] [player]",
                        "pt", "§eUso: §f/invocar <criatura> [quantidade] [jogador]")),
                Map.entry("summon.done", Map.of(
                        "es", "§aInvocaste §f{0} × {1}§a.",
                        "en", "§aSummoned §f{0} × {1}§a.",
                        "pt", "§aVocê invocou §f{0} × {1}§a.")),
                Map.entry("open.usage", Map.of(
                        "es", "§eUso: §f/abrir <menú> §7— {0}",
                        "en", "§eUsage: §f/abrir <menu> §7— {0}",
                        "pt", "§eUso: §f/abrir <menu> §7— {0}")),
                Map.entry("open.unknown", Map.of(
                        "es", "§cNo hay un menú «{0}». §7Hay: {1}",
                        "en", "§cThere is no menu \"{0}\". §7Available: {1}",
                        "pt", "§cNão existe o menu «{0}». §7Há: {1}")),
                Map.entry("open.others", Map.of(
                        "es", "§cNo puedes abrir menús a otros jugadores.",
                        "en", "§cYou can't open menus for other players.",
                        "pt", "§cVocê não pode abrir menus para outros jogadores.")),
                Map.entry("no_player", Map.of(
                        "es", "§c«{0}» no está conectado.",
                        "en", "§c\"{0}\" is not online.",
                        "pt", "§c«{0}» não está conectado.")),
                Map.entry("bad_amount", Map.of(
                        "es", "§cLa cantidad tiene que ser un número mayor que 0.",
                        "en", "§cThe amount must be a number above 0.",
                        "pt", "§cA quantidade precisa ser um número maior que 0.")),
                Map.entry("unknown", Map.of(
                        "es", "§cNo conozco «{0}». §7Pulsa Tab para ver lo que hay.",
                        "en", "§cUnknown \"{0}\". §7Press Tab to see what exists.",
                        "pt", "§cNão conheço «{0}». §7Aperte Tab para ver o que existe.")),
                Map.entry("ambiguous", Map.of(
                        "es", "§e«{0}» está en varios sitios; elige uno: §f{1}",
                        "en", "§e\"{0}\" exists in several places; pick one: §f{1}",
                        "pt", "§e«{0}» existe em vários lugares; escolha um: §f{1}")));

        static void send(CommandSender sender, String key, Object... args) {
            String text = TEXT.get(key).get(CommandWords.language(sender));
            for (int i = 0; i < args.length; i++) {
                text = text.replace("{" + i + "}", String.valueOf(args[i]));
            }
            sender.sendMessage(text);
        }
    }
}
