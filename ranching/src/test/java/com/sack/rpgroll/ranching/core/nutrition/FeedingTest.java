package com.sack.rpgroll.ranching.core.nutrition;

import com.sack.rpgroll.ranching.core.animal.Animal;
import com.sack.rpgroll.ranching.core.species.Sex;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FeedingTest {

    private static Animal cow() {
        return new Animal(UUID.randomUUID(), "cow", null, Sex.FEMALE, Map.of(), Map.of(), List.of(), null, null,
                List.of(), 0, 600, 0L);
    }

    private static final Feed GOLDEN = new Feed("golden_feed", null, null, null, FeedQuality.LEGENDARY, Set.of(),
            50, 10, 15, 20);

    @Test
    void aFullAnimalDoesNotEat() {
        Animal cow = cow();

        assertEquals(Feeding.Result.FED, Feeding.feed(cow, GOLDEN, null));
        assertEquals(Feeding.Result.FED, Feeding.feed(cow, GOLDEN, null));
        assertTrue(cow.isFull());
        assertEquals(Feeding.Result.FULL, Feeding.feed(cow, GOLDEN, null));
    }

    @Test
    void digestingMakesRoomAgain() {
        Animal cow = cow();
        cow.eat(Animal.MAX_SATIETY);

        cow.digest(30);

        assertEquals(Feeding.Result.FED, Feeding.feed(cow, GOLDEN, null));
    }

    @Test
    void theProductionBonusFromFoodIsCapped() {
        Animal cow = cow();

        for (int i = 0; i < 64; i++) {
            cow.setSatiety(0);
            Feeding.feed(cow, GOLDEN, null);
        }

        assertEquals(Animal.MAX_PRODUCTION_BONUS, cow.consumeProductionBonus());
    }

}
