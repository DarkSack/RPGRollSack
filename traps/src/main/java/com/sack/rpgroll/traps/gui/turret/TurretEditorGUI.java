package com.sack.rpgroll.traps.gui.turret;

import com.sack.rpgroll.common.lang.LangManager;
import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.traps.core.TrapAction;
import com.sack.rpgroll.traps.gui.ChatPromptManager;
import com.sack.rpgroll.traps.gui.StringListEditorGUI;
import com.sack.rpgroll.traps.turret.TurretDefinition;
import com.sack.rpgroll.traps.turret.TurretManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Editor de una TurretDefinition — a diferencia de TrapEditorHubGUI, una
 * torreta es plana (sin acciones/bloque/cadena/disguise), así que es un
 * único GUI en vez de un hub con sub-paneles. Condiciones abre un
 * sub-editor reusando {@link StringListEditorGUI} tal cual; Impacto edita
 * la única {@link TrapAction} que se ejecuta en cada disparo (mismo
 * formato libre "TIPO clave=valor" que {@code TrapActionsEditorGUI}, pero
 * para una sola acción en vez de una lista).
 */
public class TurretEditorGUI extends InventoryGUI {

    private static final int SIZE = 27;
    private static final int NAME_SLOT = 9;
    private static final int MODEL_SLOT = 10;
    private static final int RADIUS_SLOT = 11;
    private static final int TARGET_PLAYERS_SLOT = 12;
    private static final int TARGET_MOBS_SLOT = 13;
    private static final int FIRE_INTERVAL_SLOT = 14;
    private static final int IMPACT_SLOT = 15;
    private static final int CONDITIONS_SLOT = 16;
    private static final int BACK_SLOT = 22;

    private final TurretManager turretManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private String turretId;

