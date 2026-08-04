package com.sack.rpgroll.ranching.listener;

import com.sack.rpgroll.ranching.core.animal.Animal;
import com.sack.rpgroll.ranching.core.animal.AnimalManager;
import com.sack.rpgroll.ranching.core.breeding.BreedingAttemptResult;
import com.sack.rpgroll.ranching.core.breeding.BreedingEngine;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;

/**
 * Reutiliza el flujo vanilla de "alimentar a dos animales adultos para que
 * entren en modo amor" — Minecraft ya hace todo el trabajo de detectar
 * pareja/comida/cooldown. Acá solo interceptamos el resultado: si AMBOS
 * padres son animales rastreados por Ranching, cancelamos el bebé
 * instantáneo de vanilla y corremos {@link BreedingEngine} en su lugar
 * (que puede arrancar una gestación real en vez de un nacimiento
 * inmediato). Si no son animales rastreados, no tocamos nada — vanilla
 * sigue funcionando normal para mascotas/granjas comunes.
 */
public class BreedingListener implements Listener {

    private final AnimalManager animalManager;
    private final BreedingEngine breedingEngine;

    public BreedingListener(AnimalManager animalManager, BreedingEngine breedingEngine) {
        this.animalManager = animalManager;
        this.breedingEngine = breedingEngine;
    }

    @EventHandler
    public void onBreed(EntityBreedEvent event) {

        if (!(event.getMother() instanceof LivingEntity motherEntity)
                || !(event.getFather() instanceof LivingEntity fatherEntity)) {
            return;
        }

        var first = animalManager.resolve(motherEntity);
        var second = animalManager.resolve(fatherEntity);

        if (first.isEmpty() || second.isEmpty()) {
            return;
        }

        event.setCancelled(true);

        Animal firstAnimal = first.get();
        Animal secondAnimal = second.get();

        BreedingAttemptResult result = breedingEngine.attemptConception(firstAnimal, secondAnimal,
                motherEntity.getLocation());

        if (event.getBreeder() instanceof Player player) {
            player.sendMessage(Component.text(result.message(),
                    result.success() ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
        }
    }

}
