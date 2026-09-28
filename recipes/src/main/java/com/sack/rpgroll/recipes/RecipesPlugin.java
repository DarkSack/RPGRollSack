package com.sack.rpgroll.recipes;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.common.resource.DirectoryCreator;
import com.sack.rpgroll.common.resource.ResourceCopier;
import com.sack.rpgroll.license.identity.LicenseIdentity;
import com.sack.rpgroll.licensing.LicenseGate;
import com.sack.rpgroll.recipes.book.RecipeBook;
import com.sack.rpgroll.recipes.command.RecipesCommand;
import com.sack.rpgroll.recipes.gui.SearchPrompt;
import com.sack.rpgroll.recipes.gui.Viewer;
import com.sack.rpgroll.recipes.index.IndexService;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * RPGRoll-Recipes: el recetario al estilo JEI. Lee el registro de recetas de Bukkit (vanilla y
 * de cualquier plugin), la fermentación, los {@code RecipeSource} que publiquen otros plugins
 * (RPGRoll-Crafting, RPGRoll-Furniture...) y las recetas escritas a mano en {@code extra/}.
 */
public class RecipesPlugin extends JavaPlugin {

    private static final List<String> DIRECTORIES = List.of("extra");

    private LangManager lang;
    private IndexService indexes;
    private RecipeBook book;

    @Override
    public void onEnable() {
        if (!LicenseGate.verify(this, LicenseIdentity.RESOURCE_ID, LicenseIdentity.PRODUCT_SLUG,
                LicenseIdentity.VERIFY_TOKEN)) {
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        new DirectoryCreator(this).create(DIRECTORIES);
        new ResourceCopier(this).copyDirectories(DIRECTORIES);

        lang = new LangManager(this, List.of("es", "en", "pt_BR"), "es");
        lang.reload(getConfig().getString("language", "es"));

        Settings settings = Settings.from(getConfig());
        indexes = new IndexService(this, lang, settings);
        SearchPrompt prompt = new SearchPrompt(this, lang);
        Viewer viewer = new Viewer(this, lang, indexes, prompt);
        book = new RecipeBook(this, lang, viewer, settings.book());
        book.registerRecipe();

        var events = getServer().getPluginManager();
        events.registerEvents(indexes, this);
        events.registerEvents(prompt, this);
        events.registerEvents(viewer, this);
        events.registerEvents(book, this);

        PluginCommand command = getCommand("recetas");
        if (command != null) {
            RecipesCommand executor = new RecipesCommand(this, lang, viewer, indexes, book);
            command.setExecutor(executor);
            command.setTabCompleter(executor);
        }
    }

    @Override
    public void onDisable() {
        if (book != null) {
            book.unregisterRecipe();
        }
    }

    /** /recetas recargar: config, idioma, libro e índice. */
    public void reload() {
        reloadConfig();
        lang.reload(getConfig().getString("language", "es"));
        Settings settings = Settings.from(getConfig());
        book.settings(settings.book());
        indexes.settings(settings);
        indexes.rebuild();
    }
}
