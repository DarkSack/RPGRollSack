package com.sack.rpgroll.sackresourcepack.cmd;

import com.sack.rpgroll.sackresourcepack.SackResourcePackPlugin;
import com.sack.rpgroll.sackresourcepack.build.BuildResult;
import com.sack.rpgroll.sackresourcepack.gui.DashboardGUI;
import com.sack.rpgroll.sackresourcepack.manifest.AssetModule;
import com.sack.rpgroll.sackresourcepack.merge.OverrideNotice;
import com.sack.rpgroll.sackresourcepack.validation.ValidationIssue;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * /srp reload|rebuild|validate|publish|status|content|conflicts|models|textures|sounds|fonts|cache|dev|datapack|gui
 * Sin argumentos, si lo ejecuta un jugador, abre el Dashboard.
 */
public class SrpCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = List.of("reload", "rebuild", "validate", "publish", "status",
            "content", "conflicts", "models", "textures", "sounds", "fonts", "cache", "dev", "datapack", "gui");
    private static final List<String> DATAPACK_SUBCOMMANDS = List.of("build", "reload");

    private final SackResourcePackPlugin plugin;

    public SrpCommand(SackResourcePackPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("sackresourcepack.admin.*")) {
            sender.sendMessage(Component.text("No tenés permiso para usar este comando.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {

            if (sender instanceof Player player) {
                openDashboard(player);
                return true;
            }

            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> handleReload(sender);
            case "rebuild" -> handleBuild(sender, true);
            case "validate" -> handleBuild(sender, false);
            case "publish" -> handlePublish(sender);
            case "status" -> handleStatus(sender);
            case "content" -> handleContent(sender);
            case "conflicts" -> handleConflicts(sender);
            case "models" -> handleAssetType(sender, "models");
            case "textures" -> handleAssetType(sender, "textures");
            case "sounds" -> handleAssetType(sender, "sounds");
            case "fonts" -> handleAssetType(sender, "font");
            case "cache" -> handleCache(sender);
            case "dev" -> handleDev(sender);
            case "datapack" -> handleDatapack(sender, args);
            case "gui" -> {
                if (sender instanceof Player player) {
                    openDashboard(player);
                } else {
                    sender.sendMessage(Component.text("Solo un jugador puede abrir el Dashboard.", NamedTextColor.RED));
                }
            }
            default -> sendUsage(sender);
        }

        return true;
    }

    private void openDashboard(Player player) {
        new DashboardGUI(player, plugin, plugin.getBuildEngine(), plugin.getHttpServer().assetBaseUrl(),
                () -> handleBuild(player, true),
                () -> handleBuild(player, false),
                plugin::publish,
                plugin::toggleDevelopmentMode,
                plugin::isDevelopmentModeActive)
                .open();
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        sender.sendMessage(Component.text(
                "✔ config.yml recargado. Cambios de puerto HTTP, carpeta de contenido o pack-format requieren reiniciar el plugin.",
                NamedTextColor.YELLOW));
    }

    private void handleBuild(CommandSender sender, boolean force) {

        BuildResult result = plugin.getBuildEngine().build(force);

        long errors = result.issues().stream().filter(i -> i.severity() == ValidationIssue.Severity.ERROR).count()
                + result.resolutionErrors().size();
        long warnings = result.issues().stream().filter(i -> i.severity() == ValidationIssue.Severity.WARNING).count();

        sender.sendMessage(Component.text(
                (force ? "Rebuild" : "Validate") + " completo — " + result.modules().size() + " módulo(s), "
                        + errors + " error(es), " + warnings + " warning(s). SHA1: " + result.sha1(),
                errors > 0 ? NamedTextColor.RED : warnings > 0 ? NamedTextColor.YELLOW : NamedTextColor.GREEN));

        result.resolutionErrors().forEach(error -> sender.sendMessage(Component.text("  ✘ " + error, NamedTextColor.RED)));

        result.issues().forEach(issue -> sender.sendMessage(Component.text(
                "  " + (issue.severity() == ValidationIssue.Severity.ERROR ? "✘ " : "⚠ ") + issue.message(),
                issue.severity() == ValidationIssue.Severity.ERROR ? NamedTextColor.RED : NamedTextColor.YELLOW)));
    }

    private void handlePublish(CommandSender sender) {
        plugin.publish();
        sender.sendMessage(Component.text("✔ Publicación disparada — ver la consola para el resultado del upload (si aplica).",
                NamedTextColor.GREEN));
    }

    private void handleStatus(CommandSender sender) {

        BuildResult result = plugin.getBuildEngine().getLastResult();

        sender.sendMessage(Component.text("=== Estado de SackResourcePack ===", NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Módulos: " + (result != null ? result.modules().size() : 0), NamedTextColor.WHITE));
        sender.sendMessage(Component.text("Último SHA1: " + (result != null ? result.sha1() : "—"), NamedTextColor.WHITE));
        sender.sendMessage(Component.text("Host HTTP: " + (plugin.getHttpServer().isRunning() ? "activo" : "inactivo"),
                NamedTextColor.WHITE));
        sender.sendMessage(Component.text("Modo desarrollo: " + (plugin.isDevelopmentModeActive() ? "activo" : "inactivo"),
                NamedTextColor.WHITE));
        sender.sendMessage(Component.text("Datapack: " + (plugin.isDatapackEnabled() ? "habilitado" : "deshabilitado"),
                NamedTextColor.WHITE));
    }

    private void handleContent(CommandSender sender) {

        BuildResult result = plugin.getBuildEngine().getLastResult();
        List<AssetModule> modules = result != null ? result.modules() : List.of();

        sender.sendMessage(Component.text("=== Módulos de contenido (" + modules.size() + ") ===", NamedTextColor.GOLD));

        for (AssetModule module : modules) {
            sender.sendMessage(Component.text(
                    "- " + module.id() + " v" + module.version() + " (" + module.author() + ") prioridad="
                            + module.priority(),
                    NamedTextColor.WHITE));
        }
    }

    private void handleConflicts(CommandSender sender) {

        var mergeResult = plugin.getBuildEngine().getLastMergeResult();

        if (mergeResult == null || mergeResult.overrides().isEmpty()) {
            sender.sendMessage(Component.text("Sin conflictos registrados en el último build.", NamedTextColor.GREEN));
            return;
        }

        sender.sendMessage(Component.text("=== Conflictos (" + mergeResult.overrides().size() + ") ===", NamedTextColor.GOLD));

        for (OverrideNotice notice : mergeResult.overrides()) {
            sender.sendMessage(Component.text(
                    "- " + notice.relativePath() + ": " + notice.previousModuleId() + " → " + notice.newModuleId(),
                    NamedTextColor.YELLOW));
        }
    }

    private void handleAssetType(CommandSender sender, String folderName) {

        File mergedDirectory = plugin.getBuildEngine().getMergedDirectory();
        File assetsRoot = new File(mergedDirectory, "assets");

        if (!assetsRoot.isDirectory()) {
            sender.sendMessage(Component.text("No hay ningún build todavía — corré /srp rebuild.", NamedTextColor.RED));
            return;
        }

        List<String> matches;

        try (Stream<Path> walk = Files.walk(assetsRoot.toPath())) {
            matches = walk.filter(Files::isRegularFile)
                    .map(path -> assetsRoot.toPath().relativize(path).toString().replace('\\', '/'))
                    .filter(relative -> relative.contains("/" + folderName + "/"))
                    .sorted()
                    .toList();
        } catch (Exception e) {
            sender.sendMessage(Component.text("✘ Error recorriendo el pack: " + e.getMessage(), NamedTextColor.RED));
            return;
        }

        sender.sendMessage(Component.text("=== " + folderName + " (" + matches.size() + ") ===", NamedTextColor.GOLD));
        matches.forEach(match -> sender.sendMessage(Component.text("- " + match, NamedTextColor.WHITE)));
    }

    private void handleCache(CommandSender sender) {
        plugin.getBuildEngine().invalidateCache();
        sender.sendMessage(Component.text("✔ Caché invalidado — el próximo build (rebuild o validate) va a reconstruir todo desde cero.",
                NamedTextColor.GREEN));
    }

    private void handleDev(CommandSender sender) {
        plugin.toggleDevelopmentMode();
        sender.sendMessage(Component.text(
                "Modo desarrollo ahora: " + (plugin.isDevelopmentModeActive() ? "ACTIVO" : "inactivo"),
                NamedTextColor.YELLOW));
    }

    private void handleDatapack(CommandSender sender, String[] args) {

        if (!plugin.isDatapackEnabled()) {
            sender.sendMessage(Component.text(
                    "✘ El datapack está deshabilitado — activá 'datapack.enabled' en config.yml.",
                    NamedTextColor.RED));
            return;
        }

        String sub = args.length > 1 ? args[1].toLowerCase() : "";

        switch (sub) {
            case "build" -> {
                var result = plugin.getDatapackBuildEngine().build();
                plugin.getDatapackBuildEngine().distribute();
                sender.sendMessage(Component.text(
                        "✔ Datapack fusionado y copiado a datapacks/ — " + result.modules().size()
                                + " módulo(s). Corré '/srp datapack reload' para aplicarlo.",
                        result.hasErrors() ? NamedTextColor.YELLOW : NamedTextColor.GREEN));
                result.resolutionErrors()
                        .forEach(error -> sender.sendMessage(Component.text("  ✘ " + error, NamedTextColor.RED)));
            }
            case "reload" -> {
                plugin.getDatapackBuildEngine().reload();
                sender.sendMessage(Component.text(
                        "✔ Server#reloadData() ejecutado — recetas/loot tables/tags/advancements recargados.",
                        NamedTextColor.GREEN));
            }
            default -> sender.sendMessage(Component.text("Uso: /srp datapack <build|reload>", NamedTextColor.RED));
        }
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text(
                "Uso: /srp <reload|rebuild|validate|publish|status|content|conflicts|models|textures|sounds|fonts|cache|dev|datapack|gui>",
                NamedTextColor.RED));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (args.length == 1) {
            return filter(args[0], SUBCOMMANDS);
        }

        if (args.length == 2 && "datapack".equalsIgnoreCase(args[0])) {
            return filter(args[1], DATAPACK_SUBCOMMANDS);
        }

        return List.of();
    }

    private List<String> filter(String token, List<String> options) {
        List<String> result = new ArrayList<>();
        StringUtil.copyPartialMatches(token, options, result);
        return result;
    }

}
