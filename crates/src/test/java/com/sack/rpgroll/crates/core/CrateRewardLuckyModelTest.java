package com.sack.rpgroll.crates.core;

import com.sack.rpgroll.crates.core.CrateAction.CrateActionType;
import com.sack.rpgroll.crates.lucky.LuckyBlock;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** El icono de un premio que da lucky blocks usa el modelo del lucky block (si no, sale el material plano). */
class CrateRewardLuckyModelTest {

    private static final Map<String, LuckyBlock> BLOCKS = Map.of(
            "comun", lucky("comun", "sackito:lucky_comun"),
            "sin_modelo", lucky("sin_modelo", null));

    @Test
    void usesTheModelOfTheFirstLuckyBlockAction() {
        CrateReward reward = reward(new CrateAction(CrateActionType.MESSAGE, "hola"),
                new CrateAction(CrateActionType.LUCKY_BLOCK, "Comun, 2"));

        assertEquals(NamespacedKey.fromString("sackito:lucky_comun"), model(reward));
    }

    @Test
    void noModelWithoutLuckyBlockUnknownBlockOrBlockWithoutModel() {
        assertNull(model(reward(new CrateAction(CrateActionType.MESSAGE, "hola"))));
        assertNull(model(reward(new CrateAction(CrateActionType.LUCKY_BLOCK, "nada,1"))));
        assertNull(model(reward(new CrateAction(CrateActionType.LUCKY_BLOCK, "sin_modelo"))));
    }

    private static NamespacedKey model(CrateReward reward) {
        return CrateActionExecutor.luckyModel(reward, id -> Optional.ofNullable(BLOCKS.get(id)));
    }

    private static CrateReward reward(CrateAction... actions) {
        return new CrateReward("premio", "Premio", Material.NOTE_BLOCK, List.of(), 1, false, List.of(actions));
    }

    private static LuckyBlock lucky(String id, String model) {
        return new LuckyBlock(id, id, List.of(), 0, "PAPER", model, false, List.of());
    }
}
