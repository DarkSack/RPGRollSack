package com.sack.rpgroll.structures.module;

import com.sack.rpgroll.structures.api.StructureModule;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Módulos por {@code type}, sin distinguir mayúsculas. Se registran antes de leer el catálogo. */
public final class ModuleRegistry {

    private final Map<String, StructureModule> byType = new ConcurrentHashMap<>();

    /** @throws IllegalStateException si ya hay un módulo con ese tipo */
    public void register(StructureModule module) {
        String type = module.type().toLowerCase(Locale.ROOT);
        if (byType.putIfAbsent(type, module) != null) {
            throw new IllegalStateException("Ya hay un módulo de estructura con el tipo " + type);
        }
    }

    public Optional<StructureModule> get(String type) {
        return type == null ? Optional.empty() : Optional.ofNullable(byType.get(type.toLowerCase(Locale.ROOT)));
    }
}
