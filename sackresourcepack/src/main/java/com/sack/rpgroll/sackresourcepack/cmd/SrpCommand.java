package com.sack.rpgroll.sackresourcepack.cmd;

import com.sack.rpgroll.sackresourcepack.SackResourcePackPlugin;
import com.sack.rpgroll.sackresourcepack.build.BuildResult;
import com.sack.rpgroll.sackresourcepack.gui.DashboardGUI;
import com.sack.rpgroll.sackresourcepack.lang.LangManager;
import com.sack.rpgroll.sackresourcepack.manifest.AssetModule;
import com.sack.rpgroll.sackresourcepack.merge.OverrideNotice;
import com.sack.rpgroll.sackresourcepack.validation.ValidationIssue;

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

    private LangManager lang() {
        return plugin.lang();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("sackresourcepack.admin.*")) {
            lang().send(sender, "command.no-permission");
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
                    lang().send(sender, "command.gui-players-only");
                }
            }
            default -> sendUsage(sender);
        }

        return true;
    }

    private void openDashboard(Player player) {
        new DashboardGUI(player, plugin, plugin.getBuildEngine(), plugin.getHttpServer().assetBaseUrl(), lang(),
                () -> handleBuild(player, true),
                () -> handleBuild(player, false),
                plugin::publish,
                plugin::toggleDevelopmentMode,
                plugin::isDevelopmentModeActive)
                .open();
    }

    private void handleReload(CommandSender sender) {
        plugin.reloadConfig();
        lang().reload(plugin.getConfig().getString("language", "es"));
        lang().send(sender, "command.reload-done");
    }

    private void handleBuild(CommandSender sender, boolean force) {

        BuildResult result = plugin.getBuildEngine().build(force);

        long errors = result.issues().stream().filter(i -> i.severity() == ValidationIssue.Severity.ERROR).count()
                + result.resolutionErrors().size();
        long warnings = result.issues().stream().filter(i -> i.severity() == ValidationIssue.Severity.WARNING).count();

        String type = lang().raw(force ? "command.build-type-rebuild" : "command.build-type-validate");

        sender.sendMessage(lang().component("command.build-result",
                "type", type, "modules", result.modules().size(), "errors", errors, "warnings", warnings,
                "sha1", result.sha1()));

        result.resolutionErrors().forEach(error ->
                sender.sendMessage(lang().component("command.build-resolution-error", "error", error)));

        result.issues().forEach(issue -> sender.sendMessage(lang().component(
                issue.severity() == ValidationIssue.Severity.ERROR ? "command.build-issue-error" : "command.build-issue-warning",
                "message", issue.message())));
    }

    private void handlePublish(CommandSender sender) {
        plugin.publish();
        lang().send(sender, "command.publish-done");
    }

    private void handleStatus(CommandSender sender) {

        BuildResult result = plugin.getBuildEngine().getLastResult();

        sender.sendMessage(lang().component("command.status-header"));
        sender.sendMessage(lang().component("command.status-modules", "count", result != null ? result.modules().size() : 0));
        sender.sendMessage(lang().component("command.status-sha1", "sha1", result != null ? result.sha1() : "—"));
        sender.sendMessage(lang().component("command.status-http", "state",
                lang().raw(plugin.getHttpServer().isRunning() ? "command.state-active" : "command.state-inactive")));
        sender.sendMessage(lang().component("command.status-devmode", "state",
                lang().raw(plugin.isDevelopmentModeActive() ? "command.state-active" : "command.state-inactive")));
        sender.sendMessage(lang().component("command.status-datapack", "state",
                lang().raw(plugin.isDatapackEnabled() ? "command.state-enabled" : "command.state-disabled")));
    }

    private void handleContent(CommandSender sender) {

        BuildResult result = plugin.getBuildEngine().getLastResult();
        List<AssetModule> modules = result != null ? result.modules() : List.of();

        sender.sendMessage(lang().component("command.content-header", "count", modules.size()));

        for (AssetModule module : modules) {
            sender.sendMessage(lang().component("command.content-line",
                    "id", module.id(), "version", module.version(), "author", module.author(),
                    "priority", module.priority()));
        }
    }

    private void handleConflicts(CommandSender sender) {

        var mergeResult = plugin.getBuildEngine().getLastMergeResult();

        if (mergeResult == null || mergeResult.overrides().isEmpty()) {
            lang().send(sender, "command.conflicts-none");
            return;
        }

        sender.sendMessage(lang().component("command.conflicts-header", "count", mergeResult.overrides().size()));

        for (OverrideNotice notice : mergeResult.overrides()) {
            sender.sendMessage(lang().component("command.conflicts-line",
                    "path", notice.relativePath(), "previous", notice.previousModuleId(), "current", notice.newModuleId()));
        }
    }

    private void handleAssetType(CommandSender sender, String folderName) {

        File mergedDirectory = plugin.getBuildEngine().getMergedDirectory();
        File assetsRoot = new File(mergedDirectory, "assets");

        if (!assetsRoot.isDirectory()) {
            lang().send(sender, "command.assettype-no-build");
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
            sender.sendMessage(lang().component("command.assettype-error", "error", e.getMessage()));
            return;
        }

        sender.sendMessage(lang().component("command.assettype-header", "type", folderName, "count", matches.size()));
        matches.forEach(match -> sender.sendMessage(lang().component("command.assettype-line", "path", match)));
    }

    private void handleCache(CommandSender sender) {
        plugin.getBuildEngine().invalidateCache();
        lang().send(sender, "command.cache-done");
    }

    private void handleDev(CommandSender sender) {
        plugin.toggleDevelopmentMode();
        sender.sendMessage(lang().component("command.dev-toggled", "state",
                lang().raw(plugin.isDevelopmentModeActive() ? "command.dev-state-active" : "command.dev-state-inactive")));
    }

    private void handleDatapack(CommandSender sender, String[] args) {

        if (!plugin.isDatapackEnabled()) {
            lang().send(sender, "command.datapack-disabled");
            return;
        }

        String sub = args.length > 1 ? args[1].toLowerCase() : "";

        switch (sub) {
            case "build" -> {
                var result = plugin.getDatapackBuildEngine().build();
                plugin.getDatapackBuildEngine().distribute();
                sender.sendMessage(lang().component("command.datapack-build-done", "count", result.modules().size()));
                result.resolutionErrors().forEach(error ->
                        sender.sendMessage(lang().component("command.datapack-build-resolution-error", "error", error)));
            }
            case "reload" -> {
                plugin.getDatapackBuildEngine().reload();
                lang().send(sender, "command.datapack-reload-done");
            }
            default -> lang().send(sender, "command.datapack-usage");
        }
    }

    private void sendUsage(CommandSender sender) {
        lang().send(sender, "command.usage");
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
