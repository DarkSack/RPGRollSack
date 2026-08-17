package com.sack.rpgroll.mobs.gui.editor;

import com.sack.rpgroll.common.lang.LangManager;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.mobs.core.MobAction;
import com.sack.rpgroll.mobs.core.MobSkill;
import com.sack.rpgroll.mobs.core.MobTrigger;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Edita una {@link MobSkill} puntual dentro de {@code session.skills.get(index)}. */
public class SkillEditGUI extends InventoryGUI {

    private static final int SIZE = 36;

    private static final int NAME_SLOT = 10;
    private static final int ID_SLOT = 11;
    private static final int TRIGGER_SLOT = 12;
    private static final int COOLDOWN_SLOT = 13;
    private static final int CHANCE_SLOT = 14;
    private static final int CONDITIONS_SLOT = 15;
    private static final int ACTIONS_SLOT = 16;

    private static final int BACK_SLOT = 31;

    private final MobEditorSession session;
    private final int index;
    private final Runnable onBack;
    private final LangManager lang;

    public SkillEditGUI(Player player, MobEditorSession session, int index, Runnable onBack) {
        super(player, session.chatPromptManager.lang().component("gui.skill_edit.title", "id",
                session.skills.get(index).id()), SIZE);
        this.session = session;
        this.index = index;
        this.onBack = onBack;
        this.lang = session.chatPromptManager.lang();
    }

    private MobSkill skill() {
        return session.skills.get(index);
    }

    private void replace(MobSkill updated) {
        List<MobSkill> list = new ArrayList<>(session.skills);
        list.set(index, updated);
        session.skills = list;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        MobSkill skill = skill();

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(lang.component("gui.skill_edit.name_label", "name", skill.displayName()))
                .setLore(lang.component("gui.common.click_new_value"))
                .build());

        setItem(ID_SLOT, new ItemBuilder(Material.PAPER)
                .setName(lang.component("gui.skill_edit.id_label", "id", skill.id()))
                .setLore(lang.component("gui.common.click_new_value"))
                .build());

        setItem(TRIGGER_SLOT, new ItemBuilder(Material.COMPARATOR)
                .setName(lang.component("gui.skill_edit.trigger_label", "trigger",
                        skill.trigger() != null ? skill.trigger() : lang.raw("gui.skill_edit.passive")))
                .setLore(lang.component("gui.common.click_next"))
                .build());

        setItem(COOLDOWN_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(lang.component("gui.skill_edit.cooldown_label", "seconds", skill.cooldownMillis() / 1000))
                .setLore(lang.component("gui.skill_edit.cooldown_hint1"),
                        lang.component("gui.skill_edit.cooldown_hint2"))
                .build());

        setItem(CHANCE_SLOT, new ItemBuilder(Material.GOLD_NUGGET)
                .setName(lang.component("gui.skill_edit.chance_label", "chance", skill.chance()))
                .setLore(lang.component("gui.skill_edit.chance_hint1"),
                        lang.component("gui.skill_edit.chance_hint2"))
                .build());

        setItem(CONDITIONS_SLOT, new ItemBuilder(Material.BOOK)
                .setName(lang.component("gui.skill_edit.conditions_label", "count", skill.conditions().size()))
                .setLore(lang.component("gui.skill_edit.conditions_hint"))
                .build());

