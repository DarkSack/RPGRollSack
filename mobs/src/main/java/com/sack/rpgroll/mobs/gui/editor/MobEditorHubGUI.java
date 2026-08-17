package com.sack.rpgroll.mobs.gui.editor;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

/**
 * Punto de entrada del editor gráfico de un mob: un botón por componente
 * (Identidad, Modelo, Stats, Resistencias/Debilidades, IA, Skills,
 * Triggers, Fases, Loot, BossBar, Diálogos, Reglas de spawn, Datos
 * custom). Todos comparten la misma {@link MobEditorSession} — los
 * cambios se acumulan hasta presionar Guardar.
 */
public class MobEditorHubGUI extends InventoryGUI {

    private static final int SIZE = 54;
    private static final int PREVIEW_SLOT = 4;

    private static final int IDENTITY_SLOT = 10;
    private static final int MODEL_SLOT = 11;
    private static final int STATS_SLOT = 12;
    private static final int RESIST_SLOT = 13;
    private static final int AI_SLOT = 14;
    private static final int SKILLS_SLOT = 15;
    private static final int TRIGGERS_SLOT = 16;

    private static final int PHASES_SLOT = 19;
    private static final int LOOT_SLOT = 20;
    private static final int BOSSBAR_SLOT = 21;
    private static final int DIALOGUES_SLOT = 22;
    private static final int SPAWN_RULES_SLOT = 23;
    private static final int CUSTOM_DATA_SLOT = 24;

    private static final int SAVE_SLOT = 48;
    private static final int CANCEL_SLOT = 50;

    private final MobEditorSession session;
    private final LangManager lang;

    public MobEditorHubGUI(Player player, MobEditorSession session) {
        super(player, session.chatPromptManager.lang().component("gui.editor.hub_title", "id",
                session.original.id()), SIZE);
        this.session = session;
        this.lang = session.chatPromptManager.lang();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(PREVIEW_SLOT, previewIcon());

        setItem(IDENTITY_SLOT, categoryButton(Material.NAME_TAG, "gui.editor.identity_name",
                "gui.editor.identity_desc"));
        setItem(MODEL_SLOT, categoryButton(Material.ARMOR_STAND, "gui.editor.model_name",
                "gui.editor.model_desc"));
        setItem(STATS_SLOT, categoryButton(Material.REDSTONE, "gui.editor.stats_name",
                "gui.editor.stats_desc"));
        setItem(RESIST_SLOT, categoryButton(Material.SHIELD, "gui.editor.resist_name",
                "gui.editor.resist_desc"));
        setItem(AI_SLOT, categoryButton(Material.COMPASS, "gui.editor.ai_name",
                "gui.editor.ai_desc"));
        setItem(SKILLS_SLOT, categoryButton(Material.BLAZE_POWDER, "gui.editor.skills_name",
                "gui.editor.skills_desc"));
        setItem(TRIGGERS_SLOT, categoryButton(Material.COMPARATOR, "gui.editor.triggers_name",
                "gui.editor.triggers_desc"));

        setItem(PHASES_SLOT, categoryButton(Material.NETHER_STAR, "gui.editor.phases_name",
                "gui.editor.phases_desc"));
        setItem(LOOT_SLOT, categoryButton(Material.CHEST, "gui.editor.loot_name",
                "gui.editor.loot_desc"));
        setItem(BOSSBAR_SLOT, categoryButton(Material.DRAGON_HEAD, "gui.editor.bossbar_name",
                "gui.editor.bossbar_desc"));
        setItem(DIALOGUES_SLOT, categoryButton(Material.WRITABLE_BOOK, "gui.editor.dialogues_name",
                "gui.editor.dialogues_desc"));
        setItem(SPAWN_RULES_SLOT, categoryButton(Material.GRASS_BLOCK, "gui.editor.spawn_rules_name",
                "gui.editor.spawn_rules_desc"));
        setItem(CUSTOM_DATA_SLOT, categoryButton(Material.NAME_TAG, "gui.editor.custom_data_name",
                "gui.editor.custom_data_desc"));

        setItem(SAVE_SLOT, ItemBuilder.createConfirmButton(lang.raw("gui.common.save_button")));
        setItem(CANCEL_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.cancel_button")));
    }

    private ItemStack previewIcon() {

        Material egg;
        try {
            egg = Material.valueOf(session.model.baseEntityType().toUpperCase(Locale.ROOT) + "_SPAWN_EGG");
        } catch (IllegalArgumentException e) {
            egg = Material.ZOMBIE_HEAD;
        }

        return new ItemBuilder(egg)
                .setName(ComponentUtils.parse(session.displayName)
                        .colorIfAbsent(NamedTextColor.WHITE))
                .setLore(Component.text(session.original.id(), NamedTextColor.DARK_GRAY),
                        lang.component("gui.browser.item_category_level", "category", session.category,
                                "level", session.level))
                .build();
    }

    private ItemStack categoryButton(Material material, String nameKey, String descKey) {
        return new ItemBuilder(material)
                .setName(lang.component(nameKey))
                .setLore(lang.component(descKey))
                .build();
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);

        switch (event.getSlot()) {
            case IDENTITY_SLOT -> new IdentityEditorGUI(player, session, this::reopen).open();
            case MODEL_SLOT -> new ModelEditorGUI(player, session, this::reopen).open();
            case STATS_SLOT -> new MobStatsEditorGUI(player, session, this::reopen).open();
            case RESIST_SLOT -> new ResistancesEditorGUI(player, session, this::reopen).open();
            case AI_SLOT -> new AIEditorGUI(player, session, this::reopen).open();
            case SKILLS_SLOT -> new SkillsEditorGUI(player, session, this::reopen).open();
            case TRIGGERS_SLOT -> new MobTriggersEditorGUI(player, session, this::reopen).open();
            case PHASES_SLOT -> new PhasesEditorGUI(player, session, this::reopen).open();
            case LOOT_SLOT -> new LootEditorGUI(player, session, this::reopen).open();
            case BOSSBAR_SLOT -> new BossBarEditorGUI(player, session, this::reopen).open();
            case DIALOGUES_SLOT -> new DialoguesEditorGUI(player, session, this::reopen).open();
            case SPAWN_RULES_SLOT -> new SpawnRulesEditorGUI(player, session, this::reopen).open();
            case CUSTOM_DATA_SLOT -> new MobCustomDataEditorGUI(player, session, this::reopen).open();
            case SAVE_SLOT -> save();
            case CANCEL_SLOT -> close();
            default -> {
            }
        }
    }

    private void reopen() {
        new MobEditorHubGUI(player, session).open();
    }

    private void save() {

        try {
            session.save();
            lang.send(player, "gui.editor.save_success");
        } catch (java.io.IOException e) {
            lang.send(player, "gui.editor.save_error", "error", e.getMessage());
        }

        markSelectionMade();
        close();
    }

}
