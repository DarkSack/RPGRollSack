package com.sack.rpgroll.ranching.core.genetics;

import com.sack.rpgroll.common.content.ContentParser;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class GeneParser implements ContentParser<Gene> {

    @Override
    public Gene parse(YamlConfiguration config) throws Exception {

        String id = config.getString("id");
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("archivo sin campo obligatorio 'id'");
        }

        Set<String> applicableSpecies = new HashSet<>();
        for (String speciesId : config.getStringList("applicable-species")) {
            applicableSpecies.add(speciesId.trim().toLowerCase(Locale.ROOT));
        }

        return new Gene(
                id,
                config.getString("display-name", id),
                config.getString("description", ""),
                config.getString("attribute-key", id),
                parseDominance(config.getString("dominance")),
                config.getDouble("min-value", 0),
                config.getDouble("max-value", 100),
                applicableSpecies,
                parseMutations(config));
    }

    private GeneDominance parseDominance(String raw) {

        if (raw == null || raw.isBlank()) {
            return GeneDominance.MIXED;
        }

        try {
            return GeneDominance.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return GeneDominance.MIXED;
        }
    }

    private List<GeneMutation> parseMutations(YamlConfiguration config) {

        List<GeneMutation> mutations = new ArrayList<>();
        List<?> rawList = config.getList("mutations");

        if (rawList == null) {
            return mutations;
        }

        for (Object rawEntry : rawList) {

            if (!(rawEntry instanceof java.util.Map<?, ?> rawMap)) {
                continue;
            }

            String mutationId = String.valueOf(rawMap.get("id"));
            String displayName = rawMap.get("display-name") != null ? String.valueOf(rawMap.get("display-name"))
                    : mutationId;

            Object rawEffectType = rawMap.get("effect-type");
            MutationEffectType effectType;
            try {
                effectType = MutationEffectType.valueOf(
                        (rawEffectType != null ? String.valueOf(rawEffectType) : "COSMETIC_TAG")
                                .toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                effectType = MutationEffectType.COSMETIC_TAG;
            }

            double effectValue = rawMap.get("effect-value") instanceof Number number ? number.doubleValue() : 0;
            double chance = rawMap.get("chance") instanceof Number number ? number.doubleValue() : 0;

            mutations.add(new GeneMutation(mutationId, displayName, effectType, effectValue, chance));
        }

        return mutations;
    }

}
