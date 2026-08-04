package com.sack.rpgroll.quests.registry;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Tipos de objetivo conocidos por el motor. Los tipos base (KILL_ENTITY,
 * BREAK_BLOCK, etc.) progresan vía listeners internos que llaman a
 * {@code QuestEngine#progressObjective(...)}; un addon que quiera un tipo
 * nuevo (ej. "FISH_CAUGHT") solo necesita registrar el nombre acá — para
 * que el parser no lo descarte con un warning — y llamar él mismo a
 * {@code progressObjective(...)} desde su propio listener cuando corresponda.
 * No hay una interfaz de "handler" que implementar: registrar el tipo es
 * la única API de extensión que este registro exige.
 */
public class ObjectiveTypeRegistry {

    private final Set<String> types = new HashSet<>();

    public ObjectiveTypeRegistry() {
        registerBuiltins();
    }

    private void registerBuiltins() {
        register("TALK_TO_NPC");
        register("KILL_ENTITY");
        register("BREAK_BLOCK");
        register("PLACE_BLOCK");
        register("COLLECT_ITEM");
        register("DELIVER_ITEM");
        register("REACH_LOCATION");
        register("DISCOVER_REGION");
        register("WAIT");
        register("COMMAND");
    }

    public void register(String type) {
        types.add(type.trim().toUpperCase(Locale.ROOT));
    }

    public boolean isKnown(String type) {
        return types.contains(type.trim().toUpperCase(Locale.ROOT));
    }

}
