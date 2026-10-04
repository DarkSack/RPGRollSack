package com.sack.rpgroll.crates.lucky;

import com.sack.rpgroll.common.content.ContentManager;
import com.sack.rpgroll.common.yaml.YamlLoader;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.Optional;

/** Los tipos de lucky block de plugins/RPGRoll-Crates/lucky/*.yml. */
public class LuckyManager extends ContentManager<LuckyBlock> {

    private volatile Map<Integer, LuckyBlock> byNote = Map.of();

    public LuckyManager(JavaPlugin plugin) {
        super(plugin, new YamlLoader(plugin), "lucky", "lucky block", new LuckyParser());
    }

    @Override
    public void initialize() {

        super.initialize();

        Map<Integer, LuckyBlock> notes = new java.util.HashMap<>();
        for (LuckyBlock block : getAll()) {
            LuckyBlock previous = notes.put(block.note(), block);
            if (previous != null) {
                JavaPlugin.getProvidingPlugin(LuckyManager.class).getLogger().warning("✘ Los lucky blocks '"
                        + previous.id() + "' y '" + block.id() + "' usan la misma nota: " + block.note());
            }
        }
        byNote = Map.copyOf(notes);
    }

    @Override
    protected boolean optionalContent() {
        return true;
    }

    public Optional<LuckyBlock> byNote(int note) {
        return Optional.ofNullable(byNote.get(note));
    }

}
