package com.sack.rpgroll.tab.scoreboard;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Carga plugins/RPGRoll-TAB/scoreboards/*.yml y resuelve la cadena de
 * herencia ({@code extends}/{@code replacements}, sección 36) en un
 * segundo paso, ya que un parser individual no puede ver otros archivos.
 */
public class ScoreboardManager extends ContentManager<ScoreboardDefinition> {

    private final Map<String, ScoreboardDefinition> resolved = new HashMap<>();

    public ScoreboardManager(JavaPlugin tabPlugin) {
        super(resolveCoreInstance(), new YamlLoader(tabPlugin), "scoreboards", "scoreboard", new ScoreboardParser());
    }

    public void initializeAndResolve() {
        initialize();
        resolveInheritance();
    }

    @Override
    public void reload() {
        super.reload();
        resolveInheritance();
    }

    public Optional<ScoreboardDefinition> getResolved(String id) {
        return Optional.ofNullable(resolved.get(id));
    }

    /** Scoreboards con prioridad, de mayor a menor (sección 14/15). */
    public List<ScoreboardDefinition> byPriorityDescending() {
        return getAll().stream()
                .sorted((a, b) -> Integer.compare(b.priority(), a.priority()))
                .toList();
    }

    private void resolveInheritance() {

        resolved.clear();

        for (ScoreboardDefinition definition : getAll()) {
            resolved.put(definition.id(), resolve(definition, new HashSet<>()));
        }
    }

    private ScoreboardDefinition resolve(ScoreboardDefinition definition, Set<String> visiting) {

        if (definition.extendsId() == null || definition.extendsId().isBlank() || !visiting.add(definition.id())) {
            return definition;
        }

        ScoreboardDefinition parent = get(definition.extendsId())
                .map(p -> resolve(p, visiting))
                .orElse(null);

        if (parent == null) {
            return definition;
        }

        String title = definition.title() != null ? definition.title() : parent.title();
        String titleAnimationId = definition.titleAnimationId() != null
                ? definition.titleAnimationId() : parent.titleAnimationId();
        List<ScoreboardLine> lines = !definition.lines().isEmpty() ? definition.lines() : parent.lines();

        String finalTitle = applyReplacements(title, definition.replacements());
        List<ScoreboardLine> finalLines = lines.stream()
                .map(line -> new ScoreboardLine(
                        applyReplacements(line.text(), definition.replacements()), line.condition()))
                .toList();

        return new ScoreboardDefinition(definition.id(), finalTitle, titleAnimationId, finalLines, null,
                definition.replacements(), definition.priority());
    }

    private String applyReplacements(String text, Map<String, String> replacements) {

        if (text == null) {
            return null;
        }

        String result = text;

        for (var entry : replacements.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        return result;
    }

    private static RPGRoll resolveCoreInstance() {

        var corePlugin = Bukkit.getPluginManager().getPlugin("RPGRoll");

        if (!(corePlugin instanceof RPGRoll rpgRoll)) {
            throw new IllegalStateException("No se pudo resolver la instancia de RPGRoll.");
        }

        return rpgRoll;
    }

}
