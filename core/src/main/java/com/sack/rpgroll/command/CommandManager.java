package com.sack.rpgroll.command;

import com.sack.rpgroll.common.command.Senders;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.common.lang.LangManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Gestor central de comandos del plugin.
 * Maneja el registro y ejecución de todos los subcomandos.
 */
public class CommandManager implements CommandExecutor, TabCompleter {

    private final RPGRoll plugin;
    private final Map<String, RPGCommand> commands = new HashMap<>();
    private final Map<String, String> aliases = new HashMap<>();

    public CommandManager(RPGRoll plugin) {
        this.plugin = plugin;
    }

    /**
     * Registra un subcomando.
     */
    public void register(RPGCommand command) {

        String name = command.getName().toLowerCase();
        commands.put(name, command);

        // Registrar aliases
        for (String alias : command.getAliases()) {
            aliases.put(alias.toLowerCase(), name);
        }

        plugin.getLogger().info("✔ Comando registrado: " + command.getName());
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {

        // Sin argumentos, o "help" explícito, mostrar ayuda
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sendHelp(sender);
            return true;
        }

        LangManager lang = plugin.getBootstrap().getServices().get(LangManager.class);

        // Obtener subcomando
        String subCommandName = args[0].toLowerCase();
        RPGCommand subCommand = getCommand(subCommandName);

        if (subCommand == null) {
            lang.send(sender, "command_manager.unknown_subcommand", "input", args[0]);
            lang.send(sender, "command_manager.use_help");
            return true;
        }

        // Verificar si es solo para jugadores
        if (subCommand.isPlayerOnly() && !(Senders.asPlayer(sender) instanceof Player)) {
            lang.send(sender, "general.player_only");
            return true;
        }

        // Verificar permisos
        String permission = subCommand.getPermission();
        if (permission != null && !sender.hasPermission(permission)) {
            lang.send(sender, "general.no_permission");
            return true;
        }

        // Ejecutar comando
        try {
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
            subCommand.execute(sender, subArgs);
        } catch (Exception exception) {
            lang.send(sender, "error.command_failed");
            exception.printStackTrace();
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args) {

        List<String> completions = new ArrayList<>();

        // Autocompletar subcomandos
        if (args.length == 1) {
            String input = args[0].toLowerCase();

            for (RPGCommand cmd : commands.values()) {

                // Verificar permisos
                String permission = cmd.getPermission();
                if (permission != null && !sender.hasPermission(permission)) {
                    continue;
                }

                if (cmd.getName().toLowerCase().startsWith(input)) {
                    completions.add(cmd.getName());
                }
            }

            Collections.sort(completions);
            return completions;
        }

        // Delegar autocompletado de argumentos al subcomando correspondiente
        RPGCommand subCommand = getCommand(args[0]);
        if (subCommand == null) {
            return completions;
        }

        String permission = subCommand.getPermission();
        if (permission != null && !sender.hasPermission(permission)) {
            return completions;
        }

        String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
        String currentInput = subArgs[subArgs.length - 1].toLowerCase();

        for (String suggestion : subCommand.getTabCompletions(sender, subArgs)) {
            if (suggestion.toLowerCase().startsWith(currentInput)) {
                completions.add(suggestion);
            }
        }

        Collections.sort(completions);
        return completions;
    }

    /**
     * Obtiene un comando por nombre o alias.
     */
    private RPGCommand getCommand(String name) {
        name = name.toLowerCase();

        // Buscar por nombre directo
        if (commands.containsKey(name)) {
            return commands.get(name);
        }

        // Buscar por alias
        if (aliases.containsKey(name)) {
            String realName = aliases.get(name);
            return commands.get(realName);
        }

        return null;
    }

    /**
     * Muestra la ayuda con todos los comandos disponibles.
     */
    private void sendHelp(CommandSender sender) {
        LangManager lang = plugin.getBootstrap().getServices().get(LangManager.class);
        lang.send(sender, "command_manager.help_header");

        for (RPGCommand cmd : commands.values()) {

            // Verificar permisos
            String permission = cmd.getPermission();
            if (permission != null && !sender.hasPermission(permission)) {
                continue;
            }

            sender.sendMessage(
                    Component.text(cmd.getUsage(), NamedTextColor.YELLOW)
                            .append(Component.text(" - ", NamedTextColor.GRAY))
                            .append(Component.text(cmd.getDescription(), NamedTextColor.GRAY)));
        }

        lang.send(sender, "command_manager.help_footer");
    }

    /**
     * Obtiene todos los comandos registrados.
     */
    public Collection<RPGCommand> getCommands() {
        return Collections.unmodifiableCollection(commands.values());
    }

}
