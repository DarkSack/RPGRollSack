package com.sack.rpgroll.mobs.gui.editor;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

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

    public MobEditorHubGUI(Player player, MobEditorSession session) {
        super(player, Component.text("Editando: " + session.original.id(), NamedTextColor.GOLD), SIZE);
        this.session = session;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(PREVIEW_SLOT, previewIcon());

        setItem(IDENTITY_SLOT, categoryButton(Material.NAME_TAG, "Identidad",
                "Nombre, categoría, nivel, rareza, color, tags, descripción"));
        setItem(MODEL_SLOT, categoryButton(Material.ARMOR_STAND, "Modelo",
                "Tipo base, escala, brillo, invisibilidad, equipo"));
        setItem(STATS_SLOT, categoryButton(Material.REDSTONE, "Stats",
                "Vida, daño, defensa, velocidad, crítico, esquive..."));
        setItem(RESIST_SLOT, categoryButton(Material.SHIELD, "Resistencias/Debilidades",
                "% de reducción o daño extra por elemento"));
        setItem(AI_SLOT, categoryButton(Material.COMPASS, "Inteligencia artificial",
                "Objetivos de comportamiento, aggro, huida, patrulla"));
        setItem(SKILLS_SLOT, categoryButton(Material.BLAZE_POWDER, "Skills",
                "Habilidades disparadas por trigger, cooldown y condiciones"));
        setItem(TRIGGERS_SLOT, categoryButton(Material.COMPARATOR, "Triggers",
                "Acciones directas por evento (spawn, muerte, daño...)"));

        setItem(PHASES_SLOT, categoryButton(Material.NETHER_STAR, "Fases",
                "Transiciones de jefe por % de vida"));
        setItem(LOOT_SLOT, categoryButton(Material.CHEST, "Loot",
                "Tabla de drops con probabilidad"));
        setItem(BOSSBAR_SLOT, categoryButton(Material.DRAGON_HEAD, "BossBar",
                "Barra de jefe: color, estilo, título"));
        setItem(DIALOGUES_SLOT, categoryButton(Material.WRITABLE_BOOK, "Diálogos",
                "Frases por trigger o por fase"));
        setItem(SPAWN_RULES_SLOT, categoryButton(Material.GRASS_BLOCK, "Reglas de spawn",
                "Biomas, mundos, región, altura, hora, clima"));
        setItem(CUSTOM_DATA_SLOT, categoryButton(Material.NAME_TAG, "Datos custom",
                "Pares clave-valor libres para otros addons"));

        setItem(SAVE_SLOT, ItemBuilder.createConfirmButton("Guardar"));
        setItem(CANCEL_SLOT, ItemBuilder.createCancelButton("Cancelar"));
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
                        Component.text(session.category + " · Nivel " + session.level, NamedTextColor.GRAY))
                .build();
    }

    private ItemStack categoryButton(Material material, String name, String description) {
        return new ItemBuilder(material)
                .setName(Component.text(name, NamedTextColor.YELLOW))
                .setLore(Component.text(description, NamedTextColor.GRAY))
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
            player.sendMessage(Component.text("✔ Mob guardado.", NamedTextColor.GREEN));
        } catch (java.io.IOException e) {
            player.sendMessage(Component.text("✘ Error guardando el mob: " + e.getMessage(), NamedTextColor.RED));
        }

        markSelectionMade();
        close();
    }

}
