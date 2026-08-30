package com.sack.rpgroll.sackresourcepack.manifest;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyResolverTest {

    private final DependencyResolver resolver = new DependencyResolver();
    private final File directory = new File("content");

    private AssetModule module(String id, int priority, List<String> depends, List<String> optional) {
        return new AssetModule(id, id, null, null, null, priority, depends, optional, null, directory);
    }

    private List<String> ids(ResolutionResult result) {
        return result.orderedModules().stream().map(AssetModule::id).toList();
    }

    @Test
    void modulesWithoutDependenciesAreOrderedByPriorityThenId() {
        AssetModule c = module("c", 5, List.of(), List.of());
        AssetModule a = module("a", 1, List.of(), List.of());
        AssetModule b = module("b", 1, List.of(), List.of());

        ResolutionResult result = resolver.resolve(List.of(c, a, b));

        assertEquals(List.of("a", "b", "c"), ids(result));
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void moduleWithDependencyIsOrderedAfterIt() {
        AssetModule base = module("base", 0, List.of(), List.of());
        AssetModule addon = module("addon", 0, List.of("base"), List.of());

        ResolutionResult result = resolver.resolve(List.of(addon, base));

        assertEquals(List.of("base", "addon"), ids(result));
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void moduleDependingOnMissingModuleReportsErrorButStillOrdersTheRest() {
        AssetModule addon = module("addon", 0, List.of("nonexistent"), List.of());

        ResolutionResult result = resolver.resolve(List.of(addon));

        assertEquals(List.of("addon"), ids(result));
        assertEquals(1, result.errors().size());
        assertTrue(result.errors().get(0).contains("nonexistent"));
    }

    @Test
    void optionalDependencyOrdersBeforeWhenPresentButIsIgnoredWhenAbsent() {
        AssetModule base = module("base", 0, List.of(), List.of());
        AssetModule addon = module("addon", 0, List.of(), List.of("base"));

        ResolutionResult result = resolver.resolve(List.of(addon, base));

        assertEquals(List.of("base", "addon"), ids(result));
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void missingOptionalDependencyProducesNoErrorAndIsSimplyIgnored() {
        AssetModule addon = module("addon", 0, List.of(), List.of("nonexistent"));

        ResolutionResult result = resolver.resolve(List.of(addon));

        assertEquals(List.of("addon"), ids(result));
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void circularDependencyIsReportedAndStillProducesABestEffortOrder() {
        AssetModule a = module("a", 0, List.of("b"), List.of());
        AssetModule b = module("b", 0, List.of("a"), List.of());

        ResolutionResult result = resolver.resolve(List.of(a, b));

        assertEquals(2, result.orderedModules().size());
        assertEquals(1, result.errors().size());
        assertTrue(result.errors().get(0).contains("circular") || result.errors().get(0).toLowerCase().contains("circular"));
    }

    @Test
    void diamondDependencyResolvesInATopologicallyValidOrder() {
        AssetModule base = module("base", 0, List.of(), List.of());
        AssetModule left = module("left", 0, List.of("base"), List.of());
        AssetModule right = module("right", 0, List.of("base"), List.of());
        AssetModule top = module("top", 0, List.of("left", "right"), List.of());

        ResolutionResult result = resolver.resolve(List.of(top, right, left, base));
        List<String> order = ids(result);

        assertEquals(0, order.indexOf("base"));
        assertTrue(order.indexOf("left") < order.indexOf("top"));
        assertTrue(order.indexOf("right") < order.indexOf("top"));
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void emptyModuleListResolvesToEmptyResultWithNoErrors() {
        ResolutionResult result = resolver.resolve(List.of());

        assertTrue(result.orderedModules().isEmpty());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void higherPriorityBreaksTiesAmongModulesWithSatisfiedDependencies() {
        AssetModule base = module("base", 0, List.of(), List.of());
        AssetModule high = module("high-priority", 10, List.of("base"), List.of());
        AssetModule low = module("low-priority", 1, List.of("base"), List.of());

        ResolutionResult result = resolver.resolve(List.of(high, low, base));

        assertEquals(List.of("base", "low-priority", "high-priority"), ids(result));
    }
}
