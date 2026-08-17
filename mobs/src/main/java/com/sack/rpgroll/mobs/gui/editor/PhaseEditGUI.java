package com.sack.rpgroll.mobs.gui.editor;

import com.sack.rpgroll.common.lang.LangManager;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.mobs.core.MobPhase;
import com.sack.rpgroll.mobs.core.MobSkill;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Edita una {@link MobPhase} puntual. Las skills de la fase son skills ya
 * definidas en {@code session.skills} — una fase no crea skills nuevas,
 * las suma a las que el mob ya tiene (filosofía "aditiva" de fases).
 */
public class PhaseEditGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int ID_SLOT = 9;
    private static final int THRESHOLD_SLOT = 10;
    private static final int BOSSBAR_COLOR_SLOT = 11;
    private static final int BOSSBAR_TITLE_SLOT = 12;
    private static final int DIALOGUE_SLOT = 13;
    private static final int PARTICLE_SLOT = 14;

    private static final int MULTIPLIERS_START_SLOT = 18;
    private static final int ADD_MULTIPLIER_SLOT = 26;

    private static final int SKILLS_START_SLOT = 27;
    private static final int ADD_SKILL_SLOT = 35;

    private static final int BACK_SLOT = 44;

    private final MobEditorSession session;
    private final int index;
    private final Runnable onBack;
    private final LangManager lang;

    public PhaseEditGUI(Player player, MobEditorSession session, int index, Runnable onBack) {
        super(player, session.chatPromptManager.lang().component("gui.phase_edit.title", "id",
                session.phases.get(index).id()), SIZE);
        this.session = session;
        this.index = index;
        this.onBack = onBack;
        this.lang = session.chatPromptManager.lang();
    }

    private MobPhase phase() {
        return session.phases.get(index);
    }

    private void replace(MobPhase updated) {
        List<MobPhase> list = new ArrayList<>(session.phases);
        list.set(index, updated);
        session.phases = list;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        MobPhase phase = phase();

        setItem(ID_SLOT, new ItemBuilder(Material.PAPER)
                .setName(lang.component("gui.common.id_label", "id", phase.id()))
                .setLore(lang.component("gui.common.click_new_value"))
                .build());

        setItem(THRESHOLD_SLOT, new ItemBuilder(Material.NETHER_STAR)
                .setName(lang.component("gui.phase_edit.threshold_label", "value", phase.healthThresholdPercent()))
                .setLore(lang.component("gui.common.click_plus5_25"),
                        lang.component("gui.common.click_minus5_25"))
                .build());

        setItem(BOSSBAR_COLOR_SLOT, new ItemBuilder(Material.DRAGON_HEAD)
                .setName(lang.component("gui.phase_edit.bossbar_color_label", "value",
                        phase.bossBarColor() != null ? phase.bossBarColor() : lang.raw("gui.phase_edit.no_change")))
                .setLore(lang.component("gui.common.click_write_shift_remove"))
                .build());

        setItem(BOSSBAR_TITLE_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(lang.component("gui.phase_edit.bossbar_title_label", "value",
                        phase.bossBarTitle() != null ? phase.bossBarTitle() : lang.raw("gui.phase_edit.no_change")))
                .setLore(lang.component("gui.common.click_write_shift_remove"))
                .build());

        setItem(DIALOGUE_SLOT, new ItemBuilder(Material.WRITABLE_BOOK)
                .setName(lang.component("gui.phase_edit.dialogue_label", "value",
                        phase.dialogueLine() != null ? phase.dialogueLine() : lang.raw("gui.common.none_label")))
                .setLore(lang.component("gui.common.click_write_shift_remove"))
                .build());

        setItem(PARTICLE_SLOT, new ItemBuilder(Material.BLAZE_POWDER)
                .setName(lang.component("gui.phase_edit.particle_label", "value",
                        phase.particleEffect() != null ? phase.particleEffect()
                                : lang.raw("gui.phase_edit.none_feminine")))
                .setLore(lang.component("gui.phase_edit.particle_hint"),
                        lang.component("gui.common.shift_remove_hint"))
                .build());

        int i = 0;
        for (var entry : phase.statMultipliers().entrySet()) {
            if (i >= 8) {
                break;
            }
            setItem(MULTIPLIERS_START_SLOT + i, new ItemBuilder(Material.REDSTONE)
                    .setName(lang.component("gui.phase_edit.multiplier_label", "stat", entry.getKey(), "value",
                            entry.getValue()))
                    .setLore(lang.component("gui.common.shift_remove_dark"))
                    .build());
            i++;
        }

        setItem(ADD_MULTIPLIER_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("gui.phase_edit.add_multiplier"))
                .setLore(lang.component("gui.phase_edit.add_multiplier_hint"))
                .build());

        for (int s = 0; s < phase.skills().size() && s < 8; s++) {
            setItem(SKILLS_START_SLOT + s, new ItemBuilder(Material.BLAZE_ROD)
                    .setName(net.kyori.adventure.text.Component.text(phase.skills().get(s).id(),
                            net.kyori.adventure.text.format.NamedTextColor.YELLOW))
                    .setLore(lang.component("gui.common.shift_remove_dark"))
                    .build());
        }

        setItem(ADD_SKILL_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(lang.component("gui.phase_edit.add_skill"))
                .setLore(lang.component("gui.phase_edit.add_skill_hint"))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton(lang.raw("gui.common.back_button")));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == ID_SLOT) {
            session.chatPromptManager.prompt(player, "gui.common.prompt_new_id_generic", value -> {
                replace(withId(phase(), value.trim().toLowerCase().replace(' ', '_')));
                build();
            });
            return;
        }

        if (slot == THRESHOLD_SLOT) {
            double updated = Math.max(0, Math.min(100, phase().healthThresholdPercent() + delta(event.getClick())));
            replace(withThreshold(phase(), updated));
            build();
            return;
        }

        if (slot == BOSSBAR_COLOR_SLOT) {
            if (event.isShiftClick()) {
                replace(withBossBarColor(phase(), null));
                build();
                return;
            }
            session.chatPromptManager.prompt(player, "gui.phase_edit.prompt_bossbar_color", value -> {
                replace(withBossBarColor(phase(), value.trim().toUpperCase(Locale.ROOT)));
                build();
            });
            return;
        }

        if (slot == BOSSBAR_TITLE_SLOT) {
            if (event.isShiftClick()) {
                replace(withBossBarTitle(phase(), null));
                build();
                return;
            }
            session.chatPromptManager.prompt(player, "gui.bossbar.prompt_title", value -> {
                replace(withBossBarTitle(phase(), value));
                build();
            });
            return;
        }

        if (slot == DIALOGUE_SLOT) {
            if (event.isShiftClick()) {
                replace(withDialogue(phase(), null));
                build();
                return;
            }
            session.chatPromptManager.prompt(player, "gui.phase_edit.prompt_dialogue", value -> {
                replace(withDialogue(phase(), value));
                build();
            });
            return;
        }

        if (slot == PARTICLE_SLOT) {
            if (event.isShiftClick()) {
                replace(withParticle(phase(), null));
                build();
                return;
            }
            session.chatPromptManager.prompt(player, "gui.phase_edit.prompt_particle", value -> {
                replace(withParticle(phase(), value.trim().toUpperCase(Locale.ROOT)));
                build();
            });
            return;
        }

        if (slot >= MULTIPLIERS_START_SLOT && slot < MULTIPLIERS_START_SLOT + Math.min(8,
                phase().statMultipliers().size())) {
            if (event.isShiftClick()) {
                List<String> keys = new ArrayList<>(phase().statMultipliers().keySet());
                Map<String, Double> updated = new LinkedHashMap<>(phase().statMultipliers());
                updated.remove(keys.get(slot - MULTIPLIERS_START_SLOT));
                replace(withMultipliers(phase(), updated));
                build();
            }
            return;
        }

        if (slot == ADD_MULTIPLIER_SLOT) {
            promptAddMultiplier();
            return;
        }

        if (slot >= SKILLS_START_SLOT && slot < SKILLS_START_SLOT + Math.min(8, phase().skills().size())) {
            if (event.isShiftClick()) {
                List<MobSkill> updated = new ArrayList<>(phase().skills());
                updated.remove(slot - SKILLS_START_SLOT);
                replace(withSkills(phase(), updated));
                build();
            }
            return;
        }

        if (slot == ADD_SKILL_SLOT) {
            promptAddSkill();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void promptAddMultiplier() {
        session.chatPromptManager.prompt(player, "gui.phase_edit.prompt_add_multiplier", value -> {

            String[] parts = value.trim().split("\\s+");

            if (parts.length != 2) {
                lang.send(player, "gui.common.invalid_format");
                return;
            }

            try {
                Map<String, Double> updated = new LinkedHashMap<>(phase().statMultipliers());
                updated.put(parts[0].toLowerCase(Locale.ROOT), Double.parseDouble(parts[1]));
                replace(withMultipliers(phase(), updated));
            } catch (NumberFormatException e) {
                lang.send(player, "gui.common.invalid_number");
                return;
            }

            build();
        });
    }

    private void promptAddSkill() {
        session.chatPromptManager.prompt(player, "gui.phase_edit.prompt_add_skill", value -> {

            String skillId = value.trim().toLowerCase();

            MobSkill match = session.skills.stream()
                    .filter(skill -> skill.id().equalsIgnoreCase(skillId))
                    .findFirst().orElse(null);

            if (match == null) {
                lang.send(player, "gui.phase_edit.skill_not_found");
                return;
            }

            List<MobSkill> updated = new ArrayList<>(phase().skills());
            updated.add(match);
            replace(withSkills(phase(), updated));

            build();
        });
    }

    private double delta(ClickType click) {
        return switch (click) {
            case LEFT -> 5;
            case SHIFT_LEFT -> 25;
            case RIGHT -> -5;
            case SHIFT_RIGHT -> -25;
            default -> 0;
        };
    }

    private MobPhase withId(MobPhase phase, String id) {
        return new MobPhase(id, phase.healthThresholdPercent(), phase.statMultipliers(), phase.skills(),
                phase.bossBarColor(), phase.bossBarTitle(), phase.dialogueLine(), phase.particleEffect());
    }

    private MobPhase withThreshold(MobPhase phase, double threshold) {
        return new MobPhase(phase.id(), threshold, phase.statMultipliers(), phase.skills(), phase.bossBarColor(),
                phase.bossBarTitle(), phase.dialogueLine(), phase.particleEffect());
    }

    private MobPhase withMultipliers(MobPhase phase, Map<String, Double> multipliers) {
        return new MobPhase(phase.id(), phase.healthThresholdPercent(), multipliers, phase.skills(),
                phase.bossBarColor(), phase.bossBarTitle(), phase.dialogueLine(), phase.particleEffect());
    }

    private MobPhase withSkills(MobPhase phase, List<MobSkill> skills) {
        return new MobPhase(phase.id(), phase.healthThresholdPercent(), phase.statMultipliers(), skills,
                phase.bossBarColor(), phase.bossBarTitle(), phase.dialogueLine(), phase.particleEffect());
    }

    private MobPhase withBossBarColor(MobPhase phase, String color) {
        return new MobPhase(phase.id(), phase.healthThresholdPercent(), phase.statMultipliers(), phase.skills(),
                color, phase.bossBarTitle(), phase.dialogueLine(), phase.particleEffect());
    }

    private MobPhase withBossBarTitle(MobPhase phase, String title) {
        return new MobPhase(phase.id(), phase.healthThresholdPercent(), phase.statMultipliers(), phase.skills(),
                phase.bossBarColor(), title, phase.dialogueLine(), phase.particleEffect());
    }

    private MobPhase withDialogue(MobPhase phase, String dialogue) {
        return new MobPhase(phase.id(), phase.healthThresholdPercent(), phase.statMultipliers(), phase.skills(),
                phase.bossBarColor(), phase.bossBarTitle(), dialogue, phase.particleEffect());
    }

    private MobPhase withParticle(MobPhase phase, String particle) {
        return new MobPhase(phase.id(), phase.healthThresholdPercent(), phase.statMultipliers(), phase.skills(),
                phase.bossBarColor(), phase.bossBarTitle(), phase.dialogueLine(), particle);
    }

}
