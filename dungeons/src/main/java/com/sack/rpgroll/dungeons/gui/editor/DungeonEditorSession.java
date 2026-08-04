package com.sack.rpgroll.dungeons.gui.editor;

import com.sack.rpgroll.dungeons.core.DungeonAction;
import com.sack.rpgroll.dungeons.core.DungeonBounds;
import com.sack.rpgroll.dungeons.core.DungeonCheckpointPolicy;
import com.sack.rpgroll.dungeons.core.DungeonDefinition;
import com.sack.rpgroll.dungeons.core.DungeonDefinitionWriter;
import com.sack.rpgroll.dungeons.core.DungeonDifficulty;
import com.sack.rpgroll.dungeons.core.DungeonLootEntry;
import com.sack.rpgroll.dungeons.core.DungeonManager;
import com.sack.rpgroll.dungeons.core.DungeonPoint;
import com.sack.rpgroll.dungeons.core.DungeonReviveConfig;
import com.sack.rpgroll.dungeons.core.DungeonRoom;
import com.sack.rpgroll.dungeons.core.DungeonTrigger;
import com.sack.rpgroll.dungeons.gui.ChatPromptManager;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Copia de trabajo mutable de una {@link DungeonDefinition} en edición.
 * Compartida por referencia entre el hub y todos los sub-editores del
 * Dungeon Studio — mismo patrón que el editor de RPGRoll-Mobs/Items.
 */
public class DungeonEditorSession {

    public final DungeonDefinition original;
    public final DungeonManager dungeonManager;
    public final ChatPromptManager chatPromptManager;
    public final Plugin plugin;

    public String category;
    public String displayName;
    public String icon;
    public String description;
    public int recommendedLevel;
    public int estimatedMinutes;
    public int minPlayers;
    public int maxPlayers;
    public long cooldownMillis;
    public boolean repeatable;
    public List<String> tags;
    public DungeonPoint lobbyPoint;
    public DungeonBounds bounds;
    public List<DungeonRoom> rooms;
    public List<DungeonDifficulty> difficulties;
    public DungeonCheckpointPolicy checkpointPolicy;
    public DungeonReviveConfig reviveConfig;
    public List<DungeonLootEntry> loot;
    public Map<DungeonTrigger, List<DungeonAction>> triggers;

    public DungeonEditorSession(DungeonDefinition original, DungeonManager dungeonManager,
            ChatPromptManager chatPromptManager, Plugin plugin) {

        this.original = original;
        this.dungeonManager = dungeonManager;
        this.chatPromptManager = chatPromptManager;
        this.plugin = plugin;

        this.category = original.category();
        this.displayName = original.displayName();
        this.icon = original.icon();
        this.description = original.description();
        this.recommendedLevel = original.recommendedLevel();
        this.estimatedMinutes = original.estimatedMinutes();
        this.minPlayers = original.minPlayers();
        this.maxPlayers = original.maxPlayers();
        this.cooldownMillis = original.cooldownMillis();
        this.repeatable = original.repeatable();
        this.tags = new ArrayList<>(original.tags());
        this.lobbyPoint = original.lobbyPoint();
        this.bounds = original.bounds();
        this.rooms = new ArrayList<>(original.rooms());
        this.difficulties = new ArrayList<>(original.difficulties());
        this.checkpointPolicy = original.checkpointPolicy();
        this.reviveConfig = original.reviveConfig();
        this.loot = new ArrayList<>(original.loot());
        this.triggers = new EnumMap<>(DungeonTrigger.class);
        this.triggers.putAll(original.triggers());
    }

    public DungeonDefinition buildDefinition() {
        return new DungeonDefinition(
                original.id(), category, displayName, icon, description, recommendedLevel, estimatedMinutes,
                minPlayers, maxPlayers, cooldownMillis, repeatable, tags, lobbyPoint, bounds, rooms, difficulties,
                checkpointPolicy, reviveConfig, loot, triggers);
    }

    public void save() throws IOException {

        DungeonDefinition updated = buildDefinition();
        dungeonManager.register(updated);

        File file = new File(plugin.getDataFolder(), "dungeons/" + updated.category() + "/" + updated.id() + ".yml");
        new DungeonDefinitionWriter().save(updated, file);
    }

}
