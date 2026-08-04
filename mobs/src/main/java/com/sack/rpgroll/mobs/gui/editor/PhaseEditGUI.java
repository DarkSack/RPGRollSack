package com.sack.rpgroll.mobs.gui.editor;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.mobs.core.MobPhase;
import com.sack.rpgroll.mobs.core.MobSkill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

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

    public PhaseEditGUI(Player player, MobEditorSession session, int index, Runnable onBack) {
        super(player, Component.text("Fase: " + session.phases.get(index).id(), NamedTextColor.GOLD), SIZE);
        this.session = session;
        this.index = index;
        this.onBack = onBack;
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
                .setName(Component.text("ID: " + phase.id(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click para escribir uno nuevo", NamedTextColor.GRAY))
                .build());

        setItem(THRESHOLD_SLOT, new ItemBuilder(Material.NETHER_STAR)
                .setName(Component.text("Umbral de vida: " + phase.healthThresholdPercent() + "%",
                        NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +5 · Shift-click: +25", NamedTextColor.GRAY),
                        Component.text("Click derecho: -5 · Shift-click derecho: -25", NamedTextColor.GRAY))
                .build());

        setItem(BOSSBAR_COLOR_SLOT, new ItemBuilder(Material.DRAGON_HEAD)
                .setName(Component.text("Color bossbar: "
                        + (phase.bossBarColor() != null ? phase.bossBarColor() : "(sin cambio)"),
                        NamedTextColor.YELLOW))
                .setLore(Component.text("Click: escribir · Shift-click: quitar", NamedTextColor.GRAY))
                .build());

        setItem(BOSSBAR_TITLE_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(Component.text("Título bossbar: "
                        + (phase.bossBarTitle() != null ? phase.bossBarTitle() : "(sin cambio)"),
                        NamedTextColor.YELLOW))
                .setLore(Component.text("Click: escribir · Shift-click: quitar", NamedTextColor.GRAY))
                .build());

        setItem(DIALOGUE_SLOT, new ItemBuilder(Material.WRITABLE_BOOK)
                .setName(Component.text("Diálogo al entrar: "
                        + (phase.dialogueLine() != null ? phase.dialogueLine() : "(ninguno)"), NamedTextColor.YELLOW))
                .setLore(Component.text("Click: escribir · Shift-click: quitar", NamedTextColor.GRAY))
                .build());

        setItem(PARTICLE_SLOT, new ItemBuilder(Material.BLAZE_POWDER)
                .setName(Component.text("Partícula de aura: "
                        + (phase.particleEffect() != null ? phase.particleEffect() : "(ninguna)"),
                        NamedTextColor.YELLOW))
                .setLore(Component.text("Click: escribir (org.bukkit.Particle)", NamedTextColor.GRAY),
                        Component.text("Shift-click: quitar", NamedTextColor.GRAY))
                .build());

        int i = 0;
        for (var entry : phase.statMultipliers().entrySet()) {
            if (i >= 8) {
                break;
            }
            setItem(MULTIPLIERS_START_SLOT + i, new ItemBuilder(Material.REDSTONE)
                    .setName(Component.text(entry.getKey() + " x" + entry.getValue(), NamedTextColor.RED))
                    .setLore(Component.text("Shift-click para quitar", NamedTextColor.DARK_GRAY))
                    .build());
            i++;
        }

        setItem(ADD_MULTIPLIER_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Agregar multiplicador", NamedTextColor.GREEN))
                .setLore(Component.text("Click para escribir: stat multiplicador (ej. damage 1.5)",
                        NamedTextColor.GRAY))
                .build());

        for (int s = 0; s < phase.skills().size() && s < 8; s++) {
            setItem(SKILLS_START_SLOT + s, new ItemBuilder(Material.BLAZE_ROD)
                    .setName(Component.text(phase.skills().get(s).id(), NamedTextColor.YELLOW))
                    .setLore(Component.text("Shift-click para quitar", NamedTextColor.DARK_GRAY))
                    .build());
        }

        setItem(ADD_SKILL_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Agregar skill existente", NamedTextColor.GREEN))
                .setLore(Component.text("Click para escribir el id de una skill ya creada", NamedTextColor.GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == ID_SLOT) {
            session.chatPromptManager.prompt(player, "Escribí el nuevo id:", value -> {
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
            session.chatPromptManager.prompt(player, "Escribí el color (ej. RED, PURPLE):", value -> {
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
            session.chatPromptManager.prompt(player, "Escribí el nuevo título:", value -> {
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
            session.chatPromptManager.prompt(player, "Escribí la línea de diálogo:", value -> {
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
            session.chatPromptManager.prompt(player, "Escribí el nombre de la partícula:", value -> {
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
        session.chatPromptManager.prompt(player, "Escribí: <stat> <multiplicador> (ej. damage 1.5):", value -> {

            String[] parts = value.trim().split("\\s+");

            if (parts.length != 2) {
                player.sendMessage(Component.text("Formato inválido.", NamedTextColor.RED));
                return;
            }

            try {
                Map<String, Double> updated = new LinkedHashMap<>(phase().statMultipliers());
                updated.put(parts[0].toLowerCase(Locale.ROOT), Double.parseDouble(parts[1]));
                replace(withMultipliers(phase(), updated));
            } catch (NumberFormatException e) {
                player.sendMessage(Component.text("Valor numérico inválido.", NamedTextColor.RED));
                return;
            }

            build();
        });
    }

    private void promptAddSkill() {
        session.chatPromptManager.prompt(player, "Escribí el id de una skill ya definida en Skills:", value -> {

            String skillId = value.trim().toLowerCase();

            MobSkill match = session.skills.stream()
                    .filter(skill -> skill.id().equalsIgnoreCase(skillId))
                    .findFirst().orElse(null);

            if (match == null) {
                player.sendMessage(Component.text("No existe esa skill — creala primero en Skills.",
                        NamedTextColor.RED));
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
