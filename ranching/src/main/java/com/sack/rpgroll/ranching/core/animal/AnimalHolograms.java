package com.sack.rpgroll.ranching.core.animal;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.ranching.core.ownership.OwnershipService;

import net.kyori.adventure.text.Component;

import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Display;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;

import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Cartel flotante sobre cada animal del rancho: qué es (o su nombre), vida, calidad, etapa y
 * bienestar. Solo lo llevan los animales del rancho, así se distinguen de los vanilla.
 *
 * <p>El cartel se mueve a mano en vez de ir montado sobre el animal: con un modelo de
 * FreeMinecraftModels el cliente no ve al animal vanilla y un pasajero se queda a sus pies.
 */
public class AnimalHolograms implements Runnable {

    private static final int TEXT_EVERY_RUNS = 10;
    private static final float SCALE = 0.6f;

    private final AnimalManager animalManager;
    private final OwnershipService ownership;
    private final LangManager lang;
    private final Map<UUID, TextDisplay> displays = new HashMap<>();
    private int runs;

    public AnimalHolograms(AnimalManager animalManager, OwnershipService ownership, LangManager lang) {
        this.animalManager = animalManager;
        this.ownership = ownership;
        this.lang = lang;
    }

    // shortcut: recorre todos los animales en cada pasada, pasar a un índice por chunk si un servidor supera los miles.
    @Override
    public void run() {

        boolean refreshText = runs++ % TEXT_EVERY_RUNS == 0;
        Set<UUID> loaded = new HashSet<>();

        for (Animal animal : animalManager.getAll()) {

            if (!(animalManager.entityOf(animal).orElse(null) instanceof LivingEntity entity)) {
                continue;
            }

            loaded.add(animal.id());

            Location above = entity.getLocation().add(0, entity.getHeight() + 0.35, 0);
            above.setYaw(0);
            above.setPitch(0);

            TextDisplay display = displays.get(animal.id());

            if (display == null || !display.isValid() || !display.getWorld().equals(above.getWorld())) {
                if (display != null) {
                    display.remove();
                }
                displays.put(animal.id(), spawn(above, text(animal, entity)));
                continue;
            }

            if (display.getLocation().distanceSquared(above) > 0.0001) {
                display.teleport(above);
            }

            if (refreshText) {
                display.text(text(animal, entity));
            }
        }

        displays.entrySet().removeIf(entry -> {
            if (loaded.contains(entry.getKey())) {
                return false;
            }
            entry.getValue().remove();
            return true;
        });
    }

    /** Quita todos los carteles (al apagar el plugin). */
    public void clear() {
        displays.values().forEach(TextDisplay::remove);
        displays.clear();
    }

    private static TextDisplay spawn(Location above, Component text) {
        return above.getWorld().spawn(above, TextDisplay.class, display -> {
            // No se guarda con el chunk: se vuelve a crear al cargarse el animal, sin dejar carteles huérfanos.
            display.setPersistent(false);
            display.setBillboard(Display.Billboard.CENTER);
            display.setTeleportDuration(2);
            display.setViewRange(0.3f);
            display.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(SCALE),
                    new AxisAngle4f()));
            display.text(text);
        });
    }

    private Component text(Animal animal, LivingEntity entity) {

        AttributeInstance maxHealth = entity.getAttribute(Attribute.MAX_HEALTH);

        return lang.component("hologram.text",
                "name", ownership.title(animal),
                "sex", lang.raw("hologram.sex." + animal.sex().name()),
                "hp", Math.round(entity.getHealth()),
                "max_hp", Math.round(maxHealth != null ? maxHealth.getValue() : entity.getHealth()),
                "quality", lang.raw("hologram.quality." + animal.quality().name()),
                "stage", lang.raw("hologram.stage." + animal.stage().name()),
                "health", Math.round(animal.health()),
                "happiness", Math.round(animal.happiness()));
    }

}
