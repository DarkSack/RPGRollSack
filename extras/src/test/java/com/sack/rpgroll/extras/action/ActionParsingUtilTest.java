package com.sack.rpgroll.extras.action;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionParsingUtilTest {

    @Test
    void parsesValidEntriesLowercaseOrUppercaseType() {
        List<Map<?, ?>> raw = List.of(
                entry("MESSAGE", "hello"),
                entry("sound", "ENTITY_TNT_PRIMED"));

        List<ExtrasAction> actions = ActionParsingUtil.parseActions(raw);

        assertEquals(2, actions.size());
        assertEquals(ExtrasActionType.MESSAGE, actions.get(0).type());
        assertEquals("hello", actions.get(0).value());
        assertEquals(ExtrasActionType.SOUND, actions.get(1).type());
    }

    @Test
    void skipsEntriesMissingTypeOrValue() {
        List<Map<?, ?>> raw = new ArrayList<>();
        raw.add(entry(null, "hello"));
        raw.add(entryTypeOnly("MESSAGE"));

        assertTrue(ActionParsingUtil.parseActions(raw).isEmpty());
    }

    @Test
    void skipsEntriesWithUnknownActionType() {
        List<Map<?, ?>> raw = List.of(entry("NOT_A_REAL_TYPE", "value"));

        assertTrue(ActionParsingUtil.parseActions(raw).isEmpty());
    }

    @Test
    void parsesMixOfValidAndInvalidEntriesKeepingOnlyValid() {
        List<Map<?, ?>> raw = List.of(
                entry("DAMAGE", "4"),
                entry("BOGUS", "x"),
                entry("COMMAND", "say hi"));

        List<ExtrasAction> actions = ActionParsingUtil.parseActions(raw);

        assertEquals(2, actions.size());
        assertEquals(ExtrasActionType.DAMAGE, actions.get(0).type());
        assertEquals(ExtrasActionType.COMMAND, actions.get(1).type());
    }

    private Map<?, ?> entry(String type, String value) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (type != null) {
            map.put("type", type);
        }
        map.put("value", value);
        return map;
    }

    private Map<?, ?> entryTypeOnly(String type) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", type);
        return map;
    }
}
