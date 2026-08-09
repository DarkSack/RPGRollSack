package com.sack.rpgroll.gui.admin;

import com.sack.rpgroll.util.ComponentUtils;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.gameplay.trait.Trait;
import com.sack.rpgroll.gameplay.trait.TraitEffect;
import com.sack.rpgroll.gameplay.trait.TraitManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Editor de un trait — identidad + sus 10 bonos de {@link TraitEffect}. Dado
 * el número de campos, se editan todos juntos vía una sola línea de chat
 * en vez de un botón por bono.
 */
public class TraitEditorGUI extends InventoryGUI {

    private static final int SIZE = 27;
    private static final int NAME_SLOT = 9;
    private static final int DESCRIPTION_SLOT = 10;
    private static final int LEVEL_SLOT = 11;
    private static final int EFFECT_SLOT = 12;
    private static final int BACK_SLOT = 26;

    private final TraitManager traitManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private Trait current;

    public TraitEditorGUI(Player player, Trait trait, TraitManager traitManager, ChatPromptManager chatPromptManager,
            Runnable onBack) {
        super(player, Component.text("Trait: " + trait.id(), NamedTextColor.GOLD), SIZE);
        this.current = trait;
        this.traitManager = traitManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
    }

    private void replace(Trait updated) {
        current = updated;
        traitManager.save(current);
        build();
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        setItem(NAME_SLOT, new ItemBuilder(Material.NAME_TAG)
                .setName(ComponentUtils.parse("Nombre: " + current.name()).colorIfAbsent(NamedTextColor.YELLOW))
                .setLore(Component.text("Click para escribir uno nuevo", NamedTextColor.GRAY))
                .build());

        setItem(DESCRIPTION_SLOT, new ItemBuilder(Material.WRITTEN_BOOK)
                .setName(Component.text("Descripción", NamedTextColor.YELLOW))
                .setLore(ItemBuilder.toLoreLines(current.description().isBlank() ? "(sin descripción)"
                        : current.description()))
                .build());

        setItem(LEVEL_SLOT, new ItemBuilder(Material.EXPERIENCE_BOTTLE)
                .setName(Component.text("Nivel requerido: " + current.requiredLevel(), NamedTextColor.YELLOW))
                .setLore(Component.text("Click: +1 · Click derecho: -1", NamedTextColor.GRAY))
                .build());

        TraitEffect effect = current.effect();
        setItem(EFFECT_SLOT, new ItemBuilder(Material.POTION)
                .setName(Component.text("Efecto", NamedTextColor.YELLOW))
                .setLore(Component.text("STR " + effect.strengthBonus() + " · DEX " + effect.dexterityBonus()
                        + " · CON " + effect.constitutionBonus(), NamedTextColor.GRAY),
                        Component.text("INT " + effect.intelligenceBonus() + " · WIS " + effect.wisdomBonus()
                                + " · CHA " + effect.charismaBonus(), NamedTextColor.GRAY),
                        Component.text("HP " + effect.healthBonus() + " · MP " + effect.manaBonus() + " · DMG "
                                + effect.damageBonus() + " · DEF " + effect.defenseBonus(), NamedTextColor.GRAY),
                        Component.text("Click para escribir todos (str,dex,con,int,wis,cha,hp,mp,dmg,def)",
                                NamedTextColor.DARK_GRAY))
                .build());

        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();
        ClickType click = event.getClick();

        if (slot == NAME_SLOT) {
            chatPromptManager.prompt(player, "Escribí el nuevo nombre:", value -> replace(new Trait(current.id(),
                    value, current.description(), current.requiredLevel(), current.effect())));
            return;
        }

        if (slot == DESCRIPTION_SLOT) {
            chatPromptManager.prompt(player, "Escribí la nueva descripción:", value -> replace(new Trait(
                    current.id(), current.name(), value, current.requiredLevel(), current.effect())));
            return;
        }

        if (slot == LEVEL_SLOT) {
            int delta = click == ClickType.RIGHT ? -1 : 1;
            replace(new Trait(current.id(), current.name(), current.description(),
                    Math.max(1, current.requiredLevel() + delta), current.effect()));
            return;
        }

        if (slot == EFFECT_SLOT) {
            chatPromptManager.prompt(player,
                    "Escribí 10 valores separados por comas (str,dex,con,int,wis,cha,hp,mp,dmg,def):", value -> {

                        String[] parts = value.split("\\s*,\\s*");

                        if (parts.length < 10) {
                            player.sendMessage(Component.text("Hacen falta 10 valores.", NamedTextColor.RED));
                            return;
                        }

                        try {
                            TraitEffect effect = new TraitEffect(
                                    Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim()),
                                    Integer.parseInt(parts[2].trim()), Integer.parseInt(parts[3].trim()),
                                    Integer.parseInt(parts[4].trim()), Integer.parseInt(parts[5].trim()),
                                    Integer.parseInt(parts[6].trim()), Integer.parseInt(parts[7].trim()),
                                    Double.parseDouble(parts[8].trim()), Double.parseDouble(parts[9].trim()));

                            replace(new Trait(current.id(), current.name(), current.description(),
                                    current.requiredLevel(), effect));
                        } catch (NumberFormatException e) {
                            player.sendMessage(Component.text("Todos los valores deben ser numéricos.",
                                    NamedTextColor.RED));
                        }
                    });
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

}
