package com.sack.rpgroll.core;

import java.util.HashMap;
import java.util.Map;

public class ServiceRegistry {

    private final Map<Class<?>, Object> services = new HashMap<>();

    public <T> void register(Class<T> type, T service) {
        services.put(type, service);
    }

    public <T> T get(Class<T> type) {

        Object service = services.get(type);

        if (service == null) {
            throw new IllegalStateException(
                    "No se encontró el servicio: " + type.getSimpleName());
        }

        return type.cast(service);

    }

    public boolean contains(Class<?> type) {
        return services.containsKey(type);
    }

}