package com.sack.rpgroll.ranching.core.animal;

import com.sack.rpgroll.ranching.core.genetics.AllelePair;
import com.sack.rpgroll.ranching.core.genetics.AncestorRef;
import com.sack.rpgroll.ranching.core.genetics.BreedingOutcome;
import com.sack.rpgroll.ranching.core.genetics.GeneMutation;
import com.sack.rpgroll.ranching.core.genetics.MutationEffectType;
import com.sack.rpgroll.ranching.core.species.GrowthStage;
import com.sack.rpgroll.ranching.core.species.Sex;

import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnimalStoreTest {

    @TempDir
    File dataFolder;

    private AnimalStore store() {
        Plugin plugin = mock(Plugin.class);
        when(plugin.getDataFolder()).thenReturn(dataFolder);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        return new AnimalStore(plugin);
    }

    private static Animal cow(UUID id) {
        Animal animal = new Animal(id, "cow", "holstein", Sex.FEMALE,
                Map.of("milk_production", new AllelePair(0.7, 0.4)), Map.of("milk_production", 0.7), List.of(),
                null, null, List.of(), 0, 600, 1000L);
        animal.setStage(GrowthStage.ADULT);
        return animal;
    }

    @Test
    void aPregnancySurvivesARestartWithItsWholeLitter() {
        UUID motherId = UUID.randomUUID();
        UUID fatherId = UUID.randomUUID();
        Animal mother = cow(motherId);

        BreedingOutcome outcome = new BreedingOutcome(Map.of("milk_production", new AllelePair(0.9, 0.2)),
                Map.of("milk_production", 1.8),
                List.of(new GeneMutation("golden_coat", "Pelaje dorado", MutationEffectType.COSMETIC_TAG, 0, 0.01)));
        PendingOffspring calf = new PendingOffspring(outcome, Sex.MALE, "jersey", 1,
                List.of(new AncestorRef(fatherId, "Toro", "cow")), motherId, fatherId);
        mother.startPregnancy(7000, List.of(calf));

        store().save(mother);
        List<Animal> loaded = store().loadAll();

        assertEquals(1, loaded.size());
        Animal back = loaded.get(0);
        assertTrue(back.isPregnant());
        assertEquals(7000, back.pregnancyRemainingTicks());

        PendingOffspring restored = back.pendingLitter().get(0);
        assertEquals(Sex.MALE, restored.sex());
        assertEquals("jersey", restored.breedId());
        assertEquals(1, restored.generation());
        assertEquals(motherId, restored.motherId());
        assertEquals(fatherId, restored.fatherId());
        assertEquals(new AllelePair(0.9, 0.2), restored.outcome().genotype().get("milk_production"));
        assertEquals(1.8, restored.outcome().phenotype().get("milk_production"));
        assertEquals("golden_coat", restored.outcome().triggeredMutations().get(0).id());
        assertEquals(MutationEffectType.COSMETIC_TAG, restored.outcome().triggeredMutations().get(0).effectType());
        assertEquals(fatherId, restored.ancestry().get(0).id());
    }

    @Test
    void aNameSurvivesARestartWithoutColorCodesAndWithinTheLimit() {
        Animal named = cow(UUID.randomUUID());
        named.setName("  &4Lola §lla vaca más larga del rancho entero  ");

        store().save(named);
        String back = store().loadAll().get(0).name();

        assertEquals("Lola la vaca más larga d", back);
        assertEquals(Animal.MAX_NAME_LENGTH, back.length());

        named.setName(" ");
        assertEquals(null, named.name());
    }

    @Test
    void anAnimalThatIsNotPregnantStaysThatWay() {
        store().save(cow(UUID.randomUUID()));

        Animal back = store().loadAll().get(0);

        assertFalse(back.isPregnant());
        assertTrue(back.pendingLitter().isEmpty());
    }

}