        setItem(ACTIONS_SLOT, new ItemBuilder(Material.COMMAND_BLOCK)
                .setName(lang.component("gui.skill_edit.actions_label", "count", skill.actions().size()))
                .setLore(lang.component("gui.skill_edit.actions_hint"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back_button")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == NAME_SLOT) {
            session.chatPromptManager.prompt(player, "gui.common.prompt_new_name", value -> {
                replace(withName(skill(), value));
                build();
            });
            return;
        }

        if (slot == ID_SLOT) {
            session.chatPromptManager.prompt(player, "gui.common.prompt_new_id_generic", value -> {
                replace(withId(skill(), value.trim().toLowerCase().replace(' ', '_')));
                build();
            });
            return;
        }

        if (slot == TRIGGER_SLOT) {
            replace(withTrigger(skill(), nextTrigger(skill().trigger())));
            build();
            return;
        }

        if (slot == COOLDOWN_SLOT) {
            long deltaSeconds = (long) delta(event.getClick());
            long updated = Math.max(0, skill().cooldownMillis() / 1000 + deltaSeconds);
            replace(withCooldown(skill(), updated * 1000));
            build();
            return;
        }

        if (slot == CHANCE_SLOT) {
            double deltaValue = delta(event.getClick()) * 5;
            // MobSkill trata chance <= 0 como "sin configurar" y lo resetea a 100 —
            // el mínimo real editable es 1%, no 0.
            double updated = Math.max(1, Math.min(100, skill().chance() + deltaValue));
            replace(withChance(skill(), updated));
            build();
            return;
        }

        if (slot == CONDITIONS_SLOT) {
            String current = String.join("\n", skill().conditions());
            session.chatPromptManager.prompt(player, "gui.skill_edit.prompt_conditions", value -> {
                        List<String> conditions = value.isBlank() ? List.of()
                                : List.of(value.split("\\s*;\\s*"));
                        replace(withConditions(skill(), conditions));
                        build();
                    });
            return;
        }

        if (slot == ACTIONS_SLOT) {
            List<MobAction> actions = new ArrayList<>(skill().actions());

            new MobActionListEditorGUI(player,
                    lang.raw("gui.skill_edit.actions_gui_title", "name", skill().displayName()), actions,
                    session.chatPromptManager, () -> {
                        replace(withActions(skill(), actions));
                        reopen();
                    }).open();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private MobTrigger nextTrigger(MobTrigger current) {

        MobTrigger[] values = MobTrigger.values();

        if (current == null) {
            return values[0];
        }

        int next = current.ordinal() + 1;
        return next >= values.length ? null : values[next];
    }

    private double delta(ClickType click) {
        return switch (click) {
            case LEFT -> 1;
            case SHIFT_LEFT -> 5;
            case RIGHT -> -1;
            case SHIFT_RIGHT -> -5;
            default -> 0;
        };
    }

    private MobSkill withName(MobSkill skill, String name) {
        return new MobSkill(skill.id(), name, skill.trigger(), skill.cooldownMillis(), skill.chance(),
                skill.conditions(), skill.actions());
    }

    private MobSkill withId(MobSkill skill, String id) {
        return new MobSkill(id, skill.displayName(), skill.trigger(), skill.cooldownMillis(), skill.chance(),
                skill.conditions(), skill.actions());
    }

    private MobSkill withTrigger(MobSkill skill, MobTrigger trigger) {
        return new MobSkill(skill.id(), skill.displayName(), trigger, skill.cooldownMillis(), skill.chance(),
                skill.conditions(), skill.actions());
    }

    private MobSkill withCooldown(MobSkill skill, long cooldownMillis) {
        return new MobSkill(skill.id(), skill.displayName(), skill.trigger(), cooldownMillis, skill.chance(),
                skill.conditions(), skill.actions());
    }

    private MobSkill withChance(MobSkill skill, double chance) {
        return new MobSkill(skill.id(), skill.displayName(), skill.trigger(), skill.cooldownMillis(), chance,
                skill.conditions(), skill.actions());
    }

    private MobSkill withConditions(MobSkill skill, List<String> conditions) {
        return new MobSkill(skill.id(), skill.displayName(), skill.trigger(), skill.cooldownMillis(), skill.chance(),
                conditions, skill.actions());
    }

    private MobSkill withActions(MobSkill skill, List<MobAction> actions) {
        return new MobSkill(skill.id(), skill.displayName(), skill.trigger(), skill.cooldownMillis(), skill.chance(),
                skill.conditions(), actions);
    }

    private void reopen() {
        new SkillEditGUI(player, session, index, onBack).open();
    }

}
