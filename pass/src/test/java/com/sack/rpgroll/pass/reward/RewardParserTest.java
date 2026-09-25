package com.sack.rpgroll.pass.reward;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RewardParserTest {

    @Test
    void parsesEveryType() {

        assertEquals(new Reward(Reward.Type.MONEY, "", 500, "money:500"), RewardParser.parse("money:500"));
        assertEquals(new Reward(Reward.Type.KEY, "legendario", 2, "key:legendario:2"),
                RewardParser.parse("key:legendario:2"));
        assertEquals(new Reward(Reward.Type.ITEM, "phoenix_feather", 1, "item:phoenix_feather"),
                RewardParser.parse("item:phoenix_feather"));
        assertEquals(new Reward(Reward.Type.EXP, "", 300, "exp:300"), RewardParser.parse("exp:300"));
    }

    @Test
    void commandsKeepTheirColonsAndDropTheSlash() {

        Reward reward = RewardParser.parse("command:/lp user {player} meta set a:b true");

        assertEquals(Reward.Type.COMMAND, reward.type());
        assertEquals("lp user {player} meta set a:b true", reward.key());
    }

    @Test
    void rejectsUnknownTypesAndBadAmounts() {

        assertThrows(IllegalArgumentException.class, () -> RewardParser.parse("gems:5"));
        assertThrows(IllegalArgumentException.class, () -> RewardParser.parse("money:-5"));
        assertThrows(IllegalArgumentException.class, () -> RewardParser.parse("key::1"));
        assertThrows(IllegalArgumentException.class, () -> RewardParser.parse("500"));
    }

    @Test
    void parseAllSkipsTheBadOnesAndReportsThem() {

        List<String> warnings = new ArrayList<>();
        List<Reward> rewards = RewardParser.parseAll(List.of("money:100", "nope:1", "exp:5"), "test",
                warnings::add);

        assertEquals(2, rewards.size());
        assertEquals(1, warnings.size());
    }

}
