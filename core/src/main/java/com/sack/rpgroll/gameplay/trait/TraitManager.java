package com.sack.rpgroll.gameplay.trait;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.common.yaml.YamlLoader;
import com.sack.rpgroll.common.content.ContentManager;

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
    @Deprecated
    public Optional<Trait> getTrait(String traitId) {
        return get(traitId);
    }

    /** @deprecated usar {@link #getAll()} */
    @Deprecated
    public java.util.Collection<Trait> getAllTraits() {
        return getAll();
    }

    public List<Trait> getTraitsByLevel(int maxLevel) {
        return getAll().stream()
                .filter(t -> t.requiredLevel() <= maxLevel)
                .toList();
    }

}