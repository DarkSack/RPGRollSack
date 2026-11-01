package com.sack.rpgroll.items.core;

import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class ItemModelOverridesTest {
    @Test
    void skinToVanillaBaseClearsBothModelOverrides() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString("id: sword\nmaterial: IRON_SWORD\n");
        ItemDefinition definition = new ItemParser().parse(yaml);
        ItemMeta meta = mock(ItemMeta.class);
        ItemFactory.applyModelOverrides(meta, definition, new ItemSkin("skin", "Skin", null, 42, "demo:skin"));
        verify(meta).setItemModel(NamespacedKey.fromString("demo:skin"));
        verify(meta).setCustomModelData(42);
        clearInvocations(meta);
        ItemFactory.applyModelOverrides(meta, definition, null);
        verify(meta).setCustomModelData(null);
        verify(meta).setItemModel(null);
    }

    @Test
    void skinWithoutAnOverrideRestoresTheConfiguredBase() throws Exception {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.loadFromString("id: sword\nmaterial: IRON_SWORD\nitem-model: demo:base\ncustom-model-data: 7\n");
        ItemMeta meta = mock(ItemMeta.class);
        ItemFactory.applyModelOverrides(meta, new ItemParser().parse(yaml), new ItemSkin("skin", "Skin", null, null));
        verify(meta).setCustomModelData(7);
        verify(meta).setItemModel(NamespacedKey.fromString("demo:base"));
    }
}
