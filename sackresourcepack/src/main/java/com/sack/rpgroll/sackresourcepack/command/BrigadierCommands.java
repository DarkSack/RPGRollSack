package com.sack.rpgroll.sackresourcepack.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collection;
import java.util.List;

/**
 * Registra los comandos con la API Brigadier de Paper en vez de declararlos en
 * {@code plugin.yml}.
 * <p>
 * Copia deliberada de la clase de {@code :common}: SackResourcePack se vende
 * aparte y no depende de ningún módulo de RPGRoll. Si cambia una, cambiar la otra.
 * <p>
 * Existe por {@code execute as}. Un comando de {@code plugin.yml} recibe
 * siempre a quien <em>escribió</em> el comando; el jugador que
 * {@code execute as} pone como ejecutor no llega por ningún lado, así que
 * {@code execute as @a run /rpg stats} se rechazaba con "solo jugadores".
 * Desenvolver {@link org.bukkit.command.ProxiedCommandSender} no alcanza: en
 * Paper 26.2 esa clase ya no aparece en este camino (verificado en un servidor
 * real, con el desenvolvimiento desplegado y aun así rechazado).
 * <p>
 * Brigadier sí lo expone: {@link CommandSourceStack#getExecutor()} devuelve la
 * entidad del {@code as}, separada de {@link CommandSourceStack#getSender()}.
 * <p>
 * La migración no toca la lógica de ningún comando: los
 * {@link CommandExecutor} y {@link TabCompleter} que ya existen se reutilizan
 * tal cual, y acá solo se resuelve <em>quién</em> los ejecuta. Por eso el
 * cambio por módulo es sacar el bloque {@code commands:} del plugin.yml y
 * cambiar {@code getCommand(...).setExecutor(...)} por una llamada acá.
 */
public final class BrigadierCommands {

    private BrigadierCommands() {
    }

    /**
     * Registra un comando reutilizando su executor actual.
     *
     * @param executor  el mismo {@link CommandExecutor} de siempre
     * @param completer autocompletado, o null si no tiene
     */
    public static void register(JavaPlugin plugin, String name, String description, List<String> aliases,
            CommandExecutor executor, TabCompleter completer, String permission) {

        plugin.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event ->
                event.registrar().register(name, description, aliases,
                        new ExecutorBridge(name, executor, completer, permission)));
    }

    /**
     * Variante corta: usa el executor también como autocompletado si lo
     * implementa, que es el caso en todo el ecosistema.
     */
    public static void register(JavaPlugin plugin, String name, String description, String permission,
            CommandExecutor executor) {

        register(plugin, name, description, List.of(), executor,
                executor instanceof TabCompleter completer ? completer : null, permission);
    }

    /** Traduce entre lo que pide Brigadier y lo que ya implementan los comandos. */
    private record ExecutorBridge(String name, CommandExecutor executor, TabCompleter completer, String permission)
            implements BasicCommand {

        @Override
        public void execute(CommandSourceStack source, String[] args) {
            executor.onCommand(resolve(source), new StubCommand(name), name, args);
        }

        @Override
        public Collection<String> suggest(CommandSourceStack source, String[] args) {

            if (completer == null) {
                return List.of();
            }

            List<String> suggestions =
                    completer.onTabComplete(resolve(source), new StubCommand(name), name, args);

            return suggestions != null ? suggestions : List.of();
        }

        @Override
        public String permission() {
            return permission;
        }

        /**
         * El jugador del {@code execute as} si lo hay; si no, quien escribió.
         * <p>
         * Este es todo el arreglo: es la información que el camino de
         * plugin.yml nunca entrega.
         */
        private CommandSender resolve(CommandSourceStack source) {
            return source.getExecutor() instanceof Player player ? player : source.getSender();
        }
    }

    /**
     * Los executors reciben un {@link Command} que casi ninguno mira (4 usos en
     * todo el ecosistema, y solo para leer el nombre). Brigadier no entrega uno,
     * así que se les pasa este, que solo conoce su nombre.
     */
    private static final class StubCommand extends Command {

        private StubCommand(String name) {
            super(name);
        }

        @Override
        public boolean execute(CommandSender sender, String label, String[] args) {
            return true;
        }
    }

}
