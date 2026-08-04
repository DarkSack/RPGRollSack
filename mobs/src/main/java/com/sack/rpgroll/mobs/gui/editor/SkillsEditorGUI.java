package com.sack.rpgroll.mobs.gui.editor;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.mobs.core.MobSkill;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

/** Lista de skills del mob — click abre el editor de esa skill, shift-click la elimina. */
public class SkillsEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int ADD_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final MobEditorSession session;
    private final Runnable onBack;

    public SkillsEditorGUI(Player player, MobEditorSession session, Runnable onBack) {
        super(player, Component.text("Skills: " + session.original.id(), NamedTextColor.GOLD), SIZE);
        this.session = session;
        this.onBack = onBack;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        List<MobSkill> skills = session.skills;

        for (int i = 0; i < skills.size() && i < 36; i++) {

            MobSkill skill = skills.get(i);

            setItem(i, new ItemBuilder(Material.BLAZE_POWDER)
                    .setName(Component.text(skill.displayName(), NamedTextColor.YELLOW))
                    .setLore(
                            Component.text("id: " + skill.id(), NamedTextColor.DARK_GRAY),
                            Component.text("trigger: " + (skill.trigger() != null ? skill.trigger() : "(pasiva)"),
                                    NamedTextColor.GRAY),
                            Component.text("cooldown: " + (skill.cooldownMillis() / 1000) + "s · chance: "
                                    + skill.chance() + "%", NamedTextColor.GRAY),
                            Component.text("acciones: " + skill.actions().size(), NamedTextColor.GRAY),
                            Component.text("Click para editar · Shift-click para quitar", NamedTextColor.DARK_GRAY))
                    .build());
        }

        setItem(ADD_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Agregar skill", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < session.skills.size() && slot < 36) {

            if (event.isShiftClick()) {
                List<MobSkill> updated = new ArrayList<>(session.skills);
                updated.remove(slot);
                session.skills = updated;
                build();
                return;
            }

            new SkillEditGUI(player, session, slot, this::reopen).open();
            return;
        }

        if (slot == ADD_SLOT) {
            promptAdd();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void promptAdd() {
        session.chatPromptManager.prompt(player, "Escribí el id de la nueva skill:", value -> {

            String id = value.trim().toLowerCase().replace(' ', '_');

            List<MobSkill> updated = new ArrayList<>(session.skills);
            updated.add(new MobSkill(id, id, null, 0, 100.0, List.of(), List.of()));
            session.skills = updated;

            build();
        });
    }

    private void reopen() {
        new SkillsEditorGUI(player, session, onBack).open();
    }

}
