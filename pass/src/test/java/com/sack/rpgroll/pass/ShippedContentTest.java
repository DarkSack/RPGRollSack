package com.sack.rpgroll.pass;

import com.sack.rpgroll.pass.daily.DailyConfig;
import com.sack.rpgroll.pass.mission.Mission;
import com.sack.rpgroll.pass.mission.MissionLoader;
import com.sack.rpgroll.pass.mission.MissionScope;
import com.sack.rpgroll.pass.season.Season;
import com.sack.rpgroll.pass.season.SeasonLoader;
import com.sack.rpgroll.pass.vote.VoteConfig;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Lo que se distribuye en el jar tiene que cargar entero, sin una sola recompensa descartada. */
class ShippedContentTest {

    private final List<String> warnings = new ArrayList<>();

    private YamlConfiguration resource(String path) throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/" + path)) {
            assertNotNull(in, path);
            return YamlConfiguration.loadConfiguration(new InputStreamReader(in, StandardCharsets.UTF_8));
        }
    }

    @Test
    void seasonOneHasThirtyLevelsWithBothTracks() throws Exception {

        Season season = SeasonLoader.parse(resource("seasons/temporada_1.yml"), "temporada_1", warnings::add);

        assertNotNull(season);
        assertEquals(30, season.maxLevel());
        assertTrue(season.levels().values().stream().allMatch(l -> !l.free().isEmpty() && !l.premium().isEmpty()));
        assertEquals(List.of(), warnings);
    }

    @Test
    void missionsCoverEveryScopeAndLoadClean() throws Exception {

        YamlConfiguration yaml = resource("missions.yml");
        List<Mission> missions = MissionLoader.parse(yaml.getConfigurationSection("missions"), warnings::add);

        for (MissionScope scope : MissionScope.values()) {
            long count = missions.stream().filter(m -> m.scope() == scope).count();
            assertTrue(count >= 3, scope + " tiene " + count);
        }
        assertEquals(List.of(), warnings);
    }

    @Test
    void dailyAndVotesLoadClean() throws Exception {

        DailyConfig daily = DailyConfig.parse(resource("daily.yml"), warnings::add);
        VoteConfig votes = VoteConfig.parse(resource("votes.yml"), warnings::add);

        assertEquals(7, daily.days().size());
        assertEquals(3, daily.bonuses().size());
        assertTrue(votes.streaks().containsKey(7));
        assertEquals(List.of(), warnings);
    }

}
