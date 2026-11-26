package com.sack.rpgroll.structures.material;

import com.sack.rpgroll.common.quick.Things;
import com.sack.rpgroll.structures.api.MaterialProvider;
import com.sack.rpgroll.structures.catalog.MaterialSpec;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MaterialRegistryTest {
    private final ItemStack plain = mock(ItemStack.class);
    private final List<Things.Give> gives = new ArrayList<>();
    // Material es un enum que no carga sin servidor: el proveedor minecraft de las pruebas usa solo
    // VanillaProvider.isPlain y el "material" es un texto guardado en la pila.
    private final Map<ItemStack, String> materialOf = new HashMap<>();
    private final MaterialRegistry registry = new MaterialRegistry(() -> gives, new MaterialProvider() {
        public String type() { return "minecraft"; }
        public boolean knows(String id) { return true; }
        public boolean matches(String id, ItemStack stack) {
            return id.equals(materialOf.get(stack)) && VanillaProvider.isPlain(stack, plain);
        }
        public Optional<ItemStack> sample(String id) { return Optional.empty(); }
    });

    private ItemStack vanilla(String m, boolean similar) {
        ItemStack s = mock(ItemStack.class);
        ItemStack one = mock(ItemStack.class);
        when(s.asOne()).thenReturn(one);
        when(one.isSimilar(plain)).thenReturn(similar);
        materialOf.put(s, m);
        return s;
    }

    private ItemStack withPdc(String id) {
        ItemStack s = mock(ItemStack.class);
        ItemMeta meta = mock(ItemMeta.class);
        PersistentDataContainer pdc = mock(PersistentDataContainer.class);
        when(s.getItemMeta()).thenReturn(meta);
        when(meta.getPersistentDataContainer()).thenReturn(pdc);
        when(pdc.get(new NamespacedKey("rpgroll-items", "item-id"), PersistentDataType.STRING)).thenReturn(id);
        return s;
    }

    private ItemStack modelItem(String model) {
        ItemStack s = mock(ItemStack.class);
        ItemMeta meta = mock(ItemMeta.class);
        when(s.getItemMeta()).thenReturn(meta);
        when(meta.getPersistentDataContainer()).thenReturn(mock(PersistentDataContainer.class));
        when(meta.hasItemModel()).thenReturn(true);
        when(meta.getItemModel()).thenReturn(NamespacedKey.fromString(model));
        return s;
    }

    private Things.Give give(String type, String id, ItemStack sample) {
        return new Things.Give() {
            public String type() { return type; }
            public Collection<String> ids() { return List.of(id); }
            public Optional<ItemStack> create(String i) { return Optional.of(sample); }
        };
    }

    private MaterialProvider provider(String type, boolean knows) {
        return new MaterialProvider() {
            public String type() { return type; }
            public boolean knows(String id) { return knows; }
            public boolean matches(String id, ItemStack stack) { return true; }
            public Optional<ItemStack> sample(String id) { return Optional.empty(); }
        };
    }

    @Test void plainVanillaMatchesRenamedDoesNot() {
        assertTrue(registry.matches("minecraft:oak_planks", vanilla("oak_planks", true)));
        assertFalse(registry.matches("minecraft:oak_planks", vanilla("oak_planks", false)));
    }

    @Test void rpgrollItemOfSameMaterialIsNotVanilla() {
        assertFalse(registry.matches("minecraft:oak_planks", vanilla("oak_planks", false)));
        assertFalse(registry.matches("minecraft:oak_planks", vanilla("stone", true)));
    }

    @Test void thingsMatchesByPdcIdNotByName() {
        gives.add(give("item", "espada", withPdc("espada")));
        assertTrue(registry.knows("item:espada"));
        assertTrue(registry.matches("item:espada", withPdc("espada")));
        assertFalse(registry.matches("item:espada", withPdc("hacha")));
        assertFalse(registry.matches("item:espada", vanilla("iron_sword", true)));
    }

    @Test void modelIsFallbackIdentity() {
        gives.add(give("item", "lingote", modelItem("sack:lingote")));
        assertTrue(registry.matches("item:lingote", modelItem("sack:lingote")));
        assertFalse(registry.matches("item:lingote", modelItem("sack:otro")));
    }

    @Test void normalizeAddsMinecraft() {
        assertEquals("minecraft:oak_planks", registry.normalize("oak_planks"));
        assertEquals("minecraft:oak_planks", registry.normalize(" Oak_Planks "));
        assertEquals("item:espada", registry.normalize("Item:Espada"));
    }

    @Test void providersAreLookedUpEveryTime() {
        assertFalse(registry.knows("item:espada"));
        gives.add(give("item", "espada", withPdc("espada")));
        assertTrue(registry.knows("item:espada"));
    }

    @Test void lastRegisteredWinsForSameType() {
        registry.register(provider("x", false));
        registry.register(provider("x", true));
        assertTrue(registry.knows("x:a"));
    }

    @Test void countSumsAnyOf() {
        ItemStack a = vanilla("oak_planks", true);
        ItemStack b = vanilla("birch_planks", true);
        ItemStack c = vanilla("stone", true);
        when(a.getAmount()).thenReturn(10);
        when(b.getAmount()).thenReturn(5);
        when(c.getAmount()).thenReturn(7);
        Inventory inv = mock(Inventory.class);
        when(inv.getContents()).thenReturn(new ItemStack[] {a, null, b, c});
        MaterialSpec spec = new MaterialSpec(List.of("minecraft:oak_planks", "minecraft:birch_planks"), 20, false);
        assertEquals(15, registry.count(inv, spec));
    }

    /** Give que cuenta cuántas veces crea su ítem de muestra. */
    private Things.Give counting(String type, String id, ItemStack sample, int[] creates) {
        return new Things.Give() {
            public String type() { return type; }
            public Collection<String> ids() { return List.of(id); }
            public Optional<ItemStack> create(String i) { creates[0]++; return Optional.of(sample); }
        };
    }

    @Test void thingsSampleIdentityIsCachedPerId() {
        int[] creates = {0};
        gives.add(counting("item", "espada", withPdc("espada"), creates));
        for (int i = 0; i < 5; i++) {
            assertTrue(registry.matches("item:espada", withPdc("espada")));
            assertFalse(registry.matches("item:espada", withPdc("hacha")));
        }
        assertEquals(1, creates[0], "una muestra por id, no una por comparación");
        registry.clearCache(); // al recargar
        assertTrue(registry.matches("item:espada", withPdc("espada")));
        assertEquals(2, creates[0]);
    }

    @Test void thingsIdentityCacheExpires() {
        int[] creates = {0};
        long[] now = {0};
        ThingsProvider p = new ThingsProvider(counting("item", "espada", withPdc("espada"), creates), () -> now[0]);
        assertTrue(p.matches("espada", withPdc("espada")));
        now[0] += ThingsProvider.IDENTITY_TTL_MS - 1;
        assertTrue(p.matches("espada", withPdc("espada")));
        assertEquals(1, creates[0]);
        now[0] += 1;
        assertTrue(p.matches("espada", withPdc("espada")));
        assertEquals(2, creates[0], "pasado el plazo se vuelve a pedir");
    }
}
