package com.sack.rpgroll.items.gui.editor;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.items.core.ItemEffectDef;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Locale;

/** Editor de efectos de poción aplicados mientras el ítem está equipado. */
public class EffectsEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int ADD_SLOT = 40;
    private static final int BACK_SLOT = 40 + 4;

    private final EditorSession session;
    private final Runnable onBack;

    public EffectsEditorGUI(Player player, EditorSession session, Runnable onBack) {
        super(player, Component.text("Efectos: " + session.original.id(), NamedTextColor.GOLD), SIZE);
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

        for (int i = 0; i < session.effects.size() && i < 36; i++) {

            ItemEffectDef effect = session.effects.get(i);

            setItem(i, new ItemBuilder(Material.POTION)
                    .setName(Component.text(effect.potion() + " " + effect.amplifier(), NamedTextColor.LIGHT_PURPLE))
                    .setLore(Component.text("Mientras esté equipado: " + (effect.requiresHeld() ? "sí" : "no"),
                            NamedTextColor.GRAY),
                            Component.text("Shift-click para quitar", NamedTextColor.DARK_GRAY))
                    .build());
        }

        setItem(ADD_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Agregar efecto", NamedTextColor.GREEN))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < session.effects.size() && slot < 36) {
            if (event.isShiftClick()) {
                session.effects.remove(slot);
                build();
            }
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
        session.chatPromptManager.prompt(player,
                "Escribí: <poción> <amplificador> <si|no equipado> (ej. SPEED 1 si):", value -> {

                    String[] parts = value.trim().split("\\s+");

                    if (parts.length != 3) {
                        player.sendMessage(Component.text("Formato inválido.", NamedTextColor.RED));
                        return;
                    }

                    try {
                        String potion = parts[0].toUpperCase(Locale.ROOT);
                        int amplifier = Integer.parseInt(parts[1]);
                        boolean requiresHeld = parts[2].equalsIgnoreCase("si");

                        session.effects.add(new ItemEffectDef(potion, amplifier, requiresHeld));
                    } catch (NumberFormatException e) {
                        player.sendMessage(Component.text("Amplificador inválido.", NamedTextColor.RED));
                        return;
                    }

                    build();
                });
    }

}
