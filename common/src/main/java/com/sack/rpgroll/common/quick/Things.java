package com.sack.rpgroll.common.quick;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Lo que {@code /dar} puede dar y {@code /invocar} puede invocar. Cada módulo registra lo suyo
 * con un tipo ({@code item}, {@code mueble}, {@code mob}…) y el jugador escribe {@code tipo:id}, o
 * solo el id si no hay dos tipos que lo tengan. Lo de Minecraft va con el tipo {@link #VANILLA}.
 */
public final class Things {

    public static final String VANILLA = "minecraft";

    private Things() {
    }

    /** Algo con nombre de tipo y una lista de ids. */
    public interface Source {
        String type();

        Collection<String> ids();
    }

    /** Cosas que se pueden dar. */
    public interface Give extends Source {
        /** Una pieza nueva cada vez: {@code /dar} la pide una vez por unidad (y apila las que son iguales). */
        Optional<ItemStack> create(String id);
    }

    /** Criaturas que se pueden invocar. */
    public interface Summon extends Source {
        boolean spawn(String id, Location at);
    }

    public record Match(Source source, String id) {
    }

    /** Registra en una línea: {@code Things.give(this, "mueble", ids, id -> crear(id))}. */
    public static void give(Plugin owner, String type, Supplier<? extends Collection<String>> ids,
            Function<String, Optional<ItemStack>> create) {
        register(owner, new Give() {
            public String type() {
                return type;
            }

            public Collection<String> ids() {
                return ids.get();
            }

            public Optional<ItemStack> create(String id) {
                return create.apply(id);
            }
        });
    }

    public static void summon(Plugin owner, String type, Supplier<? extends Collection<String>> ids,
            BiPredicate<String, Location> spawn) {
        register(owner, new Summon() {
            public String type() {
                return type;
            }

            public Collection<String> ids() {
                return ids.get();
            }

            public boolean spawn(String id, Location at) {
                return spawn.test(id, at);
            }
        });
    }

    /** Bukkit lo da de baja solo cuando {@code owner} se deshabilita. */
    public static void register(Plugin owner, Give source) {
        Bukkit.getServicesManager().register(Give.class, source, owner, ServicePriority.Normal);
    }

    public static void register(Plugin owner, Summon source) {
        Bukkit.getServicesManager().register(Summon.class, source, owner, ServicePriority.Normal);
    }

    public static <T extends Source> List<T> sources(Class<T> kind) {
        List<T> out = new ArrayList<>();
        for (RegisteredServiceProvider<T> r : Bukkit.getServicesManager().getRegistrations(kind)) {
            out.add(r.getProvider());
        }
        // Lo de Minecraft al final: un id suelto busca antes en RPGRoll.
        out.sort((a, b) -> Boolean.compare(VANILLA.equals(a.type()), VANILLA.equals(b.type())));
        return out;
    }

    /** {@code tipo:id}, o un id suelto que solo tenga un tipo de RPGRoll (o, si ninguno, Minecraft). */
    public static <T extends Source> Optional<Match> resolve(List<T> sources, String input) {
        String in = input.toLowerCase(Locale.ROOT);
        int colon = in.indexOf(':');
        if (colon > 0) {
            String type = in.substring(0, colon);
            String id = in.substring(colon + 1);
            for (Source s : sources) {
                if (s.type().equals(type) && has(s, id)) {
                    return Optional.of(new Match(s, id));
                }
            }
            return Optional.empty();
        }
        List<Source> found = new ArrayList<>();
        Source vanilla = null;
        for (Source s : sources) {
            if (has(s, in)) {
                if (VANILLA.equals(s.type())) {
                    vanilla = s;
                } else {
                    found.add(s);
                }
            }
        }
        if (found.size() == 1) {
            return Optional.of(new Match(found.get(0), in));
        }
        return found.isEmpty() && vanilla != null ? Optional.of(new Match(vanilla, in)) : Optional.empty();
    }

    /** Las formas {@code tipo:id} de un id suelto que está en más de un tipo de RPGRoll. */
    public static <T extends Source> List<String> candidates(List<T> sources, String input) {
        String in = input.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (Source s : sources) {
            if (!VANILLA.equals(s.type()) && has(s, in)) {
                out.add(s.type() + ":" + in);
            }
        }
        return out;
    }

    /**
     * Sin escribir nada, los tipos. Con algo escrito, los ids de RPGRoll que empiezan así (sea cual
     * sea su tipo), los tipos y por último los de Minecraft. Con {@code tipo:}, los ids de ese tipo.
     */
    public static <T extends Source> List<String> suggest(List<T> sources, String typed, int limit) {
        String in = typed.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        int colon = in.indexOf(':');
        if (colon > 0) {
            String type = in.substring(0, colon);
            String prefix = in.substring(colon + 1);
            for (Source s : sources) {
                if (s.type().equals(type)) {
                    addIds(out, s, prefix, limit);
                }
            }
            return out;
        }
        if (!in.isEmpty()) {
            for (Source s : sources) {
                if (!VANILLA.equals(s.type())) {
                    addIds(out, s, in, limit);
                }
            }
        }
        for (Source s : sources) {
            if (out.size() < limit && s.type().startsWith(in) && !out.contains(s.type() + ":")) {
                out.add(s.type() + ":");
            }
        }
        if (!in.isEmpty()) {
            for (Source s : sources) {
                if (VANILLA.equals(s.type())) {
                    addIds(out, s, in, limit);
                }
            }
        }
        return out;
    }

    private static void addIds(List<String> out, Source s, String prefix, int limit) {
        for (String id : s.ids()) {
            if (out.size() >= limit) {
                return;
            }
            if (id.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                out.add(s.type() + ":" + id);
            }
        }
    }

    private static boolean has(Source s, String id) {
        for (String candidate : s.ids()) {
            if (candidate.equalsIgnoreCase(id)) {
                return true;
            }
        }
        return false;
    }
}
