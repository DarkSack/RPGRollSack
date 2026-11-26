package com.sack.rpgroll.structures.material;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ItemIdentityTest {
    private final ItemStack stack = mock(ItemStack.class);
    private final ItemMeta meta = mock(ItemMeta.class);
    private final PersistentDataContainer pdc = mock(PersistentDataContainer.class);

    ItemIdentityTest() {
        when(stack.getItemMeta()).thenReturn(meta);
        when(meta.getPersistentDataContainer()).thenReturn(pdc);
    }

    @Test void pdcIdIsPrefixedWithItsKey() {
        when(pdc.get(new NamespacedKey("rpgroll-furniture", "id"), PersistentDataType.STRING)).thenReturn("silla");
        assertEquals(Optional.of("rpgroll-furniture:id=silla"), ItemIdentity.of(stack));
    }

    @Test void firstKeyWins() {
        when(pdc.get(new NamespacedKey("rpgroll-items", "item-id"), PersistentDataType.STRING)).thenReturn("espada");
        when(pdc.get(new NamespacedKey("rpgroll-furniture", "id"), PersistentDataType.STRING)).thenReturn("silla");
        assertEquals(Optional.of("rpgroll-items:item-id=espada"), ItemIdentity.of(stack));
    }

    @Test void modelIsFallbackIdentity() {
        when(meta.hasItemModel()).thenReturn(true);
        when(meta.getItemModel()).thenReturn(new NamespacedKey("sack", "lingote"));
        assertEquals(Optional.of("model=sack:lingote"), ItemIdentity.of(stack));
    }

    @Test void noIdentityIsEmpty() {
        assertEquals(Optional.empty(), ItemIdentity.of(stack));
        assertEquals(Optional.empty(), ItemIdentity.of(null));
    }
}
