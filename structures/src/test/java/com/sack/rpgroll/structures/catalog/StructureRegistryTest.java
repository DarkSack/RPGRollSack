package com.sack.rpgroll.structures.catalog;

import com.sack.rpgroll.structures.workbench.WorkbenchTier;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StructureRegistryTest {

    private static StructureDefinition def(String id, String es, WorkbenchTier tier) {
        return new StructureDefinition(id, es == null ? Map.of() : Map.of("es", es, "en", es + " EN"), "casas", tier,
                "", null, null, List.of(), 0, false, BuildOrder.BOTTOM_UP, List.of(), 16, 20,
                new PlacementRules(false, false, List.of(), List.of(), java.util.Set.of()), "p." + id);
    }

    @Test
    void nameFallsBackToSpanishThenId() {
        var d = def("a", "Casa", WorkbenchTier.CONSTRUCTOR);
        assertEquals("Casa EN", d.name("en"));
        assertEquals("Casa", d.name("pt_BR"));
        assertEquals("x", def("x", null, WorkbenchTier.CONSTRUCTOR).name("es"));
    }

    @Test
    void registryOrdersByTierThenName() {
        var reg = new StructureRegistry();
        var p = new Problem("f.yml", "k", "r");
        reg.replace(new StructureLoader.LoadResult(List.of(def("z", "Zeta", WorkbenchTier.CONSTRUCTOR),
                def("b", "Beta", WorkbenchTier.INGENIERO), def("a", "alfa", WorkbenchTier.CONSTRUCTOR)), List.of(p)));
        assertEquals(List.of("a", "z", "b"), reg.inCategory("casas").stream().map(StructureDefinition::id).toList());
        assertTrue(reg.inCategory("nada").isEmpty());
        assertTrue(reg.get("b").isPresent());
        assertTrue(reg.get("q").isEmpty());
        assertEquals(List.of(p), reg.problems());
        reg.replace(new StructureLoader.LoadResult(List.of(), List.of()));
        assertTrue(reg.get("b").isEmpty());
    }

    private static StructureDefinition upgrade(String id, String es, WorkbenchTier tier, String from) {
        StructureDefinition d = def(id, es, tier);
        return new StructureDefinition(d.id(), d.names(), d.category(), d.tier(), d.icon(), d.schematic(), d.blocks(),
                d.materials(), d.money(), d.instant(), d.order(), d.customOrder(), d.blocksPerCycle(),
                d.ticksPerCycle(), d.placement(), d.permission(), null, null, null, from);
    }

    @Test
    void upgradesOfListsOnlyThoseThatDeclareIt() {
        var reg = new StructureRegistry();
        reg.replace(new StructureLoader.LoadResult(List.of(def("casa", "Casa", WorkbenchTier.CONSTRUCTOR),
                upgrade("mansion", "Mansión", WorkbenchTier.ARQUITECTO, "casa"),
                upgrade("casa-piedra", "Casa de piedra", WorkbenchTier.CONSTRUCTOR, "casa"),
                upgrade("torre", "Torre", WorkbenchTier.CONSTRUCTOR, "otra")), List.of()));
        assertEquals(List.of("casa-piedra", "mansion"),
                reg.upgradesOf("casa").stream().map(StructureDefinition::id).toList());
        assertTrue(reg.upgradesOf("mansion").isEmpty());
    }
}
