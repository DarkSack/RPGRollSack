package com.sack.rpgroll.workers.gui;

import com.sack.rpgroll.gui.InventoryGUI;
import com.sack.rpgroll.gui.util.ItemBuilder;
import com.sack.rpgroll.workers.core.economy.WageType;
import com.sack.rpgroll.workers.core.profession.Profession;
import com.sack.rpgroll.workers.core.profession.ProfessionManager;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ProfessionBrowserGUI extends InventoryGUI {

    private static final int SIZE = 45;
    private static final int NEW_SLOT = 40;
    private static final int BACK_SLOT = 44;

    private final ProfessionManager professionManager;
    private final ChatPromptManager chatPromptManager;
    private final Runnable onBack;
    private List<Profession> professions;

    public ProfessionBrowserGUI(Player player, ProfessionManager professionManager, ChatPromptManager chatPromptManager,
            Runnable onBack) {
        super(player, Component.text("Profesiones", NamedTextColor.GOLD), SIZE);
        this.professionManager = professionManager;
        this.chatPromptManager = chatPromptManager;
        this.onBack = onBack;
        this.professions = List.copyOf(professionManager.getAll());
    }

    static Material parseMaterial(String raw, Material fallback) {
        try {
            return Material.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    @Override
    public void build() {

        clear();

        for (int slot = 0; slot < SIZE; slot++) {
            setItem(slot, ItemBuilder.createFiller());
        }

        for (int i = 0; i < professions.size() && i < 36; i++) {

            Profession profession = professions.get(i);

            setItem(i, new ItemBuilder(parseMaterial(profession.icon(), Material.VILLAGER_SPAWN_EGG))
                    .setName(Component.text(profession.displayName(), NamedTextColor.YELLOW))
                    .setLore(Component.text("id: " + profession.id(), NamedTextColor.GRAY),
                            Component.text("entidad: " + profession.entityType(), NamedTextColor.GRAY),
                            Component.text("reglas de IA: " + profession.aiRules().size(), NamedTextColor.GRAY),
                            Component.text("Click para editar", NamedTextColor.YELLOW))
                    .build());
        }

        setItem(NEW_SLOT, new ItemBuilder(Material.EMERALD)
                .setName(Component.text("Crear profesión nueva", NamedTextColor.GREEN)).build());
        setItem(BACK_SLOT, ItemBuilder.createCancelButton("Volver"));
    }

    @Override
    public void handleClick(InventoryClickEvent event) {

        event.setCancelled(true);
        int slot = event.getSlot();

        if (slot < professions.size() && slot < 36) {
            new ProfessionEditorGUI(player, professions.get(slot), professionManager, chatPromptManager, this::reopen).open();
            return;
        }

        if (slot == NEW_SLOT) {
            promptNew();
            return;
        }

        if (slot == BACK_SLOT) {
            onBack.run();
        }
    }

    private void promptNew() {
        chatPromptManager.prompt(player, "Escribí el id de la nueva profesión:", value -> {

            String id = value.trim().toLowerCase(Locale.ROOT).replace(' ', '_');

            if (professionManager.exists(id)) {
                player.sendMessage(Component.text("Ya existe una profesión con ese id.", NamedTextColor.RED));
                reopen();
                return;
            }

            professionManager.save(new Profession(id, id, "VILLAGER_SPAWN_EGG", "", "VILLAGER", Set.of(), List.of(),
                    null, 0, WageType.PER_TASK, null));
            reopen();
        });
    }

    private void reopen() {
        this.professions = List.copyOf(professionManager.getAll());
        open();
    }

}
