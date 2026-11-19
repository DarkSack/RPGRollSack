package com.sack.rpgroll.crates.treasure;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

/** Los tesoros de plugins/RPGRoll-Crates/treasures/*.yml (los que empiezan por _ no se cargan). */
public class TreasureManager extends ContentManager<Treasure> {

    public TreasureManager(JavaPlugin plugin) {
        super(plugin, new YamlLoader(plugin), "treasures", "tesoro", new TreasureParser());
    }

    @Override
    protected boolean optionalContent() {
        return true;
    }

}
