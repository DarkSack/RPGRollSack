package com.sack.rpgroll.gameplay.trait;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.common.yaml.YamlLoader;
import com.sack.rpgroll.common.content.ContentManager;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * Servicio principal del sistema de traits.
 */
public class TraitManager extends ContentManager<Trait> {

    private final TraitDefinitionWriter writer;

    public TraitManager(RPGRoll plugin, YamlLoader yamlLoader) {
        super(plugin, yamlLoader, "traits", "trait", new TraitParser());
        this.writer = new TraitDefinitionWriter(new File(plugin.getDataFolder(), "traits"), plugin.getLogger());
    }

    /** Persiste el trait a disco y recarga todo el registro para reflejar el cambio de inmediato. */
    public void save(Trait trait) {
        writer.save(trait);
        reload();
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