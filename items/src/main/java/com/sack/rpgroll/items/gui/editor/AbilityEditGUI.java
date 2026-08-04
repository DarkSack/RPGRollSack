package com.sack.rpgroll.items.gui.editor;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.items.core.ItemAbility;
import com.sack.rpgroll.items.core.ItemAction;
import com.sack.rpgroll.items.core.ItemTrigger;
import com.sack.rpgroll.items.util.DurationParser;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.ArrayList;
import java.util.List;

/** Detalle de una habilidad: nombre, pasiva/activa, trigger, cooldown, condiciones y sus acciones. */
public class AbilityEditGUI extends InventoryGUI {

    private static final int SIZE = 36;
    private static final int RENAME_SLOT = 10;
    private static final int PASSIVE_SLOT = 11;
    private static final int TRIGGER_SLOT = 12;
    private static final int COOLDOWN_SLOT = 13;
    private static final int CONDITIONS_SLOT = 14;
    private static final int ACTIONS_SLOT = 15;
    private static final int BACK_SLOT = 31;

    private final EditorSession session;
    private final int index;
    private final Runnable onBack;

    public AbilityEditGUI(Player player, EditorSession session, int index, Runnable onBack) {
        super(player, Component.text("Habilidad: " + session.abilities.get(index).id(), NamedTextColor.GOLD), SIZE);
        this.session = session;
        this.index = index;
        this.onBack = onBack;
    }

    private ItemAbility ability() {
        return session.abilities.get(index);
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int slot = 27; slot < SIZE; slot++) {
            setItem(slot, new ItemBuilder(Material.BLACK_STAINED_GLASS_PANE)
                    .setName(Component.text(" ", NamedTextColor.GRAY)).build());
        }

        ItemAbility ability = ability();

        setItem(RENAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(Component.text("Nombre: " + ability.displayName(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click para renombrar", NamedTextColor.GRAY))
                .build());

        setItem(PASSIVE_SLOT, new ItemBuilder(ability.passive() ? Material.BEACON : Material.BLAZE_POWDER)
                .setName(Component.text("Tipo: " + (ability.passive() ? "Pasiva" : "Activa"), NamedTextColor.AQUA))
                .setLore(Component.text("Click para alternar", NamedTextColor.GRAY))
                .build());

        setItem(TRIGGER_SLOT, new ItemBuilder(Material.COMPARATOR)
                .setName(Component.text("Trigger: " + (ability.trigger() == null ? "(ninguno)" : ability.trigger()),
                        NamedTextColor.AQUA))
                .setLore(Component.text("Click para ciclar (solo activas)", NamedTextColor.GRAY))
                .build());

        setItem(COOLDOWN_SLOT, new ItemBuilder(Material.CLOCK)
                .setName(Component.text("Cooldown: " + (ability.cooldownMillis() / 1000) + "s", NamedTextColor.AQUA))
                .setLore(Component.text("Click para escribir", NamedTextColor.GRAY))
                .build());

        List<Component> conditionsLore = new ArrayList<>();
        if (ability.conditions().isEmpty()) {
            conditionsLore.add(Component.text("(ninguna)", NamedTextColor.GRAY));
        } else {
            for (String condition : ability.conditions()) {
                conditionsLore.add(Component.text(condition, NamedTextColor.GRAY));
            }
        }

        setItem(CONDITIONS_SLOT, new ItemBuilder(Material.BOOK)
                .setName(Component.text("Condiciones (" + ability.conditions().size() + ")", NamedTextColor.AQUA))
                .setLore(conditionsLore)
                .build());

        setItem(ACTIONS_SLOT, new ItemBuilder(Material.COMMAND_BLOCK)
                .setName(Component.text("Acciones (" + ability.actions().size() + ")", NamedTextColor.GREEN))
                .setLore(Component.text("Click para editar", NamedTextColor.GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);

        switch (event.getSlot()) {
            case RENAME_SLOT -> promptRename();
            case PASSIVE_SLOT -> togglePassive();
            case TRIGGER_SLOT -> cycleTrigger();
            case COOLDOWN_SLOT -> promptCooldown();
            case CONDITIONS_SLOT -> promptConditions();
            case ACTIONS_SLOT -> openActions();
            case BACK_SLOT -> onBack.run();
            default -> {
            }
        }
    }

    private void replace(ItemAbility updated) {
        session.abilities.set(index, updated);
        build();
    }

    private void promptRename() {
        session.chatPromptManager.prompt(player, "Escribí el nuevo nombre de la habilidad:", value -> {
            ItemAbility a = ability();
            replace(new ItemAbility(a.id(), value, a.passive(), a.trigger(), a.cooldownMillis(), a.conditions(),
                    a.actions()));
        });
    }

    private void togglePassive() {
        ItemAbility a = ability();
        replace(new ItemAbility(a.id(), a.displayName(), !a.passive(), a.trigger(), a.cooldownMillis(),
                a.conditions(), a.actions()));
    }

    private void cycleTrigger() {

        ItemAbility a = ability();
        ItemTrigger[] values = ItemTrigger.values();

        ItemTrigger next;
        if (a.trigger() == null) {
            next = values[0];
        } else {
            int currentIndex = a.trigger().ordinal();
            next = currentIndex + 1 < values.length ? values[currentIndex + 1] : null;
        }

        replace(new ItemAbility(a.id(), a.displayName(), a.passive(), next, a.cooldownMillis(), a.conditions(),
                a.actions()));
    }

    private void promptCooldown() {
        session.chatPromptManager.prompt(player, "Escribí el cooldown (ej. 10s, 2m):", value -> {
            ItemAbility a = ability();
            long millis = DurationParser.parseMillis(value.trim());
            replace(new ItemAbility(a.id(), a.displayName(), a.passive(), a.trigger(), millis, a.conditions(),
                    a.actions()));
        });
    }

    private void promptConditions() {
        session.chatPromptManager.prompt(player,
                "Escribí las condiciones separadas por ';' (ej. player.level >= 20):", value -> {

                    ItemAbility a = ability();

                    List<String> conditions = value.isBlank()
                            ? List.of()
                            : List.of(value.split(";")).stream().map(String::trim).filter(s -> !s.isEmpty()).toList();

                    replace(new ItemAbility(a.id(), a.displayName(), a.passive(), a.trigger(), a.cooldownMillis(),
                            conditions, a.actions()));
                });
    }

    private void openActions() {

        List<ItemAction> workingActions = new ArrayList<>(ability().actions());

        new ActionListEditorGUI(player, "Acciones de " + ability().displayName(), workingActions,
                session.chatPromptManager, () -> {

                    ItemAbility a = ability();
                    session.abilities.set(index, new ItemAbility(a.id(), a.displayName(), a.passive(), a.trigger(),
                            a.cooldownMillis(), a.conditions(), workingActions));

                    new AbilityEditGUI(player, session, index, onBack).open();
                }).open();
    }

}
