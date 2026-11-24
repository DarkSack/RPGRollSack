package com.sack.rpgroll.structures.workbench;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.structures.config.WorkbenchSettings.Size;
import com.sack.rpgroll.structures.config.WorkbenchSettings.TierSettings;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class WorkbenchItemsTest {
    private final Plugin plugin = mock(Plugin.class);
    private final WorkbenchItems items;
    private final UUID id = UUID.randomUUID();
    private final SerialSigner signer = new SerialSigner(new byte[32]);
    private final ItemStack stack = mock(ItemStack.class);
    private final ItemMeta meta = mock(ItemMeta.class);
    private final PersistentDataContainer pdc = mock(PersistentDataContainer.class);

    WorkbenchItemsTest() {
        when(plugin.namespace()).thenReturn("rpgroll-structures");
        items = new WorkbenchItems(plugin);
        when(stack.getItemMeta()).thenReturn(meta);
        when(meta.getPersistentDataContainer()).thenReturn(pdc);
    }

    private NamespacedKey key(String k) {
        return new NamespacedKey("rpgroll-structures", k);
    }

    private void fullPdc(String sig) {
        when(pdc.get(key("workbench_id"), PersistentDataType.STRING)).thenReturn(id.toString());
        when(pdc.get(key("tier"), PersistentDataType.INTEGER)).thenReturn(2);
        when(pdc.get(key("serial"), PersistentDataType.LONG)).thenReturn(9L);
        when(pdc.get(key("sig"), PersistentDataType.STRING)).thenReturn(sig);
    }

    @Test void readsAFullStamp() {
        fullPdc("abc");
        assertEquals(Optional.of(new WorkbenchStamp(id, WorkbenchTier.INGENIERO, 9, "abc")), items.read(stack));
    }

    @Test void readIgnoresItemsWithoutAllKeys() {
        fullPdc("abc");
        when(pdc.get(key("sig"), PersistentDataType.STRING)).thenReturn(null);
        assertEquals(Optional.empty(), items.read(stack));
    }

    @Test void readIgnoresInvalidTierOrId() {
        fullPdc("abc");
        when(pdc.get(key("tier"), PersistentDataType.INTEGER)).thenReturn(7);
        assertEquals(Optional.empty(), items.read(stack));
        fullPdc("abc");
        when(pdc.get(key("workbench_id"), PersistentDataType.STRING)).thenReturn("no-uuid");
        assertEquals(Optional.empty(), items.read(stack));
    }

    @Test void readIgnoresKeysStoredWithTheWrongType() {
        fullPdc("abc");
        when(pdc.get(key("tier"), PersistentDataType.INTEGER)).thenThrow(new IllegalArgumentException("wrong type"));
        assertEquals(Optional.empty(), items.read(stack));
    }

    @Test void readIgnoresNullAndMetalessItems() {
        assertEquals(Optional.empty(), items.read(null));
        when(stack.getItemMeta()).thenReturn(null);
        assertEquals(Optional.empty(), items.read(stack));
    }

    @Test void isGenuineChecksTheSignature() {
        String sig = signer.sign(id, WorkbenchTier.INGENIERO, 9);
        assertTrue(items.isGenuine(new WorkbenchStamp(id, WorkbenchTier.INGENIERO, 9, sig), signer));
        assertFalse(items.isGenuine(new WorkbenchStamp(id, WorkbenchTier.ARQUITECTO, 9, sig), signer));
    }

    @Test void applyWritesKeysNameLoreAndModel() {
        LangManager lang = mock(LangManager.class);
        when(lang.component(any(String.class), any(Object[].class))).thenReturn(Component.text("x"));
        TierSettings ts = new TierSettings(WorkbenchTier.INGENIERO, new Size(32, 32, 32), List.of(), 0, "rpgroll:bench2",
                Material.SMITHING_TABLE);
        WorkbenchStamp st = new WorkbenchStamp(id, WorkbenchTier.INGENIERO, 9, "sig");

        items.apply(meta, st, lang, ts, true);

        verify(pdc).set(key("workbench_id"), PersistentDataType.STRING, id.toString());
        verify(pdc).set(key("tier"), PersistentDataType.INTEGER, 2);
        verify(pdc).set(key("serial"), PersistentDataType.LONG, 9L);
        verify(pdc).set(key("sig"), PersistentDataType.STRING, "sig");
        verify(meta).setItemModel(NamespacedKey.fromString("rpgroll:bench2"));
        verify(meta).displayName(any());
        verify(meta).lore(any());
        verify(lang).component("workbench.item.name.ingeniero");
        // Una mesa por ítem: si se apilaran, N copias compartirían id y serie.
        verify(meta).setMaxStackSize(1);
    }

    @Test void applySkipsModelWhenEmpty() {
        LangManager lang = mock(LangManager.class);
        when(lang.component(any(String.class), any(Object[].class))).thenReturn(Component.text("x"));
        TierSettings ts = new TierSettings(WorkbenchTier.CONSTRUCTOR, new Size(16, 16, 16), List.of(), 0, "", Material.CRAFTING_TABLE);
        items.apply(meta, new WorkbenchStamp(id, WorkbenchTier.CONSTRUCTOR, 1, "s"), lang, ts, true);
        verify(meta, never()).setItemModel(any());
    }

    @Test void settingsOfAnotherTierAreRejected() {
        TierSettings constructor = new TierSettings(WorkbenchTier.CONSTRUCTOR, new Size(16, 16, 16), List.of(), 0, "",
                Material.CRAFTING_TABLE);
        WorkbenchStamp arquitecto = new WorkbenchStamp(id, WorkbenchTier.ARQUITECTO, 1, "s");
        assertThrows(IllegalArgumentException.class, () -> items.create(arquitecto, mock(LangManager.class), constructor));
        assertThrows(IllegalArgumentException.class, () -> items.sample(arquitecto, mock(LangManager.class), constructor));
    }

    @Test void recipeSampleHasNoSerialLine() {
        LangManager lang = mock(LangManager.class);
        when(lang.component(any(String.class), any(Object[].class))).thenReturn(Component.text("x"));
        TierSettings ts = new TierSettings(WorkbenchTier.CONSTRUCTOR, new Size(16, 16, 16), List.of(), 0, "",
                Material.CRAFTING_TABLE);
        items.apply(meta, new WorkbenchStamp(new UUID(0, 0), WorkbenchTier.CONSTRUCTOR, 0, "muestra"), lang, ts, false);
        verify(lang).component("workbench.item.lore.sample");
        verify(lang, never()).component(eq("workbench.item.lore.serial"), any(Object[].class));
    }
}
