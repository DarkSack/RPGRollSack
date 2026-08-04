package com.sack.rpgroll.items.gui.editor;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.items.core.ItemAbility;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;

/** Lista de habilidades activas/pasivas del ítem — click abre el detalle, shift-click la quita. */
public class AbilitiesEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int ADD_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final EditorSession session;
    private final Runnable onBack;

    public AbilitiesEditorGUI(Player player, EditorSession session, Runnable onBack) {
        super(player, Component.text("Habilidades: " + session.original.id(), NamedTextColor.GOLD), SIZE);
        this.session = session;
        this.onBack = onBack;
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int slot = 36; slot < SIZE; slot++) {
            setItem(slot, new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                    .setName(Component.text(" ", NamedTextColor.GRAY)).build());
        }

        for (int i = 0; i < session.abilities.size() && i < 36; i++) {

            ItemAbility ability = session.abilities.get(i);

            setItem(i, new ItemBuilder(ability.passive() ? Material.BEACON : Material.BLAZE_POWDER)
                    .setName(Component.text(ability.displayName(), NamedTextColor.YELLOW))
                    .setLore(Component.text(ability.passive() ? "Pasiva" : "Activa (" + ability.trigger() + ")",
                            NamedTextColor.GRAY),
                            Component.text(ability.actions().size() + " acción(es)", NamedTextColor.GRAY),
                            Component.text("Click: editar · Shift-click: quitar", NamedTextColor.DARK_GRAY))
                    .build());
        }

        setItem(ADD_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Agregar habilidad", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < session.abilities.size() && slot < 36) {

            if (event.isShiftClick()) {
                session.abilities.remove(slot);
                build();
            } else {
                new AbilityEditGUI(player, session, slot, this::reopen).open();
            }
            return;
        }

        if (slot == ADD_SLOT) {
            promptNew();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void promptNew() {
        session.chatPromptManager.prompt(player, "Escribí el id de la nueva habilidad:", value -> {

            String id = value.trim();
            if (id.isBlank()) {
                player.sendMessage(Component.text("Id inválido.", NamedTextColor.RED));
                return;
            }

            session.abilities.add(new ItemAbility(id, id, true, null, 0, List.of(), List.of()));
            reopen();
        });
    }

    private void reopen() {
        new AbilitiesEditorGUI(player, session, onBack).open();
    }

}
