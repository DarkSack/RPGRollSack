package com.sack.rpgroll.common.recipe;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;

import java.util.Collection;

/**
 * Recetas que un plugin sabe hacer por su cuenta y que no pasan por el registro de Bukkit
 * (estaciones propias, yunques, aldeanos, carpinteros...). El visor de recetas (RPGRoll-Recipes)
 * lee además todo lo que hay en {@code Bukkit.recipeIterator()}, así que una receta registrada
 * con {@code Bukkit.addRecipe} NO hace falta repetirla acá.
 * <p>
 * Se publica en el {@code ServicesManager} de Bukkit ({@link #register}); puede haber
 * muchas a la vez, una por plugin. Solo depende de RPGRoll-Lib, que es gratis: cualquier
 * plugin de terceros puede implementarla sin comprar nada.
 */
public interface RecipeSource {

    /** Nombre del origen tal como se muestra en el visor (normalmente el del plugin). */
    String name();

    /**
     * Todas las recetas de este origen. Se llama en el hilo principal cada vez que el visor
     * rehace su índice (al arrancar, al recargar y cada cierto tiempo), así que tiene que
     * devolver el estado actual y no tardar.
     */
    Collection<RecipeEntry> recipes();

    static void register(Plugin owner, RecipeSource source) {
        Bukkit.getServicesManager().register(RecipeSource.class, source, owner, ServicePriority.Normal);
    }
}
