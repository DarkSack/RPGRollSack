package com.sack.rpgroll.gameplay.enchant;

import com.sack.rpgroll.RPGRoll;
import com.sack.rpgroll.config.loader.YamlLoader;
import com.sack.rpgroll.content.ContentManager;

public class EnchantManager extends ContentManager<CustomEnchantment> {

    public EnchantManager(RPGRoll plugin, YamlLoader yamlLoader) {
        super(plugin, yamlLoader, "enchantments", "encantamiento", new EnchantParser(plugin));
    }

}