    public TurretEditorGUI(Player player, TurretDefinition current, TurretManager turretManager,
            ChatPromptManager chatPromptManager, Runnable onBack) {
        super(player, chatPromptManager.lang().component("gui.turret.title", "id", current.id()), SIZE);
        this.turretId = current.id();
        this.turretManager = turretManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private LangManager lang() {
        return chatPromptManager.lang();
    }

    private TurretDefinition current() {
        return turretManager.get(turretId).orElseThrow();
    }

    private void replace(TurretDefinition updated) {
        this.turretId = updated.id();
        turretManager.save(updated);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        TurretDefinition current = current();

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(lang().component("gui.turret.name", "value", current.displayName()))
                .setLore(lang().component("gui.common.click_to_write"))
                .build());

        setItem(MODEL_SLOT, new ItemBuilder(current.model())
                .setName(lang().component("gui.turret.model", "value", current.model().name()))
                .setLore(lang().component("gui.common.click_to_write"))
                .build());

        setItem(RADIUS_SLOT, new ItemBuilder(Material.SPYGLASS)
                .setName(lang().component("gui.turret.radius", "value", current.radius()))
                .setLore(lang().component("gui.turret.updown_hint"))
                .build());

        setItem(TARGET_PLAYERS_SLOT, new ItemBuilder(Material.PLAYER_HEAD)
                .setName(lang().component("gui.turret.target_players", "value",
                        lang().raw(current.targetPlayers() ? "gui.common.yes" : "gui.common.no")))
                .setLore(lang().component("gui.common.click_to_toggle"))
                .build());

        setItem(TARGET_MOBS_SLOT, new ItemBuilder(Material.ZOMBIE_HEAD)
                .setName(lang().component("gui.turret.target_hostile_mobs", "value",
                        lang().raw(current.targetHostileMobs() ? "gui.common.yes" : "gui.common.no")))
                .setLore(lang().component("gui.common.click_to_toggle"))
                .build());

        setItem(FIRE_INTERVAL_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(lang().component("gui.turret.fire_interval", "value", current.fireIntervalTicks()))
                .setLore(lang().component("gui.turret.updown_hint"))
                .build());

        setItem(IMPACT_SLOT, new ItemBuilder(Material.CROSSBOW)
                .setName(lang().component("gui.turret.impact", "value", current.impact().type()))
                .setLore(impactLore(current.impact()))
                .build());

        setItem(CONDITIONS_SLOT, new ItemBuilder(Material.COMPARATOR)
                .setName(lang().component("gui.hub.conditions"))
                .setLore(lang().component("gui.hub.conditions_desc", "count", current.conditions().size()))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang().raw("gui.common.back")));
    }

    private List<Component> impactLore(TrapAction impact) {

        List<Component> lore = new ArrayList<>();

        for (var entry : impact.params().entrySet()) {
            lore.add(Component.text(entry.getKey() + "=" + entry.getValue(), NamedTextColor.GRAY));
        }

        lore.add(lang().component("gui.turret.impact_hint"));
        return lore;
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        ClickType click = event.getClick();
        TurretDefinition current = current();

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "gui.turret.prompt_name", value -> replace(withDisplayName(value)));
            return;
        }

        if (slot == MODEL_SLOT) {
            chatPromptManager.prompt(player, "gui.turret.prompt_model", value -> {
                try {
                    replace(withModel(Material.valueOf(value.trim().toUpperCase(Locale.ROOT))));
                } catch (IllegalArgumentException e) {
                    lang().send(player, "gui.info.invalid_material");
                }
            });
            return;
        }

        if (slot == RADIUS_SLOT) {
            double step = click.isShiftClick() ? 4.0 : 1.0;
            double delta = click.isRightClick() ? -step : step;
            replace(withRadius(Math.max(1.0, current.radius() + delta)));
            return;
        }

        if (slot == TARGET_PLAYERS_SLOT) {
            replace(withTargetPlayers(!current.targetPlayers()));
            return;
        }

        if (slot == TARGET_MOBS_SLOT) {
            replace(withTargetHostileMobs(!current.targetHostileMobs()));
            return;
        }

        if (slot == FIRE_INTERVAL_SLOT) {
            long step = click.isShiftClick() ? 20 : 5;
            long delta = click.isRightClick() ? -step : step;
            replace(withFireInterval(Math.max(1, current.fireIntervalTicks() + delta)));
            return;
        }

        if (slot == IMPACT_SLOT) {
            promptImpact();
            return;
        }

        if (slot == CONDITIONS_SLOT) {
            new StringListEditorGUI(player, lang().raw("gui.hub.conditions"), current.conditions(),
                    chatPromptManager, "gui.conditions.prompt_add",
                    conditions -> replace(withConditions(conditions)), this::open).open();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    /** Mismo formato libre "TIPO clave=valor clave2=valor2" que {@code TrapActionsEditorGUI.promptAdd()}, para UNA sola acción. */
    private void promptImpact() {
        chatPromptManager.prompt(player, "gui.turret.prompt_impact", value -> {

            String[] tokens = value.trim().split("\\s+");

            if (tokens.length == 0 || tokens[0].isBlank()) {
                lang().send(player, "gui.actionlist.invalid_format");
                return;
            }

            String type = tokens[0].toUpperCase(Locale.ROOT);
            Map<String, String> params = new HashMap<>();

            for (int i = 1; i < tokens.length; i++) {
                String[] kv = tokens[i].split("=", 2);
                if (kv.length == 2) {
                    params.put(kv[0], kv[1]);
                }
            }

            replace(withImpact(new TrapAction(type, params)));
        });
    }

    private TurretDefinition withDisplayName(String value) {
        TurretDefinition c = current();
        return new TurretDefinition(c.id(), value, c.description(), c.radius(), c.targetPlayers(),
                c.targetHostileMobs(), c.fireIntervalTicks(), c.conditions(), c.impact(), c.model(),
                c.customModelData());
    }

    private TurretDefinition withModel(Material model) {
        TurretDefinition c = current();
        return new TurretDefinition(c.id(), c.displayName(), c.description(), c.radius(), c.targetPlayers(),
                c.targetHostileMobs(), c.fireIntervalTicks(), c.conditions(), c.impact(), model,
                c.customModelData());
    }

    private TurretDefinition withRadius(double radius) {
        TurretDefinition c = current();
        return new TurretDefinition(c.id(), c.displayName(), c.description(), radius, c.targetPlayers(),
                c.targetHostileMobs(), c.fireIntervalTicks(), c.conditions(), c.impact(), c.model(),
                c.customModelData());
    }

    private TurretDefinition withTargetPlayers(boolean value) {
        TurretDefinition c = current();
        return new TurretDefinition(c.id(), c.displayName(), c.description(), c.radius(), value,
                c.targetHostileMobs(), c.fireIntervalTicks(), c.conditions(), c.impact(), c.model(),
                c.customModelData());
    }

    private TurretDefinition withTargetHostileMobs(boolean value) {
        TurretDefinition c = current();
        return new TurretDefinition(c.id(), c.displayName(), c.description(), c.radius(), c.targetPlayers(), value,
                c.fireIntervalTicks(), c.conditions(), c.impact(), c.model(), c.customModelData());
    }

    private TurretDefinition withFireInterval(long ticks) {
        TurretDefinition c = current();
        return new TurretDefinition(c.id(), c.displayName(), c.description(), c.radius(), c.targetPlayers(),
                c.targetHostileMobs(), ticks, c.conditions(), c.impact(), c.model(), c.customModelData());
    }

    private TurretDefinition withImpact(TrapAction impact) {
        TurretDefinition c = current();
        return new TurretDefinition(c.id(), c.displayName(), c.description(), c.radius(), c.targetPlayers(),
                c.targetHostileMobs(), c.fireIntervalTicks(), c.conditions(), impact, c.model(),
                c.customModelData());
    }

    private TurretDefinition withConditions(List<String> conditions) {
        TurretDefinition c = current();
        return new TurretDefinition(c.id(), c.displayName(), c.description(), c.radius(), c.targetPlayers(),
                c.targetHostileMobs(), c.fireIntervalTicks(), conditions, c.impact(), c.model(),
                c.customModelData());
    }

}
