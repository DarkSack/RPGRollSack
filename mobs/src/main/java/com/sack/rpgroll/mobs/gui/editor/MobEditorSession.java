package com.sack.rpgroll.mobs.gui.editor;

import com.sack.rpgroll.mobs.core.MobAction;
import com.sack.rpgroll.mobs.core.MobBossBarDef;
import com.sack.rpgroll.mobs.core.MobCategory;
import com.sack.rpgroll.mobs.core.MobDefinition;
import com.sack.rpgroll.mobs.core.MobDefinitionWriter;
import com.sack.rpgroll.mobs.core.MobDialogueLine;
import com.sack.rpgroll.mobs.core.MobLootEntry;
import com.sack.rpgroll.mobs.core.MobManager;
import com.sack.rpgroll.mobs.core.MobModel;
import com.sack.rpgroll.mobs.core.AIBehaviorDef;
import com.sack.rpgroll.mobs.core.MobPhase;
import com.sack.rpgroll.mobs.core.MobSkill;
import com.sack.rpgroll.mobs.core.MobTrigger;
import com.sack.rpgroll.mobs.core.MobWeakness;
import com.sack.rpgroll.mobs.core.SpawnRules;
import com.sack.rpgroll.mobs.gui.ChatPromptManager;
import com.sack.rpgroll.mobs.registry.MobStatRegistry;

import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Copia de trabajo mutable de un {@link MobDefinition} en edición.
 * Compartida por referencia entre el hub y todos los sub-editores — mismo
 * patrón que el editor de RPGRoll-Items ({@code EditorSession}).
 */
public class MobEditorSession {

    public final MobDefinition original;
    public final MobManager mobManager;
    public final MobStatRegistry statRegistry;
    public final ChatPromptManager chatPromptManager;
    public final Plugin plugin;

    public MobCategory category;
    public String displayName;
    public String nameColor;
    public int level;
    public String rarityId;
    public List<String> tags;
    public String description;
    public MobModel model;
    public Map<String, Double> stats;
    public Map<String, Double> resistances;
    public List<MobWeakness> weaknesses;
    public AIBehaviorDef ai;
    public List<MobSkill> skills;
    public Map<MobTrigger, List<MobAction>> triggers;
    public List<MobPhase> phases;
    public List<MobLootEntry> loot;
    public MobBossBarDef bossBar;
    public List<MobDialogueLine> dialogues;
    public SpawnRules spawnRules;
    public Map<String, String> customData;

    public MobEditorSession(MobDefinition original, MobManager mobManager, MobStatRegistry statRegistry,
            ChatPromptManager chatPromptManager, Plugin plugin) {

        this.original = original;
        this.mobManager = mobManager;
        this.statRegistry = statRegistry;
        this.chatPromptManager = chatPromptManager;
        this.plugin = plugin;

        this.category = original.category();
        this.displayName = original.displayName();
        this.nameColor = original.nameColor();
        this.level = original.level();
        this.rarityId = original.rarityId();
        this.tags = new ArrayList<>(original.tags());
        this.description = original.description();
        this.model = original.model();
        this.stats = new LinkedHashMap<>(original.stats());
        this.resistances = new LinkedHashMap<>(original.resistances());
        this.weaknesses = new ArrayList<>(original.weaknesses());
        this.ai = original.ai();
        this.skills = new ArrayList<>(original.skills());
        this.triggers = new EnumMap<>(MobTrigger.class);
        this.triggers.putAll(original.triggers());
        this.phases = new ArrayList<>(original.phases());
        this.loot = new ArrayList<>(original.loot());
        this.bossBar = original.bossBar();
        this.dialogues = new ArrayList<>(original.dialogues());
        this.spawnRules = original.spawnRules();
        this.customData = new LinkedHashMap<>(original.customData());
    }

    public MobDefinition buildDefinition() {
        return new MobDefinition(
                original.id(), category, displayName, nameColor, level, rarityId, tags, description, model,
                stats, resistances, weaknesses, ai, skills, triggers, phases, loot, bossBar, dialogues,
                spawnRules, customData);
    }

    public void save() throws IOException {

        MobDefinition updated = buildDefinition();
        mobManager.register(updated);

        File file = new File(plugin.getDataFolder(), "mobs/" + updated.category().name().toLowerCase() + "/"
                + updated.id() + ".yml");
        new MobDefinitionWriter().save(updated, file);
    }

}
