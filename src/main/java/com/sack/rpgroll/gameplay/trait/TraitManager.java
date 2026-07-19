package com.sack.rpgroll.gameplay.trait;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.config.loader.YamlLoader;
import com.sack.rpgroll.content.ContentManager;

import java.util.List;
import java.util.Optional;

/**
 * Servicio principal del sistema de traits.
 */
public class TraitManager extends ContentManager<Trait> {

    public TraitManager(RPGRoll plugin, YamlLoader yamlLoader) {
        super(plugin, yamlLoader, "traits", "trait", new TraitParser());
    }

    // ============ Compatibilidad con la API anterior (TraitRegistry) ============

    /** @deprecated usar {@link #get(String)} */
    public Optional<Trait> getTrait(String traitId) {
        return get(traitId);
    }

    /** @deprecated usar {@link #getAll()} */
    public java.util.Collection<Trait> getAllTraits() {
        return getAll();
    }

    public List<Trait> getTraitsByLevel(int maxLevel) {
        return getAll().stream()
                .filter(t -> t.requiredLevel() <= maxLevel)
                .toList();
    }

}