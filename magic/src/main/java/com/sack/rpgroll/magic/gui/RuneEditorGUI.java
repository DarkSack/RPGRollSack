package com.sack.rpgroll.magic.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.magic.core.Rune;
import com.sack.rpgroll.magic.core.RuneManager;
import com.sack.rpgroll.magic.core.RuneModifierType;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public class RuneEditorGUI extends InventoryGUI {

    private static final int SIZE = 45;

    private static final int NAME_SLOT = 10;
    private static final int ICON_SLOT = 11;
    private static final int DESCRIPTION_SLOT = 12;
    private static final int TYPE_SLOT = 13;
    private static final int PARAMS_SLOT = 14;
    private static final int BACK_SLOT = 40;

    private final RuneManager runeManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private Rune current;

    public RuneEditorGUI(Player player, Rune rune, RuneManager runeManager, ChatPromptManager chatPromptManager,
            Runnable onBack) {
        super(player, Component.text("Runa: " + rune.id(), NamedTextColor.GOLD), SIZE);
        this.current = rune;
        this.runeManager = runeManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(Rune updated) {
        current = updated;
        runeManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(Component.text("Nombre: " + current.displayName(), NamedTextColor.YELLOW)).build());

        setItem(ICON_SLOT, new ItemBuilder(SchoolBrowserGUI.parseMaterial(current.icon()))
                .setName(Component.text("Ícono: " + current.icon(), NamedTextColor.YELLOW)).build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(Component.text("Descripción", NamedTextColor.YELLOW))
                .setLore(ItemBuilder.toLoreLines(
                        current.description().isBlank() ? "(sin descripción)" : current.description()))
                .build());

        setItem(TYPE_SLOT, new ItemBuilder(Material.NETHER_STAR)
                .setName(Component.text("Tipo: " + current.type(), NamedTextColor.GREEN))
                .setLore(Component.text("Click para pasar al siguiente", NamedTextColor.GRAY)).build());

        setItem(PARAMS_SLOT, new ItemBuilder(Material.COMMAND_BLOCK)
                .setName(Component.text("Params: " + current.params(), NamedTextColor.YELLOW))
                .setLore(Component.text("Escribí: clave=valor,clave2=valor2", NamedTextColor.GRAY),
                        paramsHelpFor(current.type()))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    private Component paramsHelpFor(RuneModifierType type) {
        String help = switch (type) {
            case EXTRA_PROJECTILES -> "ej. count=2";
            case PIERCING -> "ej. max-pierces=3";
            case EXPLOSIVE -> "ej. radius=3,damage=4";
            case APPLY_EFFECT -> "ej. effect-id=frozen (id de un efecto de RPGRoll-Effects)";
            case COST_MODIFIER, COOLDOWN_MODIFIER -> "ej. multiplier=0.5";
        };
        return Component.text(help, NamedTextColor.DARK_GRAY);
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nuevo nombre:", value -> replace(new Rune(current.id(),
                    value, current.icon(), current.description(), current.type(), current.params())));
            return;
        }

        if (slot == ICON_SLOT) {
            chatPromptManager.prompt(player, "Escribí el Material del ícono:", value -> replace(new Rune(
                    current.id(), current.displayName(), value, current.description(), current.type(),
                    current.params())));
            return;
        }

        if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, "Escribí la nueva descripción:", value -> replace(new Rune(current.id(),
                    current.displayName(), current.icon(), value, current.type(), current.params())));
            return;
        }

        if (slot == TYPE_SLOT) {
            RuneModifierType[] values = RuneModifierType.values();
            RuneModifierType next = values[(current.type().ordinal() + 1) % values.length];
            replace(new Rune(current.id(), current.displayName(), current.icon(), current.description(), next,
                    current.params()));
            return;
        }

        if (slot == PARAMS_SLOT) {
            chatPromptManager.prompt(player, "Escribí: clave=valor,clave2=valor2:", value -> {

                Map<String, String> params = new LinkedHashMap<>();

                for (String pair : value.split(",")) {
                    String[] kv = pair.split("=", 2);
                    if (kv.length == 2) {
                        params.put(kv[0].trim().toLowerCase(Locale.ROOT), kv[1].trim());
                    }
                }

                replace(new Rune(current.id(), current.displayName(), current.icon(), current.description(),
                        current.type(), params));
            });
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
