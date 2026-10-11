package com.sack.rpgroll.sackresourcepack.merge;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MergeEngineTest {

    private static JsonObject json(String text) {
        return JsonParser.parseString(text.replace('\'', '"')).getAsJsonObject();
    }

    private static String model(JsonObject blockstate, String state) {
        return blockstate.getAsJsonObject("variants").getAsJsonObject(state).get("model").getAsString();
    }

    /** Menas en un pack y lucky blocks en otro: cada uno conserva sus estados, en cualquier orden. */
    @Test
    void customModelsOfBothModulesSurviveWhateverTheOrder() {
        String ores = "{'variants':{'note=0':{'model':'a:ore'},'note=1':{'model':'minecraft:block/note_block'}}}";
        String lucky = "{'variants':{'note=0':{'model':'minecraft:block/note_block'},'note=1':{'model':'b:lucky'}}}";

        for (String[] order : new String[][] {{ores, lucky}, {lucky, ores}}) {
            JsonObject merged = json(order[0]);
            assertTrue(MergeEngine.mergeBlockstateVariants(merged, json(order[1])));
            assertEquals("a:ore", model(merged, "note=0"));
            assertEquals("b:lucky", model(merged, "note=1"));
        }
    }

    @Test
    void laterCustomModelWinsAndNewStatesAreAdded() {
        JsonObject merged = json("{'variants':{'note=0':{'model':'a:ore'}}}");

        assertTrue(MergeEngine.mergeBlockstateVariants(merged,
                json("{'variants':{'note=0':{'model':'b:other'},'note=2':{'model':'block/note_block'}}}")));

        assertEquals("b:other", model(merged, "note=0"));
        assertEquals("block/note_block", model(merged, "note=2"));
    }

    @Test
    void multipartIsNotMerged() {
        assertFalse(MergeEngine.mergeBlockstateVariants(json("{'multipart':[]}"), json("{'variants':{}}")));
    }

}
